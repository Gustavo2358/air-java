package io.github.gustavo2358.air.model;

import java.util.Objects;

/**
 * Official typed read access without hidden Publication materialization. Handles are source-local
 * node addresses, never external AIR IDs: identity comparison retains complete typed namespaces.
 * This access object is not a validation certificate. Structural/capability validation remains a
 * separate obligation, including when attaching an externally stored source.
 */
public final class AirSnapshot implements AutoCloseable {
    /**
     * Ownership transfers to attach. A source is frozen, repeatable, complete, and owns its storage
     * lease until close. Lengths/indices are 64-bit. Shape/child/scalar/text expose typed AIR values,
     * not a guessed wire tree. UTF-16 character blocks preserve exact text; INTEGER uses canonical
     * signed decimal characters without requiring a resident BigInteger. Operational storage
     * failures propagate; implementations must not turn them into missing fields or unknown facts.
     * characters must fill exactly the requested in-range block. File/I/O remains outside model.
     */
    public interface Source extends AutoCloseable {
        AirShape shape(long handle);
        long length(long handle);
        long child(long handle, long index);
        long scalar(long handle);
        int characters(long handle, long offset, char[] output, int start, int count);
        /** Indexed convenience for resident/custom sources; paged sources override with a linear walk. */
        default Elements elements(long handle) { return new IndexedElements(this, handle); }
        @Override void close();
    }
    public interface Elements extends AutoCloseable {
        boolean advance();
        long value();
        @Override void close();
    }
    private Source source;
    private final long root;
    private Cursor cursors;

    private AirSnapshot(Source source, long root) { this.source = source; this.root = root; }

    /** Attach frozen typed storage; no admission status or content validity is inferred. */
    public static AirSnapshot attach(Source source, long root) {
        Objects.requireNonNull(source);
        var snapshot = new AirSnapshot(source, root);
        try {
            if (snapshot.shape(root) != AirShape.PUBLICATION)
                throw new IllegalArgumentException("snapshot root must be an AIR Publication");
            return snapshot;
        } catch (RuntimeException | Error exception) {
            try { snapshot.close(); } catch (RuntimeException cleanup) { exception.addSuppressed(cleanup); }
            throw exception;
        }
    }
    /**
     * Explicit caller-owned in-memory route. It retains Publication and lazy identity indexes;
     * it has no managed heap guarantee and is not the managed large-input decoder.
     */
    public static AirSnapshot fromPublication(Publication publication) {
        var source = new PublicationSource(Objects.requireNonNull(publication));
        return attach(source, 1);
    }

    public synchronized long root() { open(); return root; }

    public synchronized AirShape shape(long handle) {
        open();
        if (handle <= 0) throw new IllegalArgumentException("positive source-local handle required");
        AirShape shape = source.shape(handle);
        if (shape == null || shape.form() == AirShape.Form.UNION)
            throw new IllegalStateException("source must expose a concrete AIR value shape");
        return shape;
    }

    /** Exact typed field by its official fixed model ordinal, not a wire/source-language name. */
    public synchronized long field(long record, AirShape expected, int ordinal) {
        Objects.requireNonNull(expected);
        if (expected.form() != AirShape.Form.RECORD || shape(record) != expected)
            throw new IllegalArgumentException("wrong AIR record shape");
        var slot = expected.field(ordinal);
        if (length(record) != expected.fieldCount()) throw new IllegalStateException("incomplete AIR record storage");
        long child = source.child(record, ordinal);
        if (!slot.value().accepts(shape(child))) throw new IllegalStateException("stored AIR field type mismatch");
        return child;
    }

    /** List/Optional cardinality, including zero. No conversion to java.util.List or int. */
    public synchronized long size(long container) {
        AirShape shape = shape(container);
        if (shape != AirShape.LIST && shape != AirShape.OPTIONAL)
            throw new IllegalArgumentException("AIR list or optional required");
        long length = length(container);
        if (shape == AirShape.OPTIONAL && length > 1) throw new IllegalStateException("invalid optional storage");
        return length;
    }

    public synchronized long element(long container, AirShape expected, long index) {
        Objects.requireNonNull(expected);
        long count = size(container);
        if (index < 0 || index >= count) throw new IndexOutOfBoundsException("AIR element " + index);
        long child = source.child(container, index);
        if (!expected.accepts(shape(child))) throw new IllegalArgumentException("wrong AIR element shape");
        return child;
    }

    /** Sequential primitive cursor, owned by this snapshot; no row objects or collection materialization. */
    public synchronized Cursor elements(long container, AirShape expected) {
        Objects.requireNonNull(expected); long length = size(container);
        Elements raw = Objects.requireNonNull(source.elements(container));
        try {
            var cursor = new Cursor(this, raw, expected, length);
            cursor.next = cursors; if (cursors != null) cursors.previous = cursor; cursors = cursor;
            return cursor;
        } catch (RuntimeException | Error exception) {
            try { raw.close(); } catch (RuntimeException | Error cleanup) { if (cleanup != exception) exception.addSuppressed(cleanup); }
            throw exception;
        }
    }

