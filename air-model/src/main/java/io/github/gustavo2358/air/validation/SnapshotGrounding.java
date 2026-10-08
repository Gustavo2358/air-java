package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.AirShape;
import io.github.gustavo2358.air.model.AirSnapshot;
import io.github.gustavo2358.air.model.AirSnapshotBuilder;
import java.util.Objects;
import static io.github.gustavo2358.air.validation.SnapshotDeclarations.Fact.NODE;

/** Shared least positive fixed point for location grounding; separate from reference validity. */
public final class SnapshotGrounding implements SnapshotGraphWalk.Visitor,AutoCloseable {
    /**
     * Empty on transfer. Rows are exact immutable source-node equations, not nominal identities.
     * expand ensures a row and marks it expanded once. link ensures both rows and retains EACH
     * reverse child->parent occurrence, including references to not-yet-expanded equations.
     * prove changes false->true at most once. All row/index/reverse/FIFO state must be managed
     * and spillable. A drained primitive dependent cursor is required before selecting another.
     * freeze seals a fully collected/saturated relation; grounded then performs required lookup
     * without insertion and rejects missing equations. Close releases scratch, not borrowed input.
     */
    public interface Storage extends AutoCloseable {
        boolean expand(long node);
        void link(long child,long parent);
        boolean prove(long node);
        boolean grounded(long node);
        void enqueue(long node);
        boolean advance(); long node();
        void dependents(long node);
        boolean advanceDependent(); long dependent();
        /** Seal all equations/edges before propagation; reject any unexpanded linked row. */
        void start();
        void freeze();
        AirSnapshotBuilder.Lease claim(long bytes);
        @Override void close();
    }
    public record Counts(long nodes,long edges,long grounded) { }
    private AirSnapshot snapshot;
    private SnapshotDeclarations declarations;
    private Storage storage;
    private AirSnapshotBuilder.Lease control;
    private long nodes,edges,grounded;
    private boolean ready,failed;

