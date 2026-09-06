package io.github.gustavo2358.air.model;

import java.util.*;
import java.math.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Typed AIR 2.0 values. Collections are defensively copied; no transport dependencies. */
public final class Expressions {
    private Expressions() {}
    public enum UnaryOperator { NOT, NEG, TO_DECIMAL, LENGTH }
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
    public record Quantize(Operand.Header header, Expression value, int scale, Rounding rounding) implements Expression {
        public Quantize {
            header = Objects.requireNonNull(header, "header");
            value = Objects.requireNonNull(value, "value");
            nonNegative(scale, "scale");
            rounding = Objects.requireNonNull(rounding, "rounding");
            
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
    public record SliceText(Operand.Header header, Expression value, Expression start, Expression count, Optional<PremiseId> boundsProof) implements Expression {
        public SliceText {
            header = Objects.requireNonNull(header, "header");
            value = Objects.requireNonNull(value, "value");
            start = Objects.requireNonNull(start, "start");
            count = Objects.requireNonNull(count, "count");
            boundsProof = Objects.requireNonNull(boundsProof, "boundsProof");
            
        }

    }
    public record TrimRight(Operand.Header header, Expression value, String characters) implements Expression {
        public TrimRight { Objects.requireNonNull(header,"header"); Objects.requireNonNull(value,"value"); characters=unicode(characters,"characters"); }
    }

}
