package io.github.gustavo2358.air.validation;

import java.util.List;

/** Structural validation is not certification of producer semantics or of a consumer profile. */
public record ValidationResult(List<ValidationIssue> issues, Statistics statistics) {
    public ValidationResult { issues=List.copyOf(issues); }
    public enum Status { INVALID_IR, INCOMPLETE_VALIDATION, STRUCTURALLY_VALID }
    public Status status() {
        if(issues.stream().anyMatch(i -> i.kind()==ValidationIssue.Kind.INVALID_IR)) return Status.INVALID_IR;
        if(issues.stream().anyMatch(i -> i.kind()==ValidationIssue.Kind.VALIDATION_LIMIT
                || i.kind()==ValidationIssue.Kind.UNSUPPORTED_CAPABILITY)) return Status.INCOMPLETE_VALIDATION;
        return Status.STRUCTURALLY_VALID;
    }
    public boolean isStructurallyValid() { return status()==Status.STRUCTURALLY_VALID; }
    public record Statistics(int entities, int operands, int operations, long domainQueries) {}
}
