package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Exact declaration/occurrence inventory over borrowed frozen AIR and identity keys.
 * Owns primitive storage; never constructs Publication, IDs or recursive traversal frames.
 * Only I-01/I-03/I-11 inventory facts are checked. This is NOT a validation certificate:
 * local model invariants, references, capabilities, types/domains and operations remain separate.
 * Borrowed snapshot/identity catalogue must outlive this owner. Not thread safe.
 */
public final class SnapshotDeclarations implements AutoCloseable {
    public enum Fact { NODE, IDENTITY, UNIT, SEQUENCE, OWNER }
    public enum Rule {
        FOREIGN_PUBLICATION("I-01"), DUPLICATE_ID("I-01"),
        OPERATION_UNIT("I-03"), SEQUENCE_UNIT("I-03"), OPERAND_OWNER("I-11");
        private final String invariant;
        Rule(String invariant) { this.invariant=invariant; }
        public String invariant() { return invariant; }
    }
    @FunctionalInterface public interface Issues { void report(Rule rule,long identity,long node); }
    public enum LimitKind { ENTITIES, NESTING }
    public static final class Limit extends RuntimeException {
        private static final long serialVersionUID=1L;
        private final LimitKind kind;
        private Limit(LimitKind kind) { super("AIR declaration index limit: "+kind); this.kind=kind; }
        public LimitKind kind() { return kind; }
    }
    /**
     * Empty at transfer. Required declaration/frontier payload must be accounted and spillable.
     * define keeps the first row and returns false for every repeated key, even the same node.
     * Zero lookup denotes no declaration/association. enqueue retains EACH occurrence, without
     * source-node deduplication; advance consumes FIFO rows and releases consumed payload.
     * node/owner/depth expose the current primitive row until next advance. close is idempotent.
     */
    public interface Storage extends AutoCloseable {
        boolean define(long key,long node,long identity,long unit,long sequence,long owner);
        long fact(long key,Fact field);
        /** Dense zero-based rows in first successful definition order, without cursor materialization. */
        long declaration(long index,Fact field);
        void enqueue(long node,long owner,long depth);
        boolean advance();
        long node(); long owner(); long depth();
        AirSnapshotBuilder.Lease claim(long bytes);
        /** Seal a drained inventory: reject definitions/frontier writes/claims; retain exact reads. */
        void freeze();
        @Override void close();
    }
    private AirSnapshot snapshot;
    private SnapshotIdentityKeys keys;
    private Issues issues;
    private final long maximumEntities,maximumNesting;
    private final LongConsumer enqueue;
    private Storage storage;
    private AirSnapshotBuilder.Lease control;
    private long publicationKey,entities,operands,operations,pendingOwner,pendingDepth;
    private boolean children;

