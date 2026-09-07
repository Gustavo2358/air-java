package io.github.gustavo2358.air.model;


/** Exactly one is required at the end of every Sequence; no implicit intersequence fallthrough. */
public sealed interface Terminator extends Operation permits Operations.Jump, Operations.Branch,
    Operations.Dispatch, Operations.Invoke, Operations.Return, Operations.Raise, Operations.Halt,
    Operations.Opaque, Operations.LocalInvoke, Operations.LocalBoundary,
    Operations.LocalResume, Operations.LocalUnwind, Operations.IndirectJump {}
