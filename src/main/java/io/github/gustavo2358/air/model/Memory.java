package io.github.gustavo2358.air.model;

import java.util.*;
import java.math.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Typed AIR 2.0 values. Collections are defensively copied; no transport dependencies. */
public final class Memory {
    private Memory() {}
    public enum Lifetime { ACTIVATION, PERSISTENT, EXTERNAL }
    public enum Visibility { PRIVATE, SHARED, UNKNOWN }
    public enum ByteOrder { LITTLE, BIG }
    public sealed interface Codec permits IdentityBytes, AsciiText, BinaryCodec, ExtensionCodec, UnknownCodec {}
    public enum IdentityBytes implements Codec { INSTANCE }
    public enum AsciiText implements Codec { INSTANCE }
    public record BinaryCodec(boolean signed, BigInteger width, ByteOrder order) implements Codec {
        public BinaryCodec {
            width = Objects.requireNonNull(width, "width");
            nonNegative(width, "width");
            order = Objects.requireNonNull(order, "order");
            if (width.signum()==0 || !width.mod(BigInteger.valueOf(8)).equals(BigInteger.ZERO))
                throw new IllegalArgumentException("width must be positive multiple of eight");
        }

    }
    public record ExtensionCodec(String name, String version, Types.TypeRef logicalType) implements Codec {
        public ExtensionCodec {
            name = text(name, "name");
            version = text(version, "version");
            logicalType = Objects.requireNonNull(logicalType, "logicalType");
            
        }

    }
    public record UnknownCodec(Types.TypeRef logicalType, UncertaintyId reason) implements Codec {
        public UnknownCodec {
            logicalType = Objects.requireNonNull(logicalType, "logicalType");
            reason = Objects.requireNonNull(reason, "reason");
            
        }

    }
    public sealed interface Binding permits CellBinding, ViewBinding, AliasBinding, AlternativesBinding, UnknownBinding {}
    public record CellBinding(StorageId storage) implements Binding {
        public CellBinding {
            storage = Objects.requireNonNull(storage, "storage");
            
        }

    }
    public record ViewBinding(StorageId region, BigInteger offset, BigInteger extent, Codec codec) implements Binding {
        public ViewBinding {
            region = Objects.requireNonNull(region, "region");
            offset = Objects.requireNonNull(offset, "offset");
            extent = Objects.requireNonNull(extent, "extent");
            codec = Objects.requireNonNull(codec, "codec");
            nonNegative(offset,"offset"); nonNegative(extent,"extent");
        }

    }
    public record AliasBinding(ObjectId object) implements Binding {
        public AliasBinding {
            object = Objects.requireNonNull(object, "object");
            
        }

    }
    public record AlternativesBinding(List<Binding> alternatives, Scopes.MemoryBound remainder) implements Binding {
        public AlternativesBinding {
            alternatives = List.copyOf(alternatives);
            remainder = Objects.requireNonNull(remainder, "remainder");
            if (alternatives.isEmpty() && remainder instanceof Scopes.NoMemory) throw new IllegalArgumentException("empty closed association");
        }

    }
    public record UnknownBinding(Scopes.MemoryScope scope, UncertaintyId reason) implements Binding {
        public UnknownBinding {
            scope = Objects.requireNonNull(scope, "scope");
            reason = Objects.requireNonNull(reason, "reason");
            
        }

    }
    public record StorageHeader(StorageId id, Optional<UnitId> owner, Lifetime lifetime, Visibility visibility, OriginId origin) {
        public StorageHeader {
            id = Objects.requireNonNull(id, "id");
            owner = Objects.requireNonNull(owner, "owner");
            lifetime = Objects.requireNonNull(lifetime, "lifetime");
            visibility = Objects.requireNonNull(visibility, "visibility");
            origin = Objects.requireNonNull(origin, "origin");
            if(lifetime==Lifetime.ACTIVATION && owner.isEmpty()) throw new IllegalArgumentException("activation storage needs owner");
        }

    }
    public sealed interface Storage permits Cell, Region { StorageHeader header(); }
    public record Cell(StorageHeader header, Types.TypeRef typeRef) implements Storage {
        public Cell {
            header = Objects.requireNonNull(header, "header");
            typeRef = Objects.requireNonNull(typeRef, "typeRef");
            
        }

    }
    public record Region(StorageHeader header, Optional<BigInteger> extent, Optional<UncertaintyId> extentUnknown) implements Storage {
        public Region {
            header = Objects.requireNonNull(header, "header");
            extent = Objects.requireNonNull(extent, "extent");
            extentUnknown = Objects.requireNonNull(extentUnknown, "extentUnknown");
            extent.ifPresent(n -> nonNegative(n,"extent")); if(extent.isPresent()==extentUnknown.isPresent()) throw new IllegalArgumentException("region extent must be known xor explicitly unknown");
        }

    }
    public record ObjectDeclaration(ObjectId id, Optional<String> displayName, Types.TypeRef typeRef, Binding storage, Visibility visibility, OriginId origin, Evidence.CoverageStatus coverage, Evidence.Precision precision) {
        public ObjectDeclaration {
            id = Objects.requireNonNull(id, "id");
            displayName = Objects.requireNonNull(displayName, "displayName");
            typeRef = Objects.requireNonNull(typeRef, "typeRef");
            storage = Objects.requireNonNull(storage, "storage");
            visibility = Objects.requireNonNull(visibility, "visibility");
            origin = Objects.requireNonNull(origin, "origin");
            coverage = Objects.requireNonNull(coverage, "coverage");
            precision = Objects.requireNonNull(precision, "precision");
            
        }

    }
    public record ByteRange(StorageId region, Expression offset, Expression extent) {
        public ByteRange {
            region = Objects.requireNonNull(region, "region");
            offset = Objects.requireNonNull(offset, "offset");
            extent = Objects.requireNonNull(extent, "extent");
            
        }

    }

}
