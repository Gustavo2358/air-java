package io.github.gustavo2358.air.model;

import java.util.*;
import java.math.*;
import static io.github.gustavo2358.air.model.Ids.*;
import static io.github.gustavo2358.air.model.Require.*;

/** Typed AIR 2.0 values. Collections are defensively copied; no transport dependencies. */
public final class Values {
    private Values() {}
    public sealed interface LiteralValue permits BoolValue, IntValue, DecimalValue, TextValue, BytesValue, LabelValue {
        Types.Type type();
    }
    public record BoolValue(boolean value) implements LiteralValue {
        public BoolValue {
            
        }
        public Types.Type type() { return Types.Builtin.BOOL; }
    }
    public record IntValue(BigInteger value) implements LiteralValue {
        public IntValue {
            value = Objects.requireNonNull(value, "value");
            
        }
        public Types.Type type() { return Types.Builtin.INT; }
    }
    public record DecimalValue(BigInteger coefficient, int scale) implements LiteralValue {
        public DecimalValue {
            coefficient = Objects.requireNonNull(coefficient, "coefficient");
            nonNegative(scale, "scale");
            
        }
        public Types.Type type() { return Types.Builtin.DECIMAL; }
        public BigDecimal decimal() { return new BigDecimal(coefficient,scale); }
    }
    /** Text permits empty strings but rejects invalid Unicode scalar sequences. */
    public record TextValue(String value) implements LiteralValue {
        public TextValue { value = unicode(value,"value"); }
        public Types.Type type() { return Types.Builtin.TEXT; }
    }
    public record BytesValue(List<Integer> octets) implements LiteralValue {
        public BytesValue {
            octets = List.copyOf(octets);
            if (octets.stream().anyMatch(n -> n<0 || n>255)) throw new IllegalArgumentException("invalid octet");
        }
        public Types.Type type() { return Types.Builtin.BYTES; }
        public static BytesValue of(byte[] bytes) {
            Objects.requireNonNull(bytes,"bytes");
            List<Integer> values = new ArrayList<>(bytes.length);
            for(byte b:bytes) values.add(Byte.toUnsignedInt(b));
            return new BytesValue(values);
        }
        public byte[] toByteArray() {
            byte[] result=new byte[octets.size()];
            for(int i=0;i<result.length;i++) result[i]=(byte)(int)octets.get(i);
            return result;
        }
    }
    public record LabelValue(LabelId label, Types.LabelType domain) implements LiteralValue {
        public LabelValue {
            label = Objects.requireNonNull(label, "label");
            domain = Objects.requireNonNull(domain, "domain");
            if (!domain.labels().contains(label)) throw new IllegalArgumentException("label outside declared domain");
        }
        public Types.Type type() { return domain; }
    }
    /** Semantic equality for dispatch keys, not Java record equality of decimal spellings. */
    public static java.lang.Object semanticKey(LiteralValue value) {
        if(value instanceof DecimalValue d) return d.decimal().stripTrailingZeros();
        return value;
    }

}
