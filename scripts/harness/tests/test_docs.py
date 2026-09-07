import json
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import ROOT, Failure, read_json, relative_path
import docs


class DocumentationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="air-harness-docs-test-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name) / "repo"
        self.root.mkdir()
        # Independent local fixture, not the active production work item. Tests must
        # survive its archival and future roadmap work without reviving old authority.
        for path in ("AGENTS.md", "README.md", "ARCHITECTURE.md", "docs/index.md",
                     "docs/architecture/invariants.md", "docs/evals/index.md"):
            file = self.root / path
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_text("# Índice de conhecimento\n")
        for path in ("docs/sources.lock.json", "docs/sources/harness-baseline.json",
                     "docs/evals/catalog.json", "docs/evals/contract-checks.json",
                     "docs/architecture/invariants.json", "docs/engineering/gates.json",
                     "docs/templates/work-item/work-item.json.template"):
            file = self.root / path
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_bytes((ROOT / path).read_bytes())
        self.work_id = "WORK-AIR-HARNESS-001"
        directory = self.root / "docs/work/active" / self.work_id
        directory.mkdir(parents=True)
        item = read_json(ROOT / "docs/templates/work-item/work-item.json.template")
        item.update(id=self.work_id, title="Fixture", authorization="harness", status="active",
                    authorization_evidence="Explicit fixture instruction", base_commit="a" * 40,
                    branch="chore/fixture", change_scope=["docs/"], gates=["full", "git"])
        (directory / "work-item.json").write_text(json.dumps(item))
        for name in ("spec.md", "plan.md", "eval.md"):
            (directory / name).write_text("# Fixture\n")
        (directory / "state.md").write_text("# Estado\n\n" + "\n\n".join("## " + h for h in docs.STATE_HEADINGS))
        (self.root / "docs/work/history").mkdir()
        (self.root / "docs/work/registry.json").write_text(json.dumps({"active": [{"id": self.work_id, "status": "active"}], "history": []}))
        (self.root / "docs/work/index.md").write_text(f"# Trabalho\n\n| [{self.work_id}](active/{self.work_id}/work-item.json) | active | fixture |\n")

    def change_json(self, path, edit):
        file = self.root / path
        data = read_json(file)
        edit(data)
        file.write_text(json.dumps(data))

    def rejects(self, message):
        with self.assertRaisesRegex(Failure, message):
            docs.check(self.root)

    def test_valid_documentation(self):
        self.assertIn("checked offline", docs.check(self.root))

    def test_actual_repository_documentation(self):
        self.assertIn("checked offline", docs.check(ROOT))

    def test_broken_link(self):
        with (self.root / "docs/index.md").open("a") as output:
            output.write("\n[missing](missing.md)\n")
        self.rejects("Broken/escaping link")

    def test_missing_anchor(self):
        with (self.root / "docs/index.md").open("a") as output:
            output.write("\n[bad anchor](index.md#does-not-exist)\n")
        self.rejects("Missing anchor")

    def test_valid_anchor_and_fenced_example(self):
        with (self.root / "docs/index.md").open("a") as output:
            output.write("\n[local](index.md#índice-de-conhecimento)\n```md\n[example](missing.md)\n```\n")
        docs.check(self.root)

    def test_duplicate_json_key(self):
        (self.root / "docs/work/registry.json").write_text('{"active":[],"active":[],"history":[]}')
        self.rejects("Duplicate JSON key")

    def test_unknown_invariant_in_eval(self):
        self.change_json("docs/evals/catalog.json", lambda data: data[0]["invariants"].append("INV-AIR-999"))
        self.rejects("Unknown invariant in eval")

    def test_duplicate_catalog_id(self):
        self.change_json("docs/evals/catalog.json", lambda data: data.append(data[0]))
        self.rejects("Invalid/duplicate ID")

    def test_unavailable_gate_cannot_claim_implementation(self):
        self.change_json("docs/engineering/gates.json", lambda data: data.update(transport="implemented"))
        self.rejects("Gate state differs")

    def test_completed_cannot_remain_active(self):
        self.change_json("docs/work/active/WORK-AIR-HARNESS-001/work-item.json", lambda data: data.update(status="completed"))
        self.rejects("Completed/invalid item")

    def test_registry_status_must_match(self):
        self.change_json("docs/work/registry.json", lambda data: data["active"][0].update(status="blocked"))
        self.rejects("registry/status mismatch")

    def test_orphan_active_directory(self):
        (self.root / "docs/work/active/WORK-AIR-ORPHAN-001").mkdir()
        self.rejects("directory/registry mismatch")

    def test_orphan_history(self):
        (self.root / "docs/work/history/WORK-AIR-ORPHAN-001.md").write_text("# Historical record\n")
        self.rejects("History/registry mismatch")

    def test_missing_must_read(self):
        self.change_json("docs/work/active/WORK-AIR-HARNESS-001/work-item.json", lambda data: data["must_read"].append("docs/absent.md"))
        self.rejects("Missing path")

    def test_harness_cannot_authorize_product(self):
        self.change_json("docs/work/active/WORK-AIR-HARNESS-001/work-item.json", lambda data: data["change_scope"].append("src/main/"))
        self.rejects("cannot authorize product")

    def test_discovery_cannot_authorize_new_module_paths(self):
        for scope in ('air-model/', 'air-json/'):
            self.change_json('docs/work/active/WORK-AIR-HARNESS-001/work-item.json',
                             lambda data: data.update(authorization='discovery', change_scope=[scope]))
            with self.subTest(scope=scope):
                self.rejects('cannot authorize product')

    def test_new_scope_is_allowed_but_absolute_and_parent_paths_are_rejected(self):
        relative_path(self.root, "docs/new/file.md", exists=False)
        for value in ("/tmp/escape", "../escape", "docs/../../escape"):
            with self.subTest(value=value), self.assertRaises(Failure):
                relative_path(self.root, value, exists=False)

    def test_symlink_escape(self):
        (self.root / "docs/escape").symlink_to(Path(self.temp.name))
        with self.assertRaisesRegex(Failure, "escapes repository"):
            relative_path(self.root, "docs/escape", exists=False)

    def test_blank_authorization_evidence(self):
        self.change_json("docs/work/active/WORK-AIR-HARNESS-001/work-item.json", lambda data: data.update(authorization_evidence=" "))
        self.rejects("Empty work field")

    def test_unexpected_work_field(self):
        self.change_json("docs/work/active/WORK-AIR-HARNESS-001/work-item.json", lambda data: data.update(auto_approve=True))
        self.rejects("fields differ")

    def test_template_is_not_valid_authorization(self):
        item = read_json(self.root / "docs/templates/work-item/work-item.json.template")
        with self.assertRaisesRegex(Failure, "full SHA"):
            docs.validate_work(self.root, item, {"INV-AIR-001": {}}, {"EVAL-AIR-001": {}})

    def test_unknown_work_gate(self):
        self.change_json("docs/work/active/WORK-AIR-HARNESS-001/work-item.json", lambda data: data["gates"].append("pretend-pass"))
        self.rejects("Unknown/missing work gate")

    def test_source_lock_requires_immutable_ref(self):
        self.change_json("docs/sources.lock.json", lambda data: data["analysis_ir"].update(ref="main"))
        self.rejects("Invalid normative authority lock")

    def test_index_must_route_current_work(self):
        index = self.root / "docs/work/index.md"
        index.write_text("# Work\n")
        self.rejects("missing from index")

    def test_index_status_cannot_be_stale(self):
        path = self.root / "docs/work/index.md"
        path.write_text(path.read_text().replace("| active |", "| blocked |"))
        self.rejects("index/status mismatch")

    def test_completed_item_can_be_archived_without_active_directory(self):
        shutil.rmtree(self.root / "docs/work/active" / self.work_id)
        (self.root / f"docs/work/history/{self.work_id}.md").write_text("# Confirmed closure\n")
        (self.root / "docs/work/registry.json").write_text(json.dumps({
            "active": [], "history": [{"id": self.work_id, "status": "completed", "merge_commit": "b" * 40}]}))
        (self.root / "docs/work/index.md").write_text(f"# History\n\n[{self.work_id}](history/{self.work_id}.md)\n")
        self.assertIn("checked offline", docs.check(self.root))

    def test_same_work_id_cannot_be_active_and_historical(self):
        self.change_json("docs/work/registry.json", lambda data: data["history"].append({
            "id": self.work_id, "status": "completed", "merge_commit": "b" * 40}))
        self.rejects("Duplicate work ID")


if __name__ == "__main__":
    unittest.main()
