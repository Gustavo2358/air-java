package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import java.math.BigInteger;
import java.util.*;

/** Arbitrary INTEGER vs actual natural row ordinals, independently checked without host narrowing. */
final class SnapshotOrdinalChecks {
    private SnapshotOrdinalChecks() { }
    static void exactNaturalOrdinalsIncludeLongBoundaryAndRejectHugePositions() {
        var source=new SnapshotAtomChecks.Source();var ordinals=new ArrayList<Long>();
        Collections.addAll(ordinals,0L,1L,9L,10L,99L,100L,999999999999999L,1000000000000000L,9999999999999999L,10000000000000000L,Long.MAX_VALUE-1,Long.MAX_VALUE);
        var random=new Random(94201);for(int n=0;n<64;n++)ordinals.add(random.nextLong()&Long.MAX_VALUE);
        var values=new ArrayList<BigInteger>();for(long ordinal:ordinals)values.add(BigInteger.valueOf(ordinal));
        values.add(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));values.add(BigInteger.ONE.negate());values.add(BigInteger.valueOf(Long.MIN_VALUE));
        values.add(BigInteger.TEN.pow(4096));var handles=new ArrayList<Long>();for(var value:values)handles.add(source.add(AirShape.INTEGER,value.toString()));
        var port=new SnapshotAtomChecks.Store();
        try(var snapshot=AirSnapshot.attach(source,1);var keys=new SnapshotIdentityKeys(snapshot,port)) {
            for(long handle:handles)keys.atomKey(handle);long chars=source.characters,requests=port.requests,words=port.words;
            for(int n=0;n<values.size();n++)for(long ordinal:ordinals)eq(values.get(n).equals(BigInteger.valueOf(ordinal)),keys.integerEqualsNatural(handles.get(n),ordinal));
            eq(chars,source.characters);eq(requests,port.requests);eq(true,port.words-words<=24L*values.size()*ordinals.size());
        }
        eq(0L,port.claimed);
    }
    static void cachedOrdinalQueriesUseBoundedPrimitiveWordsAndFailureOwnership() {
        for(int size:new int[]{16,256,4096,65536,262144}) {
            var source=new SnapshotAtomChecks.Source();long number=source.add(AirShape.INTEGER,"1"+"9".repeat(size-1));var port=new SnapshotAtomChecks.Store();
            try(var snapshot=AirSnapshot.attach(source,1);var keys=new SnapshotIdentityKeys(snapshot,port)) {
                keys.atomKey(number);long chars=source.characters,requests=port.requests,words=port.words;
                for(int n=0;n<256;n++)eq(false,keys.integerEqualsNatural(number,1));
                eq(chars,source.characters);eq(requests,port.requests);eq(true,port.words-words<=4L*256);
                System.out.println("SNAPSHOT_ORDINAL_COST characters="+size+" queries=256 words="+(port.words-words));
            }
            eq(0L,port.claimed);
        }
        var source=new SnapshotAtomChecks.Source();long number=source.add(AirShape.INTEGER,"9223372036854775807");var port=new SnapshotAtomChecks.Store();
        try(var snapshot=AirSnapshot.attach(source,1);var keys=new SnapshotIdentityKeys(snapshot,port)) {
            keys.atomKey(number);port.failWord=true;eq(port.failure,fails(IllegalStateException.class,()->keys.integerEqualsNatural(number,Long.MAX_VALUE)));
            fails(IllegalStateException.class,()->keys.atomKey(number));eq(AirShape.INTEGER,snapshot.shape(number));
        }
        eq(0L,port.claimed);
        var wrong=new SnapshotAtomChecks.Source();long text=wrong.add(AirShape.TEXT,"0");var scratch=new SnapshotAtomChecks.Store();
        try(var snapshot=AirSnapshot.attach(wrong,1);var keys=new SnapshotIdentityKeys(snapshot,scratch)) {fails(IllegalArgumentException.class,()->keys.integerEqualsNatural(text,0));eq(AirShape.TEXT,snapshot.shape(text));}
        eq(0L,scratch.claimed);
        var negative=new SnapshotAtomChecks.Source();long integer=negative.add(AirShape.INTEGER,"0");var owned=new SnapshotAtomChecks.Store();
        try(var snapshot=AirSnapshot.attach(negative,1);var keys=new SnapshotIdentityKeys(snapshot,owned)){fails(IllegalArgumentException.class,()->keys.integerEqualsNatural(integer,-1));eq(AirShape.INTEGER,snapshot.shape(integer));}
        eq(0L,owned.claimed);
    }
    private static <T extends Throwable>T fails(Class<T> type,Runnable action){try{action.run();}catch(Throwable error){if(type.isInstance(error))return type.cast(error);throw new AssertionError(error);}throw new AssertionError("missing "+type);}
    private static void eq(Object expected,Object actual){if(!Objects.equals(expected,actual))throw new AssertionError("expected="+expected+" actual="+actual);}
}
