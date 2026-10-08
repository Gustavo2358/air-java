package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;

/** Independent typed facts and synthetic 64-bit sources, not a managed decode/validation claim. */
final class PagedAccessChecks {
    private PagedAccessChecks() { }
    static void modelFacts() {
        var f = new Fixtures();
        var object = f.object("target", Fixtures.known(Types.Builtin.TEXT));
        var operation = f.op("move");
        f.linear(new Operations.Assign(f.header(operation),
                f.place(operation, "destination", object, Operand.Role.VALUE_WRITE),
                f.text(operation, "literal", "A\u0000\uD83D\uDE00Z")));
        var publication = f.build();
        try (var snapshot = AirSnapshot.fromPublication(publication)) {
            long root = snapshot.root(); eq(AirShape.PUBLICATION, snapshot.shape(root));
            long units = snapshot.field(root, AirShape.PUBLICATION, 4); eq(1L, snapshot.size(units));
            long unit = snapshot.element(units, AirShape.UNIT, 0);
            try (var cursor = snapshot.elements(units, AirShape.UNIT)) {
                eq(true, cursor.advance()); eq(unit, cursor.value());
                eq(false, cursor.advance()); eq(false, cursor.advance());
            }
            long sequences = snapshot.field(unit, AirShape.UNIT, 5);
            long sequence = snapshot.element(sequences, AirShape.SEQUENCE, 0);
            long instructions = snapshot.field(sequence, AirShape.SEQUENCE, 1);
            long assign = snapshot.element(instructions, AirShape.INSTRUCTION, 0);
            eq(AirShape.OPERATIONS_ASSIGN, snapshot.shape(assign));
            long literal = snapshot.field(assign, AirShape.OPERATIONS_ASSIGN, 2);
            long value = snapshot.field(literal, AirShape.EXPRESSIONS_LITERAL, 1);
            long text = snapshot.field(value, AirShape.VALUES_TEXT_VALUE, 0);
            eq(5L, snapshot.characterCount(text)); eq("A\u0000\uD83D\uDE00Z", characters(snapshot, text));
            long halt = snapshot.field(sequence, AirShape.SEQUENCE, 2);
            long haltKind = snapshot.field(halt, AirShape.OPERATIONS_HALT, 1);
            eq("NORMAL", snapshot.enumName(haltKind));
            long id = snapshot.field(unit, AirShape.UNIT, 0);
            long parent = snapshot.field(id, AirShape.IDS_UNIT_ID, 0);
            long parentText = snapshot.field(parent, AirShape.IDS_PUBLICATION_ID, 0);
            eq("fixture", characters(snapshot, parentText));
            long optional = snapshot.field(unit, AirShape.UNIT, 1); eq(0L, snapshot.size(optional));
            fails(IllegalArgumentException.class, () -> snapshot.field(unit, AirShape.SEQUENCE, 0));
            fails(IndexOutOfBoundsException.class, () -> snapshot.element(units, AirShape.UNIT, 1));
            fails(IllegalArgumentException.class, () -> snapshot.element(units, AirShape.SEQUENCE, 0));
        }
    }

    static void integerFacts() {
        var f = new Fixtures(); f.sequence("start", java.util.List.of(), f.halt("stop"));
        var p = f.build();
        BigInteger huge = BigInteger.TEN.pow(6000).add(BigInteger.valueOf(937));
        var version = new SemanticVersion(huge, BigInteger.ZERO, BigInteger.ONE);
        // A different AIR version remains representable; this access adapter issues no certificate.
        p = new Publication(p.id(), version, p.capabilities(), p.artifacts(), p.units(), p.storage(),
                p.resources(), p.artifactRelations(), p.origins(), p.coverage(), p.uncertainties(), p.premises());
        try (var snapshot = AirSnapshot.fromPublication(p)) {
            long semver = snapshot.field(snapshot.root(), AirShape.PUBLICATION, 1);
            long major = snapshot.field(semver, AirShape.SEMANTIC_VERSION, 0);
            eq(AirShape.INTEGER, snapshot.shape(major));
            eq(huge.toString(), characters(snapshot, major));
            eq(major, snapshot.field(semver, AirShape.SEMANTIC_VERSION, 0));
            fails(IllegalArgumentException.class, () -> snapshot.scalar(major));
        }
    }

