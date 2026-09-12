# CP6 W2C formal closeout

CP6 W2C — APPROVED / MERGED / CLOSED. WORK-AIR-JSON-004 uses lifecycle token `completed`.
The user authorized this closeout after human review of the exact qualified source.
No implementation was added during closure.

| Identity | Commit | Tree |
| --- | --- | --- |
| Reviewed main base | `2a37f5e980ba25fdc79614a66030a84d8bf5b8c9` | `8d248f4ccf207eb7b609aa9ff0cdbcd512e526c8` |
| W2C qualified source | `edad3f3eb954ae38d56975f775679e3f1160eb26` | `ab3a55e13b781ec5424356e83bddbc6fde42dcb1` |
| W2C product merge / main immediately after merge | `1d22068e9d9c1d100ecdef734e5b6995252e7ede` | `ab3a55e13b781ec5424356e83bddbc6fde42dcb1` |

PR [#10](https://github.com/Gustavo2358/air-java/pull/10) is MERGED. Merge method `merge`, with
`expected_head_sha=edad3f3eb954ae38d56975f775679e3f1160eb26`, at `2026-09-12T14:38:46Z`.
Parents are the reviewed main base followed by source HEAD. **QUALIFIED_TREE_PRESERVED**.
The preflight found exactly the two approved commits (W1B administrative closeout and
W2C source), no new findings/review threads and no auto-merge configuration. Ready did
not change source content. No rebase, squash, force-push or branch deletion occurred.

The subsequent archival commit changes only documentation/lifecycle and MANIFEST.
Its SHA/tree, resulting current main and final administrative fast/scope receipts are
recorded on PR #10 and in the final handoff after commit, following the repository's
no-future-SHA rule. Product merge authority above remains distinct from that current
main. Source Full Qualification is not claimed on the administrative tree.

## Delivered transport and preserved semantics

- Jump transport = delivered: Header and explicit destination use full IDs; no fallthrough inference.
- Branch transport = delivered: predicate, trueDestination and falseDestination remain exact; arms are neither swapped nor sorted.
- known(bool) transport = delivered within the existing closed text/bool table.
- Unknown transport = delivered: header, typeRef, dependencies in order, remainingReads and reason remain exact. Unknown remains Unknown; no expression evaluation or default literal.
- DisjointStorage wire = delivered: PremiseId, authority, justification, OriginId and every StorageId remain exact and ordered. The codec transports an upstream assertion; it does not prove or infer disjointness. Distinct StorageIds are not a proof of physical separation.

Only `air-json/src/main/java/io/github/gustavo2358/air/json/BindingReader.java` and
`BindingWriter.java` changed product behavior relative to the reviewed main base.
Their Git blobs at the source and merge are respectively
`7758fa7a3005576205fa8f6e2f600028a6dc0081` and `40a026483ece1e82c59c1b11fed22181bc088e94`.
No model, Validator, AirJson, physical JSON, POM, normative pin or protected oracle change
was introduced after review. AIR 2.0.0 / binding 1.0.0 DRAFT and analysis-ir pin
`51b4d9a8ae0364232bd97103cd73a77e1a34996c` remain unchanged.

Encode/decode PASS does not mean semantic proof discharged. I-09 and I-59 remain
SEMANTIC_OBLIGATION, and W1/W2 composition preserves I-56. A semantic obligation alone
remains nonblocking solely for transport; INVALID_IR, RESOURCE_LIMIT,
UNSUPPORTED_CAPABILITY and non-obligation incomplete validation/VALIDATION_LIMIT remain
blocking under the existing policy. No obligation is recorded as resolved.

SameDomain, general expression codec, BoolValue literal, broader types, COBOL, SP
decoder, lower, CFG, dataflow and dependency analysis remain outside this delivery.

## Evidence and administrative validation

- [Original qualified CI receipt](qualified-ci-receipt.json) is preserved as recorded before review.
- [Fresh preflight](preflight.json) confirms runs 34697847172 / 34697847176 at exact source HEAD and 34697894128 / 34697894123 at synthetic merge, all SUCCESS. Raw log hashes were rechecked against the reviewed receipts.
- Synthetic merge `8117964f745e4f201f93e7710b21c31660830c3d` has ordered parents base/source above and tree `ab3a55e13b781ec5424356e83bddbc6fde42dcb1`: SYNTHETIC_MERGE_IDENTICAL_TREE.
- [Immediate pre-merge check](pre-merge-check.json) rechecks HEAD/tree, current base, four final runs, Ready and auto_merge=null.
- [Product merge receipt](product-merge-receipt.json) records remotely confirmed merge and local fast-forward/tree/blob verification.
- [Post-merge fast](post-merge-fast.json) PASS, exit 0: docs plus 86 harness tests on the unchanged product merge tree. No local full, Maven, transport, challenge or performance rerun was needed.
- Historical REDs, initial failed Maven DNS attempt, challenge/mutation logs, second GREEN, deterministic JVM, N/2N probes, W1 byte-exact and CP3/CP4E/CP5 frozen wires remain unchanged in the [original evidence](../README.md). Performance remains UNAVAILABLE, never promoted to PASS.

Final administrative validation checks documentation-only scope/merge ancestry and
unchanged product, tests, oracles, pins and historical evidence. Its local receipt is
reported separately on the PR; it is not a new Full Qualification.

## Frozen handoff

W2C PRODUCT BASELINE — FROZEN at the product merge SHA/tree above.
Future CP6 W2B — cobol-lower structural IF assembly may consume SP 1.4.0, create explicit
Branch(Unknown BOOL), Assign/Jump arms converging on Invoke (including the open false
path), and translate source-derived IndependentStorageSet into Premise(DisjointStorage).
Choosing between the product merge and later documentary main for the cobol-lower pin
belongs to a future authorized W2B task. No repin or preparation is performed here.

W2B NOT_STARTED / NOT_AUTHORIZED. W2D NOT_STARTED / NOT_AUTHORIZED.
All five sibling repositories remain outside this closeout; existing unrelated files
are preserved. The final before/after status comparison is reported on PR #10.
