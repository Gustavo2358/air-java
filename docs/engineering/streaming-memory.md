# Streaming AIR output ownership and admission

The streaming API keeps the immutable publication as its owner and maps one
array element at a time. It does not retain a JSON tree for the whole publication
or complete output bytes. Arbitrary physical JSON lists still copy on construction;
only the private immutable mapped view is exempt. Its source list is snapshotted.

A coverage-only BindingWriter runs the same explicit typed mapping in the same
argument/array order, discarding each mapped value immediately. Its object/array
stubs carry no facts and escape nowhere. This preserves the existing requirement
that an unimplemented transport form fails before structural validation. After
coverage, AirValidator runs exactly once. The normal mapping uses immutable lazy
arrays. Physical measurement then checks UTF-8 scalars, depth and byte budget
before exposing any byte, with the existing writer rules. Emission buffers a fixed
chunk into the caller-owned stream; I/O failure may leave a prefix in that stream,
so file adapters must stage and atomically publish only after successful close.
No admission failure exposes semantic bytes. The codec does not close the caller.

Time is O(typed mapping + validation + wire bytes), including explicit coverage
and physical measurement passes. Space is O(typed snapshot + validator scratch +
largest single mapped element + JSON depth + fixed output chunk), not a claim
about intrinsic expression/result size. The byte API keeps its historical mapping
and failure order; its strict private path returns its freshly allocated bytes
without routing through a defensively cloned public PartialOutput record. Public
PartialOutput constructor/accessor ownership remains unchanged.

Independent canonical goldens, malformed/profile/validator precedence, exact
UTF-8/depth budgets and destination failure tests govern both output paths. On the
same AIR model, retention/elapsed/RSS are measured separately from shared-body
improvements. Adoption requires no runtime regression on the qualified baselines;
if the streaming passes are slower, optimize or retain the faster mode where its
owned memory fits the explicit envelope. No variant/fact/support is dropped.

`prepareWrite` (strict/partial) completes coverage, validation and physical
measurement before a file adapter touches its destination directory. The prepared
owner retains only the immutable model/lazy mapping and validation result; writeTo
emits the already checked mapping without repeated validation/measurement. It
remains safe for independent caller streams; no mutable mapping cache exists.

## Decode scope ownership law (before implementation)

Each binding read owns canonical immutable publication and unit scopes, keyed by
all exact scope fields, after the existing shape/text checks. Equal scopes within
one read reuse identity; different namespace/local pairs (including hash
collisions) remain distinct. Separate/concurrent reads share no scope cache.
Leaf IDs, declarations, occurrences, evidence and validation are not memoized.
Parallel binding workers use the same per-read owner. UTF-8, field/domain/role
checks and diagnostic order remain unchanged even after an equal scope occurred.
ScopedIdentityChecks uses the independent physical golden and adversarial scope
keys, checks reference ownership, complete facts and canonical bytes; parallel
ownership is checked by DecodeSchedulingChecks. Cache space is O(distinct scopes),
not O(all references), and there is no global string interning.

## Per-record binding field index law (pre-code)

A binding At owns one immutable physical-field lookup index, containing exact
field names and tape token offsets. Repeated shape/child checks reuse that index
instead of rebuilding a map and rescanning every field. No text, nested record,
array or typed fact is materialized by indexing. Full physical parsing precedes
binding; duplicate/UTF-8/escape failures and unknown-field diagnostic order remain
owned by the existing reference parser/Map.copyOf rule. Index lifetime follows
active binding records, not the whole decoded publication. The ownership oracle
checks repeated lookup identity and all child facts; the independent physical
parser differential corpus and complete CodecSuite govern admission and output.

The physical field index uses primitive name-ordinal/token-offset slots for at
most 64 fields. Name ordinals come from the exact parsed field vocabulary;
collisions probe and compare ordinals, never hashes alone. Larger objects retain
the original linear lookup and identical admission. This bounds auxiliary index
space per active record without imposing any cardinality limit on JSON.
