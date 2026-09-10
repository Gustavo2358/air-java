"""Closed 1A Maven shape and resolved graph. No Maven/network in this module."""
from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from pathlib import Path

from common import require

GROUP = 'io.github.gustavo2358'
MODULES = {'air-model': 'air-java', 'air-json': 'air-json'}
PLUGINS = {'maven-compiler-plugin': '3.13.0', 'maven-jar-plugin': '3.4.2',
           'exec-maven-plugin': '3.5.0', 'maven-help-plugin': '3.5.1',
           'maven-dependency-plugin': '3.8.1'}
PROPERTIES = {'maven.compiler.release': '21', 'project.build.sourceEncoding': 'UTF-8',
              'project.reporting.outputEncoding': 'UTF-8', 'skipTests': 'false',
              'maven.test.skip': 'false', 'maven.main.skip': 'false', 'exec.skip': 'false',
              'project.build.outputTimestamp': '2026-09-05T00:00:00Z'}
SUITE = 'io.github.gustavo2358.air.validation.ContractSuite'
JSON_SUITE = 'io.github.gustavo2358.air.json.CodecSuite'


def xml(path):
    require(path.is_file(), f'Missing module or Maven evidence: {path}')
    tree = ET.parse(path).getroot()
    for element in tree.iter():
        require(element.tag.startswith('{http://maven.apache.org/POM/4.0.0}'),
                f'Unexpected Maven XML namespace: {path}')
        element.tag = element.tag.split('}', 1)[1]
    return tree


def value(element, path, default=''):
    return element.findtext(path, default).strip()


def children(element, allowed, context):
    tags = [child.tag for child in element]
    require(set(tags) <= set(allowed) and len(tags) == len(set(tags)),
            f'Unsupported POM shape ({context}): {tags}')


def dependencies(project, owner, version):
    deps = project.findall('dependencies/dependency')
    if owner != 'air-json':
        require(not deps, f'{owner}: compile/runtime/test dependencies forbidden')
        return
    require(len(deps) == 1, 'air-json requires exactly one direct compile dependency')
    dep = deps[0]
    children(dep, {'groupId', 'artifactId', 'version', 'scope'}, owner)
    require((value(dep, 'groupId'), value(dep, 'artifactId'), value(dep, 'version'),
             value(dep, 'scope', 'compile')) == (GROUP, 'air-java', version, 'compile'),
            'air-json requires direct compile dependency on air-java at the joint version')


