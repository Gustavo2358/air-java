# W2C raw evidence

[Work item](../../active/WORK-AIR-JSON-004/work-item.json) and [qualification report](../../../quality/cp6-w2c-transport.md).

- `red-A.log` through `red-E.log`: future-positive failures against unchanged main mappings; original model/wire/test Java sources saved with `.red.java.txt` suffix.
- `red-typed.log`: independent replay against git-archived main, codes/paths/empty issues; `W2cRedReceipt.java.txt` is the exact probe.
- `green-bool.log`, `red-public-branch.log`, `red-public-unknown.log`: BOOL-only stage, before Branch/Unknown mappings.
- `first-forms-green.log`: all new isolated and complete form cycles.
- `challenges.json` and `challenges.logs/`: 23 compilable mutations, independent assertions, source/oracle hashes, exact restoration and second GREEN. Validator mutants exist only in disposable copies.
- `deterministic-processes.json`, `process-1.log`, `process-2.log`: seven profiles exported by two fresh JVMs.
- `w1-baseline-hashes.json`: six exports from git-archived main, reproduced byte-exact with current codec. `W2cBaselineProbe.java.txt` preserves the probe.
- `frozen-e2e-wires.json` and `.log`: read-only CP3/CP4E/CP5 frozen files; no downstream execution.
- `admin-fast-initial.log` preserves the initial broken lifecycle link failure; `admin-fast.log` is the successful corrected run. RED logs and historical files were not rewritten to obtain PASS.

Build caches, compiled classes/JARs and toolchains are not evidence and are not committed. Final CI receipt is attached to the Draft PR for its exact HEAD, not retroactively claimed by this pre-push commit.

Final local receipts: `transport.log`, `git.log`, `full-qualified.log`/`.json`, `self-review.json`, and `siblings.json`. The first Maven DNS failure remains in `full-initial-network-failure.json`. Full gate was rerun successfully; subsequent changes are documentation/evidence only.

The original failed Maven log includes trailing spaces emitted by Maven. It is stored losslessly as a UTF-8 JSON string with its original SHA-256 so the repository whitespace gate can check source files without rewriting evidence. Decoding `raw_utf8` reconstructs the exact original bytes; the failure is preserved.
