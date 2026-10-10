package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;

/** Independent nominal residual sets/cost laws; no complete Validator certificate. */
final class SnapshotCycleChecks {
    private SnapshotCycleChecks() { }
    static void allNominalFamiliesPreserveCycleDependentResiduals() {
        var f=base();OriginId a=new OriginId(f.pub,"a"),b=new OriginId(f.pub,"b"),tail=new OriginId(f.pub,"tail"),absent=new OriginId(f.pub,"absent");
        f.origins.add(new Origins.Derived(a,List.of(b,b),"r"));f.origins.add(new Origins.Derived(b,List.of(a),"r"));f.origins.add(new Origins.Derived(tail,List.of(a),"r"));
        f.origins.add(new Origins.Derived(new OriginId(f.pub,"multi"),List.of(f.origin,a),"r"));
        f.origins.add(new Origins.Derived(new OriginId(f.pub,"dangling"),List.of(absent),"r"));
        ObjectId x=new ObjectId(f.unit,"x"),y=new ObjectId(f.unit,"y");f.alias("x",y,Fixtures.known(Types.Builtin.TEXT));f.alias("y",x,Fixtures.known(Types.Builtin.TEXT));f.alias("z",x,Fixtures.known(Types.Builtin.TEXT));
        // Alternatives containing aliases are not edges of the resident direct-alias cycle rule.
        f.objects.add(new Memory.ObjectDeclaration(new ObjectId(f.unit,"alternatives"),Optional.empty(),Fixtures.known(Types.Builtin.TEXT),new Memory.AlternativesBinding(List.of(new Memory.AliasBinding(x)),Scopes.NoMemory.INSTANCE),Memory.Visibility.PRIVATE,f.origin,Evidence.CoverageStatus.MODELED,f.precision()));
        var p=f.build();UnitId u=new UnitId(f.pub,"U"),v=new UnitId(f.pub,"V"),dependent=new UnitId(f.pub,"dependent");
        var units=new ArrayList<Unit>(p.units());units.add(unavailable(f,u,v));units.add(unavailable(f,v,u));units.add(unavailable(f,dependent,u));
        p=Fixtures.withUnits(p,units);
        var expected=List.of("ORIGIN:a","ORIGIN:b","ORIGIN:tail","ORIGIN:multi","UNIT:U","UNIT:V","UNIT:dependent","ALIAS:x","ALIAS:y","ALIAS:z");
        for(boolean reverse:new boolean[]{false,true}) {
            Publication input=p;
            if(reverse){var origins=new ArrayList<>(p.origins());Collections.reverse(origins);var order=new ArrayList<>(p.units());Collections.reverse(order);input=new Publication(p.id(),p.airVersion(),p.capabilities(),p.artifacts(),order,p.storage(),p.resources(),p.artifactRelations(),origins,p.coverage(),p.uncertainties(),p.premises());}
            var port=new Store();var actual=new ArrayList<String>();
            try(var snapshot=AirSnapshot.fromPublication(input);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());
                var declarations=SnapshotDeclarations.build(snapshot,keys,new SnapshotDeclarationChecks.Store(),1000,1000,(r,id,n)->{throw new AssertionError(r);})) {
                var counts=SnapshotNominalCycles.scan(snapshot,keys,declarations,port,(rule,id,node)->actual.add(rule+":"+local(snapshot,id)));
                eq(10L,counts.residual());eq(counts.nodes()-10,counts.removed());eq(0,port.pending.size());eq(true,port.edges>0);
                eq(new HashSet<>(expected),new HashSet<>(actual));eq(expected.size(),actual.size());
                if(!reverse)eq(expected,actual);
                eq(AirShape.PUBLICATION,snapshot.shape(snapshot.root()));eq(true,declarations.entities()>counts.nodes());
            }
            eq(0L,port.claimed);eq(true,port.closed);
        }
        var self=base();OriginId id=new OriginId(self.pub,"self");self.origins.add(new Origins.Derived(id,List.of(id),"r"));var actual=new ArrayList<String>();
        run(self.build(),new Store(),(rule,identity,node)->actual.add(rule.toString()));eq(List.of("ORIGIN"),actual);
    }
    static void chainDiamondRepeatedAndMissingParentsHaveLinearPrimitiveWork() {
        for(int size:new int[]{16,64,256,1024,4096}) {
            var f=base();OriginId previous=f.origin;
            for(int n=0;n<size;n++){OriginId id=new OriginId(f.pub,"n"+n);f.origins.add(new Origins.Derived(id,List.of(previous,previous),"r"));previous=id;}
            f.origins.add(new Origins.Derived(new OriginId(f.pub,"missing"),List.of(new OriginId(f.pub,"not-declared")),"r"));
            var port=new Store();var counts=run(f.build(),port,(r,i,n)->{throw new AssertionError(r);});
            eq(size+3L,counts.nodes());eq(2L*size,counts.edges());eq(counts.nodes(),counts.removed());eq(0L,counts.residual());
            eq(counts.edges(),port.decrements);eq(counts.nodes(),port.removes);eq(counts.nodes(),port.defines);eq(counts.edges(),port.edges);
            eq(true,port.requests<=16*(counts.nodes()+counts.edges()+1));
            System.out.println("SNAPSHOT_NOMINAL_CYCLE_COST nodes="+counts.nodes()+" edges="+counts.edges()+" decrements="+port.decrements+" requests="+port.requests);
        }
        var f=base();OriginId a=new OriginId(f.pub,"a"),b=new OriginId(f.pub,"b"),join=new OriginId(f.pub,"join");
        f.origins.add(new Origins.Derived(a,List.of(f.origin),"r"));f.origins.add(new Origins.Derived(b,List.of(f.origin),"r"));f.origins.add(new Origins.Derived(join,List.of(a,b),"r"));
        eq(0L,run(f.build(),new Store(),(r,i,n)->{throw new AssertionError(r);}).residual());
    }
    static void nominalIdentityUsesCompleteKeysAndSparseHandles() {
        var s=new SnapshotGraphChecks.Source();var f=new SnapshotGraphChecks.Fixture(s);
        long a=s.record(AirShape.IDS_ORIGIN_ID,f.id,s.text("same"));
        long equal=s.record(AirShape.IDS_ORIGIN_ID,s.record(AirShape.IDS_PUBLICATION_ID,s.text("P")),s.text("same"));
        long foreign=s.record(AirShape.IDS_ORIGIN_ID,s.record(AirShape.IDS_PUBLICATION_ID,s.text("other")),s.text("same"));
        long node=s.record(AirShape.ORIGINS_DERIVED,a,s.list(equal,foreign),s.text("r"));s.replaceField(f.root,8,s.list(node));
        var port=new Store();
        try(var snapshot=AirSnapshot.attach(s,f.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());
            var declarations=SnapshotDeclarations.build(snapshot,keys,new SnapshotDeclarationChecks.Store(),1000,1000,(r,id,n)->{throw new AssertionError(r);})) {
            var actual=new ArrayList<Long>();var counts=SnapshotNominalCycles.scan(snapshot,keys,declarations,port,(rule,id,n)->{eq(SnapshotNominalCycles.Rule.ORIGIN,rule);actual.add(id);eq(node,n);});
            eq(List.of(a),actual);eq(1L,counts.nodes());eq(1L,counts.edges());eq(1L,counts.residual());eq(0,s.indexedListReads);eq(0,s.activeCursors);
        }
    }
    static void everyOperationalFailureClosesScratchWithoutOwningBorrowedIndices() {
        var f=base();OriginId a=new OriginId(f.pub,"a");f.origins.add(new Origins.Derived(a,List.of(f.origin),"r"));Publication input=f.build();
        var measured=new Store();run(input,measured,(r,id,n)->{throw new AssertionError(r);});
        for(int remaining=0;remaining<measured.requests;remaining++) {
            var port=new Store();port.remaining=remaining;
            try(var snapshot=AirSnapshot.fromPublication(input);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());
                var declarations=SnapshotDeclarations.build(snapshot,keys,new SnapshotDeclarationChecks.Store(),1000,1000,(r,id,n)->{throw new AssertionError(r);})) {
                eq(port.failure,fails(IllegalStateException.class,()->SnapshotNominalCycles.scan(snapshot,keys,declarations,port,(r,id,n)->{})));
                eq(AirShape.PUBLICATION,snapshot.shape(snapshot.root()));eq(true,declarations.entities()>0);eq(true,keys.key(snapshot.field(snapshot.root(),AirShape.PUBLICATION,0))>0);
            }
            eq(0L,port.claimed);eq(true,port.closed);
        }
        var cyclic=base();OriginId id=new OriginId(cyclic.pub,"self");cyclic.origins.add(new Origins.Derived(id,List.of(id),"r"));var port=new Store();port.closeFailure=true;port.leaseFailure=true;var primary=new IllegalStateException("issue sink failure");
        eq(primary,fails(IllegalStateException.class,()->run(cyclic.build(),port,(r,i,n)->{throw primary;})));eq(2,primary.getSuppressed().length);eq(0L,port.claimed);
        var denied=new Store();denied.deny=true;eq(denied.failure,fails(IllegalStateException.class,()->run(input,denied,(r,i,n)->{})));eq(true,denied.closed);eq(0L,denied.claimed);
        var source=new SnapshotGraphChecks.Source();var fixture=new SnapshotGraphChecks.Fixture(source);
        long origin=source.record(AirShape.IDS_ORIGIN_ID,fixture.id,source.text("o")),reference=source.record(AirShape.IDS_ORIGIN_ID,fixture.id,source.text("o"));
        long parents=source.list(reference),derived=source.record(AirShape.ORIGINS_DERIVED,origin,parents,source.text("r"));source.replaceField(fixture.root,8,source.list(derived));var scratch=new Store();
        try(var snapshot=AirSnapshot.attach(source,fixture.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());
            var declarations=SnapshotDeclarations.build(snapshot,keys,new SnapshotDeclarationChecks.Store(),1000,1000,(r,i,n)->{throw new AssertionError(r);})) {
            source.failNode=reference;source.failingCursor=parents;source.closeCursorFailure=true;
            eq(source.failure,fails(IllegalStateException.class,()->SnapshotNominalCycles.scan(snapshot,keys,declarations,scratch,(r,i,n)->{})));
            eq(1,source.failure.getSuppressed().length);eq(source.cleanup,source.failure.getSuppressed()[0]);eq(0,source.activeCursors);
            eq(AirShape.PUBLICATION,snapshot.shape(fixture.root));eq(2L,declarations.entities());
        }
        eq(true,scratch.closed);eq(0L,scratch.claimed);
    }
    private static Fixtures base(){var f=new Fixtures();f.linear(new Operations.Nop(f.header(f.op("nop"))));return f;}
    private static Unit unavailable(Fixtures f,UnitId id,UnitId parent){return new Unit(id,Optional.of(parent),List.of(),List.of(),List.of(),List.of(),List.of(),Unit.BodyAvailability.UNAVAILABLE,Optional.of(new UncertaintyId(f.pub,"reason")),f.coverage(new Scopes.UnitScope(id)),f.origin);}
    private static String local(AirSnapshot s,long id){long text=s.field(id,s.shape(id),1);char[] chars=new char[Math.toIntExact(s.characterCount(text))];s.readCharacters(text,0,chars,0,chars.length);return new String(chars);}
    private static SnapshotNominalCycles.Counts run(Publication p,Store port,SnapshotNominalCycles.Issues issues) {
        try(var snapshot=AirSnapshot.fromPublication(p);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());
            var declarations=SnapshotDeclarations.build(snapshot,keys,new SnapshotDeclarationChecks.Store(),100_000,1000,(r,id,n)->{throw new AssertionError(r);})) {
            return SnapshotNominalCycles.scan(snapshot,keys,declarations,port,issues);
        }
    }
    static final class Store implements SnapshotNominalCycles.Storage {
        final Map<Long,Long> degree=new HashMap<>();final Map<Long,ArrayList<Long>> reverse=new HashMap<>();final ArrayDeque<Long> pending=new ArrayDeque<>();
        Iterator<Long> children;long current,child,claimed,requests,remaining=Long.MAX_VALUE,defines,edges,decrements,removes;boolean closed,deny,closeFailure,leaseFailure;
        final IllegalStateException failure=new IllegalStateException("cycle scratch failure");
        private void work(){requests++;if(remaining--==0)throw failure;}
        public void define(long key){work();if(degree.putIfAbsent(key,0L)!=null)throw new AssertionError("duplicate cycle node");defines++;}
        public void link(long parent,long node){work();degree.compute(node,(k,v)->Math.incrementExact(Objects.requireNonNull(v)));reverse.computeIfAbsent(parent,k->new ArrayList<>()).add(node);edges++;}
        public long degree(long key){work();return Objects.requireNonNull(degree.get(key));}
        public long decrement(long key){work();long count=degree(key);if(count==0)throw new AssertionError("degree underflow");degree.put(key,--count);decrements++;return count;}
        public void enqueue(long key){work();pending.addLast(key);}
        public boolean advance(){work();Long next=pending.pollFirst();if(next==null)return false;current=next;removes++;return true;}
        public long node(){return current;}
        public void children(long key){work();children=reverse.getOrDefault(key,new ArrayList<>()).iterator();}
        public boolean advanceChild(){work();if(!children.hasNext())return false;child=children.next();return true;}
        public long child(){return child;}
        public AirSnapshotBuilder.Lease claim(long bytes){if(deny)throw failure;claimed+=bytes;return ()->{claimed-=bytes;if(leaseFailure)throw new IllegalStateException("cycle control cleanup");};}
        public void close(){if(closed)return;closed=true;degree.clear();reverse.clear();pending.clear();if(closeFailure)throw new IllegalStateException("cycle scratch cleanup");}
    }
    private static <T extends Throwable>T fails(Class<T> type,Runnable action){try{action.run();}catch(Throwable error){if(type.isInstance(error))return type.cast(error);throw new AssertionError(error);}throw new AssertionError("missing "+type);}
    private static void eq(Object expected,Object actual){if(!Objects.equals(expected,actual))throw new AssertionError("expected="+expected+" actual="+actual);}
}
