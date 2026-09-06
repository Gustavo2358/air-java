package io.github.gustavo2358.air.model;

import java.util.*;
import java.math.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Typed AIR 2.0 values. Collections are defensively copied; no transport dependencies. */
public final class Entries {
    private Entries() {}
    public record CompletionPort(CompletionPortId id, OriginId origin) {
        public CompletionPort {
            id = Objects.requireNonNull(id, "id");
            origin = Objects.requireNonNull(origin, "origin");
            
        }

    }
    public sealed interface InitialValue permits LiteralInitial, ParameterInitial, Preserve, ExternalUnknown, Uninitialized {}
    public enum Preserve implements InitialValue { INSTANCE }
    public record LiteralInitial(Expressions.Literal value) implements InitialValue {
        public LiteralInitial {
            value = Objects.requireNonNull(value, "value");
            
        }

    }
    public record ParameterInitial(int position) implements InitialValue {
        public ParameterInitial {
            nonNegative(position, "position");
            
        }

    }
    public record ExternalUnknown(UncertaintyId reason) implements InitialValue {
        public ExternalUnknown {
            reason = Objects.requireNonNull(reason, "reason");
            
        }

    }
    public record Uninitialized(UncertaintyId reason) implements InitialValue {
        public Uninitialized {
            reason = Objects.requireNonNull(reason, "reason");
            
        }

    }
    public record InitialCondition(Place place, InitialValue value, OriginId origin, List<PremiseId> premises) {
        public InitialCondition {
            place = Objects.requireNonNull(place, "place");
            value = Objects.requireNonNull(value, "value");
            origin = Objects.requireNonNull(origin, "origin");
            premises = List.copyOf(premises);
            
        }

    }
    public record EntryState(List<InitialCondition> conditions, List<UncertaintyId> uncertainties) {
        public EntryState {
            conditions = List.copyOf(conditions);
            uncertainties = List.copyOf(uncertainties);
            
        }

    }
    public record Entry(EntryId id, Optional<LabelId> initialLabel, Interactions.Signature signature, EntryState state, OriginId origin) {
        public Entry {
            id = Objects.requireNonNull(id, "id");
            initialLabel = Objects.requireNonNull(initialLabel, "initialLabel");
            signature = Objects.requireNonNull(signature, "signature");
            state = Objects.requireNonNull(state, "state");
            origin = Objects.requireNonNull(origin, "origin");
            
        }

    }

}
