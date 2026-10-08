package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.AirShape;
import io.github.gustavo2358.air.model.AirSnapshot;
import io.github.gustavo2358.air.model.AirSnapshotBuilder;
import java.util.Objects;
import static io.github.gustavo2358.air.model.AirShape.*;

/**
 * Required shared signature position/membership facts, including invalid lists. Borrows frozen
 * input and its atom key owner; owns spillable scratch. No reference/type/capability admission
 * or validation certificate is inferred. Calls/callbacks are sequential and non-reentrant.
 */
public final class SnapshotSignatureIndex implements AutoCloseable {
    /**
     * Empty on transfer; positive stable table handles until close. Exact (LIST source, kind)
     * keys use kind0=Parameter, kind1=ResultSlot. find exposes only finished tables and rejects
     * unfinished reuse. All growing metadata, membership and issue sequences must be managed
     * and spillable. Membership compares complete canonical INTEGER atom keys, including bad
     * positions. issue appends one row with separate open/closed ordering: unordered for open,
     * unordered OR noncontiguous for closed. select replaces a single primitive issue cursor;
     * early prefixes may be abandoned/reselected without retaining per-query cursor history.
     */
    public interface Storage extends AutoCloseable {
        long find(long list,int kind);
        long begin(long list,int kind);
        void member(long table,long atomKey);
        void issue(long table,long row,long position,long ordinal,boolean unordered,boolean noncontiguous);
        void finish(long table,long rows);
        long rows(long table);
        long issues(long table,boolean closed);
        boolean contains(long table,long atomKey);
        void select(long table,boolean closed);
        boolean advanceIssue();
        long row();
        long position();
        long ordinal();
        AirSnapshotBuilder.Lease claim(long bytes);
        @Override void close();
    }
    public enum Rule {
        POSITIONS("I-08"), PARAMETER_INITIAL("I-02");
        private final String code;
        Rule(String code){this.code=code;}
        public String code(){return code;}
    }
    /** Counts are separate from retention. Anchors are borrowed source addresses, not wire paths. */
    public interface Reports {
        long remaining();
        void occurrences(Rule rule,long ownerIdSource,long count);
        void retain(Rule rule,long ownerIdSource,long rowSource,long positionSource,long ordinal);
    }
    /** Unique folded lists/rows and distinct per-mode bad rows, never per-use diagnostic totals. */
    public record Counts(long lists,long rows,long openErrors,long closedErrors) { }
    private final AirSnapshot snapshot;
    private final SnapshotIdentityKeys keys;
    private Storage storage;
    private AirSnapshotBuilder.Lease control;
    private long lists,rows,openErrors,closedErrors;
    private boolean failed,busy;

