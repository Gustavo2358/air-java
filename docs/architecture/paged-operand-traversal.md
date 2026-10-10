# Typed primitive operand traversal

WORK-AIR-PAGED / IN_PROGRESS. Authority: AIR 2.0.0 at
`4f09e8b1b496bf8de2e0fb62532e7aa0b97b9c6e`, model operands/operations and
I-01/I-11. Operand occurrences are distinct from ID references in envelopes.

`SnapshotOperands` enumerates each operation's own operand roots and each
operand's direct children, in the official model order, through a primitive
LongConsumer. It borrows immutable access, performs no expression evaluation or
reference repair, and issues no validation certificate. This is the paged counterpart
of `Operands`, not an alternative schema or a parser.

Closed shape switches select fixed official ordinals. List payload is consumed by
one leased sequential cursor at a time. No list/row object or child array is built;
fixed-field edges take constant work, and N list edges take O(N) cursor advances.
The caller owns its primitive work queue and per-source visited state, so an eventual
full inventory pass can spill its frontier rather than retaining Java recursion or
one iterator per operand ancestor. This helper does not itself recurse or memoize
away occurrences; duplicate-ID and ownership checks remain the validator's duty.

Callbacks and source/storage failures propagate. A failed callback closes its active
cursor while preserving the first failure. Source cursor cardinality must exactly
match the frozen collection length; early/extra rows cannot silently drop facts.
Early caller close is legal, but helper enumeration consumes every selected list.

Independent expected root/child sequences cover all closed operation/operand kinds,
including computed targets, three argument modes, results/effect operands, byte-range
coordinates, unknown dependencies, choices and regions. Envelope references do not
invent occurrence definitions. Indexed-read rejection and cursor fault/cleanup laws
challenge the traversal mechanism; giant/deep input is handled by the future paged
inventory queue, not claimed qualified by this callback layer alone.

Complete paged Validator, model-local invariant admission, checked snapshot binding,
streamed JSON and consumer integration remain pending. Existing Publication-based
validation and serialized facts are unchanged.
