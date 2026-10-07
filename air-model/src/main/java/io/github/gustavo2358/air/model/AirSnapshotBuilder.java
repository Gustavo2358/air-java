package io.github.gustavo2358.air.model;

import java.util.Objects;

/**
 * Single-owner append-only typed AIR storage builder. It retains no collection/text payload in
 * resident lists/strings. Primitive storage owns capacity accounting, spill and operational failures.
 * Fields have complete official shapes; model-local constraints and cross-reference validation are
 * separate obligations. finish freezes/transfers storage but issues no validity certificate.
 */
public final class AirSnapshotBuilder implements AutoCloseable {
    public enum Column { NODES, REFERENCES, ARRAY_TREE, CHARACTERS }
    /** Capacity lease. Close releases once even if cleanup reports an operational failure. */
    public interface Lease extends AutoCloseable { @Override void close(); }
    /** Zero-default 64-bit primitive columns, plus pre-allocation control-capacity leases. */
    public interface Storage extends AutoCloseable {
        long get(Column column, long index);
        void set(Column column, long index, long value);
        Lease claim(long bytes);
        /** Irreversibly reject future writes/claims; read access and closure remain available. */
        void freeze();
        @Override void close();
    }
    private static final AirShape[] SHAPES = AirShape.values();
    private Storage storage;
    private Lease control;
    private long nodes, references, trees, characterWords;
    private Appender writers;
    private boolean closed, failed, textActive;

    public AirSnapshotBuilder(Storage storage) {
        this.storage = Objects.requireNonNull(storage);
        try { control = Objects.requireNonNull(storage.claim(512)); }
        catch (RuntimeException | Error exception) {
            try { storage.close(); } catch (RuntimeException cleanup) { exception.addSuppressed(cleanup); }
            throw exception;
        }
    }

    public long scalar(AirShape shape, long value) {
        open(); Objects.requireNonNull(shape);
        if ((shape == AirShape.BOOLEAN && (value < 0 || value > 1))
                || (shape == AirShape.SMALL_INTEGER && (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE))
                || (shape.form() == AirShape.Form.ENUM && (value < 0 || value >= shape.enumCount()))
                || (shape != AirShape.BOOLEAN && shape != AirShape.SMALL_INTEGER && shape.form() != AirShape.Form.ENUM))
            throw new IllegalArgumentException("invalid AIR primitive scalar");
        try { return node(shape, value, 0, null); }
        catch (RuntimeException exception) { failed = true; throw exception; }
    }

    /** Fixed record fields only. Large lists/text use their bounded appenders. */
    public long record(AirShape shape, long... fields) {
        open(); Objects.requireNonNull(shape); Objects.requireNonNull(fields);
        if (shape.form() != AirShape.Form.RECORD || fields.length != shape.fieldCount())
            throw new IllegalArgumentException("wrong AIR record shape or field count");
        // Shape admission precedes writes; no full-inventory/array scan for each field.
        for (int n = 0; n < fields.length; n++) {
            var slot = shape.field(n); AirShape actual = shape(fields[n]);
            if (!slot.value().accepts(actual)) throw new IllegalArgumentException("wrong AIR field type: " + slot.name());
            if (slot.element() != null) {
                AirShape declared = SHAPES[(int) get(Column.NODES, base(fields[n]) + 3) - 1];
                if (!slot.element().accepts(declared)) throw new IllegalArgumentException("wrong AIR field element type: " + slot.name());
            }
        }
        try {
            long offset = references;
            if (fields.length > Long.MAX_VALUE - references) throw new IllegalStateException("reference address space exhausted");
            for (long child : fields) storage.set(Column.REFERENCES, references++, child);
            return node(shape, fields.length, offset, null);
        } catch (RuntimeException exception) { failed = true; throw exception; }
    }

