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
    from module_policy import MODULES, inspect_topology
    version = inspect_topology(root)
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
    transport_count = inspect_transport_output(output, read_json(root / "docs/evals/transport-checks.json")["checks"])
    if maven:
        require("BUILD SUCCESS" in output, "Maven did not complete")
        inspect_reactor_output(output)
    for owner, artifact in MODULES.items():
        require((root / owner / 'target' / f'{artifact}-{version}.jar').is_file(),
                f'Missing reactor artifact: {owner}')
    return f"{count} model + {transport_count} transport deterministic checks executed via {'Maven clean verify' if maven else 'scripts/check.sh'}"


def inspect_reactor_output(output):
    """Only the full root invocation claims a complete reactor, never a cached JAR."""
    require(len(re.findall(r'^PASS: reactor topology ', output, re.M)) == 1,
            'Missing or duplicated reactor topology verification')
    owners = re.findall(r'^PASS: compiled module ([\w-]+);', output, re.M)
    require(owners == ['air-model', 'air-json'], 'Incomplete, duplicated or reordered reactor verification')


def inspect_transport_output(output, expected):
    require(expected and len(set(expected)) == len(expected), 'Empty/duplicate transport inventory')
    records = [(int(n), name) for n, name in re.findall(r'^json-ok (\d+) - (.+)$', output, re.M)]
    summaries = re.findall(r'^PASS: (\d+) deterministic transport checks$', output, re.M)
    require([name for _, name in records] == expected, 'Transport inventory missing, duplicated or reordered')
    require([n for n, _ in records] == list(range(1, len(expected) + 1)), 'Invalid transport numbering')
    require(summaries == [str(len(expected))], 'Missing, duplicate or incorrect transport summary')
    require(not re.search(r'^FAIL\b', output, re.M), 'Transport failure in output')
    return len(expected)
