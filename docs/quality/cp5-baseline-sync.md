# Normative baseline synchronization after CP5

WORK-AIR-BASELINE-001 starts at air-java `17029898fd0ee8fabcaaae89f7260148633d4b12`.
Analysis-ir advances from `122ce54e1b9ef9b00646f93ece409ca8b63bc933` to
`51b4d9a8ae0364232bd97103cd73a77e1a34996c`, a descendant with three commits.
Only README and informative bindings/checkpoint-0b-mvp.md changed. Every specification,
conformance and binding file consulted by the lock is byte-identical; [hash matrix](cp5-baseline-sync.json).

No production, public API, model, validator, codec, wire or capacity policy changes.
Canonical CP4E means the same Object/Cell/Assign literal/Return/PARTIAL facts.
Historical source receipts and evidence retain the SHA actually executed; current
normative lock, transport policy guard and current explanatory docs use the new SHA.
The capacity work is archived based on the observed PR7 merge, preserving its evidence.

Validation: fast (86 harness tests), architecture, semantic (179+79 checks), transport, Maven clean verify, full, Git/scope and diff-check PASS. Self-review found no blocker. Exact-head remote CI remains pending until push. CP6 remains NOT_STARTED / NOT_AUTHORIZED. This work does not
claim synchronized downstream E2E or 117k qualification. W3 debts belong downstream.
