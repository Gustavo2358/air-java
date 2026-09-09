package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.math.BigInteger;
import java.util.*;

/** Typed referential integrity, metadata, ownership and static cycles. */
final class ReferenceChecks {
    private enum SignatureContext { ENTRY, EXTERNAL }

    final ValidationContext c;
    final Map<UnitId,Set<ObjectId>> visible=new HashMap<>();
    private final Map<Control.InvocationOutcomes,Set<Control.OutcomeKey>> outcomeKeys=new IdentityHashMap<>();

    ReferenceChecks(ValidationContext c) { this.c=c; }

    void run() {
        Publication publication=c.index.publication;
        checkCapabilities(publication.capabilities());
        for(Unit unit:publication.units()) {
            Set<ObjectId> objects=new HashSet<>(unit.visibleObjects());
            for(Memory.ObjectDeclaration object:unit.objects()) objects.add(object.id());
            visible.put(unit.id(),objects);
        }

        for(Origins.Origin origin:publication.origins()) switch(origin) {
            case Origins.Written written -> {
                c.ref(written.artifact(),written.id());
                for(Origins.IncludeFrame frame:written.includes()) {
                    c.ref(frame.including(),written.id()); c.ref(frame.included(),written.id());
                }
            }
            case Origins.Derived derived -> c.refs(derived.inputs(),derived.id());
            case Origins.Contractual ignored -> { }
            case Origins.Unavailable ignored -> { }
        }
        cycleOrigins(); cycleUnits(); cycleAliases();

        for(Evidence.Uncertainty uncertainty:publication.uncertainties()) {
            c.ref(uncertainty.origin(),uncertainty.id());
            scope(uncertainty.scope(),uncertainty.id());
        }
        for(Proofs.Premise premise:publication.premises()) {
            c.ref(premise.origin(),premise.id());
            if(premise.assertion() instanceof Proofs.DisjointStorage disjoint) {
                c.refs(disjoint.storage(),premise.id());
                if(disjoint.storage().size()<2
                        || new HashSet<>(disjoint.storage()).size()!=disjoint.storage().size())
                    c.error("I-58",premise.id(),
                            "disjoint_storage requires at least two distinct storage bases");
                c.obligation("I-59",premise.id(),
                        "authority must substantiate pairwise physical separation for the whole publication");
            }
        }

        for(Memory.Storage storage:publication.storage()) {
            Memory.StorageHeader header=storage.header();
            c.ref(header.origin(),header.id());
            header.owner().ifPresent(owner -> c.ref(owner,header.id()));
            if(storage instanceof Memory.Cell cell) c.type(cell.typeRef(),header.id());
            if(storage instanceof Memory.Region region) {
                c.capability(Capabilities.MEMORY_REGIONS,header.id());
                region.extentUnknown().ifPresent(id -> c.uncertainty(id,null,header.id()));
            }
        }

        for(Interactions.Resource resource:publication.resources()) {
            c.ref(resource.origin(),resource.id());
            resource(resource.description(),resource.id(),null);
        }
        for(Artifacts.Relation relation:publication.artifactRelations()) {
            c.ref(relation.source(),relation.id()); c.ref(relation.origin(),relation.id());
            switch(relation.destination()) {
                case Artifacts.InternalArtifact artifact -> c.ref(artifact.artifact(),relation.id());
                case Artifacts.ExternalArtifact external -> target(external.resource(),relation.id());
            }
        }
        coverage(publication.coverage(),publication.id());

        for(Unit unit:publication.units()) {
            c.ref(unit.origin(),unit.id());
            unit.containingUnit().ifPresent(id -> c.ref(id,unit.id()));
            c.refs(unit.visibleObjects(),unit.id());
            unit.bodyUnavailable().ifPresent(id -> c.uncertainty(id,null,unit.id()));
            coverage(unit.coverage(),unit.id());

            for(Memory.ObjectDeclaration object:unit.objects()) {
                if(!object.id().unit().equals(unit.id()))
                    c.error("I-01",object.id(),"object belongs to a different unit");
                c.type(object.typeRef(),object.id()); c.ref(object.origin(),object.id());
                binding(object.storage(),object.typeRef(),object.id(),0);
                precision(object.precision(),object.id());
            }
            for(Entries.CompletionPort port:unit.completionPorts()) {
                c.capability(Capabilities.LOCAL_CONTROL,port.id());
                c.ref(port.origin(),port.id());
                if(!port.id().unit().equals(unit.id()))
                    c.error("I-01",port.id(),"completion port owner differs from unit");
            }
            for(Entries.Entry entry:unit.entries()) {
                if(!entry.id().unit().equals(unit.id()))
                    c.error("I-05",entry.id(),"entry owner differs from unit");
                c.ref(entry.origin(),entry.id());
                signature(entry.signature(),entry.id(),unit.id(),SignatureContext.ENTRY);
                if(unit.body()==Unit.BodyAvailability.AVAILABLE && entry.initialLabel().isEmpty())
                    c.error("I-05",entry.id(),"available entry requires initial label");
                entry.initialLabel().ifPresent(id -> label(id,unit.id(),entry.id()));
                c.refs(entry.state().uncertainties(),entry.id());
                Set<BigInteger> parameters=new HashSet<>();
                for(Interactions.Parameter parameter:entry.signature().parameters().known())
                    parameters.add(parameter.position());
                for(Entries.InitialCondition seed:entry.state().conditions()) {
                    c.ref(seed.origin(),entry.id()); c.refs(seed.premises(),entry.id());
                    if(seed.value() instanceof Entries.ParameterInitial parameter
                            && !parameters.contains(parameter.position()))
                        c.error("I-02",entry.id(),"entry state references a parameter position not materialized in the signature");
                    if(seed.value() instanceof Entries.ExternalUnknown external)
                        c.uncertainty(external.reason(),null,entry.id());
                    if(seed.value() instanceof Entries.Uninitialized uninitialized)
                        c.uncertainty(uninitialized.reason(),null,entry.id());
                }
            }
            for(Sequence sequence:unit.sequences()) {
                c.ref(sequence.origin(),sequence.label());
                if(!sequence.label().unit().equals(unit.id()))
                    c.error("I-03",sequence.label(),"sequence owner differs from unit");
                for(Operation operation:sequence.operations()) {
                    c.ref(operation.header().origin(),operation.header().id());
                    c.refs(operation.header().uncertainties(),operation.header().id());
                    precision(operation.header().precision(),operation.header().id());
                }
            }
        }
        for(Operand operand:c.index.operands.values()) operand(operand);
    }

