package io.github.gustavo2358.air.model;

import java.math.BigInteger;
import java.util.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Interaction facts materialized in each AIR invoke; no lazy contract or resource lookup. */
public final class Interactions {
    private Interactions() {}

    public enum PassingMode { VALUE, REFERENCE, COPY }

    public sealed interface NamePolicy permits ExactName, ExtensionName, UnknownName {}
    public enum ExactName implements NamePolicy { INSTANCE }
    public record ExtensionName(String name, String version) implements NamePolicy {
        public ExtensionName {
            name = text(name, "name");
            version = text(version, "version");
        }
    }
    public record UnknownName(UncertaintyId uncertainty) implements NamePolicy {
        public UnknownName {
            uncertainty = Objects.requireNonNull(uncertainty, "uncertainty");
        }
    }

    public sealed interface Target permits InternalTarget, LiteralTarget, ComputedTarget {}
    public sealed interface ResourceDescription permits InternalTarget, LiteralTarget, ComputedResource {}

    public record InternalTarget(EntryId entry) implements Target, ResourceDescription {
        public InternalTarget {
            entry = Objects.requireNonNull(entry, "entry");
        }
    }
    public record LiteralTarget(String category, String namespace, String name,
                                NamePolicy namePolicy, OriginId origin)
            implements Target, ResourceDescription {
        public LiteralTarget {
            category = text(category, "category");
            namespace = text(namespace, "namespace");
            name = unicode(name, "name");
            namePolicy = Objects.requireNonNull(namePolicy, "namePolicy");
            origin = Objects.requireNonNull(origin, "origin");
        }
    }
    public record ComputedTarget(String category, String namespace, Expression name,
                                 NamePolicy namePolicy, OriginId origin) implements Target {
        public ComputedTarget {
            category = text(category, "category");
            namespace = text(namespace, "namespace");
            name = Objects.requireNonNull(name, "name");
            namePolicy = Objects.requireNonNull(namePolicy, "namePolicy");
            origin = Objects.requireNonNull(origin, "origin");
        }
    }
    public record ComputedResource(String category, String namespace, OperandId name,
                                   NamePolicy namePolicy, OriginId origin) implements ResourceDescription {
        public ComputedResource {
            category = text(category, "category");
            namespace = text(namespace, "namespace");
            name = Objects.requireNonNull(name, "name");
            namePolicy = Objects.requireNonNull(namePolicy, "namePolicy");
            origin = Objects.requireNonNull(origin, "origin");
        }
    }
    public record Resource(ResourceId id, ResourceDescription description, OriginId origin) {
        public Resource {
            id = Objects.requireNonNull(id, "id");
            description = Objects.requireNonNull(description, "description");
            origin = Objects.requireNonNull(origin, "origin");
        }
    }

    public sealed interface Argument permits ValueArgument, CopyArgument, ReferenceArgument {}
    public record ValueArgument(Expression value) implements Argument {
        public ValueArgument { value = Objects.requireNonNull(value, "value"); }
    }
    public record CopyArgument(Expression value) implements Argument {
        public CopyArgument { value = Objects.requireNonNull(value, "value"); }
    }
    public record ReferenceArgument(Place place) implements Argument {
        public ReferenceArgument { place = Objects.requireNonNull(place, "place"); }
    }

    public sealed interface UnknownBound permits NoRemainder, UnknownRemainder {}
    public enum NoRemainder implements UnknownBound { INSTANCE }
    public record UnknownRemainder(UncertaintyId uncertainty) implements UnknownBound {
        public UnknownRemainder {
            uncertainty = Objects.requireNonNull(uncertainty, "uncertainty");
        }
    }

    public sealed interface ModeKnowledge permits KnownMode, UnknownMode {}
    public record KnownMode(PassingMode mode) implements ModeKnowledge {
        public KnownMode { mode = Objects.requireNonNull(mode, "mode"); }
    }
    public record UnknownMode(UncertaintyId uncertainty) implements ModeKnowledge {
        public UnknownMode {
            uncertainty = Objects.requireNonNull(uncertainty, "uncertainty");
        }
    }