    private SnapshotDeclarations(AirSnapshot snapshot,SnapshotIdentityKeys keys,Storage storage,
                                 long maximumEntities,long maximumNesting,Issues issues) {
        this.snapshot=Objects.requireNonNull(snapshot);this.keys=Objects.requireNonNull(keys);
        this.storage=Objects.requireNonNull(storage);this.issues=Objects.requireNonNull(issues);
        this.maximumEntities=maximumEntities;this.maximumNesting=maximumNesting;
        // Fixed owner/callback state is funded before constructing callback or starting the walk.
        try {
            if(maximumEntities<=0 || maximumNesting<0) throw new IllegalArgumentException("invalid declaration index limits");
            control=Objects.requireNonNull(storage.claim(512));
            enqueue=value -> {
                long depth=pendingDepth;
                if(children) {
                    if(depth==Long.MAX_VALUE) throw new Limit(LimitKind.NESTING);
                    depth++;
                }
                this.storage.enqueue(value,pendingOwner,depth);
            };
        } catch(RuntimeException|Error failure) {closeSuppressed(failure);throw failure;}
    }
    /** Transfers storage; never exposes an operationally incomplete index. Reports may mark invalid AIR. */
    public static SnapshotDeclarations build(AirSnapshot snapshot,SnapshotIdentityKeys keys,Storage storage,
                                             long maximumEntities,long maximumNesting,Issues issues) {
        var index=new SnapshotDeclarations(snapshot,keys,storage,maximumEntities,maximumNesting,issues);
        try { index.inventory();index.storage.freeze();return index; }
        catch(RuntimeException|Error failure) {index.closeSuppressed(failure);throw failure;}
    }
    public long entities(){open();return entities;}
    public long operands(){open();return operands;}
    public long operations(){open();return operations;}
    public long fact(long identity,Fact field) {
        open();Objects.requireNonNull(field);
        try {
            if(!AirShape.IDS_ID.accepts(snapshot.shape(identity))) throw new IllegalArgumentException("typed AIR ID required");
            return storage.fact(keys.key(identity),field);
        } catch(RuntimeException|Error failure) {closeSuppressed(failure);throw failure;}
    }
    /** Random access to the immutable declaration catalogue; ordinal and addresses remain 64-bit. */
    public long declaration(long ordinal,Fact field) {
        open();Objects.requireNonNull(field);
        if(ordinal<0 || ordinal>=entities)throw new IndexOutOfBoundsException("AIR declaration ordinal "+ordinal);
        try{return storage.declaration(ordinal,field);}
        catch(RuntimeException|Error failure){closeSuppressed(failure);throw failure;}
    }
    private void inventory() {
        long root=snapshot.root(),publication=snapshot.field(root,AirShape.PUBLICATION,0);
        publicationKey=keys.key(publication);add(root,publication,0,0,0);
        global(root,3,AirShape.ORIGINS_ARTIFACT,false);
        global(root,8,AirShape.ORIGINS_ORIGIN,false);
        global(root,10,AirShape.EVIDENCE_UNCERTAINTY,false);
        global(root,11,AirShape.PROOFS_PREMISE,false);
        global(root,6,AirShape.INTERACTIONS_RESOURCE,false);
        global(root,7,AirShape.ARTIFACTS_RELATION,false);
        global(root,5,AirShape.MEMORY_STORAGE,true);
        try(var units=snapshot.elements(snapshot.field(root,AirShape.PUBLICATION,4),AirShape.UNIT)) {
            while(units.advance()) unit(units.value());
        }
    }
    private void global(long root,int ordinal,AirShape expected,boolean header) {
        try(var rows=snapshot.elements(snapshot.field(root,AirShape.PUBLICATION,ordinal),expected)) {
            while(rows.advance()) {
                long node=rows.value();AirShape shape=snapshot.shape(node);
                long id=snapshot.field(node,shape,0);
                if(header) id=snapshot.field(id,AirShape.MEMORY_STORAGE_HEADER,0);
                add(node,id,0,0,0);
            }
        }
    }
    private void unit(long node) {
        long unit=snapshot.field(node,AirShape.UNIT,0);add(node,unit,unit,0,0);
        try(var rows=snapshot.elements(snapshot.field(node,AirShape.UNIT,2),AirShape.MEMORY_OBJECT_DECLARATION)) {
            while(rows.advance()) {long object=rows.value();add(object,snapshot.field(object,AirShape.MEMORY_OBJECT_DECLARATION,0),unit,0,0);}
        }
        try(var rows=snapshot.elements(snapshot.field(node,AirShape.UNIT,6),AirShape.ENTRIES_COMPLETION_PORT)) {
            while(rows.advance()) {long port=rows.value();add(port,snapshot.field(port,AirShape.ENTRIES_COMPLETION_PORT,0),unit,0,0);}
        }
        try(var rows=snapshot.elements(snapshot.field(node,AirShape.UNIT,4),AirShape.ENTRIES_ENTRY)) {
            while(rows.advance()) entry(rows.value(),unit);
        }
        try(var rows=snapshot.elements(snapshot.field(node,AirShape.UNIT,5),AirShape.SEQUENCE)) {
            while(rows.advance()) sequence(rows.value(),unit);
        }
    }
    private void entry(long node,long unit) {
        long id=snapshot.field(node,AirShape.ENTRIES_ENTRY,0);add(node,id,unit,0,0);
        roots(id);long state=snapshot.field(node,AirShape.ENTRIES_ENTRY,3);
        try(var conditions=snapshot.elements(snapshot.field(state,AirShape.ENTRIES_ENTRY_STATE,0),AirShape.ENTRIES_INITIAL_CONDITION)) {
            while(conditions.advance()) {
                long condition=conditions.value();enqueue.accept(snapshot.field(condition,AirShape.ENTRIES_INITIAL_CONDITION,0));
                long value=snapshot.field(condition,AirShape.ENTRIES_INITIAL_CONDITION,1);AirShape shape=snapshot.shape(value);
                switch(shape) {
                    case ENTRIES_LITERAL_INITIAL -> enqueue.accept(snapshot.field(value,shape,0));
                    case ENTRIES_POSSIBLE_LITERALS -> {
                        try(var candidates=snapshot.elements(snapshot.field(value,shape,0),AirShape.EXPRESSIONS_LITERAL)) {
                            while(candidates.advance()) enqueue.accept(candidates.value());
                        }
                    }
                    case ENTRIES_PARAMETER_INITIAL, ENTRIES_PRESERVE, ENTRIES_EXTERNAL_UNKNOWN, ENTRIES_UNINITIALIZED -> { }
                    default -> throw new IllegalArgumentException("concrete AIR initial value required");
                }
            }
        }
        drain(unit,0);
    }
    private void sequence(long node,long unit) {
        long label=snapshot.field(node,AirShape.SEQUENCE,0);add(node,label,unit,label,0);
        try(var instructions=snapshot.elements(snapshot.field(node,AirShape.SEQUENCE,1),AirShape.INSTRUCTION)) {
            while(instructions.advance()) operation(instructions.value(),unit,label);
        }
        operation(snapshot.field(node,AirShape.SEQUENCE,2),unit,label);
    }
    private void operation(long node,long unit,long sequence) {
        AirShape shape=snapshot.shape(node);long header=snapshot.field(node,shape,0);
        long id=snapshot.field(header,AirShape.OPERATIONS_HEADER,0);
        if(add(node,id,unit,sequence,0)) operations++;
        long expected=keys.key(unit);
        if(keys.key(snapshot.field(id,AirShape.IDS_OPERATION_ID,0))!=expected) issues.report(Rule.OPERATION_UNIT,id,node);
        else if(keys.key(snapshot.field(sequence,AirShape.IDS_LABEL_ID,0))!=expected) issues.report(Rule.SEQUENCE_UNIT,id,node);
        roots(id);SnapshotOperands.roots(snapshot,node,enqueue);drain(unit,sequence);
    }
    private void roots(long owner) {pendingOwner=owner;pendingDepth=0;children=false;}
    private void drain(long unit,long sequence) {
        while(storage.advance()) {
            long node=storage.node(),owner=storage.owner(),depth=storage.depth();
            if(depth<0 || depth>maximumNesting) throw new Limit(LimitKind.NESTING);
            AirShape shape=snapshot.shape(node);long header=snapshot.field(node,shape,0);
            long id=snapshot.field(header,AirShape.OPERAND_HEADER,0);
            boolean fresh=add(node,id,unit,sequence,owner);
            long actual=snapshot.field(id,AirShape.IDS_OPERAND_ID,0);AirShape ownerShape=snapshot.shape(actual);
            long actualId=snapshot.field(actual,ownerShape,0);
            if(keys.key(actualId)!=keys.key(owner)) issues.report(Rule.OPERAND_OWNER,id,node);
            if(fresh) {
                operands++;pendingOwner=owner;pendingDepth=depth;children=true;
                SnapshotOperands.children(snapshot,node,enqueue);
            }
        }
        pendingOwner=pendingDepth=0;children=false;
    }
    private boolean add(long node,long id,long unit,long sequence,long owner) {
        if(entities>=maximumEntities) throw new Limit(LimitKind.ENTITIES);
        if(keys.key(publication(id))!=publicationKey) issues.report(Rule.FOREIGN_PUBLICATION,id,node);
        if(!storage.define(keys.key(id),node,id,unit,sequence,owner)) {issues.report(Rule.DUPLICATE_ID,id,node);return false;}
        entities++;return true;
    }
    private long publication(long id) {
        // Official identity namespace has at most four parent edges; no recursion/ID materialization.
        for(int n=0;n<5;n++) {
            AirShape shape=snapshot.shape(id);
            if(shape==AirShape.IDS_PUBLICATION_ID)return id;
            if(!AirShape.IDS_ID.accepts(shape) && shape!=AirShape.IDS_OPERATION_OWNER && shape!=AirShape.IDS_ENTRY_OWNER)
                throw new IllegalArgumentException("typed AIR identity namespace required");
            id=snapshot.field(id,shape,0);
        }
        throw new IllegalStateException("identity namespace exceeds official schema");
    }
    private void open(){if(storage==null)throw new IllegalStateException("AIR declaration index is closed");}
    private void closeSuppressed(Throwable failure){try{close();}catch(RuntimeException|Error cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}}
    @Override public void close() {
        Storage owned=storage;AirSnapshotBuilder.Lease lease=control;storage=null;control=null;snapshot=null;keys=null;issues=null;
        pendingOwner=pendingDepth=0;children=false;Throwable failure=null;
        if(owned!=null)try{owned.close();}catch(RuntimeException|Error error){failure=error;}
        if(lease!=null)try{lease.close();}catch(RuntimeException|Error error){if(failure==null)failure=error;else if(error!=failure)failure.addSuppressed(error);}
        if(failure instanceof RuntimeException error)throw error;
        if(failure instanceof Error error)throw error;
    }
}
