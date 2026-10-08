package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.AirShape;
import io.github.gustavo2358.air.model.AirSnapshot;
import io.github.gustavo2358.air.model.AirSnapshotBuilder;
import java.util.Objects;
import static io.github.gustavo2358.air.validation.SnapshotDeclarations.Fact.*;

/** Exact nominal Kahn residuals; unrelated to physical storage cycles or executable grounding. */
public final class SnapshotNominalCycles {
    private SnapshotNominalCycles() { }
    public enum Rule {
        ORIGIN("I-36"),UNIT("I-01"),ALIAS("I-12");
        private final String invariant;
        Rule(String invariant){this.invariant=invariant;}
        public String invariant(){return invariant;}
    }
    @FunctionalInterface public interface Issues { void report(Rule rule,long identity,long node); }
    public record Counts(long nodes,long edges,long removed,long residual) { }
    /**
     * Empty on transfer; complete borrowed identity keys are literal words, not row addresses.
     * All cardinality-dependent nodes/degrees/reverse edge occurrences/FIFO must be managed and
     * spillable. Every link occurrence increments child degree and occurs once in parent's reverse
     * adjacency. define/link precede scheduling; children exposes one owned primitive cursor,
     * drained before another parent is selected. Close releases that cursor and is idempotent.
     */
    public interface Storage extends AutoCloseable {
        void define(long key);
        void link(long parent,long child);
        long degree(long key);
        /** Exact positive degree decrement; reject missing node/underflow/overflow. */
        long decrement(long key);
        void enqueue(long key);
        boolean advance();
        long node();
        void children(long key);
        boolean advanceChild();
        long child();
        AirSnapshotBuilder.Lease claim(long bytes);
        @Override void close();
    }
    /**
     * Borrows frozen input, complete keys and first-declaration inventory; transfers scratch.
     * Absent/other-family parents are excluded here and require separate reference diagnosis.
     * Reports all positive-degree residuals (including dependents of a cycle), in family then
     * declaration order. Counts after successful cleanup are not a validity certificate.
     */
    @SuppressWarnings("try")
    public static Counts scan(AirSnapshot snapshot,SnapshotIdentityKeys keys,SnapshotDeclarations declarations,
                              Storage storage,Issues issues) {
        try(Storage owner=Objects.requireNonNull(storage)) {
            Objects.requireNonNull(snapshot);Objects.requireNonNull(keys);Objects.requireNonNull(declarations);Objects.requireNonNull(issues);
            try(var control=Objects.requireNonNull(owner.claim(256))) {
                long size=declarations.entities(),nodes=0,edges=0,removed=0,residual=0;
                for(long at=0;at<size;at++) {
                    long node=declarations.declaration(at,NODE);
                    if(kind(snapshot.shape(node))!=null){owner.define(keys.key(declarations.declaration(at,IDENTITY)));nodes=Math.incrementExact(nodes);}
                }
                for(long at=0;at<size;at++) {
                    long node=declarations.declaration(at,NODE);AirShape shape=snapshot.shape(node);Rule kind=kind(shape);
                    if(kind==null)continue;long child=keys.key(declarations.declaration(at,IDENTITY));
                    if(shape==AirShape.ORIGINS_DERIVED) {
                        try(var inputs=snapshot.elements(snapshot.field(node,shape,1),AirShape.IDS_ORIGIN_ID)) {
                            while(inputs.advance())if(link(snapshot,keys,declarations,owner,kind,inputs.value(),child))edges=Math.incrementExact(edges);
                        }
                    } else if(shape==AirShape.UNIT) {
                        long optional=snapshot.field(node,shape,1);
                        if(snapshot.size(optional)!=0&&link(snapshot,keys,declarations,owner,kind,snapshot.element(optional,AirShape.IDS_UNIT_ID,0),child))edges=Math.incrementExact(edges);
                    } else if(shape==AirShape.MEMORY_OBJECT_DECLARATION) {
                        long binding=snapshot.field(node,shape,3);
                        if(snapshot.shape(binding)==AirShape.MEMORY_ALIAS_BINDING&&link(snapshot,keys,declarations,owner,kind,snapshot.field(binding,AirShape.MEMORY_ALIAS_BINDING,0),child))edges=Math.incrementExact(edges);
                    }
                }
                // Seed only after every degree/reverse occurrence exists, including later parents.
                for(long at=0;at<size;at++) {
                    long node=declarations.declaration(at,NODE);
                    if(kind(snapshot.shape(node))!=null){long key=keys.key(declarations.declaration(at,IDENTITY));if(owner.degree(key)==0)owner.enqueue(key);}
                }
                while(owner.advance()) {
                    long parent=owner.node();removed=Math.incrementExact(removed);owner.children(parent);
                    while(owner.advanceChild()){long child=owner.child();if(owner.decrement(child)==0)owner.enqueue(child);}
                }
                // Avoid a resident enum-values array and retain historical family/subject ordering.
                residual=Math.addExact(residual,report(snapshot,keys,declarations,owner,issues,Rule.ORIGIN,size));
                residual=Math.addExact(residual,report(snapshot,keys,declarations,owner,issues,Rule.UNIT,size));
                residual=Math.addExact(residual,report(snapshot,keys,declarations,owner,issues,Rule.ALIAS,size));
                if(Math.addExact(removed,residual)!=nodes)throw new IllegalStateException("nominal cycle frontier/count disagreement");
                return new Counts(nodes,edges,removed,residual);
            }
        }
    }
    private static boolean link(AirSnapshot snapshot,SnapshotIdentityKeys keys,SnapshotDeclarations declarations,
                                Storage owner,Rule kind,long reference,long child) {
        long target=declarations.fact(reference,NODE);
        if(target==0||kind(snapshot.shape(target))!=kind)return false;
        owner.link(keys.key(reference),child);return true;
    }
    private static long report(AirSnapshot snapshot,SnapshotIdentityKeys keys,SnapshotDeclarations declarations,
                                Storage owner,Issues issues,Rule wanted,long size) {
        long residual=0;
        for(long at=0;at<size;at++) {
            long node=declarations.declaration(at,NODE);
            if(kind(snapshot.shape(node))==wanted) {
                long identity=declarations.declaration(at,IDENTITY);
                if(owner.degree(keys.key(identity))>0){issues.report(wanted,identity,node);residual=Math.incrementExact(residual);}
            }
        }
        return residual;
    }
    private static Rule kind(AirShape shape) {
        return switch(shape) {
            case ORIGINS_WRITTEN,ORIGINS_DERIVED,ORIGINS_CONTRACTUAL,ORIGINS_UNAVAILABLE->Rule.ORIGIN;
            case UNIT->Rule.UNIT;
            case MEMORY_OBJECT_DECLARATION->Rule.ALIAS;
            default->null;
        };
    }
}
