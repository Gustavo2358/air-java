package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.AirShape;
import io.github.gustavo2358.air.model.AirSnapshot;
import io.github.gustavo2358.air.model.AirSnapshotBuilder;
import java.util.Objects;
import static io.github.gustavo2358.air.model.AirShape.*;
import static io.github.gustavo2358.air.validation.SnapshotIdentityKeys.AtomFact.*;

/**
 * Constructor-local predicates over borrowed typed input/atom keys. No model reconstruction,
 * reference/visibility/type/capability admission or validity certificate. Repeated collection
 * predicates use exact required index state supplied by the owned, managed storage port.
 */
public final class SnapshotLocalConstraints implements SnapshotGraphWalk.Visitor,AutoCloseable {
    /** Receives constructor-local diagnostics while a whole-graph scan continues. */
    @FunctionalInterface
    public interface Issues {
        void report(Rule rule,long node,int field);
    }
    /**
     * Empty on transfer. Facts are keyed by exact source handle and kind; label membership uses
     * the source LIST handle and complete canonical LabelId key from the borrowed key owner.
     * Required membership/fact state survives until close; cardinality-dependent state must spill.
     * Operational failure propagates, and close is idempotent, including after failed operations.
     */
    public interface Storage extends AutoCloseable {
        boolean known(long source,int kind);
        void remember(long source,int kind);
        /** True only for a new exact (labels collection,complete label key) pair. */
        boolean addLabel(long collection,long key);
        boolean containsLabel(long collection,long key);
        AirSnapshotBuilder.Lease claim(long bytes);
        @Override void close();
    }
    private static final int LABELS=0,FORMAT=1,OCTETS=2;
    public enum Rule {
        UNICODE,NONBLANK,ONE_SCALAR,NONNEGATIVE,POSITIVE,MULTIPLE_EIGHT,NONEMPTY,
        CLOSED_EMPTY,ACTIVATION_OWNER,EXTENT_EXCLUSIVE,UNIT_BODY,LABEL_DUPLICATE,
        LABEL_MEMBER,BASE,COORDINATE,ORDER,DECIMAL_PART,DECIMAL_FORMAT,OCTET
    }
    /** Source node/slot are internal diagnostic anchors, not external AIR IDs/wire paths. */
    public static final class Invalid extends IllegalArgumentException {
        private static final long serialVersionUID=1L;
        private final Rule rule;
        private final long node;
        private final int field;
        Invalid(Rule rule,long node,int field){super("AIR constructor constraint "+rule+" at node "+node+" field "+field);this.rule=rule;this.node=node;this.field=field;}
        public Rule rule(){return rule;}
        public long node(){return node;}
        public int field(){return field;}
    }
    private final AirSnapshot snapshot;
    private final SnapshotIdentityKeys keys;
    private final Issues issues;
    private Storage storage;
    private AirSnapshotBuilder.Lease control;
    private boolean failed;

