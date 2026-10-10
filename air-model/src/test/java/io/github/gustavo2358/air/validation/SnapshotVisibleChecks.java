package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;

/** Tiny independent relation oracle. */
final class SnapshotVisibleChecks {
    private SnapshotVisibleChecks(){ }
    static void declaredAndImportedObjectsShareExactNamespacedMembership() {
        var f=new Fixtures();var own=f.object("own",Fixtures.known(Types.Builtin.INT));
        f.linear(new Operations.Nop(f.header(f.op("nop"))));
        Unit base=f.build().units().get(0);
        var otherUnit=new UnitId(f.pub,"other");var imported=new ObjectId(otherUnit,"imported");
        Unit changed=new Unit(base.id(),base.containingUnit(),base.objects(),List.of(imported,imported),base.entries(),base.sequences(),base.completionPorts(),base.body(),base.bodyUnavailable(),base.coverage(),base.origin());
        var publication=Fixtures.withUnits(f.build(),List.of(changed));var store=new Store();
        try(var snapshot=AirSnapshot.fromPublication(publication);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var visible=SnapshotVisibleObjects.build(snapshot,keys,store)) {
            long unit=find(snapshot,AirShape.IDS_UNIT_ID,"unit"),ownId=find(snapshot,AirShape.IDS_OBJECT_ID,"own"),importedId=find(snapshot,AirShape.IDS_OBJECT_ID,"imported");
            eq(true,visible.contains(unit,ownId));eq(true,visible.contains(unit,importedId));
            eq(true,visible.contains(f.unit,own));eq(true,visible.contains(f.unit,imported));
            eq(false,visible.contains(otherUnit,own));
            eq(false,visible.contains(f.unit,new ObjectId(new UnitId(new PublicationId("Q"),f.unit.localId()),own.localId())));
            eq(true,visible.firstGrounding(ownId));eq(false,visible.firstGrounding(ownId));
            eq(new SnapshotVisibleObjects.Counts(1,3,2),visible.counts());
            closeSnapshot(snapshot);
            try {visible.contains(f.unit,own);throw new AssertionError("closed source accepted");}
            catch(IllegalStateException expected) { }
            try {visible.counts();throw new AssertionError("failed relation remained open");}
            catch(IllegalStateException expected) { }
        }
        eq(true,store.closed);eq(0L,store.claimed);
    }
    private static void closeSnapshot(AirSnapshot snapshot){snapshot.close();}
    private static long find(AirSnapshot snapshot,AirShape shape,String local) {
        final long[] found={0};
        SnapshotGraphWalk.scan(snapshot,new SnapshotGraphChecks.Store(),10_000,1000,(node,actual,element)->{
            if(actual==shape&&text(snapshot,snapshot.field(node,shape,1)).equals(local))found[0]=node;
        });
        return found[0];
    }
    private static String text(AirSnapshot snapshot,long node){char[] value=new char[Math.toIntExact(snapshot.characterCount(node))];snapshot.readCharacters(node,0,value,0,value.length);return new String(value);}
    private record Pair(long unit,long object){ }
    static final class Store implements SnapshotVisibleObjects.Storage {
        final Set<Pair> pairs=new HashSet<>();final Set<Long> grounding=new HashSet<>();long claimed;boolean closed;
        public boolean add(long unit,long object){return pairs.add(new Pair(unit,object));}
        public boolean contains(long unit,long object){return pairs.contains(new Pair(unit,object));}
        public boolean firstGrounding(long object){return grounding.add(object);}
        public AirSnapshotBuilder.Lease claim(long bytes){claimed+=bytes;return ()->claimed-=bytes;}
        public void close(){closed=true;pairs.clear();grounding.clear();}
    }
    private static void eq(Object expected,Object actual){if(!Objects.equals(expected,actual))throw new AssertionError("expected="+expected+" actual="+actual);}
}
