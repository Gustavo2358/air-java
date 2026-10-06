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
