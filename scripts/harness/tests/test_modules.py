"""Topology/graph falsifications use disposable trees, never the working product."""
import shutil
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import ROOT, Failure
import architecture
from module_policy import GROUP, SUITE, JSON_SUITE, inspect_graph, inspect_effective


class ModuleTests(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory(prefix="air-module-test-")
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        for name in ("pom.xml", "air-model/pom.xml", "air-json/pom.xml"):
            dest = self.root / name
            dest.parent.mkdir(parents=True, exist_ok=True)
            if (ROOT / name).exists():
                shutil.copyfile(ROOT / name, dest)
        src = self.root / "air-model/src/main/java/Value.java"
        src.parent.mkdir(parents=True)
        src.write_text("package io.github.gustavo2358.air.model; public class Value {}")
        for name in ('air-json/src/main/java/io/github/gustavo2358/air/json/AirJson.java',
                     'air-json/src/test/java/io/github/gustavo2358/air/json/CodecSuite.java',
                     'air-json/src/test/java/io/github/gustavo2358/air/json/GobackOracle.java',
                     'air-json/src/test/resources/goback.canonical.json', 'docs/evals/transport-checks.json'):
            target = self.root / name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT / name, target)

    def mutate(self, path, old, new):
        p = self.root / path
        self.assertIn(old, p.read_text())
        p.write_text(p.read_text().replace(old, new))

    def test_authorized_json_requires_implementation_suite_policy_modules_and_edge(self):
        self.assertEqual("0.1.0-SNAPSHOT", architecture.inspect_topology(self.root))

    def test_missing_module(self):
        (self.root / "air-json/pom.xml").unlink()
        with self.assertRaisesRegex(Failure, "Missing module"):
            architecture.inspect_topology(self.root)

    def test_third_or_ignored_module(self):
        for modules in ("<module>air-json</module><module>third</module>", ""):
            with self.subTest(modules=modules):
                p = self.root / "pom.xml"
                original = p.read_text()
                self.mutate("pom.xml", "<module>air-json</module>", modules)
                with self.assertRaisesRegex(Failure, "Expected modules"):
                    architecture.inspect_topology(self.root)
                p.write_text(original)

    def test_unowned_source_root_or_root_classfile(self):
        for name in ("src/main/java/X.java", "third/src/main/java/X.java", "src/test/java/X.java", "target/classes/X.class"):
            with self.subTest(path=name):
                p = self.root / name
                p.parent.mkdir(parents=True, exist_ok=True)
                p.write_bytes(b"unexpected")
                with self.assertRaisesRegex(Failure, "Unowned|Root product"):
                    architecture.inspect_topology(self.root)
                p.unlink()

    def test_json_code_without_ownership_or_suite_policy_is_rejected(self):
        for name in ("src/main/java/Codec.java", "src/test/java/CodecTest.java", "src/main/resources/binding.json"):
            p = self.root / "air-json" / name
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text("first transport content")
            with self.assertRaisesRegex(Failure, "Unowned JSON"):
                architecture.inspect_topology(self.root)
            p.unlink()

    def test_json_implementation_without_suite_policy_or_golden_is_red(self):
        for name in ('air-json/src/test/java/io/github/gustavo2358/air/json/CodecSuite.java',
                     'air-json/src/test/resources/goback.canonical.json', 'docs/evals/transport-checks.json'):
            p = self.root / name
            original = p.read_bytes()
            p.unlink()
            with self.subTest(path=name), self.assertRaisesRegex(Failure, 'Missing JSON suite/policy/evidence'):
                architecture.inspect_topology(self.root)
            p.write_bytes(original)
        architecture.inspect_topology(self.root)

    def test_json_suite_launch_cannot_be_omitted_or_skipped(self):
        p = self.root / 'air-json/pom.xml'
        original = p.read_text()
        for old, new in [('<id>air-json-suite</id>', '<id>ignored</id>'),
                         ('<skip>${skipTests}</skip>', '<skip>true</skip>'),
                         ('<argument>-ea</argument>', '<argument>-da</argument>'),
                         ('<id>compiled-module</id>', '<id>air-json-suite</id>')]:
            p.write_text(original.replace(old, new))
            with self.subTest(mutation=new), self.assertRaises(Failure):
                architecture.inspect_topology(self.root)
        p.write_text(original)
        architecture.inspect_topology(self.root)

    def test_unapproved_external_json_dependency_is_red(self):
        self.mutate('air-json/pom.xml', '</dependencies>',
                    '<dependency><groupId>com.google.code.gson</groupId><artifactId>gson</artifactId>'
                    '<version>2.13.2</version></dependency></dependencies>')
        with self.assertRaisesRegex(Failure, 'exactly one direct compile dependency'):
            architecture.inspect_topology(self.root)

    def test_inverse_runtime_optional_parent_and_cycle_dependencies(self):
        for owner, dependency in (
            ("air-model", '<groupId>com.fasterxml.jackson.core</groupId><artifactId>jackson-databind</artifactId><version>2.22.2</version><scope>runtime</scope>'),
            ("air-model", '<groupId>com.google.code.gson</groupId><artifactId>gson</artifactId><version>2.13.2</version><optional>true</optional>'),
            ("air-model", '<groupId>io.github.gustavo2358</groupId><artifactId>air-json</artifactId><version>0.1.0-SNAPSHOT</version>'),
            ("", '<groupId>com.fasterxml.jackson.core</groupId><artifactId>jackson-databind</artifactId><version>2.22.2</version><scope>runtime</scope>')):
            with self.subTest(owner=owner, dependency=dependency):
                p = self.root / owner / "pom.xml"
                original = p.read_text()
                p.write_text(original.replace("</project>", f"<dependencies><dependency>{dependency}</dependency></dependencies></project>"))
                with self.assertRaisesRegex(Failure, "dependencies forbidden"):
                    architecture.inspect_topology(self.root)
                p.write_text(original)

    def test_json_without_model_edge_is_not_accepted(self):
        self.mutate("air-json/pom.xml", "<artifactId>air-java</artifactId>", "<artifactId>other</artifactId>")
        with self.assertRaisesRegex(Failure, "direct compile dependency"):
            architecture.inspect_topology(self.root)

    def test_profile_cannot_hide_runtime_dependency(self):
        self.mutate("air-model/pom.xml", "</project>", "<profiles><profile><id>leak</id></profile></profiles></project>")
        with self.assertRaisesRegex(Failure, "Unsupported POM"):
            architecture.inspect_topology(self.root)

    def test_suite_missing_duplicated_wrong_cwd_or_assertions_is_rejected(self):
        p = self.root / 'air-model/pom.xml'
        original = p.read_text()
        for old, new in [('<id>air-contract-suite</id>', '<id>ignored</id>'),
                         ('<workingDirectory>${project.basedir}</workingDirectory>',
                          '<workingDirectory>${project.basedir}/..</workingDirectory>'),
                         ('<argument>-ea</argument>', '<argument>-da</argument>'),
                         ('<id>compiled-module</id>', '<id>air-contract-suite</id>')]:
            p.write_text(original.replace(old, new))
            with self.subTest(mutation=new), self.assertRaises(Failure):
                architecture.inspect_topology(self.root)
        p.write_text(original)
        architecture.inspect_topology(self.root)


