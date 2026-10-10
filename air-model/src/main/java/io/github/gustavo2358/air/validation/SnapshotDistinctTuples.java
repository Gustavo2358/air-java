package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.AirSnapshotBuilder;
import java.util.Objects;

/** Exact list-local tuple uniqueness backed by caller-owned managed storage. */
public final class SnapshotDistinctTuples implements AutoCloseable {
    public interface Storage extends AutoCloseable {
        boolean first(long list,long family,long first,long second);
        AirSnapshotBuilder.Lease claim(long bytes);
        @Override void close();
    }
    private Storage storage;private AirSnapshotBuilder.Lease control;private boolean failed;
    public SnapshotDistinctTuples(Storage storage) {
        this.storage=Objects.requireNonNull(storage);
        try{control=Objects.requireNonNull(storage.claim(128));}catch(RuntimeException|Error failure){closeSuppressed(failure);throw failure;}
    }
    public boolean first(long list,long family,long first,long second) {
        open();if(list<=0||family<=0||first<=0||second<0)throw new IllegalArgumentException("positive tuple coordinates required");
        try{return storage.first(list,family,first,second);}catch(RuntimeException|Error failure){failed=true;throw failure;}
    }
    private void open(){if(storage==null||failed)throw new IllegalStateException("distinct tuple relation is closed or aborted");}
    private void closeSuppressed(Throwable failure){try{close();}catch(RuntimeException|Error cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}}
    @Override public void close(){Storage owner=storage;storage=null;AirSnapshotBuilder.Lease lease=control;control=null;Throwable failure=null;try{if(owner!=null)owner.close();}catch(RuntimeException|Error cleanup){failure=cleanup;}try{if(lease!=null)lease.close();}catch(RuntimeException|Error cleanup){if(failure==null)failure=cleanup;else if(failure!=cleanup)failure.addSuppressed(cleanup);}if(failure instanceof RuntimeException error)throw error;if(failure instanceof Error error)throw error;}
}
