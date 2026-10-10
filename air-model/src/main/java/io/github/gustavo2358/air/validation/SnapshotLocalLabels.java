package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.AirSnapshot;
import io.github.gustavo2358.air.model.AirSnapshotBuilder;
import java.util.Arrays;
import java.util.Objects;
import static io.github.gustavo2358.air.model.AirShape.*;

/**
 * Owner-neutral I-02 local-label relation. Fold each typed LIST once, including invalid references;
 * select contextual complements without storing an error array per Unit. Borrowed input, complete
 * identity keys and frozen declarations must describe the same snapshot and outlive this owner.
 * Other reference/annotation rules and complete admission remain separate obligations.
 */
public final class SnapshotLocalLabels implements SnapshotDiagnosticTemplates.Projection,AutoCloseable {
    /**
     * Transferred empty managed storage. find exposes only finished tables; unfinished reuse fails.
     * Append rows in source order, preserving duplicates. finish builds sorted Unit posting spans
     * and missing ordinals in linear work using shared managed directories/payload, not an owner
     * or array per Unit. Query misses never insert. foreignAt selects the ordinal-th nonmatching
     * row through posting gaps in O(log matching count); absent buckets select directly.
     * All row/count/ordinal words are 64-bit. All growing state is accounted and spillable.
     */
    public interface Storage extends AutoCloseable {
        long find(long list);
        long begin(long list);
        void row(long table,long label,long unitKey,boolean missing);
        void finish(long table);
        long rows(long table);
        long matching(long table,long unitKey);
        long missing(long table);
        long label(long table,long rowOrdinal);
        long missingAt(long table,long ordinal);
        long foreignAt(long table,long unitKey,long ordinal);
        AirSnapshotBuilder.Lease claim(long bytes);
        @Override void close();
    }
    public enum Rule {
        DANGLING(1),CROSS_UNIT(2);
        private final int token;
        Rule(int token){this.token=token;}
        public int token(){return token;}
        public String code(){return "I-02";}
    }
    public record Counts(long lists,long rows) { }
    public static final int RECIPE=1;
    private final AirSnapshot snapshot;
    private final SnapshotIdentityKeys keys;
    private final SnapshotDeclarations declarations;
    private Storage storage;
    private AirSnapshotBuilder.Lease control;
    private long[] counts;
    private long lists,rows,currentTable,currentContext,currentOrdinal=-1,missingUsed,foreignUsed,
        currentRows,currentMissing,currentForeign;
    private boolean busy,failed;

