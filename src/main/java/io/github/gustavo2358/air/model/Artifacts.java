package io.github.gustavo2358.air.model;

import java.util.*;
import java.math.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Typed AIR 2.0 values. Collections are defensively copied; no transport dependencies. */
public final class Artifacts {
    private Artifacts() {}
    public sealed interface RelationTarget permits InternalArtifact, ExternalArtifact {}
    public record InternalArtifact(ArtifactId artifact) implements RelationTarget {
        public InternalArtifact {
            artifact = Objects.requireNonNull(artifact, "artifact");
            
        }

    }
    public record ExternalArtifact(Interactions.LiteralTarget resource) implements RelationTarget {
        public ExternalArtifact {
            resource = Objects.requireNonNull(resource, "resource");
            
        }

    }
    public record Relation(RelationId id, ArtifactId source, RelationTarget destination, String kind, OriginId origin, Evidence.CoverageStatus coverage) {
        public Relation {
            id = Objects.requireNonNull(id, "id");
            source = Objects.requireNonNull(source, "source");
            destination = Objects.requireNonNull(destination, "destination");
            kind = text(kind, "kind");
            origin = Objects.requireNonNull(origin, "origin");
            coverage = Objects.requireNonNull(coverage, "coverage");
            
        }

    }

}
