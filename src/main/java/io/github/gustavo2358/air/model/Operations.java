package io.github.gustavo2358.air.model;

import java.util.*;
import java.math.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Typed AIR 2.0 values. Collections are defensively copied; no transport dependencies. */
public final class Operations {
    private Operations() {}
    public record Header(OperationId id, OriginId origin, Evidence.CoverageStatus coverage, Evidence.Precision precision, List<UncertaintyId> uncertainties) {
        public Header {
            id = Objects.requireNonNull(id, "id");
            origin = Objects.requireNonNull(origin, "origin");
            coverage = Objects.requireNonNull(coverage, "coverage");
            precision = Objects.requireNonNull(precision, "precision");
            uncertainties = List.copyOf(uncertainties);
            
        }

    }
    public record Assign(Header header, Place destination, Expression value) implements Instruction {
        public Assign {
            header = Objects.requireNonNull(header, "header");
            destination = Objects.requireNonNull(destination, "destination");
            value = Objects.requireNonNull(value, "value");
            
        }
        @Override public String kind() { return "assign"; }
    }
    public record HavocMust(Header header, Place destination, UncertaintyId reason) implements Instruction {
        public HavocMust {
            header = Objects.requireNonNull(header, "header");
            destination = Objects.requireNonNull(destination, "destination");
            reason = Objects.requireNonNull(reason, "reason");
            
        }
        @Override public String kind() { return "havoc.must"; }
    }
    public record HavocMay(Header header, Scopes.MemoryScope scope, UncertaintyId reason) implements Instruction {
        public HavocMay {
            header = Objects.requireNonNull(header, "header");
            scope = Objects.requireNonNull(scope, "scope");
            reason = Objects.requireNonNull(reason, "reason");
            
        }
        @Override public String kind() { return "havoc.may"; }
    }
    public record Nop(Header header) implements Instruction {
        public Nop {
            header = Objects.requireNonNull(header, "header");
            
        }
        @Override public String kind() { return "nop"; }
    }
    public record CopyBytes(Header header, Memory.ByteRange destination, Memory.ByteRange source, BigInteger length, Envelopes.Envelope fallback) implements Instruction {
        public CopyBytes {
            header = Objects.requireNonNull(header, "header");
            destination = Objects.requireNonNull(destination, "destination");
            source = Objects.requireNonNull(source, "source");
            length = Objects.requireNonNull(length, "length");
            fallback = Objects.requireNonNull(fallback, "fallback");
            nonNegative(length,"length");
        }
        @Override public String kind() { return "copy_bytes"; }
    }
    public record Jump(Header header, LabelId destination) implements Terminator {
        public Jump {
            header = Objects.requireNonNull(header, "header");
            destination = Objects.requireNonNull(destination, "destination");
            
        }
        @Override public String kind() { return "jump"; }
    }
    public record Branch(Header header, Expression predicate, LabelId trueDestination, LabelId falseDestination) implements Terminator {
        public Branch {
            header = Objects.requireNonNull(header, "header");
            predicate = Objects.requireNonNull(predicate, "predicate");
            trueDestination = Objects.requireNonNull(trueDestination, "trueDestination");
            falseDestination = Objects.requireNonNull(falseDestination, "falseDestination");
            
        }
        @Override public String kind() { return "branch"; }
    }
    public record Case(Values.LiteralValue value, LabelId destination) {
        public Case {
            value = Objects.requireNonNull(value, "value");
            destination = Objects.requireNonNull(destination, "destination");
            
        }

    }
    public record Dispatch(Header header, Expression selector, List<Case> cases, LabelId defaultDestination) implements Terminator {
        public Dispatch {
            header = Objects.requireNonNull(header, "header");
            selector = Objects.requireNonNull(selector, "selector");
            cases = List.copyOf(cases);
            defaultDestination = Objects.requireNonNull(defaultDestination, "defaultDestination");
            
        }
        @Override public String kind() { return "dispatch"; }
    }
    public record Invoke(Header header, String action, Interactions.Target target,
                         List<Interactions.Argument> arguments, List<Place> results,
                         Interactions.InvocationSignature signature, List<Place> effectOperands,
                         Interactions.EffectBound effectBound, Control.InvocationOutcomes outcomes,
                         Interactions.ContractKnowledge contract) implements Terminator {
        public Invoke {
            header = Objects.requireNonNull(header, "header");
            action = text(action, "action");
            target = Objects.requireNonNull(target, "target");
            arguments = List.copyOf(arguments);
            results = List.copyOf(results);
            signature = Objects.requireNonNull(signature, "signature");
            effectOperands = List.copyOf(effectOperands);
            effectBound = Objects.requireNonNull(effectBound, "effectBound");
            outcomes = Objects.requireNonNull(outcomes, "outcomes");
            contract = Objects.requireNonNull(contract, "contract");
            
        }
        @Override public String kind() { return "invoke"; }
    }
    public record Return(Header header, List<Expression> values) implements Terminator {
        public Return {
            header = Objects.requireNonNull(header, "header");
            values = List.copyOf(values);
            
        }
        @Override public String kind() { return "return"; }
    }
    public record Raise(Header header, String tag, List<Expression> values) implements Terminator {
        public Raise {
            header = Objects.requireNonNull(header, "header");
            tag = text(tag, "tag");
            values = List.copyOf(values);
            
        }
        @Override public String kind() { return "raise"; }
    }
    public enum HaltKind { NORMAL, ABNORMAL }
    public record Halt(Header header, HaltKind haltKind) implements Terminator {
        public Halt {
            header = Objects.requireNonNull(header, "header");
            haltKind = Objects.requireNonNull(haltKind, "haltKind");
            
        }
        @Override public String kind() { return "halt"; }
    }
    public record Opaque(Header header, String observedKind, List<Operand> knownOperands,
                         List<OperandId> valueResults, Envelopes.Envelope envelope) implements Terminator {
        public Opaque {
            header = Objects.requireNonNull(header, "header");
            observedKind = text(observedKind, "observedKind");
            knownOperands = List.copyOf(knownOperands);
            valueResults = List.copyOf(valueResults);
            envelope = Objects.requireNonNull(envelope, "envelope");
            
        }
        @Override public String kind() { return "opaque"; }
    }
    public record LocalInvoke(Header header, LabelId entry, List<CompletionPortId> completionPorts, LabelId resume, Envelopes.Envelope fallback) implements Terminator {
        public LocalInvoke {
            header = Objects.requireNonNull(header, "header");
            entry = Objects.requireNonNull(entry, "entry");
            completionPorts = List.copyOf(completionPorts);
            resume = Objects.requireNonNull(resume, "resume");
            fallback = Objects.requireNonNull(fallback, "fallback");
            
        }
        @Override public String kind() { return "local.invoke"; }
    }
    public record LocalBoundary(Header header, CompletionPortId port, LabelId defaultDestination, Envelopes.Envelope fallback) implements Terminator {
        public LocalBoundary {
            header = Objects.requireNonNull(header, "header");
            port = Objects.requireNonNull(port, "port");
            defaultDestination = Objects.requireNonNull(defaultDestination, "defaultDestination");
            fallback = Objects.requireNonNull(fallback, "fallback");
            
        }
        @Override public String kind() { return "local.boundary"; }
    }
    public record LocalResume(Header header, Envelopes.Envelope fallback) implements Terminator {
        public LocalResume {
            header = Objects.requireNonNull(header, "header");
            fallback = Objects.requireNonNull(fallback, "fallback");
            
        }
        @Override public String kind() { return "local.resume"; }
    }
    public record LocalUnwind(Header header, BigInteger count, LabelId destination, Envelopes.Envelope fallback) implements Terminator {
        public LocalUnwind {
            header = Objects.requireNonNull(header, "header");
            count = Objects.requireNonNull(count, "count");
            nonNegative(count, "count");
            destination = Objects.requireNonNull(destination, "destination");
            fallback = Objects.requireNonNull(fallback, "fallback");
            
        }
        @Override public String kind() { return "local.unwind"; }
    }
    public record IndirectJump(Header header, Expression target, Types.LabelType within, Envelopes.Envelope fallback) implements Terminator {
        public IndirectJump {
            header = Objects.requireNonNull(header, "header");
            target = Objects.requireNonNull(target, "target");
            within = Objects.requireNonNull(within, "within");
            fallback = Objects.requireNonNull(fallback, "fallback");
            
        }
        @Override public String kind() { return "indirect.jump"; }
    }

}
