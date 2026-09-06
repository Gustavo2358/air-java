package io.github.gustavo2358.air.model;

import java.util.*;
import java.math.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Typed AIR 2.0 values. Collections are defensively copied; no transport dependencies. */
public final class Evidence {
    private Evidence() {}
    public enum Dimension { CONTROL, STORAGE, EFFECTS, VALUES, DEPENDENCIES }
    public enum PrecisionStatus { EXACT, CONSERVATIVE, OPEN, UNAVAILABLE, NOT_APPLICABLE }
    public enum CoverageStatus { MODELED, ABSTRACTED, UNSUPPORTED, INPUT_MISSING }
    public enum InventoryStatus { COMPLETE, PARTIAL, UNAVAILABLE }
    public record Claim(Scopes.FactScope scope, PrecisionStatus status, List<UncertaintyId> reasons) {
        public Claim {
            scope = Objects.requireNonNull(scope, "scope");
            status = Objects.requireNonNull(status, "status");
            reasons = List.copyOf(reasons);
            
        }

    }
    public record Precision(Claim control, Claim storage, Claim effects, Claim values, Claim dependencies) {
        public Precision {
            control = Objects.requireNonNull(control, "control");
            storage = Objects.requireNonNull(storage, "storage");
            effects = Objects.requireNonNull(effects, "effects");
            values = Objects.requireNonNull(values, "values");
            dependencies = Objects.requireNonNull(dependencies, "dependencies");
            
        }
        public Claim claim(Dimension d) { return switch(d) {
            case CONTROL -> control; case STORAGE -> storage; case EFFECTS -> effects;
            case VALUES -> values; case DEPENDENCIES -> dependencies;
        }; }
    }
    public record Uncertainty(UncertaintyId id, String code, List<Dimension> dimensions, Scopes.FactScope scope, String reason, OriginId origin) {
        public Uncertainty {
            id = Objects.requireNonNull(id, "id");
            code = text(code, "code");
            dimensions = List.copyOf(dimensions);
            scope = Objects.requireNonNull(scope, "scope");
            reason = text(reason, "reason");
            origin = Objects.requireNonNull(origin, "origin");
            if (dimensions.isEmpty()) throw new IllegalArgumentException("uncertainty must identify affected dimensions");
        }

    }
    public record Elimination(PremiseId justification, String rule) {
        public Elimination {
            justification = Objects.requireNonNull(justification, "justification");
            rule = text(rule, "rule");
            
        }

    }
    public record CoverageItem(String sourceKey, OriginId origin, CoverageStatus status, List<Id> outputs, List<UncertaintyId> uncertainties, Optional<Elimination> elimination) {
        public CoverageItem {
            sourceKey = text(sourceKey, "sourceKey");
            origin = Objects.requireNonNull(origin, "origin");
            status = Objects.requireNonNull(status, "status");
            outputs = List.copyOf(outputs);
            uncertainties = List.copyOf(uncertainties);
            elimination = Objects.requireNonNull(elimination, "elimination");
            
        }

    }
    public record Coverage(InventoryStatus inventory, Scopes.FactScope scope, List<CoverageItem> items, List<UncertaintyId> uncertainties) {
        public Coverage {
            inventory = Objects.requireNonNull(inventory, "inventory");
            scope = Objects.requireNonNull(scope, "scope");
            items = List.copyOf(items);
            uncertainties = List.copyOf(uncertainties);
            
        }

    }

}