    void operand(Operand operand) {
        OperandId id=operand.header().id(); c.ref(operand.header().origin(),id);
        if(operand instanceof Places.ObjectPlace place) {
            c.ref(place.object(),id);
            if(!visible.getOrDefault(id.owner().unit(),Set.of()).contains(place.object()))
                c.error("I-02",id,"object not explicitly visible in operand unit");
        } else if(operand instanceof Places.Choice choice) {
            c.type(choice.typeRef(),id); memoryBound(choice.remainder(),id,0);
        } else if(operand instanceof Places.RegionSlice slice) {
            c.ref(slice.region(),id); c.capability(Capabilities.MEMORY_REGIONS,id);
            if(!(c.index.storage.get(slice.region()) instanceof Memory.Region))
                c.error("I-13",id,"region_slice must reference a region");
            c.type(slice.typeRef(),id); codec(slice.codec(),slice.typeRef(),id);
        } else if(operand instanceof Expressions.Unknown unknown) {
            c.type(unknown.typeRef(),id); c.uncertainty(unknown.reason(),null,id);
            memoryBound(unknown.remainingReads(),id,0);
            if(unknown.typeRef() instanceof Types.UnknownType type
                    && type.uncertainty().equals(unknown.reason()))
                c.error("I-50",id,"type uncertainty and value uncertainty need distinct identities");
            c.obligation("I-09",id,
                    "producer must substantiate that unknown expression dependencies and remaining reads are pure");
        }
    }

