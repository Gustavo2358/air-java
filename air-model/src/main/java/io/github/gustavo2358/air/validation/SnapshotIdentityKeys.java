package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.AirShape;
import io.github.gustavo2358.air.model.AirSnapshot;
import io.github.gustavo2358.air.model.AirSnapshotBuilder;
import io.github.gustavo2358.air.model.Ids.*;
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
    // Fixed exact pair memo (16 x 3 primitive words), within the same control lease.
    // The level selects a cache slot only; both immutable child keys must match.
    private long[] pairLeft,pairRight,pairKeys;
    private int[] positions;
    // Five primitive words (40B), inside the existing 4096B fixed control reservation.
    // A canonical leaf is immutable for this owned catalogue's lifetime. No text/ID is retained.
    private long leafA,leafB,leafC,leafD,leafKey;
    private boolean failed;

    /** Transfers storage, borrows snapshot; claim fixed control capacity before allocation. */
    public SnapshotIdentityKeys(AirSnapshot snapshot,Storage storage) {
        this.snapshot=Objects.requireNonNull(snapshot);this.storage=Objects.requireNonNull(storage);
        try {
            snapshot.root();control=Objects.requireNonNull(storage.claim(4096));
            characters=new char[1024];forest=new long[64];nodes=new long[NAMESPACE_DEPTH];
            first=new long[NAMESPACE_DEPTH];second=new long[NAMESPACE_DEPTH];positions=new int[NAMESPACE_DEPTH];
            pairLeft=new long[16];pairRight=new long[16];pairKeys=new long[16];
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
    /**
     * Exact lookup key for a caller-owned typed ID, in the same catalogue as source IDs.
     * No typed ID/String is retained or assigned a source handle; buffers are fixed and
     * the namespace recursion is bounded by the closed official ID/owner schema (five).
     */
    public long key(Id id) {
        open();Objects.requireNonNull(id);
        try {snapshot.shape(snapshot.root());return typed(id);}
        catch(RuntimeException|Error failure){failed=true;throw failure;}
        finally{Arrays.fill(forest,0);}
    }
    private long typed(Id id) {
        AirShape shape=switch(id) {
            case PublicationId ignored->AirShape.IDS_PUBLICATION_ID;
            case UnitId ignored->AirShape.IDS_UNIT_ID;
            case EntryId ignored->AirShape.IDS_ENTRY_ID;
            case LabelId ignored->AirShape.IDS_LABEL_ID;
            case OperationId ignored->AirShape.IDS_OPERATION_ID;
            case OperandId ignored->AirShape.IDS_OPERAND_ID;
            case ObjectId ignored->AirShape.IDS_OBJECT_ID;
            case StorageId ignored->AirShape.IDS_STORAGE_ID;
            case ResourceId ignored->AirShape.IDS_RESOURCE_ID;
            case ArtifactId ignored->AirShape.IDS_ARTIFACT_ID;
            case ArtifactRelationId ignored->AirShape.IDS_ARTIFACT_RELATION_ID;
            case OriginId ignored->AirShape.IDS_ORIGIN_ID;
            case UncertaintyId ignored->AirShape.IDS_UNCERTAINTY_ID;
            case PremiseId ignored->AirShape.IDS_PREMISE_ID;
            case CompletionPortId ignored->AirShape.IDS_COMPLETION_PORT_ID;
        };
        if(id instanceof PublicationId)return record(shape,text(0,id.localId(),false),0);
        long namespace=switch(id) {
            case EntryId value->typed(value.unit());case LabelId value->typed(value.unit());
            case OperationId value->typed(value.unit());case ObjectId value->typed(value.unit());
            case CompletionPortId value->typed(value.unit());
            case OperandId value->switch(value.owner()) {
                case OperationOwner owner->record(AirShape.IDS_OPERATION_OWNER,typed(owner.operation()),0);
                case EntryOwner owner->record(AirShape.IDS_ENTRY_OWNER,typed(owner.entry()),0);
            };
            default->typed(id.publication());
        };
        return record(shape,namespace,text(0,id.localId(),false));
    }
    private long record(AirShape shape,long left,long right) {
        return positive(storage.intern(RECORD_BASE+shape.ordinal(),left,right,0,0,0,0));
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
    /**
     * Exact arbitrary canonical INTEGER equality against a natural row ordinal, without narrowing
     * the input or constructing decimal String/BigInteger/derived tuple state. A long ordinal has
     * at most19 digits; cached length/sign reject larger inputs before reading any tree leaf.
     * After atom construction at most two packed leaves are needed, independent of input size.
     */
    public boolean integerEqualsNatural(long handle,long natural) {
        open();
        try {
            if(natural<0||snapshot.shape(handle)!=AirShape.INTEGER)throw new IllegalArgumentException("INTEGER and nonnegative natural ordinal required");
            long key=atom(handle,AirShape.INTEGER);
            if(storage.word(key,5)!=(natural==0?0:1))return false;
            int digits=1;long divisor=1;
            for(long rest=natural;rest>=10;rest/=10){digits++;divisor*=10;}
            if(storage.word(key,3)!=digits)return false;
            long tree=storage.word(key,1),first=tree,second=0;
            if(digits>16) {
                if(storage.word(tree,0)!=TEXT_PAIR)throw new IllegalStateException("canonical ordinal pair required");
                first=storage.word(tree,1);second=storage.word(tree,2);
            }
            if(storage.word(first,0)!=TEXT_LEAF||(second!=0&&storage.word(second,0)!=TEXT_LEAF))throw new IllegalStateException("canonical ordinal leaves required");
            long packed=0;
            for(int at=0;at<digits;at++) {
                int local=at&15;
                if((local&3)==0)packed=storage.word(at<16?first:second,3+local/4);
                long actual=(packed>>>((local&3)*16))&65535,expected='0'+natural/divisor;
                if(actual!=expected)return false;
                natural%=divisor;divisor/=10;
            }
            return true;
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
        return text(handle,null,integer);
    }
    private long text(long handle,String value,boolean integer) {
        Arrays.fill(forest,0);long length=value==null?snapshot.characterCount(handle):value.length(),offset=0,leaves=0,scalars=0,digits=0;
        boolean unicode=true,nonblank=false,canonical=true,negative=false,firstZero=false;char high=0;int modulo=0;
        while(offset<length) {
            int count=(int)Math.min(characters.length,length-offset);
            if(value==null)count=snapshot.readCharacters(handle,offset,characters,0,count);
            else value.getChars(Math.toIntExact(offset),Math.toIntExact(offset)+count,characters,0);
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
                    long packed=(long)character<<((n-at)%4*16);
                    switch((n-at)/4) {case 0->a|=packed;case 1->b|=packed;case 2->c|=packed;case 3->d|=packed;default->throw new AssertionError();}
                }
                long tree=leaf(a,b,c,d);int level=0;long carry=leaves++;
                while((carry&1)!=0) {tree=pair(forest[level],tree,level);forest[level++]=0;carry>>>=1;}
                forest[level]=tree;
            }
            offset+=count;
        }
        if(integer&&(!canonical||digits==0||negative&&firstZero))throw new IllegalStateException("noncanonical integer AIR storage");
        if(high!=0){unicode=false;nonblank=true;}
        long tree=0;
        for(int level=forest.length-1;level>=0;level--)if(forest[level]!=0)
            tree=tree==0?forest[level]:pair(tree,forest[level],level);
        long sign=integer?(firstZero?0:negative?-1:1):0;
        long result=positive(storage.intern(integer?INTEGER_END:TEXT_END,tree,0,length,unicode?scalars:-1,integer?sign:nonblank?1:0,integer?modulo:0));
        if(value==null)storage.remember(handle,result);return result;
    }
    private long leaf(long a,long b,long c,long d) {
        if(leafKey!=0&&a==leafA&&b==leafB&&c==leafC&&d==leafD)return leafKey;
        long key=positive(storage.intern(TEXT_LEAF,0,0,a,b,c,d));
        leafA=a;leafB=b;leafC=c;leafD=d;leafKey=key;return key;
    }
    private long pair(long left,long right,int level) {
        int slot=level&(pairKeys.length-1);
        if(pairKeys[slot]!=0&&pairLeft[slot]==left&&pairRight[slot]==right)return pairKeys[slot];
        long key=positive(storage.intern(TEXT_PAIR,left,right,0,0,0,0));
        pairLeft[slot]=left;pairRight[slot]=right;pairKeys[slot]=key;return key;
    }
    private static long positive(long key) {if(key<=0)throw new IllegalStateException("positive canonical identity key required");return key;}
    private static boolean identity(AirShape shape) {return AirShape.IDS_ID.accepts(shape)||AirShape.IDS_OPERAND_OWNER.accepts(shape);}
    private void open() {if(storage==null||failed)throw new IllegalStateException("snapshot identity index is closed or aborted");}
    @Override public void close() {
        if(storage==null)return;
        Storage owner=storage;storage=null;AirSnapshotBuilder.Lease lease=control;control=null;
        characters=null;forest=null;nodes=null;first=null;second=null;positions=null;
        pairLeft=pairRight=pairKeys=null;
        leafA=leafB=leafC=leafD=leafKey=0;
        Throwable primary=null;
        try {owner.close();}catch(RuntimeException|Error failure){primary=failure;}
        try {if(lease!=null)lease.close();}catch(RuntimeException|Error failure){if(primary==null)primary=failure;else if(primary!=failure)primary.addSuppressed(failure);}
        if(primary instanceof RuntimeException failure)throw failure;if(primary instanceof Error failure)throw failure;
    }
    private void closeSuppressed(Throwable primary) {try{close();}catch(RuntimeException|Error failure){if(failure!=primary)primary.addSuppressed(failure);}}
}
