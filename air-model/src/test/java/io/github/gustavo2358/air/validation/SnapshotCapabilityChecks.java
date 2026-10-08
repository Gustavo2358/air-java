package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import java.util.*;

/** Independent managed-set oracle for capability admission. */
final class SnapshotCapabilityChecks {
    private SnapshotCapabilityChecks(){ }
    static void duplicateProfilesUnsupportedAndMissingUsesStayClassified() {
        var f=new Fixtures();f.linear(new Operations.Nop(f.header(f.op("nop"))));
        f.capabilities.add(new Capabilities.Capability("custom.extension","1"));
        f.capabilities.add(new Capabilities.Capability("custom.extension","2"));
        f.capabilities.add(new Capabilities.Capability("AIR-profile","1"));
        var found=new ArrayList<SnapshotCapabilities.Rule>();
        try(var snapshot=AirSnapshot.fromPublication(f.build());var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());
            var capabilities=SnapshotCapabilities.build(snapshot,keys,new Store(),(rule,owner,capability)->found.add(rule))) {
            assertEquals(List.of(SnapshotCapabilities.Rule.UNSUPPORTED,SnapshotCapabilities.Rule.DUPLICATE,
                SnapshotCapabilities.Rule.UNSUPPORTED,SnapshotCapabilities.Rule.PROFILE,
                SnapshotCapabilities.Rule.DUPLICATE,SnapshotCapabilities.Rule.PROFILE),found);
            found.clear();long owner=snapshot.field(snapshot.root(),AirShape.PUBLICATION,0);
            assertEquals(false,capabilities.require("memory.regions","1",owner,(rule,id,cap)->found.add(rule)));
            assertEquals(List.of(SnapshotCapabilities.Rule.MISSING),found);
        }
    }
    private record Pair(long first,long second){ }
    static final class Store implements SnapshotCapabilities.Storage {
        final Set<Pair> names=new HashSet<>(),required=new HashSet<>(),policies=new HashSet<>();long claimed;
        public boolean firstName(long list,long name){return names.add(new Pair(list,name));}
        public void require(long name,long version){required.add(new Pair(name,version));}
        public boolean required(long name,long version){return required.contains(new Pair(name,version));}
        public void addNamePolicy(long name,long version){policies.add(new Pair(name,version));}
        public boolean hasNamePolicy(long name,long version){return policies.contains(new Pair(name,version));}
        public AirSnapshotBuilder.Lease claim(long bytes){claimed+=bytes;return ()->claimed-=bytes;}
        public void close(){names.clear();required.clear();policies.clear();}
    }
    private static void assertEquals(Object expected,Object actual){if(!Objects.equals(expected,actual))throw new AssertionError("expected="+expected+" actual="+actual);}
}
