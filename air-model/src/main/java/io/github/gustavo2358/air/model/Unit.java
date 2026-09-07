package io.github.gustavo2358.air.model;


/** Closed executable unit; body absence and absence of entries are distinct facts. */
public record Unit(Ids.UnitId id, java.util.Optional<Ids.UnitId> containingUnit,
                   java.util.List<Memory.ObjectDeclaration> objects,
                   java.util.List<Ids.ObjectId> visibleObjects,
                   java.util.List<Entries.Entry> entries,
                   java.util.List<Sequence> sequences,
                   java.util.List<Entries.CompletionPort> completionPorts,
                   BodyAvailability body, java.util.Optional<Ids.UncertaintyId> bodyUnavailable,
                   Evidence.Coverage coverage, Ids.OriginId origin) {
    public enum BodyAvailability { AVAILABLE, UNAVAILABLE }
    public Unit {
        java.util.Objects.requireNonNull(id,"id");
        java.util.Objects.requireNonNull(containingUnit,"containingUnit");
        objects=java.util.List.copyOf(objects); visibleObjects=java.util.List.copyOf(visibleObjects);
        entries=java.util.List.copyOf(entries); sequences=java.util.List.copyOf(sequences);
        completionPorts=java.util.List.copyOf(completionPorts);
        java.util.Objects.requireNonNull(body,"body");
        java.util.Objects.requireNonNull(bodyUnavailable,"bodyUnavailable");
        java.util.Objects.requireNonNull(coverage,"coverage");
        java.util.Objects.requireNonNull(origin,"origin");
        if(body==BodyAvailability.AVAILABLE && (entries.isEmpty() || sequences.isEmpty() || bodyUnavailable.isPresent()))
            throw new IllegalArgumentException("available body requires entry and sequence, not missing-body reason");
        if(body==BodyAvailability.UNAVAILABLE && (!sequences.isEmpty() || bodyUnavailable.isEmpty()))
            throw new IllegalArgumentException("unavailable body requires reason and no sequences");
    }
}
