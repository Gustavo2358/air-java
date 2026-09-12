# CP6 W2C — transport qualification

Historical pre-push qualification state: W2C IMPLEMENTED / LOCALLY_QUALIFIED / AWAITING_HUMAN_REVIEW. Remote CI is the final pending qualification at commit time; its receipt is recorded on the Draft PR. Human approval and merge are not claimed.
WORK-AIR-JSON-004; branch `feat/cp6-w2c-if-air-json`. [Completed lifecycle](../work/history/WORK-AIR-JSON-004.md),
[exact transport profile](../engineering/air-json.md#cp6-w2c--controle-explícito-unknown-e-disjointstorage),
[raw evidence](../work/evidence/WORK-AIR-JSON-004/README.md).

Current lifecycle: **APPROVED / MERGED / CLOSED** after authorized PR #10 merge.
[Subsequent closeout receipt](../work/evidence/WORK-AIR-JSON-004/closeout/README.md) preserves the source/product/admin distinction; the qualification below remains historical.

## Baselines and authority

- Main/product baseline: `2a37f5e980ba25fdc79614a66030a84d8bf5b8c9`; tree `8d248f4ccf207eb7b609aa9ff0cdbcd512e526c8`. Origin `git@github.com:Gustavo2358/air-java.git`; initial working tree clean. Fetched and fast-forwarded main without discarding work.
- W1B PR9 remotely confirmed MERGED. Administrative commit `6b5fc6bdc22949d55418974f335a7735198867c8`, tree `324103e5c4516ecadeb4fc5e766c2c0e76c0859f`, archives JSON-003 as completed (APPROVED / MERGED / CLOSED). Documentation/lifecycle only, fast PASS. Lifecycle permits this on the next authorized maintenance; no separate main commit required. Product W2C is not mixed into closeout.
- AIR authority: `Gustavo2358/analysis-ir@51b4d9a8ae0364232bd97103cd73a77e1a34996c`. AIR 2.0.0; analysis-ir-json bindingVersion 1.0.0 DRAFT. Source lock unchanged.
- W2A context, read only: proleap-poc PR35 merged at `4ffabded1aad39316b8a6f337f732976fdb3ca3e`; tree `5880e174b33c85ba3f3cdbc70bd2d8dc7b1d567b`, SP 1.4.0. Later closeout only changes the contract's work-item link. BOOLEAN/PURE/TOTAL, truth UNKNOWN, complete whole-item reads, IF completion and source-derived IndependentStorageSet explain future producer needs; they do not define codec wire syntax.

## Product and symmetry

Only BindingReader.java and BindingWriter.java change product behavior. Explicit mappings add Jump, Branch, known(bool), Unknown, and Premise(DisjointStorage). Header, IDs, role, origin, coverage, precision, uncertainties, arm identity, dependencies, remainingReads, reason, premise text and ordered members are preserved. Unknown dependencies use iterative postorder frames; no JVM recursion, inventory joins or generated facts. Existing none/within visible/all memory bounds are reused. SameDomain and other unsupported forms remain explicit limits.

No AIR model, Validator, AirJson admission, physical Json parser/writer, API, source lock, version, POM, dependency, CI workflow or resource-golden changes. No source-language input or Semantic Product decoder. No lower, CFG, predicate calculation, dataflow, dependency resolver, storage inference or extension.

The model fixture is independent of wire. The complete wire oracle uses only literal binding facts and physical JSON constructors; it never invokes reader/writer or the model oracle. Encode equals independent wire bytes; independent wire decodes to the exact Publication; model→encode→decode→encode is byte-identical for seven profiles (including empty Unknown dependencies). Full object equality checks all records and arrays. Composition covers W1 Assign(Text), literal/computed Invoke, open effects/outcomes and contracts.

## RED → GREEN → challenge

Five baseline REDs preceded product edits; [typed receipt](../work/evidence/WORK-AIR-JSON-004/red-typed.log) records IMPLEMENTATION_LIMIT with no fabricated AIR issues. A Jump: `$.sequence.terminator`; B Branch: same mapping path, using model-valid Read of BOOL Cell; C known(bool): `$.typeRef`; D Unknown BOOL with Read: `$.expression`; E Premise: `$.publication.premises`. B/D use isolated mapping probes to avoid transitive gaps masking the target. After BOOL alone went GREEN, public Branch and Unknown round-trips still failed independently at their own mappings, preserved in separate RED logs. Original future-positive test sources are retained.

First form GREEN, then 150 reader negative cases (with symmetric writer checks for materializable invalid AIR), all seven complete wire profiles, W1 composition and semantic-obligation oracles. Negative partitions cover missing/foreign references, existing foreign Unit destinations, role/type/owner, operation placement, duplicate/foreign PremiseId, nonexistent origin/storage, wrong ID domain, malformed/empty/single/duplicate members, unsupported SameDomain, unknown assertions, required/extra fields and invalid/blank premise text. Codes and AIR rules are explicit; no Java message classification.

23 compilable mutations detected with immutable oracles and whole-source hash restoration, followed by second GREEN. Includes coordinated reader+writer arm swap, Jump token, predicate/BOOL/dependency/open remainder/reason/Unknown loss, premise/member/order/domain loss, permissive SameDomain/unknown assertion, asymmetric reader/writer coverage and stronger coverage metadata. I-09/I-59 suppression mutants affect disposable Validator copies only; production Validator remains byte-exact. These tests prove the exercised falsifications, not an independent implementation or complete AIR binding qualification.

I-09 and I-59 remain SEMANTIC_OBLIGATION with identical rule/subject/detail/count before and after transport. Composition retains I-56. Retention truncation preserves obligation totals. Existing W1B tests keep INVALID_IR, RESOURCE_LIMIT, unsupported capabilities and non-obligation incomplete validation blocking. Transport success does not discharge producer obligations.

## Determinism, compatibility and scale

Two fresh JVM exports for all seven profiles are byte-identical. Six unmodified-main exports (GOBACK, scalar Assign and four Invoke combinations) have frozen SHA-256 regressions. Frozen CP3/CP4E/CP5 AIR JSON files also decode/re-encode byte-exact; no producers/consumers were rerun. Original GOBACK and scalar resources unchanged.

| Independent dimension | N / 2N | bytes | physical nodes | entities | operations | operands | domainQueries |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Branch/Jump pairs | 256 / 512 | 1054820 / 2092644 | 60420 / 119812 | 1316 / 2596 | 516 / 1028 | 259 / 515 | 0 / 0 |
| Unknown dependencies | 256 / 512 | 180900 / 344996 | 10463 / 19935 | 546 / 1058 | 4 / 4 | 513 / 1025 | 0 / 0 |
| Premises | 256 / 512 | 136886 / 256694 | 6638 / 12270 | 291 / 547 | 4 / 4 | 3 / 3 | 0 / 0 |
| DisjointStorage members | 256 / 512 | 130694 / 244102 | 7420 / 13820 | 292 / 548 | 4 / 4 | 3 / 3 | 0 / 0 |

Cardinality growth is compared after subtracting constant fixture overhead. Counters are the existing Validator observations, not counts of every reference lookup. Source inspection confirms codec lists traverse once without destination/reason/storage inventory scans. IDs/text serialization and retained diagnostic paths add costs; there is no timing SLA, general asymptotic proof, streaming or unbounded-heap claim. 600 nested Unknown occurrences also pass with `-Xss256k`. Dedicated performance gate was executed and returned UNAVAILABLE (exit 3).

## Gates and review boundary

Executed: `python3 -B scripts/harness/run.py fast`, `architecture`, `semantic`, `transport`, `git --work WORK-AIR-JSON-004`, and `full` all PASS (exit 0). Full includes 86 harness tests, 179 model + 108 transport checks, and Maven clean verify. The first full failed only at Maven DNS resolution; its raw log remains, and the complete gate was rerun successfully with authorized network. `performance` was explicitly executed: UNAVAILABLE, exit 3. Integration gate was not run; no W2 E2E claim.

Focused W2C suite, independent wire oracle, 23 challenge mutations plus second GREEN, two independent JVM exports and historical frozen-wire probes all PASS. Final product/oracle hashes equal the challenged snapshot. The [self-review receipt](../work/evidence/WORK-AIR-JSON-004/self-review.json) confirms protected surfaces unchanged. Sibling HEAD/tree/status match the initial observations, including preexisting untracked evidence directories in artefatos-e2e.

Remote CI must identify final source HEAD and base plus synthetic merge SHA/tree; it is not inferred from local gates. Exact commit/PR and CI receipts belong on the PR, avoiding a self-referential commit. After documentation-only handoff updates, fast and git are rerun; remote CI then checks the committed tree.

Self-review scope: mapping authority, closed fields and typed diagnostics, iterative dependency order, atomic failure on mixed premise inventories, unchanged semantic-obligation policy, symmetry, immutable independent oracles, mutation restoration, W1 bytes, cardinality counts, dependency/POM boundaries and sibling status. This is self-review, not independent human review.

## Remaining downstream gaps

No remaining codec gap identified for the authorized W2 positive profile. Future W2B must translate SP facts into model occurrences and complete IDs/origins, retain BOOLEAN/PURE/TOTAL authority without evaluating truth, preserve known reads/completeness, materialize source-derived storage members exactly, and emit explicit Branch/Jump destinations. SP decoding and those producer decisions remain outside air-java. W2D consumer work is separate. SameDomain, general expressions, boolean literals and broader scopes are not implicitly authorized.

W2B NOT_STARTED / NOT_AUTHORIZED. W2D NOT_STARTED / NOT_AUTHORIZED.
Historical handoff boundary: stop at Draft PR for human review, without merge or auto-merge. The subsequently authorized closeout is recorded separately above.
