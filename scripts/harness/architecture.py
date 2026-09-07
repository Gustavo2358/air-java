"""Inspect freshly compiled production bytecode using the JDK, not source grep."""
from __future__ import annotations

import argparse
import re
import sys
import zipfile
import struct
import tempfile
from pathlib import Path

from common import ROOT, Failure, read_json, require, run
from module_policy import MODULES, SUITE, inspect_topology, inspect_effective, inspect_graph, xml

MODEL = "io.github.gustavo2358.air.model."
VALIDATION = "io.github.gustavo2358.air.validation."
# Ordinary values/collections/math and compiler-generated record/lambda support.
# java.io/java.nio/java.net/JSON/frontend/frameworks are outside this boundary.
JDK_PACKAGES = {"java.lang", "java.lang.invoke", "java.lang.runtime", "java.math",
                "java.util", "java.util.function", "java.util.stream"}
FORBIDDEN_CLASSES = {"java.lang.Process", "java.lang.ProcessBuilder", "java.lang.Runtime",
                     "java.lang.System", "java.util.ServiceLoader"}


def inspect_dependencies(output):
    edges = []
    for line in output.splitlines():
        match = re.fullmatch(r"\s+(\S+)\s+->\s+(\S+)\s+(.+?)\s*", line)
        if match:
            edges.append(match.groups())
    require(edges, "No class dependencies found in jdeps output")
    for source, target, location in edges:
        require(source.startswith((MODEL, VALIDATION)), f"Unexpected production class: {source}")
        require(location != "not found", f"Unresolved dependency: {source} -> {target}")
        if target.startswith((MODEL, VALIDATION)):
            require(not (source.startswith(MODEL) and target.startswith(VALIDATION)),
                    f"model -> validation forbidden: {source} -> {target}")
        else:
            package = target.rpartition(".")[0]
            require(location == "java.base" and package in JDK_PACKAGES
                    and target not in FORBIDDEN_CLASSES,
                    f"Forbidden dependency: {source} -> {target} ({location})")
    return len(edges)


def inspect_classes(classes):
    files = sorted(classes.rglob("*.class"))
    require(files, "No production classfiles")
    for file in files:
        header = file.read_bytes()[:8]
        require(len(header) == 8, f"Truncated classfile: {file.name}")
        magic, minor, major = struct.unpack(">IHH", header)
        require((magic, minor, major) == (0xCAFEBABE, 0, 65),
                f"Expected Java 21 without preview: {file.name}")
        name = file.relative_to(classes).as_posix()[:-6].replace("/", ".")
        require(name.startswith((MODEL, VALIDATION)), f"Unexpected production class: {name}")
    return len(files)


def inspect_owned_classes(root, owner, classes):
    if owner == 'air-json':
        require(not list(classes.rglob('*.*')), '0C-I air-json must remain empty: unexpected classfile/resource')
        return 0
    count = inspect_classes(classes)
    source_root = root / owner / 'src/main/java'
    for file in classes.rglob('*'):
        if file.is_file():
            require(file.suffix == '.class', f'Unexpected compiled resource: {file}')
            top_level = file.relative_to(classes).as_posix().split('$', 1)[0].removesuffix('.class')
            require((source_root / (top_level + '.java')).is_file(), f'Unexpected classfile without source owner: {file}')
    for source in source_root.rglob('*.java'):
        require((classes / source.relative_to(source_root).with_suffix('.class')).is_file(),
                f'Missing compiled source owner: {source.name}')
    return count


def inspect_jar(jar, classes, owner):
    require(jar.is_file(), f'Missing module JAR: {owner}')
    compiled = {p.relative_to(classes).as_posix(): p.read_bytes() for p in classes.rglob('*.class')}
    with zipfile.ZipFile(jar) as archive:
        names = archive.namelist()
        require(len(names) == len(set(names)), f'Duplicate JAR entries: {owner}')
        packed = {n: archive.read(n) for n in names if n.endswith('.class')}
        require(packed == compiled, f'JAR/classfile inventory or bytes mismatch: {owner}')
        require(all(n.endswith('/') or n.endswith('.class') or n == 'META-INF/MANIFEST.MF'
                    or n.startswith('META-INF/maven/') for n in names), f'Unexpected JAR resource/shading: {owner}')
    if owner == 'air-json':
        require(not packed, '0C-I air-json must remain empty: copied model or codec classes')
    else:
        require(packed, 'Missing model JAR classfiles')
    return len(packed)


def bytecode(root, classes):
    return inspect_dependencies(run(['jdeps', '--multi-release', '21', '-verbose:class', '-filter:none',
                                     str(classes)], root))