    void checkCapabilities(Capabilities.Manifest manifest) {
        for(List<Capabilities.Capability> list:List.of(manifest.required(),manifest.provided())) {
            Set<String> names=new HashSet<>();
            for(Capabilities.Capability capability:list) {
                if(!names.add(capability.name()))
                    c.error("I-43",c.index.publication.id(),
                            "duplicate capability name: "+capability.name());
                boolean standard=List.of(Capabilities.MEMORY_REGIONS,Capabilities.LOCAL_CONTROL,
                        Capabilities.INDIRECT_CONTROL).contains(capability);
                boolean profile=capability.name().startsWith("AIR-");
                if(profile) c.obligation("profile",c.index.publication.id(),
                        "declared profile requires separate oracle evidence: "+capability);
                else if(!standard && c.required.contains(capability))
                    c.unsupported("I-43",c.index.publication.id(),
                            "extension semantic contract is not implemented by this validator: "+capability);
            }
        }
    }

    void signature(Interactions.Signature signature,Id owner,UnitId unit,
                   SignatureContext context) {
        c.ref(signature.origin(),owner);
        Interactions.ParameterInventory parameters=signature.parameters();
        Interactions.ResultInventory results=signature.results();
        checkPositions(parameters.known().stream().map(Interactions.Parameter::position).toList(),
                parameters.remainder() instanceof Interactions.NoRemainder,owner);
        checkPositions(results.known().stream().map(Interactions.ResultSlot::position).toList(),
                results.remainder() instanceof Interactions.NoRemainder,owner);
        remainder(parameters.remainder(),owner); remainder(results.remainder(),owner);

        for(Interactions.Parameter parameter:parameters.known()) {
            c.type(parameter.typeRef(),owner); c.ref(parameter.origin(),owner);
            if(parameter.mode() instanceof Interactions.UnknownMode unknown)
                c.uncertainty(unknown.uncertainty(),null,owner);
            switch(parameter.objectBinding()) {
                case Interactions.ObjectBinding object -> {
                    c.ref(object.object(),owner);
                    if(context==SignatureContext.EXTERNAL)
                        c.error("I-55",owner,"external signature cannot bind an object in the called unit");
                    else if(unit!=null && !object.object().unit().equals(unit))
                        c.error("I-08",owner,"parameter initialization object is not owned by entry unit");
                }
                case Interactions.UnknownParameterBinding unknown -> {
                    c.uncertainty(unknown.uncertainty(),null,owner);
                    if(context==SignatureContext.EXTERNAL)
                        c.error("I-55",owner,"external signature binding is not applicable, not unknown");
                }
                case Interactions.ExternalBinding ignored -> {
                    if(context==SignatureContext.ENTRY)
                        c.error("I-55",owner,"entry parameter must materialize or explicitly lack its object binding");
                }
            }
        }
        for(Interactions.ResultSlot result:results.known()) {
            c.type(result.typeRef(),owner); c.ref(result.origin(),owner);
        }
    }

    private void remainder(Interactions.UnknownBound bound,Id owner) {
        if(bound instanceof Interactions.UnknownRemainder unknown)
            c.uncertainty(unknown.uncertainty(),null,owner);
    }

    private void checkPositions(List<BigInteger> positions,boolean complete,Id owner) {
        BigInteger previous=null;
        BigInteger expected=BigInteger.ZERO;
        for(BigInteger position:positions) {
            if(previous!=null && position.compareTo(previous)<=0
                    || complete && !position.equals(expected))
                c.error("I-08",owner,
                        "signature positions must be unique/ordered and contiguous when closed");
            previous=position; expected=expected.add(BigInteger.ONE);
        }
    }

    void label(LabelId label,UnitId unit,Id owner) {
        c.ref(label,owner);
        if(!label.unit().equals(unit)) c.error("I-02",owner,"local control target crosses unit");
    }

    void target(Interactions.Target target,Id owner) {
        switch(target) {
            case Interactions.InternalTarget internal -> c.ref(internal.entry(),owner);
            case Interactions.LiteralTarget literal -> {
                c.ref(literal.origin(),owner); namePolicy(literal.namePolicy(),owner);
            }
            case Interactions.ComputedTarget computed -> {
                c.ref(computed.origin(),owner); namePolicy(computed.namePolicy(),owner);
            }
        }
    }