    /** Transfers storage, borrows input and keys. Only fixed primitive control remains resident. */
    public SnapshotLocalConstraints(AirSnapshot snapshot,SnapshotIdentityKeys keys,Storage storage) {
        this(snapshot,keys,storage,null);
    }
    private SnapshotLocalConstraints(AirSnapshot snapshot,SnapshotIdentityKeys keys,Storage storage,Issues issues) {
        this.snapshot=snapshot;this.keys=keys;this.storage=Objects.requireNonNull(storage);
        this.issues=issues;
        try {Objects.requireNonNull(snapshot).root();Objects.requireNonNull(keys);control=Objects.requireNonNull(storage.claim(256));}
        catch(RuntimeException|Error failure){closeSuppressed(failure);throw failure;}
    }
    /**
     * Transfers both scratch ports. Entire reachable graph, including dead code, is visited.
     * Successful counts establish only grammar and these local predicates, never AIR validity.
     */
    public static SnapshotGraphWalk.Counts scan(AirSnapshot snapshot,SnapshotIdentityKeys keys,Storage storage,
                                                SnapshotGraphWalk.Storage graph,long maximumNodes,long maximumDepth) {
        return scan(snapshot,keys,storage,graph,maximumNodes,maximumDepth,null);
    }
    /**
     * As above, but constructor-local failures are reported and traversal continues. The callback
     * is borrowed and must retain only bounded diagnostics; callback failure poisons the owner.
     */
    public static SnapshotGraphWalk.Counts scan(AirSnapshot snapshot,SnapshotIdentityKeys keys,Storage storage,
                                                SnapshotGraphWalk.Storage graph,long maximumNodes,long maximumDepth,
                                                Issues issues) {
        SnapshotLocalConstraints check;
        try {check=new SnapshotLocalConstraints(snapshot,keys,storage,issues);}
        catch(RuntimeException|Error failure){try{Objects.requireNonNull(graph).close();}catch(RuntimeException|Error cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}throw failure;}
        try(check){return SnapshotGraphWalk.scan(snapshot,graph,maximumNodes,maximumDepth,check);}
    }
    /**
     * Can also be composed into another owned grammar walk. Local content failures are diagnostic
     * (no valid fact is published for a failed collection); operational failures poison this owner.
     * Callers retaining an inspector after an Invalid must still reject the complete publication.
     */
    @Override public void node(long node,AirShape shape,AirShape element) {
        if(storage==null||failed)throw new IllegalStateException("local constraint owner is unavailable");
        try {
            if(snapshot.shape(node)!=shape)throw new IllegalArgumentException("local constraint shape mismatch");
            switch(shape) {
                case INTEGER -> keys.atomKey(node); // Canonical integer grammar, including unconstrained descriptors.
                case IDS_PUBLICATION_ID -> text(node,shape,0,true);
                case IDS_UNIT_ID,IDS_OBJECT_ID,IDS_STORAGE_ID,IDS_LABEL_ID,IDS_ENTRY_ID,IDS_COMPLETION_PORT_ID,
                     IDS_OPERATION_ID,IDS_OPERAND_ID,IDS_ORIGIN_ID,IDS_UNCERTAINTY_ID,IDS_PREMISE_ID,IDS_ARTIFACT_ID,
                     IDS_RESOURCE_ID,IDS_ARTIFACT_RELATION_ID -> text(node,shape,1,true);
                case CAPABILITIES_CAPABILITY,TYPES_EXTENSION_TYPE,MEMORY_EXTENSION_CODEC,INTERACTIONS_EXTENSION_NAME,
                     INTERACTIONS_COMPUTED_TARGET,INTERACTIONS_COMPUTED_RESOURCE,INTERACTIONS_UNKNOWN_RESOURCE -> {
                    text(node,shape,0,true);text(node,shape,1,true);
                }
                case INTERACTIONS_LITERAL_TARGET -> {text(node,shape,0,true);text(node,shape,1,true);text(node,shape,2,false);}
                case CONTROL_EXCEPTIONAL,CONTROL_EXCEPTION_OUTCOME,EVIDENCE_ELIMINATION,EVIDENCE_COVERAGE_ITEM,
                     INTERACTIONS_LOCAL_RESOURCE,ENVELOPES_RESOURCE_USE,OPERATIONS_REENTRY_GUARD,OPERATIONS_RESUME_ROUTE -> text(node,shape,0,true);
                case INTERACTIONS_RESOURCE_OBJECT,INTERACTIONS_RESOURCE_USE,OPERATIONS_INVOKE,OPERATIONS_OPAQUE,
                     OPERATIONS_RAISE,ORIGINS_UNAVAILABLE,ORIGINS_ARTIFACT -> text(node,shape,1,true);
                case ARTIFACTS_RELATION -> text(node,shape,3,true);
                case INTERACTIONS_RESOURCE_DECLARATION -> {text(node,shape,1,true);text(node,shape,2,true);text(node,shape,3,true);}
                case PROOFS_PREMISE,ORIGINS_CONTRACTUAL -> {text(node,shape,1,true);text(node,shape,2,true);}
                case ORIGINS_INCLUDE_FRAME -> text(node,shape,2,true);
                case ORIGINS_DERIVED -> {nonempty(node,shape,1);text(node,shape,2,true);}
                case EVIDENCE_UNCERTAINTY -> {text(node,shape,1,true);nonempty(node,shape,2);text(node,shape,4,true);}
                case INTERACTIONS_CONTRACT_REF -> {text(node,shape,0,true);text(node,shape,1,true);nonempty(node,shape,2);}
                case VALUES_TEXT_VALUE -> text(node,shape,0,false);
                case EXPRESSIONS_TRIM_RIGHT -> text(node,shape,2,false);
                case EXPRESSIONS_FIT_TEXT -> {
                    nonnegative(node,shape,2);text(node,shape,3,false);
                    require(keys.atomFact(field(node,shape,3),UNICODE_SCALARS)==1,Rule.ONE_SCALAR,node,3);
                }
                case OPERATIONS_LOCAL_RESUME -> optionalText(node,shape,2);
                case OPERATIONS_LOCAL_BOUNDARY -> optionalText(node,shape,4);
                case SEMANTIC_VERSION -> {nonnegative(node,shape,0);nonnegative(node,shape,1);nonnegative(node,shape,2);}
                case VALUES_DECIMAL_VALUE,OPERATIONS_LOCAL_UNWIND,PROOFS_PARAMETER_DOMAIN,PROOFS_RESULT_DOMAIN,
                     PROOFS_EXTERNAL_PARAMETER_DOMAIN,PROOFS_EXTERNAL_RESULT_DOMAIN -> nonnegative(node,shape,1);
                case EXPRESSIONS_QUANTIZE,EXPRESSIONS_FILL_TEXT,PROOFS_CALL_PARAMETER_DOMAIN,PROOFS_CALL_RESULT_DOMAIN -> nonnegative(node,shape,2);
                case OPERATIONS_COPY_BYTES -> nonnegative(node,shape,3);
                case ENTRIES_PARAMETER_INITIAL,INTERACTIONS_PARAMETER,INTERACTIONS_RESULT_SLOT -> nonnegative(node,shape,0);
                case ORIGINS_POSITION -> {nonnegative(node,shape,0);nonnegative(node,shape,1);}
                case MEMORY_VIEW_BINDING -> {nonnegative(node,shape,1);nonnegative(node,shape,2);}
                case EXPRESSIONS_WRAP_INTEGER,EXPRESSIONS_FIT_DECIMAL,EXPRESSIONS_INTEGER_DIGITS -> positive(node,shape,2);
                case MEMORY_BINARY_CODEC -> {positive(node,shape,1);require(keys.atomFact(field(node,shape,1),MAGNITUDE_MODULO_EIGHT)==0,Rule.MULTIPLE_EIGHT,node,1);}
                case SCOPES_ENTITY_SCOPE,SCOPES_OBJECTS_MEMORY,SCOPES_STORAGE_MEMORY,SCOPES_MEMORY_UNION,ENTRIES_POSSIBLE_LITERALS -> nonempty(node,shape,0);
                case CONTROL_INVOCATION_OUTCOMES,CONTROL_CONTROL_ENVELOPE -> closed(node,shape,0,SCOPES_NO_CONTROL);
                case MEMORY_ALTERNATIVES_BINDING -> closed(node,shape,0,SCOPES_NO_MEMORY);
                case PLACES_CHOICE -> closed(node,shape,1,SCOPES_NO_MEMORY);
                case MEMORY_STORAGE_HEADER -> require(snapshot.scalar(field(node,shape,2))!=0||size(node,shape,1)!=0,Rule.ACTIVATION_OWNER,node,1);
                case MEMORY_REGION -> {
                    long known=field(node,shape,1);boolean present=snapshot.size(known)!=0;
                    require(present!=(size(node,shape,2)!=0),Rule.EXTENT_EXCLUSIVE,node,1);
                    if(present)require(keys.atomFact(snapshot.element(known,INTEGER,0),INTEGER_SIGN)>=0,Rule.NONNEGATIVE,node,1);
                }
                case UNIT -> {
                    boolean available=snapshot.scalar(field(node,shape,7))==0;
                    long entries=size(node,shape,4),sequences=size(node,shape,5),unavailable=size(node,shape,8);
                    require(available?entries!=0&&sequences!=0&&unavailable==0:sequences==0&&unavailable!=0,Rule.UNIT_BODY,node,7);
                }
                case ORIGINS_OFFSETS -> {
                    nonnegative(node,shape,0);nonnegative(node,shape,1);text(node,shape,2,true);
                    require(keys.compareIntegers(field(node,shape,0),field(node,shape,1))<=0,Rule.ORDER,node,1);
                }
                case ORIGINS_SPAN -> span(node);
                case TYPES_LABEL_TYPE -> labels(node);
                case VALUES_LABEL_VALUE -> {
                    long domain=field(node,shape,1);labels(domain);
                    require(storage.containsLabel(field(domain,TYPES_LABEL_TYPE,1),keys.key(field(node,shape,0))),Rule.LABEL_MEMBER,node,0);
                }
                case VALUES_BYTES_VALUE -> octets(node);
                case DECIMAL_TEXT_PART -> part(node);
                case EXPRESSIONS_FORMAT_DECIMAL -> format(node);
                default -> { } // Other shapes have only grammar/null/ownership constructor restrictions.
            }
        } catch(Invalid invalid){
            if(issues==null)throw invalid;
            try {issues.report(invalid.rule(),invalid.node(),invalid.field());}
            catch(RuntimeException|Error failure){failed=true;throw failure;}
        }
        catch(RuntimeException|Error failure){failed=true;throw failure;}
    }
    private long field(long node,AirShape shape,int at){return snapshot.field(node,shape,at);}
    private long size(long node,AirShape shape,int at){return snapshot.size(field(node,shape,at));}
    private void text(long node,AirShape shape,int at,boolean nonblank){textValue(field(node,shape,at),nonblank,node,at);}
    private void textValue(long value,boolean nonblank,long node,int at) {
        require(keys.atomFact(value,UNICODE_SCALARS)>=0,Rule.UNICODE,node,at);
        if(nonblank)require(keys.atomFact(value,NONBLANK)!=0,Rule.NONBLANK,node,at);
    }
    private void optionalText(long node,AirShape shape,int at){long optional=field(node,shape,at);if(snapshot.size(optional)!=0)textValue(snapshot.element(optional,TEXT,0),true,node,at);}
    private void nonnegative(long node,AirShape shape,int at){require(keys.atomFact(field(node,shape,at),INTEGER_SIGN)>=0,Rule.NONNEGATIVE,node,at);}
    private void positive(long node,AirShape shape,int at){require(keys.atomFact(field(node,shape,at),INTEGER_SIGN)>0,Rule.POSITIVE,node,at);}
    private void nonempty(long node,AirShape shape,int at){require(size(node,shape,at)!=0,Rule.NONEMPTY,node,at);}
    private void closed(long node,AirShape shape,int at,AirShape none){require(size(node,shape,at)!=0||snapshot.shape(field(node,shape,at+1))!=none,Rule.CLOSED_EMPTY,node,at);}
    private boolean one(long integer){return keys.atomFact(integer,INTEGER_SIGN)==1&&keys.atomFact(integer,CHARACTERS)==1&&keys.atomFact(integer,MAGNITUDE_MODULO_EIGHT)==1;}
    private boolean base(long integer){return keys.atomFact(integer,INTEGER_SIGN)==0||one(integer);}
    private void span(long node) {
        long start=field(node,ORIGINS_SPAN,0),end=field(node,ORIGINS_SPAN,1),line=field(node,ORIGINS_SPAN,2),column=field(node,ORIGINS_SPAN,3);
        require(base(line)&&base(column),Rule.BASE,node,2);
        long sl=field(start,ORIGINS_POSITION,0),sc=field(start,ORIGINS_POSITION,1),el=field(end,ORIGINS_POSITION,0),ec=field(end,ORIGINS_POSITION,1);
        require(keys.compareIntegers(sl,line)>=0&&keys.compareIntegers(el,line)>=0&&keys.compareIntegers(sc,column)>=0&&keys.compareIntegers(ec,column)>=0,Rule.COORDINATE,node,0);
        int order=keys.compareIntegers(sl,el);require(order<0||order==0&&keys.compareIntegers(sc,ec)<=0,Rule.ORDER,node,1);
    }
    private void labels(long node) {
        long list=field(node,TYPES_LABEL_TYPE,1);require(snapshot.size(list)!=0,Rule.NONEMPTY,node,1);
        if(storage.known(list,LABELS))return;
        try(var cursor=snapshot.elements(list,IDS_LABEL_ID)) {
            while(cursor.advance())require(storage.addLabel(list,keys.key(cursor.value())),Rule.LABEL_DUPLICATE,node,1);
        }
        storage.remember(list,LABELS);
    }
    private void octets(long node) {
        long list=field(node,VALUES_BYTES_VALUE,0);if(storage.known(list,OCTETS))return;
        try(var cursor=snapshot.elements(list,SMALL_INTEGER)){while(cursor.advance()){long value=snapshot.scalar(cursor.value());require(value>=0&&value<=255,Rule.OCTET,node,0);}}
        storage.remember(list,OCTETS);
    }
    private void part(long node) {
        positive(node,DECIMAL_TEXT_PART,1);text(node,DECIMAL_TEXT_PART,2,false);text(node,DECIMAL_TEXT_PART,3,false);
        int kind=(int)snapshot.scalar(field(node,DECIMAL_TEXT_PART,0));long count=field(node,DECIMAL_TEXT_PART,1);
        long size=keys.atomFact(field(node,DECIMAL_TEXT_PART,2),UNICODE_SCALARS),other=keys.atomFact(field(node,DECIMAL_TEXT_PART,3),UNICODE_SCALARS);
        boolean valid=switch(kind){case 0,1,2->size==0&&other==0;case 3->size>0&&other==0;case 4->one(count)&&size==1&&other==0;case 5->one(count)&&size>0&&size==other;case 6->one(count)&&size==1&&other==1;default->throw new IllegalStateException("invalid typed decimal kind");};
        require(valid,Rule.DECIMAL_PART,node,0);
    }
    private void format(long node) {
        long list=field(node,EXPRESSIONS_FORMAT_DECIMAL,2);if(storage.known(list,FORMAT))return;
        boolean digits=false,radix=false,star=false,space=false,floating=false;
        try(var cursor=snapshot.elements(list,DECIMAL_TEXT_PART)) {
            while(cursor.advance()) {
                long segment=cursor.value(); // Segment predicates are checked independently by the whole graph walk.
                int kind=(int)snapshot.scalar(field(segment,DECIMAL_TEXT_PART,0));
                switch(kind) {
                    case 0->digits=true;case 1->{digits=true;space=true;}case 2->{digits=true;star=true;}
                    case 4->{require(!radix,Rule.DECIMAL_FORMAT,node,2);radix=true;}
                    case 6->{require(!floating,Rule.DECIMAL_FORMAT,node,2);floating=true;}
                    default->{ }
                }
            }
        }
        require(digits&&!(star&&(space||floating)),Rule.DECIMAL_FORMAT,node,2);storage.remember(list,FORMAT);
    }
    private static void require(boolean condition,Rule rule,long node,int at){if(!condition)throw new Invalid(rule,node,at);}
    private void closeSuppressed(Throwable failure){try{close();}catch(RuntimeException|Error cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}}
    @Override public void close() {
        Storage owner=storage;storage=null;AirSnapshotBuilder.Lease lease=control;control=null;Throwable failure=null;
        try{if(owner!=null)owner.close();}catch(RuntimeException|Error error){failure=error;}
        try{if(lease!=null)lease.close();}catch(RuntimeException|Error error){if(failure==null)failure=error;else if(error!=failure)failure.addSuppressed(error);}
        if(failure instanceof RuntimeException error)throw error;if(failure instanceof Error error)throw error;
    }
}
