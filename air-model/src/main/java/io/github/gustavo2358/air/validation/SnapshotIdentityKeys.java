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
        /** Immutable exact tuple column: tag,left,right,a,b,c,d are columns0..6. */
        long word(long key,int column);
        AirSnapshotBuilder.Lease claim(long bytes);
        @Override void close();
    }
    private static final long TEXT_LEAF=1,TEXT_PAIR=2,TEXT_END=3,INTEGER_END=4,RECORD_BASE=16;
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
                        if(childShape==AirShape.TEXT)childKey=text(child,false);
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
    /** Cached intrinsic atom facts, not model-local or cross-reference validation. */
    public enum AtomFact {
        CHARACTERS, UNICODE_SCALARS, INTEGER_SIGN, MAGNITUDE_MODULO_EIGHT, NONBLANK
    }
    /** Exact TEXT/INTEGER content key; integer storage must use canonical signed decimal. */
    public long atomKey(long handle) {
        open();
        try {
            AirShape shape=snapshot.shape(handle);requireAtom(shape);
            return atom(handle,shape);
        } catch(RuntimeException|Error failure){failed=true;throw failure;}
        finally{Arrays.fill(forest,0);}
    }
    /**
     * Unicode scalar count is -1 for malformed UTF-16 (a content fact, never a validity certificate).
     * Sign and magnitude modulo8 require INTEGER; arbitrary precision is never materialized.
     * The source character stream is scanned once, shared with namespace key interning.
     */
    public long atomFact(long handle,AtomFact fact) {
        open();Objects.requireNonNull(fact);
        try {
            AirShape shape=snapshot.shape(handle);requireAtom(shape);
            if((fact==AtomFact.INTEGER_SIGN||fact==AtomFact.MAGNITUDE_MODULO_EIGHT)&&shape!=AirShape.INTEGER)
                throw new IllegalArgumentException("integer atom fact requires INTEGER");
            long key=atom(handle,shape);
            if(fact==AtomFact.NONBLANK&&shape==AirShape.INTEGER)return 1;
            return storage.word(key,switch(fact){case CHARACTERS->3;case UNICODE_SCALARS->4;case INTEGER_SIGN->5;case MAGNITUDE_MODULO_EIGHT->6;case NONBLANK->5;});
        } catch(RuntimeException|Error failure){failed=true;throw failure;}
        finally{Arrays.fill(forest,0);}
    }
    /**
     * Exact canonical integer order: cached sign/length, then first unequal canonical subtree.
     * Equal-length deterministic forests align; equal child keys skip entire shared prefixes.
     * After atom construction this reads O(log characters) tuple words, never source characters.
     */
    public int compareIntegers(long firstHandle,long secondHandle) {
        open();
        try {
            if(snapshot.shape(firstHandle)!=AirShape.INTEGER||snapshot.shape(secondHandle)!=AirShape.INTEGER)
                throw new IllegalArgumentException("integer order requires INTEGER atoms");
            long firstKey=atom(firstHandle,AirShape.INTEGER),secondKey=atom(secondHandle,AirShape.INTEGER);
            if(firstKey==secondKey)return 0;
            long sign=storage.word(firstKey,5),otherSign=storage.word(secondKey,5);
            int order=Long.compare(sign,otherSign);if(order!=0)return order;
            order=Long.compare(storage.word(firstKey,3),storage.word(secondKey,3));
            if(order!=0)return sign<0?-order:order;
            long firstTree=storage.word(firstKey,1),secondTree=storage.word(secondKey,1);
            while(firstTree!=secondTree) {
                long tag=storage.word(firstTree,0);
                if(tag!=storage.word(secondTree,0))throw new IllegalStateException("canonical integer tree shape disagreement");
                if(tag==TEXT_PAIR) {
                    long left=storage.word(firstTree,1),otherLeft=storage.word(secondTree,1);
                    if(left!=otherLeft){firstTree=left;secondTree=otherLeft;}
                    else{firstTree=storage.word(firstTree,2);secondTree=storage.word(secondTree,2);}
                    continue;
                }
                if(tag!=TEXT_LEAF)throw new IllegalStateException("canonical integer leaf required");
                for(int column=3;column<=6;column++) {
                    long word=storage.word(firstTree,column),otherWord=storage.word(secondTree,column);
                    if(word==otherWord)continue;
                    for(int shift=0;shift<64;shift+=16) {
                        order=Long.compare((word>>>shift)&65535,(otherWord>>>shift)&65535);
                        if(order!=0)return sign<0?-order:order;
                    }
                }
                throw new IllegalStateException("canonical integer leaves disagree with exact interning");
            }
            return 0;
        } catch(RuntimeException|Error failure){failed=true;throw failure;}
        finally{Arrays.fill(forest,0);}
    }
    private static void requireAtom(AirShape shape){if(shape!=AirShape.TEXT&&shape!=AirShape.INTEGER)throw new IllegalArgumentException("TEXT or INTEGER atom required");}
    private long atom(long handle,AirShape shape) {
        long known=storage.known(handle);
        if(known==0)return text(handle,shape==AirShape.INTEGER);
        known=positive(known);
        if(storage.word(known,0)!=(shape==AirShape.TEXT?TEXT_END:INTEGER_END))
            throw new IllegalStateException("immutable AIR atom memo changed kind");
        return known;
    }
    private long text(long handle,boolean integer) {
        Arrays.fill(forest,0);long length=snapshot.characterCount(handle),offset=0,leaves=0,scalars=0,digits=0;
        boolean unicode=true,nonblank=false,canonical=true,negative=false,firstZero=false;char high=0;int modulo=0;
        while(offset<length) {
            int count=snapshot.readCharacters(handle,offset,characters,0,(int)Math.min(characters.length,length-offset));
            for(int at=0;at<count;at+=16) {
                int end=Math.min(count,at+16);long a=0,b=0,c=0,d=0;
                for(int n=at;n<end;n++) {
                    char character=characters[n];boolean pair=high!=0&&Character.isLowSurrogate(character);
                    if(high!=0) {
                        if(pair){scalars++;nonblank|=!Character.isWhitespace(Character.toCodePoint(high,character));}
                        else{unicode=false;nonblank=true;}
                        high=0;
                    }
                    if(!pair) {
                        if(Character.isHighSurrogate(character))high=character;
                        else if(Character.isLowSurrogate(character)){unicode=false;nonblank=true;}
                        else{scalars++;nonblank|=!Character.isWhitespace(character);}
                    }
                    if(integer) {
                        if(offset+n==0&&character=='-')negative=true;
                        else if(character<'0'||character>'9')canonical=false;
                        else {
                            if(digits==0)firstZero=character=='0';else if(firstZero)canonical=false;
                            digits++;modulo=(modulo*10+character-'0')&7;
                        }
                    }
                    long value=(long)character<<((n-at)%4*16);
                    switch((n-at)/4) {case 0->a|=value;case 1->b|=value;case 2->c|=value;case 3->d|=value;default->throw new AssertionError();}
                }
                long tree=positive(storage.intern(TEXT_LEAF,0,0,a,b,c,d));int level=0;long carry=leaves++;
                while((carry&1)!=0) {tree=positive(storage.intern(TEXT_PAIR,forest[level],tree,0,0,0,0));forest[level++]=0;carry>>>=1;}
                forest[level]=tree;
            }
            offset+=count;
        }
        if(integer&&(!canonical||digits==0||negative&&firstZero))throw new IllegalStateException("noncanonical integer AIR storage");
        if(high!=0){unicode=false;nonblank=true;}
        long tree=0;
        for(int level=forest.length-1;level>=0;level--)if(forest[level]!=0)
            tree=tree==0?forest[level]:positive(storage.intern(TEXT_PAIR,tree,forest[level],0,0,0,0));
        long sign=integer?(firstZero?0:negative?-1:1):0;
        long result=positive(storage.intern(integer?INTEGER_END:TEXT_END,tree,0,length,unicode?scalars:-1,integer?sign:nonblank?1:0,integer?modulo:0));
        storage.remember(handle,result);return result;
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
