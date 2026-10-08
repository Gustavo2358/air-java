# Shared positive equations for executable location grounding

AS-W02 / IN_PROGRESS; normativeAIR4f09e8b1b496bf8de2e0fb62532e7aa0b97b9c6e.
SnapshotGrounding borrows the immutable typed input and its frozen declaration
index (which borrows the exact identity keys). It owns equation scratch and fixed
control256B, transferring graph scratch to the iterative whole-grammar collector.
Build publishes a queryable owner only after collection, graph cleanup, saturation
and freeze succeed. This is a grounding fact index, not complete validation or
I-13 diagnostics/CheckedSnapshot admission.

Each object declaration, binding or MemoryScope source is one positive Boolean
OR equation. Objects depend on bindings; aliases resolve first complete declared
ObjectIds; unknown bindings depend on scope; alternatives depend on every binding
and their remainder; object scopes depend on actual declarations; unions depend
on member scopes. WithinMemory depends on its scope and NoMemory is a false
constant. Cell/View/AllMemory/VisibleMemory are true seeds under the existing rule,
even when independent reference/kind checks would reject their IDs. StorageMemory
seeds iff the explicit storage list is nonempty. Empty scopes/closed alternatives
remain false here and are separately invalid constructor forms.

Collection is iterative and exact-source memoized. Links may register an equation
before it is expanded; start seals all edges and requires every row expanded.
Seeds are queued during collection but no propagation begins before start. Each
false->true change schedules once, propagates every reverse occurrence once and
remains reusable for all later queries. Unseeded cycles stay false; one grounded
alternative grounds a cycle and its dependents. Nominal direct-alias cycle rules
remain separate. Negative cyclic results are final facts after saturation, avoiding
root-dependent recursive DFS/recomputation. Frozen queries do required lookups and
retain no per-query answer history. Missing objects are false with an independent
reference error still required; uncollected equation queries reject.

Primitive work O(grammar contexts/edges + equation nodes/edges + queries), with
ordered lookup/page I/O separate. Required equation/index/FIFO state is supplied
by a managed spillable port; no per-row Java graph, source String/type reconstruction,
recursive alias/scope stack or all-pairs closure is introduced. Operational failures
poison the owner, never turn into unknown/partial/valid. Cleanup preserves primary
failures and closes transferred scratch without closing borrowed input/declarations.

Five registered contracts cover manual true/false residuals under inventory reversal,
seeded/unseeded cycles and dependents, OR/remainder alternatives, all intrinsic seeds,
foreign namespaces, empty typed scopes/closed alternatives,48independent tiny random
graphs checked by per-root finite seed reachability,20000deep shared union DAGs,
five cycle sizes16/64/256/1024/4096 with8queries per object and no equation replay,
every scratch interruption plus construction/graph/query/combined cleanup failures.
Map-backed test ports are semantic/work oracles, not managed residence qualification.
Missing API compilation RED is retained; compiled disabled-propagation and dropped
remainder mutations fail direct truth assertions. Untouched focused laws pass.
Consumer managed relation and executable owner diagnostic integration remain pending.
