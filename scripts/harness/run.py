#!/usr/bin/env python3
"""Stable gates. Exit 0 PASS, 1 FAIL, 2 environment/usage, 3 UNAVAILABLE."""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
from datetime import datetime, timezone

sys.dont_write_bytecode = True

import architecture
import contracts
from common import ROOT, Failure, git, read_json, require, run
from pathlib import Path

UNAVAILABLE = {"integration", "performance"}


def execute(name, root, work):
    if name == "docs":
        from lean import check
        check(root)
        return 'PASS lean policy'
    if name == "architecture":
        return architecture.check(root)
    if name == "transport":
        return architecture.transport(root)
    if name in {"semantic", "maven"}:
        return contracts.check(root, maven=name == "maven")
    if name == "harness":
        for file in ("test_architecture.py", "test_modules.py", "test_execution.py"):
            require((root / "scripts/harness/tests" / file).is_file(), f"Missing harness suite: {file}")
        output = run([sys.executable, "-B", "-m", "unittest", "discover", "-s",
                      "scripts/harness/tests", "-v"], root).strip()
        require(re.search(r"^Ran [1-9][0-9]* tests? in ", output, re.M)
                and output.endswith("\nOK") and "... skipped" not in output,
                "Harness suite missing, empty or skipped")
        return output
    raise Failure(f"No executor: {name}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('gate', choices=['fast','docs','harness','architecture','semantic','transport','full','qualification-local','maven'])
    args = parser.parse_args()
    from lean import execute as lean_execute, require_local
    from lean_project import full_local
    try:
        if args.gate in ('fast', 'docs'):
            lean_execute('CODE_CHANGE' if args.gate == 'fast' else 'DOCS_ONLY', ROOT)
        elif args.gate in ('full','qualification-local'):
            require_local()
            full_local(ROOT)
        else:
            if args.gate == 'maven':
                require_local()
            print(execute(args.gate, ROOT, None))
        print('PASS')
        return 0
    except (Failure, RuntimeError, OSError) as error:
        print('FAIL: ' + str(error), file=sys.stderr)
        return 1

if __name__ == '__main__':
    sys.exit(main())
