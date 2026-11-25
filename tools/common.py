"""Shared host-side process, SSH and ELF utilities."""
from pathlib import Path
import re
import struct
import subprocess

ROOT = Path(__file__).resolve().parents[1]
SSH_OPTIONS = ['-o', 'ConnectTimeout=8', '-o', 'ConnectionAttempts=1', '-o', 'ServerAliveInterval=10', '-o', 'ServerAliveCountMax=3']

def run(*args, **kwargs):
    return subprocess.run([str(x) for x in args], check=True, **kwargs)

def check_elf(path, machine=183):
    with open(path, 'rb') as source:
        header = source.read(20)
    if (len(header) < 20 or header[:6] != b'\x7fELF\x02\x01'
            or struct.unpack_from('<H', header, 18)[0] != machine):
        raise RuntimeError(f'{path} must be a little-endian ELF64 executable for machine {machine}')

def elf_dependencies(path):
    """Read ELF64 DT_NEEDED entries without a host readelf executable."""
    data = Path(path).read_bytes()
    try:
        phoff = struct.unpack_from('<Q', data, 32)[0]
        entry_size, count = struct.unpack_from('<HH', data, 54)
        if entry_size < 56:
            raise ValueError('Invalid program header size')
        segments = [struct.unpack_from('<IIQQQQQQ', data, phoff + i * entry_size)
                    for i in range(count)]
        dynamic = next((s for s in segments if s[0] == 2), None)
        if dynamic is None:
            return []
        entries = []
        for offset in range(dynamic[2], dynamic[2] + dynamic[5], 16):
            tag, value = struct.unpack_from('<qQ', data, offset)
            if tag == 0:
                break
            entries.append((tag, value))
        address = next(value for tag, value in entries if tag == 5)
        load = next(s for s in segments if s[0] == 1 and s[3] <= address < s[3] + s[5])
        base = load[2] + address - load[3]
        return [data[base + value:data.index(b'\0', base + value)].decode('utf-8')
                for tag, value in entries if tag == 1]
    except (struct.error, ValueError, StopIteration, UnicodeDecodeError) as error:
        raise RuntimeError(f'Cannot read ELF dependencies from {path}: {error}') from error

def check_host(host):
    if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.@-]*', host):
        raise RuntimeError('Use an SSH config alias such as rm for --host.')
