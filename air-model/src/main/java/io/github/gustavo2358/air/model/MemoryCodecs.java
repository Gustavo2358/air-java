package io.github.gustavo2358.air.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

/** Pure interpretation of explicitly selected AIR text codecs. No environmental encoding. */
public final class MemoryCodecs {
    private MemoryCodecs() {}
    public enum Status { EXACT, UNSUPPORTED_CODEC, UNREPRESENTABLE_TEXT, INVALID_BYTES, EXTENT_MISMATCH }
    public record Result<T>(Status status, Optional<T> value) {
        public Result {
            Objects.requireNonNull(status); Objects.requireNonNull(value);
            if ((status == Status.EXACT) != value.isPresent()) throw new IllegalArgumentException("exact result iff value present");
        }
    }
    // IBM CP1047 -> ISO10646 table 1.00, IBM/IANA registration 2002-09-24.
    // Each pair is a Unicode scalar U+00xx for the corresponding octet, in order.
    private static final String IBM1047_TABLE =
            "000102039c09867f978d8e0b0c0d0e0f" +
            "101112139d8508871819928f1c1d1e1f" +
            "80818283840a171b88898a8b8c050607" +
            "909116939495960498999a9b14159e1a" +
            "20a0e2e4e0e1e3e5e7f1a22e3c282b7c" +
            "26e9eaebe8edeeefecdf21242a293b5e" +
            "2d2fc2c4c0c1c3c5c7d1a62c255f3e3f" +
            "f8c9cacbc8cdcecfcc603a2340273d22" +
            "d8616263646566676869abbbf0fdfeb1" +
            "b06a6b6c6d6e6f707172aabae6b8c6a4" +
            "b57e737475767778797aa1bfd05bdeae" +
            "aca3a5b7a9a7b6bcbdbedda8af5db4d7" +
            "7b414243444546474849adf4f6f2f3f5" +
            "7d4a4b4c4d4e4f505152b9fbfcf9faff" +
            "5cf7535455565758595ab2d4d6d2d3d5" +
            "30313233343536373839b3dbdcd9da9f";
    private static final int[] DECODE = decodeTable();
    private static final int[] ENCODE = inverseTable();
    private static int[] decodeTable() {
        var table = new int[256];
        for (int i=0;i<256;i++) table[i]=Integer.parseInt(IBM1047_TABLE.substring(i*2,i*2+2),16);
        return table;
    }
    private static int[] inverseTable() {
        var table = new int[256];
        for (int i=0;i<256;i++) table[DECODE[i]]=i;
        return table;
    }
    /** Identity/version only. Logical-domain contradictions remain Validator errors. */
    public static boolean isIbm1047Identity(Memory.Codec codec) {
        return codec instanceof Memory.ExtensionCodec e && e.name().equals(Capabilities.IBM1047.name())
                && e.version().equals(Capabilities.IBM1047.version());
    }
    public static boolean isIbm1047(Memory.Codec codec) {
        return isIbm1047Identity(codec) && ((Memory.ExtensionCodec)codec).logicalType().equals(Types.known(Types.Builtin.TEXT));
    }
    public static Result<Values.BytesValue> encodeText(Memory.Codec codec, Values.TextValue text, BigInteger extent) {
        Objects.requireNonNull(codec); Objects.requireNonNull(text); requireExtent(extent);
        boolean ibm = isIbm1047(codec), ascii = codec instanceof Memory.AsciiText;
        if (!ibm && !ascii) return failure(Status.UNSUPPORTED_CODEC);
        String value=text.value();
        if (!extent.equals(BigInteger.valueOf(value.codePointCount(0,value.length())))) return failure(Status.EXTENT_MISMATCH);
        var octets=new ArrayList<Integer>(value.length());
        for(int i=0;i<value.length();i++) {
            int scalar=value.charAt(i);
            if(scalar>(ascii?127:255)) return failure(Status.UNREPRESENTABLE_TEXT);
            octets.add(ibm?ENCODE[scalar]:scalar);
        }
        return exact(new Values.BytesValue(octets));
    }
    public static Result<Values.TextValue> decodeText(Memory.Codec codec, Values.BytesValue bytes, BigInteger extent) {
        Objects.requireNonNull(codec); Objects.requireNonNull(bytes); requireExtent(extent);
        boolean ibm = isIbm1047(codec), ascii = codec instanceof Memory.AsciiText;
        if (!ibm && !ascii) return failure(Status.UNSUPPORTED_CODEC);
        if (!extent.equals(BigInteger.valueOf(bytes.octets().size()))) return failure(Status.EXTENT_MISMATCH);
        var text=new StringBuilder(bytes.octets().size());
        for(int octet:bytes.octets()) {
            if(ascii && octet>127) return failure(Status.INVALID_BYTES);
            text.append((char)(ibm?DECODE[octet]:octet));
        }
        return exact(new Values.TextValue(text.toString()));
    }
    private static void requireExtent(BigInteger extent) {
        Objects.requireNonNull(extent); if(extent.signum()<0)throw new IllegalArgumentException("negative extent");
    }
    private static <T> Result<T> failure(Status status){return new Result<>(status,Optional.empty());}
    private static <T> Result<T> exact(T value){return new Result<>(Status.EXACT,Optional.of(value));}
}