    private void resource(Interactions.ResourceDescription description,Id owner,
                          OperationId operation) {
        switch(description) {
            case Interactions.InternalTarget internal -> c.ref(internal.entry(),owner);
            case Interactions.LiteralTarget literal -> {
                c.ref(literal.origin(),owner); namePolicy(literal.namePolicy(),owner);
            }
            case Interactions.ComputedResource computed -> {
                c.ref(computed.origin(),owner); c.ref(computed.name(),owner);
                namePolicy(computed.namePolicy(),owner);
                Operand operand=c.index.operands.get(computed.name());
                if(!(computed.name().owner() instanceof OperationOwner operandOwner))
                    c.error("I-11",owner,"computed resource name must be an operation-owned occurrence");
                else if(operation!=null && !operandOwner.operation().equals(operation))
                    c.error("I-11",owner,"computed resource refers to an operand from another operation");
                if(operand!=null && operand.header().role()!=Operand.Role.RESOURCE_TARGET)
                    c.error("I-11",owner,"computed resource operand must have RESOURCE_TARGET role");
            }
        }
    }

    void namePolicy(Interactions.NamePolicy policy,Id owner) {
        if(policy instanceof Interactions.ExtensionName extension)
            c.capability(new Capabilities.Capability(extension.name(),extension.version()),owner);
        if(policy instanceof Interactions.UnknownName unknown)
            c.uncertainty(unknown.uncertainty(),null,owner);
    }

    void scope(Scopes.FactScope scope,Id owner) {
        switch(scope) {
            case Scopes.PublicationScope publication -> c.ref(publication.publication(),owner);
            case Scopes.UnitScope unit -> c.ref(unit.unit(),owner);
            case Scopes.EntityScope entities -> c.refs(entities.entities(),owner);
        }
    }

    void precision(Evidence.Precision precision,Id owner) {
        for(Evidence.Dimension dimension:Evidence.Dimension.values()) {
            Evidence.Claim claim=precision.claim(dimension);
            scope(claim.scope(),owner); c.refs(claim.reasons(),owner);
            if((claim.status()==Evidence.PrecisionStatus.OPEN
                    || claim.status()==Evidence.PrecisionStatus.UNAVAILABLE)
                    && claim.reasons().isEmpty())
                c.error("I-32",owner,"open/unavailable precision requires reasons for "+dimension);
        }
    }

    void coverage(Evidence.Coverage coverage,Id owner) {
        scope(coverage.scope(),owner); c.refs(coverage.uncertainties(),owner);
        if(coverage.inventory()!=Evidence.InventoryStatus.COMPLETE && coverage.uncertainties().isEmpty())
            c.error("I-28",owner,"partial/unavailable inventory needs explicit reason");
        Set<String> keys=new HashSet<>();
        for(Evidence.CoverageItem item:coverage.items()) {
            if(!keys.add(item.sourceKey()))
                c.error("I-29",owner,"duplicate source inventory key: "+item.sourceKey());
            c.ref(item.origin(),owner); c.refs(item.outputs(),owner); c.refs(item.uncertainties(),owner);
            item.elimination().ifPresent(elimination -> c.ref(elimination.origin(),owner));
            if(item.outputs().isEmpty() && item.uncertainties().isEmpty()
                    && item.elimination().isEmpty())
                c.error("I-30",owner,
                        "coverage item disappeared without output, uncertainty or justified elimination");
            if(item.status()!=Evidence.CoverageStatus.MODELED && item.uncertainties().isEmpty())
                c.error("I-30",owner,"incomplete coverage item lacks uncertainty");
        }
    }

    void memoryBound(Scopes.MemoryBound bound,Id owner,long depth) {
        c.depth(depth);
        if(bound instanceof Scopes.WithinMemory within) memory(within.scope(),owner,depth+1);
    }

    void memory(Scopes.MemoryScope scope,Id owner,long depth) {
        Walk.run(scope,depth,node -> node instanceof Scopes.MemoryUnion union ? union.members() : List.of(),
                new Walk.Visitor<Scopes.MemoryScope>() {
            public boolean enter(Scopes.MemoryScope node,long nesting) {
                c.depth(nesting);
                switch(node) {
                    case Scopes.ObjectsMemory objects -> c.refs(objects.objects(),owner);
                    case Scopes.StorageMemory storage -> c.refs(storage.storage(),owner);
                    case Scopes.VisibleMemory visible -> c.ref(visible.unit(),owner);
                    case Scopes.AllMemory all -> c.ref(all.publication(),owner);
                    case Scopes.MemoryUnion ignored -> { }
                }
                return true;
            }
        });
    }

