# Ordered paged graph grammar walk

WORK-AIR-PAGED / IN_PROGRESS. Authority: AIR2.0.0 at normative commit
4f09e8b1b496bf8de2e0fb62532e7aa0b97b9c6e and the official closed AirShape
catalogue. SnapshotGraphWalk visits every Publication-reachable concrete node,
including dead/global data. This is grammar traversal, not a complete Validator.

The transferred Storage owns a primitive LIFO frontier, active-source ancestry and
completed node/collection-context memo. Exact source handles and element shape are
the memo key; AIR nominal identity and Source graph identity remain distinct.
Records read official ordinals. Incoming record/cursor edges are shape-checked
before memo reuse. A collection shared under another expected type is traversed
in that context, even when empty. Sources never provide a guessed element kind.

Each enter task marks ancestry and pushes an exit marker. Children append in
schema/collection order and only that new frontier segment reverses, preserving
preorder under LIFO descent. Collections fully drain one sequential cursor before
any child descends. No recursive Java call or cursor per ancestor is retained.
Exit tasks clear ancestry and mark the exact context complete. Reaching an active
source handle rejects a storage graph cycle. Nominal Unit/origin/alias/reference
cycles remain separate subsequent checks.

For distinct contexts Vc and traversed edges E, primitive requests are O(Vc+E),
including frontier reversal. Official finite element contexts bound reuse per
collection. Cold frontier/memo/ancestry payload must spill and reserve capacity
before growth. Port lookup complexity and backend I/O remain explicit. A fixed
4096-byte control lease funds the one enum lookup table/control allocation;
map-backed test ports prove work/semantics rather than bounded managed residency.

Operational limits bound expanded contexts and actual expanded DFS depth. They do
not constrain every path in an unfolded shared DAG. Counter/address overflow aborts
rather than silently wrapping. A callback has no ancestor cursor live and receives
handles/shapes only. Failure closes cursor, control and owned storage while preserving
the first failure plus cleanup exceptions. The input is borrowed. Counts are returned
only after successful traversal and successful cleanup, never after an omitted subtree.

Grammar covers record slot/type/count, collection/optional cardinality/type and
primitive scalar ranges. Atom block content, model-local field restrictions,
declarations/references, types/domains/premises, capabilities and operation admission
are separate obligations. A callback is not a validity publication and successful
Counts cannot imply STRUCTURALLY_VALID or a CheckedSnapshot certificate.

Independent laws enumerate a manual21-context/23-edge preorder, shared empty list
contexts and mismatched nonempty list aliases;20,000-deep intersections and100,000
sequential occurrences; self/two-node cycles; node/depth budgets; malformed fields,
records, optionals, enum/boolean values and short/extra cursors; denied control,
30 storage failure positions, callback plus both cleanup failures, and cursor-read
failure with suppressed cursor cleanup. Deliberate context-only and order mutations
are rejected by the manual oracle. Full paged admission/codec/CLI remain pending.