    static void longAccessAndLifetime() {
        var source = new HugeSource();
        var snapshot = AirSnapshot.attach(source, 1);
        long list = snapshot.field(1, AirShape.PUBLICATION, 4);
        eq(1L << 42, snapshot.size(list));
        eq(3L, snapshot.element(list, AirShape.UNIT, (1L << 42) - 1));
        eq(1L << 43, snapshot.characterCount(4));
        char[] block = new char[17];
        eq(17, snapshot.readCharacters(4, (1L << 43) - 17, block, 0, 17));
        for (char c : block) eq('x', c);
        eq(0, snapshot.readCharacters(4, 1L << 43, block, 0, 17));
        fails(IndexOutOfBoundsException.class, () -> snapshot.readCharacters(4, Long.MAX_VALUE, block, 0, 1));
        fails(IndexOutOfBoundsException.class, () -> snapshot.readCharacters(4, 0, block, 16, 2));
        snapshot.close(); snapshot.close(); eq(1, source.closed);
        fails(IllegalStateException.class, snapshot::root);
        fails(IllegalStateException.class, () -> snapshot.shape(1));
        var invalid = new HugeSource();
        fails(IllegalArgumentException.class, () -> AirSnapshot.attach(invalid, 4)); eq(1, invalid.closed);
    }

    static void cursorCardinalityAndCleanup() {
        for(long[] pair:new long[][]{{0,1},{1,0},{3,2},{2,3},{Long.MAX_VALUE,0}}) {
            var source=new CardinalitySource(pair[0],pair[1]);
            try(var snapshot=AirSnapshot.attach(source,1);var cursor=snapshot.elements(2,AirShape.UNIT)) {
                long prefix=Math.min(pair[0],pair[1]);
                for(long n=0;n<prefix;n++){eq(true,cursor.advance());eq(3L,cursor.value());}
                fails(IllegalStateException.class,cursor::advance);eq(0,source.active);eq(1,source.cursorClosed);
                eq(pair[0],snapshot.size(2));eq(AirShape.PUBLICATION,snapshot.shape(1));
            }
            eq(1,source.closed);eq(1,source.cursorClosed);
        }
        for(long count:new long[]{0,1,16}) {
            var source=new CardinalitySource(count,count);
            try(var snapshot=AirSnapshot.attach(source,1);var cursor=snapshot.elements(2,AirShape.UNIT)) {
                for(long n=0;n<count;n++)eq(true,cursor.advance());
                eq(false,cursor.advance());eq(false,cursor.advance());eq(0,source.active);
            }
        }
        var huge=new CardinalitySource(Long.MAX_VALUE,Long.MAX_VALUE);
        try(var snapshot=AirSnapshot.attach(huge,1)) {
            try(var cursor=snapshot.elements(2,AirShape.UNIT)){eq(true,cursor.advance());eq(true,cursor.advance());}
            eq(0,huge.active);eq(Long.MAX_VALUE,snapshot.size(2));
        }
        var failing=new CardinalitySource(1,0);failing.failClose=true;
        try(var snapshot=AirSnapshot.attach(failing,1);var cursor=snapshot.elements(2,AirShape.UNIT)) {
            try {cursor.advance();throw new AssertionError("short collection accepted");}
            catch(IllegalStateException failure){eq(1,failure.getSuppressed().length);eq(failing.cleanup,failure.getSuppressed()[0]);}
            eq(0,failing.active);eq(1,failing.cursorClosed);
        }
    }

