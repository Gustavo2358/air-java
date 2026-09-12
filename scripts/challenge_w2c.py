#!/usr/bin/env python3
"""W2C compilable source mutations in a disposable copy; immutable oracles and exact restoration."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
from capacity_probe import ROOT, compile_sources

import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent / 'harness'))
from lean import require_local
require_local()

JSON = 'air-json/src/main/java/io/github/gustavo2358/air/json/'
WRITER = JSON + 'BindingWriter.java'
READER = JSON + 'BindingReader.java'
REFS = 'air-model/src/main/java/io/github/gustavo2358/air/validation/ReferenceChecks.java'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def mutations():
    arms = '"trueDestination", id(b.trueDestination()), "falseDestination", id(b.falseDestination())'
    swapped = '"trueDestination", id(b.falseDestination()), "falseDestination", id(b.trueDestination())'
    reader_arms = 'labelId(a.child("trueDestination")), labelId(a.child("falseDestination"))'
    unknown_wire = '''result = object("kind", "unknown", "header", operandHeader(u.header()), "typeRef", typeRef(u.typeRef()),
                        "dependencies", new Arr(frame.dependencies), "remainingReads", memoryBound(u.remainingReads()), "reason", id(u.reason()));'''
    fake_assertion = 'yield new Proofs.DisjointStorage(assertion.child("storage").list(this::storageId));'
    return [
        ('swap-branch-arms-writer', [(WRITER, arms, swapped)]),
        ('swap-arms-reader-and-writer-together', [(WRITER, arms, swapped),
            (READER, reader_arms, 'labelId(a.child("falseDestination")), labelId(a.child("trueDestination"))')]),
        ('jump-as-branch', [(WRITER, 'object("kind", "jump", "header", header(j.header())', 'object("kind", "branch", "header", header(j.header())')]),
        ('drop-predicate', [(WRITER, ', "predicate", expression(b.predicate())', '')]),
        ('bool-as-text', [(WRITER, 'else if (k.type() == Types.Builtin.BOOL) kind = "bool";', 'else if (k.type() == Types.Builtin.BOOL) kind = "text";')]),
        ('drop-known-dependencies', [(WRITER, 'new Arr(frame.dependencies)', 'new Arr(List.of())')]),
        ('close-remaining-reads', [(WRITER, 'memoryBound(u.remainingReads())', 'memoryBound(Scopes.NoMemory.INSTANCE)')]),
        ('drop-reason', [(WRITER, ', "reason", id(u.reason())', '')]),
        ('unknown-to-literal', [(WRITER, unknown_wire, 'result = object("kind", "literal", "header", operandHeader(u.header()), "value", object("kind", "text", "value", "false"));')]),
        ('drop-premise-writer', [(WRITER, 'array(p.premises(), this::premise)', 'new Arr(List.of())')]),
        ('drop-disjoint-member', [(WRITER, 'array(d.storage(), this::id)', 'array(d.storage().subList(0, d.storage().size() - 1), this::id)')]),
        ('sort-disjoint-members', [(WRITER, 'array(d.storage(), this::id)', 'array(d.storage().stream().sorted(java.util.Comparator.comparing(StorageId::localId)).toList(), this::id)')]),
        ('object-id-as-storage', [(WRITER, 'array(d.storage(), this::id)', 'array(d.storage(), s -> id(new ObjectId(new UnitId(s.publication(), "unit"), s.localId())))')]),
        ('accept-same-domain-reader', [(READER, 'assertion.fields("kind", "left", "right", "scope"); throw assertion.unsupported("Assertion.same_domain");',
            'yield new Proofs.DisjointStorage(List.of(new StorageId(new PublicationId("cp6-w2c-manual"), "cell-FLAG"), new StorageId(new PublicationId("cp6-w2c-manual"), "cell-WS-PGM")));')]),
        ('ignore-unknown-assertion', [(READER, 'default -> throw Json.input(assertion.path(), "Unknown Assertion kind");', 'default -> { ' + fake_assertion + ' }')]),
        ('empty-premises-reader', [(READER, 'origins, coverage, gaps, premises);', 'origins, coverage, gaps, List.of());')]),
        ('writer-loses-branch', [(WRITER, '''return object("kind", "branch", "header", header(b.header()), "predicate", expression(b.predicate()),
                    "trueDestination", id(b.trueDestination()), "falseDestination", id(b.falseDestination()));''',
            'return object("kind", "return", "header", header(b.header()), "values", new Arr(List.of()));')]),
        ('reader-rejects-branch', [(READER, '''if (kind.equals("branch")) return new Operations.Branch(header(a.child("header")), expression(a.child("predicate")),
                labelId(a.child("trueDestination")), labelId(a.child("falseDestination")));''',
            'if (kind.equals("branch")) throw a.unsupported("Operation branch");')]),
        ('reader-drops-dependencies', [(READER, 'frame.values, memoryBound(a.child("remainingReads"))', 'List.of(), memoryBound(a.child("remainingReads"))')]),
        ('writer-rejects-unknown', [(WRITER, unknown_wire, 'throw limit("$.expression", "Unknown not implemented");')]),
        # These two mutants affect ONLY disposable copies, never this checkout's model/Validator.
        ('suppress-purity-obligation', [(REFS, 'c.obligation("I-09",id,', 'if (false) c.obligation("I-09",id,')]),
        ('suppress-disjoint-obligation', [(REFS, 'c.obligation("I-59",premise.id(),', 'if (false) c.obligation("I-59",premise.id(),')]),
        ('strengthen-header-coverage', [(WRITER, 'coverageStatus(h.coverage())', '"MODELED"')]),
    ]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    output = args.output.resolve()
    logs = output.with_suffix('.logs'); logs.mkdir(parents=True, exist_ok=True)
    report = {'base': '2a37f5e980ba25fdc79614a66030a84d8bf5b8c9',
              'policy': 'Compilation must pass. Every mutant must reach an assertion independent of the changed production source. Oracles never mutate.',
              'mutations': []}
    with tempfile.TemporaryDirectory(prefix='air-w2c-challenge-') as temporary:
        root = Path(temporary)
        for owner in ('air-model', 'air-json'):
            shutil.copytree(ROOT/owner/'src', root/owner/'src')
        files = sorted(root.glob('air-*/src/**/*'))
        before = {str(p.relative_to(root)): sha(p) for p in files if p.is_file()}
        report['before_sha256'] = before

        def execute(name, green):
            stage = logs/name; stage.mkdir()
            _, cp = compile_sources(root, root/'build', stage)
            result = subprocess.run(['java', '-ea', '-Xmx1024m', '-Xss256k', '-cp', cp,
                'io.github.gustavo2358.air.json.W2cChecks', 'challenge'], cwd=root/'air-json',
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
            path = stage/'test.log'; path.write_text(result.stdout)
            if green and result.returncode != 0:
                raise RuntimeError(f'{name}: GREEN failed; see {path}')
            if not green and (result.returncode == 0 or 'java.lang.AssertionError:' not in result.stdout):
                raise RuntimeError(f'{name}: no independent assertion detection; see {path}')
            return {'name': name, 'compile_exit': 0, 'test_exit': result.returncode,
                    'log': str(path.relative_to(output.parent)), 'log_sha256': sha(path)}

        report['first_green'] = execute('first-green', True)
        for name, edits in mutations():
            originals = {}
            try:
                for path, old, new in edits:
                    file = root/path; original = file.read_bytes(); originals[path] = original
                    source = original.decode()
                    if source.count(old) != 1:
                        raise RuntimeError(f'{name}: expected exactly one mutation site in {path}, got {source.count(old)}')
                    file.write_text(source.replace(old, new))
                item = execute(name, False)
            finally:
                for path, original in originals.items(): (root/path).write_bytes(original)
            restored = {str(p.relative_to(root)): sha(p) for p in files if p.is_file()}
            item['all_sources_and_oracles_restored_byte_exact'] = restored == before
            if restored != before: raise RuntimeError(f'{name}: exact restoration failed')
            report['mutations'].append(item); output.write_text(json.dumps(report, indent=2)+'\n')
            print(f'CHALLENGE {name}: DETECTED; compile=0; all_sources_and_oracles_restored=true', flush=True)
        report['second_green'] = execute('second-green', True)
        report['restored_byte_exact'] = before == {str(p.relative_to(root)): sha(p) for p in files if p.is_file()}
    output.write_text(json.dumps(report, indent=2)+'\n')
    print('W2C CHALLENGES PASS; second GREEN; exact restoration', flush=True)


if __name__ == '__main__':
    main()
