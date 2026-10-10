package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;

/** Internal read-only input to the existing validation rules; bodies are borrowed views. */
interface ValidationProgram {
    PublicationId id(); SemanticVersion airVersion(); Capabilities.Manifest capabilities();
    List<Origins.Artifact> artifacts(); List<UnitView> units(); List<Memory.Storage> storage();
    List<Interactions.Resource> resources(); List<Artifacts.Relation> artifactRelations();
    List<Origins.Origin> origins(); CoverageView coverage();
    List<Evidence.Uncertainty> uncertainties(); List<Proofs.Premise> premises();
    interface UnitView {
        UnitId id(); Optional<UnitId> containingUnit(); List<Memory.ObjectDeclaration> objects();
        List<ObjectId> visibleObjects(); List<Entries.Entry> entries(); List<SequenceView> sequences();
        List<Entries.CompletionPort> completionPorts(); Unit.BodyAvailability body();
        Optional<UncertaintyId> bodyUnavailable(); CoverageView coverage(); OriginId origin();
    }
    interface SequenceView {
        LabelId label(); List<Instruction> instructions(); Terminator terminator(); OriginId origin();
        default List<Operation> operations() {
            return new AbstractList<>() {
                @Override public int size(){return Math.addExact(instructions().size(),1);}
                @Override public Operation get(int index){Objects.checkIndex(index,size());return index==instructions().size()?terminator():instructions().get(index);}
            };
        }
    }
    interface CoverageView {
        Evidence.InventoryStatus inventory(); Scopes.FactScope scope();
        List<Evidence.CoverageItem> items(); List<UncertaintyId> uncertainties();
    }
    static ValidationProgram resident(Publication p) {
        Objects.requireNonNull(p);
        return new ValidationProgram() {
            public PublicationId id(){return p.id();} public SemanticVersion airVersion(){return p.airVersion();}
            public Capabilities.Manifest capabilities(){return p.capabilities();} public List<Origins.Artifact> artifacts(){return p.artifacts();}
            public List<UnitView> units(){return p.units().stream().map(ValidationProgram::unit).toList();}
            public List<Memory.Storage> storage(){return p.storage();} public List<Interactions.Resource> resources(){return p.resources();}
            public List<Artifacts.Relation> artifactRelations(){return p.artifactRelations();} public List<Origins.Origin> origins(){return p.origins();}
            public CoverageView coverage(){return ValidationProgram.coverage(p.coverage());}
            public List<Evidence.Uncertainty> uncertainties(){return p.uncertainties();} public List<Proofs.Premise> premises(){return p.premises();}
        };
    }
    private static UnitView unit(Unit u) {
        return new UnitView() {
            public UnitId id(){return u.id();} public Optional<UnitId> containingUnit(){return u.containingUnit();}
            public List<Memory.ObjectDeclaration> objects(){return u.objects();} public List<ObjectId> visibleObjects(){return u.visibleObjects();}
            public List<Entries.Entry> entries(){return u.entries();} public List<SequenceView> sequences(){return u.sequences().stream().map(ValidationProgram::sequence).toList();}
            public List<Entries.CompletionPort> completionPorts(){return u.completionPorts();} public Unit.BodyAvailability body(){return u.body();}
            public Optional<UncertaintyId> bodyUnavailable(){return u.bodyUnavailable();} public CoverageView coverage(){return ValidationProgram.coverage(u.coverage());}
            public OriginId origin(){return u.origin();}
        };
    }
    private static SequenceView sequence(Sequence s) {
        return new SequenceView() {
            public LabelId label(){return s.label();} public List<Instruction> instructions(){return s.instructions();}
            public Terminator terminator(){return s.terminator();} public OriginId origin(){return s.origin();}
        };
    }
    static CoverageView coverage(Evidence.Coverage c) {
        return new CoverageView() {
            public Evidence.InventoryStatus inventory(){return c.inventory();} public Scopes.FactScope scope(){return c.scope();}
            public List<Evidence.CoverageItem> items(){return c.items();} public List<UncertaintyId> uncertainties(){return c.uncertainties();}
        };
    }
    default Set<Capabilities.Capability> namePolicies() {
        var result=new HashSet<Capabilities.Capability>();
        for(var unit:units())for(var sequence:unit.sequences())if(sequence.terminator() instanceof Operations.Invoke invoke) {
            if(invoke.target() instanceof Interactions.LiteralTarget t)add(result,t.namePolicy());
            if(invoke.target() instanceof Interactions.ComputedTarget t)add(result,t.namePolicy());
        }
        for(var resource:resources()) {
            if(resource.description() instanceof Interactions.LiteralTarget t)add(result,t.namePolicy());
            if(resource.description() instanceof Interactions.ComputedResource t)add(result,t.namePolicy());
        }
        return Set.copyOf(result);
    }
    private static void add(Set<Capabilities.Capability> result,Interactions.NamePolicy p) {
        if(p instanceof Interactions.ExtensionName e)result.add(new Capabilities.Capability(e.name(),e.version()));
    }
}
