#!/usr/bin/env python3
"""Opt-in CORE-SIZE probes: real inputs above old limits, in separate bounded-heap JVMs."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent / 'harness'))
from lean import require_local
require_local()

ROOT = Path(__file__).resolve().parents[1]


def run(command, cwd, log):
    result = subprocess.run(command, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    log.write_text(result.stdout)
    if result.returncode:
        raise RuntimeError(f'{command[0]} exit {result.returncode}: {log}')
    return result.stdout


def compile_sources(root, build, logs):
    model, codec, mt, jt = [build / name for name in ('model', 'codec', 'model-tests', 'json-tests')]
    for directory in (model, codec, mt, jt):
        directory.mkdir(parents=True, exist_ok=True)
    for name, owner, source, destination, cp in (
            ('model', 'air-model', 'main', model, None),
            ('codec', 'air-json', 'main', codec, str(model)),
            ('model-tests', 'air-model', 'test', mt, str(model)),
            ('json-tests', 'air-json', 'test', jt, f'{model}:{codec}')):
        files = sorted((root / owner / 'src' / source / 'java').rglob('*.java'))
        command = ['javac', '--release', '21', '-encoding', 'UTF-8', '-Xlint:all', '-Werror']
        if cp:
            command += ['-cp', cp]
        command += ['-d', str(destination)] + [str(p) for p in files]
        run(command, root, logs / f'compile-{name}.log')
    return f'{model}:{mt}', f'{model}:{codec}:{jt}'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    logs = args.output.with_suffix('.logs'); logs.mkdir(parents=True, exist_ok=True)
    report = {'base': 'ce530a7e17ab12b23c48f29425f503ff920b09fb',
              'measurement': 'logical counters only; no heap/RSS measurement or timing SLA',
              'jvm_flags': ['-Xms64m', '-Xmx1536m', '-Xss256k', '-XX:+UseG1GC'],
              'warmup': 'none; correctness probes, each scenario in a fresh JVM', 'probes': []}
    report['java'] = run(['java', '-version'], ROOT, logs / 'java.log').strip()
    with tempfile.TemporaryDirectory(prefix='air-capacity-scale-') as temporary:
        model_cp, json_cp = compile_sources(ROOT, Path(temporary), logs)
        for dimension, main_class, cp, sizes, cwd in (
                ('entities', 'io.github.gustavo2358.air.validation.CapacityChecks', model_cp,
                 [500001, 1000002, 2000004], ROOT / 'air-model'),
                ('json-bytes', 'io.github.gustavo2358.air.json.JsonCapacityChecks', json_cp,
                 [5 * 1024 * 1024, 10 * 1024 * 1024, 20 * 1024 * 1024], ROOT / 'air-json')):
            for n in sizes:
                command = ['java', '-ea'] + report['jvm_flags'] + ['-cp', cp, main_class, str(n)]
                log = logs / f'{dimension}-{n}.log'
                output = run(command, cwd, log)
                lines = [line for line in output.splitlines() if line.startswith('CAPACITY ')]
                if len(lines) != 1 or 'status=STRUCTURALLY_VALID' not in lines[0]:
                    raise RuntimeError('missing capacity oracle output')
                report['probes'].append({'dimension': dimension, 'n': n, 'exit_code': 0,
                                         'counters': lines[0], 'log_sha256': hashlib.sha256(log.read_bytes()).hexdigest()})
                print(lines[0], flush=True)
    args.output.write_text(json.dumps(report, indent=2) + '\n')


if __name__ == '__main__':
    main()
