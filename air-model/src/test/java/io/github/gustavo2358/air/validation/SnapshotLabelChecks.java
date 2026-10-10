package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import java.util.*;

/** Independent ordered local-label reference oracle. Other annotation/admission rules are separate. */
final class SnapshotLabelChecks {
    private SnapshotLabelChecks() { }
    static void danglingAndForeignReferencesPreserveNamespacesMultiplicityAndOwner() {
        var f=new Fixture();long equal=f.unit("P","a"),foreign=f.unit("P","b"),absent=f.unit("P","absent");
        long[] rows={f.label(equal,"defined"),f.label(foreign,"defined"),f.label(f.a,"missing"),f.label(f.b,"missing"),f.label(f.b,"missing")};
        long list=f.s.list(rows);var port=new Store();
        try(var snapshot=AirSnapshot.attach(f.s,f.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());
            var declarations=SnapshotDeclarations.build(snapshot,keys,new SnapshotDeclarationChecks.Store(),1000,1000,(r,i,n)->{throw new AssertionError(r);} );
            var index=new SnapshotLocalLabels(snapshot,keys,declarations,port);var tape=new SnapshotDiagnosticTemplates(new SnapshotDiagnosticChecks.Store(),index)) {
            long a=index.template(list,f.a,tape),b=index.template(list,f.b,tape),c=index.template(list,absent,tape);int folds=f.s.cursors;
            assertIssues(tape,a,71,List.of(new Item(2,rows[1]),new Item(1,rows[2]),new Item(1,rows[3]),new Item(2,rows[3]),new Item(1,rows[4]),new Item(2,rows[4])));
            assertIssues(tape,b,72,List.of(new Item(2,rows[0]),new Item(1,rows[2]),new Item(2,rows[2]),new Item(1,rows[3]),new Item(1,rows[4])));
            assertIssues(tape,c,73,List.of(new Item(2,rows[0]),new Item(2,rows[1]),new Item(1,rows[2]),new Item(2,rows[2]),new Item(1,rows[3]),new Item(2,rows[3]),new Item(1,rows[4]),new Item(2,rows[4])));
            eq(folds,f.s.cursors);eq(1L,index.counts().lists());eq(5L,index.counts().rows());
            long foreignNamespace=f.label(f.unit("Q","a"),"defined");
            assertIssues(tape,index.template(f.s.list(foreignNamespace),f.a,tape),74,List.of(new Item(1,foreignNamespace),new Item(2,foreignNamespace)));
            long empty=index.template(f.s.list(),f.a,tape);eq(0L,empty);
            // Nonsequential rank/select, ties and repeated contexts do not require prefix replay.
            var expected=List.of(new Item(2,rows[0]),new Item(2,rows[1]),new Item(1,rows[2]),new Item(2,rows[2]),new Item(1,rows[3]),new Item(2,rows[3]),new Item(1,rows[4]),new Item(2,rows[4]));
            long[] out=new long[5];long table=port.find(list),context=keys.key(absent);
            for(int at:new int[]{7,0,5,3,6,1,2,4,7}){index.occurrence(1,table,context,at,out);eq((long)expected.get(at).rule,out[1]);eq(expected.get(at).label,out[2]);}
        }
        eq(0L,port.claimed);eq(true,port.closed);eq(0,f.s.activeCursors);
    }
    static void randomTinyRelationsMatchIndependentTwoRuleOracle() {
        var random=new Random(0x1abe1L);
        for(int run=0;run<96;run++) {
            var f=new Fixture();int n=random.nextInt(40);long[] rows=new long[n],units=new long[n];boolean[] defined=new boolean[n];
            for(int at=0;at<n;at++){units[at]=random.nextBoolean()?f.a:f.b;defined[at]=random.nextBoolean();rows[at]=f.label(units[at],defined[at]?"defined":"missing");}
            long list=f.s.list(rows);var port=new Store();
            try(var snapshot=AirSnapshot.attach(f.s,f.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());
                var declarations=SnapshotDeclarations.build(snapshot,keys,new SnapshotDeclarationChecks.Store(),1000,1000,(r,i,x)->{throw new AssertionError(r);});
                var index=new SnapshotLocalLabels(snapshot,keys,declarations,port);var tape=new SnapshotDiagnosticTemplates(new SnapshotDiagnosticChecks.Store(),index)) {
                for(long unit:new long[]{f.a,f.b}) {
                    var expected=new ArrayList<Item>();for(int at=0;at<n;at++){if(!defined[at])expected.add(new Item(1,rows[at]));if(unit!=units[at])expected.add(new Item(2,rows[at]));}
                    long root=index.template(list,unit,tape);assertIssues(tape,root,81,expected);
                    var out=new long[5];long table=port.find(list),context=keys.key(unit);
                    for(int at=expected.size()-1;at>=0;at--){index.occurrence(1,table,context,at,out);eq((long)expected.get(at).rule,out[1]);eq(expected.get(at).label,out[2]);}
                }
                eq(1L,index.counts().lists());eq((long)n,index.counts().rows());
            }
            eq(0L,port.claimed);
        }
    }
    static void sharedRelationsFoldOnceAndZeroRetentionDoesNotSelectComplements() {
        for(int n:new int[]{16,64,256,1024,4096}) {
            var f=new Fixture();long[] rows=new long[n];Arrays.fill(rows,f.label(f.a,"defined"));rows[n-1]=f.label(f.b,"defined");long list=f.s.list(rows);var port=new Store();
            try(var snapshot=AirSnapshot.attach(f.s,f.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());
                var declarations=SnapshotDeclarations.build(snapshot,keys,new SnapshotDeclarationChecks.Store(),1000,1000,(r,i,x)->{throw new AssertionError(r);});
                var index=new SnapshotLocalLabels(snapshot,keys,declarations,port);var tape=new SnapshotDiagnosticTemplates(new SnapshotDiagnosticChecks.Store(),index)) {
                int before=f.s.cursors;long root=index.template(list,f.a,tape);eq(before+1,f.s.cursors);assertIssues(tape,root,91,List.of(new Item(2,rows[n-1])));
                long reads=port.selects,appends=port.appends;
                for(int q=0;q<n;q++) {
                    var none=new Report(0);tape.emit(index.template(list,f.a,tape),100+q,none);eq(1L,none.count);
                    none=new Report(0);tape.emit(index.template(list,f.b,tape),100+q,none);eq(n-1L,none.count);
                }
                eq(reads,port.selects);eq(appends,port.appends);eq(before+1,f.s.cursors);eq((long)n,appends);
                System.out.println("SNAPSHOT_LABEL_COST rows="+n+" contexts="+(2L*n+1)+" sourceFolds=1 appended="+appends+" selected="+reads);
            }
            eq(0L,port.claimed);
        }
    }
    static void everyStorageAndSourceFaultPoisonsOwnerWithoutClosingBorrowedInput() {
        var f=new Fixture();long list=f.s.list(f.label(f.b,"missing"));var measured=new Store();long requests;
        try(var snapshot=AirSnapshot.attach(f.s,f.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());
            var declarations=SnapshotDeclarations.build(snapshot,keys,new SnapshotDeclarationChecks.Store(),1000,1000,(r,i,n)->{throw new AssertionError(r);});
            var index=new SnapshotLocalLabels(snapshot,keys,declarations,measured);var tape=new SnapshotDiagnosticTemplates(new SnapshotDiagnosticChecks.Store(),index)) {
            tape.emit(index.template(list,f.a,tape),1,new Report(2));requests=measured.requests;
        }
        for(long at=0;at<requests;at++) {
            var g=new Fixture();long labels=g.s.list(g.label(g.b,"missing"));var port=new Store();port.remaining=at;
            try(var snapshot=AirSnapshot.attach(g.s,g.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());
                var declarations=SnapshotDeclarations.build(snapshot,keys,new SnapshotDeclarationChecks.Store(),1000,1000,(r,i,n)->{throw new AssertionError(r);} )) {
                SnapshotLocalLabels index=null;
                try {index=new SnapshotLocalLabels(snapshot,keys,declarations,port);var active=index;
                    try(var tape=new SnapshotDiagnosticTemplates(new SnapshotDiagnosticChecks.Store(),index)){eq(port.failure,fails(IllegalStateException.class,()->tape.emit(active.template(labels,g.a,tape),1,new Report(2))));}
                    fails(IllegalStateException.class,index::counts);
                } catch(IllegalStateException failure){eq(port.failure,failure);}finally{if(index!=null)index.close();}
                eq(AirShape.PUBLICATION,snapshot.shape(g.root));eq(true,declarations.entities()>0);
            }
            eq(0L,port.claimed);eq(true,port.closed);eq(0,g.s.activeCursors);
        }
        for(int fault=0;fault<3;fault++) {
            var g=new Fixture();long label=g.label(g.b,"missing"),labels=g.s.list(label);var port=new Store();
            try(var snapshot=AirSnapshot.attach(g.s,g.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());
                var declarations=SnapshotDeclarations.build(snapshot,keys,new SnapshotDeclarationChecks.Store(),1000,1000,(r,i,n)->{throw new AssertionError(r);} );
                var index=new SnapshotLocalLabels(snapshot,keys,declarations,port);var tape=new SnapshotDiagnosticTemplates(new SnapshotDiagnosticChecks.Store(),index)) {
                if(fault==0)g.s.failNode=label;
                if(fault==1){g.s.badLength=labels;g.s.lengthDelta=1;}
                if(fault==2){g.s.failingCursor=labels;g.s.closeCursorFailure=true;}
                fails(IllegalStateException.class,()->index.template(labels,g.a,tape));fails(IllegalStateException.class,index::counts);eq(AirShape.PUBLICATION,snapshot.shape(g.root));
            }
            eq(0L,port.claimed);eq(0,g.s.activeCursors);
        }
    }
    private static void assertIssues(SnapshotDiagnosticTemplates tape,long root,long owner,List<Item> expected){var r=new Report(Long.MAX_VALUE);tape.emit(root,owner,r);eq(expected,r.items);eq((long)expected.size(),r.count);if(!expected.isEmpty())eq(owner,r.owner);}
    private record Item(int rule,long label) { }
    private static final class Report implements SnapshotDiagnosticTemplates.Reports {
        long capacity,count,owner;final List<Item> items=new ArrayList<>();Report(long capacity){this.capacity=capacity;}
        public long remaining(){return capacity;}
        public void occurrences(ValidationIssue.Kind kind,long owner,long count){eq(ValidationIssue.Kind.INVALID_IR,kind);this.count+=count;this.owner=owner;}
        public void retain(ValidationIssue.Kind kind,int rule,long owner,long anchor,int field,long detail){eq(ValidationIssue.Kind.INVALID_IR,kind);eq(-1,field);eq(anchor,detail);eq(this.owner,owner);eq(true,capacity-->0);items.add(new Item(rule,anchor));}
    }
    static final class Fixture {
        final SnapshotGraphChecks.Source s=new SnapshotGraphChecks.Source();final long root,a,b;
        Fixture(){var f=new SnapshotGraphChecks.Fixture(s);root=f.root;a=unit("P","a");b=unit("P","b");s.replaceField(root,4,s.list(declaration(f,a),declaration(f,b)));}
        long unit(String publication,String name){return s.record(AirShape.IDS_UNIT_ID,s.record(AirShape.IDS_PUBLICATION_ID,s.text(publication)),s.text(name));}
        long label(long unit,String name){return s.record(AirShape.IDS_LABEL_ID,unit,s.text(name));}
        // Only declaration/read contracts are under test; unobserved annotation fields are not a full AIR fixture.
        long declaration(SnapshotGraphChecks.Fixture f,long unit){long id=s.record(AirShape.IDS_OPERATION_ID,unit,s.text("halt"));long header=s.record(AirShape.OPERATIONS_HEADER,id,f.id,f.id,f.id,f.empty);long halt=s.record(AirShape.OPERATIONS_HALT,header,s.scalar(AirShape.OPERATIONS_HALT_KIND,0));long sequence=s.record(AirShape.SEQUENCE,label(unit,"defined"),f.empty,halt,f.id);return s.record(AirShape.UNIT,unit,s.optional(),f.empty,f.empty,f.empty,s.list(sequence),f.empty,s.scalar(AirShape.UNIT_BODY_AVAILABILITY,0),s.optional(),f.id,f.id);}
    }
    static final class Store implements SnapshotLocalLabels.Storage {
        final Map<Long,Long> tables=new HashMap<>();final Map<Long,List<long[]>> rows=new HashMap<>();final Set<Long> complete=new HashSet<>();
        long issued,claimed,requests,remaining=Long.MAX_VALUE,appends,selects;boolean closed;
        final IllegalStateException failure=new IllegalStateException("label storage failure");
        void work(){requests++;if(remaining--==0)throw failure;}
        public long find(long list){work();long id=tables.getOrDefault(list,0L);if(id!=0&&!complete.contains(id))throw failure;return id;}
        public long begin(long list){work();long id=++issued;tables.put(list,id);rows.put(id,new ArrayList<>());return id;}
        public void row(long table,long label,long unit,boolean missing){work();rows.get(table).add(new long[]{label,unit,missing?1:0});appends++;}
        public void finish(long table){work();complete.add(table);}
        public long rows(long table){work();return rows.get(table).size();}
        public long matching(long table,long unit){work();long count=0;for(var row:rows.get(table))if(row[1]==unit)count++;return count;}
        public long missing(long table){work();long count=0;for(var row:rows.get(table))if(row[2]!=0)count++;return count;}
        public long label(long table,long ordinal){work();return rows.get(table).get(Math.toIntExact(ordinal))[0];}
        public long missingAt(long table,long ordinal){work();selects++;long at=0;for(var row:rows.get(table)){if(row[2]!=0&&ordinal--==0)return at;at++;}throw failure;}
        public long foreignAt(long table,long unit,long ordinal){work();selects++;long at=0;for(var row:rows.get(table)){if(row[1]!=unit&&ordinal--==0)return at;at++;}throw failure;}
        public AirSnapshotBuilder.Lease claim(long bytes){work();claimed+=bytes;return ()->claimed-=bytes;}
        public void close(){closed=true;tables.clear();rows.clear();complete.clear();}
    }
    private static <T extends Throwable>T fails(Class<T> type,Runnable action){try{action.run();}catch(Throwable error){if(type.isInstance(error))return type.cast(error);throw new AssertionError(error);}throw new AssertionError("missing "+type);}
    private static void eq(Object expected,Object actual){if(!Objects.equals(expected,actual))throw new AssertionError("expected="+expected+" actual="+actual);}
}
