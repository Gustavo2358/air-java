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
    public sealed interface Location permits LineColumns, Offsets {}
    public record Position(BigInteger line, BigInteger column) {
        public Position {
            line = Objects.requireNonNull(line, "line");
            column = Objects.requireNonNull(column, "column");
            nonNegative(line, "line");
            nonNegative(column, "column");
            
        }

    }
    public record Span(Position start, Position end, BigInteger lineBase, BigInteger columnBase, ColumnUnit columnUnit, boolean endExclusive) {
        public Span {
            start = Objects.requireNonNull(start, "start");
            end = Objects.requireNonNull(end, "end");
            lineBase = Objects.requireNonNull(lineBase, "lineBase");
            columnBase = Objects.requireNonNull(columnBase, "columnBase");
            nonNegative(lineBase, "lineBase");
            nonNegative(columnBase, "columnBase");
            columnUnit = Objects.requireNonNull(columnUnit, "columnUnit");
            if (lineBase.compareTo(BigInteger.ONE)>0 || columnBase.compareTo(BigInteger.ONE)>0)
                throw new IllegalArgumentException("coordinate bases must be zero or one");
            if (start.line().compareTo(lineBase)<0 || end.line().compareTo(lineBase)<0
                    || start.column().compareTo(columnBase)<0 || end.column().compareTo(columnBase)<0
                    || start.line().compareTo(end.line())>0
                    || (start.line().equals(end.line()) && start.column().compareTo(end.column())>0))
                throw new IllegalArgumentException("invalid source span");
        }

    }
    public record LineColumns(Span span) implements Location {
        public LineColumns {
            span = Objects.requireNonNull(span, "span");
        }
    }
    public record Offsets(BigInteger start, BigInteger end, String unit, boolean endExclusive) implements Location {
        public Offsets {
            start = Objects.requireNonNull(start, "start");
            end = Objects.requireNonNull(end, "end");
            unit = text(unit, "unit");
            nonNegative(start, "start");
            nonNegative(end, "end");
            if(start.compareTo(end)>0) throw new IllegalArgumentException("offset end precedes start");
        }
    }
    public record IncludeFrame(ArtifactId including, ArtifactId included, String requestedName, Optional<Location> site) {
        public IncludeFrame {
            including = Objects.requireNonNull(including, "including");
            included = Objects.requireNonNull(included, "included");
            requestedName = text(requestedName, "requestedName");
            site = Objects.requireNonNull(site, "site");
            
        }

    }
    public record Written(OriginId id, ArtifactId artifact, Optional<Location> location, List<IncludeFrame> includes, boolean exact) implements Origin {
        public Written {
            id = Objects.requireNonNull(id, "id");
            artifact = Objects.requireNonNull(artifact, "artifact");
            location = Objects.requireNonNull(location, "location");
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
