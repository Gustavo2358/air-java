# Shared actual reference lists

Status: AS-W02 / IN_PROGRESS. Authority remains analysis-ir
4f09e8b1b496bf8de2e0fb62532e7aa0b97b9c6e. The resident ValidationContext.ref/refs
checks existence of each actual reference in the declaration inventory and reports
one I-02 dangling error per missing occurrence. Namespace parents and declaration
IDs are not automatically reference call sites. Complete namespaces participate
in existence; visible/owned/local/type/applicability rules are separate obligations.

SnapshotReferenceLists borrows one frozen snapshot, its declarations and one
specific diagnostic-template catalogue. Transferred storage memoizes exact
(source,recipe) roots: a single ID or LIST with the expected concrete/union ID
family. A finished empty root is successful cached evidence, distinct from a
missing or unfinished memo table. Lists fold once even when invalid. Scalar
reference roots share too. Reporting owner binding is deferred to emission;
occurrence multiplicity and source order survive duplicate IDs, repeated roots,
zero retention and all subsequent owner uses. New read-only queries create no
memo table. Closing the memo leaves borrowed literal roots available in their
catalogue; borrowed source/declaration/catalogue lifetimes remain caller obligations.

A funded fixed64-slot binary carry forest combines actual missing occurrences.
It builds eventual subtrees rather than every successive append-prefix root.
For N distinct missing references at a power of two, N leaves + N-1 join tuples
are built. General counts need linear carry work plus O(log² N) final forest join
work/tuples, with checked64-bit counters/addresses and no source-sized Java array.
The immutable catalogue still retains constructed handles until its closure;
this assembly algorithm does not pretend to implement global root retirement.

Five independent laws cover manual and96randomized mixed ID-family references,
complete namespace differences, successful zero roots, shared valid/invalid lists,
owner/order/duplicate semantics,16..4096cardinality curves with2N-1tuples,
all memo/source/cursor/template interruptions, wrong family, callback reentry,
null/denied constructors and primary/suppressed combined cleanup failures.
Compiled naive-prefix, empty-cache-miss and duplicate-collapse mutants are rejected.
Original passes. Test flat storage is an oracle, not the managed backend.

Managed memo/backend integration, fact-scope/coverage/precision annotation recipes,
visibility/uncertainty/type/domain/premise/capability/operation rules, CheckedSnapshot,
incremental official JSON and production managed CLI remain pending. This API
produces existence templates, not complete AIR admission or a validity certificate.