def inspect_build(project, owner):
    build = project.find('build')
    require(build is not None, f'Missing build verification: {owner}')
    children(build, {'plugins', 'pluginManagement'} if owner == 'root' else {'plugins'}, owner)
    plugins = build.findall('plugins/plugin')
    expected = ['exec-maven-plugin'] if owner == 'root' else [
        'maven-help-plugin', 'maven-dependency-plugin', 'exec-maven-plugin']
    require([value(p, 'artifactId') for p in plugins] == expected,
            f'Missing, reordered or unexpected module plugins: {owner}')
    managed = build.findall('pluginManagement/plugins/plugin')
    if owner == 'root':
        require([value(p, 'artifactId') for p in managed] == list(PLUGINS),
                'Unexpected managed plugins')
    for plugin in plugins + managed:
        children(plugin, {'groupId', 'artifactId', 'version', 'configuration', 'executions', 'inherited'}, owner)
        artifact = value(plugin, 'artifactId')
        group = 'org.codehaus.mojo' if artifact == 'exec-maven-plugin' else 'org.apache.maven.plugins'
        require(value(plugin, 'groupId') == group, f'Unexpected plugin group: {artifact}')
        require(value(plugin, 'version') == (PLUGINS[artifact] if plugin in managed else ''),
                f'Unexpected plugin version override: {artifact}')
        require(plugin.find('configuration') is None or artifact == 'maven-compiler-plugin',
                f'Unexpected global plugin configuration: {artifact}')
        if plugin in managed:
            if artifact == 'maven-compiler-plugin':
                config = plugin.find('configuration')
                require(config is not None, 'Missing compiler policy')
                children(config, {'showWarnings', 'compilerArgs'}, artifact)
                require(value(config, 'showWarnings') == 'true' and
                        [a.text for a in config.findall('compilerArgs/arg')] == ['-Xlint:all', '-Werror'],
                        'Compiler warnings must be errors')
            expected_execution = {'maven-help-plugin': ('effective-module', 'effective-pom',
                                  {'output': '${project.build.directory}/effective-pom.xml'}),
                                  'maven-dependency-plugin': ('resolved-module', 'tree',
                                  {'outputType': 'json', 'outputFile': '${project.build.directory}/dependency-tree.json'})}
            managed_executions = plugin.findall('executions/execution')
            if artifact in expected_execution:
                identifier, goal, configuration = expected_execution[artifact]
                require(len(managed_executions) == 1, f'Missing/duplicate Maven evidence producer: {artifact}')
                execution = managed_executions[0]
                children(execution, {'id', 'phase', 'goals', 'configuration'}, artifact)
                require(value(execution, 'id') == identifier and value(execution, 'phase') == 'verify'
                        and [g.text for g in execution.findall('goals/goal')] == [goal],
                        f'Incorrect Maven evidence producer: {artifact}')
                config = execution.find('configuration')
                require(config is not None, f'Missing evidence configuration: {artifact}')
                children(config, configuration, artifact)
                require({c.tag: c.text for c in config} == configuration, f'Incorrect evidence paths/scope: {artifact}')
            else:
                require(not managed_executions, f'Unexpected inherited execution: {artifact}')
    executions = [e for p in plugins for e in p.findall('executions/execution')]
    ids = [value(e, 'id') for e in executions]
    require(ids == ({'root': ['reactor-topology'], 'air-model': ['air-contract-suite', 'compiled-module'],
                     'air-json': ['air-json-suite', 'compiled-module']}[owner]),
            f'Unexpected, missing or duplicate executions: {owner}')
    for execution in executions:
        identifier = value(execution, 'id')
        children(execution, {'id', 'phase', 'goals', 'configuration'}, owner)
        phase = {'reactor-topology': 'validate', 'air-contract-suite': 'test', 'air-json-suite': 'test', 'compiled-module': 'verify'}[identifier]
        require(value(execution, 'phase') == phase and
                [g.text for g in execution.findall('goals/goal')] == ['exec'],
                f'Incorrect phase/goal: {identifier}')
        config = execution.find('configuration')
        require(config is not None, f'Missing launcher: {identifier}')
        if identifier in {'air-contract-suite', 'air-json-suite'}:
            children(config, {'executable', 'workingDirectory', 'classpathScope', 'skip', 'outputFile', 'arguments'}, owner)
            required = {'executable': '${java.home}/bin/java', 'workingDirectory': '${project.basedir}',
                        'classpathScope': 'test', 'skip': '${skipTests}',
                        'outputFile': '${project.build.directory}/' + ('contract-suite.log' if owner == 'air-model' else 'transport-suite.log')}
            require(all(value(config, k) == v for k, v in required.items()), 'ContractSuite fork/cwd/test classpath required')
            require([(e.tag, (e.text or '').strip()) for e in config.find('arguments')] == [
                ('argument', '-ea'), ('argument', '-classpath'), ('classpath', ''), ('argument', SUITE if owner == 'air-model' else JSON_SUITE)],
                'ContractSuite must run once with assertions and test classpath')
        else:
            children(config, {'executable', 'skip', 'arguments'}, owner)
            require(value(config, 'skip') == 'false', 'Architecture inspection cannot be skipped')
            require(value(config, 'executable') == 'python3', 'Expected focused Python inspection')
            script = '${project.basedir}/scripts/harness/architecture.py' if owner == 'root' else '${project.basedir}/../scripts/harness/architecture.py'
            flags = ['--build-flags', '${skipTests}', '${maven.test.skip}', '${maven.main.skip}', '${exec.skip}']
            args = ['-B', script, '--topology', *flags] if owner == 'root' else [
                '-B', script, *flags, '--module', owner, '--classes', '${project.build.outputDirectory}',
                '--dependency-tree', '${project.build.directory}/dependency-tree.json',
                '--effective-pom', '${project.build.directory}/effective-pom.xml']
            require([a.text for a in config.findall('arguments/argument')] == args,
                    'Expected focused inspection; recursive/skipped gate forbidden')
    if owner == 'root':
        require(value(plugins[0], 'inherited') == 'false', 'Root inspection must not be inherited')