    void controlScope(Scopes.ControlScope scope,UnitId unit,Id owner,long depth) {
        Walk.run(scope,depth,node -> node instanceof Scopes.ControlUnion union ? union.members() : List.of(),
                new Walk.Visitor<Scopes.ControlScope>() {
            public boolean enter(Scopes.ControlScope node,long nesting) {
                c.depth(nesting);
                switch(node) {
                    case Scopes.LabelsControl labels -> {
                        for(LabelId label:labels.labels()) label(label,unit,owner);
                    }
                    case Scopes.UnitControl target -> c.ref(target.unit(),owner);
                    case Scopes.AllControl all -> c.ref(all.publication(),owner);
                    case Scopes.ControlUnion ignored -> { }
                }
                return true;
            }
        });
    }

    void outcomes(Control.InvocationOutcomes outcomes,UnitId unit,Id owner) {
        int normal=0; int catchAll=0; Set<String> tags=new HashSet<>();
        for(Control.InvocationAlternative alternative:outcomes.known()) {
            if(alternative instanceof Control.Normal && ++normal>1)
                c.error("I-60",owner,"invocation has more than one normal destination");
            if(alternative instanceof Control.Exceptional exceptional
                    && !tags.add(exceptional.tag()))
                c.error("I-60",owner,"duplicate invocation exception tag");
            if(alternative instanceof Control.AnyException && ++catchAll>1)
                c.error("I-60",owner,"duplicate invocation catch-all");
            controlAlternative(alternative,unit,owner,false);
        }
        if(outcomes.remainder() instanceof Scopes.WithinControl within)
            controlScope(within.scope(),unit,owner,0);
    }

    void control(Control.ControlEnvelope envelope,UnitId unit,Id owner,
                 boolean allowContinue) {
        for(Control.ControlAlternative alternative:envelope.known())
            controlAlternative(alternative,unit,owner,allowContinue);
        if(envelope.remainder() instanceof Scopes.WithinControl within)
            controlScope(within.scope(),unit,owner,0);
    }

    private void controlAlternative(Control.ControlAlternative alternative,UnitId unit,
                                    Id owner,boolean allowContinue) {
        switch(alternative) {
            case Control.Normal normal -> label(normal.label(),unit,owner);
            case Control.JumpAlternative jump -> label(jump.label(),unit,owner);
            case Control.Exceptional exceptional -> exception(exceptional.destination(),unit,owner);
            case Control.AnyException any -> exception(any.destination(),unit,owner);
            case Control.HaltAlternative ignored -> { }
            case Control.Diverge ignored -> { }
            case Control.ReturnAlternative ignored -> { }
            case Control.ContinueAlternative ignored -> {
                if(!allowContinue)
                    c.error("I-60",owner,"continue is allowed only in fallback of a common operation");
            }
        }
    }

    private void exception(Control.ExceptionDestination destination,UnitId unit,Id owner) {
        if(destination instanceof Control.Handler handler) label(handler.label(),unit,owner);
    }

    void effects(Interactions.EffectBound bound,Id owner,Operations.Invoke invoke) {
        foreign(bound.otherwise(),owner,invoke);
        Set<Control.OutcomeKey> keys=new HashSet<>();
        for(Interactions.OutcomeEffects outcome:bound.perOutcome()) {
            if(!keys.add(outcome.outcome()))
                c.error("I-60",owner,"duplicate outcome-specific effect key");
            foreign(outcome.effects(),owner,invoke);
            if(invoke!=null && !hasOutcome(invoke.outcomes(),outcome.outcome()))
                c.error("I-60",owner,"effect bound names an undeclared invocation outcome");
        }
    }

