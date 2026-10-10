package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import java.util.Arrays;
import java.util.Objects;
import static io.github.gustavo2358.air.model.AirShape.*;

/** Owner-neutral fact-scope, precision and coverage recipes. Not a complete validation certificate. */
public final class SnapshotAnnotationTemplates implements AutoCloseable {
    /** Exact source/recipe memo plus list-local complete text membership, all growing state managed. */
    public interface Storage extends SnapshotReferenceLists.Storage {
        /** First occurrence in this unfinished item-list table. No membership shared across lists. */
        boolean first(long table,long textKey);
    }
    /** Detail variants retain exact dimension/rule; final complete admission supplies the renderer. */
    public enum Rule {
        PRECISION_CONTROL(100,"I-32"),PRECISION_STORAGE(101,"I-32"),PRECISION_EFFECTS(102,"I-32"),
        PRECISION_VALUES(103,"I-32"),PRECISION_DEPENDENCIES(104,"I-32"),
        INVENTORY_REASON(110,"I-28"),DUPLICATE_SOURCE_KEY(111,"I-29"),
        DISAPPEARED_ITEM(112,"I-30"),INCOMPLETE_ITEM(113,"I-30");
        private final int token;private final String code;
        Rule(int token,String code){this.token=token;this.code=code;}
        public int token(){return token;}public String code(){return code;}
    }
    public record Counts(long recipes,long items) { }
    private static final int SCOPE=0,CLAIM=1,PRECISION=2,ITEM=3,COVERAGE=4,ITEMS=5;
    private final AirSnapshot input;
    private final SnapshotIdentityKeys keys;
    private final SnapshotReferenceLists refs;
    private final SnapshotDiagnosticTemplates tape;
    private Storage storage;
    private AirSnapshotBuilder.Lease control;
    private long[] forest;
    private long leaves,recipes,items;
    private boolean busy,failed;

