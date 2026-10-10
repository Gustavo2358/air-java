package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.AirSnapshotBuilder;
import java.util.*;

/** Independent flat tiny oracle; giant multiplicities use exact literal algebra, never expansion. */
final class SnapshotDiagnosticChecks {
    private SnapshotDiagnosticChecks() { }
    private record Issue(ValidationIssue.Kind kind,int rule,long anchor,int field,long detail) { }
    private static final ValidationIssue.Kind[] KINDS=ValidationIssue.Kind.values();
    private static Issue issue(int n){return new Issue(KINDS[n%KINDS.length],100+n,(1L<<44)+37L*n,n%7-1,n+1L);}
    private static long leaf(SnapshotDiagnosticTemplates tape,Issue issue){return tape.leaf(issue.kind(),issue.rule(),issue.anchor(),issue.field(),issue.detail());}

    static void independentSequencesPreserveKindsOrderMultiplicityAndOwner() {
        var random=new Random(0xa118L);
        for(int run=0;run<96;run++) {
            var store=new Store();
            try(var tape=new SnapshotDiagnosticTemplates(store)) {
                long root=0;var expected=new ArrayList<Issue>();
                for(int n=0;n<80;n++) {
                    var item=issue(random.nextInt(24));long row=leaf(tape,item);
                    if(random.nextBoolean()){root=tape.concat(root,row);expected.add(item);}
                    else{root=tape.concat(row,root);expected.addFirst(item);}
                    if(n%19==0&&expected.size()<256){root=tape.concat(root,root);expected.addAll(new ArrayList<>(expected));}
                }
                assertSequence(tape,root,expected);
                eq(root,tape.concat(root,0));eq(root,tape.concat(0,root));eq(0L,tape.concat(0,0));
                var empty=new Report(100);tape.emit(0,42,empty);eq(List.of(),empty.items);eq(0L,empty.total());
            }
            eq(0L,store.claimed);eq(true,store.closed);
        }
        var collision=new Store();collision.collisions=true;
        try(var tape=new SnapshotDiagnosticTemplates(collision)) {
            var a=issue(0);var b=issue(1);long x=leaf(tape,a),y=leaf(tape,b);eq(x,leaf(tape,a));eq(false,x==y);assertSequence(tape,tape.concat(x,y),List.of(a,b));
        }
    }
    private static void assertSequence(SnapshotDiagnosticTemplates tape,long root,List<Issue> expected) {
        var r=new Report(Long.MAX_VALUE);tape.emit(root,(1L<<45)+1,r);eq(expected,r.items);eq((long)expected.size(),tape.size(root));eq((1L<<45)+1,r.owner);
        for(var kind:KINDS){long count=0;for(var item:expected)if(item.kind()==kind)count++;eq(count,tape.count(root,kind));eq(count,r.counts[kind.ordinal()]);}
    }
    static void randomSubtreeJoinsMatchIndependentFlatParenthesizations() {
        var random=new Random(0xb17L);var store=new Store();
        try(var tape=new SnapshotDiagnosticTemplates(store)) {
            for(int run=0;run<64;run++) {
                var roots=new ArrayList<Long>();var expected=new ArrayList<List<Issue>>();
                for(int part=0;part<24;part++) {
                    int size=random.nextInt(100);long root=0;var items=new ArrayList<Issue>();
                    for(int at=0;at<size;at++){var item=issue(random.nextInt(32));root=tape.concat(root,leaf(tape,item));items.add(item);}
                    roots.add(root);expected.add(items);
                }
                while(roots.size()>1) {
                    int at=random.nextInt(roots.size()-1);long root=tape.concat(roots.get(at),roots.get(at+1));
                    var items=new ArrayList<>(expected.get(at));items.addAll(expected.get(at+1));roots.set(at,root);roots.remove(at+1);expected.set(at,items);expected.remove(at+1);
                }
                assertSequence(tape,roots.get(0),expected.get(0));
            }
        }
        eq(0L,store.claimed);
    }
    static void geometricChainsAndGiantRepeatedSubtreesHaveBoundedStructuralWork() {
        for(int size:new int[]{16,64,256,1024,4096}) {
            var store=new Store();
            try(var tape=new SnapshotDiagnosticTemplates(store)) {
                long root=0;var expected=new ArrayList<Issue>();
                for(int n=0;n<size;n++){var item=issue(n);root=tape.concat(root,leaf(tape,item));expected.add(item);}
                int logarithm=32-Integer.numberOfLeadingZeros(size);eq(true,tape.height(root)<=2*logarithm+1);eq(true,store.requests<=256L*size*logarithm);
                assertSequence(tape,root,expected);long rows=store.issued,requests=store.requests;
                for(int n=0;n<size;n++){var r=new Report(0);tape.emit(root,n+1L,r);eq((long)size,r.total());eq(List.of(),r.items);}
                eq(rows,store.issued);eq(true,store.requests-requests<=8L*size);
                long queryReads=store.requests-requests;System.out.println("SNAPSHOT_DIAGNOSTIC_COST leaves="+size+" tuples="+rows+" height="+tape.height(root)+" reads="+requests+" zeroRetentionReads="+queryReads);
            }
            eq(0L,store.claimed);
        }
        var store=new Store();
        try(var tape=new SnapshotDiagnosticTemplates(store)) {
            var item=issue(0);long root=leaf(tape,item);
            for(int n=0;n<62;n++)root=tape.concat(root,root);
            eq(1L<<62,tape.size(root));eq(63,tape.height(root));eq(63L,store.issued);
            var r=new Report(3);tape.emit(root,73,r);eq(1L<<62,r.total());eq(List.of(item,item,item),r.items);eq(73L,r.owner);
            long count=store.issued;for(int n=0;n<100000;n++)root=tape.concat(0,root);eq(count,store.issued);
            long frozen=root;fails(ArithmeticException.class,()->tape.concat(frozen,frozen));fails(IllegalStateException.class,()->tape.size(frozen));
        }
        eq(0L,store.claimed);
    }
    static void everyStorageReportAndCleanupFailureAbortsOwnedTemplates() {
        var measured=new Store();try(var tape=new SnapshotDiagnosticTemplates(measured)){sequence(tape);}
        for(long at=0;at<measured.requests;at++) {
            var store=new Store();store.remaining=at;
            try(var tape=new SnapshotDiagnosticTemplates(store)){eq(store.failure,fails(IllegalStateException.class,()->sequence(tape)));fails(IllegalStateException.class,()->tape.size(0));}
            eq(0L,store.claimed);eq(true,store.closed);
        }
        var denied=new Store();denied.denyClaim=true;eq(denied.failure,fails(IllegalStateException.class,()->new SnapshotDiagnosticTemplates(denied)));eq(true,denied.closed);eq(0L,denied.claimed);
        for(int step=0;step<3;step++) {
            var store=new Store();final int at=step;var failure=new IllegalStateException("report failure");
            try(var tape=new SnapshotDiagnosticTemplates(store)) {
                long root=leaf(tape,issue(1));var report=new SnapshotDiagnosticTemplates.Reports(){public long remaining(){if(at==0)throw failure;return 1;}public void occurrences(ValidationIssue.Kind kind,long owner,long count){if(at==1)throw failure;}public void retain(ValidationIssue.Kind kind,int rule,long owner,long anchor,int field,long detail){if(at==2)throw failure;}};
                eq(failure,fails(IllegalStateException.class,()->tape.emit(root,81,report)));fails(IllegalStateException.class,()->tape.size(root));
            }
            eq(0L,store.claimed);
        }
        var store=new Store();var tape=new SnapshotDiagnosticTemplates(store);long root=leaf(tape,issue(0));
        var report=new SnapshotDiagnosticTemplates.Reports(){public long remaining(){return 1;}public void occurrences(ValidationIssue.Kind kind,long owner,long count){tape.close();}public void retain(ValidationIssue.Kind kind,int rule,long owner,long anchor,int field,long detail){throw new AssertionError("unreachable");}};
        fails(IllegalStateException.class,()->tape.emit(root,81,report));fails(IllegalStateException.class,()->tape.size(root));store.closeFailure=store.leaseFailure=true;
        eq(store.cleanup,fails(IllegalStateException.class,tape::close));eq(List.of(store.leaseCleanup),List.of(store.cleanup.getSuppressed()));tape.close();eq(0L,store.claimed);
    }
    private static void sequence(SnapshotDiagnosticTemplates tape) {
        long a=leaf(tape,issue(0)),b=leaf(tape,issue(1)),c=leaf(tape,issue(2));long root=tape.concat(tape.concat(a,b),tape.concat(c,a));
        eq(4L,tape.size(root));eq(2L,tape.count(root,ValidationIssue.Kind.INVALID_IR));var r=new Report(3);tape.emit(root,91,r);eq(4L,r.total());eq(List.of(issue(0),issue(1),issue(2)),r.items);
    }
    private record Tuple(long[] words,boolean collisions) {
        @Override public boolean equals(Object other){return other instanceof Tuple t&&Arrays.equals(words,t.words);}
        @Override public int hashCode(){return collisions?1:Arrays.hashCode(words);}
    }
    static final class Store implements SnapshotDiagnosticTemplates.Storage {
        final Map<Tuple,Long> lookup=new HashMap<>();final Map<Long,long[]> rows=new HashMap<>();long claimed,issued,requests,remaining=Long.MAX_VALUE;boolean closed,denyClaim,closeFailure,leaseFailure,collisions;
        final IllegalStateException failure=new IllegalStateException("diagnostic storage failure"),cleanup=new IllegalStateException("diagnostic owner cleanup"),leaseCleanup=new IllegalStateException("diagnostic lease cleanup");
        private void work(){requests++;if(remaining--==0)throw failure;}
        public long intern(long[] words){work();var key=new Tuple(words.clone(),collisions);Long known=lookup.get(key);if(known!=null)return known;long handle=++issued;lookup.put(key,handle);rows.put(handle,key.words);return handle;}
        public long word(long handle,SnapshotDiagnosticTemplates.Word field){work();return rows.get(handle)[field.ordinal()];}
        public AirSnapshotBuilder.Lease claim(long bytes){if(denyClaim)throw failure;claimed+=bytes;return ()->{claimed-=bytes;if(leaseFailure)throw leaseCleanup;};}
        public void close(){if(closed)return;closed=true;lookup.clear();rows.clear();if(closeFailure)throw cleanup;}
    }
    static final class Report implements SnapshotDiagnosticTemplates.Reports {
        long remaining,owner;final long[] counts=new long[KINDS.length];final ArrayList<Issue> items=new ArrayList<>();
        Report(long remaining){this.remaining=remaining;}
        public long remaining(){return remaining;}
        public void occurrences(ValidationIssue.Kind kind,long owner,long count){counts[kind.ordinal()]=Math.addExact(counts[kind.ordinal()],count);this.owner=owner;}
        public void retain(ValidationIssue.Kind kind,int rule,long owner,long anchor,int field,long detail){if(remaining--<=0)throw new AssertionError("retention quota");this.owner=owner;items.add(new Issue(kind,rule,anchor,field,detail));}
        long total(){long total=0;for(long value:counts)total=Math.addExact(total,value);return total;}
    }
    private static <T extends Throwable>T fails(Class<T> type,Runnable action){try{action.run();}catch(Throwable error){if(type.isInstance(error))return type.cast(error);throw new AssertionError(error);}throw new AssertionError("missing "+type);}
    private static void eq(Object expected,Object actual){if(!Objects.equals(expected,actual))throw new AssertionError(expected instanceof List<?> a&&actual instanceof List<?> b?"sequence mismatch: expected size="+a.size()+" actual size="+b.size():"expected="+expected+" actual="+actual);}
}
