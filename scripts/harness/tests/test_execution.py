import contextlib
import io
import json
import re
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import Failure, run
from contracts import inspect_output, inspect_reactor_output
import run as runner


class ContractOutputTests(unittest.TestCase):
    def test_reactor_cannot_pass_with_missing_duplicated_or_reordered_modules(self):
        root = 'PASS: reactor topology 0.1.0-SNAPSHOT\n'
        model = 'PASS: compiled module air-model; checked\n'
        codec = 'PASS: compiled module air-json; codec\n'
        inspect_reactor_output(root + model + codec)
        for output in ('BUILD SUCCESS', root + model, root + codec, root + codec + model,
                       root + model + codec + codec, root + root + model + codec):
            with self.subTest(output=output), self.assertRaises(Failure):
                inspect_reactor_output(output)

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
        for gate in ("performance", "integration"):
            with self.subTest(gate=gate):
                code, report, called = self.invoke(gate, [])
                self.assertEqual(3, code)
                self.assertEqual([], called)
                self.assertEqual("UNAVAILABLE", report["results"][0]["status"])


class WorkflowOrderTests(unittest.TestCase):
    def assert_scope_before_full(self, workflow):
        # Deliberately cover the two unconditional steps, without a YAML dependency.
        steps = re.split(r"(?m)^      - ", workflow)[1:]
        gates = [step.rstrip() for step in steps if "scripts/harness/run.py" in step]
        self.assertEqual([
            "name: Check active work scope\n        run: python3 -B scripts/harness/run.py ci-scope",
            "name: Full library harness\n        run: python3 -B scripts/harness/run.py full",
        ], gates, "CI must authorize scope before unconditional full")
        self.assertIn("permissions:\n  contents: read\njobs:", workflow)

    def test_ci_authorizes_scope_before_full(self):
        self.assert_scope_before_full((runner.ROOT / ".github/workflows/harness.yml").read_text())

    def test_reversed_missing_or_optional_gate_is_rejected(self):
        workflow = (runner.ROOT / ".github/workflows/harness.yml").read_text()
        scope = "      - name: Check active work scope\n        run: python3 -B scripts/harness/run.py ci-scope\n"
        full = "      - name: Full library harness\n        run: python3 -B scripts/harness/run.py full\n"
        mutations = [workflow.replace(scope + full, full + scope),
                     workflow.replace(scope, ""), workflow.replace(full, ""),
                     workflow.replace(scope, scope + "        continue-on-error: true\n"),
                     workflow.replace(full, full + "        if: false\n")]
        for mutation in mutations:
            with self.subTest(workflow=mutation), self.assertRaises(AssertionError):
                self.assert_scope_before_full(mutation)



class TransportOutputTests(unittest.TestCase):
    def test_json_suite_output_must_execute_exactly_once_in_order(self):
        from contracts import inspect_transport_output
        good = 'json-ok 1 - encode\njson-ok 2 - decode\nPASS: 2 deterministic transport checks\n'
        self.assertEqual(2, inspect_transport_output(good, ['encode', 'decode']))
        for bad in ('', 'BUILD SUCCESS', good + good, good.replace('json-ok 2 - decode\n', ''),
                    good.replace('json-ok 1 - encode', 'json-ok 1 - decode'),
                    good.replace('json-ok 2', 'json-ok 3'), good.replace('PASS: 2', 'PASS: 0')):
            with self.subTest(output=bad), self.assertRaises(Failure):
                inspect_transport_output(bad, ['encode', 'decode'])

    def test_transport_has_an_executor_and_is_available(self):
        self.assertNotIn('transport', runner.UNAVAILABLE)
        with patch.object(runner.architecture, 'transport', return_value='actual suite') as call:
            self.assertEqual('actual suite', runner.execute('transport', runner.ROOT, None))
            call.assert_called_once_with(runner.ROOT)


if __name__ == "__main__":
    unittest.main()
