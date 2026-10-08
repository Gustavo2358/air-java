package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import java.util.*;

/** Manual recipe oracle; these reachable fragments do not claim full AIR admission. */
final class SnapshotAnnotationChecks {
    private SnapshotAnnotationChecks() { }
    static void precisionDimensionsAndSharedEmptyClaimsPreserveOwner() {
        var f=new Fixture();long open=f.claim(2,f.empty),exact=f.claim(0,f.empty);
        long precision=f.s.record(AirShape.EVIDENCE_PRECISION,open,exact,open,open,exact);
        try(var e=new Env(f,new Store())) {
            long root=e.annotations.precision(precision);assertRules(e.tape,root,71,List.of(100,102,103));
            int folds=f.s.cursors;long tables=e.port.memo.issued,tuples=e.tuples.issued;
            for(int q=0;q<1024;q++){eq(root,e.annotations.precision(precision));eq(0L,e.annotations.claim(exact));}
            eq(tables,e.port.memo.issued);eq(tuples,e.tuples.issued);eq(folds,f.s.cursors);assertRules(e.tape,root,72,List.of(100,102,103));
        }
    }
    static void coverageDuplicateKeysAndBothItemErrorsKeepExactOrder() {
        var f=new Fixture();long empty=f.item("é",0,f.empty,f.empty),bad=f.item("é",1,f.empty,f.empty),other=f.item("é",0,f.empty,f.empty);
        long rows=f.s.list(empty,bad,bad,other),coverage=f.coverage(1,rows,f.empty);
        try(var e=new Env(f,new Store())) {
            long root=e.annotations.coverage(coverage);
            assertRules(e.tape,root,81,List.of(110,112,111,112,113,111,112,113,112));
            var r=new Report(100);e.tape.emit(root,82,r);eq(List.of(coverage,empty,bad,bad,bad,bad,bad,bad,other),r.anchors);
            long same=e.annotations.coverage(f.coverage(0,rows,f.empty));assertRules(e.tape,same,83,List.of(112,111,112,113,111,112,113,112));
            // List duplicate scope is independent of previous lists; exact Unicode text, no normalization.
            assertRules(e.tape,e.annotations.coverage(f.coverage(0,f.s.list(bad),f.empty)),84,List.of(112,113));
        }
    }
    static void actualScopeReasonOutputAndEliminationReferencesStayOrdered() {
        var f=new Fixture();long missing=f.s.record(AirShape.IDS_UNIT_ID,f.pub,f.s.text("absent"));
        long scope=f.s.record(AirShape.SCOPES_ENTITY_SCOPE,f.s.list(missing,f.pub,missing));
        long reason=f.s.record(AirShape.IDS_UNCERTAINTY_ID,f.pub,f.s.text("absent"));
        long origin=f.s.record(AirShape.IDS_ORIGIN_ID,f.pub,f.s.text("absent"));
        long claim=f.s.record(AirShape.EVIDENCE_CLAIM,scope,f.s.scalar(AirShape.EVIDENCE_PRECISION_STATUS,2),f.s.list(reason));
        long precision=f.s.record(AirShape.EVIDENCE_PRECISION,claim,claim,claim,claim,claim);
        long eliminated=f.s.record(AirShape.EVIDENCE_COVERAGE_ITEM,f.s.text("x"),origin,f.s.scalar(AirShape.EVIDENCE_COVERAGE_STATUS,1),f.s.list(missing),f.s.list(reason),f.s.optional(f.s.record(AirShape.EVIDENCE_ELIMINATION,f.s.text("law"),origin)));
        try(var e=new Env(f,new Store())) {
            assertRules(e.tape,e.annotations.scope(scope),75,List.of(1,1));
            assertRules(e.tape,e.annotations.precision(precision),76,Collections.nCopies(15,1));
            var report=new Report(100);e.tape.emit(e.annotations.coverage(f.coverage(1,f.s.list(eliminated),f.s.list(reason))),77,report);
            eq(List.of(1,1,1,1,1),report.rules);eq(List.of(reason,origin,missing,reason,origin),report.anchors);
        }
    }
    static void independentRandomCoverageListsPreserveEveryOccurrence() {
        var random=new Random(0xa660L);
        for(int run=0;run<64;run++) {
            var f=new Fixture();int n=random.nextInt(36);long[] rows=new long[n];var expected=new ArrayList<Integer>();var seen=new HashSet<String>();
            for(int at=0;at<n;at++){String key="key"+random.nextInt(8);int status=random.nextInt(4);rows[at]=f.item(key,status,f.empty,f.empty);if(!seen.add(key))expected.add(111);expected.add(112);if(status!=0)expected.add(113);}
            try(var e=new Env(f,new Store())){assertRules(e.tape,e.annotations.coverage(f.coverage(0,f.s.list(rows),f.empty)),91,expected);}
        }
    }
    static void sharedCoverageHasOneFoldAndNoOwnerQueryHistory() {
        for(int n:new int[]{16,64,256,1024,4096}) {
            var f=new Fixture();long[] rows=new long[n];for(int i=0;i<n;i++)rows[i]=f.item("k"+i,0,f.empty,f.empty);long coverage=f.coverage(0,f.s.list(rows),f.empty),valid=f.coverage(0,f.empty,f.empty);var port=new Store();
            try(var e=new Env(f,port)) {
                long root=e.annotations.coverage(coverage);eq((long)n,e.tape.size(root));eq(0L,e.annotations.coverage(valid));
                int folds=f.s.cursors;long tuples=e.tuples.issued,tables=port.memo.issued,members=port.members.size();var report=new Report(2);
                for(int q=0;q<n;q++){e.tape.emit(e.annotations.coverage(coverage),q+1L,report);eq(0L,e.annotations.coverage(valid));}
                eq((long)n*n,report.count);eq(2,report.rules.size());eq(tuples,e.tuples.issued);eq(tables,port.memo.issued);eq(members,(long)port.members.size());eq(folds,f.s.cursors);eq((long)n,e.annotations.counts().items());
                eq(true,tuples<=4L*n+16);System.out.println("SNAPSHOT_ANNOTATION_COST items="+n+" uses="+(2L*n)+" tuples="+tuples+" members="+members+" retained=2");
            }
            eq(0L,port.memo.claimed);
        }
    }
    static void everyMemoMemberAndCursorFailureAbortsOnlyOwnedState() {
        var f=new Fixture();long c=f.coverage(0,f.s.list(f.item("x",1,f.empty,f.empty)),f.empty);var measured=new Store();long calls;
        try(var e=new Env(f,measured)){long before=measured.memo.requests;e.annotations.coverage(c);calls=measured.memo.requests-before;}
        for(long i=0;i<calls;i++) {
            var g=new Fixture();long coverage=g.coverage(0,g.s.list(g.item("x",1,g.empty,g.empty)),g.empty);var port=new Store();
            try(var e=new Env(g,port)) {
                port.memo.remaining=i;fails(IllegalStateException.class,()->e.annotations.coverage(coverage));fails(IllegalStateException.class,e.annotations::counts);
                eq(AirShape.PUBLICATION,e.input.shape(g.root));eq(true,e.declarations.entities()>0);eq(0L,e.tape.size(0));
            }
            eq(0L,port.memo.claimed);eq(0,g.s.activeCursors);
        }
        for(int fault=0;fault<3;fault++) {
            var g=new Fixture();long rows=g.s.list(g.item("x",0,g.empty,g.empty)),coverage=g.coverage(0,rows,g.empty);
            try(var e=new Env(g,new Store())) {
                if(fault==0){g.s.badLength=rows;g.s.lengthDelta=1;}if(fault==1){g.s.closeCursorFailure=true;g.s.failingCursor=rows;}if(fault==2)g.s.replaceField(coverage,0,g.s.scalar(AirShape.EVIDENCE_INVENTORY_STATUS,99));
                fails(IllegalStateException.class,()->e.annotations.coverage(coverage));fails(IllegalStateException.class,e.annotations::counts);
            }
            eq(0,g.s.activeCursors);
        }
    }
    private static void assertRules(SnapshotDiagnosticTemplates tape,long root,long owner,List<Integer> rules){var r=new Report(Long.MAX_VALUE);tape.emit(root,owner,r);eq(rules,r.rules);eq((long)rules.size(),r.count);if(!rules.isEmpty())eq(owner,r.owner);}
    private static final class Report implements SnapshotDiagnosticTemplates.Reports {
        long remaining,count,owner;final List<Integer> rules=new ArrayList<>();final List<Long> anchors=new ArrayList<>();Report(long n){remaining=n;}
        public long remaining(){return remaining;}public void occurrences(ValidationIssue.Kind kind,long owner,long n){eq(ValidationIssue.Kind.INVALID_IR,kind);this.owner=owner;count=Math.addExact(count,n);}
        public void retain(ValidationIssue.Kind kind,int rule,long owner,long anchor,int field,long detail){eq(this.owner,owner);eq(true,remaining-->0);rules.add(rule);anchors.add(anchor);}
    }
    static final class Fixture {
        final SnapshotGraphChecks.Source s=new SnapshotGraphChecks.Source();final long root,pub,scope,empty;
        Fixture(){var f=new SnapshotGraphChecks.Fixture(s);root=f.root;pub=f.id;empty=f.empty;scope=s.record(AirShape.SCOPES_PUBLICATION_SCOPE,pub);}
        long claim(int status,long reasons){return s.record(AirShape.EVIDENCE_CLAIM,scope,s.scalar(AirShape.EVIDENCE_PRECISION_STATUS,status),reasons);}
        long item(String key,int status,long outputs,long reasons){return s.record(AirShape.EVIDENCE_COVERAGE_ITEM,s.text(key),pubOrigin(),s.scalar(AirShape.EVIDENCE_COVERAGE_STATUS,status),outputs,reasons,s.optional());}
        long pubOrigin(){long id=s.record(AirShape.IDS_ORIGIN_ID,pub,s.text("o"));long origin=s.record(AirShape.ORIGINS_CONTRACTUAL,id,s.text("contract"),s.text("1"));s.replaceField(root,8,s.list(origin));return id;}
        long coverage(int status,long items,long reasons){return s.record(AirShape.EVIDENCE_COVERAGE,s.scalar(AirShape.EVIDENCE_INVENTORY_STATUS,status),scope,items,reasons);}
    }
    static final class Store implements SnapshotAnnotationTemplates.Storage {
        private record Member(long table,long key) { }
        final SnapshotReferenceChecks.Store memo=new SnapshotReferenceChecks.Store();final Set<Member> members=new HashSet<>();
        public long find(long source,int recipe){return memo.find(source,recipe);}public long begin(long source,int recipe){return memo.begin(source,recipe);}public void finish(long table,long root){memo.finish(table,root);}public long root(long table){return memo.root(table);}
        public boolean first(long table,long key){memo.work();return members.add(new Member(table,key));}
        public AirSnapshotBuilder.Lease claim(long bytes){return memo.claim(bytes);}public void close(){memo.close();members.clear();}
    }
    private static final class Env implements AutoCloseable {
        final AirSnapshot input;final SnapshotIdentityKeys keys;final SnapshotDeclarations declarations;final SnapshotDiagnosticChecks.Store tuples=new SnapshotDiagnosticChecks.Store();final SnapshotDiagnosticTemplates tape;final SnapshotReferenceLists refs;final SnapshotAnnotationTemplates annotations;final Store port;
        Env(Fixture f,Store port){this.port=port;input=AirSnapshot.attach(f.s,f.root);keys=new SnapshotIdentityKeys(input,new SnapshotAtomChecks.Store());declarations=SnapshotDeclarations.build(input,keys,new SnapshotDeclarationChecks.Store(),1000,1000,(r,i,n)->{throw new AssertionError(r);});tape=new SnapshotDiagnosticTemplates(tuples);refs=new SnapshotReferenceLists(input,declarations,tape,new SnapshotReferenceChecks.Store());annotations=new SnapshotAnnotationTemplates(input,keys,refs,tape,port);}
        public void close(){annotations.close();refs.close();tape.close();declarations.close();keys.close();input.close();}
    }
    private static <T extends Throwable>T fails(Class<T> type,Runnable action){try{action.run();}catch(Throwable error){if(type.isInstance(error))return type.cast(error);throw new AssertionError(error);}throw new AssertionError("missing "+type);}
    private static void eq(Object expected,Object actual){if(!Objects.equals(expected,actual))throw new AssertionError("expected="+expected+" actual="+actual);}
}
