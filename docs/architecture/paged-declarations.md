# Primitive paged declaration inventory

WORK-AIR-PAGED / IN_PROGRESS. Authority: AIR2.0.0 at
`4f09e8b1b496bf8de2e0fb62532e7aa0b97b9c6e`, I-01/I-03/I-11 and the current
PublicationIndex occurrence rules. Existing Publication-based validation is unchanged.

SnapshotDeclarations builds declaration and ownership relations once over borrowed
AirSnapshot and exact SnapshotIdentityKeys. It owns Storage and a fixed512-byte
control lease, reserved before callback allocation. The catalogue contains source
node, identity, containing Unit, Sequence and expected operand owner handles. Keys
include complete typed namespaces. Global declarations have zero absent associations.
Dense first-definition ordinals allow later passes to access rows without retaining
Java model records or resident lists. Queries and statistics use long, without int
address/cardinality narrowing. Lookup complexity belongs to the storage backend.

All15 declaration categories are indexed: Publication/Artifact/Origin/Uncertainty/
Premise/Resource/ArtifactRelation/Storage/Unit/Object/CompletionPort/Entry/Sequence/
Operation/Operand. Entry initial places, literal values and possible-literal candidates
are occurrences too. Envelope and value-result ID references do not become definitions.
Operation roots and direct operand children use the official exhaustive typed helper.
Unit/inventory cursors have fixed nesting depth; the occurrence FIFO holds primitive
node/expected-owner/depth rows. Deep operands retain no Java recursion or cursor per
ancestor. Consumed frontier rows must release payload; each occurrence is enqueued,
without deduplication by source handle. Duplicate IDs still report I-01 and stop that
operand subtree, as in PublicationIndex. Duplicate operations still enumerate roots.

Namespace checks compare full PublicationId keys; operation/sequence ownership uses
full UnitId. Entry and operation owners stay distinct even with equal local names.
Each reported Rule preserves its normative invariant code, source identity and node.
Issues are supplied to a caller-owned sink, which must account/spill retained facts.
Entity/nesting limits are explicit index limits, not invented unknown/missing facts.

No partial index escapes a failed build. Source, storage or issue-sink failure closes
owned rows/frontier/control and preserves the primary error plus cleanup failures.
Operational lookup failure aborts the owner. Bounds errors on catalogue ordinals do
not invalidate a healthy index. Close detaches borrowed handles/references and leaves
the snapshot/key catalogue available to their actual owners. The borrowed identity
catalogue must not be collected/closed while declaration keys remain in use.

Independent small ID decoding covers all declaration categories, entry modes,
namespace/type separation, duplicate definitions/shared occurrences and foreign
publication/Unit/operand owners. Synthetic20,000-deep and4,096-wide operands check
frontier width, cardinality and cleanup. Denied claims, multiple write/read failure
positions, issue-sink failure and both close failures are covered by map-backed test
ports. These establish semantics/work/lifetime only; managed backend pressure is a
consumer obligation. Fixture validity is not inferred from this subset.

Pending: local constructor invariants for external typed sources, all reference/
capability/type/domain/proof/operation checks, CheckedSnapshot binding, incremental
codec and managed CLI integration. This class returns no ValidationResult,
STRUCTURALLY_VALID or validity certificate. Full admission must run every applicable
pass, including dead code, over the shared indexed snapshot.
