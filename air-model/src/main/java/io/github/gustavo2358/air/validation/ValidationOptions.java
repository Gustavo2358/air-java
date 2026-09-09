package io.github.gustavo2358.air.validation;

/**
 * Opt-in operational work bounds and diagnostic retention. Maximum int denotes the
 * representability ceiling of this in-memory Java API, not an AIR semantic limit.
 * maximumIssues limits retained messages only; validation continues and counts all kinds.
 */
public record ValidationOptions(int maximumNesting, int maximumEntities, int maximumIssues) {
    public static ValidationOptions defaults() {
        return new ValidationOptions(Integer.MAX_VALUE, Integer.MAX_VALUE, 10_000);
    }
    public ValidationOptions {
        if(maximumNesting<1 || maximumEntities<1 || maximumIssues<1)
            throw new IllegalArgumentException("positive operational limits required");
    }
}
