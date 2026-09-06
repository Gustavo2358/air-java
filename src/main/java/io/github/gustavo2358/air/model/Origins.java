package io.github.gustavo2358.air.model;

import java.util.*;
import java.math.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Typed AIR 2.0 values. Collections are defensively copied; no transport dependencies. */
public final class Origins {
    private Origins() {}
    public sealed interface Origin permits Written, Derived, Contractual, Unavailable { OriginId id(); }
    public enum ColumnUnit { UNICODE_SCALAR, UTF16_CODE_UNIT, OCTET }
    public record Position(int line, int column) {
        public Position {
            nonNegative(line, "line");
            nonNegative(column, "column");
            
        }

    }
    public record Span(Position start, Position end, int lineBase, int columnBase, ColumnUnit columnUnit, boolean endExclusive) {
        public Span {
            start = Objects.requireNonNull(start, "start");
            end = Objects.requireNonNull(end, "end");
            nonNegative(lineBase, "lineBase");
            nonNegative(columnBase, "columnBase");
            columnUnit = Objects.requireNonNull(columnUnit, "columnUnit");
            if (lineBase>1 || columnBase>1) throw new IllegalArgumentException("coordinate bases must be zero or one");
            if (start.line()<lineBase || end.line()<lineBase || start.column()<columnBase || end.column()<columnBase || start.line()>end.line() || (start.line()==end.line() && start.column()>end.column())) throw new IllegalArgumentException("invalid source span");
        }

    }
    public record IncludeFrame(ArtifactId including, ArtifactId included, String requestedName, Optional<Span> site) {
        public IncludeFrame {
            including = Objects.requireNonNull(including, "including");
            included = Objects.requireNonNull(included, "included");
            requestedName = text(requestedName, "requestedName");
            site = Objects.requireNonNull(site, "site");
            
        }

    }
    public record Written(OriginId id, ArtifactId artifact, Optional<Span> span, List<IncludeFrame> includes, boolean exact) implements Origin {
        public Written {
            id = Objects.requireNonNull(id, "id");
            artifact = Objects.requireNonNull(artifact, "artifact");
            span = Objects.requireNonNull(span, "span");
            includes = List.copyOf(includes);
            
        }

    }
    public record Derived(OriginId id, List<OriginId> inputs, String rule) implements Origin {
        public Derived {
            id = Objects.requireNonNull(id, "id");
            inputs = List.copyOf(inputs);
            rule = text(rule, "rule");
            if (inputs.isEmpty()) throw new IllegalArgumentException("derived origin requires inputs");
        }

    }
    public record Contractual(OriginId id, String authority, String version) implements Origin {
        public Contractual {
            id = Objects.requireNonNull(id, "id");
            authority = text(authority, "authority");
            version = text(version, "version");
            
        }

    }
    public record Unavailable(OriginId id, String reason) implements Origin {
        public Unavailable {
            id = Objects.requireNonNull(id, "id");
            reason = text(reason, "reason");
            
        }

    }
    public record Artifact(ArtifactId id, String logicalName, Optional<String> contentDigest) {
        public Artifact {
            id = Objects.requireNonNull(id, "id");
            logicalName = text(logicalName, "logicalName");
            contentDigest = Objects.requireNonNull(contentDigest, "contentDigest");
            
        }

    }

}