def compile_model(root, classes):
    sources = sorted((root / 'air-model/src/main/java').rglob('*.java'))
    classes.mkdir(parents=True)
    # Explicit empty CP prevents CLASSPATH or installed air-json from participating.
    run(['javac', '--release', '21', '-encoding', 'UTF-8', '-Xlint:all', '-Werror',
         '-classpath', str(classes), '-d', str(classes), *map(str, sources)], root)
    return inspect_owned_classes(root, 'air-model', classes), bytecode(root, classes)


def check(root):
    inspect_topology(root)
    with tempfile.TemporaryDirectory(prefix='air-harness-architecture-') as temp:
        count, edges = compile_model(root, Path(temp) / 'classes')
        inspect_owned_classes(root, 'air-json', Path(temp) / 'json-classes')
    return f'{count} Java 21 classfiles; {edges} dependencies; model/validation boundary preserved; air-json empty (0C-I)'


def offline_build(root):
    from contracts import inspect_output
    version = inspect_topology(root)
    with tempfile.TemporaryDirectory(prefix='air-offline-check-') as temp:
        temp = Path(temp)
        classes, tests = temp / 'classes', temp / 'test-classes'
        count, edges = compile_model(root, classes)
        tests.mkdir()
        sources = sorted((root / 'air-model/src/test/java').rglob('*.java'))
        require(sources, 'Missing ContractSuite test sources')
        run(['javac', '--release', '21', '-encoding', 'UTF-8', '-Xlint:all', '-Werror',
             '-cp', str(classes), '-d', str(tests), *map(str, sources)], root)
        output = run(['java', '-ea', '-cp', f'{classes}:{tests}', SUITE], root / 'air-model')
        inspect_output(output, read_json(root / 'docs/evals/contract-checks.json')['checks'])
        print(output, end='')
        empty = temp / 'json-classes'
        empty.mkdir()
        for owner, compiled in [('air-model', classes), ('air-json', empty)]:
            jar = root / owner / 'target' / f'{MODULES[owner]}-{version}.jar'
            jar.parent.mkdir(parents=True, exist_ok=True)
            run(['jar', '--create', '--file', str(jar), '-C', str(compiled), '.'], root)
            inspect_jar(jar, compiled, owner)
            print(f'PASS: offline module {owner}; JAR {jar.relative_to(root)}')
    return f'{count} classfiles; {edges} dependencies; offline reactor verified'


def compiled_module(root, owner, classes, dependency_tree, effective_pom):
    from contracts import inspect_output
    version = inspect_topology(root)
    for actual, relative in [(classes, 'classes'), (dependency_tree, 'dependency-tree.json'),
                             (effective_pom, 'effective-pom.xml')]:
        require(actual.is_absolute() and actual.resolve() == (root / owner / 'target' / relative).resolve(),
                f'Wrong compiled module evidence path: {actual}')
    inspect_effective(xml(effective_pom), owner, version, root)
    inspect_graph(read_json(dependency_tree), owner, version)
    count = inspect_owned_classes(root, owner, classes)
    if owner == 'air-model':
        bytecode(root, classes)
        log = root / owner / 'target/contract-suite.log'
        require(log.is_file(), 'Missing ContractSuite execution log')
        output = log.read_text()
        inspect_output(output, read_json(root / 'docs/evals/contract-checks.json')['checks'])
        # exec:exec captures only the child JVM output. Print only after the nominal oracle passes.
        print(output, end='')
    else:
        require(not list((root / owner / 'target/test-classes').rglob('*.*'))
                and not (root / owner / 'target/contract-suite.log').exists(), 'Unexpected suite in empty air-json')
    inspect_jar(root / owner / 'target' / f'{MODULES[owner]}-{version}.jar', classes, owner)
    return f'compiled module {owner}; effective POM + resolved graph + {count} classfiles + JAR verified'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument('--topology', action='store_true')
    mode.add_argument('--offline-build', action='store_true')
    mode.add_argument('--module', choices=list(MODULES))
    parser.add_argument('--build-flags', nargs=4, default=['false'] * 4,
                        help='Resolved Maven skip flags: all must be false')
    parser.add_argument('--classes', type=Path)
    parser.add_argument('--dependency-tree', type=Path)
    parser.add_argument('--effective-pom', type=Path)
    args = parser.parse_args()
    if args.module and not all((args.classes, args.dependency_tree, args.effective_pom)):
        parser.error('compiled mode requires classes, dependency-tree and effective-pom')
    try:
        require(args.build_flags == ['false'] * 4, f'Skipped build/ContractSuite flags forbidden: {args.build_flags}')
        if args.topology:
            detail = f'reactor topology {inspect_topology(ROOT)}; parent, air-model, air-json (empty 0C-I)'
        elif args.offline_build:
            detail = offline_build(ROOT)
        elif args.module:
            detail = compiled_module(ROOT, args.module, args.classes, args.dependency_tree, args.effective_pom)
        else:
            detail = check(ROOT)
        print(f'PASS: {detail}')
        return 0
    except (Failure, OSError, ValueError) as error:
        print(f'FAIL: architecture: {error}', file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
