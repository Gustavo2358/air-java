"""Validate the local knowledge graph and work lifecycle; never infer authorization."""
from __future__ import annotations

import re
from urllib.parse import unquote, urlsplit

from common import full_sha, read_json, relative_path, require

GATES = {"docs", "harness", "fast", "architecture", "semantic", "maven", "git", "scope", "ci-scope", "full",
         "transport", "integration", "performance"}
WORK_FILES = {"work-item.json", "spec.md", "plan.md", "eval.md", "state.md"}
WORK_FIELDS = {"id", "title", "status", "authorization", "authorization_evidence", "goal",
               "checkpoint", "base_commit", "branch", "must_read", "change_scope",
               "must_not_change", "invariants", "evals", "gates", "stop_condition"}
STATE_HEADINGS = {"Onde estamos", "Verde conhecido", "Restante", "Descobertas que afetam o plano"}


def strip_fences(text):
    return re.sub(r"^(`{3,}|~{3,})[^\n]*\n.*?^\1\s*$", "", text, flags=re.M | re.S)


def anchors(text):
    found, counts = set(), {}
    for heading in re.findall(r"^#{1,6}\s+(.+?)(?:\s+#+)?$", strip_fences(text), re.M):
        slug = re.sub(r"[^\w\- ]", "", heading.lower()).replace(" ", "-")
        count = counts.get(slug, 0)
        counts[slug] = count + 1
        found.add(slug if count == 0 else f"{slug}-{count}")
    return found | set(re.findall(r'<a\s+id=[\'"]([^\'"]+)', text))


def check_links(root, path):
    text = strip_fences(path.read_text(encoding="utf-8"))
    # Harness documentation uses inline links or reference definitions, without titles.
    links = re.findall(r"\[[^\]\n]*\]\(([^)\s]+)\)", text)
    links += re.findall(r"^\[[^\]\n]+\]:\s+(\S+)", text, re.M)
    for link in links:
        parsed = urlsplit(link.strip("<>"))
        if parsed.scheme in {"https", "http", "mailto"}:
            continue  # Existence/network/authority is a separate, explicit review.
        require(not parsed.scheme and not parsed.netloc, f"Unsupported link in {path}: {link}")
        require(not parsed.path.startswith("/"), f"Absolute local link in {path}: {link}")
        target = path.parent / unquote(parsed.path) if parsed.path else path
        require(target.resolve().is_relative_to(root.resolve()) and target.exists(),
                f"Broken/escaping link in {path.relative_to(root)}: {link}")
        if parsed.fragment:
            require(target.suffix == ".md" and unquote(parsed.fragment) in anchors(target.read_text(encoding="utf-8")),
                    f"Missing anchor in {path.relative_to(root)}: {link}")


def catalog(root, path, pattern):
    entries = read_json(root / path)
    require(isinstance(entries, list) and entries, f"Empty/invalid catalog: {path}")
    result = {}
    for entry in entries:
        identifier = entry["id"]
        require(re.fullmatch(pattern, identifier) and identifier not in result, f"Invalid/duplicate ID: {identifier}")
        relative_path(root, entry["document"])
        result[identifier] = entry
    return result


def validate_work(root, item, invariants, evals):
    require(set(item) == WORK_FIELDS, "Work item fields differ from documented schema")
    for key in WORK_FIELDS - {"must_read", "change_scope", "must_not_change", "invariants", "evals", "gates"}:
        require(isinstance(item[key], str) and item[key].strip(), f"Empty work field: {key}")
    require(re.fullmatch(r"WORK-AIR-[A-Z0-9-]+", item["id"]), "Invalid work ID")
    require(item["status"] in {"active", "blocked", "ready_for_review"}, "Completed/invalid item in active")
    require(item["authorization"] in {"discovery", "harness", "implementation"}, "Invalid authorization")
    require(full_sha(item["base_commit"]), "Work base must be a full SHA")
    require(item["branch"] not in {"main", "master"}, "Work branch must be dedicated")
    for key in ("must_read", "change_scope", "must_not_change", "invariants", "evals", "gates"):
        values = item[key]
        require(isinstance(values, list) and values and all(isinstance(v, str) and v for v in values),
                f"Empty/invalid work list: {key}")
        require(len(set(values)) == len(values), f"Duplicate work list: {key}")
    for value in item["must_read"]:
        relative_path(root, value)
    for value in item["change_scope"] + item["must_not_change"]:
        relative_path(root, value, exists=False)
        require(not any(c in value for c in "*?[]"), "Scopes must be literal files or directories")
    require(set(item["invariants"]) <= invariants.keys(), "Unknown invariant in work item")
    require(set(item["evals"]) <= evals.keys(), "Unknown eval in work item")
    require(set(item["gates"]) <= GATES, "Unknown/missing work gate")
    if item["authorization"] == "harness":
        require("full" in item["gates"], "Harness work requires full gate")
    if item["authorization"] in {"harness", "discovery"}:
        for scope in item["change_scope"]:
            require(not scope.startswith(("src/", "air-model/", "air-json/", "examples/", "pom.xml")),
                    "Harness/discovery scope cannot authorize product changes")


