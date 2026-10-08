# Shared contextual local-label references

Status: IN_PROGRESS, AS-W02 under the session's authorized AIR-scale implementation.
Authority: analysis-ir `4f09e8b1b496bf8de2e0fb62532e7aa0b97b9c6e`; the resident
`ReferenceChecks.label` checks declaration existence then local Unit equality for
each actual label reference. Declaration IDs and namespace parents are not extra
reference occurrences. The two independent errors retain their original row order
and multiplicity, including both errors on one row. Both are I-02, with distinct
internal detail tokens. No new AIR rule or wire form is introduced.

`SnapshotLocalLabels` borrows the official source, its complete canonical identity
keys and frozen declarations, and owns transferred managed relation storage. Keys
include the complete namespace. Each actual typed label LIST is folded once,
including invalid lists. A shared ordered row relation retains source anchors,
Unit keys and missing declaration ordinals. Unit postings are built once and remain
shared. A context uses exact missing + nonmatching counts; no array of foreign
errors is allocated per Unit. The reporting owner is bound when emitting a
`SnapshotDiagnosticTemplates` projected chunk and does not enter truth's key.

For sorted matching ordinals m[i], m[i]-i is nondecreasing. Foreign ordinal j is
j + upperBound(m[i]-i,j), so a storage adapter can select it in O(log matches).
Absent Unit buckets select directly; entirely matching buckets report no foreign
errors. Missing ordinals and foreign ordinals are merged with dangling first at a
tie. A single primitive cursor covers a sequential retained prefix. Arbitrary
occurrence access partitions these two sorted sequences without replay; its worst
case is O(log missing * log matches), rather than a claim of universal constant or
linear query cost. All counts and addresses are checked 64-bit values.

The tuple descriptor caches five kind counts. Zero retention neither selects
occurrences nor rescans source. Constructors, calls and callbacks are non-reentrant;
operational faults poison the relation. Closing owned storage/control never closes
the borrowed snapshot, keys or declarations. Relation owners must outlive projected
descriptors and callbacks. This API cannot certify complete AIR admission.

Focused independent laws cover duplicate references, absent/same/foreign complete
namespaces, both errors on one row, two semantic Unit contexts, owner binding,
empty lists, random tiny two-rule oracles, random-access versus ordered prefixes,
late foreign rows, geometric shared-use counts, and storage/source/cursor faults.
Compiled mutants dropping foreign errors, reversing tied rules and replaying folded
lists are rejected. The test-only flat storage is an oracle, not the managed
production backend. Managed postings qualification and full reference/visibility,
type/domain/premise/capability/operation admission, diagnostic lifetime retirement,
CheckedSnapshot and incremental production CLI remain pending.
