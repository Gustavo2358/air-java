package io.github.gustavo2358.air.json;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Validates the entire physical document before binding, retaining offsets rather than a JSON DOM.
 * Strings are decoded only when the explicit binding asks for them. Each decode owns its tape and
 * field-name table; block readers share these immutable structures after parsing has completed.
 */
final class Utf8Input {
    private static final int OBJECT=0, ARRAY=1, TEXT=2, ESCAPED=3, TRUE=4, FALSE=5, NULL=6;
    private static final int SHIFT=14, MASK=(1<<SHIFT)-1;
    private final byte[] bytes;
    // Four ints per token: kind/name, byte start, byte end (or child count), subtree end.
    private final List<int[]> chunks=new ArrayList<>();
    private final List<String> names=new ArrayList<>();
    private final Map<String,Integer> symbols=new HashMap<>();
    private final int[] keyCache=new int[1024], keyStarts=new int[256], keyEnds=new int[256];
    private int count, position;

    private Utf8Input(byte[] bytes) { this.bytes=bytes; }
    static Json.Value parse(byte[] bytes,AirJson.Limits limits) {
        if(bytes.length>limits.maximumDocumentBytes())throw Json.resource("$","Document byte limit exceeded");
        try {
            var input=new Utf8Input(bytes);
            input.document(limits.maximumDepth());
            return input.node(0);
        } catch(Invalid | AirJsonException invalid) {
            // The reference parser owns diagnostics, including UTF-16 offsets and precedence when
            // several errors coexist. Invalid inputs never enter the typed/parallel binding path.
            return Json.parse(bytes,limits);
        }
    }
    private static final class Invalid extends RuntimeException {
        private static final long serialVersionUID=1L;
        Invalid() { super(null,null,false,false); }
    }
    private static void require(boolean condition) { if(!condition)throw new Invalid(); }
    private int get(int token,int field) { return chunks.get(token>>>SHIFT)[((token&MASK)<<2)+field]; }
    private void set(int token,int field,int value) { chunks.get(token>>>SHIFT)[((token&MASK)<<2)+field]=value; }
    private int kind(int token) { return get(token,0)&7; }
    private int name(int token) { return (get(token,0)>>>3)-1; }
    private int next(int token) { return get(token,3); }
    private int add(int type,int name,int start,int end) {
        int token=count++;
        if((token&MASK)==0)chunks.add(new int[(1<<SHIFT)*4]);
        set(token,0,((name+1)<<3)|type);set(token,1,start);set(token,2,end);set(token,3,count);
        return token;
    }
    private Json.Value node(int token) { return kind(token)==NULL?Json.Nil.INSTANCE:new Node(this,token); }
    record Node(Utf8Input input,int token) implements Json.Value {
        boolean object() { return input.kind(token)==OBJECT; }
        boolean array() { return input.kind(token)==ARRAY; }
        String text() { int k=input.kind(token);return k==TEXT||k==ESCAPED?input.stringValue(token):null; }
        Boolean bool() { return switch(input.kind(token)){case TRUE->true;case FALSE->false;default->null;}; }
        Map<String,Json.Value> fields() {
            return new AbstractMap<>() {
                @Override public int size() { return input.get(token,2); }
                @Override public Json.Value get(Object key) {
                    for(int t=token+1,end=input.next(token);t<end;t=input.next(t))
                        if(input.names.get(input.name(t)).equals(key))return input.node(t);
                    return null;
                }
                @Override public boolean containsKey(Object key) { return get(key)!=null; }
                @Override public Set<Entry<String,Json.Value>> entrySet() {
                    // Only error reporting enumerates object fields. Match Map.copyOf's order in
                    // the reference reader, including unknown-field precedence within this JVM.
                    var fields=new LinkedHashMap<String,Json.Value>();
                    for(int t=token+1,end=input.next(token);t<end;t=input.next(t))
                        fields.put(input.names.get(input.name(t)),input.node(t));
                    return Map.copyOf(fields).entrySet();
                }
            };
        }
        List<Json.Value> values() {
            int[] tokens=new int[input.get(token,2)];
            for(int t=token+1,i=0,end=input.next(token);t<end;t=input.next(t))tokens[i++]=t;
            return new AbstractList<>() {
                @Override public int size() { return tokens.length; }
                @Override public Json.Value get(int index) { return input.node(tokens[index]); }
            };
        }
    }
    private static final class Frame {
        final int token;
        final boolean object;
        int[] keys;
        Set<Integer> manyKeys;
        int children,state;
        Frame(int token,boolean object) { this.token=token;this.object=object;if(object)keys=new int[8]; }
        void key(int key) {
            if(children==32) {
                manyKeys=new HashSet<>();for(int existing:keys)manyKeys.add(existing);
            }
            if(manyKeys!=null) { require(manyKeys.add(key));return; }
            for(int i=0;i<children;i++)require(keys[i]!=key);
            if(children==keys.length)keys=Arrays.copyOf(keys,keys.length*2);
            keys[children]=key;
        }
    }
    private void whitespace() {
        while(position<bytes.length) {
            byte b=bytes[position];if(b!=' '&&b!='\t'&&b!='\n'&&b!='\r')return;position++;
        }
    }
    private boolean take(int c) { if(position<bytes.length&&(bytes[position]&255)==c){position++;return true;}return false; }
    private void document(int depth) {
        var stack=new ArrayDeque<Frame>();
        token(-1,stack,depth);
        while(!stack.isEmpty()) {
            Frame frame=stack.peek();whitespace();
            if((frame.state==0||frame.state==2)&&take(frame.object?'}':']')) {
                set(frame.token,2,frame.children);set(frame.token,3,count);stack.pop();continue;
            }
            if(frame.state==2) { require(take(','));frame.state=3;whitespace(); }
            int name=-1;
            if(frame.object) {
                long span=string();int start=(int)(span>>>32),end=(int)span;
                name=key(start,end);whitespace();require(take(':'));frame.key(name);
            }
            frame.children++;frame.state=2;token(name,stack,depth);
        }
        whitespace();require(position==bytes.length&&kind(0)==OBJECT);
    }
    private void token(int name,ArrayDeque<Frame> stack,int depth) {
        whitespace();require(stack.size()<=depth&&position<bytes.length);
        int c=bytes[position]&255;
        if(c=='{'||c=='[') { int t=add(c=='{'?OBJECT:ARRAY,name,position++,0);stack.push(new Frame(t,c=='{')); }
        else if(c=='"') { long span=string();int start=(int)(span>>>32),end=(int)span;add(start<0?ESCAPED:TEXT,name,start&Integer.MAX_VALUE,end); }
        else if(c=='t')literal("true",TRUE,name);
        else if(c=='f')literal("false",FALSE,name);
        else if(c=='n')literal("null",NULL,name);
        else throw new Invalid();
    }
    private void literal(String value,int kind,int name) {
        int start=position;require(bytes.length-position>=value.length());
        for(int i=0;i<value.length();i++)require(bytes[position++]==value.charAt(i));
        add(kind,name,start,position);
    }
    private long string() {
        require(take('"'));int start=position;boolean escaped=false;
        while(position<bytes.length) {
            int c=bytes[position++]&255;
            if(c=='"') {
                int end=position-1;
                if(escaped)Json.scalars(unescape(start,end),"$");
                return ((long)(escaped?start|Integer.MIN_VALUE:start)<<32)|(end&0xffffffffL);
            }
            require(c>=32);
            if(c=='\\') {
                escaped=true;require(position<bytes.length);int e=bytes[position++]&255;
                if(e=='u') { for(int i=0;i<4;i++){require(position<bytes.length);require(hex(bytes[position++]&255)>=0);} }
                else require(e=='"'||e=='\\'||e=='/'||e=='b'||e=='f'||e=='n'||e=='r'||e=='t');
            } else if(c>=128) {
                // Strict UTF-8: reject overlong forms, surrogate scalars and values above U+10FFFF.
                int n=c>=0xc2&&c<=0xdf?1:c>=0xe0&&c<=0xef?2:c>=0xf0&&c<=0xf4?3:-1;
                require(n>0&&bytes.length-position>=n);int second=bytes[position]&255;
                require((c!=0xe0||second>=0xa0)&&(c!=0xed||second<0xa0)
                        &&(c!=0xf0||second>=0x90)&&(c!=0xf4||second<0x90));
                for(int i=0;i<n;i++)require((bytes[position++]&0xc0)==0x80);
            }
        }
        throw new Invalid();
    }
    private int key(int start,int end) {
        if(start>=0) {
            int hash=0;for(int i=start;i<end;i++)hash=31*hash+(bytes[i]&255);
            int slot=hash&(keyCache.length-1);
            while(keyCache[slot]!=0) {
                int id=keyCache[slot]-1;
                if(end-start==keyEnds[id]-keyStarts[id]
                        &&Arrays.equals(bytes,start,end,bytes,keyStarts[id],keyEnds[id]))return id;
                slot=(slot+1)&(keyCache.length-1);
            }
            String key=new String(bytes,start,end-start,StandardCharsets.UTF_8);
            int id=symbol(key);
            if(id<keyStarts.length) { keyStarts[id]=start;keyEnds[id]=end;keyCache[slot]=id+1; }
            return id;
        }
        return symbol(unescape(start&Integer.MAX_VALUE,end));
    }
    private int symbol(String key) {
        Integer known=symbols.get(key);if(known!=null)return known;
        int id=names.size();require(id<Integer.MAX_VALUE>>>3);
        // Only the small field vocabulary is shared, never arbitrary AIR text or IDs.
        if(id<256&&key.length()<64)key=key.intern();
        names.add(key);symbols.put(key,id);return id;
    }
    private String stringValue(int token) {
        int start=get(token,1),end=get(token,2);
        return kind(token)==ESCAPED?unescape(start,end):new String(bytes,start,end-start,StandardCharsets.UTF_8);
    }
    private static int hex(int c) { return c>='0'&&c<='9'?c-'0':c>='a'&&c<='f'?c-'a'+10:c>='A'&&c<='F'?c-'A'+10:-1; }
    private String unescape(int start,int end) {
        var result=new StringBuilder();int run=start;
        for(int i=start;i<end;i++)if(bytes[i]=='\\') {
            result.append(new String(bytes,run,i-run,StandardCharsets.UTF_8));
            int e=bytes[++i]&255;
            if(e=='u') { int value=0;for(int j=0;j<4;j++)value=(value<<4)|hex(bytes[++i]&255);result.append((char)value); }
            else result.append(switch(e){case 'b'->'\b';case 'f'->'\f';case 'n'->'\n';case 'r'->'\r';case 't'->'\t';default->(char)e;});
            run=i+1;
        }
        return result.append(new String(bytes,run,end-run,StandardCharsets.UTF_8)).toString();
    }
}
