package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;

/** Typed referential integrity, metadata, ownership and static cycles. */
final class ReferenceChecks {
    final ValidationContext c;
    final Map<UnitId,Set<ObjectId>> visible=new HashMap<>();
    ReferenceChecks(ValidationContext c) { this.c=c; }
    void run() {
        Publication p=c.index.publication;
        checkCapabilities(p.capabilities());
        for(Unit u:p.units()) {
            Set<ObjectId> set=new HashSet<>(u.visibleObjects());
            for(Memory.ObjectDeclaration o:u.objects()) set.add(o.id());
            visible.put(u.id(),set);
        }
        for(Origins.Origin o:p.origins()) switch(o) {
            case Origins.Written w -> {
                c.ref(w.artifact(),w.id());
                for(Origins.IncludeFrame f:w.includes()) { c.ref(f.including(),w.id()); c.ref(f.included(),w.id()); }
            }
            case Origins.Derived d -> c.refs(d.inputs(),d.id());
            case Origins.Contractual ignored -> { }
            case Origins.Unavailable ignored -> { }
        }
        cycleOrigins(); cycleUnits(); cycleAliases();
        for(Evidence.Uncertainty u:p.uncertainties()) { c.ref(u.origin(),u.id()); scope(u.scope(),u.id()); }
        for(Proofs.Premise premise:p.premises()) {
            c.ref(premise.origin(),premise.id());
            if(premise.assertion() instanceof Proofs.DisjointStorage d) {
                c.refs(d.storage(),premise.id()); scope(d.scope(),premise.id());
                c.obligation("I-12",premise.id(),"storage independence must be substantiated by the producer");
            }
        }
        for(Memory.Storage storage:p.storage()) {
            Memory.StorageHeader h=storage.header(); c.ref(h.origin(),h.id()); h.owner().ifPresent(id->c.ref(id,h.id()));
            if(storage instanceof Memory.Cell cell) c.type(cell.typeRef(),h.id());
            if(storage instanceof Memory.Region region) {
                c.capability(Capabilities.MEMORY_REGIONS,h.id()); region.extentUnknown().ifPresent(id->c.uncertainty(id,null,h.id()));
            }
        }
        for(Interactions.Contract contract:p.contracts()) {
            c.ref(contract.origin(),contract.id()); c.refs(contract.premises(),contract.id());
            c.refs(contract.uncertainties(),contract.id()); signature(contract.signature(),contract.id(),null);
            contract.effects().ifPresent(e -> effects(e,contract.id(),null));
        }
        for(Interactions.Resource resource:p.resources()) target(resource.description(),resource.id());
        for(Artifacts.Relation relation:p.artifactRelations()) {
            c.ref(relation.source(),relation.id()); c.ref(relation.origin(),relation.id());
            switch(relation.destination()) {
                case Artifacts.InternalArtifact a -> c.ref(a.artifact(),relation.id());
                case Artifacts.ExternalArtifact a -> target(a.resource(),relation.id());
            }
        }
        coverage(p.coverage(),p.id());
        for(Unit unit:p.units()) {
            c.ref(unit.origin(),unit.id()); unit.containingUnit().ifPresent(id->c.ref(id,unit.id()));
            c.refs(unit.visibleObjects(),unit.id()); unit.bodyUnavailable().ifPresent(id->c.uncertainty(id,null,unit.id()));
            coverage(unit.coverage(),unit.id());
            for(Memory.ObjectDeclaration object:unit.objects()) {
                if(!object.id().unit().equals(unit.id())) c.error("I-01",object.id(),"object belongs to a different unit");
                c.type(object.typeRef(),object.id()); c.ref(object.origin(),object.id());
                binding(object.storage(),object.typeRef(),object.id(),0); precision(object.precision(),object.id());
            }
            for(Entries.CompletionPort port:unit.completionPorts()) {
                c.ref(port.origin(),port.id());
                if(!port.id().unit().equals(unit.id())) c.error("I-01",port.id(),"completion port owner differs from unit");
            }
            for(Entries.Entry entry:unit.entries()) {
                if(!entry.id().unit().equals(unit.id())) c.error("I-05",entry.id(),"entry owner differs from unit");
                c.ref(entry.origin(),entry.id()); signature(entry.signature(),entry.id(),unit.id());
                if(unit.body()==Unit.BodyAvailability.AVAILABLE && entry.initialLabel().isEmpty()) c.error("I-05",entry.id(),"available entry requires initial label");
                entry.initialLabel().ifPresent(id->label(id,unit.id(),entry.id()));
                c.refs(entry.state().uncertainties(),entry.id());
                for(Entries.InitialCondition seed:entry.state().conditions()) {
                    c.ref(seed.origin(),entry.id()); c.refs(seed.premises(),entry.id());
                    if(seed.value() instanceof Entries.ExternalUnknown e) c.uncertainty(e.reason(),null,entry.id());
                    if(seed.value() instanceof Entries.Uninitialized u) c.uncertainty(u.reason(),null,entry.id());
                }
            }
            for(Sequence sequence:unit.sequences()) {
                c.ref(sequence.origin(),sequence.label());
                if(!sequence.label().unit().equals(unit.id())) c.error("I-03",sequence.label(),"sequence owner differs from unit");
                for(Operation op:sequence.operations()) {
                    c.ref(op.header().origin(),op.header().id()); c.refs(op.header().uncertainties(),op.header().id());
                    precision(op.header().precision(),op.header().id());
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
            choice.knownRemainderDomainProof().ifPresent(proof->c.ref(proof,id));
        } else if(operand instanceof Places.RegionSlice slice) {
            c.ref(slice.region(),id); c.capability(Capabilities.MEMORY_REGIONS,id);
            if(!(c.index.storage.get(slice.region()) instanceof Memory.Region)) c.error("I-13",id,"region_slice must reference a region");
            c.type(slice.typeRef(),id); codec(slice.codec(),slice.typeRef(),id);
            slice.accessProof().ifPresent(proof->c.ref(proof,id));
        } else if(operand instanceof Expressions.Unknown unknown) {
            c.type(unknown.typeRef(),id); c.uncertainty(unknown.reason(),null,id); memoryBound(unknown.remainingReads(),id,0);
            if(unknown.typeRef() instanceof Types.UnknownType t && t.uncertainty().equals(unknown.reason()))
                c.error("I-50",id,"type uncertainty and value uncertainty need distinct identities");
            c.obligation("I-09",id,"unknown expression preserves only pure evaluation; producer must substantiate purity and remaining reads");
        } else if(operand instanceof Expressions.SliceText slice) slice.boundsProof().ifPresent(proof->c.ref(proof,id));
    }
    void checkCapabilities(Capabilities.Manifest manifest) {
        for(List<Capabilities.Capability> list:List.of(manifest.required(),manifest.provided())) {
            Set<String> names=new HashSet<>();
            for(Capabilities.Capability capability:list) {
                if(!names.add(capability.name())) c.error("I-43",c.index.publication.id(),"duplicate capability name: "+capability.name());
                boolean standard=List.of(Capabilities.MEMORY_REGIONS,Capabilities.LOCAL_CONTROL,Capabilities.INDIRECT_CONTROL).contains(capability);
                boolean profile=capability.name().startsWith("AIR-");
                if(profile) c.obligation("profile",c.index.publication.id(),"declared consumer/producer profiles require their own oracle evidence: "+capability);
                else if(!standard && manifest.required().contains(capability)) c.unsupported("I-43",c.index.publication.id(),"extension semantic contract is not implemented by this validator: "+capability);
            }
        }
    }
    void signature(Interactions.Signature signature,Id owner,UnitId unit) {
        checkPositions(signature.parameters().stream().map(Interactions.Parameter::position).toList(),signature.incomplete().isEmpty(),owner);
        checkPositions(signature.results().stream().map(Interactions.ResultSlot::position).toList(),signature.incomplete().isEmpty(),owner);
        signature.incomplete().ifPresent(id->c.uncertainty(id,null,owner));
        for(Interactions.Parameter p:signature.parameters()) {
            c.type(p.typeRef(),owner); c.ref(p.origin(),owner);
            p.object().ifPresent(id->{ c.ref(id,owner); if(unit!=null && !id.unit().equals(unit)) c.error("I-08",owner,"parameter initialization object is not owned by entry unit"); });
        }
        for(Interactions.ResultSlot p:signature.results()) { c.type(p.typeRef(),owner); c.ref(p.origin(),owner); }
    }
    private void checkPositions(List<Integer> positions,boolean complete,Id owner) {
        int previous=-1;
        for(int i=0;i<positions.size();i++) {
            int position=positions.get(i);
            if(position<=previous || (complete && position!=i)) c.error("I-08",owner,"signature positions must be unique/ordered, and contiguous when complete");
            previous=position;
        }
    }
    void label(LabelId label,UnitId unit,Id owner) {
        c.ref(label,owner);
        if(!label.unit().equals(unit)) c.error("I-02",owner,"local control target crosses unit");
    }
    void target(Interactions.Target target,Id owner) {
        switch(target) {
            case Interactions.InternalTarget t -> c.ref(t.entry(),owner);
            case Interactions.LiteralTarget t -> { c.ref(t.origin(),owner); namePolicy(t.namePolicy(),owner); }
            case Interactions.ComputedTarget t -> { c.ref(t.origin(),owner); namePolicy(t.namePolicy(),owner); }
            case Interactions.ResourceTarget t -> c.ref(t.resource(),owner);
        }
    }
    void namePolicy(Interactions.NamePolicy policy,Id owner) {
        if(policy instanceof Interactions.ContractName p) c.ref(p.contract(),owner);
        if(policy instanceof Interactions.UnknownName p) c.uncertainty(p.uncertainty(),null,owner);
    }
    void scope(Scopes.FactScope scope,Id owner) {
        switch(scope) {
            case Scopes.PublicationScope p -> c.ref(p.publication(),owner);
            case Scopes.UnitScope u -> c.ref(u.unit(),owner);
            case Scopes.EntityScope e -> c.refs(e.entities(),owner);
        }
    }
    void precision(Evidence.Precision precision,Id owner) {
        for(Evidence.Dimension d:Evidence.Dimension.values()) {
            Evidence.Claim claim=precision.claim(d); scope(claim.scope(),owner); c.refs(claim.reasons(),owner);
            if((claim.status()==Evidence.PrecisionStatus.OPEN || claim.status()==Evidence.PrecisionStatus.UNAVAILABLE) && claim.reasons().isEmpty())
                c.error("I-32",owner,"open/unavailable precision requires reasons for "+d);
        }
    }
    void coverage(Evidence.Coverage coverage,Id owner) {
        scope(coverage.scope(),owner); c.refs(coverage.uncertainties(),owner);
        if(coverage.inventory()!=Evidence.InventoryStatus.COMPLETE && coverage.uncertainties().isEmpty())
            c.error("I-28",owner,"partial/unavailable inventory needs explicit reason");
        Set<String> keys=new HashSet<>();
        for(Evidence.CoverageItem item:coverage.items()) {
            if(!keys.add(item.sourceKey())) c.error("I-29",owner,"duplicate source inventory key: "+item.sourceKey());
            c.ref(item.origin(),owner); c.refs(item.outputs(),owner); c.refs(item.uncertainties(),owner);
            item.elimination().ifPresent(e->c.ref(e.justification(),owner));
            if(item.outputs().isEmpty() && item.uncertainties().isEmpty() && item.elimination().isEmpty()) c.error("I-30",owner,"coverage item disappeared without output, uncertainty or justified elimination");
            if(item.status()!=Evidence.CoverageStatus.MODELED && item.uncertainties().isEmpty()) c.error("I-30",owner,"incomplete coverage item lacks uncertainty");
        }
    }
    void memoryBound(Scopes.MemoryBound bound,Id owner,int depth) {
        c.depth(depth); if(bound instanceof Scopes.WithinMemory b) memory(b.scope(),owner,depth+1);
    }
    void memory(Scopes.MemoryScope scope,Id owner,int depth) {
        c.depth(depth);
        switch(scope) {
            case Scopes.ObjectsMemory s -> c.refs(s.objects(),owner);
            case Scopes.StorageMemory s -> c.refs(s.storage(),owner);
            case Scopes.VisibleMemory s -> c.ref(s.unit(),owner);
            case Scopes.AllMemory s -> c.ref(s.publication(),owner);
            case Scopes.MemoryUnion s -> { for(Scopes.MemoryScope member:s.members()) memory(member,owner,depth+1); }
        }
    }
    void controlScope(Scopes.ControlScope scope,UnitId unit,Id owner,int depth) {
        c.depth(depth);
        switch(scope) {
            case Scopes.LabelsControl l -> { for(LabelId label:l.labels()) label(label,unit,owner); }
            case Scopes.UnitControl u -> c.ref(u.unit(),owner);
            case Scopes.AllControl a -> c.ref(a.publication(),owner);
            case Scopes.ControlUnion u -> { for(Scopes.ControlScope s:u.members()) controlScope(s,unit,owner,depth+1); }
        }
    }
    void control(Control.Envelope control,UnitId unit,Id owner) {
        for(Control.Alternative a:control.known()) switch(a) {
            case Control.Normal n -> label(n.label(),unit,owner);
            case Control.Exceptional e -> exception(e.destination(),unit,owner);
            case Control.AnyException e -> exception(e.destination(),unit,owner);
            case Control.HaltAlternative ignored -> { }
            case Control.Diverge ignored -> { }
        }
        if(control.remainder() instanceof Scopes.WithinControl w) controlScope(w.scope(),unit,owner,0);
    }
    private void exception(Control.ExceptionDestination d,UnitId unit,Id owner) { if(d instanceof Control.Handler h) label(h.label(),unit,owner); }
    void effects(Interactions.EffectBound bound,Id owner,Operations.Invoke invoke) {
        foreign(bound.otherwise(),owner,invoke);
        Set<Control.OutcomeKey> keys=new HashSet<>();
        for(Interactions.OutcomeEffects e:bound.perOutcome()) {
            if(!keys.add(e.outcome())) c.error("I-23",owner,"duplicate outcome-specific effects");
            foreign(e.effects(),owner,invoke);
            if(invoke!=null && !hasOutcome(invoke.outcomes(),e.outcome())) c.error("I-23",owner,"effect bound names an undeclared invocation outcome");
        }
    }
    private void foreign(Interactions.ForeignEffects e,Id owner,Operations.Invoke invoke) {
        memoryBound(e.reads(),owner,0); memoryBound(e.writes(),owner,0); c.refs(e.mustOverwrite(),owner);
        for(OperandId id:e.mustOverwrite()) {
            if(!(c.index.operands.get(id) instanceof Place)) c.error("I-23",owner,"must_overwrite must reference place operand");
            if(invoke!=null && !id.owner().equals(new OperationOwner(invoke.header().id()))) c.error("I-23",owner,"foreign effect references another site's operand");
        }
    }
    static boolean hasOutcome(Control.Envelope e,Control.OutcomeKey key) {
        return switch(key) {
            case Control.NormalOutcome ignored -> e.known().stream().anyMatch(Control.Normal.class::isInstance);
            case Control.ExceptionOutcome tag -> e.known().stream().anyMatch(a->a instanceof Control.Exceptional x && x.tag().equals(tag.tag()));
            case Control.OtherExceptionOutcome ignored -> e.known().stream().anyMatch(Control.AnyException.class::isInstance);
            case Control.HaltOutcome ignored -> e.known().contains(Control.HaltAlternative.INSTANCE);
            case Control.DivergeOutcome ignored -> e.known().contains(Control.Diverge.INSTANCE);
        };
    }
    void envelope(Envelopes.Envelope e,OperationId owner) {
        control(e.control(),owner.unit(),owner); memoryBound(e.memory().otherReads(),owner,0); memoryBound(e.memory().otherWrites(),owner,0);
        for(List<OperandId> ids:List.of(e.memory().knownReads(),e.memory().knownWrites(),e.memory().mustOverwrite())) {
            c.refs(ids,owner);
            for(OperandId id:ids) if(!id.owner().equals(new OperationOwner(owner))) c.error("I-11",owner,"envelope operand belongs to another site");
        }
        for(OperandId id:e.memory().knownWrites()) if(!(c.index.operands.get(id) instanceof Place)) c.error("I-23",owner,"known write does not identify a place");
        for(OperandId id:e.memory().mustOverwrite()) if(!(c.index.operands.get(id) instanceof Place)) c.error("I-23",owner,"must overwrite does not identify a place");
        for(Envelopes.ResourceUse use:e.dependencies().known()) {
            target(use.target(),owner); c.ref(use.origin(),owner); point(use.point(),owner);
        }
        c.obligation("I-26",owner,"fallback bounds must overapproximate source/extension semantics; validation does not prove that correspondence");
    }
    void point(Control.ProgramPoint point,Id owner) {
        switch(point) {
            case Control.Before b -> c.ref(b.operation(),owner);
            case Control.After a -> { c.ref(a.operation(),owner); if(c.index.operations.get(a.operation()) instanceof Operations.Invoke i && !hasOutcome(i.outcomes(),a.outcome())) c.error("I-02",owner,"point names undeclared invocation outcome"); }
            case Control.EntryPoint e -> c.ref(e.entry(),owner);
            case Control.ExitPoint e -> c.ref(e.unit(),owner);
        }
    }
    private void binding(Memory.Binding binding,Types.TypeRef type,Id owner,int depth) {
        c.depth(depth);
        switch(binding) {
            case Memory.CellBinding b -> {
                c.ref(b.storage(),owner);
                if(!(c.index.storage.get(b.storage()) instanceof Memory.Cell cell)) c.error("I-13",owner,"cell association must name cell");
                else if(!TypeResolver.sameRef(type,cell.typeRef())) c.error("I-49",owner,"object/cell association must share TypeRef");
            }
            case Memory.AliasBinding a -> {
                c.ref(a.object(),owner); Memory.ObjectDeclaration target=c.index.objects.get(a.object());
                if(target!=null && !TypeResolver.sameRef(type,target.typeRef())) c.error("I-49",owner,"exact alias must share TypeRef");
            }
            case Memory.ViewBinding v -> {
                c.capability(Capabilities.MEMORY_REGIONS,owner); c.ref(v.region(),owner);
                if(!(c.index.storage.get(v.region()) instanceof Memory.Region r)) c.error("I-13",owner,"view needs region");
                else if(r.extent().isPresent() && v.offset().add(v.extent()).compareTo(r.extent().get())>0) c.error("I-13",owner,"view exceeds region extent");
                codec(v.codec(),type,owner);
                if(v.codec() instanceof Memory.BinaryCodec b && !v.extent().equals(java.math.BigInteger.valueOf(b.width()/8))) c.error("I-13",owner,"binary codec width differs from view extent");
            }
            case Memory.AlternativesBinding a -> {
                List<Types.TypeRef> knownCandidates=new ArrayList<>();
                for(Memory.Binding alt:a.alternatives()) {
                    Types.TypeRef candidate=associatedType(alt).orElse(type);
                    knownCandidates.add(candidate);
                    if(type instanceof Types.Known k && !TypeResolver.sameRef(candidate,type))
                        c.error("I-51",owner,"known association domain contradicts an alternative");
                    binding(alt,candidate,owner,depth+1);
                }
                if(type instanceof Types.UnknownType && a.remainder() instanceof Scopes.NoMemory
                        && !knownCandidates.isEmpty() && knownCandidates.get(0) instanceof Types.Known
                        && knownCandidates.stream().allMatch(t->TypeResolver.sameRef(t,knownCandidates.get(0))))
                    c.error("I-51",owner,"closed homogeneous association must retain known domain");
                if(type instanceof Types.Known && a.remainder() instanceof Scopes.WithinMemory)
                    c.issue(ValidationIssue.Kind.VALIDATION_LIMIT,"ASSOCIATION_DOMAIN_BOUND",owner,"known domain of open storage alternatives requires domain-bound evidence not yet checked by this validator");
                memoryBound(a.remainder(),owner,depth+1);
            }
            case Memory.UnknownBinding u -> { memory(u.scope(),owner,depth+1); c.uncertainty(u.reason(),null,owner); }
        }
    }
    private Optional<Types.TypeRef> associatedType(Memory.Binding binding) {
        if(binding instanceof Memory.CellBinding b && c.index.storage.get(b.storage()) instanceof Memory.Cell cell) return Optional.of(cell.typeRef());
        if(binding instanceof Memory.AliasBinding a && c.index.objects.containsKey(a.object())) return Optional.of(c.index.objects.get(a.object()).typeRef());
        if(binding instanceof Memory.ViewBinding v) return Optional.of(switch(v.codec()) {
            case Memory.IdentityBytes ignored -> new Types.Known(Types.Builtin.BYTES);
            case Memory.AsciiText ignored -> new Types.Known(Types.Builtin.TEXT);
            case Memory.BinaryCodec ignored -> new Types.Known(Types.Builtin.INT);
            case Memory.UnknownCodec u -> u.logicalType();
            case Memory.ExtensionCodec e -> e.logicalType();
        });
        return Optional.empty();
    }
    void codec(Memory.Codec codec,Types.TypeRef type,Id owner) {
        Types.TypeRef expected=switch(codec) {
            case Memory.IdentityBytes ignored -> new Types.Known(Types.Builtin.BYTES);
            case Memory.AsciiText ignored -> new Types.Known(Types.Builtin.TEXT);
            case Memory.BinaryCodec ignored -> new Types.Known(Types.Builtin.INT);
            case Memory.ExtensionCodec e -> { c.ref(e.contract(),owner); c.capability(new Capabilities.Capability(e.name(),e.version().major()),owner); yield e.logicalType(); }
            case Memory.UnknownCodec u -> { c.uncertainty(u.reason(),null,owner); yield u.logicalType(); }
        };
        c.type(expected,owner);
        if(!TypeResolver.sameRef(expected,type)) c.error("I-49",owner,"codec logical domain contradicts place/object TypeRef");
    }
    private void cycleOrigins() {
        Map<OriginId,List<OriginId>> graph=new LinkedHashMap<>();
        c.index.origins.forEach((id,origin)->graph.put(id,origin instanceof Origins.Derived d?d.inputs():List.of()));
        cycles(graph,"I-36");
    }
    private void cycleUnits() {
        Map<UnitId,List<UnitId>> graph=new LinkedHashMap<>(); c.index.units.forEach((id,u)->graph.put(id,u.containingUnit().stream().toList())); cycles(graph,"I-01");
    }
    private void cycleAliases() {
        Map<ObjectId,List<ObjectId>> graph=new LinkedHashMap<>(); c.index.objects.forEach((id,o)->graph.put(id,o.storage() instanceof Memory.AliasBinding a?List.of(a.object()):List.of())); cycles(graph,"I-12");
    }
    private <T extends Id> void cycles(Map<T,List<T>> graph,String rule) {
        // Kahn elimination is iterative and avoids call-stack overflow on long origin/alias chains.
        Map<T,Integer> degree=new LinkedHashMap<>(); Map<T,List<T>> reverse=new HashMap<>();
        graph.forEach((id,parents)-> { degree.put(id,0); for(T p:parents) if(graph.containsKey(p)) { degree.merge(id,1,Integer::sum); reverse.computeIfAbsent(p,k->new ArrayList<>()).add(id); } });
        ArrayDeque<T> queue=new ArrayDeque<>(); degree.forEach((id,n)-> { if(n==0) queue.add(id); });
        while(!queue.isEmpty()) for(T child:reverse.getOrDefault(queue.remove(),List.of())) if(degree.merge(child,-1,Integer::sum)==0) queue.add(child);
        degree.forEach((id,n)-> { if(n>0) c.error(rule,id,"cyclic structural dependency"); });
    }
}
