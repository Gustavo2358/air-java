# Eval

Independent public model oracle and binding-only wire oracle (no reader/writer dependencies) compare closed shapes, exact tokens/IDs/fields and array order. Five independent baseline REDs: Jump, Branch, BOOL, Unknown BOOL+Read, Premise. Public model fixtures must be structurally valid first, with I-09/I-59 retained. Writer/reader mapping probes are explicitly distinguished from API round-trips.

Positives cover each new form, complete diamond, W1 Assign/Invoke composition, empty/many dependencies, none and open remainingReads, nonsorted premise members, Unicode text and origins, unchanged validation issues and counts. Encode→decode→encode bytes identical; independent expected wire goes into decoder. Existing goldens frozen; baseline Invoke exports and historical E2E bytes compared. Separate JVMs deterministic.

Negatives: both Branch destinations and Jump closure/foreign Unit; role/type; placement; missing/extra fields and tokens. Unknown reason/dependency closure/owner/role, unsupported type/expression, required dependencies/remainingReads/reason. Premise duplicate/domain/publication ID, missing origin/member, member count/duplicates/malformed list, unknown assertion, SameDomain, extra fields, malformed authority/justification, dangling PremiseId in existing FactScope. Classification uses Code plus AIR rule, never Java message matching.

Challenges independently detect swapped arms, wrong Jump token, dropped predicate/BOOL/dependency/open remainingReads/reason/Unknown/premise/member/order/domain, permissive SameDomain/unknown assertion, reader dropping/rejecting and writer losing forms, and suppressed semantic obligations. No oracle changes in mutants. Exact source restoration and second GREEN required.

N/2N independently varies Branch/Jump sequences, Unknown dependencies, premises and disjoint members. Observe existing definitions/operations/operands/domainQueries counters and physical nodes/bytes; source review guards inventory scans. Counts do not measure all Validator work or prove asymptotic complexity, and timings are not qualification.