    public static final class Cursor implements Elements {
        private volatile AirSnapshot owner;
        private Elements raw;
        private final AirShape expected;
        private final long length;
        private long visited;
        private Cursor previous, next;
        private long current;
        private boolean exhausted;
        private Cursor(AirSnapshot owner, Elements raw, AirShape expected, long length) {
            this.owner = owner; this.raw = raw; this.expected = expected; this.length = length;
        }
        @Override public boolean advance() {
            AirSnapshot snapshot = owner;
            if (snapshot == null) {
                if (exhausted) return false;
                throw new IllegalStateException("AIR cursor is closed");
            }
            synchronized (snapshot) {
                if (owner == null) throw new IllegalStateException("AIR cursor is closed");
                snapshot.open();
                try {
                    if (!raw.advance()) {
                        if (visited != length) throw new IllegalStateException("premature end of frozen AIR collection storage");
                        exhausted = true; close(); return false;
                    }
                    if (visited == length) throw new IllegalStateException("frozen AIR collection exceeds declared cardinality");
                    long value = raw.value();
                    if (!expected.accepts(snapshot.shape(value))) throw new IllegalArgumentException("wrong AIR cursor element type");
                    current = value; visited++; return true;
                } catch (RuntimeException | Error exception) {
                    try { close(); } catch (RuntimeException | Error cleanup) { if (cleanup != exception) exception.addSuppressed(cleanup); }
                    throw exception;
                }
            }
        }
        @Override public long value() {
            AirSnapshot snapshot = owner;
            if (snapshot == null) {
                if (exhausted) throw new java.util.NoSuchElementException("AIR cursor has no current element");
                throw new IllegalStateException("AIR cursor is closed");
            }
            synchronized (snapshot) {
                if (owner == null) throw new IllegalStateException("AIR cursor is closed");
                snapshot.open();
                if (current == 0) throw new java.util.NoSuchElementException("AIR cursor has no current element");
                return current;
            }
        }
        @Override public void close() {
            AirSnapshot snapshot = owner; if (snapshot == null) return;
            synchronized (snapshot) {
                if (owner == null) return;
                if (previous == null) snapshot.cursors = next; else previous.next = next;
                if (next != null) next.previous = previous;
                Elements iterator = raw; raw = null; owner = null; previous = null; next = null; current = 0;
                iterator.close();
            }
        }
    }

    private static final class IndexedElements implements Elements {
        private Source source;
        private final long container, length;
        private long index, current;
        private IndexedElements(Source source, long container) { this.source = source; this.container = container; length = source.length(container); }
        @Override public boolean advance() {
            if (source == null) return false;
            if (index == length) { close(); return false; }
            current = source.child(container, index++); return true;
        }
        @Override public long value() { if (current == 0) throw new java.util.NoSuchElementException(); return current; }
        @Override public void close() { source = null; current = 0; }
    }

    /** Boolean 0/1, signed 32-bit value, or an enum ordinal. Large INTEGER is read by blocks. */
    public synchronized long scalar(long handle) {
        AirShape shape = shape(handle);
        if (shape != AirShape.BOOLEAN && shape != AirShape.SMALL_INTEGER && shape.form() != AirShape.Form.ENUM)
            throw new IllegalArgumentException("primitive AIR scalar required");
        long value = source.scalar(handle);
        if ((shape == AirShape.BOOLEAN && (value < 0 || value > 1))
                || (shape == AirShape.SMALL_INTEGER && (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE))
                || (shape.form() == AirShape.Form.ENUM && (value < 0 || value >= shape.enumCount())))
            throw new IllegalStateException("invalid primitive AIR storage");
        return value;
    }

    public synchronized String enumName(long handle) {
        AirShape shape = shape(handle);
        if (shape.form() != AirShape.Form.ENUM) throw new IllegalArgumentException("AIR enum required");
        return shape.enumName((int) scalar(handle));
    }

    public synchronized long characterCount(long handle) {
        AirShape shape = shape(handle);
        if (shape != AirShape.TEXT && shape != AirShape.INTEGER)
            throw new IllegalArgumentException("AIR text or integer required");
        return length(handle);
    }

    /** Exactly one bounded block. End yields zero; arbitrary source short reads are rejected. */
    public synchronized int readCharacters(long handle, long offset, char[] output, int start, int count) {
        Objects.requireNonNull(output); Objects.checkFromIndexSize(start, count, output.length);
        long length = characterCount(handle);
        if (offset < 0 || offset > length) throw new IndexOutOfBoundsException("AIR character offset " + offset);
        int wanted = (int) Math.min(count, length - offset);
        if (wanted == 0) return 0;
        if (source.characters(handle, offset, output, start, wanted) != wanted)
            throw new IllegalStateException("premature end of frozen AIR character storage");
        return wanted;
    }

    private long length(long handle) {
        long length = source.length(handle);
        if (length < 0) throw new IllegalStateException("negative AIR storage length");
        return length;
    }
    private void open() { if (source == null) throw new IllegalStateException("AIR snapshot is closed"); }
    @Override public synchronized void close() {
        if (source == null) return;
        Source owner = source; source = null; Throwable failure = null;
        while (cursors != null) try { cursors.close(); } catch (RuntimeException | Error exception) {
            if (failure == null) failure = exception; else if (failure != exception) failure.addSuppressed(exception);
        }
        try { owner.close(); } catch (RuntimeException | Error exception) {
            if (failure == null) failure = exception; else if (failure != exception) failure.addSuppressed(exception);
        }
        if (failure instanceof RuntimeException exception) throw exception;
        if (failure instanceof Error error) throw error;
    }
}
