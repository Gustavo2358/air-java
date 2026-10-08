# Nominal cycle residuals over typed paged AIR

AS-W02 / IN_PROGRESS. AIR authority remains
4f09e8b1b496bf8de2e0fb62532e7aa0b97b9c6e. SnapshotNominalCycles borrows
AirSnapshot, exact complete identity keys and frozen first-declaration rows; it
transfers the degree/reverse-occurrence/FIFO scratch port. This pass implements
I-36 Origin inputs, I-01 Unit containing relations and I-12 direct Object alias
relations. Alternatives/unknown bindings do not add nominal alias edges.
Executable grounding and missing-reference diagnostics remain separate obligations.
Successful counts are neither full validation nor a CheckedSnapshot certificate.

Register all selected declarations before linking parents, then build every degree
before scheduling zero-degree nodes. Every repeated input contributes one degree
and reverse occurrence. Absent or other-family parents contribute none here.
Kahn removal decrements each occurrence once; a child is scheduled only when its
degree reaches zero. Report every positive-degree residual, including dependents
of a cycle, in Origin/Unit/Alias then first-declaration order. SCC-only reporting
would change existing diagnostics. Identity equality uses complete typed namespaces,
never source addresses or local names. All counters use checked long arithmetic.

For D declaration rows, N selected nodes and E actual edge occurrences, primitive
work is O(D+N+E). Ordered storage lookup comparisons and I/O are accounted separately.
No per-root DFS, all-pairs closure, collection copy, resident graph or recursive
stack is introduced. Fixed control is claimed256B; all cardinality-dependent state
belongs to the spillable scratch port. Operational/callback/cleanup failures never
return counts; borrowed snapshot and indices remain caller-owned. The first failure
is preserved and cleanup failures are suppressed.

Four registered contracts enumerate independent residual sets across all three
families, cycles and cycle dependents, self cycles, repeated parents, DAG diamonds,
absent parents, reversed inventories, namespace equality across distinct sparse
64-bit source handles, and failure at every scratch call plus cursor/callback/lease
cleanup failures. Five chain sizes16/64/256/1024/4096 decrement exactly2N repeated
edges and visit each node once. Map-backed test ports establish semantics and work,
not bounded-memory admission. Compiled mutations dropping later input parents and
adding absent parents respectively lose and invent a residual; the original passes
all four focused laws. Managed consumer integration remains pending at this point.

[Shared location grounding](paged-grounding.md) now provides the separate positive
fixed-point fact index; executable owner/reference admission remains pending.
