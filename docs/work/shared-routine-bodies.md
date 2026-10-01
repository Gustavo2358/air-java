# Shared routine bodies — stage 5

- id: SHARED-ROUTINE-BODIES
- title: Share routine representation while retaining call, return and data contexts
- status: DONE
- scope: Stage 5 authorized on 2026-09-29; separate from the merged values/control campaign. This first PR implements the AIR JSON transport prerequisite only.

Merged in [AIR #23](https://github.com/Gustavo2358/air-java/pull/23) on 2026-10-01.
[Current integration and codec qualification](air-codec-latency.md).
The discovery and checkpoint sections below retain their original scope.

## Discovery and authority

The remote main baseline is frontend `4c00dea2a6bad1ba21076e55681f6038b80f8a47`, lower `c98ff4821fdbd89a472792f02f11aa00d5b3583d`, CFG `4ae401171ebb706905a1014445d9f40ddb67eb53`, AIR Java `59df1f7d6f3523b21b172a3ea4b5a0dc95128faa` and AIR specification `2c7f31f19efbe3211a2aea5bbda90173a9666fe2`.

`TopologyProgramAssembler` in lower specializes labels and operations through `LocalIds.activation(binding.id())`, including enclosing activations. Each body receives a concrete completion/resume destination. CICS dispatch adds `HandlerStateAnalysis.Support` to that identity. This duplication encodes real distinctions: removing the activation from IDs would merge return destinations and data states.

The existing specification already defines the representation we need: [`05 §7`](https://github.com/Gustavo2358/analysis-ir/blob/2c7f31f19efbe3211a2aea5bbda90173a9666fe2/especificacao/05-controle-e-invocacoes.md), [`JSON §§7,10`](https://github.com/Gustavo2358/analysis-ir/blob/2c7f31f19efbe3211a2aea5bbda90173a9666fe2/bindings/json-v1.md) and [`O-56–O-60`](https://github.com/Gustavo2358/analysis-ir/blob/2c7f31f19efbe3211a2aea5bbda90173a9666fe2/conformidade/02-oraculos.md) were read at that pin. These are normative premises, not properties inferred from CardDemo.

`air-model` already has the four local-control terminators, completion-port declarations and structural validation. The JSON reader recognizes their field names but rejects their payloads; writer and manifest negotiation also reject them. CFG explicitly rejects `control.local@1`; its current dataflow point is `(activation Entry, node)`, which does not encode a local call stack. Consequently, changing only the lowerer or emitting all possible returns would be incorrect.

## Rules that must survive the campaign

- One local body shares unit memory. Invoking it does not initialize another unit activation.
- `local.invoke` pushes its operation identity, resume and completion ports, then enters the body. No direct invoke-to-resume execution edge.
- `local.boundary` tests only the top frame. On a match it pops and returns; otherwise it follows the default with the stack intact, including empty-stack ordinary execution.
- `local.resume` pops the top frame; an empty stack produces `invalid_local_return`.
- `local.unwind` pops exactly its natural count. Overrun produces `invalid_local_unwind`; an ordinary jump does not pop.
- Sharing code does not authorize joining distinct incoming value states before transfer. Calls, nested ranges, exceptional exits and independent ENTRY activations must keep their appropriate contexts.
- Reuse is based on typed identities and control contracts. Text, source lines and routine spelling do not prove equivalence.
- Source hypotheses, unknown-control remainders, modelAssumed, supports and proof-of-kill requirements remain independent of representation compression.

## Checkpoints and review boundaries

| Checkpoint | Owning repository | Exit criterion |
| --- | --- | --- |
| S0 — discovery | air-java work item | Pin current contracts; identify cloning and consumer limitations; measure the existing corpus without modifying historical products. |
| S1 — transport (this PR) | air-java | Lossless codec for all four local-control operations and completion ports. Independent wire oracle, negative closure/capability tests, unchanged legacy bytes, FAST and local qualification. |
| S2 — CFG representation | analysis-cfg | Typed push/pop/guard transitions over one body. No ordinary return-edge union. Validate O-56–O-60 and exact identity/ownership in the transport. |
| S3 — contextual analysis | analysis-cfg | Reachability, value propagation and queries respect matched returns. Two callers with different values cannot contaminate one another. Define recursion/termination and explicit unsupported cases before implementation; no arbitrary silent stack-depth cutoff. |
| S4 — producer | cobol-lower | Translate published topology using the consumer's admitted domain. Preserve PERFORM phases, ranges, exits, ordinary fallthrough, GO TO and CICS state. Broader contexts remain explicit wherever sharing cannot yet be proved equivalent. |
| S5 — integrated qualification | affected repositories | Compare all 73 CardDemo programs, PERFORM, Chaos, aliases and the broader regression corpus. Audit candidate/support/provenance deltas and measure nodes, bytes, time and memory. Open separate product PRs as required; no merge without authorization. |

This first PR does not claim that the production graphs are smaller or that the CFG/dataflow consumer now executes this extension. The existing lowerer and consumer pins stay in place. The next checkpoints require their own implementation and evidence. Transport acceptance is not executable support.

## S1 algorithm, preservation and cost

Decode closed binding fields into the existing immutable records; encode those records field by field. Carry full port/label/operation identities, array order, origins, claims, uncertainties and fallback envelopes unchanged. `count` uses the existing canonical Natural parser and BigInteger; there is no machine-integer semantic cutoff. Invoke destinations, completion ports and resumes are distinct fields and may legitimately coincide by identity. No stack is executed and no successor is inferred in the codec.

AirValidator retains authority for namespace, ownership, reference closure and required capability. Missing fields, extra fields and noncanonical naturals remain physical input failures. Unknown capability versions and the indirect-control extension remain unsupported. Fallback coverage remains a semantic obligation of its producer; a successful round-trip does not certify it.

The added work is linear in local operations plus port references/declarations, on top of the existing envelope traversal and canonical JSON sorting. It adds no global scan per operation, recursion on call depth, solver, source-language dependency or mutable shared cache. Finite input and existing codec/validator operational limits govern termination; recursive call structure does not recurse during transport.

## Oracles fixed before implementation

1. A manually specified shared body has two distinct call sites and resumes, two boundary occurrences signaling one declared port, ordinary default continuation, explicit resume and unwind. Compare complete Java facts with an independently authored JSON fixture in both directions.
2. Preserve empty/multiple port sets, declaration and sequence permutations, Unicode IDs, self-recursive references and counts above 64 bits. Transport imposes no execution-depth policy.
3. Reject dangling/foreign/wrong-domain labels and ports, duplicate declarations, missing capability, unknown versions, malformed count and fallback fields, and local terminators used as instructions. Exercise encode and decode where applicable.
4. Preserve operation headers, memory/control/dependency fallback content and outstanding obligations exactly. No manufactured return, normal exit, empty effect or storage initialization.
5. Replay canonical transport over all 560 previously qualified AIR products, compare bytes and validation results against the baseline codec. This is transport regression evidence, not a rerun of frontend/lower/CFG/dependencies.

## Validation plan

Use analyzer-lean-gates C2/C3 guidance. The frontier is the shared AIR codec and its capability admission, so focused codec tests, unchanged core/model checks, mandatory FAST and model/codec local qualification apply. A differential transport replay of existing corpus products can detect unintended changes to accepted facts/bytes. Full four-stage corpus execution becomes necessary when S2–S4 change executable representation or consumer behavior; S1 does not repin those consumers.

## S0 measurement and S1 RED

The existing qualified CardDemo products (73 programs) contain 110,317 AIR sequences, 110,570 CFG nodes and 124,356 transitions; canonical AIR totals 1,338,048,740 bytes and CFG 80,302,820 bytes. COACCT01 has 32,145 nodes; CODATE01 17,440; COTRTLIC 5,656; COACTUPC 5,037. These counts measure the baseline, not an achievable compression ratio. Products were read from the previous campaign's `review-qualification/results.json`; no pipeline rerun is claimed.

Before implementation, the new independent local-control fixture compiles and AirValidator accepts its Java model, but CodecSuite fails with IMPLEMENTATION_LIMIT at the nonempty completion-port inventory. The existing 189 model checks pass. Evidence: local campaign `evidence/transport-red-02.log`. The first attempted wrapper run stopped on the new resource's missing explicit harness ownership; its entry was added before the semantic RED run.

## S1 result — ready for Draft review, not merged

Implementation commit: `84ab27823fa1210329901fc22bbca37b901960b6`. [Machine-readable summary](evidence/shared-routine-bodies-s1.json).

- **FAST: PASS** — 189 model checks, 134 transport checks and 41 harness tests. The new golden is required by module ownership and missing-resource falsification tests. The transient harness failure was corrected by adding that resource to temporary project fixtures, without relaxing the gate.
- **qualification-local: PASS** — complete Maven `clean verify`, including model/codec suites and module boundaries.
- **560/560 differential transport replays: PASS** — baseline codec versus new codec; every input equals its canonical output byte for byte, and validation results are identical. Population: CardDemo 73, PERFORM 39, Chaos 48, aliases 14, PERFORM adversarial 25, general fixtures 331, final focal contracts 29 and frontier payload 1. No transport regressions found.
- **Three compiled mutations detected** — swapping invoke entry/resume, discarding unwind count, and discarding fallback read bounds. Clean focal suites passed before and after; mutations used separate temporary classes, never modified production evidence.
- **Consumer boundary checked** — the new codec decodes the local-control golden, but the unchanged frozen CFG consumer explicitly returns `UNSUPPORTED_CAPABILITY control.local@1` (exit 4) and emits no CFG. I-26 fallback obligations remain visible. This verifies that transport support does not silently enable execution.

The prior unsupported-capability test now targets the still-unsupported `control.indirect@1`; local-control version 2 is rejected separately. A malformed completion-port item now fails INPUT_ERROR after closed-shape parsing, instead of the former unimplemented-inventory limit. Neither negative was removed.

New evidence is in the local `.shared-routine-bodies/evidence/` directory; hashes are in the summary. Historical products are reused as immutable transport inputs. Frontend, lowering, CFG and dependency generation were not rerun in S1: their execution model and pins did not change. No new reduction in nodes, runtime or memory is claimed. S2/S3 must establish matched-return traversal and isolated value states before S4 enables sharing.
