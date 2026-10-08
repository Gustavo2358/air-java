# Model-local predicates over typed paged AIR

AS-W02 / IN_PROGRESS. Authority AIR2.0.0 at
4f09e8b1b496bf8de2e0fb62532e7aa0b97b9c6e; model constructor semantics
remain unchanged. SnapshotLocalConstraints composes with SnapshotGraphWalk and
borrows the immutable snapshot and exact atom/identity index. It transfers local
scratch and, through scan, graph scratch. Successful counts establish grammar and
constructor-local predicates only. Full Validator, CheckedSnapshot, reference,
visibility, types/domains/premises, capabilities and operation
admission, streaming codec and managed CLI remain pending.

The closed AirShape switch applies field-specific Unicode/nonblank rules, exact
canonical-integer sign/positive/modulo8 predicates, optional text/extent predicates,
nonempty/closed-empty collections, storage lifetime and body-availability rules.
Span bases/coordinates and ordered offsets compare cached arbitrary-precision
integer subtrees; no narrowing or BigInteger reconstruction is performed. FitDecimal
scale remains signed. LocalUnwind all/count agreement belongs to the subsequent
operation pass. Optional displayName/contentDigest and ResourceCategories retain
their existing constructor behavior; this pass does not add Unicode/nonblank rules
to those fields. Transport Unicode admission remains a distinct obligation.

LabelType nonempty/uniqueness and LabelValue membership share an exact index keyed
by labels LIST source handle and complete canonical LabelId (including Unit and
Publication namespaces). Each distinct labels list is drained once, then every
membership lookup uses the required index. FormatDecimal folds finite flags once
per parts list; DecimalText.Part predicates are checked independently by the whole
graph walk. Valid aggregate flags require a digit-bearing part, unique radix and
floating sign, and compatible suppression. The discarded BigInteger digits/scale/
extent result of DecimalText.describe is never constructed for local validity.
Exact shape arithmetic needed by later domain consumers is still pending. Bytes
lists are similarly checked once. Facts are published only after their corresponding
collection predicate succeeds. A failed predicate cannot become a valid fact.

Let Vc/E be grammar contexts/edges, C distinct constrained atom character units,
L distinct labels/parts/octet list rows, Q membership requests. Primitive work is
O(Vc+E+C+L+Q), apart from exact integer comparisons (logarithmic primitive subtree
reads after atom construction). Complete namespace depth is schema-bounded. Every
cardinality-dependent membership/memo is transferred to a managed storage port;
ordered-index comparisons and page I/O have their own complexity. Fixed inspector
control is claimed256B before publication; no recursion, streams, reflection,
proportional String/BigInteger or per-row object is introduced in production.
Map-backed test ports are semantic/work oracles, not a managed-input memory claim.

Invalid exposes a typed local rule plus source node/slot anchors. These are internal
handles, not external AIR IDs or transport paths. A standalone inspector may retain
other diagnostics after Invalid but its caller must reject the publication; scan
stops and closes both scratch owners. Operational source/key/storage failures poison
the inspector and never become invalid/unknown/partial semantic results. Borrowed
input and keys remain caller-owned. Cleanup preserves the primary exception.

Five deliberately registered contract checks cover all restricted text/numeric
field positions, empty/Unicode/split scalar facts, huge signed descriptors and
signed FitDecimal scale, conditional collections, exact coordinates, octet limits,
every DecimalText kind and aggregate incompatibilities, complete-ID duplicates and
membership, shared-list folds, dead/global content and failure ownership. Membership
curves16/64/256/1024/4096 index exactlyN labels and use one cursor forN repeated
queries;100000 shared decimal parts drain once for4096 aggregate checks. Independent
constructor/BigInteger/Unicode rules supply expected outcomes. Missing API compile
RED and the first positive-offset fixture's incorrect digit length are retained;
the fixture was corrected to actually satisfy the declared manual ordering.

Mandatory FAST passed219 model and143 transport checks in29.824s, with43
policy checks and compiled module boundaries. The same production implementation
passed the five focused laws before/after isolated compiled identity, membership
and memo mutations. Source-identity and unconditional-membership mutations are
also refined to reach their direct semantic duplicate/nonmember assertion before
checking instrumentation; their initial failures remain preserved separately.

[Nominal cycle residuals](paged-nominal-cycles.md) now provide a separate exact
removal pass; this does not complete reference or executable-grounding admission.
