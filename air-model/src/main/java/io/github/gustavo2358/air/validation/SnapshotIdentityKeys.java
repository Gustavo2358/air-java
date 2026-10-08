package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.AirShape;
import io.github.gustavo2358.air.model.AirSnapshot;
import io.github.gustavo2358.air.model.AirSnapshotBuilder;
import java.util.Arrays;
import java.util.Objects;

/**
 * Exact complete typed identity keys over a borrowed immutable snapshot. No model/text
 * materialization, I/O or validity certificate. The owned storage supplies spill and capacity
 * accounting; key numbers are local to this index and must never become published AIR IDs.
 */
public final class SnapshotIdentityKeys implements AutoCloseable {
    /**
     * One index lifetime: empty memo and tuple catalogue on transfer, exact tuple equality even
     * under hash collisions, positive keys stable until close. Zero means no memo entry/reference.
     * known/remember index source-local handles (including sparse 64-bit addresses). Every
     * cardinality-dependent payload/index must be accounted and spillable by a managed adapter.
     * References are left/right; a/b/c/d are literal words. No collection before owner closure.
     */
    public interface Storage extends AutoCloseable {
        long known(long sourceHandle);
        void remember(long sourceHandle,long key);
        long intern(long tag,long left,long right,long a,long b,long c,long d);
        AirSnapshotBuilder.Lease claim(long bytes);
        @Override void close();
    }
    private static final long TEXT_LEAF=1,TEXT_PAIR=2,TEXT_END=3,RECORD_BASE=16;
    // OperandId -> OperandOwner -> EntryId/OperationId -> UnitId -> PublicationId.
    private static final int NAMESPACE_DEPTH=5;
    private final AirSnapshot snapshot;
    private Storage storage;
    private AirSnapshotBuilder.Lease control;
    private char[] characters;
    private long[] forest,nodes,first,second;
    private int[] positions;
    private boolean failed;

    /** Transfers storage, borrows snapshot; claim fixed control capacity before allocation. */
    public SnapshotIdentityKeys(AirSnapshot snapshot,Storage storage) {
        this.snapshot=Objects.requireNonNull(snapshot);this.storage=Objects.requireNonNull(storage);
        try {
            snapshot.root();control=Objects.requireNonNull(storage.claim(4096));
            characters=new char[1024];forest=new long[64];nodes=new long[NAMESPACE_DEPTH];
            first=new long[NAMESPACE_DEPTH];second=new long[NAMESPACE_DEPTH];positions=new int[NAMESPACE_DEPTH];
        } catch(RuntimeException|Error failure) {closeSuppressed(failure);throw failure;}
    }
    /** Complete ID or operand-owner key. Memoized source nodes perform no character re-read. */
    public long key(long handle) {
        open();
        try {
            AirShape rootShape=snapshot.shape(handle);
            if(!identity(rootShape))throw new IllegalArgumentException("typed AIR identity or operand owner required");
            long known=storage.known(handle);if(known!=0)return positive(known);
            int depth=0;nodes[0]=handle;positions[0]=0;first[0]=second[0]=0;
            while(depth>=0) {
                long node=nodes[depth];AirShape shape=snapshot.shape(node);int position=positions[depth];
                if(position<shape.fieldCount()) {
                    long child=snapshot.field(node,shape,position);AirShape childShape=snapshot.shape(child);
                    long childKey=storage.known(child);
                    if(childKey==0) {
                        if(childShape==AirShape.TEXT)childKey=text(child);
                        else {
                            if(!identity(childShape)||depth+1==NAMESPACE_DEPTH)
                                throw new IllegalStateException("identity namespace shape exceeds official schema");
                            depth++;nodes[depth]=child;positions[depth]=0;first[depth]=second[depth]=0;continue;
                        }
                    }
                    if(position==0)first[depth]=positive(childKey);else second[depth]=positive(childKey);
                    positions[depth]++;
                } else {
                    long result=positive(storage.intern(RECORD_BASE+shape.ordinal(),first[depth],second[depth],0,0,0,0));
                    storage.remember(node,result);depth--;if(depth<0)return result;
                    if(positions[depth]++==0)first[depth]=result;else second[depth]=result;
                }
            }
            throw new IllegalStateException("identity traversal produced no key");
        } catch(RuntimeException|Error failure) {failed=true;throw failure;}
        finally {Arrays.fill(nodes,0);Arrays.fill(first,0);Arrays.fill(second,0);}
    }
    private long text(long handle) {
        Arrays.fill(forest,0);long length=snapshot.characterCount(handle),offset=0,leaves=0;
        while(offset<length) {
            int count=snapshot.readCharacters(handle,offset,characters,0,(int)Math.min(characters.length,length-offset));
            for(int at=0;at<count;at+=16) {
                int end=Math.min(count,at+16);long a=0,b=0,c=0,d=0;
                for(int n=at;n<end;n++) {
                    long value=(long)characters[n]<<((n-at)%4*16);
                    switch((n-at)/4) {case 0->a|=value;case 1->b|=value;case 2->c|=value;case 3->d|=value;default->throw new AssertionError();}
                }
                long tree=positive(storage.intern(TEXT_LEAF,0,0,a,b,c,d));int level=0;long carry=leaves++;
                while((carry&1)!=0) {tree=positive(storage.intern(TEXT_PAIR,forest[level],tree,0,0,0,0));forest[level++]=0;carry>>>=1;}
                forest[level]=tree;
            }
            offset+=count;
        }
        long tree=0;
        for(int level=forest.length-1;level>=0;level--)if(forest[level]!=0)
            tree=tree==0?forest[level]:positive(storage.intern(TEXT_PAIR,tree,forest[level],0,0,0,0));
        long result=positive(storage.intern(TEXT_END,tree,0,length,0,0,0));storage.remember(handle,result);return result;
    }
    private static long positive(long key) {if(key<=0)throw new IllegalStateException("positive canonical identity key required");return key;}
    private static boolean identity(AirShape shape) {return AirShape.IDS_ID.accepts(shape)||AirShape.IDS_OPERAND_OWNER.accepts(shape);}
    private void open() {if(storage==null||failed)throw new IllegalStateException("snapshot identity index is closed or aborted");}
    @Override public void close() {
        if(storage==null)return;
        Storage owner=storage;storage=null;AirSnapshotBuilder.Lease lease=control;control=null;
        characters=null;forest=null;nodes=null;first=null;second=null;positions=null;
        Throwable primary=null;
        try {owner.close();}catch(RuntimeException|Error failure){primary=failure;}
        try {if(lease!=null)lease.close();}catch(RuntimeException|Error failure){if(primary==null)primary=failure;else if(primary!=failure)primary.addSuppressed(failure);}
        if(primary instanceof RuntimeException failure)throw failure;if(primary instanceof Error failure)throw failure;
    }
    private void closeSuppressed(Throwable primary) {try{close();}catch(RuntimeException|Error failure){if(failure!=primary)primary.addSuppressed(failure);}}
}