    public long optional(AirShape element, long child) {
        open(); Objects.requireNonNull(element);
        if (child != 0 && !element.accepts(shape(child))) throw new IllegalArgumentException("wrong optional element type");
        try {
            long offset = references;
            if (child != 0) {
                if (references == Long.MAX_VALUE) throw new IllegalStateException("reference address space exhausted");
                storage.set(Column.REFERENCES, references++, child);
            }
            return node(AirShape.OPTIONAL, child == 0 ? 0 : 1, offset, element);
        } catch (RuntimeException exception) { failed = true; throw exception; }
    }

    public ListAppender list(AirShape element) {
        open(); Objects.requireNonNull(element);
        Lease lease = null;
        try { lease = Objects.requireNonNull(storage.claim(1024)); return new ListAppender(this, lease, element); }
        catch (RuntimeException | Error exception) { failed = true; if (lease != null) lease.close(); throw exception; }
    }

    /** One character stream at a time; list/record construction may proceed between its blocks. */
    public TextAppender text(AirShape shape) {
        open(); Objects.requireNonNull(shape);
        if (shape != AirShape.TEXT && shape != AirShape.INTEGER) throw new IllegalArgumentException("text or integer shape required");
        if (textActive) throw new IllegalStateException("another character stream is active");
        Lease lease = null;
        try {
            lease = Objects.requireNonNull(storage.claim(256));
            var writer = new TextAppender(this, lease, shape, characterWords); textActive = true; return writer;
        } catch (RuntimeException | Error exception) { failed = true; if (lease != null) lease.close(); throw exception; }
    }

    public AirSnapshot finish(long publication) {
        open();
        if (writers != null) throw new IllegalStateException("unfinished AIR appenders");
        if (shape(publication) != AirShape.PUBLICATION) throw new IllegalArgumentException("AIR Publication root required");
        try { storage.freeze(); }
        catch (RuntimeException | Error exception) { failed = true; throw exception; }
        var source = new StoredSource(storage, control, nodes);
        closed = true; storage = null; control = null;
        return AirSnapshot.attach(source, publication);
    }

    private AirShape shape(long handle) {
        if (handle <= 0 || handle > nodes) throw new IllegalArgumentException("foreign or unknown builder node");
        return SHAPES[(int) get(Column.NODES, base(handle)) - 1];
    }
    private long get(Column column, long index) {
        try { return storage.get(column, index); }
        catch (RuntimeException exception) { failed = true; throw exception; }
    }
    private long node(AirShape shape, long value, long offset, AirShape element) {
        if (nodes == Long.MAX_VALUE / 4) throw new IllegalStateException("node address space exhausted");
        long handle = nodes + 1, base = base(handle);
        storage.set(Column.NODES, base + 1, value); storage.set(Column.NODES, base + 2, offset);
        storage.set(Column.NODES, base + 3, element == null ? 0 : element.ordinal() + 1L);
        storage.set(Column.NODES, base, shape.ordinal() + 1L); nodes = handle; return handle;
    }
    private long tree(long left, long right, long count, long value) {
        if (trees == Long.MAX_VALUE / 4) throw new IllegalStateException("array tree address space exhausted");
        long handle = trees + 1, base = base(handle);
        storage.set(Column.ARRAY_TREE, base, left); storage.set(Column.ARRAY_TREE, base + 1, right);
        storage.set(Column.ARRAY_TREE, base + 2, count); storage.set(Column.ARRAY_TREE, base + 3, value);
        trees = handle; return handle;
    }
    private long branch(long left, long right) {
        long count = Math.addExact(get(Column.ARRAY_TREE, base(left) + 2), get(Column.ARRAY_TREE, base(right) + 2));
        return tree(left, right, count, 0);
    }
    private static long base(long handle) { return (handle - 1) * 4; }
    private void open() { if (closed || failed) throw new IllegalStateException("AIR builder is closed or aborted"); }

