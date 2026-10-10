package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.model.Proofs.*;
import java.util.*;

/** Unused declarations are not a second proof-graph inventory; queries remain exact. */
final class DomainRetentionChecks {
    private DomainRetentionChecks() { }

    static void isolatedSubjectsAreRegisteredOnlyByRelationsOrQueries() {
        var f=new Fixtures();var reason=f.uncertainty("unknown-type","TYPE_UNKNOWN");
        var unknown=new Types.UnknownType(reason);var cold=new ArrayList<ObjectId>();
        for(int n=0;n<128;n++)cold.add(isolated(f,"unused-"+"x".repeat(4096)+n,unknown,reason));
        var text=isolated(f,"text",Fixtures.known(Types.Builtin.TEXT),reason);
        var otherText=isolated(f,"other-text",Fixtures.known(Types.Builtin.TEXT),reason);
        var integer=isolated(f,"integer",Fixtures.known(Types.Builtin.INT),reason);
        var cell=f.object("cell",unknown);var alias=f.alias("alias",cell,unknown);
        f.sequence("start",List.of(),f.halt("stop"));
        var context=new ValidationContext(f.build(),ValidationOptions.defaults());context.index.build();
        var domains=new DomainProofEngine(context,new TypeResolver(context));domains.initialize();
        var registered=registered(domains);
        for(var id:cold)eq(false,registered.contains(new ObjectDomain(id)));
        for(var id:List.of(text,otherText,integer))eq(false,registered.contains(new ObjectDomain(id)));
        var site=new OperationSite(f.op("stop"));
        eq(true,domains.same(new ObjectDomain(cell),new ObjectDomain(alias),site));
        eq(false,domains.same(new ObjectDomain(cold.get(0)),new ObjectDomain(cold.get(1)),site));
        eq(true,domains.same(new ObjectDomain(cold.get(0)),new ObjectDomain(cold.get(0)),site));
        eq(true,domains.same(new ObjectDomain(text),new ObjectDomain(otherText),site));
        eq(false,domains.same(new ObjectDomain(text),new ObjectDomain(integer),site));
        for(int n=2;n<cold.size();n++)eq(false,registered.contains(new ObjectDomain(cold.get(n))));
        eq(List.of(),context.issues); // Shared unknown_type did not invent a relation.
        var resident=AirValidator.validate(f.build());eq(ValidationResult.Status.STRUCTURALLY_VALID,resident.status());
        try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(f.build()),ValidationOptions.defaults(),new SnapshotValidatorChecks.Stores())) {
            eq(resident,checked.result());
        }
        lateRegistrationPreservesScopedProofs();
    }

    private static void lateRegistrationPreservesScopedProofs() {
        var f=new Fixtures();var reason=f.uncertainty("type","TYPE_UNKNOWN");var unknown=new Types.UnknownType(reason);
        var left=isolated(f,"left",unknown,reason);var unrelated=isolated(f,"unrelated",unknown,reason);
        var expected=isolated(f,"expected",Fixtures.known(Types.Builtin.TEXT),reason);
        f.sequence("start",List.of(),f.halt("stop"));f.sequence("other",List.of(),f.halt("elsewhere"));
        f.proof("local",new ObjectDomain(left),new ObjectDomain(expected),new OperationDomain(f.op("stop")));
        var fresh=new ArrayList<ObjectId>();for(int n=0;n<32;n++)fresh.add(isolated(f,"fresh-"+n,Fixtures.known(Types.Builtin.TEXT),reason));
        var different=isolated(f,"different",Fixtures.known(Types.Builtin.INT),reason);
        var context=new ValidationContext(f.build(),ValidationOptions.defaults());context.index.build();
        lateRegistrationQueries(context,f,left,unrelated,different,fresh);
        try(var snapshot=AirSnapshot.fromPublication(f.build());
            var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());
            var declarations=SnapshotDeclarations.build(snapshot,keys,new SnapshotDeclarationChecks.Store(),Long.MAX_VALUE,Long.MAX_VALUE,(r,i,n)->{throw new AssertionError(r);});
            var visible=SnapshotVisibleObjects.build(snapshot,keys,new SnapshotVisibleChecks.Store())) {
            SnapshotNominalCycles.scan(snapshot,keys,declarations,new SnapshotCycleChecks.Store(),(r,i,n)->{throw new AssertionError(r);});
            var nativeContext=new ValidationContext(SnapshotValidationProgram.afterPrimitiveAdmission(snapshot,declarations,visible),ValidationOptions.defaults());nativeContext.index.build();
            lateRegistrationQueries(nativeContext,f,left,unrelated,different,fresh);
        }
    }
    private static void lateRegistrationQueries(ValidationContext context,Fixtures f,ObjectId left,ObjectId unrelated,ObjectId different,List<ObjectId> fresh) {
        var domains=new DomainProofEngine(context,new TypeResolver(context));domains.initialize();
        var local=new OperationSite(f.op("stop"));var outside=new OperationSite(f.op("elsewhere"));
        for(var id:fresh) {
            eq(true,domains.same(new ObjectDomain(left),new ObjectDomain(id),local));
            eq(false,domains.same(new ObjectDomain(left),new ObjectDomain(id),outside));
        }
        eq(false,domains.same(new ObjectDomain(left),new ObjectDomain(different),local));
        eq(false,domains.same(new ObjectDomain(left),new ObjectDomain(unrelated),local));
        eq(1,context.issues.size());eq(ValidationIssue.Kind.SEMANTIC_OBLIGATION,context.issues.getFirst().kind());
        eq("I-53",context.issues.getFirst().rule()); // Scope/authority did not become a global fact.
    }

    static void nativeVisibilityBorrowsTheCompletePrimitiveRelation() {
        var f=new Fixtures();var reason=f.uncertainty("memory","MEMORY_UNKNOWN");
        var own=isolated(f,"own-"+"x".repeat(8192),Fixtures.known(Types.Builtin.INT),reason);
        f.sequence("start",List.of(),f.halt("stop"));var publication=f.build();
        try(var snapshot=AirSnapshot.fromPublication(publication);
            var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());
            var declarations=SnapshotDeclarations.build(snapshot,keys,new SnapshotDeclarationChecks.Store(),
                Long.MAX_VALUE,Long.MAX_VALUE,(r,i,n)->{throw new AssertionError(r);});
            var visible=SnapshotVisibleObjects.build(snapshot,keys,new SnapshotVisibleChecks.Store())) {
            eq(1L,visible.counts().distinct());
            SnapshotNominalCycles.scan(snapshot,keys,declarations,new SnapshotCycleChecks.Store(),
                (r,i,n)->{throw new AssertionError(r);});
            var program=SnapshotValidationProgram.afterPrimitiveAdmission(snapshot,declarations,visible);
            var context=new ValidationContext(program,ValidationOptions.defaults());context.index.build();
            var refs=new ReferenceChecks(context);refs.run();
            eq(0,refs.visible.size()); // No typed UnitId/ObjectId inventory after primitive admission.
            eq(true,refs.objectVisible(f.unit,own));
            eq(false,refs.objectVisible(new UnitId(f.pub,"other"),own));
            eq(false,refs.objectVisible(f.unit,new ObjectId(new UnitId(new PublicationId("foreign"),f.unit.localId()),own.localId())));
            eq(List.of(),context.issues);
            eq(true,context.index.objects.containsKey(own));
        }
    }

    private static ObjectId isolated(Fixtures f,String local,Types.TypeRef type,UncertaintyId reason) {
        var id=new ObjectId(f.unit,local);
        f.objects.add(new Memory.ObjectDeclaration(id,Optional.empty(),type,
            new Memory.UnknownBinding(new Scopes.VisibleMemory(f.unit,true),reason),Memory.Visibility.PRIVATE,
            f.origin,Evidence.CoverageStatus.MODELED,f.precision()));
        return id;
    }
    static void nativeTypeAndProofKeysBorrowCompleteOperandOwners() {
        var f=new Fixtures();var text=f.object("text",Fixtures.known(Types.Builtin.TEXT));var integer=f.object("integer",Fixtures.known(Types.Builtin.INT));
        var instructions=new ArrayList<Instruction>();
        for(int at=0;at<16;at++) {
            String name="cold-operation-owner-"+"x".repeat(4096)+"/"+at;var id=f.op(name);
            Expression value=at%2==0?f.text(id,"value","TEXT"):f.integer(id,"value",7,Operand.Role.VALUE_READ);
            instructions.add(f.assign(name,at%2==0?text:integer,value));
        }
        f.sequence("start",instructions,f.halt("stop"));var publication=f.build();var expected=AirValidator.validate(publication);
        eq(ValidationResult.Status.STRUCTURALLY_VALID,expected.status());eq(List.of(),expected.issues());
        TypeResolver borrowedTypes;DomainProofEngine borrowedDomains;
        try(var snapshot=AirSnapshot.fromPublication(publication);
            var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());
            var declarations=SnapshotDeclarations.build(snapshot,keys,new SnapshotDeclarationChecks.Store(),Long.MAX_VALUE,Long.MAX_VALUE,(r,i,n)->{throw new AssertionError(r);});
            var visible=SnapshotVisibleObjects.build(snapshot,keys,new SnapshotVisibleChecks.Store())) {
            SnapshotNominalCycles.scan(snapshot,keys,declarations,new SnapshotCycleChecks.Store(),(r,i,n)->{throw new AssertionError(r);});
            var program=SnapshotValidationProgram.afterPrimitiveAdmission(snapshot,declarations,visible);
            var context=new ValidationContext(program,ValidationOptions.defaults());context.index.build();new ReferenceChecks(context).run();
            borrowedTypes=new TypeResolver(context);
            for(var operand:context.index.operands.values())borrowedTypes.type(operand);
            eq(0,retainedOwnerTexts(borrowedTypes.types));
            borrowedDomains=new DomainProofEngine(context,borrowedTypes);borrowedDomains.initialize();
            var first=(Operations.Assign)instructions.get(0);var second=(Operations.Assign)instructions.get(1);
            var site=new OperationSite(first.header().id());
            eq(true,borrowedDomains.same(new OperandDomain(first.destination().header().id()),new OperandDomain(first.value().header().id()),site));
            eq(false,borrowedDomains.same(new OperandDomain(first.destination().header().id()),new OperandDomain(second.value().header().id()),site));
            for(var instruction:instructions) {
                var assign=(Operations.Assign)instruction;
                eq(true,borrowedDomains.same(new OperandDomain(assign.destination().header().id()),new OperandDomain(assign.value().header().id()),new OperationSite(assign.header().id())));
            }
            for(var field:DomainProofEngine.class.getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers())&&!Set.of("c","types","scopes").contains(field.getName())) {
                try{field.setAccessible(true);eq(0,retainedOwnerTexts(field.get(borrowedDomains)));}catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
            }
            var missingA=new OperandId(new OperationOwner(first.header().id()),"Aa");var missingB=new OperandId(new OperationOwner(first.header().id()),"BB");
            eq(missingA.hashCode(),missingB.hashCode());eq(false,missingA.equals(missingB));
            eq(false,borrowedDomains.same(new OperandDomain(missingA),new OperandDomain(missingB),site));
            eq(true,borrowedDomains.same(new OperandDomain(missingA),new OperandDomain(missingA),site));
            var foreign=new OperandId(new OperationOwner(new OperationId(new UnitId(f.pub,"foreign-unit"),first.header().id().localId())),"Aa");
            eq(false,borrowedDomains.same(new OperandDomain(missingA),new OperandDomain(foreign),site));
            eq(List.of(),context.issues);eq(expected,AirValidator.validate(program,ValidationOptions.defaults()));
        }
        var types=borrowedTypes;var domains=borrowedDomains;var first=(Operations.Assign)instructions.getFirst();
        expired(()->types.type(first.value()));expired(()->domains.same(new OperandDomain(first.destination().header().id()),new OperandDomain(first.value().header().id()),new OperationSite(first.header().id())));
    }
    private static void expired(Runnable action){try{action.run();}catch(IllegalStateException expected){return;}throw new AssertionError("expired admission accepted");}
    /** Inspect stored keys/rows only,never expand borrowed ValidationContext/source inventories. */
    private static int retainedOwnerTexts(Object root) {
        var pending=new ArrayDeque<Object>();if(root!=null)pending.add(root);var seen=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());int count=0;
        while(!pending.isEmpty()) {
            var value=pending.removeFirst();if(!seen.add(value))continue;
            if(value instanceof String text&&text.startsWith("cold-operation-owner-")){count++;continue;}
            if(value instanceof Optional<?> optional){optional.ifPresent(pending::add);continue;}
            if(value instanceof Map<?,?> map){for(var entry:map.entrySet()){if(entry.getKey()!=null)pending.add(entry.getKey());if(entry.getValue()!=null)pending.add(entry.getValue());}continue;}
            if(value instanceof Iterable<?> items){for(var item:items)if(item!=null)pending.add(item);continue;}
            for(var owner=value.getClass();owner!=null&&owner.getName().startsWith("io.github.gustavo2358.");owner=owner.getSuperclass())
                for(var field:owner.getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers())&&!field.getType().isPrimitive()) {
                    try{field.setAccessible(true);var item=field.get(value);if(item!=null)pending.add(item);}catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
                }
        }
        return count;
    }
    private static Set<?> registered(DomainProofEngine engine) {
        try {var field=DomainProofEngine.class.getDeclaredField("registered");field.setAccessible(true);
            return (Set<?>)field.get(engine);
        } catch(ReflectiveOperationException failure) {throw new AssertionError(failure);}
    }
    private static void eq(Object expected,Object actual) {
        if(!Objects.equals(expected,actual))throw new AssertionError("expected="+expected+" actual="+actual);
    }
}
