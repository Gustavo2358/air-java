package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.AirShape;
import io.github.gustavo2358.air.model.AirSnapshot;
import io.github.gustavo2358.air.model.AirSnapshotBuilder;
import java.util.Objects;

/**
 * Ordered iterative grammar walk over borrowed typed storage. No model reconstruction,
 * reference/type/capability admission or validity certificate. The transferred port owns all
 * cardinality-dependent frontier, ancestry and contextual memo data and supplies managed spill.
 */
public final class SnapshotGraphWalk {
    private SnapshotGraphWalk() { }
    /** Empty on transfer, exact source/context equality; a collection context is element ordinal+1. */
    public interface Storage extends AutoCloseable {
        long size();
        void push(long node,int element,long depth,boolean exit);
        /** Reverse only the newly appended tasks so LIFO descent preserves declared order. */
        void reverse(long from);
        boolean advance();
        long node();
        int element();
        long depth();
        boolean exiting();
        boolean active(long node);
        void active(long node,boolean value);
        boolean completed(long node,int context);
        void complete(long node,int context);
        AirSnapshotBuilder.Lease claim(long bytes);
        @Override void close();
    }
    @FunctionalInterface public interface Visitor {
        /** Called once per node/collection context, before children; no ancestor cursor is live. */
        void node(long node,AirShape shape,AirShape element);
    }
    public record Counts(long nodes,long edges) { }
    public enum LimitKind { NODES, EXPANDED_DEPTH }
    public static final class Limit extends RuntimeException {
        private static final long serialVersionUID=1L;
        private final LimitKind kind;
        Limit(LimitKind kind){super("typed AIR graph walk limit: "+kind);this.kind=kind;}
        public LimitKind kind(){return kind;}
    }
    /** Storage graph cycle; distinct from nominal AIR reference cycles checked by later passes. */
    public static final class Cycle extends IllegalStateException {
        private static final long serialVersionUID=1L;
        private final long node;
        Cycle(long node){super("cyclic typed AIR storage at source handle "+node);this.node=node;}
        public long node(){return node;}
    }
    /**
     * Limits bound distinct expanded contexts and actual expanded DFS depth, not all unfolded DAG
     * paths. Incoming edges are typed before memo reuse. Atom content is the visitor's obligation;
     * this walk checks shape/cardinality/scalar grammar, never constructor-local semantic rules.
     */
    @SuppressWarnings("try") // The fixed-capacity lease is held solely for its lifetime.
    public static Counts scan(AirSnapshot snapshot,Storage storage,long maximumNodes,long maximumDepth,Visitor visitor) {
        try(Storage owner=Objects.requireNonNull(storage)) {
            Objects.requireNonNull(snapshot);Objects.requireNonNull(visitor);
            if(maximumNodes<0||maximumDepth<0)throw new IllegalArgumentException("nonnegative graph walk limits required");
            try(var control=Objects.requireNonNull(owner.claim(4096))) {
                AirShape[] shapes=AirShape.values();long nodes=0,edges=0;
                owner.push(snapshot.root(),0,1,false);
                while(owner.advance()) {
                    long node=owner.node(),depth=owner.depth();int context=owner.element();
                    if(owner.exiting()){owner.active(node,false);owner.complete(node,context);continue;}
                    AirShape shape=snapshot.shape(node);
                    if(owner.active(node))throw new Cycle(node);
                    if(depth>maximumDepth)throw new Limit(LimitKind.EXPANDED_DEPTH);
                    if(owner.completed(node,context))continue;
                    if(nodes==maximumNodes)throw new Limit(LimitKind.NODES);nodes++;
                    AirShape element=context==0?null:shapes[context-1];
                    boolean container=shape==AirShape.LIST||shape==AirShape.OPTIONAL;
                    if(container!=(element!=null))throw new IllegalStateException("missing or unexpected typed collection context");
                    owner.active(node,true);owner.push(node,context,depth,true);
                    visitor.node(node,shape,element);long from=owner.size();
                    switch(shape.form()) {
                        case RECORD -> {
                            for(int at=0;at<shape.fieldCount();at++) {
                                var slot=shape.field(at);long child=snapshot.field(node,shape,at);
                                owner.push(child,slot.element()==null?0:slot.element().ordinal()+1,Math.incrementExact(depth),false);
                                edges=Math.incrementExact(edges);
                            }
                        }
                        case LIST,OPTIONAL -> {
                            try(var cursor=snapshot.elements(node,element)) {
                                while(cursor.advance()) {
                                    owner.push(cursor.value(),0,Math.incrementExact(depth),false);edges=Math.incrementExact(edges);
                                }
                            }
                        }
                        case BOOLEAN,SMALL_INTEGER,ENUM -> snapshot.scalar(node);
                        case TEXT,INTEGER -> snapshot.characterCount(node);
                        case UNION -> throw new IllegalStateException("concrete AIR graph node required");
                    }
                    owner.reverse(from);
                }
                return new Counts(nodes,edges);
            }
        }
    }
}
