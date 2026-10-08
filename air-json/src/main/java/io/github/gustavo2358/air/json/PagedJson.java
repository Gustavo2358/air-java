package io.github.gustavo2358.air.json;

import java.io.*;
import java.util.*;
import io.github.gustavo2358.air.model.AirSnapshotBuilder;
import static io.github.gustavo2358.air.json.AirJson.InputStorage.Column.*;

/** Incremental physical JSON staging. Existing binding still constructs a resident Publication. */
final class PagedJson implements AutoCloseable {
    static final int OBJECT=0,ARRAY=1,TEXT=2,TRUE=3,FALSE=4,NULL=5;
    private final InputStream input;
    private final AirJson.Limits limits;
    private AirJson.InputStorage storage;
    private AirSnapshotBuilder.Lease control;
    private byte[] buffer;
    private int at,end,look=-2,low=-1;
    private long bytes,position,nodes,characters,root,characterWord;
    private PagedJson(InputStream input,AirJson.Limits limits,AirJson.InputStorage storage) {
        this.input=input;this.limits=limits;this.storage=Objects.requireNonNull(storage);
    }
    static PagedJson parse(InputStream input,AirJson.Limits limits,AirJson.InputStorage storage)throws IOException {
        var staged=new PagedJson(input,limits,storage);
        try{Objects.requireNonNull(input);Objects.requireNonNull(limits);staged.control=Objects.requireNonNull(storage.claim(9216));staged.buffer=new byte[8192];staged.document();return staged;}
        catch(IOException|RuntimeException|Error failure){try{staged.close();}catch(RuntimeException|Error cleanup){if(failure!=cleanup)failure.addSuppressed(cleanup);}throw failure;}
    }
    private static long base(long token){if(token<=0||token>Long.MAX_VALUE/8)throw new IllegalStateException("JSON token address capacity");return (token-1)*8;}
    long root(){open();return root;}
    Node node(long token){open();if(token>nodes)throw new IllegalStateException("unknown JSON token");base(token);return new Node(this,token);}
    private long get(long token,int field){open();if(token>nodes)throw new IllegalStateException("unknown JSON token");return storage.get(NODES,base(token)+field);}
    private void set(long token,int field,long value){storage.set(NODES,base(token)+field,value);}
    private long token(int kind,long name){long token=Math.incrementExact(nodes);base(token);set(token,0,kind);set(token,1,name);nodes=token;return token;}
    static char character(AirJson.InputStorage storage,long index){if(index<0)throw new IllegalArgumentException("negative JSON character index");return (char)(storage.get(CHARACTERS,index>>>2)>>>((index&3)*16));}
    private void append(char value){int shift=(int)((characters&3)*16);if(shift==0)characterWord=0;characterWord|=(long)value<<shift;if(shift==48)storage.set(CHARACTERS,characters>>>2,characterWord);characters=Math.incrementExact(characters);}
    private void flushCharacters(){if((characters&3)!=0)storage.set(CHARACTERS,characters>>>2,characterWord);}
    private int readByte()throws IOException {
        if(at==end) {
            int size=input.read(buffer,0,(int)Math.min(buffer.length,(long)limits.maximumDocumentBytes()-bytes+1));
            if(size==0){int b=input.read();if(b<0)return -1;buffer[0]=(byte)b;size=1;}
            if(size<0)return -1;bytes=Math.addExact(bytes,size);if(bytes>limits.maximumDocumentBytes())throw Json.resource("$","Document byte limit exceeded");at=0;end=size;
        }
        return buffer[at++]&255;
    }
    private int raw()throws IOException {
        if(low>=0){int c=low;low=-1;return c;}int first=readByte();if(first<128)return first;
        int count,code,min;if(first>=0xc2&&first<=0xdf){count=1;code=first&31;min=0x80;}
        else if(first>=0xe0&&first<=0xef){count=2;code=first&15;min=0x800;}
        else if(first>=0xf0&&first<=0xf4){count=3;code=first&7;min=0x10000;}
        else throw error("Invalid UTF-8");
        for(int i=0;i<count;i++){int b=readByte();if(b<0x80||b>0xbf)throw error("Invalid UTF-8");code=(code<<6)|(b&63);}
        if(code<min||code>0x10ffff||code>=0xd800&&code<=0xdfff)throw error("Invalid UTF-8");
        if(code<=65535)return code;code-=0x10000;low=0xdc00|(code&1023);return 0xd800|(code>>>10);
    }
    private int peek()throws IOException {if(look==-2)look=raw();return look;}
    private int read()throws IOException {int c=peek();look=-2;if(c>=0)position=Math.incrementExact(position);return c;}
    private boolean take(int c)throws IOException {if(peek()!=c)return false;read();return true;}
    private void expect(int c)throws IOException {if(!take(c))throw error("Expected '"+(char)c+"'");}
    private void whitespace()throws IOException {for(int c;(c=peek())==' '||c=='\t'||c=='\r'||c=='\n';)read();}
    private AirJsonException error(String message){return Json.input("$@"+position,message);}
    private long string(long name)throws IOException {
        expect('"');long result=token(TEXT,name),start=characters;boolean high=false;
        while(true) {
            int c=read();if(c<0)throw error("Unterminated string");
            if(c=='"'){if(high)throw error("Isolated high surrogate");flushCharacters();set(result,4,start);set(result,5,characters-start);return result;}
            if(c<32)throw error("Unescaped control character");
            if(c=='\\') {
                c=read();c=switch(c){case '"','\\','/'->c;case 'b'->'\b';case 'f'->'\f';case 'n'->'\n';case 'r'->'\r';case 't'->'\t';case 'u'->{int v=0;for(int i=0;i<4;i++){int h=read(),d=Character.digit(h,16);if(h<0||d<0||h>127)throw error("Invalid Unicode escape");v=(v<<4)|d;}yield v;}default->throw error("Unknown escape");};
            }
            if(high&&!Character.isLowSurrogate((char)c))throw error("Isolated high surrogate");
            if(!high&&Character.isLowSurrogate((char)c))throw error("Isolated low surrogate");
            high=Character.isHighSurrogate((char)c);append((char)c);
        }
    }
    private long value(long name,long depth)throws IOException {
        whitespace();if(depth>limits.maximumDepth())throw Json.resource("$@"+position,"JSON depth limit exceeded");
        int c=peek();if(c=='"')return string(name);
        if(c=='{'||c=='['){read();return token(c=='{'?OBJECT:ARRAY,name);}
        String literal;int kind;
        if(c=='t'){literal="true";kind=TRUE;}else if(c=='f'){literal="false";kind=FALSE;}else if(c=='n'){literal="null";kind=NULL;}else throw error("Expected binding JSON value; numbers must be canonical decimal strings");
        for(int i=0;i<literal.length();i++)if(read()!=literal.charAt(i))throw error("Invalid literal");return token(kind,name);
    }
    private void frame(long depth,long node,long state){long offset=Math.multiplyExact(depth,2);storage.set(FRAMES,offset,node);storage.set(FRAMES,offset+1,state);}
    private void document()throws IOException {
        if(peek()==0xfeff)throw error("BOM forbidden");root=value(0,0);long depth=0;
        if(get(root,0)<=ARRAY){frame(0,root,0);depth=1;}
        while(depth!=0) {
            long offset=Math.multiplyExact(depth-1,2),parent=storage.get(FRAMES,offset),state=storage.get(FRAMES,offset+1);boolean object=get(parent,0)==OBJECT;whitespace();
            if((state==0||state==2)&&take(object?'}':']')){frame(--depth,0,0);continue;}
            if(state==2){expect(',');state=3;whitespace();}
            long name=0;
            if(object){name=string(0);whitespace();expect(':');if(!storage.firstField(parent,name))throw error("Duplicate property");}
            long child=value(name,depth),ordinal=get(parent,3);storage.child(parent,ordinal,child);set(parent,3,Math.incrementExact(ordinal));frame(depth-1,parent,2);
            if(get(child,0)<=ARRAY){frame(depth,child,0);depth=Math.incrementExact(depth);}
        }
        whitespace();if(peek()!=-1)throw error("Trailing content or second document");if(get(root,0)!=OBJECT)throw Json.input("$","Document root must be an object");
    }
    private boolean equalText(long token,String text){long n=get(token,5);if(n!=text.length())return false;long start=get(token,4);for(int i=0;i<text.length();i++)if(character(storage,start+i)!=text.charAt(i))return false;return true;}
    private String text(long token) {
        long length=get(token,5);if(length>Integer.MAX_VALUE)throw Json.limit("$","Resident binding text exceeds int representability");
        // This guards the conversion scratch only. Returned model Strings are the resident route.
        try(var scratch=storage.claim(Math.addExact(128,Math.multiplyExact(length,4)))) {
            Objects.requireNonNull(scratch);char[] value=new char[(int)length];long start=get(token,4),cachedIndex=-1,packed=0;for(int i=0;i<value.length;i++){long at=start+i,index=at>>>2;if(index!=cachedIndex){packed=storage.get(CHARACTERS,index);cachedIndex=index;}value[i]=(char)(packed>>>((at&3)*16));}return new String(value);
        }
    }
    record Node(PagedJson input,long token) implements Json.Value {
        boolean object(){return input.get(token,0)==OBJECT;}boolean array(){return input.get(token,0)==ARRAY;}boolean textValue(){return input.get(token,0)==TEXT;}
        String text(){return input.text(token);}Boolean bool(){return switch((int)input.get(token,0)){case TRUE->true;case FALSE->false;default->null;};}
        List<Json.Value> values() {
            long length=input.get(token,3);if(length>Integer.MAX_VALUE)throw Json.limit("$","Resident binding array exceeds int representability");
            return new AbstractList<>(){public int size(){return (int)length;}public Json.Value get(int index){Objects.checkIndex(index,size());return input.valueNode(input.storage.child(token,index));}};
        }
        Map<String,Json.Value> fields() {
            long length=input.get(token,3);if(length>Integer.MAX_VALUE)throw Json.limit("$","Resident binding object exceeds int representability");
            return new AbstractMap<>() {
                public int size(){return (int)length;}
                public Json.Value get(Object key){if(!(key instanceof String text))return null;for(long i=0;i<length;i++){long child=input.storage.child(token,i);if(input.equalText(input.get(child,1),text))return input.valueNode(child);}return null;}
                public boolean containsKey(Object key){return get(key)!=null;}
                public Set<Entry<String,Json.Value>> entrySet(){return new AbstractSet<>(){public int size(){return (int)length;}public Iterator<Entry<String,Json.Value>> iterator(){return new Iterator<>(){long at;public boolean hasNext(){return at<length;}public Entry<String,Json.Value> next(){if(!hasNext())throw new NoSuchElementException();long child=input.storage.child(token,at++);return new SimpleImmutableEntry<>(input.text(input.get(child,1)),input.valueNode(child));}};}};}
            };
        }
    }
    private Json.Value valueNode(long token){return get(token,0)==NULL?Json.Nil.INSTANCE:node(token);}
    private void open(){if(storage==null)throw new IllegalStateException("paged JSON staging closed");}
    public void close(){var owner=storage;storage=null;var lease=control;control=null;buffer=null;Throwable failure=null;try{if(owner!=null)owner.close();}catch(RuntimeException|Error error){failure=error;}try{if(lease!=null)lease.close();}catch(RuntimeException|Error error){if(failure==null)failure=error;else if(error!=failure)failure.addSuppressed(error);}if(failure instanceof RuntimeException error)throw error;if(failure instanceof Error error)throw error;}
}
