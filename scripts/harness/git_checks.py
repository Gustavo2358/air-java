"""Read-only scope/branch checks. Fetch and remote PR checks remain explicit actions."""
import json

from common import full_sha, git, matches_scope, read_json, relative_path, require, run, unique_keys


def check_scope(paths, item):
    allowed = item["change_scope"]
    protected = item["must_not_change"]
    for path in paths:
        require(not any(matches_scope(path, s) for s in protected), f"Protected path changed: {path}")
        require(any(matches_scope(path, s) for s in allowed), f"Out-of-scope change: {path}")


def changed_paths(root, base):
    # Include committed, staged, unstaged and untracked changes, including both rename sides.
    output = run(["git", "diff", "--no-renames", "--name-only", "-z", base, "--"], root)
    untracked = run(["git", "ls-files", "--others", "--exclude-standard", "-z"], root)
    return sorted(set(p for p in (output + untracked).split("\0") if p))


def check(root, work_id, *, scope_only=False):
    require(work_id and work_id.startswith("WORK-AIR-"), "Explicit --work WORK-AIR-* required")
    path = relative_path(root, f"docs/work/active/{work_id}/work-item.json")
    item = read_json(path)
    require(item["id"] == work_id, "Work item identity mismatch")
    base = item["base_commit"]
    require(full_sha(base), "Full base_commit required")
    git(root, "merge-base", "--is-ancestor", base, "HEAD")
    if not scope_only:
        branch = git(root, "branch", "--show-current")
        require(branch and branch not in {"main", "master"}, "Dedicated work branch required")
        require(branch == item["branch"], f"Unexpected work branch: {branch}")
        remote = git(root, "remote", "get-url", "origin")
        require(remote in {"git@github.com:Gustavo2358/air-java.git",
                           "https://github.com/Gustavo2358/air-java.git"}, "Unexpected origin")
        git(root, "merge-base", "--is-ancestor", "origin/main", "HEAD")
    paths = changed_paths(root, base)
    check_scope(paths, item)
    run(["git", "diff", "--check", base, "--"], root)
    return f"{len(paths)} changed paths inside {work_id}; base {base}; no remote claim"


def check_ci(root, event_base):
    require(full_sha(event_base) and event_base != "0" * 40, "CI requires a concrete event base")
    base = git(root, "merge-base", event_base, "HEAD")
    registry = read_json(root / "docs/work/registry.json")
    active = registry["active"]
    require(len(active) <= 1, "CI requires a single explicit active work item")
    if active:
        identifier = active[0]["id"]
        result = check(root, identifier, scope_only=True)
        item = read_json(relative_path(root, f"docs/work/active/{identifier}/work-item.json"))
        # Also inspect the event diff: moving manifest base to HEAD cannot hide changes.
        check_scope(changed_paths(root, base), item)
        return result + "; event diff checked"
    previous = json.loads(git(root, "show", f"{base}:docs/work/registry.json"), object_pairs_hook=unique_keys)
    previous_ids = {entry["id"] for entry in previous["active"]}
    require(previous_ids, "No active work: only explicit closure may omit an active item")
    history = {entry["id"]: entry for entry in registry["history"]}
    require(previous_ids <= history.keys(), "Closure must archive every previous active item")
    for identifier in previous_ids:
        merge = history[identifier]["merge_commit"]
        require(full_sha(merge), "Closure requires a full merge SHA")
        git(root, "merge-base", "--is-ancestor", merge, "HEAD")
    paths = changed_paths(root, base)
    check_scope(paths, {"change_scope": ["docs/", "AGENTS.md", "MANIFEST.sha256"],
                        "must_not_change": ["docs/sources.lock.json"]})
    return f"{len(paths)} documentation-only closure paths; merge ancestry checked, not human approval"
