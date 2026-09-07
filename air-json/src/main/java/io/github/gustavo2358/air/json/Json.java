package io.github.gustavo2358.air.json;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    static Value parse(byte[] bytes, AirJson.Limits limits) {
        if (bytes.length > limits.maximumDocumentBytes()) throw limit("$", "Document byte limit exceeded");
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException error) { throw input("$", "Invalid UTF-8"); }
        if (text.startsWith("\ufeff")) throw input("$", "BOM forbidden");
        var parser = new Parser(text, limits.maximumDepth());
        Value value = parser.value(0);
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
        Value value(int depth) {
            whitespace();
            if (depth > maximumDepth) throw limit("$@" + position, "JSON depth limit exceeded");
            if (position == text.length()) throw error("Expected value");
            return switch (text.charAt(position)) {
                case '{' -> object(depth + 1);
                case '[' -> array(depth + 1);
                case '"' -> new Text(string());
                case 't' -> literal("true", new Bool(true));
                case 'f' -> literal("false", new Bool(false));
                case 'n' -> literal("null", Nil.INSTANCE);
                // The binding contains no JSON-number-valued field, including deferred forms.
                default -> throw error("Expected binding JSON value; numbers must be canonical decimal strings");
            };
        }
        Value literal(String token, Value value) {
            if (!text.startsWith(token, position)) throw error("Invalid literal");
            position += token.length(); return value;
        }
        Obj object(int depth) {
            expect('{'); whitespace();
            var fields = new LinkedHashMap<String, Value>();
            if (take('}')) return new Obj(fields);
            do {
                whitespace(); String key = string(); whitespace(); expect(':');
                // Test presence BEFORE inserting, after unescaping: escaped aliases are duplicates.
                if (fields.containsKey(key)) throw error("Duplicate property: " + key);
                fields.put(key, value(depth)); whitespace();
                if (take('}')) return new Obj(fields);
                expect(',');
            } while (true);
        }
        Arr array(int depth) {
            expect('['); whitespace(); var values = new ArrayList<Value>();
            if (take(']')) return new Arr(values);
            do {
                values.add(value(depth)); whitespace();
                if (take(']')) return new Arr(values);
                expect(',');
            } while (true);
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
        var writer = new Writer(limits); writer.write(value, 0);
        byte[] bytes = writer.out.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > limits.maximumDocumentBytes()) throw limit("$", "Output byte limit exceeded");
        return bytes;
    }
    private static final class Writer {
        private final AirJson.Limits limits;
        private final StringBuilder out = new StringBuilder();
        Writer(AirJson.Limits limits) { this.limits = limits; }
        void write(Value value, int depth) {
            if (depth > limits.maximumDepth()) throw limit("$", "Output depth limit exceeded");
            switch (value) {
                case Obj obj -> {
                    out.append('{'); var keys = new ArrayList<>(obj.fields().keySet());
                    keys.sort(Json::compareScalars);
                    boolean first = true;
                    for (String key : keys) {
                        if (!first) out.append(','); first = false;
                        string(key); out.append(':'); write(obj.fields().get(key), depth + 1);
                    }
                    out.append('}');
                }
                case Arr arr -> {
                    out.append('['); boolean first = true;
                    for (Value element : arr.values()) {
                        if (!first) out.append(','); first = false; write(element, depth + 1);
                    }
                    out.append(']');
                }
                case Text text -> string(text.value());
                case Bool bool -> out.append(bool.value() ? "true" : "false");
                case Nil ignored -> out.append("null");
            }
            if (out.length() > limits.maximumDocumentBytes()) throw limit("$", "Output size limit exceeded");
        }
        void string(String text) {
            scalars(text, "$");
            if (text.length() > limits.maximumDocumentBytes() - out.length()) throw limit("$", "Output size limit exceeded");
            out.append('"');
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                switch (c) {
                    case '"' -> out.append("\\\""); case '\\' -> out.append("\\\\");
                    case '\b' -> out.append("\\b"); case '\f' -> out.append("\\f");
                    case '\n' -> out.append("\\n"); case '\r' -> out.append("\\r"); case '\t' -> out.append("\\t");
                    default -> {
                        if (c < 0x20) out.append("\\u00").append("0123456789abcdef".charAt(c / 16))
                                .append("0123456789abcdef".charAt(c % 16));
                        else out.append(c);
                    }
                }
                if (out.length() > limits.maximumDocumentBytes()) throw limit("$", "Output size limit exceeded");
            }
            out.append('"');
        }
    }
}