    /** Transfers scratch even on construction failure. Input and atom keys remain caller-owned. */
    public SnapshotSignatureIndex(AirSnapshot snapshot,SnapshotIdentityKeys keys,Storage storage) {
        this.snapshot=snapshot;this.keys=keys;this.storage=Objects.requireNonNull(storage);
        try {Objects.requireNonNull(snapshot).root();Objects.requireNonNull(keys);control=Objects.requireNonNull(storage.claim(256));}
        catch(RuntimeException|Error failure){closeSuppressed(failure);throw failure;}
    }
    public Counts counts(){open();return new Counts(lists,rows,openErrors,closedErrors);}
    /** One owner occurrence; counts all bad rows and enumerates only the retained mode prefix. */
    public void checkInventory(long inventory,long owner,Reports reports) {
        enter();
        try {Objects.requireNonNull(reports);keys.key(owner);check(inventory,owner,reports);}
        catch(RuntimeException|Error failure){failed=true;throw failure;}
        finally{busy=false;}
    }
    /** Exact canonical membership, independent of inventory positional validity. */
    public boolean containsParameter(long inventory,long position) {
        enter();
        try {return contains(inventory,position);}
        catch(RuntimeException|Error failure){failed=true;throw failure;}
        finally{busy=false;}
    }
    /** Entry position checks, then I-02 for each missing ParameterInitial. Other rules are separate. */
    public void checkEntry(long entry,Reports reports) {
        enter();
        try {
            Objects.requireNonNull(reports);
            long owner=snapshot.field(entry,ENTRIES_ENTRY,0);keys.key(owner);
            long signature=snapshot.field(entry,ENTRIES_ENTRY,2);
            long parameters=snapshot.field(signature,INTERACTIONS_SIGNATURE,0);
            check(parameters,owner,reports);check(snapshot.field(signature,INTERACTIONS_SIGNATURE,1),owner,reports);
            long state=snapshot.field(entry,ENTRIES_ENTRY,3),conditions=snapshot.field(state,ENTRIES_ENTRY_STATE,0),ordinal=0;
            try(var cursor=snapshot.elements(conditions,ENTRIES_INITIAL_CONDITION)) {
                while(cursor.advance()) {
                    long row=cursor.value(),value=snapshot.field(row,ENTRIES_INITIAL_CONDITION,1);
                    if(snapshot.shape(value)==ENTRIES_PARAMETER_INITIAL) {
                        long position=snapshot.field(value,ENTRIES_PARAMETER_INITIAL,0);
                        if(!contains(parameters,position)) {
                            reports.occurrences(Rule.PARAMETER_INITIAL,owner,1);
                            if(capacity(reports)>0)reports.retain(Rule.PARAMETER_INITIAL,owner,row,position,ordinal);
                        }
                    }
                    ordinal=Math.incrementExact(ordinal);
                }
            }
        } catch(RuntimeException|Error failure){failed=true;throw failure;}
        finally{busy=false;}
    }
    private boolean contains(long inventory,long position) {
        if(snapshot.shape(inventory)!=INTERACTIONS_PARAMETER_INVENTORY||snapshot.shape(position)!=INTEGER)
            throw new IllegalArgumentException("parameter inventory and INTEGER position required");
        long table=table(snapshot.field(inventory,INTERACTIONS_PARAMETER_INVENTORY,0),0);
        return storage.contains(table,keys.atomKey(position));
    }
    private void check(long inventory,long owner,Reports reports) {
        AirShape shape=snapshot.shape(inventory);
        int kind;
        if(shape==INTERACTIONS_PARAMETER_INVENTORY)kind=0;
        else if(shape==INTERACTIONS_RESULT_INVENTORY)kind=1;
        else throw new IllegalArgumentException("signature inventory required");
        long table=table(snapshot.field(inventory,shape,0),kind);
        boolean closed=snapshot.shape(snapshot.field(inventory,shape,1))==INTERACTIONS_NO_REMAINDER;
        long count=storage.issues(table,closed);
        if(count<0)throw new IllegalStateException("negative signature issue count");
        if(count==0)return;
        reports.occurrences(Rule.POSITIONS,owner,count);
        long retained=Math.min(capacity(reports),count);
        if(retained==0)return;
        long length=storage.rows(table),previous=-1;
        if(count>length)throw new IllegalStateException("signature issue count exceeds row count");
        storage.select(table,closed);
        for(long n=0;n<retained;n++) {
            if(!storage.advanceIssue())throw new IllegalStateException("incomplete signature issue prefix");
            long row=storage.row(),position=storage.position(),ordinal=storage.ordinal();
            if(row<=0||position<=0||ordinal<=previous||ordinal>=length)
                throw new IllegalStateException("invalid signature issue anchors/order");
            reports.retain(Rule.POSITIONS,owner,row,position,ordinal);previous=ordinal;
        }
    }
    private long table(long list,int kind) {
        long table=storage.find(list,kind);
        if(table!=0)return positive(table);
        table=positive(storage.begin(list,kind));
        long length=0,previous=0,open=0,closed=0;
        AirShape rowShape=kind==0?INTERACTIONS_PARAMETER:INTERACTIONS_RESULT_SLOT;
        try(var cursor=snapshot.elements(list,rowShape)) {
            while(cursor.advance()) {
                long row=cursor.value(),position=snapshot.field(row,rowShape,0);
                storage.member(table,keys.atomKey(position));
                boolean unordered=previous!=0&&keys.compareIntegers(position,previous)<=0;
                boolean noncontiguous=!keys.integerEqualsNatural(position,length);
                if(unordered||noncontiguous)storage.issue(table,row,position,length,unordered,noncontiguous);
                if(unordered)open=Math.incrementExact(open);
                if(unordered||noncontiguous)closed=Math.incrementExact(closed);
                length=Math.incrementExact(length);previous=position;
            }
        }
        long nextLists=Math.incrementExact(lists),nextRows=Math.addExact(rows,length);
        long nextOpen=Math.addExact(openErrors,open),nextClosed=Math.addExact(closedErrors,closed);
        storage.finish(table,length);
        lists=nextLists;rows=nextRows;openErrors=nextOpen;closedErrors=nextClosed;
        return table;
    }
    private static long capacity(Reports reports){long capacity=reports.remaining();if(capacity<0)throw new IllegalStateException("negative diagnostic retention capacity");return capacity;}
    private static long positive(long handle){if(handle<=0)throw new IllegalStateException("positive signature table required");return handle;}
    private void open(){if(storage==null||failed||busy)throw new IllegalStateException("signature index is unavailable");}
    private void enter(){open();busy=true;}
    private void closeSuppressed(Throwable failure){try{close();}catch(RuntimeException|Error cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}}
    @Override public void close() {
        if(busy)throw new IllegalStateException("cannot close signature index during a callback");
        Storage owner=storage;storage=null;AirSnapshotBuilder.Lease lease=control;control=null;Throwable failure=null;
        try{if(owner!=null)owner.close();}catch(RuntimeException|Error error){failure=error;}
        try{if(lease!=null)lease.close();}catch(RuntimeException|Error error){if(failure==null)failure=error;else if(error!=failure)failure.addSuppressed(error);}
        if(failure instanceof RuntimeException error)throw error;if(failure instanceof Error error)throw error;
    }
}
