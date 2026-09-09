package io.github.gustavo2358.air.validation;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
