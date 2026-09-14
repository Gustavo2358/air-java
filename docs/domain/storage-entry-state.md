# Simultaneous regional entry conditions

W7 completes existing AIR2.0 EntryState and JSON1 binding (analysis-ir
03-memory section8; bindings/json-v1 InitialValue). No new variant or version.
Literal initialization obeys sameDomain, bounds and declared encoding. Equal
partial overlaps are consistent; contradictory bytes are I-17 regardless of
inventory order. Unprovided bytes stay open. Preserve cannot initialize a new
activation allocation, including regions. Unsupported representations retain
explicit validation limits.

Algorithm: encode each admitted bounded literal once; group by physical region,
sort intervals, compare overlap with the farthest-reaching previous consistent
interval. Inductively it represents all covered bytes at the current start.
O(n log n + payload) time, O(n + payload) space, no region-sized allocation or
all-pairs scan. Views/aliases use the existing binding cache. This is structural
consistency, not execution of initial state or producer-premise certification.

Focal oracles: same/partial/nested overlap, contradictory bytes, permutation,
encoding/extent failure, activation preserve; codec closed shapes and roundtrip.

W7.2 completes existing MemoryScope.union JSON1 representation (members array),
including WithinMemory in effect bounds. No new AIR variant or external effect
model. Writer/reader use iterative scope frames, preserving members/order,
including repeated scopes; empty bounds use NoMemory. O(nodes) work, O(depth) frame stack;
no recursive Java scope serialization. Unknown/extra/missing fields reject.