class ResolvedGraphTests(unittest.TestCase):
    def graph(self, artifact='air-java'):
        return {'groupId': GROUP, 'artifactId': artifact, 'version': '0.1.0-SNAPSHOT', 'type': 'jar', 'children': []}

    def test_real_directions_and_empty_compile_runtime_graph(self):
        inspect_graph(self.graph(), 'air-model', '0.1.0-SNAPSHOT')
        model = self.graph()
        model['scope'] = 'compile'
        codec = self.graph('air-json')
        codec['children'] = [model]
        inspect_graph(codec, 'air-json', '0.1.0-SNAPSHOT')

    def test_unused_runtime_optional_and_test_dependencies_are_rejected(self):
        for scope in ('runtime', 'compile', 'test'):
            for optional in ('true', 'false'):
                tree = self.graph()
                tree['children'] = [{'groupId': 'com.fasterxml.jackson.core', 'artifactId': 'jackson-databind',
                                     'version': '2.22.2', 'type': 'jar', 'scope': scope, 'optional': optional}]
                with self.subTest(scope=scope, optional=optional), self.assertRaisesRegex(Failure, 'resolved dependencies forbidden'):
                    inspect_graph(tree, 'air-model', '0.1.0-SNAPSHOT')

    def test_cycle_transitive_and_wrong_scope_in_json_graph_are_rejected(self):
        for scope, children in [('runtime', []), ('compile', [self.graph('air-json')]),
                                ('compile', [{'artifactId': 'jackson-databind'}])]:
            model = self.graph()
            model.update(scope=scope, children=children)
            tree = self.graph('air-json')
            tree['children'] = [model]
            with self.subTest(scope=scope, children=children), self.assertRaisesRegex(Failure, 'graph, transitive dependency or cycle'):
                inspect_graph(tree, 'air-json', '0.1.0-SNAPSHOT')

    def effective(self, root):
        project = ET.Element('project')
        for name, text in [('groupId', GROUP), ('artifactId', 'air-java'), ('version', '0.1.0-SNAPSHOT'), ('packaging', 'jar')]:
            ET.SubElement(project, name).text = text
        props = ET.SubElement(project, 'properties')
        ET.SubElement(props, 'maven.compiler.release').text = '21'
        build = ET.SubElement(project, 'build')
        for field, path in {'sourceDirectory': 'src/main/java', 'testSourceDirectory': 'src/test/java',
                            'directory': 'target', 'outputDirectory': 'target/classes',
                            'testOutputDirectory': 'target/test-classes'}.items():
            ET.SubElement(build, field).text = str(root / 'air-model' / path)
        ET.SubElement(build, 'finalName').text = 'air-java-0.1.0-SNAPSHOT'
        plugins = ET.SubElement(build, 'plugins')
        plugin = ET.SubElement(plugins, 'plugin')
        executions = ET.SubElement(plugin, 'executions')
        execution = ET.SubElement(executions, 'execution')
        ET.SubElement(execution, 'id').text = 'air-contract-suite'
        config = ET.SubElement(execution, 'configuration')
        for field, text in [('skip', 'false'), ('classpathScope', 'test'), ('workingDirectory', str(root / 'air-model'))]:
            ET.SubElement(config, field).text = text
        args = ET.SubElement(config, 'arguments')
        for field, text in [('argument', '-ea'), ('argument', '-classpath'), ('classpath', ''), ('argument', SUITE)]:
            ET.SubElement(args, field).text = text
        return project

    def test_effective_runtime_dependency_is_rejected_without_bytecode_reference(self):
        root = Path('/tmp/effective-oracle')
        project = self.effective(root)
        inspect_effective(project, 'air-model', '0.1.0-SNAPSHOT', root)
        deps = ET.SubElement(project, 'dependencies')
        dependency = ET.SubElement(deps, 'dependency')
        for field, text in [('groupId', 'com.fasterxml.jackson.core'), ('artifactId', 'jackson-databind'),
                            ('version', '2.22.2'), ('scope', 'runtime')]:
            ET.SubElement(dependency, field).text = text
        with self.assertRaisesRegex(Failure, 'dependencies forbidden'):
            inspect_effective(project, 'air-model', '0.1.0-SNAPSHOT', root)

    def test_cli_skip_interpolation_overrides_declaration_in_launcher(self):
        root = Path('/tmp/effective-oracle')
        project = self.effective(root)
        ET.SubElement(project.find('properties'), 'skipTests').text = 'false'
        project.find('build/plugins/plugin/executions/execution/configuration/skip').text = 'true'
        with self.assertRaisesRegex(Failure, 'Skipped effective ContractSuite launcher'):
            inspect_effective(project, 'air-model', '0.1.0-SNAPSHOT', root)

    def test_skip_or_wrong_effective_source_path_is_rejected(self):
        root = Path('/tmp/effective-oracle')
        for field in ('skipTests', 'maven.test.skip', 'exec.skip'):
            project = self.effective(root)
            ET.SubElement(project.find('properties'), field).text = 'true'
            with self.subTest(field=field), self.assertRaisesRegex(Failure, 'Skipped build/ContractSuite'):
                inspect_effective(project, 'air-model', '0.1.0-SNAPSHOT', root)
        project = self.effective(root)
        project.find('build/sourceDirectory').text = str(root / 'air-json/src/main/java')
        with self.assertRaisesRegex(Failure, 'ownership path'):
            inspect_effective(project, 'air-model', '0.1.0-SNAPSHOT', root)


