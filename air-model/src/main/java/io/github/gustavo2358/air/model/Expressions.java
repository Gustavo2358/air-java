package io.github.gustavo2358.air.model;

import java.util.*;
import java.math.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Typed AIR 2.0 values. Collections are defensively copied; no transport dependencies. */
public final class Expressions {
    private Expressions() {}
    public enum UnaryOperator { NOT, NEG, ABS, TO_DECIMAL, TO_INT, IS_DIGITS, LENGTH }
    public enum BinaryOperator { EQ, NE, LT, LE, GT, GE, AND, OR, ADD, SUB, MUL, CONCAT }
    public enum Rounding { TOWARD_ZERO, HALF_EVEN }
    public record Literal(Operand.Header header, Values.LiteralValue value) implements Expression {
        public Literal {
            header = Objects.requireNonNull(header, "header");
            value = Objects.requireNonNull(value, "value");
            
        }

    }
    public record Read(Operand.Header header, Place place) implements Expression {
        public Read {
            header = Objects.requireNonNull(header, "header");
            place = Objects.requireNonNull(place, "place");
            
        }

    }
    public record Unknown(Operand.Header header, Types.TypeRef typeRef, List<Expression> dependencies, Scopes.MemoryBound remainingReads, UncertaintyId reason) implements Expression {
        public Unknown {
            header = Objects.requireNonNull(header, "header");
            typeRef = Objects.requireNonNull(typeRef, "typeRef");
            dependencies = List.copyOf(dependencies);
            remainingReads = Objects.requireNonNull(remainingReads, "remainingReads");
            reason = Objects.requireNonNull(reason, "reason");
            
        }

    }
    public record Unary(Operand.Header header, UnaryOperator operator, Expression argument) implements Expression {
        public Unary {
            header = Objects.requireNonNull(header, "header");
            operator = Objects.requireNonNull(operator, "operator");
            argument = Objects.requireNonNull(argument, "argument");
            
        }

    }
    public record Binary(Operand.Header header, BinaryOperator operator, Expression left, Expression right) implements Expression {
        public Binary {
            header = Objects.requireNonNull(header, "header");
            operator = Objects.requireNonNull(operator, "operator");
            left = Objects.requireNonNull(left, "left");
            right = Objects.requireNonNull(right, "right");
            
        }

    }
    public record Quantize(Operand.Header header, Expression value, BigInteger scale, Rounding rounding) implements Expression {
        public Quantize {
            header = Objects.requireNonNull(header, "header");
            value = Objects.requireNonNull(value, "value");
            scale = Objects.requireNonNull(scale, "scale");
            nonNegative(scale, "scale");
            rounding = Objects.requireNonNull(rounding, "rounding");
            
        }

    }
    /** Value-level wrapping; does not infer an encoding or a memory layout. */
    public record WrapInteger(Operand.Header header,Expression value,BigInteger width,boolean signed) implements Expression {
        public WrapInteger { Objects.requireNonNull(header);Objects.requireNonNull(value);Objects.requireNonNull(width);
            if(width.signum()<=0)throw new IllegalArgumentException("positive bit width"); }
    }
    /** Explicit finite decimal adjustment; d/s are descriptors, never expanded by the model. */
    public record FitDecimal(Operand.Header header, Expression value, BigInteger digits,
                             BigInteger scale, boolean absolute) implements Expression {
        public FitDecimal {
            Objects.requireNonNull(header);Objects.requireNonNull(value);Objects.requireNonNull(digits);Objects.requireNonNull(scale);
            if(digits.signum()<=0)throw new IllegalArgumentException("positive decimal precision");
        }
    }
    /** Decimal digits of a magnitude, with explicit fixed length; no locale or physical encoding. */
    public record IntegerDigits(Operand.Header header,Expression value,BigInteger digits) implements Expression {
        public IntegerDigits { Objects.requireNonNull(header);Objects.requireNonNull(value);Objects.requireNonNull(digits);
            if(digits.signum()<=0)throw new IllegalArgumentException("positive digit length"); }
    }
    /** Total decimal digit interpretation; invalid input uses an explicit INT expression. */
    public record ParseInteger(Operand.Header header,Expression value,Expression onInvalid) implements Expression {
        public ParseInteger { Objects.requireNonNull(header);Objects.requireNonNull(value);Objects.requireNonNull(onInvalid); }
    }
    public record FormatDecimal(Operand.Header header,Expression value,List<DecimalText.Part> parts) implements Expression {
        public FormatDecimal { Objects.requireNonNull(header);Objects.requireNonNull(value);parts=List.copyOf(parts);DecimalText.describe(parts); }
    }
    /** One logical scalar repeated a natural number of times, without materialization. */
    public record FillText(Operand.Header header, Expression character, BigInteger length) implements Expression {
        public FillText {
            Objects.requireNonNull(header); Objects.requireNonNull(character); Objects.requireNonNull(length);
            nonNegative(length,"length");
        }
    }
    public record FitText(Operand.Header header, Expression value, BigInteger length, String pad) implements Expression {
        public FitText {
            header = Objects.requireNonNull(header, "header");
            value = Objects.requireNonNull(value, "value");
            length = Objects.requireNonNull(length, "length");
            pad = unicode(pad, "pad");
            nonNegative(length,"length"); if (pad.codePointCount(0,pad.length())!=1) throw new IllegalArgumentException("pad must be one Unicode scalar");
        }

    }
    public record SliceText(Operand.Header header, Expression value, Expression start,
                            Expression count) implements Expression {
        public SliceText {
            header = Objects.requireNonNull(header, "header");
            value = Objects.requireNonNull(value, "value");
            start = Objects.requireNonNull(start, "start");
            count = Objects.requireNonNull(count, "count");
            
        }

    }
    public record TrimRight(Operand.Header header, Expression value, String characters) implements Expression {
        public TrimRight { Objects.requireNonNull(header,"header"); Objects.requireNonNull(value,"value"); characters=unicode(characters,"characters"); }
    }

}
