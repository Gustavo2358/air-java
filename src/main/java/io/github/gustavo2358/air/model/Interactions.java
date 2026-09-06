package io.github.gustavo2358.air.model;

import java.util.*;
import java.math.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Typed AIR 2.0 values. Collections are defensively copied; no transport dependencies. */
public final class Interactions {
    private Interactions() {}
    public enum PassingMode { VALUE, REFERENCE, COPY }
    public sealed interface NamePolicy permits ExactName, ContractName, UnknownName {}
    public enum ExactName implements NamePolicy { INSTANCE }
    public record ContractName(ContractId contract) implements NamePolicy {
        public ContractName {
            contract = Objects.requireNonNull(contract, "contract");
            
        }

    }
    public record UnknownName(UncertaintyId uncertainty) implements NamePolicy {
        public UnknownName {
            uncertainty = Objects.requireNonNull(uncertainty, "uncertainty");
            
        }

    }
    public sealed interface Target permits InternalTarget, LiteralTarget, ComputedTarget, ResourceTarget {}
    public record InternalTarget(EntryId entry) implements Target {
        public InternalTarget {
            entry = Objects.requireNonNull(entry, "entry");
            
        }

    }
    public record LiteralTarget(String category, String namespace, String name, NamePolicy namePolicy, OriginId origin) implements Target {
        public LiteralTarget {
            category = text(category, "category");
            namespace = text(namespace, "namespace");
            name = unicode(name, "name");
            namePolicy = Objects.requireNonNull(namePolicy, "namePolicy");
            origin = Objects.requireNonNull(origin, "origin");
            
        }

    }
    public record ComputedTarget(String category, String namespace, Expression name, NamePolicy namePolicy, OriginId origin) implements Target {
        public ComputedTarget {
            category = text(category, "category");
            namespace = text(namespace, "namespace");
            name = Objects.requireNonNull(name, "name");
            namePolicy = Objects.requireNonNull(namePolicy, "namePolicy");
            origin = Objects.requireNonNull(origin, "origin");
            
        }

    }
    public record ResourceTarget(ResourceId resource) implements Target {
        public ResourceTarget {
            resource = Objects.requireNonNull(resource, "resource");
            
        }

    }
    public record Resource(ResourceId id, LiteralTarget description) {
        public Resource {
            id = Objects.requireNonNull(id, "id");
            description = Objects.requireNonNull(description, "description");
            
        }

    }
    public sealed interface Argument permits ValueArgument, CopyArgument, ReferenceArgument {}
    public record ValueArgument(Expression value) implements Argument {
        public ValueArgument {
            value = Objects.requireNonNull(value, "value");
            
        }

    }
    public record CopyArgument(Expression value) implements Argument {
        public CopyArgument {
            value = Objects.requireNonNull(value, "value");
            
        }

    }
    public record ReferenceArgument(Place place) implements Argument {
        public ReferenceArgument {
            place = Objects.requireNonNull(place, "place");
            
        }

    }
    public record Parameter(int position, PassingMode mode, Types.TypeRef typeRef, Optional<ObjectId> object, OriginId origin) {
        public Parameter {
            nonNegative(position, "position");
            mode = Objects.requireNonNull(mode, "mode");
            typeRef = Objects.requireNonNull(typeRef, "typeRef");
            object = Objects.requireNonNull(object, "object");
            origin = Objects.requireNonNull(origin, "origin");
            
        }

    }
    public record ResultSlot(int position, Types.TypeRef typeRef, OriginId origin) {
        public ResultSlot {
            nonNegative(position, "position");
            typeRef = Objects.requireNonNull(typeRef, "typeRef");
            origin = Objects.requireNonNull(origin, "origin");
            
        }

    }
    public record Signature(List<Parameter> parameters, List<ResultSlot> results, Optional<UncertaintyId> incomplete) {
        public Signature {
            parameters = List.copyOf(parameters);
            results = List.copyOf(results);
            incomplete = Objects.requireNonNull(incomplete, "incomplete");
            
        }

    }
    public record ForeignEffects(Scopes.MemoryBound reads, Scopes.MemoryBound writes, List<OperandId> mustOverwrite) {
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
    public record Contract(ContractId id, String authority, SemanticVersion version, Signature signature, Optional<EffectBound> effects, List<PremiseId> premises, List<UncertaintyId> uncertainties, OriginId origin) {
        public Contract {
            id = Objects.requireNonNull(id, "id");
            authority = text(authority, "authority");
            version = Objects.requireNonNull(version, "version");
            signature = Objects.requireNonNull(signature, "signature");
            effects = Objects.requireNonNull(effects, "effects");
            premises = List.copyOf(premises);
            uncertainties = List.copyOf(uncertainties);
            origin = Objects.requireNonNull(origin, "origin");
            
        }

    }
    public record ContractKnowledge(Optional<ContractId> contract, Optional<UncertaintyId> unknown) {
        public ContractKnowledge {
            contract = Objects.requireNonNull(contract, "contract");
            unknown = Objects.requireNonNull(unknown, "unknown");
            if (contract.isPresent()==unknown.isPresent()) throw new IllegalArgumentException("contract known xor unknown");
        }

    }
    public sealed interface SignatureTarget permits EntrySignature, ExternalSignature {}
    public record EntrySignature(EntryId entry) implements SignatureTarget {
        public EntrySignature {
            entry = Objects.requireNonNull(entry, "entry");
            
        }

    }
    public record ExternalSignature(ContractId contract) implements SignatureTarget {
        public ExternalSignature {
            contract = Objects.requireNonNull(contract, "contract");
            
        }

    }

}
