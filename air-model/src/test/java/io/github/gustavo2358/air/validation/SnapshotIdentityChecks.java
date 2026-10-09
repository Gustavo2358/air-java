package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Independent model equality oracle; resident ports here are deliberately not scale backends. */
final class SnapshotIdentityChecks {
    private SnapshotIdentityChecks() { }
    static void completeNamespaces() {
        var source = new Source(); var values = new ArrayList<Object>();
        for (String publication : List.of("P", "Q")) for (String unit : List.of("U", "V")) {
            var p = new PublicationId(publication); var u = new UnitId(p, unit);
            for (String text : List.of("Aa", "BB", "x", "x\u0000", "x\u0000\u0000", " x", "x ", "𝄞é", "𝄞é")) {
                values.addAll(List.of(p, u, new StorageId(p,text), new ResourceId(p,text), new ArtifactId(p,text),
                        new ArtifactRelationId(p,text), new OriginId(p,text), new UncertaintyId(p,text), new PremiseId(p,text),
                        new EntryId(u,text), new LabelId(u,text), new OperationId(u,text), new ObjectId(u,text),
                        new CompletionPortId(u,text), new OperationOwner(new OperationId(u,text)), new EntryOwner(new EntryId(u,text)),
                        new OperandId(new OperationOwner(new OperationId(u,"owner")),text),
                        new OperandId(new EntryOwner(new EntryId(u,"owner")),text)));
            }
        }
        for(int length:new int[]{15,16,17,31,32,33,1023,1024,1025,8191}) {
            String text=("ab𝄞é\u0000CD".repeat(1200)).substring(0,length);
            // Avoid cutting a supplementary scalar in the independent model oracle.
            if(Character.isHighSurrogate(text.charAt(text.length()-1)))text+="\uDD1E";
            values.add(new PublicationId(text));values.add(new PublicationId(new String(text.toCharArray())));
            values.add(new PublicationId(text+"\u0000"));values.add(new PublicationId(text+"Z"));
        }
        long[] handles = new long[values.size()], keys = new long[values.size()];
        for (int n=0;n<handles.length;n++) handles[n]=source.add(values.get(n));
        var storage = new Store();
        try (var snapshot=AirSnapshot.attach(source,1); var index=new SnapshotIdentityKeys(snapshot,storage)) {
            for (int n=0;n<keys.length;n++) keys[n]=index.key(handles[n]);
            long sourceReads=source.characters;
            for(int n=0;n<keys.length;n++)if(values.get(n) instanceof Id id)eq(keys[n],index.key(id));
            eq(sourceReads,source.characters); // Typed queries do not reread source text.
            for (int a=0;a<keys.length;a++) for (int b=0;b<keys.length;b++)
                eq(values.get(a).equals(values.get(b)),keys[a]==keys[b]);
            long characters=source.characters, tuples=storage.calls;
            for(long handle:handles) index.key(handle);
            eq(characters,source.characters);eq(tuples,storage.calls);
            fails(IllegalArgumentException.class,()->index.key(1));
        }
        eq(0L,storage.claimed);eq(true,storage.closed);
    }
    static void sharedTextAndFailureLifetimes() {
        var source=new Source();var p=new PublicationId("P"+"𝄞\u0000abc".repeat(16384));var u=new UnitId(p,"U");
        long[] handles=new long[256];
        for(int n=0;n<handles.length;n++) handles[n]=source.add(new OperandId(new OperationOwner(new OperationId(u,"O"+n)),"operand"));
        var storage=new Store();
        try(var snapshot=AirSnapshot.attach(source,1);var index=new SnapshotIdentityKeys(snapshot,storage)) {
            for(long handle:handles)index.key(handle);
            long characters=source.characters,calls=storage.calls;
            // Each source text is read once, including the giant shared publication prefix.
            eq(source.textUnits,characters);
            if(calls>characters/4+4096)throw new AssertionError("repeated text-prefix reconstruction: "+calls);
            for(int n=0;n<10;n++)for(long handle:handles)index.key(handle);
            eq(characters,source.characters);eq(calls,storage.calls);
            closeSnapshot(snapshot);fails(IllegalStateException.class,()->index.key(handles[0]));
        }
        eq(0L,storage.claimed);
        var denied=new Store();denied.denyClaim=true;var borrowed=new Source();
        try(var snapshot=AirSnapshot.attach(borrowed,1)) {
            fails(Denied.class,()->new SnapshotIdentityKeys(snapshot,denied));
            eq(true,denied.closed);eq(AirShape.PUBLICATION,snapshot.shape(1));
        }
        var readSource=new Source();long readId=readSource.add(new PublicationId("source-read"));var readStorage=new Store();
        try(var snapshot=AirSnapshot.attach(readSource,1);var index=new SnapshotIdentityKeys(snapshot,readStorage)) {
            index.key(readId);readSource.failShape=true;
            eq(readSource.failure,fails(Denied.class,()->index.key(readId)));
            readSource.failShape=false;
            fails(IllegalStateException.class,()->index.key(readId));
            eq(AirShape.PUBLICATION,snapshot.shape(1));
        }
        eq(0L,readStorage.claimed);
        // Keep the original 2000 characters and every denial position. Distinct
        // adjacent leaves make position 100 reachable even with exact pair reuse.
        String failureText="abcdefghijklmnopqrstuvwxyz".repeat(77).substring(0,2000);
        for(int at:new int[]{0,1,5,100}) {
            var failingSource=new Source();long id=failingSource.add(new OperandId(new EntryOwner(new EntryId(new UnitId(new PublicationId("p"),"u"),"e")),failureText));
            var failing=new Store();failing.remaining=at;
            try(var snapshot=AirSnapshot.attach(failingSource,1);var index=new SnapshotIdentityKeys(snapshot,failing)) {
                var first=fails(Denied.class,()->index.key(id));eq(failing.failure,first);
                fails(IllegalStateException.class,()->index.key(id));
                failing.closeFailure=true;failing.leaseFailure=true;
                var teardown=fails(Denied.class,index::close);eq(1,teardown.getSuppressed().length);
                closeIndex(index);eq(0L,failing.claimed);eq(AirShape.PUBLICATION,snapshot.shape(1));
            }
        }
        for(int at:new int[]{0,1,5,100}) {
            var input=new Source();var store=new Store();store.remaining=at;
            var typed=new OperandId(new EntryOwner(new EntryId(new UnitId(new PublicationId("p"),"u"),"e")),failureText);
            try(var snapshot=AirSnapshot.attach(input,1);var index=new SnapshotIdentityKeys(snapshot,store)) {
                eq(store.failure,fails(Denied.class,()->index.key(typed)));
                fails(IllegalStateException.class,()->index.key(typed));eq(0L,input.characters);
                eq(AirShape.PUBLICATION,snapshot.shape(1));
            }
            eq(0L,store.claimed);eq(true,store.closed);
        }
        var input=new Source();var store=new Store();
        try(var snapshot=AirSnapshot.attach(input,1);var index=new SnapshotIdentityKeys(snapshot,store)) {
            index.key(new OriginId(new PublicationId("P"),"original"));closeSnapshot(snapshot);
            fails(IllegalStateException.class,()->index.key(new OriginId(new PublicationId("P"),"original")));
        }
        eq(0L,store.claimed);
    }
    static void fixedPackedLeafReusePreservesCompleteIdentityAndSourceReads() {
        for(int blocks:new int[]{1,4,16,64,256}) {
            var source=new Source();var store=new Store();
            var id=new PublicationId("x".repeat(1024*blocks));long handle=source.add(id);
            try(var snapshot=AirSnapshot.attach(source,1);var keys=new SnapshotIdentityKeys(snapshot,store)) {
                eq(4096L,store.claimed);
                long key=keys.key(handle);
                eq((long)id.localId().length(),source.characters); // All content is still inspected.
                eq(1L,store.leafRequests); // One repeated exact packed leaf, not one lookup per occurrence.
                if(store.pairRequests>Long.SIZE)throw new AssertionError("repeated canonical text-pair probes: "+store.pairRequests);
                long rows=store.issued,reads=source.characters;
                long pairs=store.pairRequests;
                eq(key,keys.key(id));eq(rows,store.issued);eq(reads,source.characters);
                eq(pairs,store.pairRequests); // The same complete text reuses its exact immutable pairs.
                var changed=new PublicationId(id.localId()+"\u0000");long changedHandle=source.add(changed);
                eq(false,key==keys.key(changedHandle));eq(keys.key(changedHandle),keys.key(changed));
                eq(false,key==keys.key(new PublicationId(id.localId().substring(0,id.localId().length()-1)+"y")));
                eq(false,key==keys.key(new OriginId(id,"x")));
            }
            eq(0L,store.claimed);eq(true,store.closed);
        }
        // Carry levels wrap the fixed 16-slot memo; a slot is never identity.
        var source=new Source();var store=new Store();String text="x".repeat(2*1024*1024);
        var original=new PublicationId(text);var changed=new PublicationId(text.substring(0,1024*1024)+"y"+text.substring(1024*1024+1));
        long first=source.add(original),second=source.add(changed);
        try(var snapshot=AirSnapshot.attach(source,1);var keys=new SnapshotIdentityKeys(snapshot,store)) {
            long a=keys.key(first),b=keys.key(second);eq(false,a==b);
            eq(a,keys.key(original));eq(b,keys.key(changed));eq(source.textUnits,source.characters);
            eq(4096L,store.claimed);
        }
        eq(0L,store.claimed);eq(true,store.closed);
    }
    private record Node(AirShape shape,Object scalar,long[] fields) { }
    private static final class Source implements AirSnapshot.Source {
        final Map<Long,Node> nodes=new HashMap<>();final IdentityHashMap<Object,Long> seen=new IdentityHashMap<>();
        long next=(long)Integer.MAX_VALUE+100,characters,textUnits;boolean closed,failShape;final Denied failure=new Denied();
        long add(Object value) {
            Long old=seen.get(value);if(old!=null)return old;
            long handle=next++;seen.put(value,handle);
            if(value instanceof String text) {nodes.put(handle,new Node(AirShape.TEXT,text,null));textUnits+=text.length();return handle;}
            AirShape shape=null;
            for(var candidate:AirShape.values())if(candidate.modelName().equals(value.getClass().getName().replace('$','.'))){shape=candidate;break;}
            if(shape==null)throw new AssertionError("uncatalogued model identity");
            RecordComponent[] fields=value.getClass().getRecordComponents();long[] children=new long[fields.length];
            try {for(int n=0;n<fields.length;n++)children[n]=add(fields[n].getAccessor().invoke(value));}
            catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
            nodes.put(handle,new Node(shape,null,children));return handle;
        }
        public AirShape shape(long h){if(failShape)throw failure;if(closed)throw new IllegalStateException();return h==1?AirShape.PUBLICATION:nodes.get(h).shape();}
        public long length(long h){Node n=nodes.get(h);return n.shape()==AirShape.TEXT?((String)n.scalar()).length():n.fields().length;}
        public long child(long h,long i){return nodes.get(h).fields()[Math.toIntExact(i)];}
        public long scalar(long h){throw new AssertionError("identities have no primitive scalar fields");}
        public int characters(long h,long offset,char[] output,int start,int count){characters+=count;((String)nodes.get(h).scalar()).getChars(Math.toIntExact(offset),Math.toIntExact(offset)+count,output,start);return count;}
        public void close(){closed=true;}
    }
    private record Tuple(long tag,long left,long right,long a,long b,long c,long d) {
        @Override public int hashCode(){return 0;} // All keys collide; equality still compares every column.
    }
    private static final class Store implements SnapshotIdentityKeys.Storage {
        final Map<Long,Long> memo=new HashMap<>();final Map<Tuple,Long> tuples=new HashMap<>();final Map<Long,Tuple> rows=new HashMap<>();
        final Denied failure=new Denied();long issued,calls,leafRequests,pairRequests,claimed,remaining=Long.MAX_VALUE;
        boolean closed,denyClaim,closeFailure,leaseFailure;
        public long known(long node){return memo.getOrDefault(node,0L);}
        public void remember(long node,long key){if(remaining--==0)throw failure;Long old=memo.putIfAbsent(node,key);if(old!=null&&old!=key)throw new AssertionError("memo identity changed");}
        public long intern(long tag,long l,long r,long a,long b,long c,long d){calls++;if(tag==1)leafRequests++;if(tag==2)pairRequests++;if(remaining--==0)throw failure;var tuple=new Tuple(tag,l,r,a,b,c,d);Long old=tuples.get(tuple);if(old!=null)return old;long key=++issued;tuples.put(tuple,key);rows.put(key,tuple);return key;}
        public long word(long key,int column){var row=rows.get(key);return switch(column){case 0->row.tag();case 1->row.left();case 2->row.right();case 3->row.a();case 4->row.b();case 5->row.c();case 6->row.d();default->throw new IllegalArgumentException("tuple column");};}
        public AirSnapshotBuilder.Lease claim(long bytes){if(denyClaim)throw failure;claimed+=bytes;return new AirSnapshotBuilder.Lease(){boolean released;public void close(){if(released)return;released=true;claimed-=bytes;if(leaseFailure)throw new Denied();}};}
        public void close(){if(closed)return;closed=true;memo.clear();tuples.clear();rows.clear();if(closeFailure)throw failure;}
    }
    private static final class Denied extends RuntimeException {private static final long serialVersionUID=1L;}
    private static void closeIndex(SnapshotIdentityKeys index){index.close();}
    private static void closeSnapshot(AirSnapshot snapshot){snapshot.close();}
    private interface Action {void run();}
    private static <T extends Throwable>T fails(Class<T> type,Action action){try{action.run();}catch(Throwable failure){if(type.isInstance(failure))return type.cast(failure);throw new AssertionError(failure);}throw new AssertionError("expected "+type.getName());}
    private static void eq(Object expected,Object actual){if(!java.util.Objects.equals(expected,actual))throw new AssertionError("expected "+expected+", got "+actual);}
}
