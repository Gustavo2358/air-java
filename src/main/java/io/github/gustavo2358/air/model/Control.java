package io.github.gustavo2358.air.model;

import java.util.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Distinct invocation outcomes and generic conservative control envelopes. */
public final class Control {
    private Control() {}

    public sealed interface ExceptionDestination permits Handler, Propagate {}
    public enum Propagate implements ExceptionDestination { INSTANCE }
    public record Handler(LabelId label) implements ExceptionDestination {
        public Handler { label = Objects.requireNonNull(label, "label"); }
    }

    public sealed interface ControlAlternative permits InvocationAlternative, JumpAlternative,
            ReturnAlternative, ContinueAlternative {}
    public sealed interface InvocationAlternative extends ControlAlternative permits Normal,
            Exceptional, AnyException, HaltAlternative, Diverge {}
    public enum HaltAlternative implements InvocationAlternative { INSTANCE }
    public enum Diverge implements InvocationAlternative { INSTANCE }
    public enum ReturnAlternative implements ControlAlternative { INSTANCE }
    public enum ContinueAlternative implements ControlAlternative { INSTANCE }
    public record Normal(LabelId label) implements InvocationAlternative {
        public Normal { label = Objects.requireNonNull(label, "label"); }
    }
    public record JumpAlternative(LabelId label) implements ControlAlternative {
        public JumpAlternative { label = Objects.requireNonNull(label, "label"); }
    }
    public record Exceptional(String tag, ExceptionDestination destination)
            implements InvocationAlternative {
        public Exceptional {
            tag = text(tag, "tag");
            destination = Objects.requireNonNull(destination, "destination");
        }
    }
    public record AnyException(ExceptionDestination destination) implements InvocationAlternative {
        public AnyException { destination = Objects.requireNonNull(destination, "destination"); }
    }

    public record InvocationOutcomes(List<InvocationAlternative> known, Scopes.ControlBound remainder) {
        public InvocationOutcomes {
            known = List.copyOf(known);
            remainder = Objects.requireNonNull(remainder, "remainder");
            if(known.isEmpty() && remainder instanceof Scopes.NoControl)
                throw new IllegalArgumentException("empty closed outcomes are not implicit divergence");
        }
    }
    public record ControlEnvelope(List<ControlAlternative> known, Scopes.ControlBound remainder) {
        public ControlEnvelope {
            known = List.copyOf(known);
            remainder = Objects.requireNonNull(remainder, "remainder");
            if(known.isEmpty() && remainder instanceof Scopes.NoControl)
                throw new IllegalArgumentException("empty closed control is not implicit divergence");
        }
    }

    public sealed interface OutcomeKey permits NormalOutcome, ExceptionOutcome,
            OtherExceptionOutcome, HaltOutcome, DivergeOutcome {}
    public enum NormalOutcome implements OutcomeKey { INSTANCE }
    public enum OtherExceptionOutcome implements OutcomeKey { INSTANCE }
    public enum HaltOutcome implements OutcomeKey { INSTANCE }
    public enum DivergeOutcome implements OutcomeKey { INSTANCE }
    public record ExceptionOutcome(String tag) implements OutcomeKey {
        public ExceptionOutcome { tag = text(tag, "tag"); }
    }

    public sealed interface ProgramPoint permits Before, After, EntryPoint, ExitPoint {}
    public record Before(OperationId operation) implements ProgramPoint {
        public Before { operation = Objects.requireNonNull(operation, "operation"); }
    }
    public record After(OperationId operation, OutcomeKey outcome) implements ProgramPoint {
        public After {
            operation = Objects.requireNonNull(operation, "operation");
            outcome = Objects.requireNonNull(outcome, "outcome");
        }
    }
    public record EntryPoint(EntryId entry) implements ProgramPoint {
        public EntryPoint { entry = Objects.requireNonNull(entry, "entry"); }
    }
    public record ExitPoint(UnitId unit, OutcomeKey outcome) implements ProgramPoint {
        public ExitPoint {
            unit = Objects.requireNonNull(unit, "unit");
            outcome = Objects.requireNonNull(outcome, "outcome");
        }
    }
}
