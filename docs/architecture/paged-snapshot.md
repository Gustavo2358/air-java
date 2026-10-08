# Official paged AIR access

Status: implementation in progress, WORK-AIR-PAGED. Large-publication consumers need
typed immutable access whose input, dictionaries, arrays and indexes can exceed heap.
This evolves official AIR access; it does not introduce a competing consumer parser.

The snapshot retains the complete AIR vocabulary, IDs/ownership, capabilities,
coverage, uncertainty and proof. Handles are snapshot-local implementation identities;
they never replace external IDs. Indexed access and cursors do not implicitly turn
all pages into a Publication. Large text/operand arrays have explicit materialization
and budget boundaries. A caller lease defines lifetime and prevents use after close.

The model/access/validation owners do not perform I/O. Typed storage ports provide
immutable blocks; adapters manage pages and files. The official codec feeds the
official builder incrementally. Validation traverses the same typed access, resolves
forward references through indexes, checks all inventoried entities including dead
control, and preserves diagnostic paths. Complete validation cannot be skipped or
weakened because a demand is small. Certificates remain unforgeable and bound to the
snapshot and validation options.

Publication continues as an in-memory adapter with its current immutable semantics.
Lazy I/O cannot be hidden in the old value contract. Holding a caller-supplied giant
Publication outside the managed reader does not gain a bounded-input-memory guarantee.
The temporary page format is internal, not a new AIR version or public transport.

Memory and paged backends must agree on accepted facts and rejected rules, including
duplicate IDs, references, ownership, Unicode, capabilities and coverage. Resource
failure is operational and distinct from invalid/unsupported/incomplete validation.
Offsets and counters avoid 32-bit overflow; exact key equality survives hash collisions.

The integration order is access/storage contracts → official validation/codec →
producer tests → immutable AIR commit → consumer repin and boundary tests. Java 21
without preview and existing model/codec dependency boundaries remain requirements;
any deliberate boundary evolution is documented and tested, never bypassed.

## Typed access checkpoint

`AirShape` enumerates every shape reachable from `Publication`: 197 records,
37 enums, 40 closed unions and 518 fields. Fixed metadata describes exact field
types, list/optional element types, enum values and transitive subtype membership.
An independent test reads the Java model's record components and sealed hierarchies
to challenge the catalogue. Production access has explicit generated typed switches;
it performs no reflection and does not interpret JSON tags. Computed helper types
that are not publication facts do not enter this catalogue.

`AirSnapshot` reads typed fields, 64-bit list/optional indices, primitive scalar
values and bounded UTF-16 blocks. Unbounded integers use canonical signed decimal
blocks instead of requiring a resident BigInteger. External storage implements the
frozen `Source` port and transfers ownership of its lease to the snapshot. Closure
detaches that owner and rejects further access. Concrete node handles are scoped to
the source; equal model IDs may occupy different nodes, so full typed ID contents
and namespaces remain the authority for semantic identity.

The explicit `fromPublication` adapter retains the caller-owned Publication and lazy
in-memory identity indexes. It computes a large integer's text once per model
identity. It does not claim managed residency. The external-source tests exercise
indices/text offsets above Integer.MAX_VALUE, not a valid giant AIR or a managed
decoder/Validator execution.

API impact is COMPATIBLE: these are additive access types; existing model, codec and
validation signatures/semantics are unchanged. Access snapshots carry no validation
status. In particular attaching typed storage, or wrapping a Publication with another
AIR version, cannot produce a checked certificate. Incremental builder, paged storage,
official codec and complete Validator migration remain pending; the consumer has not
been repinned or integrated yet.

## Incremental storage builder checkpoint

`AirSnapshotBuilder` owns four zero-default primitive column ports: node headers,
fixed record references, indexed collection trees and packed UTF-16 data. Port
implementations fund capacity/spill from the session ledger; the builder claims
control-state leases before allocating its fixed buffers. The initial owner uses
512 reserved bytes, each active list appender 1,024, and each character appender
256. Those are coarse capacity reservations, not measured retained-heap totals.

Record admission checks exact fields/subtypes and each container's declared element
shape without repeatedly scanning its elements. Children exist before parents;
forward semantic AIR references remain ID values that the complete Validator must
resolve later. All offsets, counts and identity arithmetic are 64-bit and reject
overflow before addressing storage.

A list appender uses a 64-slot binary-counter forest. Appending N elements creates
O(N) total tree nodes; indexed reads follow weighted child counts in O(log N).
Array length never becomes an int-sized list. Character streams preserve surrogate
pairs across blocks and canonical signed decimal integers. Four UTF-16 units share
one primitive word; read blocks reuse each loaded word. Lists/records can be built
between character blocks, but only one character stream owns the contiguous text
append position at once.

Successful finish irreversibly seals the storage write/claim port, then transfers
the frozen store/control lease to a snapshot and
invalidates the builder. Unfinished writers prevent transfer; abandoning a writer
aborts the builder instead of certifying a prefix. Partial storage writes abort it
as well. Closing detaches owners and releases reservations, preserving both primary
and suppressed cleanup failures. Independent tests confront all facts from a
representative Publication, randomized array positions and streamed scalar blocks.
The mutation that returns the first array element for every index is rejected.

The producer tests use a map-backed test port; they do not establish bounded disk
residency. The consumer page-backed port, incremental JSON binding, model-local
invariant checks and complete cross-reference Validator are still required. Frozen
typed storage alone remains insufficient for a checked validity certificate.
The explicit `Storage.freeze` requirement corrects the unmerged builder prototype
in place: callers cannot keep using a borrowed write port after ownership transfer.

Sequential `AirSnapshot.elements` cursors yield primitive node handles. The paged
source traverses each collection tree once with a fixed iterative stack, so visiting
N items takes O(N) logical tree reads instead of N indexed searches. Cursor scratch
leases are permitted after storage freeze; mutation leases remain forbidden. Empty
and exhausted cursors release their leases, and snapshot close closes active cursors
before storage. A denied cursor lease leaves input facts intact. The public indexed
default supports caller-owned resident/custom sources; managed paged access overrides
it. None of these cursors materializes a java.util.List or allocates a row per item.

## Exact identity-key migration

[Snapshot identity keys](paged-identity-keys.md) provide the first primitive
identity-index layer for paged validation. They include the complete concrete
domain and namespace, stream text into exact canonical primitive tuples and memoize
source-node keys within one index lifetime. They issue no validation certificate.
Complete cross-reference admission and the incremental codec remain pending.

Sequential cursors also verify their exact 64-bit declared cardinality. A premature
end or extra row fails operational storage access and closes the cursor; it cannot
become a successful truncated inventory. Early caller closure remains permitted,
and exact empty/large collections retain their semantics. The cardinality fault
law was RED before this guard, including zero/short/extra/Long.MAX_VALUE sources
and a second cleanup failure. This access check still issues no AIR certificate.

Direct operation roots and operand children are available through
[typed primitive traversal](paged-operand-traversal.md). This does not certify
whole-publication structural validation.

[Primitive declaration inventory](paged-declarations.md) checks complete namespace,
duplicate occurrence and owner relations through an owned storage port. It is one
indexing pass, not full paged admission.

The [ordered primitive graph walk](paged-graph-walk.md) covers complete typed
grammar reachability and contextual collection memoization; model-local and full
semantic validation remain separate obligations.
