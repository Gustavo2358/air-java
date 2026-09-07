package io.github.gustavo2358.air.model;


/** Pure expression evaluated at its use; never a captured runtime value by object identity. */
public sealed interface Expression extends Operand permits Expressions.Literal, Expressions.Read,
    Expressions.Unknown, Expressions.Unary, Expressions.Binary, Expressions.Quantize,
    Expressions.FitText, Expressions.SliceText, Expressions.TrimRight {}
