package io.github.gustavo2358.air.validation;

/** Validation work limits; hitting a limit never reports a successful check. */
public record ValidationOptions(int maximumNesting, int maximumEntities, int maximumIssues) {
    public static ValidationOptions defaults() { return new ValidationOptions(128, 2_000_000, 10_000); }
    public ValidationOptions {
        if(maximumNesting<1 || maximumNesting>512 || maximumEntities<1 || maximumIssues<1)
            throw new IllegalArgumentException("positive limits required; nesting must be <=512");
    }
}
