package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;

/** Hand-written declaration/ownership facts. No full Validator certificate is inferred. */
final class SnapshotDeclarationChecks {
    private SnapshotDeclarationChecks() { }
    static void declarationFamiliesAndEntrySeeds() {
        var f = new Fixtures(); var object = f.object("same",Fixtures.known(Types.Builtin.TEXT));
        var artifact = new ArtifactId(f.pub,"same");
        f.artifacts.add(new Origins.Artifact(artifact,"synthetic",Optional.empty()));
        var relation = new ArtifactRelationId(f.pub,"same");
        f.artifactRelations.add(new Artifacts.Relation(relation,artifact,new Artifacts.InternalArtifact(artifact),"synthetic",f.origin,Evidence.CoverageStatus.MODELED));
        var resource = new ResourceId(f.pub,"same");
        f.resources.add(new Interactions.Resource(resource,new Interactions.LocalResource("synthetic"),f.origin));
        var reason = f.uncertainty("same","VALUE_UNKNOWN");
        var premise = f.proof("same",new Proofs.ObjectDomain(object),new Proofs.ObjectDomain(object),Proofs.PublicationDomain.INSTANCE);
        var port = new CompletionPortId(f.unit,"same"); f.completionPorts.add(new Entries.CompletionPort(port,f.origin));
        var op = f.op("same");
        var left = f.text(op,"left","a"); var right = f.text(op,"right","b");
        var parent = new Expressions.Binary(f.operand(op,"parent",Operand.Role.VALUE_READ),Expressions.BinaryOperator.CONCAT,left,right);
        f.sequence("same",List.of(),new Operations.Opaque(f.header(op),"synthetic",List.of(parent),List.of(left.header().id()),f.envelope(null)));
        var seeds = new ArrayList<Entries.InitialCondition>(); var seedIds = new ArrayList<Id>();
        Entries.InitialValue[] initial = {
            new Entries.LiteralInitial(new Expressions.Literal(f.entryOperand("literal",Operand.Role.VALUE_READ),new Values.TextValue("a"))),
            new Entries.PossibleLiterals(List.of(new Expressions.Literal(f.entryOperand("possible-a",Operand.Role.VALUE_READ),new Values.TextValue("a")),
                new Expressions.Literal(f.entryOperand("possible-b",Operand.Role.VALUE_READ),new Values.TextValue("b"))),reason),
            new Entries.ParameterInitial(java.math.BigInteger.ZERO),Entries.Preserve.INSTANCE,
            new Entries.ExternalUnknown(reason),new Entries.Uninitialized(reason)
        };
        for (int n=0;n<initial.length;n++) {
            var place = new Places.ObjectPlace(f.entryOperand("seed-"+n,Operand.Role.VALUE_WRITE),object);
            seeds.add(new Entries.InitialCondition(place,initial[n],f.origin,List.of())); seedIds.add(place.header().id());
        }
        seedIds.add(((Entries.LiteralInitial)initial[0]).value().header().id());
        for (var value : ((Entries.PossibleLiterals)initial[1]).candidates()) seedIds.add(value.header().id());
        f.state = new Entries.EntryState(seeds,List.of());
        var expected = new ArrayList<Id>(List.of(f.pub,artifact,f.origin,reason,premise.id(),resource,relation,
            new StorageId(f.pub,"same-cell"),f.unit,object,port,f.entry(),f.label("same"),op,parent.header().id(),left.header().id(),right.header().id()));
        expected.addAll(seedIds);
        var p = f.build(); var store = new Store(); var violations = new ArrayList<SnapshotDeclarations.Rule>();
        try (var snapshot = AirSnapshot.fromPublication(p); var keys = new SnapshotIdentityKeys(snapshot,new Keys());
             var index = SnapshotDeclarations.build(snapshot,keys,store,Long.MAX_VALUE,Long.MAX_VALUE,(rule,id,node) -> violations.add(rule))) {
            eq(List.of(),violations); eq(26L,index.entities()); eq(12L,index.operands()); eq(1L,index.operations());
            eq(26,expected.size());
            // Storage captures declarations in primitive rows. Reconstruct small test-only IDs,
            // compare to expected IDs independently; never derive expected from index iteration.
            var actual = new HashSet<Id>();
            for (long n=0;n<index.entities();n++) actual.add(readId(snapshot,index.declaration(n,SnapshotDeclarations.Fact.IDENTITY)));
            fails(IndexOutOfBoundsException.class,() -> index.declaration(-1,SnapshotDeclarations.Fact.NODE));
            fails(IndexOutOfBoundsException.class,() -> index.declaration(index.entities(),SnapshotDeclarations.Fact.NODE));
            eq(new HashSet<>(expected),actual);
            long operation = findById(snapshot,store,op);
            long opId = snapshot.field(snapshot.field(operation,AirShape.OPERATIONS_OPAQUE,0),AirShape.OPERATIONS_HEADER,0);
            eq(operation,index.fact(opId,SnapshotDeclarations.Fact.NODE));
            eq(f.unit,readId(snapshot,index.fact(opId,SnapshotDeclarations.Fact.UNIT)));
            eq(f.label("same"),readId(snapshot,index.fact(opId,SnapshotDeclarations.Fact.SEQUENCE)));
            long operand = findById(snapshot,store,parent.header().id());
            long operandId = snapshot.field(snapshot.field(operand,AirShape.EXPRESSIONS_BINARY,0),AirShape.OPERAND_HEADER,0);
            eq(op,readId(snapshot,index.fact(operandId,SnapshotDeclarations.Fact.OWNER)));
            eq(0L,index.fact(snapshot.field(opId,AirShape.IDS_OPERATION_ID,0),SnapshotDeclarations.Fact.OWNER));
        }
        eq(true,store.closed); eq(0L,store.claimed); eq(0,store.frontier.size());
    }
    static void duplicatesAndCompleteOwnership() {
        var f = new Fixtures(); var op = f.op("same"); var header = f.header(op);
        var leaf = f.text(op,"leaf","a");
        var foreignPub = new PublicationId("foreign");
        var foreignUnit = new UnitId(foreignPub,"unit");
        var foreign = new Expressions.Literal(new Operand.Header(new OperandId(new OperationOwner(new OperationId(foreignUnit,"same")),"leaf"),Operand.Role.VALUE_READ,f.origin),new Values.TextValue("b"));
        var wrongEntry = new Expressions.Literal(f.entryOperand("leaf",Operand.Role.VALUE_READ),new Values.TextValue("c"));
        f.sequence("start",List.of(new Operations.Nop(header)),new Operations.Opaque(header,"synthetic",List.of(leaf,leaf,foreign,wrongEntry),List.of(),f.envelope(null)));
        var rules = new EnumMap<SnapshotDeclarations.Rule,Integer>(SnapshotDeclarations.Rule.class);
        try (var snapshot = AirSnapshot.fromPublication(f.build()); var keys = new SnapshotIdentityKeys(snapshot,new Keys());
             var index = SnapshotDeclarations.build(snapshot,keys,new Store(),Long.MAX_VALUE,Long.MAX_VALUE,(rule,id,node) -> rules.merge(rule,1,Integer::sum))) {
            eq(2,rules.get(SnapshotDeclarations.Rule.DUPLICATE_ID)); // operation + repeated occurrence
            eq(1,rules.get(SnapshotDeclarations.Rule.FOREIGN_PUBLICATION));
            eq(2,rules.get(SnapshotDeclarations.Rule.OPERAND_OWNER));
            eq(3L,index.operands()); eq(1L,index.operations());
        }
        var g = new Fixtures(); var other = new UnitId(g.pub,"other");
        var foreignOperation = new OperationId(other,"same");
        g.sequence("start",List.of(),new Operations.Halt(g.header(foreignOperation),Operations.HaltKind.NORMAL));
        var found = new ArrayList<SnapshotDeclarations.Rule>();
        try (var snapshot = AirSnapshot.fromPublication(g.build()); var keys = new SnapshotIdentityKeys(snapshot,new Keys());
             var index = SnapshotDeclarations.build(snapshot,keys,new Store(),100,100,(rule,id,node) -> found.add(rule))) {
            eq(1L,index.operations()); eq(List.of(SnapshotDeclarations.Rule.OPERATION_UNIT),found);
        }
    }
    static void deepFrontierAndFailureLifetimes() {
        var f = new Fixtures(); var op = f.op("deep"); Expression root = f.text(op,"leaf","a");
        for (int n=0;n<20_000;n++) root = new Expressions.Unary(f.operand(op,"u"+n,Operand.Role.VALUE_READ),Expressions.UnaryOperator.LENGTH,root);
        f.sequence("start",List.of(),new Operations.Opaque(f.header(op),"synthetic",List.of(root),List.of(),f.envelope(null)));
        var store = new Store();
        try (var snapshot = AirSnapshot.fromPublication(f.build()); var keys = new SnapshotIdentityKeys(snapshot,new Keys());
             var index = SnapshotDeclarations.build(snapshot,keys,store,Long.MAX_VALUE,20_000,(rule,id,node) -> { throw new AssertionError(rule); })) {
            eq(20_001L,index.operands()); eq(1,store.maxPending); eq(0,store.frontier.size());
        }
        eq(0L,store.claimed);
        for (boolean nesting : new boolean[]{false,true}) {
            var limited = new Store();
            try (var snapshot = AirSnapshot.fromPublication(f.build()); var keys = new SnapshotIdentityKeys(snapshot,new Keys())) {
                var failure = fails(SnapshotDeclarations.Limit.class,() -> SnapshotDeclarations.build(snapshot,keys,limited,nesting?Long.MAX_VALUE:10,nesting?10:Long.MAX_VALUE,(rule,id,node) -> { }));
                eq(nesting?SnapshotDeclarations.LimitKind.NESTING:SnapshotDeclarations.LimitKind.ENTITIES,failure.kind());
                eq(AirShape.PUBLICATION,snapshot.shape(snapshot.root())); eq(0L,limited.claimed); eq(true,limited.closed);
            }
        }
        for (int at : new int[]{0,1,5,50}) {
            var broken = new Store(); broken.remaining=at;
            try (var snapshot = AirSnapshot.fromPublication(f.build()); var keys = new SnapshotIdentityKeys(snapshot,new Keys())) {
                eq(broken.failure,fails(IllegalStateException.class,() -> SnapshotDeclarations.build(snapshot,keys,broken,Long.MAX_VALUE,Long.MAX_VALUE,(rule,id,node) -> { })));
                eq(AirShape.PUBLICATION,snapshot.shape(snapshot.root())); eq(true,broken.closed); eq(0L,broken.claimed);
            }
        }
        var denied = new Store(); denied.deny=true;
        try (var snapshot = AirSnapshot.fromPublication(f.build()); var keys = new SnapshotIdentityKeys(snapshot,new Keys())) {
            eq(denied.failure,fails(IllegalStateException.class,() -> SnapshotDeclarations.build(snapshot,keys,denied,100,100,(rule,id,node) -> { })));
            eq(true,denied.closed); eq(0L,denied.claimed);
        }
    }
    static void wideFrontierAndAbortScopes() {
        var f=new Fixtures();var op=f.op("wide");var values=new ArrayList<Operand>();
        for(int n=0;n<4096;n++)values.add(f.text(op,"v"+n,"a"));
        f.sequence("start",List.of(),new Operations.Opaque(f.header(op),"synthetic",values,List.of(),f.envelope(null)));
        var store=new Store();
        try(var snapshot=AirSnapshot.fromPublication(f.build());var keys=new SnapshotIdentityKeys(snapshot,new Keys());
            var index=SnapshotDeclarations.build(snapshot,keys,store,Long.MAX_VALUE,0,(rule,id,node)->{throw new AssertionError(rule);})) {
            eq(4096L,index.operands());eq(4096,store.maxPending);eq(0,store.frontier.size());
            eq(true,store.frozen);
            fails(IllegalStateException.class,()->store.define(999,1,2,3,4,5));
            fails(IllegalStateException.class,()->store.enqueue(1,2,0));
            fails(IllegalStateException.class,store::advance);
            eq(AirShape.PUBLICATION,snapshot.shape(index.declaration(0,SnapshotDeclarations.Fact.NODE)));
            store.remaining=0;
            eq(store.failure,fails(IllegalStateException.class,()->index.declaration(0,SnapshotDeclarations.Fact.NODE)));
            fails(IllegalStateException.class,index::entities);eq(true,store.closed);eq(0L,store.claimed);
            eq(AirShape.PUBLICATION,snapshot.shape(snapshot.root()));
        }
        var g=new Fixtures();var header=g.header(g.op("duplicate"));
        g.sequence("start",List.of(new Operations.Nop(header)),new Operations.Halt(header,Operations.HaltKind.NORMAL));
        var broken=new Store();broken.closeFailure=true;broken.leaseFailure=true;
        var primary=new IllegalArgumentException("issue sink failure");
        try(var snapshot=AirSnapshot.fromPublication(g.build());var keys=new SnapshotIdentityKeys(snapshot,new Keys())) {
            eq(primary,fails(IllegalArgumentException.class,()->SnapshotDeclarations.build(snapshot,keys,broken,100,100,(rule,id,node)->{throw primary;})));
            eq(1,primary.getSuppressed().length);eq(broken.failure,primary.getSuppressed()[0]);
            eq(1,broken.failure.getSuppressed().length);eq(0L,broken.claimed);eq(true,broken.closed);
            eq(AirShape.PUBLICATION,snapshot.shape(snapshot.root()));
        }
        var freezeDenied=new Store();freezeDenied.freezeFailure=true;
        try(var snapshot=AirSnapshot.fromPublication(g.build());var keys=new SnapshotIdentityKeys(snapshot,new Keys())) {
            eq(freezeDenied.failure,fails(IllegalStateException.class,()->SnapshotDeclarations.build(snapshot,keys,freezeDenied,100,100,(rule,id,node)->{ })));
            eq(true,freezeDenied.closed);eq(0L,freezeDenied.claimed);eq(AirShape.PUBLICATION,snapshot.shape(snapshot.root()));
        }
        // Unknown references can be indexed without implying reference validity.
        var h=new Fixtures();var missing=h.label("missing");
        h.sequence("start",List.of(),new Operations.Jump(h.header(h.op("jump")),missing));
        var lookupStore=new Store();
        try(var snapshot=AirSnapshot.fromPublication(h.build());var keys=new SnapshotIdentityKeys(snapshot,new Keys());
            var index=SnapshotDeclarations.build(snapshot,keys,lookupStore,100,100,(rule,id,node)->{throw new AssertionError(rule);})) {
            long operation=0;
            for(long n=0;n<index.entities();n++) {
                long node=index.declaration(n,SnapshotDeclarations.Fact.NODE);
                if(snapshot.shape(node)==AirShape.OPERATIONS_JUMP)operation=node;
            }
            long target=snapshot.field(operation,AirShape.OPERATIONS_JUMP,1);
            eq(0L,index.fact(target,SnapshotDeclarations.Fact.NODE));
        }
    }
    private static long findById(AirSnapshot snapshot,Store store,Id id) {
        for (long[] row : store.facts.values()) if (id.equals(readId(snapshot,row[SnapshotDeclarations.Fact.IDENTITY.ordinal()]))) return row[SnapshotDeclarations.Fact.NODE.ordinal()];
        throw new AssertionError("missing "+id);
    }
    // Small independently decoded ID facts, outside production code and managed memory claims.
    private static Id readId(AirSnapshot s,long id) {
        AirShape shape=s.shape(id); String local;
        if(shape==AirShape.IDS_PUBLICATION_ID) return new PublicationId(text(s,s.field(id,shape,0)));
        local=text(s,s.field(id,shape,1)); long parent=s.field(id,shape,0);
        return switch(shape) {
            case IDS_UNIT_ID -> new UnitId((PublicationId)readId(s,parent),local);
            case IDS_ENTRY_ID -> new EntryId((UnitId)readId(s,parent),local);
            case IDS_LABEL_ID -> new LabelId((UnitId)readId(s,parent),local);
            case IDS_OPERATION_ID -> new OperationId((UnitId)readId(s,parent),local);
            case IDS_OPERAND_ID -> {
                AirShape owner=s.shape(parent); Id ownerId=readId(s,s.field(parent,owner,0));
                yield new OperandId(owner==AirShape.IDS_OPERATION_OWNER?new OperationOwner((OperationId)ownerId):new EntryOwner((EntryId)ownerId),local);
            }
            case IDS_OBJECT_ID -> new ObjectId((UnitId)readId(s,parent),local);
            case IDS_STORAGE_ID -> new StorageId((PublicationId)readId(s,parent),local);
            case IDS_RESOURCE_ID -> new ResourceId((PublicationId)readId(s,parent),local);
            case IDS_ARTIFACT_ID -> new ArtifactId((PublicationId)readId(s,parent),local);
            case IDS_ARTIFACT_RELATION_ID -> new ArtifactRelationId((PublicationId)readId(s,parent),local);
            case IDS_ORIGIN_ID -> new OriginId((PublicationId)readId(s,parent),local);
            case IDS_UNCERTAINTY_ID -> new UncertaintyId((PublicationId)readId(s,parent),local);
            case IDS_PREMISE_ID -> new PremiseId((PublicationId)readId(s,parent),local);
            case IDS_COMPLETION_PORT_ID -> new CompletionPortId((UnitId)readId(s,parent),local);
            default -> throw new AssertionError(shape);
        };
    }
    private static String text(AirSnapshot s,long node) { char[] chars=new char[Math.toIntExact(s.characterCount(node))];s.readCharacters(node,0,chars,0,chars.length);return new String(chars); }
    private record Tuple(long tag,long left,long right,long a,long b,long c,long d) { }
    private static final class Keys implements SnapshotIdentityKeys.Storage {
        final Map<Long,Long> memo=new HashMap<>();final Map<Tuple,Long> tuples=new HashMap<>();long issued;
        public long known(long node){return memo.getOrDefault(node,0L);}
        public void remember(long node,long key){memo.put(node,key);}
        public long intern(long tag,long l,long r,long a,long b,long c,long d){return tuples.computeIfAbsent(new Tuple(tag,l,r,a,b,c,d),ignored -> ++issued);}
        public AirSnapshotBuilder.Lease claim(long bytes){return () -> { };}
        public void close(){memo.clear();tuples.clear();}
    }
    private static final class Store implements SnapshotDeclarations.Storage {
        final Map<Long,long[]> facts=new HashMap<>(); final ArrayList<long[]> rows=new ArrayList<>(); final ArrayDeque<long[]> frontier=new ArrayDeque<>();
        final IllegalStateException failure=new IllegalStateException("injected store failure");
        long[] current; long claimed,remaining=Long.MAX_VALUE; int maxPending; boolean closed,deny,closeFailure,leaseFailure,frozen,freezeFailure;
        private void work(){if(remaining--==0)throw failure;}
        private void mutable(){if(frozen)throw new IllegalStateException("frozen index");}
        public void freeze(){if(freezeFailure)throw failure;if(!frontier.isEmpty())throw new AssertionError("frontier not drained");frozen=true;}
        public boolean define(long key,long node,long identity,long unit,long sequence,long owner){mutable();work();long[] row=new long[]{node,identity,unit,sequence,owner};if(facts.putIfAbsent(key,row)!=null)return false;rows.add(row);return true;}
        public long declaration(long index,SnapshotDeclarations.Fact field){work();return rows.get(Math.toIntExact(index))[field.ordinal()];}
        public long fact(long key,SnapshotDeclarations.Fact field){work();long[] row=facts.get(key);return row==null?0:row[field.ordinal()];}
        public void enqueue(long node,long owner,long depth){mutable();work();frontier.addLast(new long[]{node,owner,depth});maxPending=Math.max(maxPending,frontier.size());}
        public boolean advance(){mutable();work();current=frontier.pollFirst();return current!=null;}
        public long node(){return current[0];} public long owner(){return current[1];} public long depth(){return current[2];}
        public AirSnapshotBuilder.Lease claim(long bytes){mutable();if(deny)throw failure;claimed+=bytes;return new AirSnapshotBuilder.Lease(){boolean closed;public void close(){if(!closed){closed=true;claimed-=bytes;if(leaseFailure)throw new IllegalStateException("lease cleanup failure");}}};}
        public void close(){if(closed)return;closed=true;facts.clear();rows.clear();frontier.clear();current=null;if(closeFailure)throw failure;}
    }
    private static <T extends Throwable>T fails(Class<T> type,Runnable action){try{action.run();}catch(Throwable error){if(type.isInstance(error))return type.cast(error);throw new AssertionError(error);}throw new AssertionError("missing "+type);}
    private static void eq(Object expected,Object actual){if(!Objects.equals(expected,actual))throw new AssertionError("expected="+expected+" actual="+actual);}
}