def inspect_topology(root):
    root = root.resolve()
    parent = xml(root / 'pom.xml')
    require([m.text for m in parent.findall('modules/module')] == list(MODULES),
            'Expected modules exactly air-model, air-json in dependency order')
    dependencies(parent, 'root', '')
    children(parent, {'modelVersion', 'groupId', 'artifactId', 'version', 'packaging', 'name',
                      'description', 'properties', 'modules', 'build'}, 'root')
    version = value(parent, 'version')
    require(re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+-SNAPSHOT', version), 'Expected literal joint SNAPSHOT version')
    require((value(parent, 'groupId'), value(parent, 'artifactId'), value(parent, 'packaging')) ==
            (GROUP, 'air-java-parent', 'pom'), 'Expected distinct air-java-parent:pom')
    props = parent.find('properties')
    require(props is not None and {p.tag: p.text for p in props} == PROPERTIES,
            'Unsupported POM properties; offline shape must stay closed')
    inspect_build(parent, 'root')
    for module, artifact in MODULES.items():
        pom = xml(root / module / 'pom.xml')
        dependencies(pom, module, '${project.version}')
        children(pom, {'modelVersion', 'parent', 'artifactId', 'packaging', 'name', 'description',
                       'build', 'dependencies'} if module == 'air-json' else
                      {'modelVersion', 'parent', 'artifactId', 'packaging', 'name', 'description', 'build'}, module)
        require(value(pom, 'artifactId') == artifact and value(pom, 'packaging') == 'jar',
                f'Wrong module artifact/packaging: {module}')
        p = pom.find('parent')
        require(p is not None, f'Missing local parent: {module}')
        children(p, {'groupId', 'artifactId', 'version', 'relativePath'}, module)
        require((value(p, 'groupId'), value(p, 'artifactId'), value(p, 'version'), value(p, 'relativePath')) ==
                (GROUP, 'air-java-parent', version, '../pom.xml'), f'Expected joint local parent: {module}')
        inspect_build(pom, module)
    # Enumerate all physical inputs, including untracked owners; do not just grep imports.
    for path in root.rglob('*'):
        rel = path.relative_to(root)
        if '.git' not in rel.parts and path.is_file() and path.suffix in {'.class', '.jar'}:
            require(len(rel.parts) > 2 and rel.parts[0] in MODULES and rel.parts[1] == 'target',
                    f'Root product or Unowned compiled artifact: {rel}')
        if any(p in {'.git', 'target', '.idea', '.vscode'} for p in rel.parts):
            continue
        require(not path.is_symlink(), f'Symlink input not supported: {rel}')
        if not path.is_file():
            continue
        if path.name == 'pom.xml':
            require(rel.as_posix() in {'pom.xml', 'air-model/pom.xml', 'air-json/pom.xml'}, f'Unowned Maven module: {rel}')
        if path.suffix == '.java' and 'src' not in rel.parts:
            require(rel.as_posix() == 'examples/MinimalPublication.java', f'Unowned Java source: {rel}')
        if 'src' in rel.parts:
            if rel.parts[0] == 'air-json':
                java_roots = ('air-json/src/main/java/io/github/gustavo2358/air/json/',
                              'air-json/src/test/java/io/github/gustavo2358/air/json/')
                require((rel.as_posix().startswith(java_roots) and path.suffix == '.java')
                        or rel.as_posix() in {'air-json/src/test/resources/goback.canonical.json',
                                              'air-json/src/test/resources/scalar-assign.canonical.json'},
                        f'Unowned JSON source/resource: {rel}')
            else:
                require(rel.parts[:4] in [('air-model', 'src', 'main', 'java'), ('air-model', 'src', 'test', 'java')],
                        f'Unowned source/resource: {rel}')
                require(path.suffix == '.java', f'Unsupported model source/resource: {rel}')
        if rel.parts[0] == '.mvn':
            require(False, f'Unsupported POM extension/configuration: {rel}')
    require(any((root / 'air-model/src/main/java').rglob('*.java')), 'No model production sources')
    require(not list((root / 'target').rglob('*.class')) and not list((root / 'target').glob('*.jar')),
            'Root product classfile/JAR forbidden; remove stale root build output')
    require(any((root / 'air-json/src/main/java').rglob('*.java')), 'Missing JSON implementation')
    for required in ('air-json/src/main/java/io/github/gustavo2358/air/json/AirJson.java',
                     'air-json/src/test/java/io/github/gustavo2358/air/json/CodecSuite.java',
                     'air-json/src/test/java/io/github/gustavo2358/air/json/GobackOracle.java',
                     'air-json/src/test/resources/goback.canonical.json',
                     'air-json/src/test/resources/scalar-assign.canonical.json', 'docs/evals/transport-checks.json'):
        require((root / required).is_file(), f'Missing JSON suite/policy/evidence: {required}')
    from common import read_json
    policy = read_json(root / 'docs/evals/transport-checks.json')
    require(policy.get('binding') == 'analysis-ir-json' and policy.get('bindingVersion') == '1.0.0'
            and policy.get('airVersion') == '2.0.0' and policy.get('status') == 'DRAFT'
            and policy.get('analysis_ir_pin') == '51b4d9a8ae0364232bd97103cd73a77e1a34996c'
            and policy.get('external_dependencies') == [] and policy.get('checks'), 'Invalid JSON suite/dependency policy')
    return version


def inspect_effective(project, owner, version, root):
    # help:effective-pom can wrap reactor models in <projects>.
    if project.tag == 'projects':
        projects = [p for p in project if value(p, 'artifactId') == MODULES[owner]]
        require(len(projects) == 1, f'Missing/duplicate effective module: {owner}')
        project = projects[0]
    require(project.tag == 'project', 'Expected effective Maven project')
    require((value(project, 'groupId'), value(project, 'artifactId'), value(project, 'version'), value(project, 'packaging', 'jar')) ==
            (GROUP, MODULES[owner], version, 'jar'), 'Wrong effective module GAV')
    dependencies(project, owner, version)
    for property_name in ('skipTests', 'maven.test.skip', 'maven.main.skip', 'exec.skip'):
        require(value(project, 'properties/' + property_name, 'false') == 'false', f'Skipped build/ContractSuite: {property_name}')
    suite_executions = [e for e in project.findall('build/plugins/plugin/executions/execution')
                        if value(e, 'id') in {'air-contract-suite', 'air-json-suite'}]
    require(len(suite_executions) == 1 and value(suite_executions[0], 'id') ==
            ('air-contract-suite' if owner == 'air-model' else 'air-json-suite'), 'Missing/duplicate effective ContractSuite')
    if suite_executions:
        config = suite_executions[0].find('configuration')
        require(config is not None and value(config, 'skip') == 'false', 'Skipped effective ContractSuite launcher')
        require(value(config, 'classpathScope') == 'test' and
                Path(value(config, 'workingDirectory')).resolve() == (root / owner).resolve() and
                [(e.tag, (e.text or '').strip()) for e in config.find('arguments')] == [
                    ('argument', '-ea'), ('argument', '-classpath'), ('classpath', ''), ('argument', SUITE if owner == 'air-model' else JSON_SUITE)],
                'Incorrect effective ContractSuite cwd/assertions/classpath')
    require(value(project, 'properties/maven.compiler.release') == '21', 'Expected effective Java release 21')
    for field, relative in {'sourceDirectory': 'src/main/java', 'testSourceDirectory': 'src/test/java',
                            'directory': 'target', 'outputDirectory': 'target/classes',
                            'testOutputDirectory': 'target/test-classes'}.items():
        require(Path(value(project, 'build/' + field)).resolve() == (root / owner / relative).resolve(),
                f'Unexpected effective ownership path: {owner}/{field}')
    require(value(project, 'build/finalName') == f'{MODULES[owner]}-{version}', 'Unexpected effective JAR name')


def inspect_graph(tree, owner, version):
    require((tree.get('groupId'), tree.get('artifactId'), tree.get('version'), tree.get('type')) ==
            (GROUP, MODULES[owner], version, 'jar'), 'Wrong dependency graph root')
    children_ = tree.get('children', [])
    if owner == 'air-model':
        require(not children_, 'air-model resolved dependencies forbidden, including unused runtime/optional')
    else:
        require(len(children_) == 1, 'air-json resolved graph requires exactly one direct edge')
        dep = children_[0]
        require((dep.get('groupId'), dep.get('artifactId'), dep.get('version'), dep.get('type'), dep.get('scope')) ==
                (GROUP, 'air-java', version, 'jar', 'compile') and not dep.get('children')
                and str(dep.get('optional', 'false')).lower() == 'false' and not dep.get('classifier'),
                'Unexpected air-json graph, transitive dependency or cycle')
