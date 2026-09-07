"""Small, offline harness primitives. No product or AIR semantics live here."""
from __future__ import annotations

import json
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class Failure(Exception):
    pass


def require(condition, message):
    if not condition:
        raise Failure(message)


def unique_keys(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, f"Duplicate JSON key: {key}")
        result[key] = value
    return result


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=unique_keys)


def relative_path(root, value, *, exists=True):
    require(isinstance(value, str) and value and not Path(value).is_absolute(),
            f"Expected repository-relative path: {value!r}")
    require(".." not in Path(value).parts, f"Parent traversal forbidden: {value}")
    path = root / value
    require(path.resolve().is_relative_to(root.resolve()), f"Path escapes repository: {value}")
    require(not exists or path.exists(), f"Missing path: {value}")
    return path


def run(command, root=ROOT):
    result = subprocess.run(command, cwd=root, text=True, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, check=False)
    require(result.returncode == 0,
            f"Command failed ({result.returncode}): {' '.join(map(str, command))}\n{result.stdout}")
    return result.stdout


def git(root, *args):
    return run(["git", *args], root).strip()


def matches_scope(path, scope):
    # Literal file or directory subtree; no shell/glob interpretation.
    return path == scope.rstrip("/") or (scope.endswith("/") and path.startswith(scope))


def full_sha(value):
    return isinstance(value, str) and re.fullmatch(r"[0-9a-f]{40}", value) is not None
