#!/usr/bin/env python3
"""Build, test, package and deploy the rm-knowledge tablet app."""
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

TABLET = ROOT / 'client/tablet'
DEPLOY = TABLET / 'deploy'
DIST = ROOT / 'dist'
MAC_BUILD = ROOT / 'build/tablet-mac'
# App name -> executable path within a build directory.
APPS = {
    'rm-knowledge': 'app/rm-knowledge',
    'interaction-benchmark': 'toolkit/examples/interaction-benchmark/interaction-benchmark',
}


def mac_build():
    qt = subprocess.run(['brew', '--prefix', 'qt'], capture_output=True, text=True, check=True).stdout.strip()
    run('cmake', '-S', TABLET, '-B', MAC_BUILD, '-DCMAKE_BUILD_TYPE=Release', '-DCMAKE_PREFIX_PATH=' + qt)
    run('cmake', '--build', MAC_BUILD, '--parallel')


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
        '--build-arg', f'SDK_HOST_ARCH={architecture}', '-t', 'rm-knowledge-sdk:local', DEPLOY)
    run(executable, 'run', '--rm', '--platform', target, '-v', f'{ROOT}:/workspace', 'rm-knowledge-sdk:local')


def package(binary, name):
    binary = Path(binary).resolve()
    check_elf(binary)
    dependencies = elf_dependencies(binary)
    DIST.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory() as directory:
        bundle = Path(directory) / name
        bundle.mkdir()
        shutil.copy2(binary, bundle / name)
        shutil.copy2(DEPLOY / 'run-on-tablet.sh', bundle / 'run-on-tablet.sh')
        for filename in (name, 'run-on-tablet.sh'):
            (bundle / filename).chmod(0o755)
        (bundle / 'metadata.json').write_text(json.dumps({
            'name': name, 'architecture': 'aarch64', 'dependencies': dependencies,
            'sdkSource': os.environ.get('INK_BUILD_SDK_URL', 'user-provided build'),
        }, indent=2) + '\n')
        archive = DIST / f'{name}.tar.gz'
        with tarfile.open(archive, 'w:gz') as output:
            output.add(bundle, arcname=name)
    print(f'Bundle: {archive}\nDependencies: {", ".join(dependencies)}')


def deploy(host, name):
    archive = DIST / f'{name}.tar.gz'
    if not archive.is_file():
        raise RuntimeError('No tablet bundle. Run: python3 tools/tablet.py build')
    script = f'''set -eu
root=/home/root/{name}
if systemctl is-active --quiet {name}.service; then
    echo 'Stop {name} before deploying.' >&2; exit 1
fi
mkdir -p "$root"
stage=$(mktemp -d "$root/stage.XXXXXX")
trap 'rm -rf "$stage"' EXIT
tar -xzf - -C "$stage"
"$stage/{name}/{name}" --probe
if test -d "$root/current"; then
    previous=$(mktemp -d "$root/previous.XXXXXX")
    mv "$root/current" "$previous/bundle"
    if ! mv "$stage/{name}" "$root/current"; then
        mv "$previous/bundle" "$root/current"; exit 1
    fi
else
    mv "$stage/{name}" "$root/current"
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
    for command in (preview, package_parser):
        command.add_argument('--app', choices=APPS, default='rm-knowledge')
    for name in ('deploy', 'run', 'stop', 'logs'):
        command = commands.add_parser(name)
        command.add_argument('--app', choices=APPS, default='rm-knowledge')
        command.add_argument('--host', default='rm')
        if name == 'run':
            command.add_argument('--pen-input', choices=['none', 'evdevtablet'], default='none')
            command.add_argument('--server', default='', help='Backend base URL')
    args = parser.parse_args()
    if args.command == 'preview':
        mac_build()
        binary = MAC_BUILD / APPS[args.app]
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
        package(args.binary, args.app)
    else:
        check_host(args.host)
        if args.command == 'deploy':
            deploy(args.host, args.app)
        elif args.command == 'run':
            run('ssh', *SSH_OPTIONS, args.host, f'RM_KNOWLEDGE_SERVER={shlex.quote(args.server)} '
                f'/home/root/{args.app}/current/run-on-tablet.sh ' + shlex.quote(args.pen_input))
        elif args.command == 'stop':
            run('ssh', *SSH_OPTIONS, args.host, f'systemctl stop {args.app}.service')
        elif args.command == 'logs':
            run('ssh', *SSH_OPTIONS, '-t', args.host, f'journalctl -u {args.app}.service -n 100 -f --no-pager')


if __name__ == '__main__':
    try:
        main()
    except subprocess.CalledProcessError as error:
        print(f'Error: {Path(str(error.cmd[0])).name} failed (exit {error.returncode}).', file=sys.stderr)
        sys.exit(1)
    except (RuntimeError, OSError, subprocess.TimeoutExpired) as error:
        print(f'Error: {error}', file=sys.stderr)
        sys.exit(1)
