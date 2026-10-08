package io.github.gustavo2358.air.json;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.PublicationId;

/** Independent physical JSON and complete existing binding/admission comparisons. */
final class PagedInputChecks {
    private PagedInputChecks() { }
    static void run() {
        try{checked();}catch(IOException failure){throw new AssertionError(failure);}
    }
    private static void checked()throws IOException {
        var codec=new AirJson();
        directSnapshot(codec);
        for(String name:List.of("goback","scalar-assign","regional","local-control")) {
            byte[] bytes=Files.readAllBytes(Path.of("src/test/resources/"+name+".canonical.json"));var expected=codec.decodeCheckedForPartialAnalysis(bytes);var store=new Store();
            var input=new ByteArrayInputStream(bytes){@Override public synchronized int read(byte[] b,int off,int n){return super.read(b,off,Math.min(n,3));}@Override public void close(){throw new AssertionError("caller input closed");}};
            var actual=codec.decodeCheckedForPartialAnalysis(input,store);
            eq(expected.publication(),actual.publication());eq(expected.result(),actual.result());eq(0L,store.claimed);eq(true,store.closed);
        }
        // Property order is arbitrary; duplicate equality is decoded Unicode equality.
        var store=new Store();String raw="{\"late\":[\"\\uD83D\\uDE00\",null,true,false],\"first\":\"é\"}";
        try(var input=PagedJson.parse(new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)),AirJson.Limits.defaults(),store)) {
            var root=input.node(input.root());eq(2,root.fields().size());eq("é",((PagedJson.Node)root.fields().get("first")).text());eq(4,((PagedJson.Node)root.fields().get("late")).values().size());
        }
        eq(0L,store.claimed);
        for(String invalid:List.of("{\"x\":1}","{\"x\":\"\\uD800\"}","{\"x\":null,\"\\u0078\":true}","{\"x\":[] ,}","{}{}","[]","{\"x\":\"a\n\"}")) {
            var port=new Store();fails(AirJsonException.class,()->parse(invalid,port));eq(true,port.closed);eq(0L,port.claimed);
        }
        var io=new IOException("input failure");var port=new Store();
        try{codec.decodeChecked(new InputStream(){public int read()throws IOException{throw io;}},port);throw new AssertionError("missing IO");}
        catch(IOException failure){eq(io,failure);}eq(true,port.closed);eq(0L,port.claimed);
        var zero=new Store();
        try(var input=PagedJson.parse(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)){int zeros=4;@Override public synchronized int read(byte[] b,int off,int n){return zeros-->0?0:super.read(b,off,n);}},AirJson.Limits.defaults(),zero)){eq(true,input.node(input.root()).object());}
        eq(0L,zero.claimed);
        for(byte[] invalid:new byte[][]{{(byte)0xc0,(byte)0xaf},{'"',(byte)0xed,(byte)0xa0,(byte)0x80,'"'},{'{','"',(byte)0xf4,(byte)0x90,(byte)0x80,(byte)0x80,'"',':','n','u','l','l','}'}}) {
            var port3=new Store();try{PagedJson.parse(new ByteArrayInputStream(invalid),AirJson.Limits.defaults(),port3);throw new AssertionError("invalid UTF8 accepted");}catch(AirJsonException failure){eq(AirJsonException.Code.INPUT_ERROR,failure.code());}eq(0L,port3.claimed);eq(true,port3.closed);
        }
        String deep="{\"x\":"+"[".repeat(20000)+"null"+"]".repeat(20000)+"}";var deepPort=new Store();parse(deep,deepPort);eq(0L,deepPort.claimed);
        for(AirJson.Limits limits:List.of(new AirJson.Limits(1,100),new AirJson.Limits(1000,1))) {
            var port3=new Store();try{PagedJson.parse(new ByteArrayInputStream("{\"x\":[[null]]}".getBytes(StandardCharsets.UTF_8)),limits,port3);throw new AssertionError("ignored operational bound");}catch(AirJsonException failure){eq(AirJsonException.Code.RESOURCE_LIMIT,failure.code());}eq(true,port3.closed);eq(0L,port3.claimed);
        }
        String physical="{\"a\":[\"é\"],\"b\":true}";var measured=new Store();parse(physical,measured);
        for(long at=0;at<measured.requests;at++) {var port3=new Store();port3.remaining=at;eq(port3.failure,fails(IllegalStateException.class,()->parse(physical,port3)));eq(true,port3.closed);eq(0L,port3.claimed);}
        for(int n:new int[]{16,64,256,1024,4096}) {
            var rawArray=new StringBuilder("{\"items\":[");for(int i=0;i<n;i++){if(i!=0)rawArray.append(',');rawArray.append("\"x\"");}rawArray.append("]}");var port2=new Store();
            try(var input=PagedJson.parse(new ByteArrayInputStream(rawArray.toString().getBytes(StandardCharsets.UTF_8)),AirJson.Limits.defaults(),port2)){var list=((PagedJson.Node)input.node(input.root()).fields().get("items")).values();eq(n,list.size());for(int at=0;at<n;at++)eq("x",((PagedJson.Node)list.get(at)).text());eq((long)n,port2.childReads-1);}
            eq(0L,port2.claimed);
        }
    }
    private static void directSnapshot(AirJson codec)throws IOException {
        var id=new PublicationId("empty-😀");
        var publication=new Publication(id,SemanticVersion.AIR_2_0_0,new Capabilities.Manifest(List.of(),List.of()),
                List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),
                new Evidence.Coverage(Evidence.InventoryStatus.COMPLETE,new Scopes.PublicationScope(id),List.of(),List.of()),List.of(),List.of());
        String canonical=new String(codec.encode(publication),StandardCharsets.UTF_8);
        int publicationAt=canonical.indexOf("\"publication\":")+14;
        String raw="{\"publication\":"+canonical.substring(publicationAt,canonical.length()-1)
                +",\"bindingVersion\":\"1.0.0\",\"binding\":\"analysis-ir-json\",\"airVersion\":\"2.0.0\"}";
        var inputStore=new Store();var snapshotStore=new SnapshotStore();
        try(var snapshot=codec.decodeSnapshot(new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)),inputStore,snapshotStore)) {
            long root=snapshot.root();eq(AirShape.PUBLICATION,snapshot.shape(root));
            long actualId=snapshot.field(root,AirShape.PUBLICATION,0);
            eq("empty-😀",characters(snapshot,snapshot.field(actualId,AirShape.IDS_PUBLICATION_ID,0)));
            long version=snapshot.field(root,AirShape.PUBLICATION,1);
            eq("2",characters(snapshot,snapshot.field(version,AirShape.SEMANTIC_VERSION,0)));
            eq(0L,snapshot.size(snapshot.field(root,AirShape.PUBLICATION,3)));
            eq(0L,snapshot.size(snapshot.field(root,AirShape.PUBLICATION,4)));
            long coverage=snapshot.field(root,AirShape.PUBLICATION,9);
            eq("COMPLETE",snapshot.enumName(snapshot.field(coverage,AirShape.EVIDENCE_COVERAGE,0)));
            eq(true,inputStore.closed);eq(true,snapshotStore.frozen);eq(false,snapshotStore.closed);
        }
        eq(true,snapshotStore.closed);eq(0L,snapshotStore.claimed);

        byte[] goback=Files.readAllBytes(Path.of("src/test/resources/goback.canonical.json"));
        var gobackInput=new Store();var gobackOutput=new SnapshotStore();
        try(var expected=AirSnapshot.fromPublication(codec.decode(goback));
            var actual=codec.decodeSnapshot(new ByteArrayInputStream(goback),gobackInput,gobackOutput)) {
            compare(expected,expected.root(),actual,actual.root(),null);
            eq(true,gobackInput.closed);eq(true,gobackOutput.frozen);eq(false,gobackOutput.closed);
        }
        eq(true,gobackOutput.closed);eq(0L,gobackOutput.claimed);

        var unsupportedInput=new Store();var unsupportedOutput=new SnapshotStore();
        var unsupported=fails(AirJsonException.class,()->decodeSnapshot(codec,raw.replace("\"storage\":[]","\"storage\":[null]"),unsupportedInput,unsupportedOutput));
        eq(AirJsonException.Code.IMPLEMENTATION_LIMIT,unsupported.code());eq(true,unsupportedInput.closed);eq(true,unsupportedOutput.closed);eq(0L,unsupportedOutput.claimed);

        var deniedInput=new Store();var deniedOutput=new SnapshotStore();deniedOutput.deny=true;
        eq(deniedOutput.failure,fails(IllegalStateException.class,()->decodeSnapshot(codec,raw,deniedInput,deniedOutput)));
        eq(true,deniedInput.closed);eq(true,deniedOutput.closed);eq(0L,deniedOutput.claimed);
    }
    private static void decodeSnapshot(AirJson codec,String raw,Store input,SnapshotStore output) {
        try(var snapshot=codec.decodeSnapshot(new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)),input,output)) { snapshot.root(); }
        catch(IOException failure){throw new AssertionError(failure);}
    }
    private static String characters(AirSnapshot snapshot,long node) {
        char[] value=new char[Math.toIntExact(snapshot.characterCount(node))];snapshot.readCharacters(node,0,value,0,value.length);return new String(value);
    }
    private static void compare(AirSnapshot expected,long left,AirSnapshot actual,long right,AirShape element) {
        AirShape shape=expected.shape(left);eq(shape,actual.shape(right));
        switch(shape.form()) {
            case TEXT,INTEGER->eq(characters(expected,left),characters(actual,right));
            case BOOLEAN,SMALL_INTEGER,ENUM->eq(expected.scalar(left),actual.scalar(right));
            case RECORD->{
                for(int field=0;field<shape.fieldCount();field++) {
                    var slot=shape.field(field);
                    compare(expected,expected.field(left,shape,field),actual,actual.field(right,shape,field),slot.element());
                }
            }
            case LIST,OPTIONAL->{
                eq(expected.size(left),actual.size(right));
                for(long at=0;at<expected.size(left);at++)
                    compare(expected,expected.element(left,element,at),actual,actual.element(right,element,at),null);
            }
            case UNION->throw new AssertionError("concrete snapshot exposed union");
        }
    }
    private static void parse(String raw,Store port){try(var input=PagedJson.parse(new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)),AirJson.Limits.defaults(),port)){input.root();}catch(IOException failure){throw new AssertionError(failure);}}
    static final class Store implements AirJson.InputStorage {
        private record Address(Column column,long index) { }
        private record Child(long parent,long ordinal) { }
        final Map<Address,Long> words=new HashMap<>();final Map<Child,Long> children=new HashMap<>();final Map<Long,Set<String>> names=new HashMap<>();long claimed,childReads,requests,remaining=Long.MAX_VALUE;boolean closed;final IllegalStateException failure=new IllegalStateException("staging port failure");
        private void work(){requests++;if(remaining--==0)throw failure;}
        public long get(Column column,long index){work();return words.getOrDefault(new Address(column,index),0L);}public void set(Column column,long index,long value){work();words.put(new Address(column,index),value);}
        public void child(long parent,long ordinal,long child){work();children.put(new Child(parent,ordinal),child);}public long child(long parent,long ordinal){work();childReads++;return children.getOrDefault(new Child(parent,ordinal),0L);}
        public boolean firstField(long parent,long name){work();long base=(name-1)*8,start=get(Column.NODES,base+4),length=get(Column.NODES,base+5);var key=new StringBuilder();for(long n=0;n<length;n++)key.append(PagedJson.character(this,start+n));return names.computeIfAbsent(parent,k->new HashSet<>()).add(key.toString());}
        public AirSnapshotBuilder.Lease claim(long bytes){work();claimed+=bytes;return ()->claimed-=bytes;}
        public void close(){closed=true;words.clear();children.clear();names.clear();}
    }
    static final class SnapshotStore implements AirSnapshotBuilder.Storage {
        private record Address(AirSnapshotBuilder.Column column,long index) { }
        final Map<Address,Long> words=new HashMap<>();long claimed;boolean frozen,closed,deny;
        final IllegalStateException failure=new IllegalStateException("snapshot storage denied");
        public long get(AirSnapshotBuilder.Column column,long index){if(closed)throw new IllegalStateException("closed");return words.getOrDefault(new Address(column,index),0L);}
        public void set(AirSnapshotBuilder.Column column,long index,long value){if(closed||frozen)throw new IllegalStateException("not writable");words.put(new Address(column,index),value);}
        public AirSnapshotBuilder.Lease claim(long bytes){if(deny)throw failure;claimed+=bytes;return lease(bytes);}
        public AirSnapshotBuilder.Lease readLease(long bytes){if(deny)throw failure;claimed+=bytes;return lease(bytes);}
        private AirSnapshotBuilder.Lease lease(long bytes){return new AirSnapshotBuilder.Lease(){boolean released;public void close(){if(!released){released=true;claimed-=bytes;}}};}
        public void freeze(){if(closed)throw new IllegalStateException("closed");frozen=true;}
        public void close(){closed=true;words.clear();}
    }
    private static <T extends Throwable>T fails(Class<T> type,Runnable action){try{action.run();}catch(Throwable error){if(type.isInstance(error))return type.cast(error);throw new AssertionError(error);}throw new AssertionError("missing "+type);}
    private static void eq(Object expected,Object actual){if(!Objects.equals(expected,actual))throw new AssertionError("expected="+expected+" actual="+actual);}
}
