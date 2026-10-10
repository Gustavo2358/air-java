package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.AirSnapshotBuilder;
import java.util.Objects;

/** Managed exact contextual worklist for iterative snapshot rules. */
public final class SnapshotContextWalk implements AutoCloseable {
    public interface Storage extends AutoCloseable {
        boolean schedule(long node,long owner,long kind,long context,long depth);
        boolean advance();long node();long owner();long kind();long context();long depth();
        AirSnapshotBuilder.Lease claim(long bytes);
        @Override void close();
    }
    @FunctionalInterface public interface Visitor {void visit(long node,long owner,long kind,long context,long depth);}
    private Storage storage;private AirSnapshotBuilder.Lease control;private boolean draining,failed;
    public SnapshotContextWalk(Storage storage){this.storage=Objects.requireNonNull(storage);try{control=Objects.requireNonNull(storage.claim(256));}catch(RuntimeException|Error failure){closeSuppressed(failure);throw failure;}}
    public boolean schedule(long node,long owner,long kind,long context,long depth){open();if(node<=0||owner<=0||kind<=0||context<0||depth<0)throw new IllegalArgumentException("invalid contextual work item");try{return storage.schedule(node,owner,kind,context,depth);}catch(RuntimeException|Error failure){failed=true;throw failure;}}
    public void drain(long maximumDepth,Visitor visitor){open();if(draining)throw new IllegalStateException("contextual worklist already draining");Objects.requireNonNull(visitor);draining=true;try{while(storage.advance()){long depth=storage.depth();if(depth>maximumDepth)throw new SnapshotGraphWalk.Limit(SnapshotGraphWalk.LimitKind.EXPANDED_DEPTH);visitor.visit(storage.node(),storage.owner(),storage.kind(),storage.context(),depth);}}catch(RuntimeException|Error failure){failed=true;throw failure;}finally{draining=false;}}
    private void open(){if(storage==null||failed)throw new IllegalStateException("contextual worklist is closed or aborted");}
    private void closeSuppressed(Throwable failure){try{close();}catch(RuntimeException|Error cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}}
    @Override public void close(){if(draining)throw new IllegalStateException("cannot close contextual worklist during callback");Storage owner=storage;storage=null;AirSnapshotBuilder.Lease lease=control;control=null;Throwable failure=null;try{if(owner!=null)owner.close();}catch(RuntimeException|Error cleanup){failure=cleanup;}try{if(lease!=null)lease.close();}catch(RuntimeException|Error cleanup){if(failure==null)failure=cleanup;else if(failure!=cleanup)failure.addSuppressed(cleanup);}if(failure instanceof RuntimeException error)throw error;if(failure instanceof Error error)throw error;}
}
