package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;

/** Per-validation identity indexes, built once. No resolution by names or source locations. */
final class PublicationIndex {
    final Publication publication;
    final Set<Id> identities = new LinkedHashSet<>();
    final Map<UnitId,Unit> units=new LinkedHashMap<>();
    final Map<ObjectId,Memory.ObjectDeclaration> objects=new LinkedHashMap<>();
    final Map<StorageId,Memory.Storage> storage=new LinkedHashMap<>();
    final Map<EntryId,Entries.Entry> entries=new LinkedHashMap<>();
    final Map<LabelId,Sequence> sequences=new LinkedHashMap<>();
    final Map<OperationId,Operation> operations=new LinkedHashMap<>();
    final Map<OperationId,LabelId> sequenceOf=new LinkedHashMap<>();
    final Map<OperandId,Operand> operands=new LinkedHashMap<>();
    final Map<OriginId,Origins.Origin> origins=new LinkedHashMap<>();
    final Map<UncertaintyId,Evidence.Uncertainty> uncertainties=new LinkedHashMap<>();
    final Map<PremiseId,Proofs.Premise> premises=new LinkedHashMap<>();
    final Map<ResourceId,Interactions.Resource> resources=new LinkedHashMap<>();
    final Map<ArtifactRelationId,Artifacts.Relation> artifactRelations=new LinkedHashMap<>();
    final ValidationContext context;

    PublicationIndex(Publication p,ValidationContext context) { this.publication=p; this.context=context; }
    void build() {
        add(publication.id());
        for(Origins.Artifact a:publication.artifacts()) add(a.id());
        for(Origins.Origin o:publication.origins()) { add(o.id()); origins.putIfAbsent(o.id(),o); }
        for(Evidence.Uncertainty u:publication.uncertainties()) { add(u.id()); uncertainties.putIfAbsent(u.id(),u); }
        for(Proofs.Premise p:publication.premises()) { add(p.id()); premises.putIfAbsent(p.id(),p); }
        for(Interactions.Resource r:publication.resources()) { add(r.id()); resources.putIfAbsent(r.id(),r); }
        for(Artifacts.Relation r:publication.artifactRelations()) {
            add(r.id()); artifactRelations.putIfAbsent(r.id(),r);
        }
        for(Memory.Storage s:publication.storage()) { add(s.header().id()); storage.putIfAbsent(s.header().id(),s); }
        for(Unit u:publication.units()) {
            add(u.id()); units.putIfAbsent(u.id(),u);
            for(Memory.ObjectDeclaration o:u.objects()) { add(o.id()); objects.putIfAbsent(o.id(),o); }
            for(Entries.CompletionPort c:u.completionPorts()) add(c.id());
            for(Entries.Entry e:u.entries()) {
                add(e.id()); entries.putIfAbsent(e.id(),e);
                for(Entries.InitialCondition seed:e.state().conditions()) {
                    operand(seed.place(),new EntryOwner(e.id()),0);
                    if(seed.value() instanceof Entries.LiteralInitial literal)
                        operand(literal.value(),new EntryOwner(e.id()),0);
                }
            }
            for(Sequence s:u.sequences()) {
                add(s.label()); sequences.putIfAbsent(s.label(),s);
                for(Operation op:s.operations()) {
                    add(op.header().id()); operations.putIfAbsent(op.header().id(),op);
                    sequenceOf.putIfAbsent(op.header().id(),s.label());
                    if(!op.header().id().unit().equals(u.id()) || !s.label().unit().equals(u.id()))
                        context.error("I-03",op.header().id(),"operation/sequence owner differs from unit");
                    for(Operand operand:Operands.roots(op)) operand(operand,new OperationOwner(op.header().id()),0);
                }
            }
        }
    }
    void add(Id id) {
        if(identities.size()>=context.options.maximumEntities()) throw new ValidationContext.Limit("entity limit");
        if(!publication.id().equals(id.publication())) context.error("I-01",id,"identity crosses publication namespace");
        if(!identities.add(id)) context.error("I-01",id,"duplicate identity");
    }
    private void operand(Operand o,OperandOwner expected,int depth) {
        Walk.run(o,depth,Operands::children,new Walk.Visitor<Operand>() {
            public boolean enter(Operand node,long nesting) {
                context.depth(nesting);
                OperandId id=node.header().id(); add(id);
                if(!id.owner().equals(expected)) context.error("I-11",id,"operand belongs to another operation/entry");
                return operands.putIfAbsent(id,node)==null;
            }
        });
    }
}
