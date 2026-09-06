package io.github.gustavo2358.air.model;

import java.util.*;
import java.math.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Typed AIR 2.0 values. Collections are defensively copied; no transport dependencies. */
public final class Capabilities {
    private Capabilities() {}
    public record Capability(String name, String version) {
        public Capability {
            name = text(name, "name");
            version = text(version, "version");
        }

    }
    public record Manifest(List<Capability> required, List<Capability> provided) {
        public Manifest {
            required = List.copyOf(required);
            provided = List.copyOf(provided);
            
        }

    }
    public static final Capability MEMORY_REGIONS = new Capability("memory.regions","1");
    public static final Capability LOCAL_CONTROL = new Capability("control.local","1");
    public static final Capability INDIRECT_CONTROL = new Capability("control.indirect","1");

}