    private void foreign(Interactions.ForeignEffects effects,Id owner,Operations.Invoke invoke) {
        memoryBound(effects.reads(),owner,0); memoryBound(effects.writes(),owner,0);
        c.refs(effects.mustOverwrite(),owner);
        for(OperandId id:effects.mustOverwrite()) {
            if(!(c.index.operands.get(id) instanceof Place))
                c.error("I-23",owner,"must_overwrite must reference a place operand");
            if(invoke!=null && !id.owner().equals(new OperationOwner(invoke.header().id())))
                c.error("I-23",owner,"foreign effect references another site's operand");
        }
    }

    boolean hasOutcome(Control.InvocationOutcomes outcomes,Control.OutcomeKey key) {
        Set<Control.OutcomeKey> keys=outcomeKeys.computeIfAbsent(outcomes,value -> {
            Set<Control.OutcomeKey> result=new HashSet<>();
            for(Control.InvocationAlternative alternative:value.known()) {
                switch(alternative) {
                    case Control.Normal ignored -> result.add(Control.NormalOutcome.INSTANCE);
                    case Control.Exceptional tag -> result.add(new Control.ExceptionOutcome(tag.tag()));
                    case Control.AnyException ignored -> result.add(Control.OtherExceptionOutcome.INSTANCE);
                    case Control.HaltAlternative ignored -> result.add(Control.HaltOutcome.INSTANCE);
                    case Control.Diverge ignored -> result.add(Control.DivergeOutcome.INSTANCE);
                }
            }
            return result;
        });
        return keys.contains(key);
    }

    void envelope(Envelopes.Envelope envelope,OperationId owner,boolean allowContinue) {
        control(envelope.control(),owner.unit(),owner,allowContinue);
        memoryBound(envelope.memory().otherReads(),owner,0);
        memoryBound(envelope.memory().otherWrites(),owner,0);
        for(List<OperandId> ids:List.of(envelope.memory().knownReads(),
                envelope.memory().knownWrites(),envelope.memory().mustOverwrite())) {
            c.refs(ids,owner);
            for(OperandId id:ids) if(!id.owner().equals(new OperationOwner(owner)))
                c.error("I-11",owner,"envelope operand belongs to another site");
        }
        for(OperandId id:envelope.memory().knownWrites())
            if(!(c.index.operands.get(id) instanceof Place))
                c.error("I-23",owner,"known write does not identify a place");
        for(OperandId id:envelope.memory().mustOverwrite())
            if(!(c.index.operands.get(id) instanceof Place))
                c.error("I-23",owner,"must overwrite does not identify a place");
        for(Envelopes.ResourceUse use:envelope.dependencies().known()) {
            resource(use.target(),owner,owner); c.ref(use.origin(),owner); point(use.point(),owner);
            if(use.target() instanceof Interactions.ComputedResource
                    && (!(use.point() instanceof Control.Before before)
                    || !before.operation().equals(owner)))
                c.error("I-11",owner,
                        "computed resource use must observe its name before the owning operation");
        }
        c.obligation("I-26",owner,
                "fallback bounds must overapproximate source/extension semantics; shape validation cannot prove correspondence");
    }

    void point(Control.ProgramPoint point,Id owner) {
        switch(point) {
            case Control.Before before -> c.ref(before.operation(),owner);
            case Control.After after -> {
                c.ref(after.operation(),owner);
                if(c.index.operations.get(after.operation()) instanceof Operations.Invoke invoke
                        && !hasOutcome(invoke.outcomes(),after.outcome()))
                    c.error("I-02",owner,"point names undeclared invocation outcome");
            }
            case Control.EntryPoint entry -> c.ref(entry.entry(),owner);
            case Control.ExitPoint exit -> c.ref(exit.unit(),owner);
        }
    }

    void invocationSignature(Interactions.InvocationSignature signature,
                             Interactions.Target target,Id owner) {
        switch(signature) {
            case Interactions.EntrySignature entry -> {
                c.ref(entry.entry(),owner);
                if(!(target instanceof Interactions.InternalTarget internal)
                        || !internal.entry().equals(entry.entry()))
                    c.error("I-55",owner,
                            "internal invocation signature must reference exactly the target entry");
            }
            case Interactions.ExternalSignature external -> {
                if(target instanceof Interactions.InternalTarget)
                    c.error("I-55",owner,"internal target cannot carry an external signature");
                signature(external.signature(),owner,null,SignatureContext.EXTERNAL);
            }
        }
    }

