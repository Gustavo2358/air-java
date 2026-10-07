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