def check(root):
    for path in [root / "AGENTS.md", root / "ARCHITECTURE.md", root / "README.md",
                 *sorted((root / "docs").rglob("*.md")), *sorted((root / ".github").glob("*.md"))]:
        check_links(root, path)
    require(len((root / "AGENTS.md").read_text().splitlines()) <= 100, "AGENTS must remain a short index")
    for path in (root / "docs").rglob("*.json"):
        read_json(path)
    lock = read_json(root / "docs/sources.lock.json")["analysis_ir"]
    require(lock["repository"] == "Gustavo2358/analysis-ir" and lock["role"] == "normative"
            and full_sha(lock["ref"]), "Invalid normative authority lock")
    for path in lock["normative_sections"]:
        relative_path(root, path, exists=False)
    sources = read_json(root / "docs/sources/harness-baseline.json")
    require(len(sources["repositories"]) == 5, "Expected five baseline repositories")
    require(len({s["repository"] for s in sources["repositories"]}) == 5, "Duplicate baseline repository")
    for source in sources["repositories"]:
        require(full_sha(source["commit"]), "Baseline must use full SHA")
    require(re.fullmatch(r"[0-9a-f]{64}", sources["roadmap"]["sha256"]), "Invalid roadmap digest")
    invariants = catalog(root, "docs/architecture/invariants.json", r"INV-AIR-\d{3}")
    evals = catalog(root, "docs/evals/catalog.json", r"EVAL-AIR-\d{3}")
    for entry in evals.values():
        require(set(entry["invariants"]) <= invariants.keys(), "Unknown invariant in eval")
        require(entry["gate"] in GATES, "Unknown eval gate")
        require(entry["status"] in {"implemented", "planned"}, "Unknown eval status")
    gate_state = read_json(root / "docs/engineering/gates.json")
    require(set(gate_state) == GATES, "Gate registry differs from executors")
    for name, status in gate_state.items():
        expected = "unavailable" if name in {"performance", "integration"} else "implemented"
        require(status == expected, f"Gate state differs from implementation: {name}")
    for entry in evals.values():
        require(entry["status"] != "implemented" or gate_state[entry["gate"]] == "implemented",
                "Implemented eval points to unavailable gate")
    contracts = read_json(root / "docs/evals/contract-checks.json")
    require(full_sha(contracts["baseline_commit"]), "Contract inventory baseline must be pinned")
    names = contracts["checks"]
    require(names and all(isinstance(n, str) and n for n in names) and len(names) == len(set(names)),
            "Invalid contract inventory")
    registry = read_json(root / "docs/work/registry.json")
    require(set(registry) == {"active", "history"}, "Invalid work registry fields")
    require(isinstance(registry["active"], list) and isinstance(registry["history"], list), "Invalid lifecycle lists")
    ids = [entry["id"] for entry in registry["active"] + registry["history"]]
    require(len(ids) == len(set(ids)), "Duplicate work ID across lifecycle")
    active_root = root / "docs/work/active"
    directories = {p.name for p in active_root.iterdir() if p.is_dir()}
    require(directories == {e["id"] for e in registry["active"]}, "Active directory/registry mismatch")
    history_files = {p.stem for p in (root / "docs/work/history").glob("WORK-*.md")}
    require(history_files == {e["id"] for e in registry["history"]}, "History/registry mismatch")
    index = (root / "docs/work/index.md").read_text(encoding="utf-8")
    for entry in registry["active"]:
        directory = active_root / entry["id"]
        require({p.name for p in directory.iterdir()} == WORK_FILES, "Active item must contain exactly five files")
        item = read_json(directory / "work-item.json")
        validate_work(root, item, invariants, evals)
        require(item["id"] == entry["id"] and item["status"] == entry["status"], "Work registry/status mismatch")
        require(f"active/{item['id']}/work-item.json" in index, "Active item missing from index")
        require(re.search(r"\|\s*\[" + re.escape(item["id"]) + r"\]\([^\n]+?\)\s*\|\s*"
                          + re.escape(item["status"]) + r"\s*\|", index), "Work index/status mismatch")
        headings = set(re.findall(r"^## (.+)$", (directory / "state.md").read_text(), re.M))
        require(headings == STATE_HEADINGS, "State must contain the four documented headings")
    for entry in registry["history"]:
        require(entry["status"] == "completed" and full_sha(entry["merge_commit"]), "Unconfirmed completed work")
        require(f"history/{entry['id']}.md" in index, "Historical item missing from index")
    return f"knowledge links, {len(invariants)} invariants, {len(evals)} evals and work lifecycle checked offline"
