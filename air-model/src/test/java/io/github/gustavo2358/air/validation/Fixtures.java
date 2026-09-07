package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.math.BigInteger;
import java.util.*;

/** Test builders only. No builder or dataflow fixtures leak into the production domain. */
final class Fixtures {
    final PublicationId pub=new PublicationId("fixture");
    final UnitId unit=new UnitId(pub,"unit");
    final OriginId origin=new OriginId(pub,"origin");
    final List<Memory.ObjectDeclaration> objects=new ArrayList<>();
    final List<Memory.Storage> storage=new ArrayList<>();
    final List<Sequence> sequences=new ArrayList<>();
    final List<Proofs.Premise> premises=new ArrayList<>();
    final List<Evidence.Uncertainty> uncertainties=new ArrayList<>();
    final List<Capabilities.Capability> capabilities=new ArrayList<>();
    final List<Origins.Artifact> artifacts=new ArrayList<>();
    final List<Interactions.Resource> resources=new ArrayList<>();
    final List<Artifacts.Relation> artifactRelations=new ArrayList<>();
    final List<Origins.Origin> origins=new ArrayList<>();
    final List<Entries.CompletionPort> completionPorts=new ArrayList<>();
    Interactions.Signature signature=signature(List.of(),List.of());
    Entries.EntryState state=new Entries.EntryState(List.of(),List.of());
    Fixtures() { origins.add(new Origins.Unavailable(origin,"synthetic normative fixture")); }
    OperationId op(String name) { return new OperationId(unit,name); }
    LabelId label(String name) { return new LabelId(unit,name); }
    EntryId entry() { return new EntryId(unit,"entry"); }
    Operand.Header operand(OperationId op,String name,Operand.Role role) { return new Operand.Header(new OperandId(new OperationOwner(op),name),role,origin); }
    Operand.Header entryOperand(String name,Operand.Role role) { return new Operand.Header(new OperandId(new EntryOwner(entry()),name),role,origin); }
    Evidence.Claim claim() { return new Evidence.Claim(new Scopes.UnitScope(unit),Evidence.PrecisionStatus.EXACT,List.of()); }
    Evidence.Precision precision() { Evidence.Claim c=claim(); return new Evidence.Precision(c,c,c,c,c); }
    Operations.Header header(OperationId id) { return new Operations.Header(id,origin,Evidence.CoverageStatus.MODELED,precision(),List.of()); }
    Operations.Header header(OperationId id,UncertaintyId reason) { return new Operations.Header(id,origin,Evidence.CoverageStatus.ABSTRACTED,precision(),List.of(reason)); }
    ObjectId object(String name,Types.TypeRef type) {
        ObjectId id=new ObjectId(unit,name); StorageId sid=new StorageId(pub,name+"-cell");
        storage.add(new Memory.Cell(new Memory.StorageHeader(sid,Optional.of(unit),Memory.Lifetime.ACTIVATION,Memory.Visibility.PRIVATE,origin),type));
        objects.add(new Memory.ObjectDeclaration(id,Optional.of(name),type,new Memory.CellBinding(sid),Memory.Visibility.PRIVATE,origin,Evidence.CoverageStatus.MODELED,precision()));
        return id;
    }
    ObjectId alias(String name,ObjectId to,Types.TypeRef type) {
        ObjectId id=new ObjectId(unit,name);
        objects.add(new Memory.ObjectDeclaration(id,Optional.of(name),type,new Memory.AliasBinding(to),Memory.Visibility.PRIVATE,origin,Evidence.CoverageStatus.MODELED,precision())); return id;
    }
    UncertaintyId uncertainty(String name,String code) {
        UncertaintyId id=new UncertaintyId(pub,name);
        uncertainties.add(new Evidence.Uncertainty(id,code,List.of(Evidence.Dimension.VALUES),new Scopes.UnitScope(unit),"fixture uncertainty",origin)); return id;
    }
    Places.ObjectPlace place(OperationId op,String name,ObjectId object,Operand.Role role) { return new Places.ObjectPlace(operand(op,name,role),object); }
    Expressions.Read read(OperationId op,String name,ObjectId object,Operand.Role role) {
        return new Expressions.Read(operand(op,name,role),place(op,name+"-place",object,Operand.Role.VALUE_READ));
    }
    Expressions.Literal text(OperationId op,String name,String value) { return new Expressions.Literal(operand(op,name,Operand.Role.VALUE_READ),new Values.TextValue(value)); }
    Expressions.Literal integer(OperationId op,String name,long value,Operand.Role role) { return new Expressions.Literal(operand(op,name,role),new Values.IntValue(BigInteger.valueOf(value))); }
    Operations.Assign assign(String id,ObjectId destination,Expression value) { OperationId op=op(id); return new Operations.Assign(header(op),place(op,"destination",destination,Operand.Role.VALUE_WRITE),value); }
    void sequence(String label,List<Instruction> instructions,Terminator terminator) { sequences.add(new Sequence(label(label),instructions,terminator,origin)); }
    Operations.Halt halt(String id) { return new Operations.Halt(header(op(id)),Operations.HaltKind.NORMAL); }
    void linear(Instruction instruction) { sequence("start",List.of(instruction),halt("stop")); }
    Proofs.Premise proof(String name,Proofs.DomainSubject a,Proofs.DomainSubject b,Proofs.DomainProofScope scope) {
        Proofs.Premise premise=new Proofs.Premise(new PremiseId(pub,name),"fixture authority","explicit semantic premise",origin,new Proofs.SameDomain(a,b,scope)); premises.add(premise); return premise;
    }
    Envelopes.Envelope envelope(LabelId normal) {
        return new Envelopes.Envelope(new Envelopes.MemoryEnvelope(List.of(),new Scopes.WithinMemory(new Scopes.VisibleMemory(unit,true)),List.of(),new Scopes.WithinMemory(new Scopes.VisibleMemory(unit,true)),List.of()),
                new Control.ControlEnvelope(normal==null?List.of():List.of(new Control.Normal(normal)),new Scopes.WithinControl(new Scopes.AllControl(pub))),
                new Envelopes.DependencyEnvelope(List.of(),Scopes.AnyResource.INSTANCE));
    }
    Interactions.EffectBound effects() { return new Interactions.EffectBound(new Interactions.ForeignEffects(new Scopes.WithinMemory(new Scopes.VisibleMemory(unit,true)),new Scopes.WithinMemory(new Scopes.VisibleMemory(unit,true)),List.of()),List.of()); }
    Evidence.Coverage coverage(Scopes.FactScope scope) { return new Evidence.Coverage(Evidence.InventoryStatus.COMPLETE,scope,List.of(),List.of()); }
    Interactions.Signature signature(List<Interactions.Parameter> parameters,List<Interactions.ResultSlot> results) {
        return new Interactions.Signature(
                new Interactions.ParameterInventory(parameters,Interactions.NoRemainder.INSTANCE),
                new Interactions.ResultInventory(results,Interactions.NoRemainder.INSTANCE),origin);
    }
    Publication build() {
        Entries.Entry entry=new Entries.Entry(entry(),sequences.isEmpty()?Optional.empty():Optional.of(sequences.get(0).label()),signature,state,origin);
        Unit u=new Unit(unit,Optional.empty(),objects,List.of(),List.of(entry),sequences,completionPorts,Unit.BodyAvailability.AVAILABLE,Optional.empty(),coverage(new Scopes.UnitScope(unit)),origin);
        return new Publication(pub,SemanticVersion.AIR_2_0_0,new Capabilities.Manifest(capabilities,capabilities),artifacts,List.of(u),storage,resources,artifactRelations,origins,coverage(new Scopes.PublicationScope(pub)),uncertainties,premises);
    }
    static Publication withUnits(Publication p,List<Unit> units) { return new Publication(p.id(),p.airVersion(),p.capabilities(),p.artifacts(),units,p.storage(),p.resources(),p.artifactRelations(),p.origins(),p.coverage(),p.uncertainties(),p.premises()); }
    static Types.Known known(Types.Type type) { return new Types.Known(type); }
}
