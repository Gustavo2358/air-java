package io.github.gustavo2358.air.model;


/** An operation is owned by exactly one Sequence. Derived CFG edges are not part of AIR. */
public sealed interface Operation permits Instruction, Terminator {
    Operations.Header header();
    String kind();
}
