#!/usr/bin/env python3
"""Build, test, package and deploy the knowledge tablet app."""
import argparse
import json
import os
import platform
import shlex
from pathlib import Path
import shutil
import subprocess
import sys
import tarfile
import tempfile

from common import ROOT, SSH_OPTIONS, check_host, check_elf, elf_dependencies, run

TABLET = ROOT / 'tablet'
DEPLOY = TABLET / 'deploy'
DIST = ROOT / 'dist'
NAME = 'knowledge'
MAC_BUILD = ROOT / 'build/tablet-mac'


def mac_build():
    qt = subprocess.run(['brew', '--prefix', 'qt'], capture_output=True, text=True, check=True).stdout.strip()
    run('cmake', '-S', TABLET, '-B', MAC_BUILD, '-DCMAKE_BUILD_TYPE=Release', '-DCMAKE_PREFIX_PATH=' + qt)
    run('cmake', '--build', MAC_BUILD, '--parallel')
    return MAC_BUILD / 'app' / NAME


def check_docker():
    executable = shutil.which('docker')
    if not executable:
        raise RuntimeError('Docker is not installed. Install and start Docker Desktop first.')
    result = subprocess.run([executable, 'info', '--format', '{{.OSType}}/{{.Architecture}}'],
                            capture_output=True, text=True, timeout=15)
    if result.returncode:
        raise RuntimeError('Cannot access Docker Engine. ' + result.stderr.strip()[-600:])
    return executable


def build_tablet(url=None):
    executable = check_docker()
    architecture = 'aarch64' if platform.machine().lower() in ('arm64', 'aarch64') else 'x86_64'
    if not url:
        configuration = json.loads((DEPLOY / 'sdk.json').read_text())
        url = configuration['urls'][architecture]
        print(f"SDK candidate: {configuration['candidateFirmware']}; "
              f"tablet target: {configuration['targetFirmware']}.", flush=True)
    target = 'linux/arm64' if architecture == 'aarch64' else 'linux/amd64'
    run(executable, 'build', '--progress=plain', '--platform', target, '--build-arg', f'SDK_URL={url}',
        '--build-arg', f'SDK_HOST_ARCH={architecture}', '-t', 'knowledge-sdk:local', DEPLOY)
    run(executable, 'run', '--rm', '--platform', target, '-v', f'{ROOT}:/workspace', 'knowledge-sdk:local')


def package(binary):
    binary = Path(binary).resolve()
    check_elf(binary)
    dependencies = elf_dependencies(binary)
    DIST.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory() as directory:
        bundle = Path(directory) / NAME
        bundle.mkdir()
        shutil.copy2(binary, bundle / NAME)
        shutil.copy2(DEPLOY / 'run-on-tablet.sh', bundle / 'run-on-tablet.sh')
        for filename in (NAME, 'run-on-tablet.sh'):
            (bundle / filename).chmod(0o755)
        (bundle / 'metadata.json').write_text(json.dumps({
            'name': NAME, 'architecture': 'aarch64', 'dependencies': dependencies,
            'sdkSource': os.environ.get('INK_BUILD_SDK_URL', 'user-provided build'),
        }, indent=2) + '\n')
        archive = DIST / f'{NAME}.tar.gz'
        with tarfile.open(archive, 'w:gz') as output:
            output.add(bundle, arcname=NAME)
    print(f'Bundle: {archive}\nDependencies: {", ".join(dependencies)}')


def deploy(host):
    archive = DIST / f'{NAME}.tar.gz'
    if not archive.is_file():
        raise RuntimeError('No tablet bundle. Run: python3 tools/tablet.py build')
    script = f'''set -eu
root=/home/root/{NAME}
if systemctl is-active --quiet {NAME}.service; then
    echo 'Stop {NAME} before deploying.' >&2; exit 1
fi
mkdir -p "$root"
stage=$(mktemp -d "$root/stage.XXXXXX")
trap 'rm -rf "$stage"' EXIT
tar -xzf - -C "$stage"
"$stage/{NAME}/{NAME}" --probe
if test -d "$root/current"; then
    previous=$(mktemp -d "$root/previous.XXXXXX")
    mv "$root/current" "$previous/bundle"
    if ! mv "$stage/{NAME}" "$root/current"; then
        mv "$previous/bundle" "$root/current"; exit 1
    fi
else
    mv "$stage/{NAME}" "$root/current"
fi
echo 'Installed.'
'''
    with archive.open('rb') as source:
        run('ssh', *SSH_OPTIONS, host, script, stdin=source)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command', required=True)
    preview = commands.add_parser('preview', help='Build and run the macOS preview')
    preview.add_argument('--smoke-test', action='store_true')
    commands.add_parser('test', help='Build and run native tests on macOS')
    build = commands.add_parser('build', help='Build for the tablet in the SDK container')
    build.add_argument('--sdk-url', help='Override the recorded SDK candidate')
    package_parser = commands.add_parser('package', help='Package an SDK-built AArch64 executable')
    package_parser.add_argument('--binary', required=True)
    for name in ('deploy', 'run', 'stop', 'logs'):
        command = commands.add_parser(name)
        command.add_argument('--host', default='rm')
        if name == 'run':
            command.add_argument('--pen-input', choices=['none', 'evdevtablet'], default='none')
    args = parser.parse_args()
    if args.command == 'preview':
        binary = mac_build()
        env = os.environ.copy()
        arguments = [binary]
        if args.smoke_test:
            env.update(QT_QPA_PLATFORM='offscreen', QT_QUICK_BACKEND='software')
            arguments.append('--smoke-test')
        run(*arguments, env=env)
    elif args.command == 'test':
        mac_build()
        run('ctest', '--test-dir', MAC_BUILD, '--output-on-failure')
    elif args.command == 'build':
        build_tablet(args.sdk_url)
    elif args.command == 'package':
        package(args.binary)
    else:
        check_host(args.host)
        if args.command == 'deploy':
            deploy(args.host)
        elif args.command == 'run':
            run('ssh', *SSH_OPTIONS, args.host,
                f'/home/root/{NAME}/current/run-on-tablet.sh ' + shlex.quote(args.pen_input))
        elif args.command == 'stop':
            run('ssh', *SSH_OPTIONS, args.host, f'systemctl stop {NAME}.service')
        elif args.command == 'logs':
            run('ssh', *SSH_OPTIONS, '-t', args.host, f'journalctl -u {NAME}.service -n 100 -f --no-pager')


if __name__ == '__main__':
    try:
        main()
    except subprocess.CalledProcessError as error:
        print(f'Error: {Path(str(error.cmd[0])).name} failed (exit {error.returncode}).', file=sys.stderr)
        sys.exit(1)
    except (RuntimeError, OSError, subprocess.TimeoutExpired) as error:
        print(f'Error: {error}', file=sys.stderr)
        sys.exit(1)
