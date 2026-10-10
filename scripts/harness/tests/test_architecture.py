import struct
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import Failure, run
from architecture import inspect_classes, inspect_dependencies, inspect_runtime_methods


class BytecodeTests(unittest.TestCase):
    def compile(self, files):
        temp = tempfile.TemporaryDirectory(prefix="air-harness-bytecode-test-")
        self.addCleanup(temp.cleanup)
        root = Path(temp.name)
        sources = []
        for name, text in files.items():
            path = root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text)
            sources.append(str(path))
        classes = root / "classes"
        classes.mkdir()
        run(["javac", "--release", "21", "-d", str(classes), *sources], root)
        return classes

    def inspect(self, classes):
        inspect_classes(classes)
        return inspect_dependencies(run(["jdeps", "--multi-release", "21", "-verbose:class", "-filter:none", str(classes)]))

    def test_values_records_lambdas_and_validation_to_model_are_allowed(self):
        classes = self.compile({
            "Value.java": "package io.github.gustavo2358.air.model; public record Value(java.math.BigInteger n) {}",
            "Check.java": "package io.github.gustavo2358.air.validation; public class Check { public java.util.function.Supplier<io.github.gustavo2358.air.model.Value> x() { return () -> new io.github.gustavo2358.air.model.Value(java.math.BigInteger.ONE); } }"})
        self.assertGreater(self.inspect(classes), 0)

    def test_inverse_dependency_is_rejected_from_compiled_descriptor(self):
        classes = self.compile({
            "Value.java": "package io.github.gustavo2358.air.model; public class Value { public io.github.gustavo2358.air.validation.Check field; }",
            "Check.java": "package io.github.gustavo2358.air.validation; public class Check {}"})
        with self.assertRaisesRegex(Failure, "model -> validation forbidden"):
            self.inspect(classes)

    def test_io_network_reflection_process_and_json_are_rejected(self):
        targets = ["java.io.File", "java.nio.file.Path", "java.net.URI", "java.lang.ProcessBuilder",
                   "java.lang.reflect.Method", "java.util.ServiceLoader<?>" ]
        for target in targets:
            with self.subTest(target=target):
                classes = self.compile({"Value.java": f"package io.github.gustavo2358.air.model; public class Value {{ public {target} field; }}"})
                with self.assertRaisesRegex(Failure, "Forbidden dependency"):
                    self.inspect(classes)

    def test_json_dependency_is_rejected_even_if_missing_at_inspection(self):
        classes = self.compile({
            "Value.java": "package io.github.gustavo2358.air.model; public class Value { public com.fasterxml.jackson.databind.ObjectMapper field; }",
            "ObjectMapper.java": "package com.fasterxml.jackson.databind; public class ObjectMapper {}"})
        (classes / "com/fasterxml/jackson/databind/ObjectMapper.class").unlink()
        with self.assertRaisesRegex(Failure, "Unresolved dependency"):
            self.inspect(classes)

    def test_foreign_class_is_rejected(self):
        classes = self.compile({"Codec.java": "package io.github.gustavo2358.air.json; public class Codec {}"})
        with self.assertRaisesRegex(Failure, "Unexpected production class"):
            self.inspect(classes)

    def test_nested_model_dependency_on_validation_is_rejected(self):
        classes = self.compile({
            'Value.java': 'package io.github.gustavo2358.air.model; public class Value { public static class Nested { public io.github.gustavo2358.air.validation.Check field; } }',
            'Check.java': 'package io.github.gustavo2358.air.validation; public class Check {}'})
        with self.assertRaisesRegex(Failure, 'model -> validation forbidden'):
            self.inspect(classes)

    def test_model_cannot_depend_on_json_module_or_gson(self):
        for package, name in [('io.github.gustavo2358.air.json', 'Codec'), ('com.google.gson', 'Gson')]:
            classes = self.compile({
                'Value.java': f'package io.github.gustavo2358.air.model; public class Value {{ public {package}.{name} field; }}',
                name + '.java': f'package {package}; public class {name} {{}}'})
            (classes / (package.replace('.', '/') + '/' + name + '.class')).unlink()
            with self.subTest(package=package), self.assertRaisesRegex(Failure, 'Unresolved dependency'):
                self.inspect(classes)

    def test_codec_scheduler_dependencies_are_scoped_to_their_owners(self):
        source = "io.github.gustavo2358.air.json."
        def edge(owner, target):
            return f"   {source}{owner} -> {target} java.base"
        self.assertEqual(1, inspect_dependencies(edge("AirJson$DecodeOptions", "java.lang.Runtime"), "air-json"))
        for name in ("Callable", "ExecutionException", "ExecutorService", "Executors", "Future", "ThreadFactory"):
            self.assertEqual(1, inspect_dependencies(edge("OrderedBlocks", "java.util.concurrent." + name), "air-json"))
        for owner, target in (("BindingReader", "java.lang.Runtime"), ("OrderedBlocks", "java.lang.Runtime"),
                              ("AirJson$DecodeOptions", "java.util.concurrent.Executors"),
                              ("AirJson$DecodeOptions", "java.lang.ProcessBuilder"),
                              ("OrderedBlocks", "java.util.concurrent.ForkJoinPool")):
            with self.subTest(owner=owner, target=target), self.assertRaisesRegex(Failure, "Forbidden dependency"):
                inspect_dependencies(edge(owner, target), "air-json")
        with self.assertRaisesRegex(Failure, "Forbidden dependency"):
            inspect_dependencies("   io.github.gustavo2358.air.model.Value -> java.lang.Runtime java.base")

    def test_codec_input_stream_is_confined_to_exact_transport_owners(self):
        prefix = "io.github.gustavo2358.air.json."
        for owner in ("AirJson", "PagedJson"):
            for target in ("java.io.InputStream", "java.io.IOException"):
                self.assertEqual(1, inspect_dependencies(f"   {prefix}{owner} -> {target} java.base", "air-json"))
            for target in ("java.io.FileInputStream", "java.nio.file.Path", "java.net.Socket"):
                with self.subTest(owner=owner, target=target), self.assertRaisesRegex(Failure, "Forbidden dependency"):
                    inspect_dependencies(f"   {prefix}{owner} -> {target} java.base", "air-json")
        for source in (prefix + "BindingReader", "io.github.gustavo2358.air.model.Value"):
            with self.subTest(source=source), self.assertRaisesRegex(Failure, "Forbidden dependency"):
                inspect_dependencies(f"   {source} -> java.io.InputStream java.base", "air-json" if source.startswith(prefix) else "air-model")

    def test_runtime_cpu_query_is_allowed_but_process_and_other_runtime_methods_are_not(self):
        for body, allowed in (("return Runtime.getRuntime().availableProcessors();", True),
                              ("return (int) Runtime.getRuntime().freeMemory();", False),
                              ('Runtime.getRuntime().exec("example"); return 1;', False),
                              ("java.util.function.IntConsumer stop=Runtime.getRuntime()::exit; return 1;", False)):
            with self.subTest(body=body):
                classes = self.compile({"AirJson.java":
                    "package io.github.gustavo2358.air.json; public class AirJson { public static class DecodeOptions { public int workers() throws Exception { " + body + " } } }"})
                output = run(["javap", "-classpath", str(classes), "-verbose", "-p",
                              "io.github.gustavo2358.air.json.AirJson$DecodeOptions"])
                if allowed:
                    inspect_runtime_methods(output)
                else:
                    with self.assertRaisesRegex(Failure, "Runtime access is limited"):
                        inspect_runtime_methods(output)

    def test_wrong_major_or_preview_is_rejected(self):
        classes = self.compile({"Value.java": "package io.github.gustavo2358.air.model; public class Value {}"})
        path = next(classes.rglob("*.class"))
        original = path.read_bytes()
        for minor, major in [(0, 66), (65535, 65)]:
            with self.subTest(minor=minor, major=major):
                path.write_bytes(struct.pack(">IHH", 0xCAFEBABE, minor, major) + original[8:])
                with self.assertRaisesRegex(Failure, "Expected Java 21"):
                    inspect_classes(classes)
        path.write_bytes(original)
        self.assertEqual(1, inspect_classes(classes))

    def test_empty_or_unparseable_output_fails_closed(self):
        for output in ("", "classes -> java.base\n", "warning: no classes"):
            with self.subTest(output=output), self.assertRaisesRegex(Failure, "No class dependencies"):
                inspect_dependencies(output)


if __name__ == "__main__":
    unittest.main()