    /** Control-state lease; it never holds the accumulated payload in heap collections. */
    public abstract static class Appender implements AutoCloseable {
        private AirSnapshotBuilder owner;
        private Lease control;
        private Appender previous, next;
        private Appender(AirSnapshotBuilder owner, Lease control) {
            this.owner = owner; this.control = control;
            next = owner.writers; if (next != null) next.previous = this; owner.writers = this;
        }
        final AirSnapshotBuilder owner() {
            if (owner == null) throw new IllegalStateException("AIR appender is closed"); owner.open(); return owner;
        }
        final void detach(boolean completed) {
            if (owner == null) return;
            AirSnapshotBuilder parent = owner;
            if (!completed) parent.failed = true;
            if (previous == null) parent.writers = next; else previous.next = next;
            if (next != null) next.previous = previous;
            owner = null; previous = null; next = null;
            if (this instanceof TextAppender) parent.textActive = false;
            releaseBuffers(); Lease reservation = control; control = null; reservation.close();
        }
        void releaseBuffers() { }
        @Override public final void close() { detach(false); }
    }

    /** Binary-counter forest: O(N) total tree nodes and O(log N) random access, no prior-item scan. */
    public static final class ListAppender extends Appender {
        private final AirShape element;
        private long[] forest;
        private long count;
        private ListAppender(AirSnapshotBuilder owner, Lease lease, AirShape element) {
            super(owner, lease); this.element = element; forest = new long[64];
        }
        public void add(long child) {
            AirSnapshotBuilder parent = owner();
            if (!element.accepts(parent.shape(child))) throw new IllegalArgumentException("wrong list element type");
            try {
                if (count == Long.MAX_VALUE) throw new IllegalStateException("list cardinality exhausted");
                long tree = parent.tree(0, 0, 1, child); int level = 0;
                while (forest[level] != 0) { tree = parent.branch(forest[level], tree); forest[level++] = 0; }
                forest[level] = tree; count++;
            } catch (RuntimeException exception) { parent.failed = true; throw exception; }
        }
        public long finish() {
            AirSnapshotBuilder parent = owner();
            try {
                long root = 0;
                for (int level = 63; level >= 0; level--) if (forest[level] != 0)
                    root = root == 0 ? forest[level] : parent.branch(root, forest[level]);
                long node = parent.node(AirShape.LIST, count, root, element); detach(true); return node;
            } catch (RuntimeException exception) { parent.failed = true; throw exception; }
        }
        @Override void releaseBuffers() { forest = null; }
    }

    /** Four UTF-16 units per primitive word; Unicode/decimal checks cross append-block boundaries. */
    public static final class TextAppender extends Appender {
        private final AirShape shape;
        private final long start;
        private long count, packed;
        private int packedCount;
        private boolean high, negative, firstDigit, zero;
        private TextAppender(AirSnapshotBuilder owner, Lease lease, AirShape shape, long start) {
            super(owner, lease); this.shape = shape; this.start = start;
        }
        public void append(char[] characters, int offset, int length) {
            AirSnapshotBuilder parent = owner(); Objects.requireNonNull(characters);
            Objects.checkFromIndexSize(offset, length, characters.length);
            try {
                for (int n = 0; n < length; n++) {
                    char value = characters[offset + n];
                    if (shape == AirShape.INTEGER) decimal(value);
                    else if (high) { if (!Character.isLowSurrogate(value)) throw new IllegalArgumentException("unpaired high surrogate"); high = false; }
                    else if (Character.isHighSurrogate(value)) high = true;
                    else if (Character.isLowSurrogate(value)) throw new IllegalArgumentException("unpaired low surrogate");
                    if (count == Long.MAX_VALUE) throw new IllegalStateException("character cardinality exhausted");
                    packed |= (long) value << (packedCount * 16); packedCount++; count++;
                    if (packedCount == 4) flush(parent);
                }
            } catch (RuntimeException exception) { parent.failed = true; throw exception; }
        }
        private void decimal(char value) {
            if (count == 0 && value == '-') { negative = true; return; }
            if (value < '0' || value > '9' || zero) throw new IllegalArgumentException("canonical decimal integer required");
            if (!firstDigit) { firstDigit = true; zero = value == '0'; }
        }
        private void flush(AirSnapshotBuilder parent) {
            if (parent.characterWords == Long.MAX_VALUE) throw new IllegalStateException("character address space exhausted");
            parent.storage.set(Column.CHARACTERS, parent.characterWords++, packed); packed = 0; packedCount = 0;
        }
        public long finish() {
            AirSnapshotBuilder parent = owner();
            try {
                if (high) throw new IllegalArgumentException("unpaired high surrogate");
                if (shape == AirShape.INTEGER && (!firstDigit || (zero && negative))) throw new IllegalArgumentException("canonical decimal integer required");
                if (packedCount != 0) flush(parent);
                long node = parent.node(shape, count, start, null); detach(true); return node;
            } catch (RuntimeException exception) { parent.failed = true; throw exception; }
        }
    }

