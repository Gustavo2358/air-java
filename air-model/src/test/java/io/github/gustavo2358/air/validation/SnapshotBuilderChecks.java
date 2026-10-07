package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Pure producer storage tests. The map-backed test store is not a bounded-resident backend. */
final class SnapshotBuilderChecks {
    private SnapshotBuilderChecks() { }
    static void completeFactsAndTransfer() {
        var f = new Fixtures(); var object = f.object("x", Fixtures.known(Types.Builtin.TEXT)); var operation = f.op("a");
        f.linear(new Operations.Assign(f.header(operation), f.place(operation, "d", object, Operand.Role.VALUE_WRITE),
                f.text(operation, "v", "A\u0000\uD83D\uDE00Z")));
        var storage = new Store(65536); var builder = new AirSnapshotBuilder(storage);
        try (var original = AirSnapshot.fromPublication(f.build())) {
            long root = copy(original, original.root(), null, builder);
            try (var actual = builder.finish(root)) {
                builder.close(); // Ownership was transferred: this must not close actual storage.
                eq(false, storage.closed); compare(original, actual);
                fails(IllegalStateException.class, () -> builder.scalar(AirShape.BOOLEAN, 1));
            }
        }
        eq(true, storage.closed); eq(0L, storage.claimed);
    }
    static void indexedCollectionsAndStreamedScalars() {
        var f = new Fixtures(); f.sequence("start", List.of(), f.halt("stop"));
        var storage = new Store(65536);
        try (var builder = new AirSnapshotBuilder(storage); var original = AirSnapshot.fromPublication(f.build())) {
            long root = copy(original, original.root(), null, builder);
            long[] values = new long[4097]; long array;
            try (var list = builder.list(AirShape.SMALL_INTEGER)) {
                for (int n = 0; n < values.length; n++) { values[n] = builder.scalar(AirShape.SMALL_INTEGER, n - 2048); list.add(values[n]); }
                array = list.finish();
            }
            long integer;
            try (var text = builder.text(AirShape.INTEGER)) {
                text.append(new char[]{'-', '1'}, 0, 2);
                char[] zeros = new char[4096]; java.util.Arrays.fill(zeros, '0');
                text.append(zeros, 0, zeros.length); integer = text.finish();
            }
            long unicode;
            try (var text = builder.text(AirShape.TEXT)) {
                text.append(new char[]{'A', '\uD83D'}, 0, 2);
                text.append(new char[]{'\uDE00', '\u0000', 'Z'}, 0, 3); unicode = text.finish();
            }
            try (var actual = builder.finish(root)) {
                eq(4097L, actual.size(array)); var random = new Random(8087);
                for (int n = 0; n < 512; n++) {
                    int at = random.nextInt(values.length); long before = storage.reads;
                    eq(values[at], actual.element(array, AirShape.SMALL_INTEGER, at));
                    if (storage.reads - before > 90) throw new AssertionError("array lookup scanned prior elements");
                    eq((long) at - 2048, actual.scalar(values[at]));
                }
                eq(4098L, actual.characterCount(integer));
                char[] block = new char[8]; eq(8, actual.readCharacters(integer, 0, block, 0, 8));
                eq("-1000000", new String(block));
                eq(8, actual.readCharacters(integer, 4089, block, 0, 8)); eq("00000000", new String(block));
                eq("A\uD83D\uDE00\u0000Z", characters(actual, unicode));
            }
        }
        eq(0L, storage.claimed); eq(true, storage.closed);
    }
    static void failuresAndLocalShapes() {
        var denied = new Store(1535);
        try (var builder = new AirSnapshotBuilder(denied)) {
            fails(StoreDenied.class, () -> builder.list(AirShape.UNIT));
        }
        eq(0L, denied.claimed); eq(true, denied.closed);
        var storage = new Store(65536);
        try (var builder = new AirSnapshotBuilder(storage)) {
            long one = builder.scalar(AirShape.BOOLEAN, 1);
            fails(IllegalArgumentException.class, () -> builder.record(AirShape.IDS_PUBLICATION_ID, one));
            fails(IllegalArgumentException.class, () -> builder.scalar(AirShape.BOOLEAN, 2));
            fails(IllegalArgumentException.class, () -> builder.scalar(AirShape.INSTRUCTION, 0));
            try (var abandoned = builder.list(AirShape.BOOLEAN)) { abandoned.add(one); }
            fails(IllegalStateException.class, () -> builder.scalar(AirShape.BOOLEAN, 0));
        }
        eq(0L, storage.claimed);
        for (String invalid : new String[]{"", "-", "03", "-0", "+1", "1x", "\uD800"}) {
            var bad = new Store(65536);
            try (var builder = new AirSnapshotBuilder(bad)) {
                try (var text = builder.text(AirShape.INTEGER)) {
                    fails(IllegalArgumentException.class, () -> { char[] chars = invalid.toCharArray(); text.append(chars, 0, chars.length); text.finish(); });
                }
            }
            eq(0L, bad.claimed); eq(true, bad.closed);
        }
        var badUnicode = new Store(65536);
        try (var builder = new AirSnapshotBuilder(badUnicode); var text = builder.text(AirShape.TEXT)) {
            text.append(new char[]{'\uD800'}, 0, 1);
            fails(IllegalArgumentException.class, text::finish);
        }
        eq(0L, badUnicode.claimed);
    }
    static void operationalAbortAndCleanupFailures() {
        var f = new Fixtures(); f.sequence("start", List.of(), f.halt("stop"));
        var denied = new Store(65536);
        try (var builder = new AirSnapshotBuilder(denied); var original = AirSnapshot.fromPublication(f.build())) {
            long root = copy(original, original.root(), null, builder);
            denied.remainingWrites = 1;
            fails(StoreDenied.class, () -> builder.scalar(AirShape.BOOLEAN, 0));
            fails(IllegalStateException.class, () -> builder.finish(root));
        }
        eq(0L, denied.claimed); eq(true, denied.closed);
        var faulty = new Store(65536); var builder = new AirSnapshotBuilder(faulty);
        AirSnapshot snapshot;
        try (var original = AirSnapshot.fromPublication(f.build())) {
            snapshot = builder.finish(copy(original, original.root(), null, builder));
        }
        faulty.failStorageClose = true; faulty.failLeaseClose = true;
        try { snapshot.close(); throw new AssertionError("expected close failure"); }
        catch (StoreDenied exception) { eq(1, exception.getSuppressed().length); }
        snapshot.close(); builder.close(); eq(0L, faulty.claimed); eq(true, faulty.closed);
        fails(IllegalStateException.class, snapshot::root);
    }
    private static long copy(AirSnapshot source, long node, AirShape element, AirSnapshotBuilder target) {
        AirShape shape = source.shape(node);
        return switch (shape.form()) {
            case RECORD -> {
                long[] children = new long[shape.fieldCount()];
                for (int n = 0; n < children.length; n++) children[n] = copy(source, source.field(node, shape, n), shape.field(n).element(), target);
                yield target.record(shape, children);
            }
            case LIST -> {
                try (var list = target.list(element)) {
                    for (long n = 0; n < source.size(node); n++) list.add(copy(source, source.element(node, element, n), null, target));
                    yield list.finish();
                }
            }
            case OPTIONAL -> target.optional(element, source.size(node) == 0 ? 0 : copy(source, source.element(node, element, 0), null, target));
            case TEXT, INTEGER -> {
                try (var text = target.text(shape)) {
                    char[] block = new char[17]; long at = 0;
                    while (at < source.characterCount(node)) { int n = source.readCharacters(node, at, block, 0, block.length); text.append(block, 0, n); at += n; }
                    yield text.finish();
                }
            }
            case BOOLEAN, SMALL_INTEGER, ENUM -> target.scalar(shape, source.scalar(node));
            case UNION -> throw new AssertionError("concrete values required");
        };
    }
    private record Pair(long before, long after, AirShape element) { }
    private static void compare(AirSnapshot before, AirSnapshot after) {
        var pending = new ArrayDeque<Pair>(); pending.add(new Pair(before.root(), after.root(), null));
        while (!pending.isEmpty()) {
            var pair = pending.removeFirst(); AirShape shape = before.shape(pair.before()); eq(shape, after.shape(pair.after()));
            switch (shape.form()) {
                case RECORD -> {
                    for (int n = 0; n < shape.fieldCount(); n++) pending.add(new Pair(before.field(pair.before(), shape, n),
                            after.field(pair.after(), shape, n), shape.field(n).element()));
                }
                case LIST, OPTIONAL -> {
                    eq(before.size(pair.before()), after.size(pair.after()));
                    for (long n = 0; n < before.size(pair.before()); n++) pending.add(new Pair(before.element(pair.before(), pair.element(), n),
                            after.element(pair.after(), pair.element(), n), null));
                }
                case TEXT, INTEGER -> eq(characters(before, pair.before()), characters(after, pair.after()));
                case BOOLEAN, SMALL_INTEGER, ENUM -> eq(before.scalar(pair.before()), after.scalar(pair.after()));
                case UNION -> throw new AssertionError();
            }
        }
    }
    private static String characters(AirSnapshot source, long node) {
        var result = new StringBuilder(); char[] chars = new char[13]; long offset = 0;
        while (offset < source.characterCount(node)) { int n = source.readCharacters(node, offset, chars, 0, chars.length); result.append(chars, 0, n); offset += n; }
        return result.toString();
    }
    private static void eq(Object expected, Object actual) { if (!Objects.equals(expected, actual)) throw new AssertionError("expected " + expected + ", got " + actual); }
    private static void fails(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (Throwable exception) { if (type.isInstance(exception)) return; throw new AssertionError("unexpected exception", exception); }
        throw new AssertionError("expected " + type.getName());
    }
    private static final class StoreDenied extends RuntimeException { private static final long serialVersionUID = 1L; }
    private static final class Store implements AirSnapshotBuilder.Storage {
        private final HashMap<AirSnapshotBuilder.Column, HashMap<Long, Long>> tables = new HashMap<>();
        private final long limit;
        long claimed, reads, remainingWrites = Long.MAX_VALUE;
        boolean closed, failStorageClose, failLeaseClose;
        Store(long limit) { this.limit = limit; }
        @Override public long get(AirSnapshotBuilder.Column column, long index) { reads++; return tables.computeIfAbsent(column, ignored -> new HashMap<>()).getOrDefault(index, 0L); }
        @Override public void set(AirSnapshotBuilder.Column column, long index, long value) {
            if (remainingWrites == 0) throw new StoreDenied(); remainingWrites--;
            tables.computeIfAbsent(column, ignored -> new HashMap<>()).put(index, value);
        }
        @Override public AirSnapshotBuilder.Lease claim(long bytes) {
            if (bytes > limit - claimed) throw new StoreDenied(); claimed += bytes;
            return new AirSnapshotBuilder.Lease() { private boolean released; @Override public void close() { if (!released) {
                released = true; claimed -= bytes; if (failLeaseClose) throw new StoreDenied();
            } } };
        }
        @Override public void close() { closed = true; tables.clear(); if (failStorageClose) throw new StoreDenied(); }
    }
}
