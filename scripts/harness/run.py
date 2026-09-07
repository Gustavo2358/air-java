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
import docs
import git_checks
import manifest
from common import ROOT, Failure, git, read_json, require, run
from pathlib import Path

UNAVAILABLE = {"integration", "performance"}
GROUPS = {"fast": ["docs", "harness"],
          "full": ["docs", "harness", "architecture", "semantic", "maven"]}


def execute(name, root, work):
    if name == "docs":
        return docs.check(root) + "; " + manifest.check(root)
    if name == "architecture":
        return architecture.check(root)
    if name == "transport":
        return architecture.transport(root)
    if name in {"semantic", "maven"}:
        return contracts.check(root, maven=name == "maven")
    if name in {"git", "scope"}:
        return git_checks.check(root, work, scope_only=name == "scope")
    if name == "ci-scope":
        require(os.environ.get("GITHUB_EVENT_PATH"), "ci-scope requires GitHub event metadata")
        event = read_json(Path(os.environ["GITHUB_EVENT_PATH"]))
        base = event.get("pull_request", {}).get("base", {}).get("sha") or event.get("before")
        if base == "0" * 40:
            base = git(root, "rev-parse", "origin/main")
        return git_checks.check_ci(root, base)
    if name == "harness":
        for file in ("test_docs.py", "test_architecture.py", "test_modules.py", "test_manifest.py", "test_execution.py", "test_git.py"):
            require((root / "scripts/harness/tests" / file).is_file(), f"Missing harness suite: {file}")
        output = run([sys.executable, "-B", "-m", "unittest", "discover", "-s",
                      "scripts/harness/tests", "-v"], root).strip()
        require(re.search(r"^Ran [1-9][0-9]* tests? in ", output, re.M)
                and output.endswith("\nOK") and "... skipped" not in output,
                "Harness suite missing, empty or skipped")
        return output
    raise Failure(f"No executor: {name}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("gate", choices=sorted(docs.GATES))
    parser.add_argument("--work", help="Active work item for git/scope; no inferred authorization")
    args = parser.parse_args()
    if args.gate in {"git", "scope"} and not args.work:
        parser.error("git/scope require --work")
    report = {"gate": args.gate, "head": git(ROOT, "rev-parse", "HEAD"),
              "observed_at": datetime.now(timezone.utc).isoformat(), "results": []}
    code = 0
    for name in GROUPS.get(args.gate, [args.gate]):
        print(f"RUN: {name}", flush=True)
        try:
            if name in UNAVAILABLE:
                status, detail, code = "UNAVAILABLE", "Future work; no executor or product claim", 3
            else:
                detail = execute(name, ROOT, args.work)
                status = "PASS"
        except Failure as error:
            status, detail, code = "FAIL", str(error), 1
        except (OSError, ValueError, KeyError, TypeError) as error:
            status, detail, code = "ERROR", str(error), 2
        print(f"{status}: {name}: {detail}", flush=True)
        report["results"].append({"gate": name, "status": status, "detail": detail})
        if code:
            break
    report["exit_code"] = code
    report["unexecuted"] = [name for name in GROUPS.get(args.gate, [args.gate])
                            if name not in {entry["gate"] for entry in report["results"]}]
    output = ROOT / "target/harness"
    output.mkdir(parents=True, exist_ok=True)
    (output / f"{args.gate}.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    return code


if __name__ == "__main__":
    sys.exit(main())
