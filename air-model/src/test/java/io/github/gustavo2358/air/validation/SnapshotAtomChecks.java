package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import java.util.*;

/** Exact hand-written Unicode/integer summaries over frozen synthetic scalar sources. */
final class SnapshotAtomChecks {
    private SnapshotAtomChecks() { }
    static void exactUnicodeAndTypedAtomKeys() {
        var source=new Source();long empty=source.add(AirShape.TEXT,"");long nul=source.add(AirShape.TEXT,"\u0000");
        long astral=source.add(AirShape.TEXT,"\uD83D\uDE00");long split=source.add(AirShape.TEXT,"x".repeat(1023)+"\uD83D\uDE00");
        long composed=source.add(AirShape.TEXT,"é");long decomposed=source.add(AirShape.TEXT,"e\u0301");
        long high=source.add(AirShape.TEXT,"a\uD800b");long low=source.add(AirShape.TEXT,"a\uDC00b");
        long end=source.add(AirShape.TEXT,"x".repeat(1023)+"\uD800");
        long textOne=source.add(AirShape.TEXT,"1");long integerOne=source.add(AirShape.INTEGER,"1");
        long same=source.add(AirShape.TEXT,"\uD83D\uDE00");
        long padded=source.add(AirShape.TEXT,"x\u0000");long plain=source.add(AirShape.TEXT,"x");
        try(var snapshot=AirSnapshot.attach(source,1);var keys=new SnapshotIdentityKeys(snapshot,new Store())) {
            eq(0L,keys.atomFact(empty,SnapshotIdentityKeys.AtomFact.CHARACTERS));eq(0L,keys.atomFact(empty,SnapshotIdentityKeys.AtomFact.UNICODE_SCALARS));
            eq(1L,keys.atomFact(nul,SnapshotIdentityKeys.AtomFact.UNICODE_SCALARS));eq(1L,keys.atomFact(astral,SnapshotIdentityKeys.AtomFact.UNICODE_SCALARS));
            eq(1025L,keys.atomFact(split,SnapshotIdentityKeys.AtomFact.CHARACTERS));eq(1024L,keys.atomFact(split,SnapshotIdentityKeys.AtomFact.UNICODE_SCALARS));
            eq(1L,keys.atomFact(composed,SnapshotIdentityKeys.AtomFact.UNICODE_SCALARS));eq(2L,keys.atomFact(decomposed,SnapshotIdentityKeys.AtomFact.UNICODE_SCALARS));
            for(long handle:new long[]{high,low,end})eq(-1L,keys.atomFact(handle,SnapshotIdentityKeys.AtomFact.UNICODE_SCALARS));
            different(keys.atomKey(composed),keys.atomKey(decomposed));different(keys.atomKey(textOne),keys.atomKey(integerOne));different(keys.atomKey(plain),keys.atomKey(padded));
            eq(keys.atomKey(astral),keys.atomKey(same));
        }
    }
    static void canonicalHugeIntegersAndSingleSourceReads() {
        var source=new Source();long idText=source.add(AirShape.TEXT,"publication-\uD83D\uDE00");
        long zero=source.add(AirShape.INTEGER,"0");long positive=source.add(AirShape.INTEGER,"1000");
        long negative=source.add(AirShape.INTEGER,"-5");long huge=source.add(AirShape.INTEGER,"-"+"9".repeat(100_000)+"7");
        var store=new Store();
        try(var snapshot=AirSnapshot.attach(source,1);var keys=new SnapshotIdentityKeys(snapshot,store)) {
            keys.key(3);long characters=source.characters,requests=store.requests;
            eq(13L,keys.atomFact(idText,SnapshotIdentityKeys.AtomFact.UNICODE_SCALARS));
            eq(characters,source.characters);eq(requests,store.requests); // ID interning already computed text properties.
            eq(0L,keys.atomFact(zero,SnapshotIdentityKeys.AtomFact.INTEGER_SIGN));eq(0L,keys.atomFact(zero,SnapshotIdentityKeys.AtomFact.MAGNITUDE_MODULO_EIGHT));
            eq(1L,keys.atomFact(positive,SnapshotIdentityKeys.AtomFact.INTEGER_SIGN));eq(0L,keys.atomFact(positive,SnapshotIdentityKeys.AtomFact.MAGNITUDE_MODULO_EIGHT));
            eq(-1L,keys.atomFact(negative,SnapshotIdentityKeys.AtomFact.INTEGER_SIGN));eq(5L,keys.atomFact(negative,SnapshotIdentityKeys.AtomFact.MAGNITUDE_MODULO_EIGHT));
            eq(100002L,keys.atomFact(huge,SnapshotIdentityKeys.AtomFact.CHARACTERS));eq(100002L,keys.atomFact(huge,SnapshotIdentityKeys.AtomFact.UNICODE_SCALARS));
            eq(-1L,keys.atomFact(huge,SnapshotIdentityKeys.AtomFact.INTEGER_SIGN));eq(5L,keys.atomFact(huge,SnapshotIdentityKeys.AtomFact.MAGNITUDE_MODULO_EIGHT)); // ...997 mod8=5
            characters=source.characters;requests=store.requests;
            for(int n=0;n<128;n++)for(var fact:SnapshotIdentityKeys.AtomFact.values())keys.atomFact(huge,fact);
            eq(characters,source.characters);eq(requests,store.requests);
            eq(100002L,source.reads.get(huge));
        }
        eq(0L,store.claimed);eq(true,store.closed);
        for(String invalid:List.of("","-","+1","01","-0","-01","1a"," 1","1\u0000")) {
            var broken=new Source();long handle=broken.add(AirShape.INTEGER,invalid);var storage=new Store();
            try(var snapshot=AirSnapshot.attach(broken,1);var keys=new SnapshotIdentityKeys(snapshot,storage)) {
                fails(IllegalStateException.class,()->keys.atomKey(handle));
                fails(IllegalStateException.class,()->keys.atomKey(handle));
                eq(AirShape.INTEGER,snapshot.shape(handle));
            }
            eq(0L,storage.claimed);eq(true,storage.closed);
        }
    }
    static void metadataReadFailureAbortsOwnerAndPreservesBorrowedSource() {
        var source=new Source();long handle=source.add(AirShape.TEXT,"a");var store=new Store();
        try(var snapshot=AirSnapshot.attach(source,1);var keys=new SnapshotIdentityKeys(snapshot,store)) {
            keys.atomKey(handle);store.failWord=true;
            eq(store.failure,fails(IllegalStateException.class,()->keys.atomFact(handle,SnapshotIdentityKeys.AtomFact.CHARACTERS)));
            fails(IllegalStateException.class,()->keys.key(3));eq(AirShape.TEXT,snapshot.shape(handle));
        }
        eq(0L,store.claimed);eq(true,store.closed);
        var wrong=new Source();long text=wrong.add(AirShape.TEXT,"5");
        try(var snapshot=AirSnapshot.attach(wrong,1);var keys=new SnapshotIdentityKeys(snapshot,new Store())) {
            fails(IllegalArgumentException.class,()->keys.atomFact(text,SnapshotIdentityKeys.AtomFact.INTEGER_SIGN));
            eq(AirShape.TEXT,snapshot.shape(text));
        }
    }
    static void integerOrderAndBlanknessReuseCanonicalSubtrees() {
        var source=new Source();var handles=new ArrayList<Long>();var expected=new ArrayList<java.math.BigInteger>();
        var random=new Random(924701);
        for(int n=0;n<40;n++) {
            var number=new java.math.BigInteger(512,random);if(n%2==0)number=number.negate();if(n==0)number=java.math.BigInteger.ZERO;
            expected.add(number);handles.add(source.add(AirShape.INTEGER,number.toString()));
        }
        long a=source.add(AirShape.INTEGER,"1"+"0".repeat(32765)+"123"),b=source.add(AirShape.INTEGER,"1"+"0".repeat(32765)+"124");
        long c=source.add(AirShape.INTEGER,"2"+"0".repeat(32765)+"123"),negativeA=source.add(AirShape.INTEGER,"-1"+"0".repeat(32765)+"123"),negativeB=source.add(AirShape.INTEGER,"-1"+"0".repeat(32765)+"124");
        String[] texts={""," \t\n\u2003","\u00A0","\u200B","\uD83D\uDE00","x\uD800","\uD800","x".repeat(1023)+"\uD83D\uDE00"};
        long[] blankness=new long[texts.length];for(int n=0;n<texts.length;n++)blankness[n]=source.add(AirShape.TEXT,texts[n]);
        var store=new Store();
        try(var snapshot=AirSnapshot.attach(source,1);var keys=new SnapshotIdentityKeys(snapshot,store)) {
            for(long handle:handles)keys.atomKey(handle);for(long handle:new long[]{a,b,c,negativeA,negativeB})keys.atomKey(handle);
            for(int n=0;n<texts.length;n++)eq(texts[n].isBlank()?0L:1L,keys.atomFact(blankness[n],SnapshotIdentityKeys.AtomFact.NONBLANK));
            long chars=source.characters,requests=store.requests,words=store.words;
            for(int left=0;left<handles.size();left++)for(int right=0;right<handles.size();right++)
                eq(Integer.signum(expected.get(left).compareTo(expected.get(right))),keys.compareIntegers(handles.get(left),handles.get(right)));
            for(int repeat=0;repeat<128;repeat++) {
                eq(-1,keys.compareIntegers(a,b));eq(-1,keys.compareIntegers(b,c));eq(-1,keys.compareIntegers(negativeB,negativeA));
                eq(-1,keys.compareIntegers(negativeA,a));eq(0,keys.compareIntegers(a,a));
            }
            eq(chars,source.characters);eq(requests,store.requests);
            eq(true,store.words-words<(1600L+128*5)*256); // No leaf-by-leaf replay of huge cached integers.
        }
        eq(0L,store.claimed);
        for(int length:new int[]{16,256,4096,65536,262144}) {
            var input=new Source();long left=input.add(AirShape.INTEGER,"1"+"0".repeat(length-2)+"3"),right=input.add(AirShape.INTEGER,"1"+"0".repeat(length-2)+"4");
            var port=new Store();
            try(var snapshot=AirSnapshot.attach(input,1);var keys=new SnapshotIdentityKeys(snapshot,port)) {
                keys.atomKey(left);keys.atomKey(right);long reads=input.characters,words=port.words,requests=port.requests;
                for(int n=0;n<256;n++)eq(-1,keys.compareIntegers(left,right));
                long limit=16L+12*(64-Long.numberOfLeadingZeros(length/16L+1));
                eq(true,port.words-words<=256*limit);eq(reads,input.characters);eq(requests,port.requests);
                System.out.println("SNAPSHOT_INTEGER_ORDER_COST characters="+length+" queries=256 words="+(port.words-words)+" bound="+(256*limit));
            }
        }
        var broken=new Source();long number=broken.add(AirShape.INTEGER,"123");var port=new Store();
        try(var snapshot=AirSnapshot.attach(broken,1);var keys=new SnapshotIdentityKeys(snapshot,port)) {
            keys.atomKey(number);port.failWord=true;
            eq(port.failure,fails(IllegalStateException.class,()->keys.compareIntegers(number,number)));
            fails(IllegalStateException.class,()->keys.atomKey(number));eq(AirShape.INTEGER,snapshot.shape(number));
        }
        eq(0L,port.claimed);
    }
    private record Atom(AirShape shape,String value) { }
    static final class Source implements AirSnapshot.Source {
        final Map<Long,Atom> atoms=new HashMap<>();final Map<Long,Long> reads=new HashMap<>();
        long issued=1L<<42,first,characters;boolean closed;
        long add(AirShape shape,String value){long handle=++issued;atoms.put(handle,new Atom(shape,value));if(first==0)first=handle;return handle;}
        public AirShape shape(long handle){if(closed)throw new IllegalStateException("closed source");return handle==1?AirShape.PUBLICATION:handle==3?AirShape.IDS_PUBLICATION_ID:atoms.get(handle).shape();}
        public long length(long handle){return handle==1?12:handle==3?1:atoms.get(handle).value().length();}
        public long child(long handle,long index){if(handle!=3||index!=0)throw new AssertionError("unexpected field");return first;}
        public long scalar(long handle){throw new AssertionError("no primitive scalar read");}
        public int characters(long handle,long offset,char[] out,int start,int count){characters+=count;reads.merge(handle,(long)count,Long::sum);atoms.get(handle).value().getChars(Math.toIntExact(offset),Math.toIntExact(offset)+count,out,start);return count;}
        public void close(){closed=true;}
    }
    private record Tuple(long tag,long left,long right,long a,long b,long c,long d) { }
    static final class Store implements SnapshotIdentityKeys.Storage {
        final Map<Long,Long> memo=new HashMap<>();final Map<Tuple,Long> tuples=new HashMap<>();final Map<Long,Tuple> rows=new HashMap<>();
        final IllegalStateException failure=new IllegalStateException("metadata read failure");long issued,requests,claimed,words;boolean closed,failWord;
        public long known(long node){return memo.getOrDefault(node,0L);}public void remember(long node,long key){memo.put(node,key);}
        public long intern(long tag,long l,long r,long a,long b,long c,long d){requests++;var tuple=new Tuple(tag,l,r,a,b,c,d);Long known=tuples.get(tuple);if(known!=null)return known;long key=++issued;tuples.put(tuple,key);rows.put(key,tuple);return key;}
        public long word(long key,int column){words++;if(failWord)throw failure;var row=rows.get(key);return switch(column){case 0->row.tag();case 1->row.left();case 2->row.right();case 3->row.a();case 4->row.b();case 5->row.c();case 6->row.d();default->throw new AssertionError(column);};}
        public AirSnapshotBuilder.Lease claim(long bytes){claimed+=bytes;return ()->claimed-=bytes;}
        public void close(){closed=true;memo.clear();tuples.clear();rows.clear();}
    }
    private static <T extends Throwable>T fails(Class<T> type,Runnable action){try{action.run();}catch(Throwable error){if(type.isInstance(error))return type.cast(error);throw new AssertionError(error);}throw new AssertionError("missing "+type);}
    private static void eq(Object expected,Object actual){if(!Objects.equals(expected,actual))throw new AssertionError("expected="+expected+" actual="+actual);}
    private static void different(long left,long right){if(left==right)throw new AssertionError("different typed atoms merged");}
}