    private SnapshotGrounding(AirSnapshot snapshot,SnapshotDeclarations declarations,Storage storage) {
        this.storage=Objects.requireNonNull(storage);
        try {
            this.snapshot=Objects.requireNonNull(snapshot);this.declarations=Objects.requireNonNull(declarations);
            control=Objects.requireNonNull(storage.claim(256));
        } catch(RuntimeException|Error failure){closeSuppressed(failure);throw failure;}
    }
    /** Transfers both scratch ports; queries are published only after collection and saturation. */
    public static SnapshotGrounding build(AirSnapshot snapshot,SnapshotDeclarations declarations,
                                          Storage storage,SnapshotGraphWalk.Storage graphStorage,long maximumNodes,long maximumDepth) {
        SnapshotGrounding result;
        try {result=new SnapshotGrounding(snapshot,declarations,storage);}
        catch(RuntimeException|Error failure) {
            if(graphStorage!=null)try{graphStorage.close();}catch(RuntimeException|Error cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}
            throw failure;
        }
        try {
            SnapshotGraphWalk.scan(snapshot,graphStorage,maximumNodes,maximumDepth,result);
            result.saturate();result.storage.freeze();result.ready=true;return result;
        } catch(RuntimeException|Error failure){result.closeSuppressed(failure);throw failure;}
    }
    @Override public void node(long node,AirShape shape,AirShape element) {
        available();if(ready)throw new IllegalStateException("grounding collection is sealed");
        try {
            if(!equation(shape)||!storage.expand(node))return;
            nodes=Math.incrementExact(nodes);
            switch(shape) {
                case MEMORY_OBJECT_DECLARATION -> link(node,snapshot.field(node,shape,3));
                case MEMORY_CELL_BINDING,MEMORY_VIEW_BINDING,SCOPES_ALL_MEMORY,SCOPES_VISIBLE_MEMORY -> seed(node);
                case MEMORY_ALIAS_BINDING -> linkObject(node,snapshot.field(node,shape,0));
                case MEMORY_UNKNOWN_BINDING,SCOPES_WITHIN_MEMORY -> link(node,snapshot.field(node,shape,0));
                case MEMORY_ALTERNATIVES_BINDING -> {
                    links(node,snapshot.field(node,shape,0),AirShape.MEMORY_BINDING,false);
                    link(node,snapshot.field(node,shape,1));
                }
                case SCOPES_OBJECTS_MEMORY -> links(node,snapshot.field(node,shape,0),AirShape.IDS_OBJECT_ID,true);
                case SCOPES_MEMORY_UNION -> links(node,snapshot.field(node,shape,0),AirShape.SCOPES_MEMORY_SCOPE,false);
                case SCOPES_STORAGE_MEMORY -> {if(snapshot.size(snapshot.field(node,shape,0))!=0)seed(node);}
                case SCOPES_NO_MEMORY -> { }
                default -> throw new IllegalStateException("unhandled grounding equation");
            }
        } catch(RuntimeException|Error failure){failed=true;throw failure;}
    }
    private static boolean equation(AirShape shape) {
        return switch(shape) {
            case MEMORY_OBJECT_DECLARATION,MEMORY_CELL_BINDING,MEMORY_VIEW_BINDING,MEMORY_ALIAS_BINDING,
                 MEMORY_UNKNOWN_BINDING,MEMORY_ALTERNATIVES_BINDING,SCOPES_WITHIN_MEMORY,SCOPES_NO_MEMORY,
                 SCOPES_ALL_MEMORY,SCOPES_VISIBLE_MEMORY,SCOPES_OBJECTS_MEMORY,SCOPES_MEMORY_UNION,SCOPES_STORAGE_MEMORY -> true;
            default -> false;
        };
    }
    private void links(long parent,long collection,AirShape element,boolean objects) {
        try(var cursor=snapshot.elements(collection,element)) {
            while(cursor.advance()){long child=cursor.value();if(objects)linkObject(parent,child);else link(parent,child);}
        }
    }
    private void linkObject(long parent,long reference) {
        long declaration=declarations.fact(reference,NODE);
        if(declaration!=0&&snapshot.shape(declaration)==AirShape.MEMORY_OBJECT_DECLARATION)link(parent,declaration);
    }
    private void link(long parent,long child){storage.link(child,parent);edges=Math.incrementExact(edges);}
    private void seed(long node){if(storage.prove(node)){grounded=Math.incrementExact(grounded);storage.enqueue(node);}}
    private void saturate() {
        storage.start();
        while(storage.advance()) {
            long child=storage.node();storage.dependents(child);
            while(storage.advanceDependent()) {
                long parent=storage.dependent();
                if(storage.prove(parent)){grounded=Math.incrementExact(grounded);storage.enqueue(parent);}
            }
        }
        if(grounded>nodes)throw new IllegalStateException("grounding equation count disagreement");
    }
    public Counts counts(){queryable();return new Counts(nodes,edges,grounded);}
    /** Missing object is false and still requires independent I-02 diagnosis. */
    public boolean groundedObject(long identity) {
        queryable();
        try {
            if(snapshot.shape(identity)!=AirShape.IDS_OBJECT_ID)throw new IllegalArgumentException("ObjectId required");
            long node=declarations.fact(identity,NODE);
            return node!=0&&snapshot.shape(node)==AirShape.MEMORY_OBJECT_DECLARATION&&storage.grounded(node);
        } catch(RuntimeException|Error failure){failed=true;throw failure;}
    }
    /** Only collected binding/scope/object equations are queryable; no silent insertion on a miss. */
    public boolean groundedNode(long node) {
        queryable();try{return storage.grounded(node);}catch(RuntimeException|Error failure){failed=true;throw failure;}
    }
    private void available(){if(storage==null||failed)throw new IllegalStateException("grounding owner is closed or aborted");}
    private void queryable(){available();if(!ready)throw new IllegalStateException("grounding relation is not saturated");}
    private void closeSuppressed(Throwable failure){try{close();}catch(RuntimeException|Error cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}}
    @Override public void close() {
        if(storage==null)return;Storage owner=storage;storage=null;Throwable failure=null;
        try{owner.close();}catch(RuntimeException|Error cleanup){failure=cleanup;}
        try{if(control!=null)control.close();}catch(RuntimeException|Error cleanup){if(failure==null)failure=cleanup;else if(failure!=cleanup)failure.addSuppressed(cleanup);}
        finally{control=null;snapshot=null;declarations=null;ready=false;}
        if(failure instanceof RuntimeException error)throw error;if(failure instanceof Error error)throw error;
    }
}
