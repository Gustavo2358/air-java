package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.Ids;
import java.util.Objects;
import java.util.Optional;

/** A validation diagnostic, never a semantic fact added to the supplied AIR. */
public record ValidationIssue(Kind kind, String rule, Optional<Ids.Id> subject, String detail) {
    public enum Kind { INVALID_IR, UNSUPPORTED_CAPABILITY, SEMANTIC_OBLIGATION, VALIDATION_LIMIT }
    public ValidationIssue {
        Objects.requireNonNull(kind,"kind"); Objects.requireNonNull(rule,"rule");
        Objects.requireNonNull(subject,"subject"); Objects.requireNonNull(detail,"detail");
    }
}
