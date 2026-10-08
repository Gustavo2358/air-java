# Exact paged snapshot identity keys

WORK-AIR-PAGED / IN_PROGRESS. Authority: AIR 2.0.0, analysis-ir
`4f09e8b1b496bf8de2e0fb62532e7aa0b97b9c6e`, model §2 and I-01/I-02.
The complete identity includes its concrete domain and publication/unit/operand
owner. Source-local handles and hashes cannot substitute for identity.

The next validator vertical canonicalizes these exact keys over a storage port.
The snapshot is borrowed; one index owns its primitive memo, canonical tuples and
scratch lease. Memo entries bind source-local handles to exact keys for that one
immutable snapshot. Storage must compare complete seven-column tuples and preserve
issued keys until index closure; a collision is never evidence of equality.

Texts are read once per source handle in fixed UTF-16 blocks. Sixteen code units
form a four-word leaf, with zero padding only on the final leaf. A binary-counter
forest forms a deterministic balanced sequence; a final length record distinguishes
real trailing NULs from padding. No normalization, case folding, trimming or name
interpretation occurs. Surrogate pairs may cross leaves without changing identity.
Namespace records reference canonical children and include their concrete AIR shape.
The namespace schema is acyclic with at most five record ancestors. Traversal uses
fixed primitive frames, not input-dependent Java recursion or retained model records.

For distinct visited source nodes V and character units C, the number of port
operations is O(V+C); exact lookup complexity belongs to the storage implementation.
The memory implementation may use an exact table; the managed implementation must
spill payload and indexes. Canonical keys and memo records are required state, not
an evictable cache. Repeated requests for a source handle perform no text/prefix
reconstruction. Fixed control buffers reserve 4096 bytes before allocation. This
is a coarse allocation reservation, not a measured JVM retained size.

Independent oracles compare all concrete Java Id variants and both operand owner
kinds using model record equality, with intentionally colliding tuple hashes,
different source handles/namespaces, NUL suffixes, Unicode and long shared prefixes.
Counter laws check one text pass and O(C) tuple requests, plus failure/closure laws.
Tests using map-backed ports establish semantics and work, not bounded residency.

This helper does not check declarations, duplicates, reference existence, owner
admission, capabilities or local model constraints, and issues no validity
certificate. Complete paged Validator, streamed codec, managed consumer integration
and backend parity remain required. Existing Publication validation is unchanged.

The source-failure law was RED against the first implementation: a failure in the
initial shape read occurred outside the abort scope and a later call could reuse a
previous key. All source reads now participate in the same fail-closed key scope.
The exact first operational failure propagates; owner closure still releases every
fixed lease even if storage and lease cleanup both fail. No certificate is issued.
