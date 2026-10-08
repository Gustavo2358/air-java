package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import java.util.*;

/** Hand-built storage grammar laws; no full Validator/certificate assertion. */
final class SnapshotGraphChecks {
    private SnapshotGraphChecks() { }
    static void exactPreorderAndContextualCollectionSharing() {
        var source=new Source();var f=new Fixture(source);var store=new Store();var visited=new ArrayList<String>();
        try(var snapshot=AirSnapshot.attach(source,f.root)) {
            var result=SnapshotGraphWalk.scan(snapshot,store,1000,1000,(node,shape,element)->visited.add(shape.name()+":"+(element==null?"-":element.name())));
            eq(List.of("PUBLICATION:-","IDS_PUBLICATION_ID:-","TEXT:-","SEMANTIC_VERSION:-","INTEGER:-","INTEGER:-","CAPABILITIES_MANIFEST:-",
                "LIST:CAPABILITIES_CAPABILITY","LIST:ORIGINS_ARTIFACT","LIST:UNIT","LIST:MEMORY_STORAGE","LIST:INTERACTIONS_RESOURCE","LIST:ARTIFACTS_RELATION","LIST:ORIGINS_ORIGIN",
                "EVIDENCE_COVERAGE:-","EVIDENCE_INVENTORY_STATUS:-","SCOPES_PUBLICATION_SCOPE:-","LIST:EVIDENCE_COVERAGE_ITEM","LIST:IDS_UNCERTAINTY_ID","LIST:EVIDENCE_UNCERTAINTY","LIST:PROOFS_PREMISE"),visited);
            eq(21L,result.nodes());eq(23L,result.edges());eq(11,source.cursors);eq(0,source.activeCursors);
            eq(AirShape.PUBLICATION,snapshot.shape(f.root));
        }
        eq(true,store.closed);eq(0L,store.claimed);
    }
    static void deepAndWideSequentialTraversal() {
        var source=new Source();var f=new Fixture(source);long leaf=source.scalar(AirShape.PROOFS_PUBLICATION_DOMAIN,0),scope=leaf;
        for(int n=0;n<20_000;n++)scope=source.record(AirShape.PROOFS_INTERSECTION,scope,leaf);
        long premise=f.premise(scope);source.replaceField(f.root,11,source.list(premise));var store=new Store();int[] intersections={0};
        try(var snapshot=AirSnapshot.attach(source,f.root)) {
            SnapshotGraphWalk.scan(snapshot,store,100_000,30_000,(node,shape,element)->{if(shape==AirShape.PROOFS_INTERSECTION)intersections[0]++;eq(0,source.activeCursors);});
        }
        eq(20_000,intersections[0]);eq(0L,store.claimed);
        var wide=new Source();var w=new Fixture(wide);long[] premises=new long[100_000];
        long shared=w.premise(wide.scalar(AirShape.PROOFS_PUBLICATION_DOMAIN,0));Arrays.fill(premises,shared);
        wide.replaceField(w.root,11,wide.list(premises));int[] seen={0};
        try(var snapshot=AirSnapshot.attach(wide,w.root)) {
            var result=SnapshotGraphWalk.scan(snapshot,new Store(),1000,1000,(node,shape,element)->{if(shape==AirShape.PROOFS_PREMISE)seen[0]++;eq(0,wide.activeCursors);});
            eq(true,result.edges()>=100_000);
        }
        eq(1,seen[0]);eq(0,wide.indexedListReads);eq(1,wide.maxCursors);
    }
    static void cyclesGrammarLimitsAndFailureCleanup() {
        for(boolean two:new boolean[]{false,true}) {
            var source=new Source();var f=new Fixture(source);long leaf=source.scalar(AirShape.PROOFS_PUBLICATION_DOMAIN,0);
            long a=source.record(AirShape.PROOFS_INTERSECTION,leaf,leaf),b=two?source.record(AirShape.PROOFS_INTERSECTION,a,leaf):a;
            source.replaceField(a,0,b);source.replaceField(f.root,11,source.list(f.premise(a)));var store=new Store();
            try(var snapshot=AirSnapshot.attach(source,f.root)) {
                fails(SnapshotGraphWalk.Cycle.class,()->SnapshotGraphWalk.scan(snapshot,store,1000,1000,(n,s,e)->{}));
                eq(AirShape.PUBLICATION,snapshot.shape(f.root));
            }
            eq(true,store.closed);eq(0L,store.claimed);
        }
        var source=new Source();var f=new Fixture(source);
        // One shared collection is valid as artifacts but not as Units; source handle alone cannot memoize it.
        long artifact=source.record(AirShape.ORIGINS_ARTIFACT,source.record(AirShape.IDS_ARTIFACT_ID,f.id,source.text("a")),source.text("a"),source.optional());
        long collection=source.list(artifact);source.replaceField(f.root,3,collection);source.replaceField(f.root,4,collection);
        try(var snapshot=AirSnapshot.attach(source,f.root)) {
            fails(IllegalArgumentException.class,()->SnapshotGraphWalk.scan(snapshot,new Store(),1000,1000,(n,s,e)->{}));eq(0,source.activeCursors);
        }
        for(long nodes:new long[]{0,1,10}) {
            var s=new Source();var fixture=new Fixture(s);var store=new Store();
            try(var snapshot=AirSnapshot.attach(s,fixture.root)) {fails(SnapshotGraphWalk.Limit.class,()->SnapshotGraphWalk.scan(snapshot,store,nodes,1000,(n,k,e)->{}));}
            eq(true,store.closed);eq(0L,store.claimed);
        }
        var s=new Source();var fixture=new Fixture(s);var store=new Store();
        try(var snapshot=AirSnapshot.attach(s,fixture.root)) {fails(SnapshotGraphWalk.Limit.class,()->SnapshotGraphWalk.scan(snapshot,store,1000,1,(n,k,e)->{}));}
        eq(0L,store.claimed);
        var broken=new Source();var fixture2=new Fixture(broken);var owner=new Store();owner.closeFailure=true;owner.leaseFailure=true;
        var primary=new IllegalStateException("visitor failure");
        try(var snapshot=AirSnapshot.attach(broken,fixture2.root)) {
            eq(primary,fails(IllegalStateException.class,()->SnapshotGraphWalk.scan(snapshot,owner,1000,1000,(n,k,e)->{throw primary;})));
            eq(2,primary.getSuppressed().length);eq(AirShape.PUBLICATION,snapshot.shape(fixture2.root));
        }
        eq(0L,owner.claimed);eq(true,owner.closed);
        for(int remaining=0;remaining<30;remaining++) {
            var input=new Source();var fixture3=new Fixture(input);var port=new Store();port.remaining=remaining;
            try(var snapshot=AirSnapshot.attach(input,fixture3.root)) {
                eq(port.failure,fails(IllegalStateException.class,()->SnapshotGraphWalk.scan(snapshot,port,1000,1000,(n,k,e)->{})));
                eq(0,input.activeCursors);eq(AirShape.PUBLICATION,snapshot.shape(fixture3.root));
            }
            eq(true,port.closed);eq(0L,port.claimed);
        }
        var denied=new Source();var fixture4=new Fixture(denied);var port=new Store();port.denyClaim=true;
        try(var snapshot=AirSnapshot.attach(denied,fixture4.root)) {eq(port.failure,fails(IllegalStateException.class,()->SnapshotGraphWalk.scan(snapshot,port,1000,1000,(n,k,e)->{})));}
        eq(true,port.closed);eq(0L,port.claimed);
    }
    static void malformedStorageAndCursorFailureNeverProduceCounts() {
        for(int fault=0;fault<5;fault++) {
            var source=new Source();var fixture=new Fixture(source);var store=new Store();
            switch(fault) {
                case 0 -> {var root=source.nodes.get(fixture.root);source.nodes.put(fixture.root,new Node(root.shape(),Arrays.copyOf(root.children(),11),null,0));}
                case 1 -> source.replaceField(fixture.root,1,source.text("version is not a text field"));
                case 2 -> {
                    long coverage=source.nodes.get(fixture.root).children()[9];
                    source.replaceField(coverage,0,source.scalar(AirShape.EVIDENCE_INVENTORY_STATUS,99));
                }
                case 3 -> {
                    long artifactId=source.record(AirShape.IDS_ARTIFACT_ID,fixture.id,source.text("a"));
                    long artifact=source.record(AirShape.ORIGINS_ARTIFACT,artifactId,source.text("a"),source.optional(source.text("x"),source.text("y")));
                    source.replaceField(fixture.root,3,source.list(artifact));
                }
                case 4 -> {
                    long origin=source.record(AirShape.IDS_ORIGIN_ID,fixture.id,source.text("o"));
                    long artifact=source.record(AirShape.IDS_ARTIFACT_ID,fixture.id,source.text("a"));
                    long written=source.record(AirShape.ORIGINS_WRITTEN,origin,artifact,source.optional(),fixture.empty,source.scalar(AirShape.BOOLEAN,2));
                    source.replaceField(fixture.root,8,source.list(written));
                }
                default -> throw new AssertionError();
            }
            try(var snapshot=AirSnapshot.attach(source,fixture.root)) {
                fails(IllegalStateException.class,()->SnapshotGraphWalk.scan(snapshot,store,1000,1000,(n,k,e)->{}));
                eq(AirShape.PUBLICATION,snapshot.shape(fixture.root));eq(0,source.activeCursors);
            }
            eq(0L,store.claimed);eq(true,store.closed);
        }
        for(boolean excessive:new boolean[]{false,true}) {
            var source=new Source();var fixture=new Fixture(source);
            long cap=source.record(AirShape.CAPABILITIES_CAPABILITY,source.text("c"),source.text("1"));
            long list=source.list(cap),manifest=source.nodes.get(fixture.root).children()[2];
            source.replaceField(manifest,0,list);source.badLength=list;source.lengthDelta=excessive?1:-1;
            try(var snapshot=AirSnapshot.attach(source,fixture.root)) {
                fails(IllegalStateException.class,()->SnapshotGraphWalk.scan(snapshot,new Store(),1000,1000,(n,k,e)->{}));eq(0,source.activeCursors);
            }
        }
        var source=new Source();var fixture=new Fixture(source);
        long scope=source.scalar(AirShape.PROOFS_PUBLICATION_DOMAIN,0),premise=fixture.premise(scope);
        source.replaceField(fixture.root,11,source.list(premise));source.failNode=premise;source.closeCursorFailure=true;source.failingCursor=source.nodes.get(fixture.root).children()[11];
        var port=new Store();
        try(var snapshot=AirSnapshot.attach(source,fixture.root)) {
            eq(source.failure,fails(IllegalStateException.class,()->SnapshotGraphWalk.scan(snapshot,port,1000,1000,(n,k,e)->{})));
            eq(1,source.failure.getSuppressed().length);eq(source.cleanup,source.failure.getSuppressed()[0]);
            eq(0,source.activeCursors);eq(AirShape.PUBLICATION,snapshot.shape(fixture.root));
        }
        eq(0L,port.claimed);eq(true,port.closed);
    }
    static final class Fixture {
        final Source source;final long root,id,empty;
        Fixture(Source source) {
            this.source=source;id=source.record(AirShape.IDS_PUBLICATION_ID,source.text("P"));empty=source.list();
            long zero=source.integer("0");long version=source.record(AirShape.SEMANTIC_VERSION,source.integer("2"),zero,zero);
            long caps=source.record(AirShape.CAPABILITIES_MANIFEST,empty,empty);
            long coverage=source.record(AirShape.EVIDENCE_COVERAGE,source.scalar(AirShape.EVIDENCE_INVENTORY_STATUS,0),source.record(AirShape.SCOPES_PUBLICATION_SCOPE,id),empty,empty);
            root=source.record(AirShape.PUBLICATION,id,version,caps,empty,empty,empty,empty,empty,empty,coverage,empty,empty);
        }
        long premise(long scope) {
            long origin=source.record(AirShape.IDS_ORIGIN_ID,id,source.text("o"));
            long unit=source.record(AirShape.IDS_UNIT_ID,id,source.text("u"));
            long entry=source.record(AirShape.IDS_ENTRY_ID,unit,source.text("e"));
            long subject=source.record(AirShape.PROOFS_PARAMETER_DOMAIN,entry,source.integer("0"));
            return source.record(AirShape.PROOFS_PREMISE,source.record(AirShape.IDS_PREMISE_ID,id,source.text("p")),source.text("a"),source.text("j"),origin,source.record(AirShape.PROOFS_SAME_DOMAIN,subject,subject,scope));
        }
    }
    private record Node(AirShape shape,long[] children,String text,long scalar) { }
    static final class Source implements AirSnapshot.Source {
        final Map<Long,Node> nodes=new HashMap<>();long next=1L<<42,badLength,failNode,failingCursor;int cursors,activeCursors,maxCursors,indexedListReads,lengthDelta;
        boolean closeCursorFailure;
        final IllegalStateException failure=new IllegalStateException("source read failure"),cleanup=new IllegalStateException("cursor cleanup failure");
        long add(Node node){long key=++next;nodes.put(key,node);return key;}
        long text(String text){return add(new Node(AirShape.TEXT,null,text,0));}long integer(String text){return add(new Node(AirShape.INTEGER,null,text,0));}
        long scalar(AirShape shape,long scalar){return add(new Node(shape,null,null,scalar));}
        long record(AirShape shape,long...children){return add(new Node(shape,children,null,0));}
        long list(long...children){return record(AirShape.LIST,children);}long optional(long...children){return record(AirShape.OPTIONAL,children);}
        void replaceField(long node,int field,long replacement){nodes.get(node).children()[field]=replacement;}
        public AirShape shape(long node){if(node==failNode)throw failure;return nodes.get(node).shape();}
        public long length(long node){var n=nodes.get(node);return (n.text()!=null?n.text().length():n.children()!=null?n.children().length:0)+(node==badLength?lengthDelta:0);}
        public long child(long node,long at){if(shape(node)==AirShape.LIST){indexedListReads++;throw new AssertionError("indexed list read");}return nodes.get(node).children()[Math.toIntExact(at)];}
        public long scalar(long node){return nodes.get(node).scalar();}
        public int characters(long node,long at,char[] out,int start,int count){nodes.get(node).text().getChars(Math.toIntExact(at),Math.toIntExact(at)+count,out,start);return count;}
        public AirSnapshot.Elements elements(long node) {
            cursors++;activeCursors++;maxCursors=Math.max(maxCursors,activeCursors);
            return new AirSnapshot.Elements(){int at;long current;boolean closed;public boolean advance(){if(at==nodes.get(node).children().length)return false;current=nodes.get(node).children()[at++];return true;}public long value(){return current;}public void close(){if(!closed){closed=true;activeCursors--;if(closeCursorFailure&&node==failingCursor)throw cleanup;}}};
        }
        public void close(){eq(0,activeCursors);}
    }
    private record Context(long node,int element) { }
    static final class Store implements SnapshotGraphWalk.Storage {
        final ArrayList<long[]> stack=new ArrayList<>();final Set<Context> done=new HashSet<>();final Set<Long> active=new HashSet<>();
        final IllegalStateException failure=new IllegalStateException("storage failure");long[] current;long claimed,remaining=Long.MAX_VALUE;boolean closed,closeFailure,leaseFailure,denyClaim;
        private void work(){if(remaining--==0)throw failure;}
        public long size(){work();return stack.size();}
        public void push(long node,int element,long depth,boolean exit){work();stack.add(new long[]{node,element,depth,exit?1:0});}
        public void reverse(long from){work();Collections.reverse(stack.subList(Math.toIntExact(from),stack.size()));}
        public boolean advance(){work();if(stack.isEmpty())return false;current=stack.removeLast();return true;}
        public long node(){return current[0];}public int element(){return Math.toIntExact(current[1]);}public long depth(){return current[2];}public boolean exiting(){return current[3]!=0;}
        public boolean active(long node){work();return active.contains(node);}public void active(long node,boolean value){work();if(value)active.add(node);else active.remove(node);}
        public boolean completed(long node,int context){work();return done.contains(new Context(node,context));}public void complete(long node,int context){work();done.add(new Context(node,context));}
        public AirSnapshotBuilder.Lease claim(long bytes){if(denyClaim)throw failure;claimed+=bytes;return ()->{claimed-=bytes;if(leaseFailure)throw new IllegalStateException("lease close failure");};}
        public void close(){closed=true;stack.clear();done.clear();active.clear();if(closeFailure)throw new IllegalStateException("owner close failure");}
    }
    private static <T extends Throwable>T fails(Class<T> type,Runnable action){try{action.run();}catch(Throwable error){if(type.isInstance(error))return type.cast(error);throw new AssertionError(error);}throw new AssertionError("missing "+type);}
    private static void eq(Object expected,Object actual){if(!Objects.equals(expected,actual))throw new AssertionError("expected="+expected+" actual="+actual);}
}