    /** Transfers an empty storage. All other owners describe the same input and remain borrowed. */
    public SnapshotAnnotationTemplates(AirSnapshot input,SnapshotIdentityKeys keys,SnapshotReferenceLists refs,SnapshotDiagnosticTemplates tape,Storage storage) {
        this.input=input;this.keys=keys;this.refs=refs;this.tape=tape;this.storage=Objects.requireNonNull(storage);
        try {
            Objects.requireNonNull(input).root();Objects.requireNonNull(keys);Objects.requireNonNull(refs).counts();Objects.requireNonNull(tape).size(0);
            control=Objects.requireNonNull(storage.claim(1024));forest=new long[64];borrowed();
        }catch(RuntimeException|Error failure){closeSuppressed(failure);throw failure;}
    }
    public Counts counts(){open();return new Counts(recipes,items);}
    public long scope(long source){return template(source,SCOPE);}
    /** Context-free claim references; dimension-specific I-32 belongs to precision assembly. */
    public long claim(long source){return template(source,CLAIM);}
    public long precision(long source){return template(source,PRECISION);}
    public long coverage(long source){return template(source,COVERAGE);}
    private long template(long source,int recipe) {
        open();busy=true;
        try{borrowed();return build(source,recipe);}
        catch(RuntimeException|Error failure){failed=true;throw failure;}
        finally{Arrays.fill(forest,0);leaves=0;busy=false;}
    }
    private void borrowed(){input.root();keys.key(input.field(input.root(),PUBLICATION,0));refs.counts();tape.size(0);}
    private long build(long source,int recipe) {
        AirShape shape=input.shape(source);
        boolean correct=switch(recipe){case SCOPE->SCOPES_FACT_SCOPE.accepts(shape);case CLAIM->shape==EVIDENCE_CLAIM;case PRECISION->shape==EVIDENCE_PRECISION;case ITEM->shape==EVIDENCE_COVERAGE_ITEM;case COVERAGE->shape==EVIDENCE_COVERAGE;case ITEMS->shape==LIST;default->false;};
        if(!correct)throw new IllegalArgumentException("wrong annotation recipe shape");
        long table=storage.find(source,recipe);if(table!=0)return root(table);
        table=positive(storage.begin(source,recipe));long result;
        // Recipe dependencies are an acyclic fixed schema: coverage -> item-list -> item;
        // precision -> claim -> fact-scope. No input-depth recursion or resident object stack.
        switch(recipe) {
            case SCOPE -> result=switch(shape){
                case SCOPES_PUBLICATION_SCOPE,SCOPES_UNIT_SCOPE -> refs.reference(input.field(source,shape,0));
                case SCOPES_ENTITY_SCOPE -> refs.references(input.field(source,shape,0),IDS_ID);
                default->throw new IllegalStateException("concrete fact scope required");
            };
            case CLAIM -> result=tape.concat(build(field(source,0),SCOPE),refs.references(field(source,2),IDS_UNCERTAINTY_ID));
            case PRECISION -> {
                result=0;
                for(int dimension=0;dimension<5;dimension++) {
                    long claim=field(source,dimension);result=tape.concat(result,build(claim,CLAIM));
                    long status=enumeration(input.field(claim,EVIDENCE_CLAIM,1),EVIDENCE_PRECISION_STATUS);
                    if((status==2||status==3)&&input.size(input.field(claim,EVIDENCE_CLAIM,2))==0)
                        result=tape.concat(result,issue(100+dimension,claim,-1,claim));
                }
            }
            case ITEM -> {
                long outputs=field(source,3),uncertainties=field(source,4),elimination=field(source,5);
                result=refs.reference(field(source,1));result=tape.concat(result,refs.references(outputs,IDS_ID));
                result=tape.concat(result,refs.references(uncertainties,IDS_UNCERTAINTY_ID));
                if(input.size(elimination)!=0)result=tape.concat(result,refs.reference(input.field(input.element(elimination,EVIDENCE_ELIMINATION,0),EVIDENCE_ELIMINATION,1)));
                if(input.size(outputs)==0&&input.size(uncertainties)==0&&input.size(elimination)==0)result=tape.concat(result,issue(112,source,-1,source));
                if(enumeration(field(source,2),EVIDENCE_COVERAGE_STATUS)!=0&&input.size(uncertainties)==0)result=tape.concat(result,issue(113,source,-1,source));
            }
            case COVERAGE -> {
                long uncertainties=field(source,3);result=build(field(source,1),SCOPE);
                result=tape.concat(result,refs.references(uncertainties,IDS_UNCERTAINTY_ID));
                if(enumeration(field(source,0),EVIDENCE_INVENTORY_STATUS)!=0&&input.size(uncertainties)==0)result=tape.concat(result,issue(110,source,-1,source));
                result=tape.concat(result,build(field(source,2),ITEMS));
            }
            case ITEMS -> {
                long rows=0;
                try(var cursor=input.elements(source,EVIDENCE_COVERAGE_ITEM)) {
                    while(cursor.advance()) {
                        long item=cursor.value(),text=input.field(item,EVIDENCE_COVERAGE_ITEM,0);
                        if(!storage.first(table,keys.atomKey(text)))append(issue(111,item,0,text));
                        append(build(item,ITEM));rows=Math.incrementExact(rows);
                    }
                }
                result=0;for(int level=63;level>=0;level--)if(forest[level]!=0)result=tape.concat(result,forest[level]);
                items=Math.addExact(items,rows);Arrays.fill(forest,0);leaves=0;
            }
            default -> throw new IllegalStateException("unknown annotation recipe");
        }
        long next=Math.incrementExact(recipes);storage.finish(table,result);recipes=next;return result;
    }
    private long field(long source,int ordinal){return input.field(source,input.shape(source),ordinal);}
    private long enumeration(long source,AirShape expected){if(input.shape(source)!=expected)throw new IllegalStateException("wrong annotation enum shape");return input.scalar(source);}
    private long issue(int rule,long source,int field,long detail){return tape.leaf(ValidationIssue.Kind.INVALID_IR,rule,source,field,detail);}
    private void append(long root){if(root==0)return;long carry=leaves;leaves=Math.incrementExact(leaves);int level=0;while((carry&1)!=0){root=tape.concat(forest[level],root);forest[level++]=0;carry>>>=1;}if(level==64)throw new IllegalStateException("annotation carry capacity");forest[level]=root;}
    private long root(long table){long root=storage.root(positive(table));if(root<0)throw new IllegalStateException("negative annotation root");tape.size(root);return root;}
    private static long positive(long handle){if(handle<=0)throw new IllegalStateException("positive annotation table required");return handle;}
    private void open(){if(storage==null||failed||busy)throw new IllegalStateException("annotation recipes unavailable");}
    private void closeSuppressed(Throwable failure){try{close();}catch(RuntimeException|Error cleanup){if(failure!=cleanup)failure.addSuppressed(cleanup);}}
    @Override public void close() {
        if(busy)throw new IllegalStateException("cannot close annotation memo during callback");Storage owner=storage;storage=null;AirSnapshotBuilder.Lease lease=control;control=null;forest=null;Throwable failure=null;
        try{if(owner!=null)owner.close();}catch(RuntimeException|Error error){failure=error;}
        try{if(lease!=null)lease.close();}catch(RuntimeException|Error error){if(failure==null)failure=error;else if(error!=failure)failure.addSuppressed(error);}
        if(failure instanceof RuntimeException error)throw error;if(failure instanceof Error error)throw error;
    }
}
