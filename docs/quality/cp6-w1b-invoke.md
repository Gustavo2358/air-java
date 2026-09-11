# CP6 W1B — Invoke transport

## Authority and scope

air-java only, baseline `3bafe3978f0f392e842038ad5628e85dfd91d00d`, tree `201096ee137a897fb43b22e4c831d82e948d6f81`. Clean main and fetched origin/main matched before branch creation. Initial fetch hit local SSH config permissions; explicit `ssh -F /dev/null` with authorized network access succeeded without changing config.

W1A [PR34](https://github.com/Gustavo2358/proleap-poc/pull/34) confirmed merged at `53d774026a1e4bcd969c7783a1d277aaa87b5f2f`, 2026-09-11 14:40:07 UTC; head `bd6dd1c49bd724899a599beeff7dd3cb66d0f879`; SP 1.3.0 read-only handoff. Discovery [PR17](https://github.com/Gustavo2358/analysis-cfg/pull/17) confirmed merged at `c39a92f930b1c693857a0b30a1f5155f3f81520c`. Consulted exact merged PR head `b3e0a1b2b1978805d156172ee1283de0094371cc`: architecture discovery, gap matrix and future waves/oracles. Local sibling main is stale and was not changed. Normative AIR 02/04/05 and binding §§7–10 read at `51b4d9a8ae0364232bd97103cd73a77e1a34996c`.

## Transitive closure decided before implementation

| Node / variant | Baseline | W1B decision |
| --- | --- | --- |
| Operations.Invoke, action Text, Header | Header supported; Invoke missing | required; preserve action verbatim |
| LiteralTarget / ComputedTarget | missing | required; category, namespace, name, policy, origin |
| InternalTarget / EntrySignature | missing | not required, reject explicitly |
| ExactName / UnknownName | missing | required / small generic extension with own test |
| ExtensionName | missing | not required; no capability negotiation |
| Expressions.Read / Literal(TextValue) | Read missing / Literal supported | Read required; reuse Literal |
| Places.ObjectPlace, Operand.Header | supported | reuse, including effectOperands |
| TypeRef known(text), Object/Cell | supported | reuse; no duplicated TypeRef on Read/Place |
| ExternalSignature / Signature | wrapper missing; closed empty signature supported | wrapper required; zero known parameters/results |
| UnknownBound.none / unknown | none supported / unknown missing | preserve both; unknown needed for independent incomplete-validation oracle |
| Parameter / ResultSlot, arguments / results content | missing | not required; empty argument/result arrays only |
| EffectBound / ForeignEffects | missing | required; otherwise, reads/writes/mustOverwrite; perOutcome empty |
| MemoryBound none/within; VisibleMemory/AllMemory | missing | required conservative scopes; preserve booleans |
| ObjectsMemory/StorageMemory/MemoryUnion; perOutcome | missing | not required, explicitly unimplemented |
| InvocationOutcomes finite alternatives | missing | normal, exceptional tag, any exception, halt, diverge; Handler/Propagate |
| ControlBound none/within; UnitControl/AllControl | missing | required open remainder; every flag preserved |
| LabelsControl/ControlUnion | missing | not required, explicitly unimplemented |
| KnownContract / UnknownContract | missing | both required; evidence origins/uncertainty identity |
| Written/Derived origins; IDs, claims, coverage, uncertainties | supported | reuse without substitution; distinct origins by role |
| Contractual/Unavailable origins; premises with content | missing | not required; not in fixtures |
| TrimRight/FitText, other operations/expressions/places | missing | not required; no runtime name interpretation |

Mappings are explicit, linear in mapped document size, with no inventory scan per Invoke. Signature remainder support also enables a real VALIDATION_LIMIT return fixture; it does not implement argument/result slots. No normative/model/Validator semantic change, AIR or binding version bump, POM/dependency change, assessed wrapper or parallel public API.

Transport policy impact: BREAKING for callers relying on semantic obligations causing failure; source API unchanged. Consumers needing assessment run AirValidator.validate(decoded). W1C/W1D migrate later under their own authorization. Existing supported Publication bytes must remain identical.

## Evidence

Work lifecycle: [JSON-003](../work/active/WORK-AIR-JSON-003/state.md). Raw receipts are under [evidence](../work/evidence/WORK-AIR-JSON-003/README.md).

| Stage | Observed evidence |
| --- | --- |
| RED 1, baseline codec | STRUCTURALLY_VALID, complete traversal, I-56; encode fails IMPLEMENTATION_LIMIT at sequence.terminator, exit 1 |
| RED 2, isolated policy | Same Publication; actual private AirJson.validate invoked after test-only mapping bypass, INCOMPLETE_VALIDATION, exit 1 |
| RED 2, real mappings | Mapping-only production delta with original AirJson policy; real public encode still INCOMPLETE_VALIDATION, exit 1 |
| First GREEN | Remove only semantic-obligation refusal; literal/computed full record equality and canonical bytes, I-56 unchanged |
| Directed validation | INVALID_IR encode/decode preserve full result; RESOURCE_LIMIT remains operational; unsupported capability blocked; actual VALIDATION_LIMIT on a shared Return with open result inventory remains INCOMPLETE_VALIDATION, including when retained issues contain only I-56 |
| Physical grammar | Required/unknown/null/wrong-type fields and direct/escaped duplicate keys challenged through invocation subtrees; unknown variants rejected; empty closed outcomes and empty contract evidence retain explicit AIR rules |
| Regression | Existing model/Validator sources and both golden files byte-identical to baseline hashes; 179 model and 91 transport checks PASS |
| Challenges | Thirteen compilable semantic mutations, restored source/resource hashes and second GREEN; no production source mutated in this checkout |
| Maven | Canonical gate executed Maven clean verify, PASS, 179 model + 91 transport checks |
| Performance | 500 Invokes: 1,673,561 bytes / 1,026 entities / 1,000 obligations. 1,000 Invokes: 3,336,061 bytes / 2,026 entities / 2,000 obligations. Existing N/2N/capacity probes also run in semantic/transport. Dedicated performance gate remains UNAVAILABLE (exit 3), not PASS |

Two fresh JVM runs of the final checks produced identical files: literal 15,252 bytes,
SHA-256 `f8a9bfc24e94fb9f4d60120419bc444072780458c08366587f8d95fd745bc378`;
computed 15,256 bytes, SHA-256 `f694780ce0a928663f8c1b679a282c79a3afdb0f51ca38e87ef13b275a595c98`.
These generated outputs are evidence, not the independent wire oracle. The wire
oracle is explicitly authored from binding fields in InvokeChecks, while InvokeOracle
uses only public model records. Neither oracle reads these generated outputs.

The initial expanded test used the wrong expected rule for a contract uncertainty
code mismatch (I-31); source/authority routing identified the existing rule I-49.
Only the new test expectation changed; the failing log is preserved. The first
full run exposed a dangling link when archiving BASELINE-001; fixed within lifecycle
scope. The second reached Maven and failed DNS in the sandbox; the authorized
network retry passed. No expected golden or product semantic rule was changed.
The first old-wire mutant reached an INPUT_ERROR before the golden assertion;
it was not counted as semantic detection. The replay targets the independent
GOBACK byte assertion directly. Both logs remain preserved.

## W1C handoff and boundaries

The exact [transport profile](../engineering/air-json.md#cp6-w1b--perfil-de-transporte-para-w1c)
is authoritative for implementation coverage. A correctly constructed Publication
within that profile no longer encounters Invoke IMPLEMENTATION_LIMIT or an I-56-only
transport refusal. Structural errors, capabilities, operational limits and
non-obligation incomplete validation still refuse the whole operation.

No body/target resolution, source-language semantics, SP decoder, lowering, CFG,
solver/dataflow, dependency resolver or runtime name canonicalization. W1A is
APPROVED / MERGED. W1B stops at IMPLEMENTED / AWAITING_HUMAN_REVIEW in a Draft PR.
W1C/W1D/W2 remain NOT_STARTED / NOT_AUTHORIZED. Siblings, normative AIR and all
versions remain unchanged. Remote HEAD/tree/CI receipt is recorded on the PR,
not self-inscribed in the commit being checked. No merge or auto-merge.



## Final local gate and frozen-wire checks

Final `python3 -B scripts/harness/run.py full`: PASS (docs, 86 harness tests,
architecture, 179 model + 91 transport via scripts/check.sh, Maven clean verify).
No unexecuted stage. [Raw full receipt](../work/evidence/WORK-AIR-JSON-003/full-final-local.json).
Git/scope PASS. Dedicated performance reports UNAVAILABLE (exit 3).

Read-only decode/encode of the frozen CP5 `cp4e-a`, `cp4e-b`, `cp3` and
`generic-overwrite` AIR files also returned byte-identical output. Their original
hashes were first matched to the pre-CP6 receipt, whose component SHAs are preserved
in [the frozen-wire receipt](../work/evidence/WORK-AIR-JSON-003/cp5-frozen-wires.json).
Historical CP3 and both CP4E original files passed as well. No producer, lower, CFG
or dataflow was rerun; this proves codec regression on those exact files only.

Self-review: explicit wire discriminators and strict fields match the frozen binding;
no semantic repair, additional model traversal per Invoke, extra dependencies or
unrelated product changes found. This is self-review, not independent human approval.
