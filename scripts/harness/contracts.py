"""Verify actual ContractSuite output against an independently reviewed inventory."""
import os
import re
from pathlib import Path

from common import read_json, require, run


def inspect_output(output, expected):
    require(expected and len(set(expected)) == len(expected), "Empty/duplicate contract inventory")
    records = [(int(n), name) for n, name in re.findall(r"^ok (\d+) - (.+)$", output, re.M)]
    summaries = re.findall(r"^PASS: (\d+) deterministic contract checks$", output, re.M)
    require([name for _, name in records] == expected, "Contract inventory missing, duplicated or reordered")
    require([n for n, _ in records] == list(range(1, len(expected) + 1)), "Invalid contract numbering")
    require(summaries == [str(len(expected))], "Missing, duplicate or incorrect contract summary")
    require(not re.search(r"^FAIL\b", output, re.M), "Contract failure in output")
    return len(expected)


def check(root, *, maven=False):
    expected = read_json(root / "docs/evals/contract-checks.json")["checks"]
    if maven:
        # Dependencies are build plugins only. Do not install/publish a library.
        cache = Path(os.environ.get("AIR_MAVEN_REPO", "/tmp/air-java-harness-m2"))
        require(cache.is_absolute() and not cache.resolve().is_relative_to((root / "target").resolve()),
                "AIR_MAVEN_REPO must be absolute and outside target (Maven clean)")
        command = ["mvn", "--batch-mode", "--no-transfer-progress",
                   f"-Dmaven.repo.local={cache}", "clean", "verify"]
    else:
        command = [str(root / "scripts/check.sh")]
    output = run(command, root)
    count = inspect_output(output, expected)
    if maven:
        require("BUILD SUCCESS" in output, "Maven did not complete")
    return f"{count} deterministic checks executed via {'Maven clean verify' if maven else 'scripts/check.sh'}"
