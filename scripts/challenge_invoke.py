#!/usr/bin/env python3
"""CP6 W1B: compilable mutations in a disposable copy, hashes restored and second GREEN."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
from capacity_probe import ROOT, compile_sources

JSON = 'air-json/src/main/java/io/github/gustavo2358/air/json/'
MODEL = 'air-model/src/main/java/io/github/gustavo2358/air/validation/'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    args.output = args.output.resolve()
    logs = args.output.with_suffix('.logs')
    logs.mkdir(parents=True, exist_ok=True)
    mutations = [
        ('drop-literal-name', JSON+'BindingWriter.java', '"name", l.name(),', '"name", "",', 'InvokeChecks'),
        ('drop-computed-expression', JSON+'BindingWriter.java', '"name", expression(c.name()),',
         '"name", object("kind", "literal", "header", operandHeader(c.name().header()), "value", object("kind", "text", "value", "invented")),', 'InvokeChecks'),
        ('change-read-object', JSON+'BindingWriter.java', '"place", place(r.place())',
         '"place", place(new Places.ObjectPlace(r.place().header(), new ObjectId(r.header().id().owner().unit(), "other-cell")))', 'InvokeChecks'),
        ('drop-normal-outcome', JSON+'BindingWriter.java', 'array(o.known(), this::alternative)',
         'array(o.known().stream().filter(a -> !(a instanceof Control.Normal)).toList(), this::alternative)', 'InvokeChecks'),
        ('erase-effects-scope', JSON+'BindingWriter.java', '"includingExternal", v.includingExternal()',
         '"includingExternal", false', 'InvokeChecks'),
        ('drop-must-overwrite', JSON+'BindingWriter.java', 'array(f.mustOverwrite(), this::id)',
         'array(List.<OperandId>of(), this::id)', 'InvokeChecks'),
        ('drop-contract-evidence', JSON+'BindingWriter.java', 'array(c.reference().evidence(), this::id)',
         'array(c.reference().evidence().subList(0, 1), this::id)', 'InvokeChecks'),
        ('erase-contract-uncertainty', JSON+'BindingWriter.java',
         'case Interactions.UnknownContract u -> object("kind", "unknown", "uncertainty", id(u.uncertainty()));',
         'case Interactions.UnknownContract u -> object("kind", "known", "reference", object("authority", "invented", "version", "1", "evidence", array(List.of(new OriginId(u.uncertainty().publication(), "evidence-a")), this::id)));', 'InvokeChecks'),
        ('allow-invalid-ir', JSON+'AirJson.java', 'if (result.status() == ValidationResult.Status.INVALID_IR)',
         'if (false)', 'InvokeChecks'),
        ('allow-generic-incomplete', JSON+'AirJson.java', 'if (result.status() == ValidationResult.Status.INCOMPLETE_VALIDATION)',
         'if (false)', 'InvokeChecks'),
        ('suppress-i56', MODEL+'OperationChecks.java', 'c.obligation("I-56",id,',
         'if (false) c.obligation("I-56",id,', 'InvokeChecks'),
        ('change-goback-bytes', JSON+'BindingWriter.java', 'return object("kind", "return", "header",',
         'return object("kind", "goback", "header",', 'InvokeChecks existing-bytes'),
        ('enum-runtime-authority', JSON+'BindingWriter.java', 'case CONTROL -> "CONTROL";',
         'case CONTROL -> value.name();', 'CodecSuite'),
    ]
    report = {'base': '3bafe3978f0f392e842038ad5628e85dfd91d00d', 'mutations': [],
              'policy': 'Compilation failure is not detection; each mutant must reach an assertion oracle.'}
    with tempfile.TemporaryDirectory(prefix='air-w1b-challenge-') as temporary:
        root = Path(temporary)
        for owner in ('air-model', 'air-json'):
            shutil.copytree(ROOT/owner/'src', root/owner/'src')
        files = sorted([*root.glob('air-*/src/main/java/**/*.java'), *root.glob('air-*/src/test/java/**/*.java'), *root.glob('air-json/src/test/resources/*')])
        before = {str(p.relative_to(root)): sha(p) for p in files}
        report['before_sha256'] = before
        def execute(name, suite, expect_green):
            stage = logs/name; stage.mkdir()
            _, cp = compile_sources(root, root/'build', stage)
            command = ['java', '-ea', '-Xmx1024m', '-cp', cp, 'io.github.gustavo2358.air.json.'+suite.split()[0], *suite.split()[1:]]
            result = subprocess.run(command, cwd=root/'air-json', stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
            log = stage/'test.log'; log.write_text(result.stdout)
            if expect_green:
                if result.returncode != 0: raise RuntimeError(f'{name}: GREEN failed; see {log}')
            elif result.returncode == 0 or 'java.lang.AssertionError:' not in result.stdout:
                raise RuntimeError(f'{name}: no semantic assertion detection; see {log}')
            return {'name': name, 'compile_exit': 0, 'test_exit': result.returncode,
                    'log': str(log.relative_to(args.output.parent)), 'log_sha256': sha(log)}
        report['first_green'] = execute('first-green', 'InvokeChecks', True)
        for name, path, old, new, suite in mutations:
            file = root/path; original = file.read_bytes(); text = original.decode()
            if text.count(old) != 1: raise RuntimeError(f'{name}: expected one mutation site')
            try:
                file.write_text(text.replace(old, new))
                entry = execute(name, suite, False)
            finally:
                file.write_bytes(original)
            entry['restored_byte_exact'] = sha(file) == before[path]
            if not entry['restored_byte_exact']: raise RuntimeError(f'{name}: restoration mismatch')
            report['mutations'].append(entry)
            args.output.write_text(json.dumps(report, indent=2)+'\n')
            print(f'CHALLENGE {name}: DETECTED; compile=0; restored_byte_exact=true', flush=True)
        report['second_green'] = execute('second-green', 'InvokeChecks', True)
        after = {str(p.relative_to(root)): sha(p) for p in files}
        report['restored_byte_exact'] = before == after
        if before != after: raise RuntimeError('Whole source/resource hash restoration mismatch')
    args.output.write_text(json.dumps(report, indent=2)+'\n')
    print('W1B CHALLENGES PASS; second GREEN; byte-exact restoration', flush=True)


if __name__ == '__main__':
    main()