    static void completeCatalogue() {
        // The Java model is an independent representation authority. Reflection is test-only.
        var seen = new HashSet<Class<?>>(); var pending = new ArrayDeque<Class<?>>(); pending.add(Publication.class);
        int records = 0, enums = 0, fields = 0;
        while (!pending.isEmpty()) {
            Class<?> model = pending.removeFirst(); if (!seen.add(model)) continue;
            var shape = AirShape.valueOf(model.getCanonicalName().replace("io.github.gustavo2358.air.model.", "")
                    .replace('.', '_').replaceAll("(?<=[a-z0-9])(?=[A-Z])", "_").toUpperCase(java.util.Locale.ROOT));
            eq(model.getCanonicalName(), shape.modelName());
            if (model.isRecord()) {
                records++; var components = model.getRecordComponents(); eq(components.length, shape.fieldCount());
                for (int n = 0; n < components.length; n++) {
                    fields++; var actual = shape.field(n); eq(shape, actual.owner()); eq(n, actual.index());
                    eq(components[n].getName(), actual.name());
                    add(components[n].getGenericType(), pending);
                    eq(components[n].getType().getCanonicalName(), actual.value().modelName());
                    if (components[n].getGenericType() instanceof java.lang.reflect.ParameterizedType p)
                        eq(((Class<?>) p.getActualTypeArguments()[0]).getCanonicalName(), actual.element().modelName());
                    else eq(null, actual.element());
                }
            } else if (model.isEnum()) {
                enums++; Object[] values = model.getEnumConstants(); eq(values.length, shape.enumCount());
                for (int n = 0; n < values.length; n++) eq(((Enum<?>) values[n]).name(), shape.enumName(n));
            } else if (model.isSealed()) {
                for (Class<?> child : model.getPermittedSubclasses()) {
                    pending.add(child);
                    var childShape = AirShape.valueOf(child.getCanonicalName().replace("io.github.gustavo2358.air.model.", "")
                            .replace('.', '_').replaceAll("(?<=[a-z0-9])(?=[A-Z])", "_").toUpperCase(java.util.Locale.ROOT));
                    eq(true, shape.accepts(childShape));
                }
            }
        }
        eq(197, records); eq(37, enums); eq(518, fields); eq(274, seen.size());
        eq(false, AirShape.INSTRUCTION.accepts(AirShape.OPERATIONS_HALT));
        eq(false, AirShape.TYPES_KNOWN.accepts(AirShape.TYPES_UNKNOWN_TYPE));
        eq(true, AirShape.OPERAND.accepts(AirShape.EXPRESSIONS_READ));
    }
    private static void add(java.lang.reflect.Type type, ArrayDeque<Class<?>> pending) {
        if (type instanceof Class<?> c) {
            if (c.getPackageName().equals("io.github.gustavo2358.air.model")) pending.add(c);
        } else if (type instanceof java.lang.reflect.ParameterizedType p) {
            for (var argument : p.getActualTypeArguments()) add(argument, pending);
        } else throw new AssertionError("unaccounted model type " + type);
    }
    private static String characters(AirSnapshot snapshot, long node) {
        var result = new StringBuilder(); char[] block = new char[113]; long offset = 0;
        while (offset < snapshot.characterCount(node)) {
            int count = snapshot.readCharacters(node, offset, block, 0, block.length);
            if (count == 0) throw new AssertionError("premature end");
            result.append(block, 0, count); offset += count;
        }
        return result.toString();
    }
    private static void eq(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) throw new AssertionError("expected " + expected + ", got " + actual);
    }
    private static void fails(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (Throwable exception) {
            if (type.isInstance(exception)) return;
            throw new AssertionError("unexpected exception", exception);
        }
        throw new AssertionError("expected " + type.getName());
    }
    private static final class CardinalitySource implements AirSnapshot.Source {
        final long declared,actual;final IllegalArgumentException cleanup=new IllegalArgumentException("controlled cursor cleanup");
        int active,cursorClosed,closed;boolean failClose;
        CardinalitySource(long declared,long actual){this.declared=declared;this.actual=actual;}
        public AirShape shape(long handle){return handle==1?AirShape.PUBLICATION:handle==2?AirShape.LIST:AirShape.UNIT;}
        public long length(long handle){return handle==1?12:handle==2?declared:11;}
        public long child(long handle,long index){throw new AssertionError("cursor must not use indexed reads");}
        public long scalar(long handle){throw new AssertionError();}
        public int characters(long handle,long offset,char[] output,int start,int count){throw new AssertionError();}
        public AirSnapshot.Elements elements(long handle){active++;return new AirSnapshot.Elements(){
            long seen;boolean released;
            public boolean advance(){if(seen==actual)return false;seen++;return true;}
            public long value(){return 3;}
            public void close(){if(released)return;released=true;active--;cursorClosed++;if(failClose)throw cleanup;}
        };}
        public void close(){closed++;}
    }
    private static final class HugeSource implements AirSnapshot.Source {
        int closed;
        @Override public AirShape shape(long handle) {
            return switch ((int) handle) { case 1 -> AirShape.PUBLICATION; case 2 -> AirShape.LIST;
                case 3 -> AirShape.UNIT; case 4 -> AirShape.TEXT; default -> throw new IllegalArgumentException(); };
        }
        @Override public long length(long handle) { return handle == 1 ? 12 : handle == 2 ? 1L << 42 : handle == 4 ? 1L << 43 : 11; }
        @Override public long child(long handle, long index) { return handle == 1 && index == 4 ? 2 : 3; }
        @Override public long scalar(long handle) { throw new IllegalArgumentException(); }
        @Override public int characters(long handle, long offset, char[] output, int start, int count) {
            for (int n = 0; n < count; n++) output[start + n] = 'x'; return count;
        }
        @Override public void close() { closed++; }
    }
}