class CompiledOwnershipTests(unittest.TestCase):
    def test_unexpected_classfile_inside_known_package_is_rejected(self):
        import struct
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            path = root / 'classes/io/github/gustavo2358/air/model/Ghost.class'
            path.parent.mkdir(parents=True)
            path.write_bytes(struct.pack('>IHH', 0xCAFEBABE, 0, 65))
            with self.assertRaisesRegex(Failure, 'without source owner'):
                architecture.inspect_owned_classes(root, 'air-model', root / 'classes')

    def test_json_cannot_contain_model_classes_or_unowned_codec(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            for name in ('io/github/gustavo2358/air/model/Publication.class', 'io/github/gustavo2358/air/json/Codec.class'):
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                import struct
                path.write_bytes(struct.pack('>IHH', 0xCAFEBABE, 0, 65))
                with self.subTest(name=name), self.assertRaisesRegex(Failure, 'Unexpected production class|without source owner'):
                    architecture.inspect_owned_classes(root, 'air-json', root)
                path.unlink()

    def test_relocated_model_class_with_source_owner_is_rejected(self):
        import struct
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            model = root / 'air-model/src/main/java/io/github/gustavo2358/air/model/Publication.java'
            model.parent.mkdir(parents=True)
            model.write_text('model source')
            name = 'io/github/gustavo2358/air/json/Publication'
            source = root / ('air-json/src/main/java/' + name + '.java')
            source.parent.mkdir(parents=True)
            source.write_text('relocated model source')
            compiled = root / ('classes/' + name + '.class')
            compiled.parent.mkdir(parents=True)
            compiled.write_bytes(struct.pack('>IHH', 0xCAFEBABE, 0, 65))
            with self.assertRaisesRegex(Failure, 'Copied/shaded model'):
                architecture.inspect_owned_classes(root, 'air-json', root / 'classes')

    def test_model_output_cannot_be_missing(self):
        with tempfile.TemporaryDirectory() as temp, self.assertRaisesRegex(Failure, 'No production classfiles'):
            architecture.inspect_owned_classes(Path(temp), 'air-model', Path(temp))

    def test_jar_cannot_contain_stale_classfile(self):
        import zipfile
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            classes = root / 'classes'
            classes.mkdir()
            (classes / 'Value.class').write_bytes(b'new compiled bytes')
            jar = root / 'model.jar'
            with zipfile.ZipFile(jar, 'w') as out:
                out.writestr('Value.class', b'stale jar bytes')
            with self.assertRaisesRegex(Failure, 'inventory or bytes mismatch'):
                architecture.inspect_jar(jar, classes, 'air-model')


if __name__ == "__main__":
    unittest.main()
