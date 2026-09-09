package io.github.gustavo2358.air.json;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import static io.github.gustavo2358.air.json.AirJsonException.Code.*;

/** Physical rules of pinned binding §§3, 5, 6. No model reflection or runtime type names. */
final class Json {
    private Json() {}
    sealed interface Value permits Obj, Arr, Text, Bool, Nil {}
    record Obj(Map<String, Value> fields) implements Value { Obj { fields = Map.copyOf(fields); } }
    record Arr(List<Value> values) implements Value { Arr { values = List.copyOf(values); } }
    record Text(String value) implements Value {}
    record Bool(boolean value) implements Value {}
    enum Nil implements Value { INSTANCE }

    static AirJsonException input(String path, String message) { return new AirJsonException(INPUT_ERROR, path, message); }
    static AirJsonException limit(String path, String message) { return new AirJsonException(IMPLEMENTATION_LIMIT, path, message); }

    static AirJsonException resource(String path, String message) { return new AirJsonException(RESOURCE_LIMIT, path, message); }

    static Value parse(byte[] bytes, AirJson.Limits limits) {
        if (bytes.length > limits.maximumDocumentBytes()) throw resource("$", "Document byte limit exceeded");
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException error) { throw input("$", "Invalid UTF-8"); }
        if (text.startsWith("\ufeff")) throw input("$", "BOM forbidden");
        var parser = new Parser(text, limits.maximumDepth());
        Value value = parser.value();
        parser.whitespace();
        if (parser.position != text.length()) throw parser.error("Trailing content or second document");
        if (!(value instanceof Obj)) throw input("$", "Document root must be an object");
        return value;
    }

    static void scalars(String value, String path) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i == value.length() || !Character.isLowSurrogate(value.charAt(i)))
                    throw input(path, "Isolated high surrogate");
            } else if (Character.isLowSurrogate(c)) throw input(path, "Isolated low surrogate");
        }
    }

    private static final class Parser {
        private final String text;
        private final int maximumDepth;
        private int position;
        Parser(String text, int maximumDepth) { this.text = text; this.maximumDepth = maximumDepth; }
        AirJsonException error(String detail) { return input("$@" + position, detail); }
        void whitespace() {
            while (position < text.length() && " \t\n\r".indexOf(text.charAt(position)) >= 0) position++;
        }
        boolean take(char c) {
            if (position < text.length() && text.charAt(position) == c) { position++; return true; }
            return false;
        }
        void expect(char c) { if (!take(c)) throw error("Expected '" + c + "'"); }
        private static final class Container {
            final Map<String,Value> fields;
            final List<Value> values;
            String key;
            int state; // 0 first member, 1 awaiting value, 2 delimiter, 3 member after comma
            Container(boolean object) {
                fields=object ? new LinkedHashMap<>() : null;
                values=object ? null : new ArrayList<>();
            }
            void accept(Value value) {
                if(fields!=null) fields.put(key,value); else values.add(value);
                state=2;
            }
            char close() { return fields!=null ? '}' : ']'; }
            Value finish() { return fields!=null ? new Obj(fields) : new Arr(values); }
        }
        Value value() {
            var stack=new ArrayDeque<Container>();
            Value pending=token(stack);
            while(true) {
                if(pending!=null) {
                    if(stack.isEmpty()) return pending;
                    stack.peek().accept(pending); pending=null;
                }
                Container frame=stack.peek(); whitespace();
                if((frame.state==0 || frame.state==2) && take(frame.close())) {
                    pending=frame.finish(); stack.pop(); continue;
                }
                if(frame.state==2) { expect(','); frame.state=3; whitespace(); }
                if(frame.state==0 || frame.state==3) {
                    if(frame.fields!=null) {
                        String key=string(); whitespace(); expect(':');
                        // Check the unescaped name before insertion; null is a real value.
                        if (frame.fields.containsKey(key)) throw error("Duplicate property: " + key);
                        frame.key=key;
                    }
                    frame.state=1;
                }
                pending=token(stack);
            }
        }
        Value token(ArrayDeque<Container> stack) {
            whitespace();
            if(stack.size()>maximumDepth) throw resource("$@"+position,"JSON depth limit exceeded");
            if(position==text.length()) throw error("Expected value");
            return switch(text.charAt(position)) {
                case '{' -> { position++; stack.push(new Container(true)); yield null; }
                case '[' -> { position++; stack.push(new Container(false)); yield null; }
                case '"' -> new Text(string());
                case 't' -> literal("true",new Bool(true));
                case 'f' -> literal("false",new Bool(false));
                case 'n' -> literal("null",Nil.INSTANCE);
                default -> throw error("Expected binding JSON value; numbers must be canonical decimal strings");
            };
        }
        Value literal(String token, Value value) {
            if (!text.startsWith(token, position)) throw error("Invalid literal");
            position += token.length(); return value;
        }
        String string() {
            expect('"'); var result = new StringBuilder();
            while (position < text.length()) {
                char c = text.charAt(position++);
                if (c == '"') {
                    String value = result.toString(); scalars(value, "$@" + position); return value;
                }
                if (c < 0x20) throw error("Unescaped control character");
                if (c != '\\') { result.append(c); continue; }
                if (position == text.length()) throw error("Incomplete escape");
                char escape = text.charAt(position++);
                switch (escape) {
                    case '"', '\\', '/' -> result.append(escape);
                    case 'b' -> result.append('\b'); case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n'); case 'r' -> result.append('\r'); case 't' -> result.append('\t');
                    case 'u' -> {
                        int value = 0;
                        for (int i = 0; i < 4; i++) {
                            if (position == text.length()) throw error("Incomplete Unicode escape");
                            char hex = text.charAt(position++);
                            int digit = "0123456789abcdef".indexOf(hex);
                            if (digit < 0) digit = "0123456789ABCDEF".indexOf(hex);
                            if (digit < 0) throw error("Invalid Unicode escape");
                            value = value * 16 + digit;
                        }
                        result.append((char) value);
                    }
                    default -> throw error("Unknown escape");
                }
            }
            throw error("Unterminated string");
        }
    }

    static Value value(Object value) {
        if (value == null) return Nil.INSTANCE;
        if (value instanceof Value node) return node;
        if (value instanceof String text) return new Text(text);
        if (value instanceof Boolean bool) return new Bool(bool);
        if (value instanceof BigInteger number) return new Text(number.toString());
        throw new IllegalArgumentException("Unmapped wire value");
    }
    static Obj object(Object... pairs) {
        if (pairs.length % 2 != 0) throw new IllegalArgumentException("Key/value pairs required");
        var fields = new LinkedHashMap<String, Value>();
        for (int i = 0; i < pairs.length; i += 2) {
            String key = (String) pairs[i];
            if (fields.putIfAbsent(key, value(pairs[i + 1])) != null) throw new IllegalArgumentException("Duplicate writer property");
        }
        return new Obj(fields);
    }
    static <T> Arr array(List<T> items, Function<T, Value> mapper) {
        return new Arr(items.stream().map(mapper).toList());
    }
    static <T> Value optional(java.util.Optional<T> item, Function<T, Value> mapper) {
        return item.map(mapper).orElse(Nil.INSTANCE);
    }
    static int compareScalars(String a, String b) {
        int ai = 0, bi = 0;
        while (ai < a.length() && bi < b.length()) {
            int ac = a.codePointAt(ai), bc = b.codePointAt(bi);
            if (ac != bc) return Integer.compare(ac, bc);
            ai += Character.charCount(ac); bi += Character.charCount(bc);
        }
        return Integer.compare(a.length() - ai, b.length() - bi);
    }
    static byte[] write(Value value, AirJson.Limits limits) {
        // First pass validates scalars/depth and counts exact escaped UTF-8 bytes.
        var measure=new Writer(limits,null); measure.write(value);
        byte[] bytes=new byte[Math.toIntExact(measure.position)];
        new Writer(limits,bytes).write(value);
        return bytes;
    }
    private static final class Writer {
        private final AirJson.Limits limits;
        private final byte[] output;
        private long position;
        private static final class Frame {
            final Value node;
            final Iterator<?> children;
            boolean first=true;
            Frame(Value node) {
                this.node=node;
                if(node instanceof Obj obj) {
                    var keys=new ArrayList<>(obj.fields().keySet()); keys.sort(Json::compareScalars);
                    children=keys.iterator();
                } else children=((Arr)node).values().iterator();
            }
        }
        Writer(AirJson.Limits limits,byte[] output) { this.limits=limits; this.output=output; }
        void write(Value root) {
            var stack=new ArrayDeque<Frame>();
            node(root,0,stack);
            while(!stack.isEmpty()) {
                Frame frame=stack.peek();
                if(!frame.children.hasNext()) {
                    octet(frame.node instanceof Obj ? '}' : ']'); stack.pop(); continue;
                }
                if(!frame.first) octet(','); frame.first=false;
                Value child;
                if(frame.node instanceof Obj obj) {
                    String key=(String)frame.children.next(); string(key); octet(':'); child=obj.fields().get(key);
                } else child=(Value)frame.children.next();
                node(child,stack.size(),stack);
            }
        }
        void node(Value value,int depth,ArrayDeque<Frame> stack) {
            if(depth>limits.maximumDepth()) throw resource("$","Output depth limit exceeded");
            switch(value) {
                case Obj ignored -> { octet('{'); stack.push(new Frame(value)); }
                case Arr ignored -> { octet('['); stack.push(new Frame(value)); }
                case Text text -> string(text.value());
                case Bool bool -> ascii(bool.value() ? "true" : "false");
                case Nil ignored -> ascii("null");
            }
        }
        void octet(int value) {
            if(position>=limits.maximumDocumentBytes()) throw resource("$","Output byte limit exceeded");
            if(output!=null) output[(int)position]=(byte)value;
            position++;
        }
        void ascii(String text) { for(int i=0;i<text.length();i++) octet(text.charAt(i)); }
        void scalar(int value) {
            if(value<0x80) octet(value);
            else if(value<0x800) { octet(0xc0 | value >> 6); octet(0x80 | value & 63); }
            else if(value<0x10000) {
                octet(0xe0 | value >> 12); octet(0x80 | value >> 6 & 63); octet(0x80 | value & 63);
            } else {
                octet(0xf0 | value >> 18); octet(0x80 | value >> 12 & 63);
                octet(0x80 | value >> 6 & 63); octet(0x80 | value & 63);
            }
        }
        void string(String text) {
            scalars(text,"$"); octet('"');
            for(int i=0;i<text.length();) {
                int c=text.codePointAt(i); i+=Character.charCount(c);
                switch(c) {
                    case '"' -> ascii("\\\""); case '\\' -> ascii("\\\\");
                    case '\b' -> ascii("\\b"); case '\f' -> ascii("\\f");
                    case '\n' -> ascii("\\n"); case '\r' -> ascii("\\r"); case '\t' -> ascii("\\t");
                    default -> {
                        if(c<0x20) { ascii("\\u00"); octet("0123456789abcdef".charAt(c / 16)); octet("0123456789abcdef".charAt(c % 16)); }
                        else scalar(c);
                    }
                }
            }
            octet('"');
        }
    }
}
