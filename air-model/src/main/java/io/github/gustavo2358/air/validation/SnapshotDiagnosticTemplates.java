package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.AirSnapshotBuilder;
import java.util.Arrays;
import java.util.Objects;

/**
 * Shared ordered diagnostic occurrences, not a validity certificate. Immutable balanced sequence
 * tuples retain multiplicity/counts/anchors, while owner binding and retained rendering stay outside.
 * All growing tuples use transferred managed storage. Operations/callbacks are non-reentrant.
 */
public final class SnapshotDiagnosticTemplates implements AutoCloseable {
    /** Stable exact tuple schema; only LEFT/RIGHT are owned template references, other words literal. */
    public enum Word {
        LEFT,RIGHT,HEIGHT,TOTAL,INVALID,UNSUPPORTED,OBLIGATION,VALIDATION_LIMIT,RESOURCE_LIMIT,
        KIND,RULE,ANCHOR,FIELD,DETAIL
    }
    /**
     * Empty on transfer. Intern copies the fixed14word staging tuple and uses exact equality even
     * under collisions; positive handles are stable until close. word is immutable/read-only.
     * Required tuple payload and indexes must be accounted and spillable. No collection before
     * close, so parent joins, current roots and retained callers cannot expire independently.
     */
    public interface Storage extends AutoCloseable {
        long intern(long[] tuple);
        long word(long handle,Word field);
        AirSnapshotBuilder.Lease claim(long bytes);
        @Override void close();
    }
    /** Counts are separate from retention; rule tokens/anchors are interpreted by the admission pass. */
    public interface Reports {
        long remaining();
        void occurrences(ValidationIssue.Kind kind,long ownerSource,long count);
        void retain(ValidationIssue.Kind kind,int rule,long ownerSource,long anchorSource,int field,long detailSource);
    }
    /**
     * Borrowed frozen admission relation, matching recipe/relation/context and cached counts.
     * Resolves only a requested zero-based occurrence into five fixed output words:
     * kind ordinal, positive rule token, source anchor, field(-1 absent), detail source.
     * Must write every word, preserve exact order/multiplicity, and propagate operational faults.
     * The template owner neither closes nor mutates this relation. No admission is inferred.
     */
    @FunctionalInterface public interface Projection {
        void occurrence(int recipe,long relation,long context,long ordinal,long[] output);
    }
    private static final ValidationIssue.Kind[] KINDS=ValidationIssue.Kind.values();
    private static final Word[] COUNT_WORDS={Word.INVALID,Word.UNSUPPORTED,Word.OBLIGATION,Word.VALIDATION_LIMIT,Word.RESOURCE_LIMIT};
    // AVL min leaves follow Fibonacci: height92 already needs >Long.MAX_VALUE occurrences.
    //96 provides conservative fixed scratch; checked counts and balanced joins enforce the proof.
    private static final int MAX_HEIGHT=96;
    private static final int PROJECTED=8;
    private final Projection projection;
    private Storage storage;
    private AirSnapshotBuilder.Lease control;
    private long[] staged,path,projected;
    private boolean busy,failed;

