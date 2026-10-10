package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import java.util.*;

/** Actual context-free reference occurrences; declaration/namespace slots are not invented references. */
final class SnapshotReferenceChecks {
    private SnapshotReferenceChecks() { }
    static void exactMissingOccurrencesAndSuccessfulEmptyRootsPreserveOwners() {
        var f=new SnapshotLabelChecks.Fixture();long known=f.label(f.a,"defined"),a=f.label(f.a,"missing"),b=f.label(f.b,"missing"),publication=f.s.record(AirShape.IDS_PUBLICATION_ID,f.s.text("P"));
        long list=f.s.list(known,a,b,publication,a),valid=f.s.list(known,publication),empty=f.s.list();var port=new Store();var tuples=new SnapshotDiagnosticChecks.Store();
        try(var input=AirSnapshot.attach(f.s,f.root);var keys=new SnapshotIdentityKeys(input,new SnapshotAtomChecks.Store());
            var declarations=SnapshotDeclarations.build(input,keys,new SnapshotDeclarationChecks.Store(),1000,1000,(r,i,n)->{throw new AssertionError(r);});
            var tape=new SnapshotDiagnosticTemplates(tuples);var refs=new SnapshotReferenceLists(input,declarations,tape,port)) {
            long root=refs.references(list,AirShape.IDS_ID);assertReferences(tape,root,71,List.of(a,b,a));
            eq(0L,refs.references(valid,AirShape.IDS_ID));eq(0L,refs.references(empty,AirShape.IDS_ORIGIN_ID));eq(0L,refs.references(empty,AirShape.IDS_ID));
            int folds=f.s.cursors;long tables=port.issued,created=tuples.issued;
            for(int q=0;q<1024;q++){eq(root,refs.references(list,AirShape.IDS_ID));eq(0L,refs.references(valid,AirShape.IDS_ID));eq(0L,refs.references(empty,AirShape.IDS_ORIGIN_ID));eq(0L,refs.reference(known));eq(0L,refs.reference(publication));}
            eq(folds,f.s.cursors);eq(tables,port.issued);eq(created,tuples.issued);eq(4L,refs.counts().lists());eq(7L,refs.counts().rows());eq(4L,refs.counts().identities());
            assertReferences(tape,root,72,List.of(a,b,a));long scalar=refs.reference(a);assertReferences(tape,scalar,73,List.of(a));
            closeMemo(refs);assertReferences(tape,root,74,List.of(a,b,a));eq(true,declarations.entities()>0);eq(AirShape.PUBLICATION,input.shape(f.root));
        }
        eq(0L,port.claimed);eq(0L,tuples.claimed);eq(0,f.s.activeCursors);
    }
    static void independentRandomReferenceListsKeepAllOccurrencesAcrossFamilies() {
        var random=new Random(0x1d5L);
        for(int run=0;run<96;run++) {
            var f=new SnapshotLabelChecks.Fixture();long pub=f.s.record(AirShape.IDS_PUBLICATION_ID,f.s.text("P"));
            long[] ids={f.a,f.label(f.a,"defined"),pub,f.label(f.a,"missing"),f.label(f.b,"missing"),f.unit("Q","a"),f.s.record(AirShape.IDS_ORIGIN_ID,pub,f.s.text("missing"))};
            int size=random.nextInt(48);long[] rows=new long[size];var expected=new ArrayList<Long>();
            for(int at=0;at<size;at++){int index=random.nextInt(ids.length);rows[at]=ids[index];if(index>=3)expected.add(rows[at]);}
            long list=f.s.list(rows);var port=new Store();
            try(var input=AirSnapshot.attach(f.s,f.root);var keys=new SnapshotIdentityKeys(input,new SnapshotAtomChecks.Store());
                var declarations=SnapshotDeclarations.build(input,keys,new SnapshotDeclarationChecks.Store(),1000,1000,(r,i,n)->{throw new AssertionError(r);});
                var tape=new SnapshotDiagnosticTemplates(new SnapshotDiagnosticChecks.Store());var refs=new SnapshotReferenceLists(input,declarations,tape,port)) {
                long root=refs.references(list,AirShape.IDS_ID);assertReferences(tape,root,81,expected);assertReferences(tape,tape.concat(root,root),82,duplicated(expected));
            }
            eq(0L,port.claimed);
        }
    }
    private static List<Long> duplicated(List<Long> values){var result=new ArrayList<>(values);result.addAll(values);return result;}
    static void carryForestUsesLinearTuplesAndCachesInvalidAndValidLists() {
        for(int size:new int[]{16,64,256,1024,4096}) {
            var f=new SnapshotLabelChecks.Fixture();long[] ids=new long[size];for(int at=0;at<size;at++)ids[at]=f.label(f.a,"absent"+at);
            long list=f.s.list(ids),known=f.label(f.a,"defined"),valid=f.s.list(known,known,known);var port=new Store();var tuples=new SnapshotDiagnosticChecks.Store();
            try(var input=AirSnapshot.attach(f.s,f.root);var keys=new SnapshotIdentityKeys(input,new SnapshotAtomChecks.Store());
                var declarations=SnapshotDeclarations.build(input,keys,new SnapshotDeclarationChecks.Store(),1000,1000,(r,i,n)->{throw new AssertionError(r);});
                var tape=new SnapshotDiagnosticTemplates(tuples);var refs=new SnapshotReferenceLists(input,declarations,tape,port)) {
                int before=f.s.cursors;long root=refs.references(list,AirShape.IDS_LABEL_ID);eq((long)size,tape.size(root));eq(0L,refs.references(valid,AirShape.IDS_LABEL_ID));eq(before+2,f.s.cursors);
                eq(true,tuples.issued<=3L*size);long issued=tuples.issued,tables=port.issued;var report=new Report(2);
                for(int q=0;q<size;q++){tape.emit(refs.references(list,AirShape.IDS_LABEL_ID),91,report);eq(0L,refs.references(valid,AirShape.IDS_LABEL_ID));}
                eq((long)size*size,report.count);eq(List.of(ids[0],ids[1]),report.ids);eq(issued,tuples.issued);eq(tables,port.issued);eq(before+2,f.s.cursors);
                System.out.println("SNAPSHOT_REFERENCE_COST rows="+size+" uses="+(2L*size)+" tuples="+issued+" sourceFolds=2 retained=2");
            }
            eq(0L,port.claimed);eq(0L,tuples.claimed);
        }
    }
    static void everyMemoCursorAndBorrowedTemplateFaultFailsClosed() {
        var f=new SnapshotLabelChecks.Fixture();long list=f.s.list(f.label(f.a,"missing"));var measured=new Store();long requests;
        try(var input=AirSnapshot.attach(f.s,f.root);var keys=new SnapshotIdentityKeys(input,new SnapshotAtomChecks.Store());
            var declarations=SnapshotDeclarations.build(input,keys,new SnapshotDeclarationChecks.Store(),1000,1000,(r,i,n)->{throw new AssertionError(r);});
            var tape=new SnapshotDiagnosticTemplates(new SnapshotDiagnosticChecks.Store());var refs=new SnapshotReferenceLists(input,declarations,tape,measured)) {refs.references(list,AirShape.IDS_LABEL_ID);requests=measured.requests;}
        for(long at=0;at<requests;at++) {
            var g=new SnapshotLabelChecks.Fixture();long labels=g.s.list(g.label(g.a,"missing"));var port=new Store();port.remaining=at;
            try(var input=AirSnapshot.attach(g.s,g.root);var keys=new SnapshotIdentityKeys(input,new SnapshotAtomChecks.Store());
                var declarations=SnapshotDeclarations.build(input,keys,new SnapshotDeclarationChecks.Store(),1000,1000,(r,i,n)->{throw new AssertionError(r);});
                var tape=new SnapshotDiagnosticTemplates(new SnapshotDiagnosticChecks.Store())) {
                SnapshotReferenceLists refs=null;
                try{refs=new SnapshotReferenceLists(input,declarations,tape,port);var active=refs;eq(port.failure,fails(IllegalStateException.class,()->active.references(labels,AirShape.IDS_LABEL_ID)));fails(IllegalStateException.class,refs::counts);}
                catch(IllegalStateException failure){eq(port.failure,failure);}finally{if(refs!=null)refs.close();}
                eq(0L,tape.size(0));eq(true,declarations.entities()>0);eq(AirShape.PUBLICATION,input.shape(g.root));
            }
            eq(0L,port.claimed);eq(true,port.closed);eq(0,g.s.activeCursors);
        }
        for(int fault=0;fault<5;fault++) {
            var g=new SnapshotLabelChecks.Fixture();long label=g.label(g.a,"missing"),labels=g.s.list(label);var port=new Store();var tuples=new SnapshotDiagnosticChecks.Store();
            try(var input=AirSnapshot.attach(g.s,g.root);var keys=new SnapshotIdentityKeys(input,new SnapshotAtomChecks.Store());
                var declarations=SnapshotDeclarations.build(input,keys,new SnapshotDeclarationChecks.Store(),1000,1000,(r,i,n)->{throw new AssertionError(r);});
                var tape=new SnapshotDiagnosticTemplates(tuples);var refs=new SnapshotReferenceLists(input,declarations,tape,port)) {
                if(fault==0){g.s.badLength=labels;g.s.lengthDelta=1;}if(fault==1){g.s.closeCursorFailure=true;g.s.failingCursor=labels;}if(fault==2)tuples.remaining=0;if(fault==3)port.callback=()->refs.reference(label);
                final int at=fault;
                if(at==4)fails(IllegalArgumentException.class,()->refs.references(labels,AirShape.IDS_ORIGIN_ID));
                else fails(IllegalStateException.class,()->refs.references(labels,AirShape.IDS_LABEL_ID));fails(IllegalStateException.class,refs::counts);eq(AirShape.PUBLICATION,input.shape(g.root));
            }
            eq(0L,port.claimed);eq(0L,tuples.claimed);eq(0,g.s.activeCursors);
        }
    }
    static void constructorAndCombinedCleanupFailuresPreserveAllBorrowedOwners() {
        var f=new SnapshotLabelChecks.Fixture();
        try(var input=AirSnapshot.attach(f.s,f.root);var keys=new SnapshotIdentityKeys(input,new SnapshotAtomChecks.Store());
            var declarations=SnapshotDeclarations.build(input,keys,new SnapshotDeclarationChecks.Store(),1000,1000,(r,i,n)->{throw new AssertionError(r);});
            var tape=new SnapshotDiagnosticTemplates(new SnapshotDiagnosticChecks.Store())) {
            for(int at=0;at<3;at++) {
                var port=new Store();final int missing=at;
                fails(NullPointerException.class,()->new SnapshotReferenceLists(missing==0?null:input,missing==1?null:declarations,missing==2?null:tape,port));
                eq(true,port.closed);eq(0L,port.claimed);
            }
            var denied=new Store();denied.deny=true;denied.closeFailure=true;
            eq(denied.failure,fails(IllegalStateException.class,()->new SnapshotReferenceLists(input,declarations,tape,denied)));eq(List.of(denied.cleanup),List.of(denied.failure.getSuppressed()));eq(0L,denied.claimed);
            var combined=new Store();combined.closeFailure=true;combined.leaseFailure=true;var refs=new SnapshotReferenceLists(input,declarations,tape,combined);
            var error=fails(IllegalStateException.class,refs::close);eq(combined.cleanup,error);eq(List.of(combined.leaseCleanup),List.of(error.getSuppressed()));eq(0L,combined.claimed);refs.close();
            eq(AirShape.PUBLICATION,input.shape(input.root()));eq(true,declarations.entities()>0);eq(0L,tape.size(0));
        }
    }
    private static void closeMemo(SnapshotReferenceLists refs){refs.close();}
    private static void assertReferences(SnapshotDiagnosticTemplates tape,long root,long owner,List<Long> expected){var r=new Report(Long.MAX_VALUE);tape.emit(root,owner,r);eq(expected,r.ids);eq((long)expected.size(),r.count);if(!expected.isEmpty())eq(owner,r.owner);}
    private static final class Report implements SnapshotDiagnosticTemplates.Reports {
        long capacity,count,owner;final List<Long> ids=new ArrayList<>();Report(long capacity){this.capacity=capacity;}
        public long remaining(){return capacity;}public void occurrences(ValidationIssue.Kind kind,long owner,long count){eq(ValidationIssue.Kind.INVALID_IR,kind);this.count=Math.addExact(this.count,count);this.owner=owner;}
        public void retain(ValidationIssue.Kind kind,int rule,long owner,long anchor,int field,long detail){eq(ValidationIssue.Kind.INVALID_IR,kind);eq(1,rule);eq(-1,field);eq(anchor,detail);eq(this.owner,owner);eq(true,capacity-->0);ids.add(anchor);}
    }
    static final class Store implements SnapshotReferenceLists.Storage {
        private record Key(long source,int kind) { }
        final Map<Key,Long> memo=new HashMap<>();final Map<Long,Long> roots=new HashMap<>();long issued,claimed,requests,remaining=Long.MAX_VALUE;boolean closed,deny,closeFailure,leaseFailure;Runnable callback;
        final IllegalStateException failure=new IllegalStateException("reference memo failure"),cleanup=new IllegalStateException("memo cleanup failure"),leaseCleanup=new IllegalStateException("memo lease cleanup failure");
        void work(){requests++;if(remaining--==0)throw failure;if(callback!=null)callback.run();}
        public long find(long source,int kind){work();long table=memo.getOrDefault(new Key(source,kind),0L);if(table!=0&&!roots.containsKey(table))throw failure;return table;}
        public long begin(long source,int kind){work();if(memo.containsKey(new Key(source,kind)))throw failure;long table=++issued;memo.put(new Key(source,kind),table);return table;}
        public void finish(long table,long root){work();roots.put(table,root);}public long root(long table){work();return roots.get(table);}
        public AirSnapshotBuilder.Lease claim(long bytes){work();if(deny)throw failure;claimed+=bytes;return ()->{claimed-=bytes;if(leaseFailure)throw leaseCleanup;};}
        public void close(){if(closed)return;closed=true;memo.clear();roots.clear();callback=null;if(closeFailure)throw cleanup;}
    }
    private static <T extends Throwable>T fails(Class<T> type,Runnable action){try{action.run();}catch(Throwable error){if(type.isInstance(error))return type.cast(error);throw new AssertionError(error);}throw new AssertionError("missing "+type);}
    private static void eq(Object expected,Object actual){if(!Objects.equals(expected,actual))throw new AssertionError("expected="+expected+" actual="+actual);}
}
