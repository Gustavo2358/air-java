# Shared ordered diagnostic occurrences

AS-W02 / IN_PROGRESS. SnapshotDiagnosticTemplates is an ordered diagnostic sequence
foundation, not a Validator or CheckedSnapshot. Rule tokens/source anchors are
interpreted by the semantic admission pass; the reporting owner is bound at emission.
All five ValidationIssue.Kind counts are exact and independent of retained capacity.

Empty is0; a leaf carries kind/rule/anchor/field/detail and one occurrence. A branch
shares its two child templates, height and total/per-kind counts. Concatenation
preserves order and multiplicity: a repeated child represents twice its occurrences.
The sequence is an immutable AVL rope through managed exact14word tuples. Only two
tuple columns are template references; source/detail addresses stay literal. No
resident issue lists, model/ID strings, recursive joins or Source-depth cursor exist.

Joins descend and rebalance iteratively, creating O(log occurrence count) tuples/work.
AVL positive leaf counts have a Fibonacci height lower bound; height92 exceeds
signed-long count capacity. Fixed96level primitive join/cursor scratch and staging
are reserved2048B before allocation. Balanced tuples/checkable counts enforce this
bound; arithmetic overflow aborts before publishing a new root. Empty/one-child
forwarding retains the existing root. Exact tuple interning and source/context memo
in the future semantic inspector share children; equivalent sequence groupings need
not have byte-identical shapes. All required roots/tuples remain stable until owner
closure; lifetime/version collection is a separate future integration obligation.

Emission sends cached kind totals in bulk, then traverses only the retained ordered
prefix. Zero retention reads six summary words and no tree nodes, independent of
source depth, number of occurrences or old queries. The fixed primitive cursor can
be abandoned without retaining query history. Callbacks cannot re-enter/mutate/close
the owner. Every operational/report failure poisons it; transferred scratch and
leases close with primary/suppressed errors intact. No incomplete result is valid.

Four registered independent laws cover96mixed prepend/append/repeated-subtree
sequences,64random joins of24independent chunks, all kinds/anchors/owners/order,
forced tuple hash collisions, five sizes16/64/256/1024/4096 with height5/7/9/11/13,
all measured storage calls and report/constructor/reentrant/cleanup failures.
62shared doubling joins represent2^62diagnostic occurrences with63tuples/height63
and emit only three anchors.100000empty forwards create no tuples; the overflowing
next doubling fails and poisons the owner. Map ports qualify semantic/work laws
only; the literal managed backend is separately qualified in analysis-cfg. Complete
reference recipes and projected-context backend integration remain pending.

Compiled repeated-child collapse loses ordered occurrences, and dropped right-child
traversal cannot emit the requested prefix; both mutations RED, original GREEN.
Required tuple payload grows with constructed joins, not expanded occurrences.
This does not qualify full admission, external diagnostic rendering, managed decode,
dependencies or global AS-W00–W10 completion. Those frontiers remain explicit.


## Lazy projected chunks

A borrowed Projection relation can resolve a frozen exact recipe/relation/context
into requested primitive occurrence anchors. A projected chunk stores all five kind
counts and their checked sum in one height-one tuple, regardless of represented
occurrences. Mixed kinds/rules preserve actual occurrence order. Concatenation and
literal behavior keep the same14word schema; node kind8 denotes projection, recipe/
relation/context occupy literal payload columns. Positive chunk counts preserve the
height proof. The borrowed relation owner proves descriptor counts and lifetime;
the partial template API does not certify those claims or complete AIR admission.

Only a retained chunk prefix calls the projector. Zero retention makes no projected
call. Fixed five-word output staging is included in the2048B control reservation;
Long.MIN_VALUE sentinels detect every unwritten word without masking an absent field
or zero source anchor. Kind/rule/source/field/detail bounds reject incomplete output,
missing projector, bad descriptor counts, overflow and wrong context. Projection/
report/metadata failure poisons the template owner, never falls back to missing/valid.
The borrowed projector is not closed. Reentrant calls/closure are rejected.

Three additional independent laws cover different contexts on the same relation,
mixed literal/projected/repeated chunks, defensive count copying,4096zero-retention
owner uses without projected reads/interning, a trillion occurrences in one tuple
with three projected reads, every incomplete output field/invalid kind/rule, missing
projector/count-length/sign/overflow, every storage interruption, report and projector
failures/reentrancy/borrowed lifetime. Compiled dropped-context and zero-quota-query
mutations fail independent assertions. Managed projected context/reference index
integration is still pending; the literal consumer checkpoint does not qualify it.

The first contextual relation is specified in [shared local-label references](paged-local-labels.md).