    public SnapshotDiagnosticTemplates(Storage storage) {this(storage,null);}
    /** Transfers storage; projection is optional for literal-only use and always borrowed. */
    public SnapshotDiagnosticTemplates(Storage storage,Projection projection) {
        this.projection=projection;
        this.storage=Objects.requireNonNull(storage);
        try {
            if(KINDS.length!=5)throw new IllegalStateException("diagnostic kind schema changed");
            control=Objects.requireNonNull(storage.claim(2048));staged=new long[14];path=new long[MAX_HEIGHT];projected=new long[5];
        } catch(RuntimeException|Error failure){closeSuppressed(failure);throw failure;}
    }
    /**
     * Lazy ordered chunk from a frozen exact relation. Copy five cached kind counts into a single
     * height-one tuple; an all-zero chunk is empty. No occurrence or per-context error array is
     * materialized. The semantic relation owner proves these counts before calling this method.
     */
    public long projected(int recipe,long relation,long context,long[] counts) {
        enter();
        try {
            if(projection==null)throw new IllegalStateException("projected diagnostic relation unavailable");
            Objects.requireNonNull(counts);
            if(recipe<=0||relation<=0||context<0||counts.length!=5)throw new IllegalArgumentException("projection recipe/relation/context and five counts required");
            long total=0;
            for(int n=0;n<5;n++){if(counts[n]<0)throw new IllegalArgumentException("nonnegative projected counts required");total=Math.addExact(total,counts[n]);staged[COUNT_WORDS[n].ordinal()]=counts[n];}
            if(total==0)return 0;
            staged[Word.HEIGHT.ordinal()]=1;staged[Word.TOTAL.ordinal()]=total;staged[Word.KIND.ordinal()]=PROJECTED;
            staged[Word.RULE.ordinal()]=recipe;staged[Word.ANCHOR.ordinal()]=relation;staged[Word.DETAIL.ordinal()]=context;
            return positive(storage.intern(staged));
        } catch(RuntimeException|Error failure){failed=true;throw failure;}
        finally{leave();}
    }
    /** Empty sequence is0; external rule token positive, anchor/detail0 permitted, field-1 is absent. */
    public long leaf(ValidationIssue.Kind kind,int rule,long anchor,int field,long detail) {
        enter();
        try {
            Objects.requireNonNull(kind);
            if(rule<=0||anchor<0||field< -1||detail<0)throw new IllegalArgumentException("diagnostic rule/anchors required");
            staged[Word.HEIGHT.ordinal()]=1;staged[Word.TOTAL.ordinal()]=1;staged[COUNT_WORDS[kind.ordinal()].ordinal()]=1;
            staged[Word.KIND.ordinal()]=kind.ordinal()+1L;staged[Word.RULE.ordinal()]=rule;
            staged[Word.ANCHOR.ordinal()]=anchor;staged[Word.FIELD.ordinal()]=field;staged[Word.DETAIL.ordinal()]=detail;
            return positive(storage.intern(staged));
        } catch(RuntimeException|Error failure){failed=true;throw failure;}
        finally{leave();}
    }
    /** Semantic ordered concatenation: same child twice retains twice its occurrences. */
    public long concat(long left,long right) {
        enter();
        try {
            nonnegative(left);nonnegative(right);
            if(left==0)return right;if(right==0)return left;
            Math.addExact(total(left),total(right)); // Reject overflow before producing join tuples.
            int lh=nodeHeight(left),rh=nodeHeight(right),depth=0;
            if(lh>rh+1) {
                while(lh>rh+1){if(depth==MAX_HEIGHT)throw new IllegalStateException("diagnostic join height exceeds capacity");path[depth++]=child(left,Word.LEFT);left=child(left,Word.RIGHT);lh=nodeHeight(left);}
                long result=branch(left,right);
                while(depth!=0)result=balance(path[--depth],result);
                return result;
            }
            if(rh>lh+1) {
                while(rh>lh+1){if(depth==MAX_HEIGHT)throw new IllegalStateException("diagnostic join height exceeds capacity");path[depth++]=child(right,Word.RIGHT);right=child(right,Word.LEFT);rh=nodeHeight(right);}
                long result=branch(left,right);
                while(depth!=0)result=balance(result,path[--depth]);
                return result;
            }
            return branch(left,right);
        } catch(RuntimeException|Error failure){failed=true;throw failure;}
        finally{leave();}
    }
    public long size(long root) {
        enter();try{nonnegative(root);return root==0?0:total(root);}
        catch(RuntimeException|Error failure){failed=true;throw failure;}finally{leave();}
    }
    public long count(long root,ValidationIssue.Kind kind) {
        enter();try{nonnegative(root);Objects.requireNonNull(kind);return root==0?0:countWord(root,COUNT_WORDS[kind.ordinal()]);}
        catch(RuntimeException|Error failure){failed=true;throw failure;}finally{leave();}
    }
    public int height(long root) {
        enter();try{nonnegative(root);return root==0?0:nodeHeight(root);}
        catch(RuntimeException|Error failure){failed=true;throw failure;}finally{leave();}
    }
    /** Bulk exact kind counts; a zero-capacity report performs no tree traversal or query insertion. */
    public void emit(long root,long owner,Reports reports) {
        enter();
        try {
            nonnegative(root);nonnegative(owner);Objects.requireNonNull(reports);if(root==0)return;
            long total=total(root),sum=0;
            for(int n=0;n<KINDS.length;n++){long count=countWord(root,COUNT_WORDS[n]);sum=Math.addExact(sum,count);if(count!=0)reports.occurrences(KINDS[n],owner,count);}
            if(sum!=total)throw new IllegalStateException("diagnostic kind counts disagree with occurrences");
            long capacity=reports.remaining();if(capacity<0)throw new IllegalStateException("negative diagnostic capacity");
            long retained=Math.min(capacity,total);if(retained==0)return;
            int depth=1;path[0]=root;long emitted=0;
            while(emitted<retained) {
                if(depth==0)throw new IllegalStateException("incomplete diagnostic prefix");
                long node=path[--depth],kind=storage.word(node,Word.KIND);
                if(kind==0) {
                    if(depth>MAX_HEIGHT-2)throw new IllegalStateException("diagnostic cursor height exceeds capacity");
                    long left=child(node,Word.LEFT),right=child(node,Word.RIGHT);
                    path[depth++]=right;path[depth++]=left;
                } else if(kind==PROJECTED) {
                    if(projection==null)throw new IllegalStateException("projected diagnostic relation unavailable");
                    long length=total(node),recipe=storage.word(node,Word.RULE),relation=storage.word(node,Word.ANCHOR),context=storage.word(node,Word.DETAIL);
                    if(recipe<=0||recipe>Integer.MAX_VALUE||relation<=0||context<0)throw new IllegalStateException("invalid projected descriptor");
                    long prefix=Math.min(length,retained-emitted);
                    for(long at=0;at<prefix;at++) {
                        Arrays.fill(projected,Long.MIN_VALUE);projection.occurrence((int)recipe,relation,context,at,projected);
                        long actualKind=projected[0],rule=projected[1],anchor=projected[2],field=projected[3],detail=projected[4];
                        if(actualKind<0||actualKind>=KINDS.length||rule<=0||rule>Integer.MAX_VALUE||anchor<0||field< -1||field>Integer.MAX_VALUE||detail<0)
                            throw new IllegalStateException("incomplete or invalid projected occurrence");
                        reports.retain(KINDS[(int)actualKind],(int)rule,owner,anchor,(int)field,detail);emitted++;
                    }
                } else {
                    if(kind<1||kind>KINDS.length||total(node)!=1)throw new IllegalStateException("invalid diagnostic leaf");
                    int rule=Math.toIntExact(storage.word(node,Word.RULE)),field=Math.toIntExact(storage.word(node,Word.FIELD));
                    long anchor=storage.word(node,Word.ANCHOR),detail=storage.word(node,Word.DETAIL);
                    if(rule<=0||field< -1||anchor<0||detail<0)throw new IllegalStateException("invalid diagnostic leaf anchors");
                    reports.retain(KINDS[(int)kind-1],rule,owner,anchor,field,detail);emitted++;
                }
            }
        } catch(RuntimeException|Error failure){failed=true;throw failure;}
        finally{leave();}
    }
    private long balance(long left,long right) {
        int delta=nodeHeight(left)-nodeHeight(right);
        if(delta>2||delta< -2)throw new IllegalStateException("unbalanced diagnostic join");
        if(delta==2) {
            long ll=child(left,Word.LEFT),lr=child(left,Word.RIGHT);
            if(nodeHeight(ll)>=nodeHeight(lr))return branch(ll,branch(lr,right));
            return branch(branch(ll,child(lr,Word.LEFT)),branch(child(lr,Word.RIGHT),right));
        }
        if(delta== -2) {
            long rl=child(right,Word.LEFT),rr=child(right,Word.RIGHT);
            if(nodeHeight(rr)>=nodeHeight(rl))return branch(branch(left,rl),rr);
            return branch(branch(left,child(rl,Word.LEFT)),branch(child(rl,Word.RIGHT),rr));
        }
        return branch(left,right);
    }
    private long branch(long left,long right) {
        int lh=nodeHeight(left),rh=nodeHeight(right),height=Math.max(lh,rh)+1;
        if(Math.abs(lh-rh)>1||height>MAX_HEIGHT)throw new IllegalStateException("diagnostic branch height disagreement");
        Arrays.fill(staged,0);staged[Word.LEFT.ordinal()]=left;staged[Word.RIGHT.ordinal()]=right;
        staged[Word.HEIGHT.ordinal()]=height;staged[Word.TOTAL.ordinal()]=Math.addExact(total(left),total(right));
        for(Word word:COUNT_WORDS)staged[word.ordinal()]=Math.addExact(countWord(left,word),countWord(right,word));
        return positive(storage.intern(staged));
    }
    private long child(long node,Word field){return positive(storage.word(node,field));}
    private long total(long node){return positive(storage.word(node,Word.TOTAL));}
    private long countWord(long node,Word field){long value=storage.word(node,field);if(value<0)throw new IllegalStateException("negative diagnostic count");return value;}
    private int nodeHeight(long node){long value=storage.word(node,Word.HEIGHT);if(value<1||value>MAX_HEIGHT)throw new IllegalStateException("invalid diagnostic height");return (int)value;}
    private static long positive(long value){if(value<=0)throw new IllegalStateException("positive diagnostic handle/count required");return value;}
    private static void nonnegative(long value){if(value<0)throw new IllegalArgumentException("nonnegative diagnostic handle/anchor required");}
    private void enter(){if(storage==null||failed||busy)throw new IllegalStateException("diagnostic owner unavailable");busy=true;}
    private void leave(){if(staged!=null)Arrays.fill(staged,0);if(path!=null)Arrays.fill(path,0);if(projected!=null)Arrays.fill(projected,0);busy=false;}
    private void closeSuppressed(Throwable failure){try{close();}catch(RuntimeException|Error cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}}
    @Override public void close() {
        if(busy)throw new IllegalStateException("cannot close diagnostic owner during callback");
        Storage owner=storage;storage=null;AirSnapshotBuilder.Lease lease=control;control=null;Throwable failure=null;
        try{if(owner!=null)owner.close();}catch(RuntimeException|Error error){failure=error;}
        try{if(lease!=null)lease.close();}catch(RuntimeException|Error error){if(failure==null)failure=error;else if(error!=failure)failure.addSuppressed(error);}
        staged=path=projected=null;
        if(failure instanceof RuntimeException error)throw error;if(failure instanceof Error error)throw error;
    }
}
