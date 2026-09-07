"""Verify hashes and complete Git-visible coverage; never rewrite the manifest."""
import hashlib
import re

from common import git, require


def check(root):
    entries = {}
    for line in (root / 'MANIFEST.sha256').read_text().splitlines():
        match = re.fullmatch(r'([0-9a-f]{64})  (.+)', line)
        require(match, f'Invalid MANIFEST line: {line}')
        digest, name = match.groups()
        require(name not in entries, f'Duplicate MANIFEST path: {name}')
        require(name != 'MANIFEST.sha256' and not any(p in {'target', '.git', '__pycache__'} for p in name.split('/'))
                and not name.endswith(('.class', '.jar', '.pyc')), f'Generated/self MANIFEST path: {name}')
        entries[name] = digest
    visible = set(git(root, 'ls-files', '-z', '--cached', '--others', '--exclude-standard').split('\0')) - {''}
    visible = {name for name in visible if (root / name).is_file()} - {'MANIFEST.sha256'}
    require(set(entries) == visible,
            f'MANIFEST coverage mismatch: missing={sorted(visible - entries.keys())}; extra={sorted(entries.keys() - visible)}')
    for name, digest in entries.items():
        require(hashlib.sha256((root / name).read_bytes()).hexdigest() == digest, f'MANIFEST hash mismatch: {name}')
    return f'{len(entries)} Git-visible paths; MANIFEST hashes and coverage verified'
