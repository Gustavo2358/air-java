package io.github.gustavo2358.air.model;

import java.util.*;
import java.math.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Typed AIR 2.0 values. Collections are defensively copied; no transport dependencies. */
public final class Scopes {
    private Scopes() {}
    public sealed interface FactScope permits PublicationScope, UnitScope, EntityScope {}
    public record PublicationScope(PublicationId publication) implements FactScope {
        public PublicationScope {
            publication = Objects.requireNonNull(publication, "publication");
            
        }

    }
    public record UnitScope(UnitId unit) implements FactScope {
        public UnitScope {
            unit = Objects.requireNonNull(unit, "unit");
            
        }

    }
    public record EntityScope(List<Id> entities) implements FactScope {
        public EntityScope {
            entities = List.copyOf(entities);
            if (entities.isEmpty()) throw new IllegalArgumentException("entity scope must not be empty");
        }

    }
    public sealed interface MemoryScope permits ObjectsMemory, StorageMemory, VisibleMemory, AllMemory, MemoryUnion {}
    public record ObjectsMemory(List<ObjectId> objects) implements MemoryScope {
        public ObjectsMemory {
            objects = List.copyOf(objects);
            if (objects.isEmpty()) throw new IllegalArgumentException("use NoMemory instead of empty scope");
        }

    }
    public record StorageMemory(List<StorageId> storage) implements MemoryScope {
        public StorageMemory {
            storage = List.copyOf(storage);
            if (storage.isEmpty()) throw new IllegalArgumentException("use NoMemory instead of empty scope");
        }

    }
    public record VisibleMemory(UnitId unit, boolean includingExternal) implements MemoryScope {
        public VisibleMemory {
            unit = Objects.requireNonNull(unit, "unit");
            
        }

    }
    public record AllMemory(PublicationId publication, boolean includingEnvironment) implements MemoryScope {
        public AllMemory {
            publication = Objects.requireNonNull(publication, "publication");
            
        }

    }
    public record MemoryUnion(List<MemoryScope> members) implements MemoryScope {
        public MemoryUnion {
            members = List.copyOf(members);
            if (members.isEmpty()) throw new IllegalArgumentException("empty union");
        }

    }
    public sealed interface MemoryBound permits NoMemory, WithinMemory {}
    public enum NoMemory implements MemoryBound { INSTANCE }
    public record WithinMemory(MemoryScope scope) implements MemoryBound {
        public WithinMemory {
            scope = Objects.requireNonNull(scope, "scope");
            
        }

    }
    public sealed interface ControlScope permits LabelsControl, UnitControl, AllControl, ControlUnion {}
    public record LabelsControl(List<LabelId> labels) implements ControlScope {
        public LabelsControl {
            labels = List.copyOf(labels);
            
        }

    }
    public record UnitControl(UnitId unit, boolean labels, boolean normalExit, boolean exceptionalExit, boolean halt, boolean diverge, boolean externalControl) implements ControlScope {
        public UnitControl {
            unit = Objects.requireNonNull(unit, "unit");
            
        }

    }
    public record AllControl(PublicationId publication) implements ControlScope {
        public AllControl {
            publication = Objects.requireNonNull(publication, "publication");
            
        }

    }
    public record ControlUnion(List<ControlScope> members) implements ControlScope {
        public ControlUnion {
            members = List.copyOf(members);
            
        }

    }
    public sealed interface ControlBound permits NoControl, WithinControl {}
    public enum NoControl implements ControlBound { INSTANCE }
    public record WithinControl(ControlScope scope) implements ControlBound {
        public WithinControl {
            scope = Objects.requireNonNull(scope, "scope");
            
        }

    }
    public sealed interface DependencyBound permits NoResources, AnyResource, ResourceCategories {}
    public enum NoResources implements DependencyBound { INSTANCE }
    public enum AnyResource implements DependencyBound { INSTANCE }
    public record ResourceCategories(List<String> categories) implements DependencyBound {
        public ResourceCategories {
            categories = List.copyOf(categories);
            
        }

    }

}