    void contract(Interactions.ContractKnowledge knowledge,Id owner) {
        switch(knowledge) {
            case Interactions.KnownContract known -> c.refs(known.reference().evidence(),owner);
            case Interactions.UnknownContract unknown ->
                    c.uncertainty(unknown.uncertainty(),"CONTRACT_UNKNOWN",owner);
        }
    }

    private record BindingNode(Memory.Binding binding,Types.TypeRef type,Types.TypeRef parentType) {}
    private void binding(Memory.Binding binding,Types.TypeRef type,Id owner,int depth) {
        Walk.run(new BindingNode(binding,type,null),depth,node -> {
            if(!(node.binding() instanceof Memory.AlternativesBinding alternatives)) return List.of();
            // Lazy indexed view: no list of all sibling frames retained at each level.
            return new AbstractList<BindingNode>() {
                public int size() { return alternatives.alternatives().size(); }
                public BindingNode get(int index) {
                    Memory.Binding child=alternatives.alternatives().get(index);
                    return new BindingNode(child,associatedType(child).orElse(node.type()),node.type());
                }
            };
        },new Walk.Visitor<BindingNode>() {
            public boolean enter(BindingNode node,long nesting) {
                c.depth(nesting);
                if(node.parentType() instanceof Types.Known && !TypeResolver.sameRef(node.type(),node.parentType()))
                    c.error("I-51",owner,"known association domain contradicts an alternative");
                bindingLeaf(node.binding(),node.type(),owner,nesting);
                return true;
            }
            public void exit(BindingNode node,long nesting) {
                if(!(node.binding() instanceof Memory.AlternativesBinding alternatives)) return;
                Types.TypeRef declared=node.type();
                Types.TypeRef first=null; boolean homogeneous=true;
                for(Memory.Binding alternative:alternatives.alternatives()) {
                    Types.TypeRef candidate=associatedType(alternative).orElse(declared);
                    if(first==null) first=candidate;
                    else if(!TypeResolver.sameRef(candidate,first)) homogeneous=false;
                }
                if(declared instanceof Types.UnknownType && alternatives.remainder() instanceof Scopes.NoMemory
                        && first instanceof Types.Known && homogeneous)
                    c.error("I-51",owner,"closed homogeneous association must retain known domain");
                if(declared instanceof Types.Known && alternatives.remainder() instanceof Scopes.WithinMemory)
                    c.issue(ValidationIssue.Kind.VALIDATION_LIMIT,"ASSOCIATION_DOMAIN_BOUND",owner,
                            "known domain of open storage alternatives needs evidence outside this validator slice");
                memoryBound(alternatives.remainder(),owner,nesting+1);
            }
        });
    }
    private void bindingLeaf(Memory.Binding binding,Types.TypeRef type,Id owner,long depth) {
        switch(binding) {
            case Memory.CellBinding cellBinding -> {
                c.ref(cellBinding.storage(),owner);
                if(!(c.index.storage.get(cellBinding.storage()) instanceof Memory.Cell cell))
                    c.error("I-13",owner,"cell association must name cell");
                else if(!TypeResolver.sameRef(type,cell.typeRef()))
                    c.error("I-49",owner,"object/cell association must share TypeRef");
            }
            case Memory.AliasBinding alias -> {
                c.ref(alias.object(),owner);
                Memory.ObjectDeclaration target=c.index.objects.get(alias.object());
                if(target!=null && !TypeResolver.sameRef(type,target.typeRef()))
                    c.error("I-49",owner,"exact alias must share TypeRef");
            }
            case Memory.ViewBinding view -> {
                c.capability(Capabilities.MEMORY_REGIONS,owner); c.ref(view.region(),owner);
                if(!(c.index.storage.get(view.region()) instanceof Memory.Region region))
                    c.error("I-13",owner,"view needs region");
                else if(region.extent().isPresent()
                        && view.offset().add(view.extent()).compareTo(region.extent().get())>0)
                    c.error("I-13",owner,"view exceeds region extent");
                codec(view.codec(),type,owner);
                if(view.codec() instanceof Memory.BinaryCodec binary
                        && !view.extent().equals(binary.width().divide(BigInteger.valueOf(8))))
                    c.error("I-13",owner,"binary codec width differs from view extent");
            }
            case Memory.AlternativesBinding alternatives -> {
                // Contradictions belong to the declared parent type, before child checks.
                // The traversal still checks every alternative and the remainder.
            }
            case Memory.UnknownBinding unknown -> {
                memory(unknown.scope(),owner,depth+1); c.uncertainty(unknown.reason(),null,owner);
            }
        }
    }

