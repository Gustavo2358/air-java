package io.github.gustavo2358.air.validation;

import java.util.*;

/** Literal projected occurrence oracle; relation/context handles are deliberately independent. */
final class SnapshotProjectionChecks {
    private SnapshotProjectionChecks() { }
    private static final ValidationIssue.Kind[] KINDS=ValidationIssue.Kind.values();
    private record Item(ValidationIssue.Kind kind,int rule,long anchor,int field,long detail) { }
    private static final List<Item> A=List.of(new Item(KINDS[0],201,101,-1,501),new Item(KINDS[1],202,102,0,502),new Item(KINDS[2],203,103,2,503));
    private static final List<Item> B=List.of(new Item(KINDS[2],204,104,1,504),new Item(KINDS[0],205,105,3,505));
    private static final Item LITERAL=new Item(KINDS[0],101,100,0,500);
    static void mixedProjectedChunksPreserveExactContextOrderAndBulkKinds() {
        var port=new SnapshotDiagnosticChecks.Store();var projector=new Projector();
        try(var tape=new SnapshotDiagnosticTemplates(port,projector)) {
            long[] counts={1,1,1,0,0};long a=tape.projected(41,27,11,counts);Arrays.fill(counts,0);
            long b=tape.projected(41,27,12,new long[]{1,0,1,0,0});long literal=tape.leaf(LITERAL.kind(),LITERAL.rule(),LITERAL.anchor(),LITERAL.field(),LITERAL.detail());
            long root=tape.concat(tape.concat(a,literal),b);var expected=new ArrayList<>(A);expected.add(LITERAL);expected.addAll(B);var report=new Report(100);tape.emit(root,91,report);
            eq(expected,report.items);eq(6L,tape.size(root));eq(3L,tape.count(root,KINDS[0]));eq(1L,tape.count(root,KINDS[1]));eq(2L,tape.count(root,KINDS[2]));eq(91L,report.owner);eq(5L,projector.calls);
            long calls=projector.calls,tuples=port.issued;for(int n=0;n<4096;n++){var r=new Report(0);tape.emit(root,n+1L,r);eq(6L,r.total());eq(List.of(),r.items);}
            eq(calls,projector.calls);eq(tuples,port.issued);
            long repeated=tape.concat(a,a);var r=new Report(4);tape.emit(repeated,92,r);eq(List.of(A.get(0),A.get(1),A.get(2),A.get(0)),r.items);eq(6L,r.total());
            eq(0L,tape.projected(41,27,11,new long[5]));
        }
        eq(0L,port.claimed);eq(false,projector.closed);
    }
    static void trillionOccurrencesReadOnlyRequestedPrefixAndRejectEveryProjectionFault() {
        var port=new SnapshotDiagnosticChecks.Store();var projector=new Projector();
        try(var tape=new SnapshotDiagnosticTemplates(port,projector)) {
            long root=tape.projected(42,28,13,new long[]{1_000_000_000_000L,0,0,0,0});var r=new Report(3);tape.emit(root,93,r);
            eq(1_000_000_000_000L,r.total());eq(3L,projector.calls);eq(List.of(new Item(KINDS[0],301,1L<<44,-1,0),new Item(KINDS[0],301,(1L<<44)+1,-1,1),new Item(KINDS[0],301,(1L<<44)+2,-1,2)),r.items);eq(1L,port.issued);eq(1,tape.height(root));
        }
        eq(0L,port.claimed);
        for(int bad=0;bad<8;bad++) {
            var storage=new SnapshotDiagnosticChecks.Store();var projection=new Projector();projection.bad=bad;
            try(var tape=new SnapshotDiagnosticTemplates(storage,projection)) {
                long root=tape.projected(41,27,11,new long[]{1,1,1,0,0});
                var failure=fails(IllegalStateException.class,()->tape.emit(root,94,new Report(1)));if(bad==5)eq(projection.failure,failure);
                fails(IllegalStateException.class,()->tape.size(root));
            }
            eq(0L,storage.claimed);eq(false,projection.closed);
        }
        var missing=new SnapshotDiagnosticChecks.Store();try(var tape=new SnapshotDiagnosticTemplates(missing)){fails(IllegalStateException.class,()->tape.projected(41,27,11,new long[]{1,0,0,0,0}));fails(IllegalStateException.class,()->tape.size(0));}eq(0L,missing.claimed);
        for(long[] counts:new long[][]{{1,2},{-1,0,0,0,0},{Long.MAX_VALUE,1,0,0,0}}) {
            var storage=new SnapshotDiagnosticChecks.Store();try(var tape=new SnapshotDiagnosticTemplates(storage,new Projector())){fails(RuntimeException.class,()->tape.projected(41,27,11,counts));fails(IllegalStateException.class,()->tape.size(0));}eq(0L,storage.claimed);
        }
    }
    static void everyProjectedStorageReportAndReentrantFailurePreservesBorrowedRelation() {
        var measured=new SnapshotDiagnosticChecks.Store();try(var tape=new SnapshotDiagnosticTemplates(measured,new Projector())){long root=tape.projected(41,27,11,new long[]{1,1,1,0,0});var r=new Report(2);tape.emit(root,95,r);eq(List.of(A.get(0),A.get(1)),r.items);}
        for(long at=0;at<measured.requests;at++) {
            var storage=new SnapshotDiagnosticChecks.Store();storage.remaining=at;var projection=new Projector();
            try(var tape=new SnapshotDiagnosticTemplates(storage,projection)) {eq(storage.failure,fails(IllegalStateException.class,()->{long root=tape.projected(41,27,11,new long[]{1,1,1,0,0});tape.emit(root,95,new Report(2));}));fails(IllegalStateException.class,()->tape.size(0));}
            eq(0L,storage.claimed);eq(false,projection.closed);
        }
        var storage=new SnapshotDiagnosticChecks.Store();var projection=new Projector();var tape=new SnapshotDiagnosticTemplates(storage,projection);long root=tape.projected(41,27,11,new long[]{1,1,1,0,0});var failure=new IllegalStateException("projected retain failure");
        var r=new SnapshotDiagnosticTemplates.Reports(){public long remaining(){return 1;}public void occurrences(ValidationIssue.Kind kind,long owner,long count){}public void retain(ValidationIssue.Kind kind,int rule,long owner,long anchor,int field,long detail){throw failure;}};
        eq(failure,fails(IllegalStateException.class,()->tape.emit(root,96,r)));fails(IllegalStateException.class,()->tape.size(root));tape.close();eq(0L,storage.claimed);eq(false,projection.closed);
        var port=new SnapshotDiagnosticChecks.Store();SnapshotDiagnosticTemplates[] holder=new SnapshotDiagnosticTemplates[1];
        SnapshotDiagnosticTemplates.Projection callback=(recipe,relation,context,ordinal,output)->holder[0].size(0);
        try(var owner=new SnapshotDiagnosticTemplates(port,callback)){holder[0]=owner;long row=owner.projected(41,27,11,new long[]{1,0,0,0,0});fails(IllegalStateException.class,()->owner.emit(row,97,new Report(1)));fails(IllegalStateException.class,()->owner.size(row));}
        eq(0L,port.claimed);
    }
    private static final class Projector implements SnapshotDiagnosticTemplates.Projection,AutoCloseable {
        long calls;int bad=-1;boolean closed;final IllegalStateException failure=new IllegalStateException("projection failure");
        public void occurrence(int recipe,long relation,long context,long ordinal,long[] output) {
            calls++;if(bad==5)throw failure;
            Item item;
            if(recipe==41&&relation==27&&context==11)item=A.get(Math.toIntExact(ordinal));
            else if(recipe==41&&relation==27&&context==12)item=B.get(Math.toIntExact(ordinal));
            else if(recipe==42&&relation==28&&context==13)item=new Item(KINDS[0],301,(1L<<44)+ordinal,-1,ordinal);
            else throw new IllegalStateException("projection context/recipe changed");
            long[] expected={item.kind().ordinal(),item.rule(),item.anchor(),item.field(),item.detail()};
            for(int n=0;n<5;n++)if(bad!=n)output[n]=expected[n];
            if(bad==6)output[0]=5;if(bad==7)output[1]=0;
        }
        public void close(){closed=true;}
    }
    private static final class Report implements SnapshotDiagnosticTemplates.Reports {
        long capacity,owner;final long[] counts=new long[5];final ArrayList<Item> items=new ArrayList<>();
        Report(long capacity){this.capacity=capacity;}
        public long remaining(){return capacity;}
        public void occurrences(ValidationIssue.Kind kind,long owner,long count){this.owner=owner;counts[kind.ordinal()]=Math.addExact(counts[kind.ordinal()],count);}
        public void retain(ValidationIssue.Kind kind,int rule,long owner,long anchor,int field,long detail){if(capacity--<=0)throw new AssertionError("retention overrun");this.owner=owner;items.add(new Item(kind,rule,anchor,field,detail));}
        long total(){long total=0;for(long count:counts)total=Math.addExact(total,count);return total;}
    }
    private static <T extends Throwable>T fails(Class<T> type,Runnable action){try{action.run();}catch(Throwable error){if(type.isInstance(error))return type.cast(error);throw new AssertionError(error);}throw new AssertionError("missing "+type);}
    private static void eq(Object expected,Object actual){if(!Objects.equals(expected,actual))throw new AssertionError("expected="+expected+" actual="+actual);}
}
