package io.github.gustavo2358.air.model;

import java.util.*;
import java.math.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Typed AIR 2.0 values. Collections are defensively copied; no transport dependencies. */
public final class Capabilities {
    private Capabilities() {}
    public record Capability(String name, int major) {
        public Capability {
            name = text(name, "name");
            nonNegative(major, "major");
            if (major == 0) throw new IllegalArgumentException("capability major must be positive");
        }

    }
    public record Manifest(List<Capability> required, List<Capability> provided) {
        public Manifest {
            required = List.copyOf(required);
            provided = List.copyOf(provided);
            
        }

    }
    public static final Capability MEMORY_REGIONS = new Capability("memory.regions",1);
    public static final Capability LOCAL_CONTROL = new Capability("control.local",1);
    public static final Capability INDIRECT_CONTROL = new Capability("control.indirect",1);

}
