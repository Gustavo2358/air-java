import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import Failure, git
from git_checks import changed_paths, check, check_scope, check_ci


class GitScopeTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="air-harness-git-test-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        git(self.root, "init", "-b", "main")
        git(self.root, "config", "user.email", "fixture@example.invalid")
        git(self.root, "config", "user.name", "Harness Fixture")
        git(self.root, "config", "commit.gpgsign", "false")
        (self.root / "src").mkdir()
        (self.root / "src/Value.java").write_text("baseline")
        (self.root / "docs").mkdir()
        (self.root / "docs/guide.md").write_text("baseline")
        git(self.root, "add", ".")
        git(self.root, "commit", "-m", "fixture baseline")
        self.base = git(self.root, "rev-parse", "HEAD")
        git(self.root, "update-ref", "refs/remotes/origin/main", self.base)
        git(self.root, "remote", "add", "origin", "git@github.com:Gustavo2358/air-java.git")
        git(self.root, "switch", "-c", "chore/fixture")
        self.item = {"id": "WORK-AIR-TEST-001", "base_commit": self.base, "branch": "chore/fixture",
                     "change_scope": ["docs/"], "must_not_change": ["src/"]}
        directory = self.root / "docs/work/active/WORK-AIR-TEST-001"
        directory.mkdir(parents=True)
        (directory / "work-item.json").write_text(json.dumps(self.item))
        (self.root / "docs/work/registry.json").write_text(json.dumps({
            "active": [{"id": self.item["id"], "status": "active"}], "history": []}))

    def test_clean_scope_and_expected_branch(self):
        self.assertIn("inside WORK-AIR-TEST-001", check(self.root, self.item["id"]))

    def test_product_change_unstaged_staged_and_committed(self):
        (self.root / "src/Value.java").write_text("modified")
        for state in ("unstaged", "staged", "committed"):
            if state == "staged":
                git(self.root, "add", "src/Value.java")
            if state == "committed":
                git(self.root, "commit", "-m", "fixture forbidden change")
            with self.subTest(state=state), self.assertRaisesRegex(Failure, "Protected path changed"):
                check_scope(changed_paths(self.root, self.base), self.item)

    def test_untracked_outside_scope(self):
        (self.root / "surprise.txt").write_text("untracked")
        with self.assertRaisesRegex(Failure, "Out-of-scope change"):
            check(self.root, self.item["id"])

    def test_rename_cannot_hide_protected_source(self):
        git(self.root, "mv", "src/Value.java", "docs/Value.java")
        paths = changed_paths(self.root, self.base)
        self.assertIn("src/Value.java", paths)
        self.assertIn("docs/Value.java", paths)
        with self.assertRaisesRegex(Failure, "Protected path changed"):
            check_scope(paths, self.item)

    def test_similar_directory_name_is_outside_scope(self):
        with self.assertRaisesRegex(Failure, "Out-of-scope"):
            check_scope(["docs-escape/file"], self.item)

    def test_main_and_detached_are_not_work_branches(self):
        git(self.root, "switch", "main")
        with self.assertRaisesRegex(Failure, "Dedicated work branch"):
            check(self.root, self.item["id"])
        git(self.root, "checkout", "--detach", self.base)
        with self.assertRaisesRegex(Failure, "Dedicated work branch"):
            check(self.root, self.item["id"])
        self.assertIn("inside", check(self.root, self.item["id"], scope_only=True))

    def test_new_origin_main_must_be_integrated_before_local_handoff(self):
        git(self.root, "switch", "main")
        (self.root / "docs/guide.md").write_text("upstream update")
        git(self.root, "add", "docs/guide.md")
        git(self.root, "commit", "-m", "fixture upstream update")
        git(self.root, "update-ref", "refs/remotes/origin/main", "HEAD")
        git(self.root, "switch", "chore/fixture")
        with self.assertRaisesRegex(Failure, "Command failed"):
            check(self.root, self.item["id"])

    def test_ci_event_diff_prevents_moving_base_to_hide_changes(self):
        (self.root / "src/Value.java").write_text("hidden product change")
        git(self.root, "add", ".")
        git(self.root, "commit", "-m", "fixture forbidden change")
        self.item["base_commit"] = git(self.root, "rev-parse", "HEAD")
        (self.root / "docs/work/active/WORK-AIR-TEST-001/work-item.json").write_text(json.dumps(self.item))
        with self.assertRaisesRegex(Failure, "Protected path changed"):
            check_ci(self.root, self.base)

    def prepare_closure(self):
        git(self.root, "add", ".")
        git(self.root, "commit", "-m", "fixture work accepted")
        self.closure_base = git(self.root, "rev-parse", "HEAD")
        (self.root / "docs/work/registry.json").write_text(json.dumps({
            "active": [], "history": [{"id": self.item["id"], "status": "completed",
                                        "merge_commit": self.closure_base}]}))

    def test_ci_allows_documentary_closure_without_permanent_active_item(self):
        self.prepare_closure()
        self.assertIn("documentation-only closure", check_ci(self.root, self.closure_base))

    def test_ci_closure_cannot_hide_product_or_source_lock_changes(self):
        self.prepare_closure()
        for path in ("src/Value.java", "docs/sources.lock.json"):
            with self.subTest(path=path):
                file = self.root / path
                existed = file.exists()
                old = file.read_bytes() if existed else None
                file.write_text("forbidden closure change")
                with self.assertRaises(Failure):
                    check_ci(self.root, self.closure_base)
                if existed:
                    file.write_bytes(old)
                else:
                    file.unlink()

    def test_ci_missing_work_is_not_blanket_authorization(self):
        self.prepare_closure()
        (self.root / "docs/work/registry.json").write_text('{"active":[],"history":[]}')
        with self.assertRaisesRegex(Failure, "must archive"):
            check_ci(self.root, self.closure_base)


if __name__ == "__main__":
    unittest.main()
