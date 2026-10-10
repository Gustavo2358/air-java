package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import java.math.BigInteger;
import java.util.*;

/** Manual positional/membership/diagnostic-count oracles, separate from full signature admission. */
final class SnapshotSignatureChecks {
    private SnapshotSignatureChecks() { }
    static void openAndClosedPositionsCountEachBadRowOnceWithIndependentOrder() {
        var source=new SnapshotGraphChecks.Source();var f=new SnapshotGraphChecks.Fixture(source);long owner=f.id;
        long[] rows={row(source,AirShape.INTERACTIONS_RESULT_SLOT,"0"),row(source,AirShape.INTERACTIONS_RESULT_SLOT,"2"),row(source,AirShape.INTERACTIONS_RESULT_SLOT,"2"),row(source,AirShape.INTERACTIONS_RESULT_SLOT,"9223372036854775808")};
        long list=source.list(rows),closed=inventory(source,list,true,false),open=inventory(source,list,false,false);long emptyClosed=inventory(source,source.list(),true,true);var port=new Store();var report=new Report(100);
        try(var snapshot=AirSnapshot.attach(source,f.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var index=new SnapshotSignatureIndex(snapshot,keys,port)) {
            index.checkInventory(closed,owner,report);eq(3L,report.count);eq(List.of(1L,2L,3L),report.ordinals);eq(3L,index.counts().closedErrors());
            report=new Report(100);index.checkInventory(open,owner,report);eq(1L,report.count);eq(List.of(2L),report.ordinals);eq(1L,index.counts().openErrors());eq(1L,index.counts().lists());eq(4L,index.counts().rows());
            report=new Report(100);index.checkInventory(emptyClosed,owner,report);eq(0L,report.count);
            eq(0,source.indexedListReads);eq(0,source.activeCursors);
        }
        eq(true,port.closed);eq(0L,port.claimed);
        // Ordered open gaps are allowed; closed rows still report every noncontiguous position.
        var gaps=new SnapshotGraphChecks.Source();var g=new SnapshotGraphChecks.Fixture(gaps);long listGaps=gaps.list(row(gaps,AirShape.INTERACTIONS_PARAMETER,"1"),row(gaps,AirShape.INTERACTIONS_PARAMETER,"3"));
        long closedGaps=inventory(gaps,listGaps,true,true),openGaps=inventory(gaps,listGaps,false,true);
        try(var snapshot=AirSnapshot.attach(gaps,g.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var index=new SnapshotSignatureIndex(snapshot,keys,new Store())) {
            var r=new Report(100);index.checkInventory(openGaps,g.id,r);eq(0L,r.count);index.checkInventory(closedGaps,g.id,r);eq(2L,r.count);eq(List.of(0L,1L),r.ordinals);
        }
    }
    static void badSharedListsNeverReplayAndRetentionDoesNotStopExactCounts() {
        for(int size:new int[]{16,64,256,1024,4096}) {
            var source=new SnapshotGraphChecks.Source();var f=new SnapshotGraphChecks.Fixture(source);long[] rows=new long[size];
            for(int n=0;n<size;n++)rows[n]=row(source,AirShape.INTERACTIONS_PARAMETER,Integer.toString(n+1));
            long list=source.list(rows),closed=inventory(source,list,true,true),open=inventory(source,list,false,true);long actual=source.integer(Integer.toString(size)),absent=source.integer("999999999999999999999999999999999999999");var port=new Store();
            try(var snapshot=AirSnapshot.attach(source,f.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var index=new SnapshotSignatureIndex(snapshot,keys,port)) {
                var r=new Report(2);index.checkInventory(closed,f.id,r);eq((long)size,r.count);eq(List.of(0L,1L),r.ordinals);eq(1,source.cursors);
                long reads=port.appends,selections=port.selections;
                for(int q=0;q<size;q++){index.checkInventory(closed,f.id,r);index.checkInventory(open,f.id,r);}
                eq((size+1L)*size,r.count);eq(2,r.ordinals.size());eq(selections,port.selections);eq(reads,port.appends);eq(1,source.cursors);
                eq(1L,index.counts().lists());eq((long)size,index.counts().rows());eq(0L,index.counts().openErrors());eq((long)size,index.counts().closedErrors());
                for(int q=0;q<size;q++){eq(true,index.containsParameter(closed,actual));eq(false,index.containsParameter(closed,absent));}
                eq(1,source.cursors);eq(size,port.members.size());
                System.out.println("SNAPSHOT_SIGNATURE_COST rows="+size+" ownerUses="+(2L*size+1)+" errors="+r.count+" retained=2 folds="+source.cursors+" appended="+port.appends);
            }
            eq(0L,port.claimed);
        }
    }
    static void independentIntegerListsAndContextKeysPreserveEveryBadOrdinal() {
        var random=new Random(0x518aL);
        for(int run=0;run<96;run++) {
            var source=new SnapshotGraphChecks.Source();var f=new SnapshotGraphChecks.Fixture(source);
            int size=random.nextInt(48);long[] rows=new long[size];var values=new ArrayList<BigInteger>();
            for(int at=0;at<size;at++) {
                BigInteger value=switch(random.nextInt(5)){case 0->BigInteger.valueOf(at);case 1->BigInteger.valueOf(random.nextInt(16));case 2->BigInteger.ONE.shiftLeft(130).add(BigInteger.valueOf(at));case 3->BigInteger.valueOf(-random.nextInt(16));default->BigInteger.valueOf(at+1L);};
                values.add(value);rows[at]=row(source,AirShape.INTERACTIONS_PARAMETER,value.toString());
            }
            long list=source.list(rows),open=inventory(source,list,false,true),closed=inventory(source,list,true,true);
            var expectedOpen=new ArrayList<Long>();var expectedClosed=new ArrayList<Long>();
            for(int at=0;at<size;at++) {
                boolean unordered=at>0&&values.get(at).compareTo(values.get(at-1))<=0;
                if(unordered)expectedOpen.add((long)at);
                if(unordered||!values.get(at).equals(BigInteger.valueOf(at)))expectedClosed.add((long)at);
            }
            try(var snapshot=AirSnapshot.attach(source,f.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var index=new SnapshotSignatureIndex(snapshot,keys,new Store())) {
                var a=new Report(Long.MAX_VALUE);var b=new Report(Long.MAX_VALUE);index.checkInventory(open,f.id,a);index.checkInventory(closed,f.id,b);
                eq(expectedOpen,a.ordinals);eq(expectedClosed,b.ordinals);eq((long)expectedOpen.size(),a.count);eq((long)expectedClosed.size(),b.count);eq(1,source.cursors);
            }
        }
        var source=new SnapshotGraphChecks.Source();var f=new SnapshotGraphChecks.Fixture(source);long empty=source.list(),p=inventory(source,empty,true,true),r=inventory(source,empty,true,false);
        try(var snapshot=AirSnapshot.attach(source,f.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var index=new SnapshotSignatureIndex(snapshot,keys,new Store())) {
            index.checkInventory(p,f.id,new Report(0));index.checkInventory(r,f.id,new Report(0));eq(2L,index.counts().lists());eq(0L,index.counts().rows());eq(2,source.cursors);
        }
    }
    static void parameterInitialMembershipUsesCanonicalIntegersAndEntryOwner() {
        var f=new Fixtures();f.linear(new Operations.Nop(f.header(f.op("nop"))));
        var type=Fixtures.known(Types.Builtin.TEXT);var unknown=f.uncertainty("mode","MODE_UNKNOWN");
        f.signature=f.signature(List.of(new Interactions.Parameter(BigInteger.ZERO,new Interactions.UnknownMode(unknown),type,Interactions.ExternalBinding.INSTANCE,f.origin)),List.of());
        var object=f.object("x",type);var place=new Places.ObjectPlace(f.entryOperand("seed",Operand.Role.VALUE_WRITE),object);
        f.state=new Entries.EntryState(List.of(new Entries.InitialCondition(place,new Entries.ParameterInitial(BigInteger.ZERO),f.origin,List.of()),new Entries.InitialCondition(place,new Entries.ParameterInitial(BigInteger.ONE),f.origin,List.of())),List.of());
        var report=new Report(100);
        try(var snapshot=AirSnapshot.fromPublication(f.build());var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var index=new SnapshotSignatureIndex(snapshot,keys,new Store())) {
            long unit=snapshot.element(snapshot.field(snapshot.root(),AirShape.PUBLICATION,4),AirShape.UNIT,0),entry=snapshot.element(snapshot.field(unit,AirShape.UNIT,4),AirShape.ENTRIES_ENTRY,0);
            index.checkEntry(entry,report);eq(1L,report.count);eq(List.of(SnapshotSignatureIndex.Rule.PARAMETER_INITIAL),report.rules);eq(snapshot.field(entry,AirShape.ENTRIES_ENTRY,0),report.owner);
        }
    }
    static void everyFoldAndReportFailurePoisonsScratchAndPreservesBorrowedInput() {
        var source=new SnapshotGraphChecks.Source();var f=new SnapshotGraphChecks.Fixture(source);long list=source.list(row(source,AirShape.INTERACTIONS_PARAMETER,"2")),inventory=inventory(source,list,true,true);var measured=new Store();
        try(var snapshot=AirSnapshot.attach(source,f.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var index=new SnapshotSignatureIndex(snapshot,keys,measured)){index.checkInventory(inventory,f.id,new Report(100));}
        for(long at=0;at<measured.requests;at++) {
            var input=new SnapshotGraphChecks.Source();var fixture=new SnapshotGraphChecks.Fixture(input);long inv=inventory(input,input.list(row(input,AirShape.INTERACTIONS_PARAMETER,"2")),true,true);var port=new Store();port.remaining=at;
            try(var snapshot=AirSnapshot.attach(input,fixture.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var index=new SnapshotSignatureIndex(snapshot,keys,port)) {
                eq(port.failure,fails(IllegalStateException.class,()->index.checkInventory(inv,fixture.id,new Report(100))));fails(IllegalStateException.class,index::counts);eq(AirShape.PUBLICATION,snapshot.shape(fixture.root));
            }
            eq(0L,port.claimed);eq(true,port.closed);eq(0,input.activeCursors);
        }
        var input=new SnapshotGraphChecks.Source();var fixture=new SnapshotGraphChecks.Fixture(input);long inv=inventory(input,input.list(row(input,AirShape.INTERACTIONS_PARAMETER,"2")),true,true);var port=new Store();var report=new Report(100);report.fail=true;
        try(var snapshot=AirSnapshot.attach(input,fixture.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var index=new SnapshotSignatureIndex(snapshot,keys,port)) {
            eq(report.failure,fails(IllegalStateException.class,()->index.checkInventory(inv,fixture.id,report)));fails(IllegalStateException.class,index::counts);eq(0,input.activeCursors);
        }
        eq(0L,port.claimed);
    }
    static void sourceCallbackConstructorAndCleanupFailuresPreserveOwnership() {
        for(int mode=0;mode<4;mode++) {
            var source=new SnapshotGraphChecks.Source();var f=new SnapshotGraphChecks.Fixture(source);long bad=row(source,AirShape.INTERACTIONS_PARAMETER,"2"),list=source.list(bad),inv=inventory(source,list,true,true);var port=new Store();
            if(mode==0){source.failNode=bad;source.closeCursorFailure=true;source.failingCursor=list;}
            if(mode==1){source.badLength=list;source.lengthDelta=1;}
            if(mode==2){source.closeCursorFailure=true;source.failingCursor=list;}
            if(mode==3){source.badLength=list;source.lengthDelta=-1;}
            try(var snapshot=AirSnapshot.attach(source,f.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var index=new SnapshotSignatureIndex(snapshot,keys,port)) {
                var error=fails(IllegalStateException.class,()->index.checkInventory(inv,f.id,new Report(100)));
                if(mode==0){eq(source.failure,error);eq(List.of(source.cleanup),List.of(error.getSuppressed()));}
                if(mode==2)eq(source.cleanup,error);
                fails(IllegalStateException.class,index::counts);eq(AirShape.PUBLICATION,snapshot.shape(f.root));eq(0,source.activeCursors);
            }
            eq(0L,port.claimed);eq(true,port.closed);
        }
        for(int step=0;step<3;step++) {
            var source=new SnapshotGraphChecks.Source();var f=new SnapshotGraphChecks.Fixture(source);long inv=inventory(source,source.list(row(source,AirShape.INTERACTIONS_PARAMETER,"2")),true,true);var port=new Store();var failure=new IllegalStateException("report step");final int at=step;
            var report=new SnapshotSignatureIndex.Reports(){public long remaining(){if(at==0)throw failure;return 1;}public void occurrences(SnapshotSignatureIndex.Rule rule,long owner,long count){if(at==1)throw failure;}public void retain(SnapshotSignatureIndex.Rule rule,long owner,long row,long position,long ordinal){if(at==2)throw failure;}};
            try(var snapshot=AirSnapshot.attach(source,f.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var index=new SnapshotSignatureIndex(snapshot,keys,port)) {
                eq(failure,fails(IllegalStateException.class,()->index.checkInventory(inv,f.id,report)));fails(IllegalStateException.class,index::counts);
            }
            eq(0L,port.claimed);
        }
        var source=new SnapshotGraphChecks.Source();var f=new SnapshotGraphChecks.Fixture(source);long inv=inventory(source,source.list(row(source,AirShape.INTERACTIONS_PARAMETER,"2")),true,true);
        try(var snapshot=AirSnapshot.attach(source,f.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store())) {
            var denied=new Store();denied.denyClaim=true;eq(denied.failure,fails(IllegalStateException.class,()->new SnapshotSignatureIndex(snapshot,keys,denied)));eq(true,denied.closed);eq(0L,denied.claimed);
            var nullInput=new Store();fails(NullPointerException.class,()->new SnapshotSignatureIndex(null,keys,nullInput));eq(true,nullInput.closed);
            var nullKeys=new Store();fails(NullPointerException.class,()->new SnapshotSignatureIndex(snapshot,null,nullKeys));eq(true,nullKeys.closed);
            var port=new Store();var index=new SnapshotSignatureIndex(snapshot,keys,port);
            var callback=new SnapshotSignatureIndex.Reports(){public long remaining(){return 1;}public void occurrences(SnapshotSignatureIndex.Rule rule,long owner,long count){index.containsParameter(inv,source.nodes.size());}public void retain(SnapshotSignatureIndex.Rule rule,long owner,long row,long position,long ordinal){throw new AssertionError("unreachable");}};
            fails(IllegalStateException.class,()->index.checkInventory(inv,f.id,callback));fails(IllegalStateException.class,index::counts);
            port.closeFailure=port.leaseFailure=true;eq(port.cleanup,fails(IllegalStateException.class,index::close));eq(List.of(port.leaseCleanup),List.of(port.cleanup.getSuppressed()));index.close();eq(0L,port.claimed);eq(AirShape.PUBLICATION,snapshot.shape(f.root));
        }
    }
    // Only position fields are inspected; other slots belong to later full signature checks.
    private static long row(SnapshotGraphChecks.Source s,AirShape kind,String value) {long p=s.integer(value);return kind==AirShape.INTERACTIONS_PARAMETER?s.record(kind,p,p,p,p,p):s.record(kind,p,p,p);}
    private static long inventory(SnapshotGraphChecks.Source s,long list,boolean closed,boolean parameter){long remainder=closed?s.scalar(AirShape.INTERACTIONS_NO_REMAINDER,0):s.record(AirShape.INTERACTIONS_UNKNOWN_REMAINDER,s.record(AirShape.IDS_UNCERTAINTY_ID,s.record(AirShape.IDS_PUBLICATION_ID,s.text("P")),s.text("r")));return s.record(parameter?AirShape.INTERACTIONS_PARAMETER_INVENTORY:AirShape.INTERACTIONS_RESULT_INVENTORY,list,remainder);}
    private record Key(long list,int kind) { }
    private record Member(long handle,long atom) { }
    private record Bad(long row,long position,long ordinal,boolean ordered,boolean natural) { }
    private static final class Table {long rows;boolean done;final ArrayList<Bad> open=new ArrayList<>(),closed=new ArrayList<>();}
    static final class Store implements SnapshotSignatureIndex.Storage {
        final Map<Key,Long> lookup=new HashMap<>();final Map<Long,Table> tables=new HashMap<>();final Set<Member> members=new HashSet<>();
        Iterator<Bad> cursor;Bad current;long issued,claimed,requests,remaining=Long.MAX_VALUE,appends,selections;boolean closed,denyClaim,closeFailure,leaseFailure;
        final IllegalStateException cleanup=new IllegalStateException("signature owner cleanup"),leaseCleanup=new IllegalStateException("signature lease cleanup");
        final IllegalStateException failure=new IllegalStateException("signature scratch failure");
        private void work(){requests++;if(remaining--==0)throw failure;}
        public long find(long list,int kind){work();long handle=lookup.getOrDefault(new Key(list,kind),0L);return handle!=0&&tables.get(handle).done?handle:0;}
        public long begin(long list,int kind){work();long handle=++issued;if(lookup.putIfAbsent(new Key(list,kind),handle)!=null)throw new AssertionError("unfinished signature reused");tables.put(handle,new Table());return handle;}
        public void member(long handle,long atom){work();members.add(new Member(handle,atom));}
        public void issue(long handle,long row,long position,long ordinal,boolean unordered,boolean noncontiguous){work();appends++;var bad=new Bad(row,position,ordinal,unordered,noncontiguous);if(unordered)tables.get(handle).open.add(bad);if(unordered||noncontiguous)tables.get(handle).closed.add(bad);}
        public void finish(long handle,long rows){work();tables.get(handle).rows=rows;tables.get(handle).done=true;}
        public long rows(long handle){work();return tables.get(handle).rows;}
        public long issues(long handle,boolean closedMode){work();return (closedMode?tables.get(handle).closed:tables.get(handle).open).size();}
        public boolean contains(long handle,long atom){work();return members.contains(new Member(handle,atom));}
        public void select(long handle,boolean closedMode){work();selections++;cursor=(closedMode?tables.get(handle).closed:tables.get(handle).open).iterator();}
        public boolean advanceIssue(){work();if(!cursor.hasNext())return false;current=cursor.next();return true;}
        public long row(){return current.row();}public long position(){return current.position();}public long ordinal(){return current.ordinal();}
        public AirSnapshotBuilder.Lease claim(long bytes){if(denyClaim)throw failure;claimed+=bytes;return ()->{claimed-=bytes;if(leaseFailure)throw leaseCleanup;};}
        public void close(){if(closed)return;closed=true;lookup.clear();tables.clear();members.clear();cursor=null;current=null;if(closeFailure)throw cleanup;}
    }
    static final class Report implements SnapshotSignatureIndex.Reports {
        long capacity,count,owner;final ArrayList<Long> ordinals=new ArrayList<>();final ArrayList<SnapshotSignatureIndex.Rule> rules=new ArrayList<>();boolean fail;
        final IllegalStateException failure=new IllegalStateException("signature report failure");
        Report(long capacity){this.capacity=capacity;}
        public long remaining(){return capacity;}
        public void occurrences(SnapshotSignatureIndex.Rule rule,long owner,long count){if(fail)throw failure;this.count=Math.addExact(this.count,count);this.owner=owner;}
        public void retain(SnapshotSignatureIndex.Rule rule,long owner,long row,long position,long ordinal){if(fail)throw failure;if(capacity--==0)throw new AssertionError("retention overrun");this.owner=owner;ordinals.add(ordinal);rules.add(rule);}
    }
    private static <T extends Throwable>T fails(Class<T> type,Runnable action){try{action.run();}catch(Throwable error){if(type.isInstance(error))return type.cast(error);throw new AssertionError(error);}throw new AssertionError("missing "+type);}
    private static void eq(Object expected,Object actual){if(!Objects.equals(expected,actual))throw new AssertionError("expected="+expected+" actual="+actual);}
}
