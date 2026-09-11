# W1B evidence receipts

Baseline: air-java `3bafe3978f0f392e842038ad5628e85dfd91d00d`. [Decision/closure and handoff](../../../quality/cp6-w1b-invoke.md).

- `InvokeOracle.red.java.txt`, `InvokeChecks.red.java.txt` and `W1bRedReceipt.java.txt`: exact test sources for initial REDs; compile alongside main sources extracted from the baseline. Run W1bRedReceipt with no arguments for RED 1 and `policy-red` for the isolated admission RED.
- `red-1-typed.log`, `red-2-typed.log`: failures with typed codes and retained Validator results. Original unwrapped logs and compile exit retained too.
- `red-2-real-mappings.log`: public encode after mappings with original AirJson policy; mappings alone still fail.
- `first-roundtrip-green.log`: first literal/computed GREEN after the minimal admission delta.
- `expanded-checks-first.log`: new-test rule-label mismatch, subsequently corrected to the pre-existing I-49; no production change to that rule.
- `baseline-hashes.json`, `preserved-model-and-goldens.json`: exact hashes and preservation result for all model/Validator sources and existing goldens.
- `independent-run-1/`, `independent-run-2/`, logs and `independent-runs.json`: two fresh JVM executions, complete generated canonical bytes, hashes and equality. Generated evidence is not expected fixture authority.
- `challenge-first-run.log`, `challenges.logs/`: initial challenges; old-wire mutation failed physically before the golden assertion and was not counted. Final challenge report/logs preserve the replay with direct byte assertion and final test sources.
- Local gate logs/JSON identify the observed HEAD at run time (baseline plus dirty authorized diff); they do not claim that baseline commit alone contains W1B. Exact pushed HEAD and CI checkouts/trees are recorded on the PR after creation.

The raw original evidence is preserved; no builds, downloaded dependencies or toolchains are versioned. performance-local.json records UNAVAILABLE, not PASS. Sibling E2E/consumer integration is outside this wave.

The raw Maven DNS failure log is stored as `full-second-maven-dns-failure.log.gz`; its `.sha256.json` records original and compressed hashes. Lossless decompression was verified. Original trailing whitespace was preserved, not edited to pass diff checks.
