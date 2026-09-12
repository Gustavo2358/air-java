"""Offline model/codec contracts and module boundaries; expensive campaigns local."""
import shutil
import subprocess
import sys
from lean import validate_pins, require_local

LOCK = 'docs/sources.lock.json'
PINS = ('Gustavo2358/analysis-ir',)


def pin_errors(root):
    return validate_pins(root, LOCK, PINS)


def copy_pin_fixture(source, destination):
    target = destination / LOCK
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source / LOCK, target)


def break_pin_fixture(root):
    (root / LOCK).write_text('{}')


def technical_fast(root):
    # Offline compile + model suite + codec suite + javap/jdeps/module ownership.
    subprocess.run(['bash', 'scripts/check.sh'], cwd=root, check=True)
    subprocess.run([sys.executable, '-B', '-m', 'unittest', 'discover', '-s', 'scripts/harness/tests', '-v'], cwd=root, check=True)


def full_local(root):
    require_local()
    import contracts
    print(contracts.check(root, maven=True))
