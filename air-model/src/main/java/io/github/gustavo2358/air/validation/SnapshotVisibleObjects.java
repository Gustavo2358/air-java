package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.AirSnapshot;
import io.github.gustavo2358.air.model.AirSnapshotBuilder;
import java.util.Objects;

import static io.github.gustavo2358.air.model.AirShape.*;

/** Exact explicitly-visible object relation shared by all operand/resource checks. */
public final class SnapshotVisibleObjects implements AutoCloseable {
    /** Empty on transfer; exact pair membership and all cardinality-dependent state are managed. */
    public interface Storage extends AutoCloseable {
        /** True only when the exact complete (UnitId,ObjectId) pair was absent. */
        boolean add(long unitKey,long objectKey);
        boolean contains(long unitKey,long objectKey);
        /** True only for the first executable-grounding query for this exact ObjectId. */
        boolean firstGrounding(long objectKey);
        AirSnapshotBuilder.Lease claim(long bytes);
        @Override void close();
    }
    public record Counts(long units,long occurrences,long distinct) { }
    private AirSnapshot snapshot;
    private SnapshotIdentityKeys keys;
    private Storage storage;
    private AirSnapshotBuilder.Lease control;
    private long units,occurrences,distinct;
    private boolean failed;

    /** Transfers storage and builds the complete relation before publishing queries. */
    public static SnapshotVisibleObjects build(AirSnapshot snapshot,SnapshotIdentityKeys keys,Storage storage) {
        var result=new SnapshotVisibleObjects(snapshot,keys,storage);
        try{result.index();return result;}catch(RuntimeException|Error failure){result.closeSuppressed(failure);throw failure;}
    }
    private SnapshotVisibleObjects(AirSnapshot snapshot,SnapshotIdentityKeys keys,Storage storage) {
        this.snapshot=Objects.requireNonNull(snapshot);this.keys=Objects.requireNonNull(keys);this.storage=Objects.requireNonNull(storage);
        try{snapshot.root();control=Objects.requireNonNull(storage.claim(256));}
        catch(RuntimeException|Error failure){closeSuppressed(failure);throw failure;}
    }
    private void index() {
        try(var rows=snapshot.elements(snapshot.field(snapshot.root(),PUBLICATION,4),UNIT)) {
            while(rows.advance()) {
                long unit=rows.value(),id=snapshot.field(unit,UNIT,0),unitKey=keys.key(id);units=Math.incrementExact(units);
                try(var objects=snapshot.elements(snapshot.field(unit,UNIT,2),MEMORY_OBJECT_DECLARATION)) {
                    while(objects.advance())add(unitKey,snapshot.field(objects.value(),MEMORY_OBJECT_DECLARATION,0));
                }
                try(var visible=snapshot.elements(snapshot.field(unit,UNIT,3),IDS_OBJECT_ID)) {
                    while(visible.advance())add(unitKey,visible.value());
                }
            }
        } catch(RuntimeException|Error failure){failed=true;throw failure;}
    }
    private void add(long unit,long object){occurrences=Math.incrementExact(occurrences);if(storage.add(unit,keys.key(object)))distinct=Math.incrementExact(distinct);}
    public boolean contains(long unit,long object) {
        open();
        try {
            if(snapshot.shape(unit)!=IDS_UNIT_ID||snapshot.shape(object)!=IDS_OBJECT_ID)throw new IllegalArgumentException("UnitId and ObjectId required");
            return storage.contains(keys.key(unit),keys.key(object));
        } catch(RuntimeException|Error failure){failed=true;throw failure;}
    }
    public Counts counts(){open();return new Counts(units,occurrences,distinct);}
    public boolean firstGrounding(long object) {
        open();
        try{if(snapshot.shape(object)!=IDS_OBJECT_ID)throw new IllegalArgumentException("ObjectId required");return storage.firstGrounding(keys.key(object));}
        catch(RuntimeException|Error failure){failed=true;throw failure;}
    }
    private void open(){if(storage==null||failed)throw new IllegalStateException("visible object relation is closed or aborted");}
    private void closeSuppressed(Throwable failure){try{close();}catch(RuntimeException|Error cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}}
    @Override public void close() {
        Storage owner=storage;storage=null;AirSnapshotBuilder.Lease lease=control;control=null;snapshot=null;keys=null;Throwable failure=null;
        try{if(owner!=null)owner.close();}catch(RuntimeException|Error cleanup){failure=cleanup;}
        try{if(lease!=null)lease.close();}catch(RuntimeException|Error cleanup){if(failure==null)failure=cleanup;else if(failure!=cleanup)failure.addSuppressed(cleanup);}
        if(failure instanceof RuntimeException error)throw error;if(failure instanceof Error error)throw error;
    }
}
