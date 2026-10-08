package io.github.gustavo2358.air.model;

import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Direct typed occurrence edges in model order, without row/list materialization or recursion.
 * Borrows snapshot and callback; neither validation nor expression evaluation is performed.
 * Callers own traversal frontiers and occurrence/identity checks. Envelope IDs are references,
 * not operand definitions. Operational and callback failures propagate with cursor cleanup.
 */
public final class SnapshotOperands {
    private SnapshotOperands() { }

    public static void children(AirSnapshot snapshot, long operand, LongConsumer sink) {
        Objects.requireNonNull(snapshot); Objects.requireNonNull(sink);
        AirShape shape = snapshot.shape(operand);
        switch (shape) {
            case EXPRESSIONS_LITERAL, PLACES_OBJECT_PLACE -> { }
            case EXPRESSIONS_READ, EXPRESSIONS_QUANTIZE, EXPRESSIONS_FORMAT_DECIMAL,
                 EXPRESSIONS_INTEGER_DIGITS, EXPRESSIONS_WRAP_INTEGER, EXPRESSIONS_FIT_DECIMAL,
                 EXPRESSIONS_FILL_TEXT, EXPRESSIONS_FIT_TEXT, EXPRESSIONS_TRIM_RIGHT ->
                sink.accept(snapshot.field(operand, shape, 1));
            case EXPRESSIONS_UNKNOWN -> list(snapshot, snapshot.field(operand, shape, 2), AirShape.EXPRESSION, sink);
            case PLACES_CHOICE -> list(snapshot, snapshot.field(operand, shape, 1), AirShape.PLACE, sink);
            case EXPRESSIONS_UNARY -> sink.accept(snapshot.field(operand, shape, 2));
            case EXPRESSIONS_BINARY, PLACES_REGION_SLICE -> {
                sink.accept(snapshot.field(operand, shape, 2));
                sink.accept(snapshot.field(operand, shape, 3));
            }
            case EXPRESSIONS_PARSE_INTEGER -> {
                sink.accept(snapshot.field(operand, shape, 1));
                sink.accept(snapshot.field(operand, shape, 2));
            }
            case EXPRESSIONS_SLICE_TEXT -> {
                sink.accept(snapshot.field(operand, shape, 1));
                sink.accept(snapshot.field(operand, shape, 2));
                sink.accept(snapshot.field(operand, shape, 3));
            }
            default -> throw new IllegalArgumentException("concrete AIR operand required");
        }
    }

    public static void roots(AirSnapshot snapshot, long operation, LongConsumer sink) {
        Objects.requireNonNull(snapshot); Objects.requireNonNull(sink);
        AirShape shape = snapshot.shape(operation);
        switch (shape) {
            case OPERATIONS_ASSIGN -> {
                sink.accept(snapshot.field(operation, shape, 1));
                sink.accept(snapshot.field(operation, shape, 2));
            }
            case OPERATIONS_HAVOC_MUST, OPERATIONS_BRANCH, OPERATIONS_DISPATCH,
                 OPERATIONS_INDIRECT_JUMP -> sink.accept(snapshot.field(operation, shape, 1));
            case OPERATIONS_COPY_BYTES -> {
                range(snapshot, snapshot.field(operation, shape, 1), sink);
                range(snapshot, snapshot.field(operation, shape, 2), sink);
            }
            case OPERATIONS_INVOKE -> {
                long target = snapshot.field(operation, shape, 2);
                AirShape targetShape = snapshot.shape(target);
                switch (targetShape) {
                    case INTERACTIONS_COMPUTED_TARGET -> sink.accept(snapshot.field(target, targetShape, 2));
                    case INTERACTIONS_INTERNAL_TARGET, INTERACTIONS_LITERAL_TARGET -> { }
                    default -> throw new IllegalArgumentException("concrete AIR target required");
                }
                long arguments = snapshot.field(operation, shape, 3);
                try (var cursor = snapshot.elements(arguments, AirShape.INTERACTIONS_ARGUMENT)) {
                    while (cursor.advance()) {
                        long argument = cursor.value(); AirShape argumentShape = snapshot.shape(argument);
                        switch (argumentShape) {
                            case INTERACTIONS_VALUE_ARGUMENT, INTERACTIONS_COPY_ARGUMENT,
                                 INTERACTIONS_REFERENCE_ARGUMENT -> sink.accept(snapshot.field(argument, argumentShape, 0));
                            default -> throw new IllegalArgumentException("concrete AIR argument required");
                        }
                    }
                }
                list(snapshot, snapshot.field(operation, shape, 4), AirShape.PLACE, sink);
                list(snapshot, snapshot.field(operation, shape, 6), AirShape.PLACE, sink);
            }
            case OPERATIONS_RETURN -> list(snapshot, snapshot.field(operation, shape, 1), AirShape.EXPRESSION, sink);
            case OPERATIONS_RAISE -> list(snapshot, snapshot.field(operation, shape, 2), AirShape.EXPRESSION, sink);
            case OPERATIONS_OPAQUE -> list(snapshot, snapshot.field(operation, shape, 2), AirShape.OPERAND, sink);
            case OPERATIONS_HAVOC_MAY, OPERATIONS_NOP, OPERATIONS_JUMP, OPERATIONS_HALT,
                 OPERATIONS_LOCAL_INVOKE, OPERATIONS_LOCAL_BOUNDARY, OPERATIONS_LOCAL_RESUME,
                 OPERATIONS_LOCAL_UNWIND -> { }
            default -> throw new IllegalArgumentException("concrete AIR operation required");
        }
    }

    private static void range(AirSnapshot snapshot, long range, LongConsumer sink) {
        sink.accept(snapshot.field(range, AirShape.MEMORY_BYTE_RANGE, 1));
        sink.accept(snapshot.field(range, AirShape.MEMORY_BYTE_RANGE, 2));
    }
    private static void list(AirSnapshot snapshot, long list, AirShape element, LongConsumer sink) {
        try (var cursor = snapshot.elements(list, element)) {
            while (cursor.advance()) sink.accept(cursor.value());
        }
    }
}
