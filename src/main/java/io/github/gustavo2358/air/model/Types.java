package io.github.gustavo2358.air.model;

import java.util.*;
import java.math.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Typed AIR 2.0 values. Collections are defensively copied; no transport dependencies. */
public final class Types {
    private Types() {}
    public sealed interface Type permits Builtin, ExtensionType, LabelType {}
    public enum Builtin implements Type { BOOL, INT, DECIMAL, TEXT, BYTES }
    public sealed interface TypeRef permits Known, UnknownType {}
    public record ExtensionType(String name, SemanticVersion version) implements Type {
        public ExtensionType {
            name = text(name, "name");
            version = Objects.requireNonNull(version, "version");
            
        }

    }
    public record LabelType(UnitId unit, List<LabelId> labels) implements Type {
        public LabelType {
            unit = Objects.requireNonNull(unit, "unit");
            labels = List.copyOf(labels);
            if (labels.isEmpty() || new HashSet<>(labels).size()!=labels.size()) throw new IllegalArgumentException("label type requires a nonempty unique universe");
        }

    }
    public record Known(Type type) implements TypeRef {
        public Known {
            type = Objects.requireNonNull(type, "type");
            
        }

    }
    public record UnknownType(UncertaintyId uncertainty) implements TypeRef {
        public UnknownType {
            uncertainty = Objects.requireNonNull(uncertainty, "uncertainty");
            
        }

    }
    public static Known known(Type t) { return new Known(t); }

}
