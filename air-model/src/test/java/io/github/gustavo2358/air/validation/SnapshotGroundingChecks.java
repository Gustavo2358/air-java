package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.math.BigInteger;
import java.util.*;

/** Manual grounding truth sets and independent finite reachability, separate from reference validity. */
final class SnapshotGroundingChecks {
    private SnapshotGroundingChecks() { }
    static void seededAndUnseededCyclesPreserveOrSemanticsAcrossInventoryOrder() {
        for(boolean reverse:new boolean[]{false,true}) {
            var f=base();ObjectId a=new ObjectId(f.unit,"a"),b=new ObjectId(f.unit,"b"),dead=new ObjectId(f.unit,"dead"),dead2=new ObjectId(f.unit,"dead2");
            f.alias("a",b,Fixtures.known(Types.Builtin.TEXT));
            add(f,"b",new Memory.AlternativesBinding(List.of(new Memory.AliasBinding(a),new Memory.CellBinding(new StorageId(f.pub,"absent-cell"))),Scopes.NoMemory.INSTANCE));
            f.alias("tail",a,Fixtures.known(Types.Builtin.TEXT));f.alias("dead",dead2,Fixtures.known(Types.Builtin.TEXT));f.alias("dead2",dead,Fixtures.known(Types.Builtin.TEXT));f.alias("dead-tail",dead,Fixtures.known(Types.Builtin.TEXT));
            add(f,"or",unknown(f,new Scopes.ObjectsMemory(List.of(dead,a))));
            add(f,"nonempty-storage",unknown(f,new Scopes.StorageMemory(List.of(new StorageId(f.pub,"absent-storage")))));
            add(f,"all",unknown(f,new Scopes.AllMemory(new PublicationId("absent"),false)));
            add(f,"visible",unknown(f,new Scopes.VisibleMemory(new UnitId(new PublicationId("absent"),"u"),false)));
            add(f,"view",new Memory.ViewBinding(new StorageId(f.pub,"absent-region"),BigInteger.ZERO,BigInteger.ZERO,Memory.IdentityBytes.INSTANCE));
            add(f,"remainder",new Memory.AlternativesBinding(List.of(new Memory.AliasBinding(dead)),new Scopes.WithinMemory(new Scopes.ObjectsMemory(List.of(a)))));
            add(f,"foreign",new Memory.AliasBinding(new ObjectId(new UnitId(new PublicationId("foreign"),f.unit.localId()),"a")));
            if(reverse)Collections.reverse(f.objects);
            var positive=Set.of("a","b","tail","or","nonempty-storage","all","visible","view","remainder");var port=new Store();
            try(var snapshot=AirSnapshot.fromPublication(f.build());var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());
                var declarations=declarations(snapshot,keys);var grounding=SnapshotGrounding.build(snapshot,declarations,port,new SnapshotGraphChecks.Store(),100_000,100_000)) {
                for(long at=0;at<declarations.entities();at++) {
                    long node=declarations.declaration(at,SnapshotDeclarations.Fact.NODE);
                    if(snapshot.shape(node)==AirShape.MEMORY_OBJECT_DECLARATION) {
                        long id=declarations.declaration(at,SnapshotDeclarations.Fact.IDENTITY);eq(positive.contains(local(snapshot,id)),grounding.groundedObject(id));
                        eq(grounding.groundedObject(id),grounding.groundedNode(node));
                    }
                }
                eq(true,port.frozen);long deadIdentity=objectIdentity(snapshot,declarations,"dead"),requests=port.requests;
                for(int q=0;q<1000;q++)eq(false,grounding.groundedObject(deadIdentity));
                eq(true,port.requests-requests<=1000); // Required lookup only; no cyclic equation replay.
                eq(AirShape.PUBLICATION,snapshot.shape(snapshot.root()));
            }
            eq(0L,port.claimed);eq(true,port.closed);
        }
    }
    static void independentTinyReachabilityOracleMatchesEveryObject() {
        var random=new Random(77109);
        for(int sample=0;sample<48;sample++) {
            var f=base();int size=16;boolean[] seeds=new boolean[size];int[][] links=new int[size][];
            for(int n=0;n<size;n++) {
                seeds[n]=random.nextInt(7)==0;int count=1+random.nextInt(3);links[n]=new int[count];var alternatives=new ArrayList<Memory.Binding>();
                for(int j=0;j<count;j++){links[n][j]=random.nextInt(size);alternatives.add(new Memory.AliasBinding(new ObjectId(f.unit,"n"+links[n][j])));}
                if(seeds[n])alternatives.add(new Memory.CellBinding(new StorageId(f.pub,"absent")));
                add(f,"n"+n,new Memory.AlternativesBinding(alternatives,Scopes.NoMemory.INSTANCE));
            }
            try(var snapshot=AirSnapshot.fromPublication(f.build());var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());
                var declarations=declarations(snapshot,keys);var grounding=SnapshotGrounding.build(snapshot,declarations,new Store(),new SnapshotGraphChecks.Store(),100_000,100_000)) {
                for(int n=0;n<size;n++) {
                    boolean found=false;var visited=new boolean[size];var pending=new ArrayDeque<Integer>();pending.add(n);
                    while(!pending.isEmpty()) {int next=pending.removeFirst();if(visited[next])continue;visited[next]=true;if(seeds[next]){found=true;break;}for(int target:links[next])pending.addLast(target);}
                    eq(found,grounding.groundedObject(objectIdentity(snapshot,declarations,"n"+n)));
                }
            }
        }
    }
    static void sharedCyclicQueriesAndDeepScopesHaveLinearEquationWork() {
        for(int size:new int[]{16,64,256,1024,4096}) {
            var f=base();
            for(int n=0;n<size;n++)f.alias("n"+n,new ObjectId(f.unit,"n"+((n+1)%size)),Fixtures.known(Types.Builtin.TEXT));
            var port=new Store();
            try(var snapshot=AirSnapshot.fromPublication(f.build());var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());
                var declarations=declarations(snapshot,keys);var grounding=SnapshotGrounding.build(snapshot,declarations,port,new SnapshotGraphChecks.Store(),100_000,100_000)) {
                eq(2L*size,grounding.counts().nodes());eq(2L*size,grounding.counts().edges());eq(0L,grounding.counts().grounded());eq(0L,port.propagations);
                long before=port.requests;
                for(long at=0;at<declarations.entities();at++)if(snapshot.shape(declarations.declaration(at,SnapshotDeclarations.Fact.NODE))==AirShape.MEMORY_OBJECT_DECLARATION) {
                    long id=declarations.declaration(at,SnapshotDeclarations.Fact.IDENTITY);for(int repeat=0;repeat<8;repeat++)eq(false,grounding.groundedObject(id));
                }
                eq(8L*size,port.requests-before);eq(true,port.requests<=40L*(size+1));
                System.out.println("SNAPSHOT_GROUNDING_COST objects="+size+" equations="+grounding.counts().nodes()+" edges="+grounding.counts().edges()+" requests="+port.requests);
            }
        }
        var f=base();Scopes.MemoryScope scope=new Scopes.StorageMemory(List.of(new StorageId(f.pub,"s")));
        for(int n=0;n<20_000;n++)scope=new Scopes.MemoryUnion(List.of(scope,scope)); // Shared DAG, no unfolding.
        add(f,"deep",unknown(f,scope));var port=new Store();
        try(var snapshot=AirSnapshot.fromPublication(f.build());var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());
            var declarations=declarations(snapshot,keys);var grounding=SnapshotGrounding.build(snapshot,declarations,port,new SnapshotGraphChecks.Store(),200_000,100_000)) {
            eq(true,grounding.groundedObject(objectIdentity(snapshot,declarations,"deep")));eq(20_003L,grounding.counts().nodes());eq(40_002L,grounding.counts().edges());eq(grounding.counts().nodes(),grounding.counts().grounded());
            eq(grounding.counts().grounded(),port.propagations);eq(true,port.expansions==grounding.counts().nodes());
        }
    }
    static void emptyTypedScopesAndClosedAlternativesRemainFalse() {
        var source=new SnapshotGraphChecks.Source();var fixture=new SnapshotGraphChecks.Fixture(source);
        long unitId=source.record(AirShape.IDS_UNIT_ID,fixture.id,source.text("u"));
        long originId=source.record(AirShape.IDS_ORIGIN_ID,fixture.id,source.text("o"));
        source.replaceField(fixture.root,8,source.list(source.record(AirShape.ORIGINS_UNAVAILABLE,originId,source.text("r"))));
        long reason=source.record(AirShape.IDS_UNCERTAINTY_ID,fixture.id,source.text("reason"));
        long claim=source.record(AirShape.EVIDENCE_CLAIM,source.record(AirShape.SCOPES_PUBLICATION_SCOPE,fixture.id),source.scalar(AirShape.EVIDENCE_PRECISION_STATUS,0),fixture.empty);
        long precision=source.record(AirShape.EVIDENCE_PRECISION,claim,claim,claim,claim,claim);
        long type=source.record(AirShape.TYPES_KNOWN,source.scalar(AirShape.TYPES_BUILTIN,Types.Builtin.TEXT.ordinal()));
        long none=source.scalar(AirShape.SCOPES_NO_MEMORY,0);
        long[] scopes={source.record(AirShape.SCOPES_OBJECTS_MEMORY,fixture.empty),source.record(AirShape.SCOPES_STORAGE_MEMORY,fixture.empty),source.record(AirShape.SCOPES_MEMORY_UNION,fixture.empty)};
        long[] objects=new long[4];
        for(int n=0;n<objects.length;n++) {
            long binding=n==3?source.record(AirShape.MEMORY_ALTERNATIVES_BINDING,fixture.empty,none):source.record(AirShape.MEMORY_UNKNOWN_BINDING,scopes[n],reason);
            objects[n]=source.record(AirShape.MEMORY_OBJECT_DECLARATION,source.record(AirShape.IDS_OBJECT_ID,unitId,source.text("n"+n)),source.optional(),type,binding,source.scalar(AirShape.MEMORY_VISIBILITY,0),originId,source.scalar(AirShape.EVIDENCE_COVERAGE_STATUS,0),precision);
        }
        long unit=source.record(AirShape.UNIT,unitId,source.optional(),source.list(objects),fixture.empty,fixture.empty,fixture.empty,fixture.empty,source.scalar(AirShape.UNIT_BODY_AVAILABILITY,1),source.optional(reason),source.child(fixture.root,9),originId);
        source.replaceField(fixture.root,4,source.list(unit));var port=new Store();
        try(var snapshot=AirSnapshot.attach(source,fixture.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var declarations=declarations(snapshot,keys);
            var grounding=SnapshotGrounding.build(snapshot,declarations,port,new SnapshotGraphChecks.Store(),1000,1000)) {
            for(long scope:scopes)eq(false,grounding.groundedNode(scope));eq(false,grounding.groundedNode(none));
            for(long object:objects)eq(false,grounding.groundedNode(object));
            eq(0L,grounding.counts().grounded());eq(0,source.indexedListReads);eq(0,source.activeCursors);
            // Constructor-local scope predicates reject these forms separately; grounding cannot invent a seed.
        }
        eq(0L,port.claimed);
    }
    static void operationalFailuresNeverPublishGroundingAndPreserveBorrowedInput() {
        var f=base();add(f,"seed",new Memory.CellBinding(new StorageId(f.pub,"s")));var input=f.build();var measured=new Store();
        try(var snapshot=AirSnapshot.fromPublication(input);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var declarations=declarations(snapshot,keys);
            var grounding=SnapshotGrounding.build(snapshot,declarations,measured,new SnapshotGraphChecks.Store(),1000,1000)){eq(2L,grounding.counts().grounded());}
        for(long at=0;at<measured.requests;at++) {
            var port=new Store();port.remaining=at;var graph=new SnapshotGraphChecks.Store();
            try(var snapshot=AirSnapshot.fromPublication(input);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var declarations=declarations(snapshot,keys)) {
                eq(port.failure,fails(IllegalStateException.class,()->SnapshotGrounding.build(snapshot,declarations,port,graph,1000,1000)));
                eq(AirShape.PUBLICATION,snapshot.shape(snapshot.root()));eq(true,declarations.entities()>0);
            }
            eq(0L,port.claimed);eq(true,port.closed);eq(true,graph.closed);eq(0L,graph.claimed);
        }
        var port=new Store();port.deny=true;var graph=new SnapshotGraphChecks.Store();
        try(var snapshot=AirSnapshot.fromPublication(input);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var declarations=declarations(snapshot,keys)) {
            eq(port.failure,fails(IllegalStateException.class,()->SnapshotGrounding.build(snapshot,declarations,port,graph,1000,1000)));eq(true,graph.closed);
        }
        eq(true,port.closed);eq(0L,port.claimed);
        var failedGraph=new SnapshotGraphChecks.Store();failedGraph.remaining=0;var owned=new Store();
        try(var snapshot=AirSnapshot.fromPublication(input);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var declarations=declarations(snapshot,keys)) {
            eq(failedGraph.failure,fails(IllegalStateException.class,()->SnapshotGrounding.build(snapshot,declarations,owned,failedGraph,1000,1000)));eq(true,owned.closed);eq(0L,owned.claimed);
            var queried=new Store();
            try(var grounding=SnapshotGrounding.build(snapshot,declarations,queried,new SnapshotGraphChecks.Store(),1000,1000)) {
                long id=objectIdentity(snapshot,declarations,"seed");queried.remaining=0;
                eq(queried.failure,fails(IllegalStateException.class,()->grounding.groundedObject(id)));fails(IllegalStateException.class,grounding::counts);
                eq(AirShape.PUBLICATION,snapshot.shape(snapshot.root()));
            }
            eq(0L,queried.claimed);
            var cleanup=new Store();cleanup.remaining=0;cleanup.closeFailure=true;cleanup.leaseFailure=true;
            eq(cleanup.failure,fails(IllegalStateException.class,()->SnapshotGrounding.build(snapshot,declarations,cleanup,new SnapshotGraphChecks.Store(),1000,1000)));
            eq(1,cleanup.failure.getSuppressed().length);eq(cleanup.cleanup,cleanup.failure.getSuppressed()[0]);eq(1,cleanup.cleanup.getSuppressed().length);eq(cleanup.leaseCleanup,cleanup.cleanup.getSuppressed()[0]);eq(0L,cleanup.claimed);
        }
    }
    private static Fixtures base(){var f=new Fixtures();f.linear(new Operations.Nop(f.header(f.op("nop"))));return f;}
    private static Memory.UnknownBinding unknown(Fixtures f,Scopes.MemoryScope scope){return new Memory.UnknownBinding(scope,new UncertaintyId(f.pub,"reason"));}
    private static void add(Fixtures f,String name,Memory.Binding binding){f.objects.add(new Memory.ObjectDeclaration(new ObjectId(f.unit,name),Optional.empty(),Fixtures.known(Types.Builtin.TEXT),binding,Memory.Visibility.PRIVATE,f.origin,Evidence.CoverageStatus.MODELED,f.precision()));}
    private static SnapshotDeclarations declarations(AirSnapshot s,SnapshotIdentityKeys keys){return SnapshotDeclarations.build(s,keys,new SnapshotDeclarationChecks.Store(),100_000,100_000,(r,i,n)->{throw new AssertionError(r);});}
    private static long objectIdentity(AirSnapshot s,SnapshotDeclarations d,String name){for(long at=0;at<d.entities();at++){long node=d.declaration(at,SnapshotDeclarations.Fact.NODE),id=d.declaration(at,SnapshotDeclarations.Fact.IDENTITY);if(s.shape(node)==AirShape.MEMORY_OBJECT_DECLARATION&&local(s,id).equals(name))return id;}throw new AssertionError(name);}
    private static String local(AirSnapshot s,long id){long text=s.field(id,s.shape(id),1);char[] chars=new char[Math.toIntExact(s.characterCount(text))];s.readCharacters(text,0,chars,0,chars.length);return new String(chars);}
    static final class Store implements SnapshotGrounding.Storage {
        final Set<Long> expanded=new HashSet<>(),grounded=new HashSet<>(),defined=new HashSet<>();final Map<Long,ArrayList<Long>> reverse=new HashMap<>();final ArrayDeque<Long> queue=new ArrayDeque<>();
        Iterator<Long> dependents;long current,dependent,claimed,requests,remaining=Long.MAX_VALUE,expansions,propagations;boolean frozen,closed,deny,closeFailure,leaseFailure;
        final IllegalStateException failure=new IllegalStateException("grounding scratch failure"),cleanup=new IllegalStateException("grounding scratch cleanup"),leaseCleanup=new IllegalStateException("grounding control cleanup");
        private void work(){requests++;if(remaining--==0)throw failure;}
        public boolean expand(long node){work();defined.add(node);if(!expanded.add(node))return false;expansions++;return true;}
        public void link(long child,long parent){work();defined.add(child);defined.add(parent);reverse.computeIfAbsent(child,k->new ArrayList<>()).add(parent);}
        public boolean prove(long node){work();if(!defined.contains(node))throw new AssertionError("undefined grounding node");return grounded.add(node);}
        public boolean grounded(long node){work();if(!defined.contains(node))throw new IllegalArgumentException("uncollected grounding node");return grounded.contains(node);}
        public void enqueue(long node){work();queue.addLast(node);}
        public boolean advance(){work();Long next=queue.pollFirst();if(next==null)return false;current=next;propagations++;return true;}
        public long node(){return current;}
        public void dependents(long node){work();dependents=reverse.getOrDefault(node,new ArrayList<>()).iterator();}
        public boolean advanceDependent(){work();if(!dependents.hasNext())return false;dependent=dependents.next();return true;}
        public long dependent(){return dependent;}
        public void start(){work();eq(defined,expanded);}
        public void freeze(){work();eq(0,queue.size());frozen=true;}
        public AirSnapshotBuilder.Lease claim(long bytes){if(deny)throw failure;claimed+=bytes;return ()->{claimed-=bytes;if(leaseFailure)throw leaseCleanup;};}
        public void close(){if(closed)return;closed=true;expanded.clear();grounded.clear();defined.clear();reverse.clear();queue.clear();if(closeFailure)throw cleanup;}
    }
    private static <T extends Throwable>T fails(Class<T> type,Runnable action){try{action.run();}catch(Throwable error){if(type.isInstance(error))return type.cast(error);throw new AssertionError(error);}throw new AssertionError("missing "+type);}
    private static void eq(Object expected,Object actual){if(!Objects.equals(expected,actual))throw new AssertionError("expected="+expected+" actual="+actual);}
}
