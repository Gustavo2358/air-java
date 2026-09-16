package io.github.gustavo2358.air.validation;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import io.github.gustavo2358.air.model.Ids;

/** Structural validation is not certification of producer semantics or of a consumer profile. */
public record ValidationResult(List<ValidationIssue> issues, Statistics statistics, Diagnostics diagnostics) {
    public ValidationResult {
        issues=List.copyOf(issues); Objects.requireNonNull(statistics); Objects.requireNonNull(diagnostics);
        Map<ValidationIssue.Kind,Long> retained=counts(issues);
        retained.forEach((kind,count) -> {
            if(diagnostics.count(kind)<count) throw new IllegalArgumentException("retained issues exceed total");
        });
    }
    /** Source/binary compatibility for callers constructing a result from complete diagnostics. */
    public ValidationResult(List<ValidationIssue> issues, Statistics statistics) {
        this(issues,statistics,new Diagnostics(counts(issues),
                issues.stream().noneMatch(i -> i.kind()==ValidationIssue.Kind.RESOURCE_LIMIT)));
    }
    public enum Status { INVALID_IR, INCOMPLETE_VALIDATION, STRUCTURALLY_VALID }
    public Status status() {
        if(hasIssues(ValidationIssue.Kind.INVALID_IR)) return Status.INVALID_IR;
        if(!diagnostics.traversalCompleted() || hasIssues(ValidationIssue.Kind.RESOURCE_LIMIT)
                || hasIssues(ValidationIssue.Kind.VALIDATION_LIMIT)
                || hasIssues(ValidationIssue.Kind.UNSUPPORTED_CAPABILITY)) return Status.INCOMPLETE_VALIDATION;
        return Status.STRUCTURALLY_VALID;
    }
    public boolean hasIssues(ValidationIssue.Kind kind) { return diagnostics.count(kind)>0; }
    public boolean isStructurallyValid() { return status()==Status.STRUCTURALLY_VALID; }
    /**
     * Complete operation scope for opt-in evidence-preserving partial analysis (AIR 08 §9).
     * This is not structural validity: status() deliberately remains INCOMPLETE_VALIDATION.
     * Omitted limits, interrupted checks, invalidity, extensions and Entry obligations cannot
     * grant this scoped permission. The consumer must disable strong updates at these operations.
     */
    public Optional<Set<Ids.OperationId>> unprovedOperationPreconditions() {
        long limits=diagnostics.count(ValidationIssue.Kind.VALIDATION_LIMIT);
        if(limits==0 || !diagnostics.traversalCompleted()
                || hasIssues(ValidationIssue.Kind.INVALID_IR) || hasIssues(ValidationIssue.Kind.RESOURCE_LIMIT)
                || hasIssues(ValidationIssue.Kind.UNSUPPORTED_CAPABILITY)) return Optional.empty();
        var operations=new HashSet<Ids.OperationId>();long retained=0;
        for(var issue:issues)if(issue.kind()==ValidationIssue.Kind.VALIDATION_LIMIT) {
            retained++;
            if(!issue.rule().equals("PRECONDITION_NOT_DISCHARGED"))return Optional.empty();
            var subject=issue.subject().orElse(null);
            if(subject instanceof Ids.OperationId operation)operations.add(operation);
            else if(subject instanceof Ids.OperandId operand && operand.owner() instanceof Ids.OperationOwner owner)operations.add(owner.operation());
            else return Optional.empty();
        }
        return retained==limits?Optional.of(Set.copyOf(operations)):Optional.empty();
    }
    /** Counts include omitted diagnostics; completion says whether all scheduled checks ran. */
    public record Diagnostics(Map<ValidationIssue.Kind,Long> counts, boolean traversalCompleted) {
        public Diagnostics {
            counts=Map.copyOf(counts);
            if(counts.values().stream().anyMatch(n -> n<0)) throw new IllegalArgumentException("negative count");
        }
        public long count(ValidationIssue.Kind kind) { return counts.getOrDefault(kind,0L); }
    }
    private static Map<ValidationIssue.Kind,Long> counts(List<ValidationIssue> issues) {
        Map<ValidationIssue.Kind,Long> result=new EnumMap<>(ValidationIssue.Kind.class);
        for(ValidationIssue issue:issues) result.merge(issue.kind(),1L,Math::addExact);
        return result;
    }
    public record Statistics(int entities, int operands, int operations, long domainQueries) {}
}
