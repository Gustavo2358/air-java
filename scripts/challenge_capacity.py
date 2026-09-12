#!/usr/bin/env python3
"""Compilable CORE-SIZE mutants in a disposable copy, with byte-exact restoration and two GREENs."""
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

MODEL = 'air-model/src/main/java/io/github/gustavo2358/air/validation/'
JSON = 'air-json/src/main/java/io/github/gustavo2358/air/json/'


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    logs = args.output.with_suffix('.logs'); logs.mkdir(parents=True, exist_ok=True)
    mutations = [
        ('entities-default-2M', MODEL+'PublicationIndex.java',
         'if(identities.size()>=context.options.maximumEntities())', 'if(identities.size()>=2_000_000)',
         'model', ['2000004'], 'CORE-SIZE entities 2000004 INCOMPLETE_VALIDATION'),
        ('document-default-16MiB', JSON+'AirJson.java',
         'new Limits(Integer.MAX_VALUE, Integer.MAX_VALUE)', 'new Limits(16 * 1024 * 1024, Integer.MAX_VALUE)',
         'json', ['20971520'], 'Output byte limit exceeded'),
        ('exhaustion-success', MODEL+'AirValidator.java',
         'c.resourceLimit(limit.getMessage());', '// mutation: exhaustion silently accepted',
         'model', [], 'exhaustion cannot succeed'),
        ('exhaustion-unsupported', MODEL+'ValidationContext.java',
         'count(ValidationIssue.Kind.RESOURCE_LIMIT);', 'count(ValidationIssue.Kind.UNSUPPORTED_CAPABILITY);',
         'model', [], 'retained issues exceed total'),
        ('retention-aborts-work', MODEL+'ValidationContext.java',
         '        count(kind);', '        if(issues.size()>=options.maximumIssues()) throw new Limit("diagnostic stop");\n        count(kind);',
         'model', [], 'late invalidity after retained obligation'),
        ('completion-marker-removed', MODEL+'ValidationContext.java',
         'traversalCompleted=false;', '// mutation: missing incomplete traversal marker',
         'model', [], 'exhaustion must mark incomplete work'),
        ('resource-issue-removed', MODEL+'ValidationContext.java',
         'issues.add(new ValidationIssue(ValidationIssue.Kind.RESOURCE_LIMIT,"ANALYSIS_LIMIT",\n                Optional.of(index.publication.id()),message));',
         '// mutation: operational diagnostic removed',
         'model', [], 'retained resource marker'),
        ('resource-as-implementation-limit', JSON+'Json.java',
         'new AirJsonException(RESOURCE_LIMIT, path, message)', 'new AirJsonException(IMPLEMENTATION_LIMIT, path, message)',
         'json', [], 'expected RESOURCE_LIMIT, got IMPLEMENTATION_LIMIT'),
        ('codec-resource-as-unsupported', JSON+'AirJson.java',
         'new AirJsonException(RESOURCE_LIMIT, "$", "AIR validation operational budget exhausted", result)',
         'new AirJsonException(UNSUPPORTED_CAPABILITY, "$", "AIR validation operational budget exhausted", result)',
         'json', [], 'expected RESOURCE_LIMIT, got UNSUPPORTED_CAPABILITY'),
        ('unretained-kind-lost', MODEL+'ValidationResult.java',
         'return diagnostics.count(kind)>0;', 'return issues.stream().anyMatch(i -> i.kind()==kind);',
         'model', [], 'late invalidity after retained obligation'),
        ('incomplete-empty-success', MODEL+'ValidationResult.java',
         '!diagnostics.traversalCompleted() || ', '',
         'model', [], 'missing traversal completion must not succeed'),
        ('reference-global-scan', MODEL+'ValidationContext.java',
         'index.identities.contains(id)', 'index.identities.stream().anyMatch(id::equals)',
         'model', [], 'global inventory traversal per reference'),
    ]
    # Change both operational count and marker to challenge the classification, not constructor consistency.
    report = {'base': 'ce530a7e17ab12b23c48f29425f503ff920b09fb', 'mutations': []}
    with tempfile.TemporaryDirectory(prefix='air-capacity-challenge-') as temporary:
        root = Path(temporary)
        for owner in ('air-model','air-json'):
            shutil.copytree(ROOT/owner/'src', root/owner/'src')
        sources = sorted(p for p in root.rglob('*') if p.is_file())
        originals = {str(p.relative_to(root)):digest(p) for p in sources}
        report['source_sha256'] = originals
        build = root/'classes'
        compile_logs = logs/'initial'; compile_logs.mkdir(exist_ok=True)
        cp = dict(zip(('model','json'),compile_sources(root,build,compile_logs)))
        def test(which, arguments, label, expected=None):
            owner = 'air-model' if which=='model' else 'air-json'
            main_class = 'io.github.gustavo2358.air.' + ('validation.CapacityChecks' if which=='model' else 'json.JsonCapacityChecks')
            command = ['java','-ea','-Xms64m','-Xmx1536m','-Xss256k','-XX:+UseG1GC','-cp',cp[which],main_class]+arguments
            result = subprocess.run(command,cwd=root/owner,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
            log=logs/(label+'.log'); log.write_text(result.stdout)
            if expected is None:
                if result.returncode: raise RuntimeError(f'GREEN failed: {log}')
            elif result.returncode==0 or expected not in result.stdout:
                raise RuntimeError(f'mutant survived or wrong RED: {log}')
            return {'exit_code':result.returncode,'log_sha256':digest(log),'oracle':expected}
        for which in ('model','json'): test(which,[],'initial-'+which)
        for name,relative,before,after,which,arguments,expected in mutations:
            target=root/relative; original=target.read_bytes(); text=original.decode()
            if text.count(before)!=1: raise RuntimeError(f'non-unique mutation target: {name}')
            green=test(which,arguments,name+'-green')
            try:
                mutated=text.replace(before,after)
                if name=='exhaustion-unsupported':
                    mutated=mutated.replace('new ValidationIssue(ValidationIssue.Kind.RESOURCE_LIMIT,"ANALYSIS_LIMIT",',
                                            'new ValidationIssue(ValidationIssue.Kind.UNSUPPORTED_CAPABILITY,"ANALYSIS_LIMIT",')
                    expected='exhaustion kind'
                target.write_text(mutated)
                mutation_hash=digest(target)
                step_logs=logs/name; step_logs.mkdir(exist_ok=True)
                cp=dict(zip(('model','json'),compile_sources(root,build,step_logs)))
                red=test(which,arguments,name+'-red',expected)
            finally:
                target.write_bytes(original)
            if digest(target)!=hashlib.sha256(original).hexdigest(): raise RuntimeError('restore mismatch')
            restore_logs=logs/(name+'-restore'); restore_logs.mkdir(exist_ok=True)
            cp=dict(zip(('model','json'),compile_sources(root,build,restore_logs)))
            second=test(which,arguments,name+'-second-green')
            report['mutations'].append({'name':name,'file':relative,'compiled':True,'green':green,'red':red,
                'mutant_sha256':mutation_hash,'restore_byte_exact':True,'restored_sha256':digest(target),'second_green':second})
            print('KILLED '+name+'; compiled, expected RED, byte-exact restore, second GREEN',flush=True)
        report['all_sources_restored'] = all(digest(root/name)==sha for name,sha in originals.items())
        if not report['all_sources_restored']: raise RuntimeError('source snapshot not restored')
        for which in ('model','json'): test(which,[],'final-'+which)
    args.output.write_text(json.dumps(report,indent=2)+'\n')


if __name__=='__main__':
    main()
