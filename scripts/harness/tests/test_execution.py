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