    private Optional<Types.TypeRef> associatedType(Memory.Binding binding) {
        if(binding instanceof Memory.CellBinding cell
                && c.index.storage.get(cell.storage()) instanceof Memory.Cell storage)
            return Optional.of(storage.typeRef());
        if(binding instanceof Memory.AliasBinding alias && c.index.objects.containsKey(alias.object()))
            return Optional.of(c.index.objects.get(alias.object()).typeRef());
        if(binding instanceof Memory.ViewBinding view) return Optional.of(switch(view.codec()) {
            case Memory.IdentityBytes ignored -> new Types.Known(Types.Builtin.BYTES);
            case Memory.AsciiText ignored -> new Types.Known(Types.Builtin.TEXT);
            case Memory.BinaryCodec ignored -> new Types.Known(Types.Builtin.INT);
            case Memory.UnknownCodec unknown -> unknown.logicalType();
            case Memory.ExtensionCodec extension -> extension.logicalType();
        });
        return Optional.empty();
    }

    void codec(Memory.Codec codec,Types.TypeRef type,Id owner) {
        Types.TypeRef expected=switch(codec) {
            case Memory.IdentityBytes ignored -> new Types.Known(Types.Builtin.BYTES);
            case Memory.AsciiText ignored -> new Types.Known(Types.Builtin.TEXT);
            case Memory.BinaryCodec ignored -> new Types.Known(Types.Builtin.INT);
            case Memory.ExtensionCodec extension -> {
                c.capability(new Capabilities.Capability(extension.name(),extension.version()),owner);
                yield extension.logicalType();
            }
            case Memory.UnknownCodec unknown -> {
                c.uncertainty(unknown.reason(),null,owner); yield unknown.logicalType();
            }
        };
        c.type(expected,owner);
        if(!TypeResolver.sameRef(expected,type))
            c.error("I-49",owner,"codec logical domain contradicts place/object TypeRef");
    }

    private void cycleOrigins() {
        Map<OriginId,List<OriginId>> graph=new LinkedHashMap<>();
        c.index.origins.forEach((id,origin) -> graph.put(id,
                origin instanceof Origins.Derived derived ? derived.inputs() : List.of()));
        cycles(graph,"I-36");
    }
    private void cycleUnits() {
        Map<UnitId,List<UnitId>> graph=new LinkedHashMap<>();
        c.index.units.forEach((id,unit) -> graph.put(id,unit.containingUnit().stream().toList()));
        cycles(graph,"I-01");
    }
    private void cycleAliases() {
        Map<ObjectId,List<ObjectId>> graph=new LinkedHashMap<>();
        c.index.objects.forEach((id,object) -> graph.put(id,
                object.storage() instanceof Memory.AliasBinding alias
                        ? List.of(alias.object()) : List.of()));
        cycles(graph,"I-12");
    }
    private <T extends Id> void cycles(Map<T,List<T>> graph,String rule) {
        Map<T,Integer> degree=new LinkedHashMap<>(); Map<T,List<T>> reverse=new HashMap<>();
        graph.forEach((id,parents) -> {
            degree.put(id,0);
            for(T parent:parents) if(graph.containsKey(parent)) {
                degree.merge(id,1,Integer::sum);
                reverse.computeIfAbsent(parent,ignored -> new ArrayList<>()).add(id);
            }
        });
        ArrayDeque<T> queue=new ArrayDeque<>();
        degree.forEach((id,count) -> { if(count==0) queue.add(id); });
        while(!queue.isEmpty()) for(T child:reverse.getOrDefault(queue.remove(),List.of()))
            if(degree.merge(child,-1,Integer::sum)==0) queue.add(child);
        degree.forEach((id,count) -> {
            if(count>0) c.error(rule,id,"cyclic structural dependency");
        });
    }
}
