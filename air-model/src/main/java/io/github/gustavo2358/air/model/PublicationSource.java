package io.github.gustavo2358.air.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;

/** Explicit in-memory Publication adapter. Its heap is caller managed, not a paged-decoding claim. */
final class PublicationSource implements AirSnapshot.Source {
    private record IntegerText(String decimal) { }
    private ArrayList<Object> values = new ArrayList<>();
    private IdentityHashMap<Object, Long> identities = new IdentityHashMap<>();

    PublicationSource(Publication publication) { node(publication); }

    private long node(Object value) {
        Long existing = identities.get(value);
        if (existing != null) return existing;
        if (values.size() == Integer.MAX_VALUE) throw new IllegalStateException("in-memory adapter address space exhausted");
        long handle = (long) values.size() + 1;
        // Compute a large integer's representation once per model identity, not once per text block.
        values.add(value instanceof BigInteger integer ? new IntegerText(integer.toString()) : value);
        identities.put(value, handle); return handle;
    }
    private Object value(long handle) {
        if (values == null) throw new IllegalStateException("Publication source is closed");
        if (handle <= 0 || handle > values.size()) throw new IllegalArgumentException("unknown Publication node");
        return values.get((int) (handle - 1));
    }
    @Override public AirShape shape(long handle) {
        Object value = value(handle);
        return value instanceof IntegerText ? AirShape.INTEGER : PublicationProjection.shape(value);
    }
    @Override public long length(long handle) {
        Object value = value(handle);
        return switch (value) {
            case List<?> list -> list.size();
            case Optional<?> optional -> optional.isPresent() ? 1 : 0;
            case String text -> text.length();
            case IntegerText integer -> integer.decimal().length();
            default -> PublicationProjection.shape(value).fieldCount();
        };
    }
    @Override public long child(long handle, long index) {
        Object value = value(handle);
        if (index < 0 || index >= length(handle)) throw new IndexOutOfBoundsException("Publication node element " + index);
        Object child = switch (value) {
            case List<?> list -> list.get((int) index);
            case Optional<?> optional -> optional.orElseThrow();
            default -> PublicationProjection.field(value, (int) index);
        };
        return node(child);
    }
    @Override public long scalar(long handle) {
        return switch (value(handle)) {
            case Boolean bool -> bool ? 1 : 0;
            case Integer integer -> integer;
            case Enum<?> enumeration -> enumeration.ordinal();
            default -> throw new IllegalArgumentException("Publication scalar required");
        };
    }
    @Override public int characters(long handle, long offset, char[] output, int start, int count) {
        String text = switch (value(handle)) {
            case String string -> string;
            case IntegerText integer -> integer.decimal();
            default -> throw new IllegalArgumentException("Publication character data required");
        };
        text.getChars((int) offset, (int) offset + count, output, start); return count;
    }
    @Override public void close() { values = null; identities = null; }
}
