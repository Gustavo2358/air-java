package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;

/** CORE-SIZE-001 oracles: expected classifications are fixed by construction. */
public final class CapacityChecks {
    private CapacityChecks() {}
    static void nesting() {
        for(int n:new int[]{256,512,1024}) {
            Fixtures f=new Fixtures(); ObjectId x=f.object("x",Types.known(Types.Builtin.INT));
            OperationId op=f.op("assign");
            Expression value=f.integer(op,"leaf",1,Operand.Role.VALUE_READ);
            for(int i=0;i<n;i++) value=new Expressions.Unary(f.operand(op,"neg"+i,Operand.Role.VALUE_READ),Expressions.UnaryOperator.NEG,value);
            f.linear(f.assign("assign",x,value));
            ValidationResult r=AirValidator.validate(f.build());
            require(r.isStructurallyValid(),"CORE-SIZE nesting "+n+": "+r.status());
            require(r.statistics().operands()==n+2,"complete deep operand inventory");
        }
    }
    static void retention() {
        Fixtures f=new Fixtures();
        f.capabilities.add(new Capabilities.Capability("AIR-test-profile","2"));
        f.origins.clear(); // Errors occur AFTER the retained profile obligation(s).
        ObjectId x=f.object("x",Types.known(Types.Builtin.TEXT));
        f.linear(f.assign("a",x,f.text(f.op("a"),"value","ok")));
        ValidationResult r=AirValidator.validate(f.build(),new ValidationOptions(128,1000,1));
        require(r.status()==ValidationResult.Status.INVALID_IR,"late invalidity after retained obligation");
        require(r.statistics().domainQueries()==1,"retention must not stop operations");
    }
    static void operational() {
        Publication p=fixture("operations",8);
        ValidationResult bounded=AirValidator.validate(p,new ValidationOptions(1,1,1));
        require(bounded.status()==ValidationResult.Status.INCOMPLETE_VALIDATION,"exhaustion cannot succeed");
        require(!bounded.diagnostics().traversalCompleted(),"exhaustion must mark incomplete work");
        require(bounded.hasIssues(ValidationIssue.Kind.RESOURCE_LIMIT),"exhaustion kind");
        require(!bounded.hasIssues(ValidationIssue.Kind.UNSUPPORTED_CAPABILITY),"exhaustion is not unsupported");
        require(bounded.issues().stream().anyMatch(i -> i.kind()==ValidationIssue.Kind.RESOURCE_LIMIT),"retained resource marker");
        ValidationContext c=new ValidationContext(p,ValidationOptions.defaults()); c.domainQueries=Long.MAX_VALUE;
        try { c.query(); throw new AssertionError("query overflow not guarded"); }
        catch(ValidationContext.Limit expected) { require(c.domainQueries==Long.MAX_VALUE,"no overflow wrap"); }
        c.issueCounts.put(ValidationIssue.Kind.INVALID_IR,Long.MAX_VALUE);
        try { c.error("I-02",p.id(),"overflow"); throw new AssertionError("diagnostic overflow not guarded"); }
        catch(ValidationContext.Limit expected) { require(c.issueCounts.get(ValidationIssue.Kind.INVALID_IR)==Long.MAX_VALUE,"no diagnostic wrap"); }
        Publication unknownVersion=new Publication(p.id(),new SemanticVersion(java.math.BigInteger.ONE,java.math.BigInteger.ZERO,java.math.BigInteger.ZERO),
                p.capabilities(),p.artifacts(),p.units(),p.storage(),p.resources(),p.artifactRelations(),p.origins(),p.coverage(),p.uncertainties(),p.premises());
        ValidationResult skipped=AirValidator.validate(unknownVersion);
        require(!skipped.diagnostics().traversalCompleted(),"unsupported version was not traversed");
        require(skipped.hasIssues(ValidationIssue.Kind.UNSUPPORTED_CAPABILITY) && !skipped.hasIssues(ValidationIssue.Kind.RESOURCE_LIMIT),"coverage is not operational exhaustion");
        var legacy=new ValidationResult(List.of(),new ValidationResult.Statistics(0,0,0,0));
        require(legacy.isStructurallyValid() && legacy.diagnostics().traversalCompleted(),"legacy constructor");
        var unfinished=new ValidationResult(List.of(),legacy.statistics(),new ValidationResult.Diagnostics(Map.of(),false));
        require(!unfinished.isStructurallyValid(),"missing traversal completion must not succeed");
    }
    static void omittedKinds() {
        Fixtures f=new Fixtures(); f.sequence("s",List.of(),f.halt("h"));
        f.capabilities.add(new Capabilities.Capability("AIR-profile","2"));
        f.capabilities.add(new Capabilities.Capability("vendor.unsupported","1"));
        ValidationResult r=AirValidator.validate(f.build(),new ValidationOptions(128,1000,1));
        require(r.issues().size()==1 && r.issues().get(0).kind()==ValidationIssue.Kind.SEMANTIC_OBLIGATION,"prefix retained");
        require(r.hasIssues(ValidationIssue.Kind.UNSUPPORTED_CAPABILITY),"unretained unsupported counted");
        require(r.status()==ValidationResult.Status.INCOMPLETE_VALIDATION,"unretained unsupported classification");
        require(r.diagnostics().traversalCompleted(),"unsupported check still completes work");
        require(r.diagnostics().count(ValidationIssue.Kind.SEMANTIC_OBLIGATION)==2,"all obligations counted");
        try { r.diagnostics().counts().clear(); throw new AssertionError("mutable counters"); }
        catch(UnsupportedOperationException expected) { /* immutable summary */ }
    }
    static void recursiveFamilies() {
        for(int n:new int[]{256,512,1024}) {
            Fixtures f=new Fixtures();
            Types.TypeRef unknown=new Types.UnknownType(f.uncertainty("t","TYPE_UNKNOWN"));
            ObjectId x=f.object("x",unknown); OperationId op=f.op("a");
            Place place=f.place(op,"leaf",x,Operand.Role.VALUE_READ);
            for(int i=0;i<n;i++) place=new Places.Choice(f.operand(op,"choice"+i,Operand.Role.VALUE_READ),List.of(place),Scopes.NoMemory.INSTANCE,unknown);
            var read=new Expressions.Read(f.operand(op,"read",Operand.Role.VALUE_READ),place);
            f.linear(f.assign("a",x,read));
            require(AirValidator.validate(f.build()).isStructurallyValid(),"postorder choice normalization "+n);
            Proofs.DomainProofScope scope=Proofs.PublicationDomain.INSTANCE;
            for(int i=0;i<n;i++) scope=new Proofs.Intersection(scope,Proofs.PublicationDomain.INSTANCE);
            f.proof("all",new Proofs.OperandDomain(place.header().id()),new Proofs.ObjectDomain(x),scope);
            var proofResult=AirValidator.validate(f.build());
            require(proofResult.isStructurallyValid(),"iterative universal expansion and scope "+n);
            require(proofResult.hasIssues(ValidationIssue.Kind.SEMANTIC_OBLIGATION),"premise obligation preserved");
            var limited=AirValidator.validate(f.build(),new ValidationOptions(128,100000,1));
            require(limited.hasIssues(ValidationIssue.Kind.RESOURCE_LIMIT),"opt-in semantic nesting budget");

            Fixtures bindings=new Fixtures(); ObjectId object=bindings.object("x",Types.known(Types.Builtin.TEXT));
            var declaration=bindings.objects.get(0); Memory.Binding binding=declaration.storage();
            for(int i=0;i<n;i++) binding=new Memory.AlternativesBinding(List.of(binding),Scopes.NoMemory.INSTANCE);
            bindings.objects.set(0,new Memory.ObjectDeclaration(object,declaration.displayName(),declaration.typeRef(),binding,
                    declaration.visibility(),declaration.origin(),declaration.coverage(),declaration.precision()));
            bindings.sequence("s",List.of(),bindings.halt("h"));
            require(AirValidator.validate(bindings.build()).isStructurallyValid(),"iterative bindings "+n);

            Publication p=fixture("objects",1); var c=new ValidationContext(p,ValidationOptions.defaults()); c.index.build();
            var refs=new ReferenceChecks(c); Scopes.MemoryScope memory=new Scopes.AllMemory(p.id(),true);
            Scopes.ControlScope control=new Scopes.AllControl(p.id());
            for(int i=0;i<n;i++) { memory=new Scopes.MemoryUnion(List.of(memory)); control=new Scopes.ControlUnion(List.of(control)); }
            refs.memory(memory,p.id(),0); refs.controlScope(control,p.units().get(0).id(),p.id(),0);
            require(c.issues.isEmpty(),"iterative memory/control scopes "+n);
        }
    }
    static void series() {
        for(int n:new int[]{32,64,128}) for(String dimension:List.of("objects","sequences","operations","references")) {
            Publication p=fixture(dimension,n); ValidationResult r=AirValidator.validate(p);
            require(r.status()==ValidationResult.Status.STRUCTURALLY_VALID,"CORE-SIZE "+dimension+" "+n);
            int operations=dimension.equals("sequences")?n:dimension.equals("operations")?n+1:1;
            require(r.statistics().operations()==operations,"full operations");
            if(dimension.equals("objects")) require(p.storage().size()==n,"storage cardinality");
            System.out.println("CAPACITY dimension="+dimension+" n="+n+" status="+r.status()+" entities="+r.statistics().entities()+" operations="+r.statistics().operations()+" operands="+r.statistics().operands()+" queries="+r.statistics().domainQueries());
        }
    }
    static Publication fixture(String dimension,int n) {
        Fixtures f=new Fixtures();
        if(dimension.equals("objects")) for(int i=0;i<n;i++) f.object("x"+i,Types.known(Types.Builtin.TEXT));
        if(dimension.equals("sequences")) for(int i=0;i<n;i++) f.sequence("s"+i,List.of(),f.halt("h"+i));
        else if(dimension.equals("operations")) {
            ObjectId x=f.object("x",Types.known(Types.Builtin.TEXT)); List<Instruction> operations=new ArrayList<>();
            for(int i=0;i<n;i++) operations.add(f.assign("a"+i,x,f.text(f.op("a"+i),"v","text")));
            f.sequence("s",operations,f.halt("h"));
        } else f.sequence("s",List.of(),f.halt("h"));
        Publication p=f.build();
        if(!dimension.equals("references")) return p;
        Evidence.Coverage coverage=new Evidence.Coverage(Evidence.InventoryStatus.COMPLETE,
                new Scopes.EntityScope(Collections.nCopies(n,p.id())),List.of(),List.of());
        return new Publication(p.id(),p.airVersion(),p.capabilities(),p.artifacts(),p.units(),p.storage(),p.resources(),p.artifactRelations(),p.origins(),coverage,p.uncertainties(),p.premises());
    }
    static void indexedReferences() {
        Publication p=fixture("references",64); ValidationContext c=new ValidationContext(p,ValidationOptions.defaults()); c.index.build();
        Set<Id> delegate=c.index.identities;
        var probe=new AbstractSet<Id>() {
            int lookups;
            public int size() { return delegate.size(); }
            public boolean contains(Object id) { lookups++; return delegate.contains(id); }
            public Iterator<Id> iterator() { throw new AssertionError("global inventory traversal per reference"); }
        };
        try {
            var field=PublicationIndex.class.getDeclaredField("identities"); field.setAccessible(true); field.set(c.index,probe);
        } catch(ReflectiveOperationException e) { throw new AssertionError(e); }
        new ReferenceChecks(c).coverage(p.coverage(),p.id());
        require(probe.lookups==64,"one indexed lookup per reference");
    }
    static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    /** Dedicated real-publication probe; not run by ordinary Maven builds. */
    public static void main(String[] args) {
        if(args.length==0) { nesting(); retention(); operational(); omittedKinds(); recursiveFamilies(); indexedReferences(); series(); return; }
        int n=Integer.parseInt(args[0]); PublicationId id=new PublicationId("capacity");
        List<Origins.Artifact> artifacts=new ArrayList<>();
        for(int i=0;i<n;i++) artifacts.add(new Origins.Artifact(new ArtifactId(id,"a"+i),"source",Optional.empty()));
        Publication p=new Publication(id,SemanticVersion.AIR_2_0_0,new Capabilities.Manifest(List.of(),List.of()),artifacts,List.of(),List.of(),List.of(),List.of(),List.of(),
                new Evidence.Coverage(Evidence.InventoryStatus.COMPLETE,new Scopes.PublicationScope(id),List.of(),List.of()),List.of(),List.of());
        ValidationResult r=AirValidator.validate(p);
        require(r.status()==ValidationResult.Status.STRUCTURALLY_VALID,"CORE-SIZE entities "+n+" "+r.status());
        require(r.statistics().entities()==n+1,"all entities indexed");
        System.out.println("CAPACITY dimension=entities n="+n+" entities="+r.statistics().entities()+" status="+r.status());
    }
}
