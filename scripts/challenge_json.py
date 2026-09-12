#!/usr/bin/env python3
"""Opt-in mutation challenge of the 1A/4B codec in a disposable copy; never called by Maven."""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent / 'harness'))
from lean import require_local
require_local()

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'air-json/src/main/java/io/github/gustavo2358/air/json/'


def run(command, cwd):
    result = subprocess.run(command, cwd=cwd, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    return result.returncode, result.stdout


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    mutations = [
        ('partial-to-complete', 'BindingWriter.java', '"inventory", inventoryStatus(c.inventory())', '"inventory", "COMPLETE"'),
        ('unavailable-to-exact', 'BindingWriter.java', '"status", precisionStatus(c.status())', '"status", "EXACT"'),
        ('drop-uncertainty', 'BindingWriter.java', 'array(p.uncertainties(), this::uncertainty)', 'array(p.uncertainties().stream().skip(1).toList(), this::uncertainty)'),
        ('drop-origin', 'BindingWriter.java', 'array(p.origins(), this::origin)', 'array(p.origins().stream().skip(1).toList(), this::origin)'),
        ('sort-artifacts', 'BindingWriter.java', 'array(p.artifacts(), this::artifact)', 'array(p.artifacts().stream().sorted(java.util.Comparator.comparing(a -> a.id().localId())).toList(), this::artifact)'),
        ('accept-duplicate', 'Json.java', 'if (frame.fields.containsKey(key)) throw error("Duplicate property: " + key);', '// mutation: overwrite duplicate'),
        ('accept-number', 'Json.java', 'default -> throw error("Expected binding JSON value; numbers must be canonical decimal strings");',
         'default -> { int start = position; while (position < text.length() && "0123456789-+.eE".indexOf(text.charAt(position)) >= 0) position++; if (start == position) throw error("Expected value"); yield new Text(text.substring(start, position)); }'),
        ('accept-version', 'BindingReader.java', 'if (!expected.equals(actual)) throw new AirJsonException(VERSION_MISMATCH, at.path(), "Expected " + expected + ", received " + actual);', '// mutation: accept versions'),
        ('accept-unknown-field', 'BindingReader.java', 'if (!required.contains(name)) throw Json.input(path + "." + name, "Unknown field");', 'if (!required.contains(name)) { /* mutation: ignore field */ }'),
        ('final-newline', 'Json.java', 'return bytes;', 'byte[] changed = java.util.Arrays.copyOf(bytes, bytes.length + 1); changed[bytes.length] = 10; return changed;'),
        ('noncanonical-nonascii-escaping', 'Json.java', 'else scalar(c);', 'else if (c == \'á\') ascii("\\\\u00e1"); else scalar(c);'),
        ('ignore-valid-halt', 'BindingReader.java', 'if (!kind.equals("return")) throw a.unsupported("Operation " + kind);',
         'if (!kind.equals("return")) return new Operations.Return(header(a.child("header")), List.of());'),
        ('runtime-enum-name', 'BindingWriter.java', '"inventory", inventoryStatus(c.inventory())', '"inventory", c.inventory().name()'),
        ('runtime-enum-to-string', 'BindingWriter.java', '"inventory", inventoryStatus(c.inventory())', '"inventory", c.inventory().toString()'),
        ('runtime-string-value-of', 'BindingWriter.java', '"inventory", inventoryStatus(c.inventory())', '"inventory", String.valueOf(c.inventory())'),
        ('local-invalid-without-rule', 'BindingReader.java', 'ValidationIssue.Kind.INVALID_IR, rule, Optional.empty(), detail',
         'ValidationIssue.Kind.INVALID_IR, "", Optional.empty(), detail'),
        ('line-base-limit-as-invalid', 'BindingReader.java',
         's.child("lineBase").representability("Span only supports bases 0 or 1; binding admits Natural")',
         's.child("lineBase").invalid("I-36", "mutation: Java base restriction misclassified as AIR")'),
        ('column-base-limit-as-invalid', 'BindingReader.java',
         's.child("columnBase").representability("Span only supports bases 0 or 1; binding admits Natural")',
         's.child("columnBase").invalid("I-36", "mutation: Java base restriction misclassified as AIR")'),
        ('empty-entities-limit-as-invalid', 'BindingReader.java',
         'a.child("entities").representability("EntityScope requires nonempty entities; binding admits Id[]")',
         'a.child("entities").invalid("I-32", "mutation: Java entities restriction misclassified as AIR")'),
        ('blank-text-limit-as-invalid', 'BindingReader.java',
         'representability("Require.text rejects blank Text admitted by the pinned binding")',
         'invalid("AIR-06 §4", "mutation: Java blank restriction misclassified as AIR")'),
        ('generic-constructor-as-limit', 'BindingReader.java', 'return constructor.get();',
         'try { return constructor.get(); } catch (IllegalArgumentException error) { throw Json.limit(path, "mutation: generic constructor limit"); }'),
    ]
    # Keep the 21 earlier mutations; challenge each newly authorized boundary independently.
    for name, restriction in (
            ('span-below-base', 'air-java requires coordinates at or above the declared bases'),
            ('span-inverted-lines', 'air-java requires start.line <= end.line'),
            ('span-inverted-columns', 'air-java requires start.column <= end.column on the same line')):
        before = f's.spanRepresentability("{restriction}")'
        mutations.append((name + '-as-invalid', 'BindingReader.java', before,
                          's.invalid("mutation-only", "misclassified Span")'))
        mutations.append((name + '-as-input', 'BindingReader.java', before,
                          'Json.input(s.path(), "misclassified Span")'))
    mutations.extend([
        ('4b-drop-object', 'BindingWriter.java', 'array(u.objects(), this::objectDeclaration)', 'array(u.objects().stream().skip(1).toList(), this::objectDeclaration)'),
        ('4b-drop-cell', 'BindingWriter.java', 'array(p.storage(), this::storage)', 'array(p.storage().stream().skip(1).toList(), this::storage)'),
        ('4b-ignore-instructions', 'BindingReader.java', 'a.child("instructions").list(this::instruction)', 'List.of()'),
        ('4b-swap-write-role', 'BindingWriter.java', 'case VALUE_WRITE -> "VALUE_WRITE";', 'case VALUE_WRITE -> "VALUE_READ";'),
        ('4b-repair-operand-owner', 'BindingReader.java', 'return new Operations.Assign(header(a.child("header")), place(a.child("destination")), expression(a.child("value")));',
         'var h = header(a.child("header")); var d = (Places.ObjectPlace)place(a.child("destination")); var oh = d.header(); return new Operations.Assign(h, new Places.ObjectPlace(new Operand.Header(new OperandId(new OperationOwner(h.id()), oh.id().localId()), oh.role(), oh.origin()), d.object()), expression(a.child("value")));'),
        ('4b-runtime-enum-name', 'BindingWriter.java', '"role", role(h.role())', '"role", h.role().name()'),
        ('4b-unsupported-as-invalid', 'BindingReader.java', 'throw a.unsupported("Instruction " + kind)', 'throw a.invalid("I-04", "mutation: unsupported is invalid")'),
        ('4b-drop-destination', 'BindingWriter.java', '"destination", place(a.destination())', '"destination", null'),
        ('4b-drop-literal-value', 'BindingWriter.java', '"value", literalValue(l.value())', '"value", null'),
        ('4b-reverse-instructions-reader', 'BindingReader.java', 'a.child("instructions").list(this::instruction)', 'a.child("instructions").list(this::instruction).reversed()'),
        ('4b-sort-instructions-writer', 'BindingWriter.java', 'array(s.instructions(), this::instruction)', 'array(s.instructions().stream().sorted(java.util.Comparator.comparing(i -> i.header().id().localId())).toList(), this::instruction)'),
        ('4b-normalize-text', 'BindingWriter.java', '"value", t.value()', '"value", t.value().strip()'),
        ('4b-replace-text-by-constant', 'BindingWriter.java', '"value", t.value()', '"value", "CHANGED"'),
        ('4b-derived-model-oracle', 'air-json/src/test/java/io/github/gustavo2358/air/json/ScalarAssignOracle.java',
         'static Publication publication() { return publication(1, 1); }',
         'static Publication publication() { try { return new AirJson().decode(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/test/resources/scalar-assign.canonical.json"))); } catch (java.io.IOException e) { throw new AssertionError(e); } }'),
        ('4b-altered-manual-golden', 'air-json/src/test/resources/scalar-assign.canonical.json', '"value":"PROGA"', '"value":"CHANGED"'),
    ])
    expected_checks = json.loads((ROOT / 'docs/evals/transport-checks.json').read_text())['checks']
    report = {'baseline': 'ce530a7e17ab12b23c48f29425f503ff920b09fb', 'mutations': []}
    logs = args.output.with_suffix('.logs')
    logs.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='air-json-challenge-') as temporary:
        root = Path(temporary)
        for owner in ('air-model', 'air-json'):
            shutil.copytree(ROOT / owner / 'src', root / owner / 'src')
        model, codec, tests = (root / n for n in ('model-classes', 'json-classes', 'test-classes'))
        for directory in (model, codec, tests): directory.mkdir()
        def compile_(source, output, classpath):
            code, log = run(['javac', '--release', '21', '-encoding', 'UTF-8', '-Xlint:all', '-Werror',
                             '-cp', str(classpath), '-d', str(output), *map(str, sorted(source.rglob('*.java')))], root)
            if code: raise RuntimeError('Challenge compilation failed; not a semantic RED:\n' + log)
        def compile_codec(): compile_(root / 'air-json/src/main/java', codec, model)
        def suite(): return run(['java', '-ea', '-cp', f'{model}:{codec}:{tests}',
                                'io.github.gustavo2358.air.json.CodecSuite'], root / 'air-json')
        compile_(root / 'air-model/src/main/java', model, model)
        compile_codec()
        compile_(root / 'air-json/src/test/java', tests, f'{model}:{codec}')
        code, log = suite()
        if code: raise RuntimeError('Baseline not GREEN:\n' + log)
        (logs / 'initial-green.log').write_text(log)
        report['initial_green'] = re.search(r'PASS: \d+ deterministic transport checks', log).group()
        for name, filename, before, after in mutations:
            path = root / (filename if '/' in filename else PACKAGE + filename)
            original = path.read_bytes()
            source = original.decode()
            if source.count(before) != 1: raise RuntimeError(f'Ambiguous mutation site: {name}')
            try:
                path.write_text(source.replace(before, after))
                compile_codec()
                compile_(root / 'air-json/src/test/java', tests, f'{model}:{codec}')
                code, log = suite()
                (logs / (name + '.log')).write_text(log)
                if code == 0: raise RuntimeError('SURVIVING mutation: ' + name)
                passed = re.findall(r'^json-ok \d+ - (.+)$', log, re.M)
                failure = next((line for line in log.splitlines() if line.startswith('Exception')), '')
                report['mutations'].append({'name': name, 'exit_code': code, 'failing_check': expected_checks[len(passed)], 'last_passed': passed[-1] if passed else None,
                                            'failure': failure, 'restored_sha256': hashlib.sha256(original).hexdigest()})
                print(f'RED: {name}; {failure}', flush=True)
            finally:
                path.write_bytes(original)
                if path.read_bytes() != original: raise RuntimeError('Restoration failed')
        compile_codec()
        compile_(root / 'air-json/src/test/java', tests, f'{model}:{codec}')
        code, log = suite()
        (logs / 'restored-green.log').write_text(log)
        if code: raise RuntimeError('Restored suite not GREEN:\n' + log)
        report['restored_green'] = re.search(r'PASS: \d+ deterministic transport checks', log).group()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2, ensure_ascii=False) + '\n')
    print('PASS: all mutations killed, sources restored byte for byte, second GREEN')


if __name__ == '__main__': main()
