package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.AirShape;
import io.github.gustavo2358.air.model.AirSnapshot;
import io.github.gustavo2358.air.model.AirSnapshotBuilder;
import java.util.Arrays;
import java.util.Objects;
import static io.github.gustavo2358.air.model.AirShape.*;

/**
 * Shared owner-neutral I-02 existence templates for actual reference call sites. Declaration IDs
 * and namespace parents are not implicit references. Borrowed snapshot/declarations/templates
 * describe one frozen input and outlive this memo. Contextual rules remain separate obligations.
 */
public final class SnapshotReferenceLists implements AutoCloseable {
    /**
     * Empty managed memo on transfer; exact (source,kind) key. kind0 is a single ID; positive kind
     * is expected ID element shape ordinal+1 for a LIST. find exposes only finished tables, even
     * when their root is0; unknown is table0. Unfinished reuse and repeated begin/finish fail.
     * Stored template roots are borrowed literal handles, not owned tuple references. No query
     * insertion/retained history; all growing keys/roots/status/index state is spillable/funded.
     */
    public interface Storage extends AutoCloseable {
        long find(long source,int kind);
        long begin(long source,int kind);
        void finish(long table,long root);
        long root(long table);
        AirSnapshotBuilder.Lease claim(long bytes);
        @Override void close();
    }
    public record Counts(long lists,long rows,long identities) { }
    private final AirSnapshot snapshot;
    private final SnapshotDeclarations declarations;
    private final SnapshotDiagnosticTemplates tape;
    private Storage storage;
    private AirSnapshotBuilder.Lease control;
    private long[] forest;
    private long leaves,lists,rows,identities;
    private boolean busy,failed;

    public SnapshotReferenceLists(AirSnapshot snapshot,SnapshotDeclarations declarations,SnapshotDiagnosticTemplates tape,Storage storage) {
        this.snapshot=snapshot;this.declarations=declarations;this.tape=tape;this.storage=Objects.requireNonNull(storage);
        try {Objects.requireNonNull(snapshot).root();Objects.requireNonNull(declarations).entities();Objects.requireNonNull(tape).size(0);control=Objects.requireNonNull(storage.claim(1024));forest=new long[64];}
        catch(RuntimeException|Error failure){closeSuppressed(failure);throw failure;}
    }
    public Counts counts(){open();return new Counts(lists,rows,identities);}
    /** Existence only; this explicit call site, not an automatic walk of every ID-shaped node. */
    public long reference(long identity) {
        enter();
        try {borrowed();return single(identity);}
        catch(RuntimeException|Error failure){failed=true;throw failure;}
        finally{leave();}
    }
    /** Fold once, cache success as well as errors, preserve source order and duplicate occurrences. */
    public long references(long list,AirShape element) {
        enter();
        try {
            borrowed();Objects.requireNonNull(element);
            if(snapshot.shape(list)!=LIST||element!=IDS_ID&&!IDS_ID.accepts(element))throw new IllegalArgumentException("typed reference LIST and ID element required");
            int kind=element.ordinal()+1;long table=storage.find(list,kind);if(table!=0)return root(table);
            table=positive(storage.begin(list,kind));long length=0;
            try(var cursor=snapshot.elements(list,element)) {
                while(cursor.advance()){append(single(cursor.value()));length=Math.incrementExact(length);}
            }
            long result=0;for(int level=forest.length-1;level>=0;level--)if(forest[level]!=0)result=tape.concat(result,forest[level]);
            long nextLists=Math.incrementExact(lists),nextRows=Math.addExact(rows,length);
            storage.finish(table,result);lists=nextLists;rows=nextRows;return result;
        } catch(RuntimeException|Error failure){failed=true;throw failure;}
        finally{leave();}
    }
    private void borrowed(){snapshot.root();declarations.entities();tape.size(0);}
    private long single(long identity) {
        if(!IDS_ID.accepts(snapshot.shape(identity)))throw new IllegalArgumentException("typed reference ID required");
        long table=storage.find(identity,0);if(table!=0)return root(table);
        table=positive(storage.begin(identity,0));long result=0;
        if(declarations.fact(identity,SnapshotDeclarations.Fact.NODE)==0)
            result=tape.leaf(ValidationIssue.Kind.INVALID_IR,SnapshotLocalLabels.Rule.DANGLING.token(),identity,-1,identity);
        long next=Math.incrementExact(identities);storage.finish(table,result);identities=next;return result;
    }
    /** Binary carry forest: emitted leaves and eventual subtrees, not every growing append prefix. */
    private void append(long root) {
        if(root==0)return;
        long carry=leaves;leaves=Math.incrementExact(leaves);int level=0;
        while((carry&1)!=0){root=tape.concat(forest[level],root);forest[level++]=0;carry>>>=1;}
        if(level>=forest.length)throw new IllegalStateException("reference template count exceeds forest capacity");forest[level]=root;
    }
    private long root(long table){long root=storage.root(positive(table));if(root<0)throw new IllegalStateException("negative reference template root");tape.size(root);return root;}
    private static long positive(long value){if(value<=0)throw new IllegalStateException("positive reference memo table required");return value;}
    private void open(){if(storage==null||failed||busy)throw new IllegalStateException("reference templates unavailable");}
    private void enter(){open();busy=true;}
    private void leave(){Arrays.fill(forest,0);leaves=0;busy=false;}
    private void closeSuppressed(Throwable failure){try{close();}catch(RuntimeException|Error cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}}
    @Override public void close() {
        if(busy)throw new IllegalStateException("cannot close reference memo during callback");
        Storage owner=storage;storage=null;AirSnapshotBuilder.Lease lease=control;control=null;if(forest!=null)Arrays.fill(forest,0);forest=null;Throwable failure=null;
        try{if(owner!=null)owner.close();}catch(RuntimeException|Error error){failure=error;}
        try{if(lease!=null)lease.close();}catch(RuntimeException|Error error){if(failure==null)failure=error;else if(error!=failure)failure.addSuppressed(error);}
        if(failure instanceof RuntimeException error)throw error;if(failure instanceof Error error)throw error;
    }
}
