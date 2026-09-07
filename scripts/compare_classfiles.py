#!/usr/bin/env python3
"""Compare a captured baseline JAR with the reactor model; no fixed class count."""
import argparse
import hashlib
import json
import struct
import subprocess
import zipfile
from pathlib import Path


def digest(data):
    return hashlib.sha256(data).hexdigest()


def classes(path):
    with zipfile.ZipFile(path) as jar:
        names = jar.namelist()
        if len(names) != len(set(names)):
            raise ValueError('Duplicate JAR entries')
        result = {name: jar.read(name) for name in sorted(names) if name.endswith('.class')}
        if not result:
            raise ValueError('Empty model inventory')
        return result


def command(*args):
    return subprocess.check_output(args, text=True).encode()


def compare(baseline, candidate):
    old, new = classes(baseline), classes(candidate)
    if old.keys() != new.keys():
        raise ValueError(f'Class inventory mismatch: removed={old.keys() - new.keys()}, added={new.keys() - old.keys()}')
    changed = [name for name in old if old[name] != new[name]]
    if changed:
        raise ValueError(f'Product bytecode differs: {changed}')
    names = [name[:-6].replace('/', '.') for name in old]
    api_old = command('javap', '-classpath', str(baseline), '-public', '-s', *names)
    api_new = command('javap', '-classpath', str(candidate), '-public', '-s', *names)
    if api_old != api_new:
        raise ValueError('Public API differs')
    # JAR basenames are equal for the preserved GAV; normalize only this header label.
    deps = []
    for jar in (baseline, candidate):
        output = command('jdeps', '--multi-release', '21', '-verbose:class', '-filter:none', str(jar))
        deps.append(output.replace(jar.name.encode(), b'air-java.jar'))
    if deps[0] != deps[1]:
        raise ValueError('Bytecode dependency output differs')
    inventory = {}
    for name, data in old.items():
        magic, minor, major = struct.unpack('>IHH', data[:8])
        if (magic, minor, major) != (0xCAFEBABE, 0, 65):
            raise ValueError(f'Unexpected Java class version: {name}')
        inventory[name] = {'sha256': digest(data), 'major': major, 'minor': minor}
    with zipfile.ZipFile(baseline) as a, zipfile.ZipFile(candidate) as b:
        metadata_changes = [n for n in sorted(set(a.namelist()) | set(b.namelist()))
                            if not n.endswith('/') and (n not in a.namelist() or n not in b.namelist() or a.read(n) != b.read(n))]
    return {'class_count': len(old), 'identical_bytes': True, 'public_api_sha256': digest(api_old),
            'jdeps_sha256': digest(deps[0]), 'packages': sorted({n.rpartition('/')[0].replace('/', '.') for n in old}),
            'jar_changed_entries': metadata_changes, 'classfiles': inventory}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--baseline-jar', type=Path, required=True)
    parser.add_argument('--candidate-jar', type=Path, required=True)
    parser.add_argument('--baseline-commit', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    result = compare(args.baseline_jar, args.candidate_jar)
    result['baseline_commit'] = args.baseline_commit
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    print(f"PASS: {result['class_count']} classfiles byte-identical, Java 21, public API and dependencies equivalent")