    public SnapshotLocalLabels(AirSnapshot snapshot,SnapshotIdentityKeys keys,SnapshotDeclarations declarations,Storage storage) {
        this.snapshot=snapshot;this.keys=keys;this.declarations=declarations;this.storage=Objects.requireNonNull(storage);
        try {Objects.requireNonNull(snapshot).root();Objects.requireNonNull(keys);Objects.requireNonNull(declarations).entities();control=Objects.requireNonNull(storage.claim(512));counts=new long[5];}
        catch(RuntimeException|Error failure){closeSuppressed(failure);throw failure;}
    }
    public Counts counts(){open();return new Counts(lists,rows);}
    /** Bind semantic Unit only; reporting owner is applied later by the diagnostic sequence. */
    public long template(long list,long unit,SnapshotDiagnosticTemplates tape) {
        enter();
        try {
            Objects.requireNonNull(tape);if(snapshot.shape(unit)!=IDS_UNIT_ID||snapshot.shape(list)!=LIST)throw new IllegalArgumentException("local-label LIST and UnitId required");
            long table=table(list),context=keys.key(unit),length=storage.rows(table),missing=storage.missing(table),matching=storage.matching(table,context);
            checkCounts(length,missing,matching);counts[ValidationIssue.Kind.INVALID_IR.ordinal()]=Math.addExact(missing,length-matching);
            return tape.projected(RECIPE,table,context,counts);
        } catch(RuntimeException|Error failure){failed=true;throw failure;}
        finally{Arrays.fill(counts,0);busy=false;}
    }
    private long table(long list) {
        long table=storage.find(list);if(table!=0)return positive(table);
        table=positive(storage.begin(list));long length=0;
        try(var cursor=snapshot.elements(list,IDS_LABEL_ID)) {
            while(cursor.advance()) {
                long label=cursor.value(),unit=snapshot.field(label,IDS_LABEL_ID,0);
                boolean missing=declarations.fact(label,SnapshotDeclarations.Fact.NODE)==0;
                storage.row(table,label,keys.key(unit),missing);length=Math.incrementExact(length);
            }
        }
        long nextLists=Math.incrementExact(lists),nextRows=Math.addExact(rows,length);
        storage.finish(table);if(storage.rows(table)!=length)throw new IllegalStateException("local-label row count changed");
        lists=nextLists;rows=nextRows;return table;
    }
    /**
     * Merge missing and contextual foreign rows; ties emit dangling first. One primitive cursor
     * handles sequential prefixes without per-query history. Random access partitions two sorted
     * sequences in O(log missing * log matching), with no prefix replay or source read.
     */
    @Override public void occurrence(int recipe,long table,long context,long ordinal,long[] out) {
        enter();
        try {
            if(recipe!=RECIPE||table<=0||context<=0||ordinal<0||out==null||out.length!=5)throw new IllegalArgumentException("complete local-label projection descriptor required");
            if(currentTable!=table||currentContext!=context) {
                long length=storage.rows(table),missing=storage.missing(table),matching=storage.matching(table,context);
                checkCounts(length,missing,matching);currentRows=length;currentMissing=missing;currentForeign=length-matching;
                currentTable=table;currentContext=context;currentOrdinal=-1;missingUsed=foreignUsed=0;
            }
            long total=Math.addExact(currentMissing,currentForeign);if(ordinal>=total)throw new IndexOutOfBoundsException("local-label occurrence ordinal");
            long row;int rule;
            if(ordinal==currentOrdinal+1) {
                long a=missingUsed<currentMissing?missingAt(missingUsed):Long.MAX_VALUE;
                long b=foreignUsed<currentForeign?foreignAt(foreignUsed):Long.MAX_VALUE;
                if(a<=b){row=a;rule=Rule.DANGLING.token();missingUsed++;}else{row=b;rule=Rule.CROSS_UNIT.token();foreignUsed++;}
            } else {
                long k=ordinal+1,low=Math.max(0,k-currentForeign),high=Math.min(k,currentMissing);row=-1;rule=0;
                while(low<=high) {
                    long a=low+(high-low)/2,b=k-a;
                    // Missing precedes foreign at equal row. These strict/non-strict boundaries
                    // preserve both independent errors and duplicate label occurrences.
                    if(a>0&&b<currentForeign&&missingAt(a-1)>foreignAt(b)){high=a-1;continue;}
                    if(b>0&&a<currentMissing&&foreignAt(b-1)>=missingAt(a)){low=a+1;continue;}
                    long x=a==0?-1:missingAt(a-1),y=b==0?-1:foreignAt(b-1);
                    if(x>y){row=x;rule=Rule.DANGLING.token();}else{row=y;rule=Rule.CROSS_UNIT.token();}
                    missingUsed=a;foreignUsed=b;break;
                }
                if(row<0)throw new IllegalStateException("inconsistent local-label diagnostic order");
            }
            long label=positive(storage.label(table,row));currentOrdinal=ordinal;
            out[0]=ValidationIssue.Kind.INVALID_IR.ordinal();out[1]=rule;out[2]=label;out[3]=-1;out[4]=label;
        } catch(RuntimeException|Error failure){failed=true;throw failure;}
        finally{busy=false;}
    }
    private long missingAt(long ordinal){return row(storage.missingAt(currentTable,ordinal));}
    private long foreignAt(long ordinal){return row(storage.foreignAt(currentTable,currentContext,ordinal));}
    private long row(long row){if(row<0||row>=currentRows)throw new IllegalStateException("invalid local-label row ordinal");return row;}
    private static void checkCounts(long rows,long missing,long matching){if(rows<0||missing<0||missing>rows||matching<0||matching>rows)throw new IllegalStateException("invalid local-label relation counts");}
    private static long positive(long value){if(value<=0)throw new IllegalStateException("positive local-label table/anchor required");return value;}
    private void open(){if(storage==null||failed||busy)throw new IllegalStateException("local-label relation unavailable");}
    private void enter(){open();busy=true;}
    private void closeSuppressed(Throwable failure){try{close();}catch(RuntimeException|Error cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}}
    @Override public void close() {
        if(busy)throw new IllegalStateException("cannot close local-label relation during callback");
        Storage owner=storage;storage=null;AirSnapshotBuilder.Lease lease=control;control=null;if(counts!=null)Arrays.fill(counts,0);counts=null;Throwable failure=null;
        try{if(owner!=null)owner.close();}catch(RuntimeException|Error error){failure=error;}
        try{if(lease!=null)lease.close();}catch(RuntimeException|Error error){if(failure==null)failure=error;else if(error!=failure)failure.addSuppressed(error);}
        if(failure instanceof RuntimeException error)throw error;if(failure instanceof Error error)throw error;
    }
}
