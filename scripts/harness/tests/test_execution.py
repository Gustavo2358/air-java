import contextlib
import io
import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import Failure, run
from contracts import inspect_output
import run as runner


class ContractOutputTests(unittest.TestCase):
    def test_real_output_shape(self):
        self.assertEqual(2, inspect_output("ok 1 - first\nok 2 - second\nPASS: 2 deterministic contract checks\n", ["first", "second"]))

    def test_missing_duplicate_reordered_or_unexecuted_checks_fail(self):
        cases = ["", "BUILD SUCCESS", "PASS: 2 deterministic contract checks",
                 "ok 1 - first\nPASS: 1 deterministic contract checks",
                 "ok 1 - first\nok 2 - first\nPASS: 2 deterministic contract checks",
                 "ok 1 - second\nok 2 - first\nPASS: 2 deterministic contract checks",
                 "ok 1 - first\nok 3 - second\nPASS: 2 deterministic contract checks",
                 "ok 1 - first\nok 2 - second\nPASS: 0 deterministic contract checks",
                 "ok 1 - first\nok 2 - second\nPASS: 2 deterministic contract checks\nPASS: 2 deterministic contract checks",
                 "ok 1 - first\nok 2 - second\nPASS: 2 deterministic contract checks\nFAIL - error"]
        for output in cases:
            with self.subTest(output=output), self.assertRaises(Failure):
                inspect_output(output, ["first", "second"])

    def test_empty_expected_inventory_fails(self):
        with self.assertRaisesRegex(Failure, "Empty/duplicate"):
            inspect_output("PASS: 0 deterministic contract checks", [])

    def test_child_failure_is_not_success(self):
        with self.assertRaisesRegex(Failure, "Command failed \\(7\\)"):
            run([sys.executable, "-c", "raise SystemExit(7)"])


class RunnerTests(unittest.TestCase):
    def test_empty_or_skipped_harness_cannot_pass(self):
        for output in ("Ran 0 tests in 0s\n\nOK", "Ran 1 test in 0s\n\nOK (skipped=1)"):
            with self.subTest(output=output), patch.object(runner, "run", return_value=output), \
                    self.assertRaisesRegex(Failure, "Harness suite missing, empty or skipped"):
                runner.execute("harness", runner.ROOT, None)

    def test_missing_harness_test_file_cannot_pass(self):
        with tempfile.TemporaryDirectory() as temp, self.assertRaisesRegex(Failure, "Missing harness suite"):
            runner.execute("harness", Path(temp), None)

    def invoke(self, gate, effect):
        with tempfile.TemporaryDirectory() as temp, patch.object(runner, "ROOT", Path(temp)), \
                patch.object(runner, "git", return_value="a" * 40), \
                patch.object(runner, "execute", side_effect=effect) as execute, \
                patch.object(sys, "argv", ["run.py", gate]), contextlib.redirect_stdout(io.StringIO()):
            code = runner.main()
            report = json.loads((Path(temp) / f"target/harness/{gate}.json").read_text())
            return code, report, [call.args[0] for call in execute.call_args_list]

    def test_full_stops_and_records_unexecuted_after_failure(self):
        code, report, called = self.invoke("full", [Failure("bad docs")])
        self.assertEqual(1, code)
        self.assertEqual(["docs"], called)
        self.assertEqual(["harness", "architecture", "semantic", "maven"], report["unexecuted"])

    def test_full_runs_each_required_gate_once(self):
        code, report, called = self.invoke("full", ["done"] * 5)
        self.assertEqual(0, code)
        self.assertEqual(["docs", "harness", "architecture", "semantic", "maven"], called)
        self.assertEqual([], report["unexecuted"])

    def test_missing_tool_reports_error(self):
        code, report, _ = self.invoke("full", [FileNotFoundError("javac")])
        self.assertEqual(2, code)
        self.assertEqual("ERROR", report["results"][0]["status"])

    def test_future_gates_are_unavailable(self):
        for gate in ("transport", "performance", "integration"):
            with self.subTest(gate=gate):
                code, report, called = self.invoke(gate, [])
                self.assertEqual(3, code)
                self.assertEqual([], called)
                self.assertEqual("UNAVAILABLE", report["results"][0]["status"])


if __name__ == "__main__":
    unittest.main()