    private static final class StoredSource implements AirSnapshot.Source {
        private Storage storage;
        private Lease control;
        private final long nodes;
        private StoredSource(Storage storage, Lease control, long nodes) { this.storage = storage; this.control = control; this.nodes = nodes; }
        private long baseChecked(long handle) {
            if (storage == null) throw new IllegalStateException("frozen AIR source is closed");
            if (handle <= 0 || handle > nodes) throw new IllegalArgumentException("unknown frozen AIR node");
            return base(handle);
        }
        @Override public AirShape shape(long handle) {
            long tag = storage.get(Column.NODES, baseChecked(handle));
            if (tag <= 0 || tag > SHAPES.length) throw new IllegalStateException("invalid frozen AIR type tag");
            return SHAPES[(int) tag - 1];
        }
        @Override public long length(long handle) { return storage.get(Column.NODES, baseChecked(handle) + 1); }
        @Override public long scalar(long handle) { return length(handle); }
        @Override public long child(long handle, long index) {
            long base = baseChecked(handle), root = storage.get(Column.NODES, base + 2);
            if (shape(handle) != AirShape.LIST) return storage.get(Column.REFERENCES, root + index);
            while (true) {
                long row = base(root), value = storage.get(Column.ARRAY_TREE, row + 3);
                if (value != 0) return value;
                long left = storage.get(Column.ARRAY_TREE, row), count = storage.get(Column.ARRAY_TREE, base(left) + 2);
                if (index < count) root = left;
                else { index -= count; root = storage.get(Column.ARRAY_TREE, row + 1); }
            }
        }
        @Override public int characters(long handle, long offset, char[] output, int start, int count) {
            long first = storage.get(Column.NODES, baseChecked(handle) + 2), prior = -1, word = 0;
            for (int n = 0; n < count; n++) {
                long at = offset + n, slot = first + (at >>> 2);
                if (slot != prior) { word = storage.get(Column.CHARACTERS, slot); prior = slot; }
                output[start + n] = (char) (word >>> ((at & 3) * 16));
            }
            return count;
        }
        @Override public void close() {
            if (storage == null) return;
            Storage owner = storage; Lease reservation = control; storage = null; control = null;
            closeOwnership(owner, reservation);
        }
    }

    private static void closeOwnership(Storage storage, Lease lease) {
        Throwable failure = null;
        try { storage.close(); } catch (RuntimeException | Error exception) { failure = exception; }
        try { lease.close(); } catch (RuntimeException | Error exception) {
            if (failure == null) failure = exception;
            else if (failure != exception) failure.addSuppressed(exception);
        }
        if (failure instanceof RuntimeException exception) throw exception;
        if (failure instanceof Error error) throw error;
    }

    @Override public void close() {
        if (closed) return; closed = true;
        Throwable failure = null;
        while (writers != null) try { writers.detach(false); } catch (RuntimeException | Error exception) {
            if (failure == null) failure = exception; else if (failure != exception) failure.addSuppressed(exception);
        }
        Storage owner = storage; Lease reservation = control; storage = null; control = null;
        try { closeOwnership(owner, reservation); } catch (RuntimeException | Error exception) {
            if (failure == null) failure = exception; else if (failure != exception) failure.addSuppressed(exception);
        }
        if (failure instanceof RuntimeException exception) throw exception;
        if (failure instanceof Error error) throw error;
    }
}
