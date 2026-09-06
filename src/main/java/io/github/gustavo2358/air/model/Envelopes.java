package io.github.gustavo2358.air.model;

import java.util.*;
import java.math.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Typed AIR 2.0 values. Collections are defensively copied; no transport dependencies. */
public final class Envelopes {
    private Envelopes() {}
    public record MemoryEnvelope(List<OperandId> knownReads, Scopes.MemoryBound otherReads, List<OperandId> knownWrites, Scopes.MemoryBound otherWrites, List<OperandId> mustOverwrite) {
        public MemoryEnvelope {
            knownReads = List.copyOf(knownReads);
            otherReads = Objects.requireNonNull(otherReads, "otherReads");
            knownWrites = List.copyOf(knownWrites);
            otherWrites = Objects.requireNonNull(otherWrites, "otherWrites");
            mustOverwrite = List.copyOf(mustOverwrite);
            
        }

    }
    public record ResourceUse(String action, Interactions.Target target, Control.ProgramPoint point, OriginId origin) {
        public ResourceUse {
            action = text(action, "action");
            target = Objects.requireNonNull(target, "target");
            point = Objects.requireNonNull(point, "point");
            origin = Objects.requireNonNull(origin, "origin");
            
        }

    }
    public record DependencyEnvelope(List<ResourceUse> known, Scopes.DependencyBound remainder) {
        public DependencyEnvelope {
            known = List.copyOf(known);
            remainder = Objects.requireNonNull(remainder, "remainder");
            
        }

    }
    public record Envelope(MemoryEnvelope memory, Control.Envelope control, DependencyEnvelope dependencies) {
        public Envelope {
            memory = Objects.requireNonNull(memory, "memory");
            control = Objects.requireNonNull(control, "control");
            dependencies = Objects.requireNonNull(dependencies, "dependencies");
            
        }

    }

}
