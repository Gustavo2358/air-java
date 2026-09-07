package io.github.gustavo2358.air.model;


/** Non-terminating core operations continue only within their owning Sequence. */
public sealed interface Instruction extends Operation permits Operations.Assign, Operations.HavocMust,
    Operations.HavocMay, Operations.Nop, Operations.CopyBytes {}
