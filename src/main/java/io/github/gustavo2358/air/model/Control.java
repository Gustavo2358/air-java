package io.github.gustavo2358.air.model;

import java.util.*;
import java.math.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Typed AIR 2.0 values. Collections are defensively copied; no transport dependencies. */
public final class Control {
    private Control() {}
    public sealed interface ExceptionDestination permits Handler, Propagate {}
    public enum Propagate implements ExceptionDestination { INSTANCE }
    public record Handler(LabelId label) implements ExceptionDestination {
        public Handler {
            label = Objects.requireNonNull(label, "label");
            
        }

    }
    public sealed interface Alternative permits Normal, Exceptional, AnyException, HaltAlternative, Diverge {}
    public enum HaltAlternative implements Alternative { INSTANCE }
    public enum Diverge implements Alternative { INSTANCE }
    public record Normal(LabelId label) implements Alternative {
        public Normal {
            label = Objects.requireNonNull(label, "label");
            
        }

    }
    public record Exceptional(String tag, ExceptionDestination destination) implements Alternative {
        public Exceptional {
            tag = text(tag, "tag");
            destination = Objects.requireNonNull(destination, "destination");
            
        }

    }
    public record AnyException(ExceptionDestination destination) implements Alternative {
        public AnyException {
            destination = Objects.requireNonNull(destination, "destination");
            
        }

    }
    public record Envelope(List<Alternative> known, Scopes.ControlBound remainder) {
        public Envelope {
            known = List.copyOf(known);
            remainder = Objects.requireNonNull(remainder, "remainder");
            if(known.isEmpty() && remainder instanceof Scopes.NoControl) throw new IllegalArgumentException("empty closed outcomes are not implicit divergence");
        }

    }
    public sealed interface OutcomeKey permits NormalOutcome, ExceptionOutcome, OtherExceptionOutcome, HaltOutcome, DivergeOutcome {}
    public enum NormalOutcome implements OutcomeKey { INSTANCE }
    public enum OtherExceptionOutcome implements OutcomeKey { INSTANCE }
    public enum HaltOutcome implements OutcomeKey { INSTANCE }
    public enum DivergeOutcome implements OutcomeKey { INSTANCE }
    public record ExceptionOutcome(String tag) implements OutcomeKey {
        public ExceptionOutcome {
            tag = text(tag, "tag");
            
        }

    }
    public sealed interface ProgramPoint permits Before, After, EntryPoint, ExitPoint {}
    public record Before(OperationId operation) implements ProgramPoint {
        public Before {
            operation = Objects.requireNonNull(operation, "operation");
            
        }

    }
    public record After(OperationId operation, OutcomeKey outcome) implements ProgramPoint {
        public After {
            operation = Objects.requireNonNull(operation, "operation");
            outcome = Objects.requireNonNull(outcome, "outcome");
            
        }

    }
    public record EntryPoint(EntryId entry) implements ProgramPoint {
        public EntryPoint {
            entry = Objects.requireNonNull(entry, "entry");
            
        }

    }
    public record ExitPoint(UnitId unit, OutcomeKey outcome) implements ProgramPoint {
        public ExitPoint {
            unit = Objects.requireNonNull(unit, "unit");
            outcome = Objects.requireNonNull(outcome, "outcome");
            
        }

    }

}
