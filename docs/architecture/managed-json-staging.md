# Incremental managed physical JSON staging

AS-W02 / IN_PROGRESS. AirJson has InputStream+InputStorage checked decode overloads.
The physical input stream is borrowed; staging storage is transferred and closed
on every outcome. Incremental UTF-8 parsing uses a funded8192byte input buffer,
packed UTF-16 words, page-backed token metadata, indexed child ordinals, decoded
property equality and managed depth frames. No whole byte document, resident token
array, sibling-prefix random-read replay or per-depth Java frame is required.
InputStorage documents the exact primitive staging layout and ownership contract.

The same explicit BindingReader and AirValidator own model/capability/admission
semantics. Arbitrary property order, future references, list occurrence order,
Unicode, escaped duplicate names, strict physical errors, short/zero reads and
original IO failures are preserved. A fixed cached character word prevents four
repeated page reads/writes for adjacent UTF-16 units. The indexed child's actual
lookup cost belongs to the storage adapter; no universal constant query claim.

This is a production bridge toward the official snapshot decoder, not full managed
AIR admission. BindingReader still creates Publication, lists, Strings and
BigIntegers. Conversion scratch is guarded, but returned model ownership is not
funded by this staging lease. SnapshotAnnotationTemplates supplies context-free
fact-scope/claim/precision/coverage recipes, not all validation or CheckedSnapshot.
The additive `decodeSnapshot` route now transfers the same physical staging into the
official builder without creating a `Publication` for empty publications and the
independent minimal-return GOBACK profile. Its whole-tree equality law compares every
typed node, scalar and collection with the resident projection; ownership laws cover
success, unsupported inventory and denied builder capacity. This is a real direct
binding vertical, not complete codec coverage: other already admitted variants still
fail explicitly and the route issues no validation certificate.

Full typed builder binding coverage, complete snapshot rules and downstream snapshot
CFG/planner/domain/output ownership remain required. A successful checked resident
Publication retains the complete original Validator result; partial checks cannot
issue it. No end-to-end memory bound or global dependencies completion is claimed.

Focused laws compare all four canonical binding fixtures and unchanged full
admission results; independent physical cases cover decoded name equality, strict
Unicode, arbitrary order, zero progress,20000depth,16..4096indexed arrays, byte/depth
limits, every primitive staging fault, caller stream ownership and0leases.

The compiled dependency policy allows InputStream/IOException only on AirJson and PagedJson; model, filesystem/network access and other codec owners remain denied, with a dedicated negative policy law.
