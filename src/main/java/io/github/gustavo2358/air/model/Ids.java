package io.github.gustavo2358.air.model;

import java.util.*;
import java.math.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Typed AIR 2.0 values. Collections are defensively copied; no transport dependencies. */
public final class Ids {
    private Ids() {}
    public sealed interface Id permits PublicationId, UnitId, EntryId, LabelId,
            OperationId, OperandId, ObjectId, StorageId, ResourceId, ArtifactId,
            ArtifactRelationId, OriginId, UncertaintyId, PremiseId, CompletionPortId {
        PublicationId publication();
        String localId();
    }
    public sealed interface OperandOwner permits OperationOwner, EntryOwner {
        PublicationId publication();
        UnitId unit();
    }
    public record PublicationId(String localId) implements Id {
        public PublicationId {
            localId = text(localId, "localId");
            
        }
        @Override public PublicationId publication() { return this; }
    }
    public record UnitId(PublicationId publication, String localId) implements Id {
        public UnitId {
            publication = Objects.requireNonNull(publication, "publication");
            localId = text(localId, "localId");
            
        }

    }
    public record StorageId(PublicationId publication, String localId) implements Id {
        public StorageId {
            publication = Objects.requireNonNull(publication, "publication");
            localId = text(localId, "localId");
            
        }

    }
    public record ResourceId(PublicationId publication, String localId) implements Id {
        public ResourceId {
            publication = Objects.requireNonNull(publication, "publication");
            localId = text(localId, "localId");
            
        }

    }
    public record ArtifactId(PublicationId publication, String localId) implements Id {
        public ArtifactId {
            publication = Objects.requireNonNull(publication, "publication");
            localId = text(localId, "localId");
            
        }

    }
    public record OriginId(PublicationId publication, String localId) implements Id {
        public OriginId {
            publication = Objects.requireNonNull(publication, "publication");
            localId = text(localId, "localId");
            
        }

    }
    public record UncertaintyId(PublicationId publication, String localId) implements Id {
        public UncertaintyId {
            publication = Objects.requireNonNull(publication, "publication");
            localId = text(localId, "localId");
            
        }

    }
    public record PremiseId(PublicationId publication, String localId) implements Id {
        public PremiseId {
            publication = Objects.requireNonNull(publication, "publication");
            localId = text(localId, "localId");
            
        }

    }
    public record ArtifactRelationId(PublicationId publication, String localId) implements Id {
        public ArtifactRelationId {
            publication = Objects.requireNonNull(publication, "publication");
            localId = text(localId, "localId");
            
        }

    }
    public record EntryId(UnitId unit, String localId) implements Id {
        public EntryId {
            unit = Objects.requireNonNull(unit, "unit");
            localId = text(localId, "localId");
            
        }
        @Override public PublicationId publication() { return unit.publication(); }
    }
    public record LabelId(UnitId unit, String localId) implements Id {
        public LabelId {
            unit = Objects.requireNonNull(unit, "unit");
            localId = text(localId, "localId");
            
        }
        @Override public PublicationId publication() { return unit.publication(); }
    }
    public record OperationId(UnitId unit, String localId) implements Id {
        public OperationId {
            unit = Objects.requireNonNull(unit, "unit");
            localId = text(localId, "localId");
            
        }
        @Override public PublicationId publication() { return unit.publication(); }
    }
    public record ObjectId(UnitId unit, String localId) implements Id {
        public ObjectId {
            unit = Objects.requireNonNull(unit, "unit");
            localId = text(localId, "localId");
            
        }
        @Override public PublicationId publication() { return unit.publication(); }
    }
    public record CompletionPortId(UnitId unit, String localId) implements Id {
        public CompletionPortId {
            unit = Objects.requireNonNull(unit, "unit");
            localId = text(localId, "localId");
            
        }
        @Override public PublicationId publication() { return unit.publication(); }
    }
    public record OperationOwner(OperationId operation) implements OperandOwner {
        public OperationOwner {
            operation = Objects.requireNonNull(operation, "operation");
            
        }
        @Override public UnitId unit() { return operation.unit(); }
        @Override public PublicationId publication() { return unit().publication(); }
    }
    public record EntryOwner(EntryId entry) implements OperandOwner {
        public EntryOwner {
            entry = Objects.requireNonNull(entry, "entry");
            
        }
        @Override public UnitId unit() { return entry.unit(); }
        @Override public PublicationId publication() { return unit().publication(); }
    }
    public record OperandId(OperandOwner owner, String localId) implements Id {
        public OperandId {
            owner = Objects.requireNonNull(owner, "owner");
            localId = text(localId, "localId");
            
        }
        @Override public PublicationId publication() { return owner.publication(); }
    }

}