    public sealed interface ParameterBinding permits ObjectBinding, UnknownParameterBinding, ExternalBinding {}
    public record ObjectBinding(ObjectId object) implements ParameterBinding {
        public ObjectBinding { object = Objects.requireNonNull(object, "object"); }
    }
    public record UnknownParameterBinding(UncertaintyId uncertainty) implements ParameterBinding {
        public UnknownParameterBinding {
            uncertainty = Objects.requireNonNull(uncertainty, "uncertainty");
        }
    }
    public enum ExternalBinding implements ParameterBinding { INSTANCE }

    public record Parameter(BigInteger position, ModeKnowledge mode, Types.TypeRef typeRef,
                            ParameterBinding objectBinding, OriginId origin) {
        public Parameter {
            position = Objects.requireNonNull(position, "position");
            nonNegative(position, "position");
            mode = Objects.requireNonNull(mode, "mode");
            typeRef = Objects.requireNonNull(typeRef, "typeRef");
            objectBinding = Objects.requireNonNull(objectBinding, "objectBinding");
            origin = Objects.requireNonNull(origin, "origin");
        }
    }
    public record ResultSlot(BigInteger position, Types.TypeRef typeRef, OriginId origin) {
        public ResultSlot {
            position = Objects.requireNonNull(position, "position");
            nonNegative(position, "position");
            typeRef = Objects.requireNonNull(typeRef, "typeRef");
            origin = Objects.requireNonNull(origin, "origin");
        }
    }
    public record ParameterInventory(List<Parameter> known, UnknownBound remainder) {
        public ParameterInventory {
            known = List.copyOf(known);
            remainder = Objects.requireNonNull(remainder, "remainder");
        }
    }
    public record ResultInventory(List<ResultSlot> known, UnknownBound remainder) {
        public ResultInventory {
            known = List.copyOf(known);
            remainder = Objects.requireNonNull(remainder, "remainder");
        }
    }
    public record Signature(ParameterInventory parameters, ResultInventory results, OriginId origin) {
        public Signature {
            parameters = Objects.requireNonNull(parameters, "parameters");
            results = Objects.requireNonNull(results, "results");
            origin = Objects.requireNonNull(origin, "origin");
        }
    }

    public record ForeignEffects(Scopes.MemoryBound reads, Scopes.MemoryBound writes,
                                 List<OperandId> mustOverwrite) {
        public ForeignEffects {
            reads = Objects.requireNonNull(reads, "reads");
            writes = Objects.requireNonNull(writes, "writes");
            mustOverwrite = List.copyOf(mustOverwrite);
        }
    }
    public record OutcomeEffects(Control.OutcomeKey outcome, ForeignEffects effects) {
        public OutcomeEffects {
            outcome = Objects.requireNonNull(outcome, "outcome");
            effects = Objects.requireNonNull(effects, "effects");
        }
    }
    public record EffectBound(ForeignEffects otherwise, List<OutcomeEffects> perOutcome) {
        public EffectBound {
            otherwise = Objects.requireNonNull(otherwise, "otherwise");
            perOutcome = List.copyOf(perOutcome);
        }
    }

    public record ContractRef(String authority, String version, List<OriginId> evidence) {
        public ContractRef {
            authority = text(authority, "authority");
            version = text(version, "version");
            evidence = List.copyOf(evidence);
            if(evidence.isEmpty()) throw new IllegalArgumentException("contract evidence must not be empty");
        }
    }
    public sealed interface ContractKnowledge permits KnownContract, UnknownContract {}
    public record KnownContract(ContractRef reference) implements ContractKnowledge {
        public KnownContract { reference = Objects.requireNonNull(reference, "reference"); }
    }
    public record UnknownContract(UncertaintyId uncertainty) implements ContractKnowledge {
        public UnknownContract {
            uncertainty = Objects.requireNonNull(uncertainty, "uncertainty");
        }
    }

    public sealed interface InvocationSignature permits EntrySignature, ExternalSignature {}
    public record EntrySignature(EntryId entry) implements InvocationSignature {
        public EntrySignature { entry = Objects.requireNonNull(entry, "entry"); }
    }
    public record ExternalSignature(Signature signature) implements InvocationSignature {
        public ExternalSignature { signature = Objects.requireNonNull(signature, "signature"); }
    }
}
