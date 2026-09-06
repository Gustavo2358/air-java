package io.github.gustavo2358.air.model;


/** An occurrence of a value or place, distinct from its nominal entity. */
public sealed interface Operand permits Expression, Place {
    Header header();
    enum Role { VALUE_READ, VALUE_WRITE, ADDRESS_READ, PREDICATE, CALL_TARGET,
        ARGUMENT_VALUE, ARGUMENT_REFERENCE, RESULT_TARGET, RESOURCE_TARGET, CONTROL_TARGET }
    record Header(Ids.OperandId id, Role role, Ids.OriginId origin) {
        public Header {
            java.util.Objects.requireNonNull(id,"id");
            java.util.Objects.requireNonNull(role,"role");
            java.util.Objects.requireNonNull(origin,"origin");
        }
    }
}
