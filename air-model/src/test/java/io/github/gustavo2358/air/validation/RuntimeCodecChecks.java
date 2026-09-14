package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import java.math.BigInteger;
import java.util.*;
import static io.github.gustavo2358.air.model.MemoryCodecs.Status.*;

/** Independently selected byte oracles plus a second, explicitly named table implementation. */
final class RuntimeCodecChecks {
    static final Memory.Codec IBM=new Memory.ExtensionCodec("text.ebcdic.ibm1047","1",Types.known(Types.Builtin.TEXT));
    // Test oracle extracted from the independently preserved IANA table, not production.
    private static final String IANA_CODEPOINTS =
            "0000000100020003009C00090086007F0097008D008E000B000C000D000E000F" +
            "0010001100120013009D008500080087001800190092008F001C001D001E001F" +
            "00800081008200830084000A0017001B00880089008A008B008C000500060007" +
            "0090009100160093009400950096000400980099009A009B00140015009E001A" +
            "002000A000E200E400E000E100E300E500E700F100A2002E003C0028002B007C" +
            "002600E900EA00EB00E800ED00EE00EF00EC00DF00210024002A0029003B005E" +
            "002D002F00C200C400C000C100C300C500C700D100A6002C0025005F003E003F" +
            "00F800C900CA00CB00C800CD00CE00CF00CC0060003A002300400027003D0022" +
            "00D800610062006300640065006600670068006900AB00BB00F000FD00FE00B1" +
            "00B0006A006B006C006D006E006F00700071007200AA00BA00E600B800C600A4" +
            "00B5007E0073007400750076007700780079007A00A100BF00D0005B00DE00AE" +
            "00AC00A300A500B700A900A700B600BC00BD00BE00DD00A800AF005D00B400D7" +
            "007B00410042004300440045004600470048004900AD00F400F600F200F300F5" +
            "007D004A004B004C004D004E004F00500051005200B900FB00FC00F900FA00FF" +
            "005C00F70053005400550056005700580059005A00B200D400D600D200D300D5" +
            "003000310032003300340035003600370038003900B300DB00DC00D900DA009F";
    static void run() {
        check("PGM00001",List.of(0xd7,0xc7,0xd4,0xf0,0xf0,0xf0,0xf0,0xf1));
        check(" Aé ",List.of(0x40,0xc1,0x51,0x40));
        check("[]^",List.of(0xad,0xbd,0x5f));
        check("\u0085\n",List.of(0x15,0x25));
        check("",List.of());
        byte[] every=new byte[256];for(int i=0;i<256;i++)every[i]=(byte)i;
        // Explicit IBM1047 in the independent JDK implementation, never defaultCharset.
        String jdk=new String(every,java.nio.charset.Charset.forName("IBM1047"));
        var expectedBuilder=new StringBuilder();
        for(int i=0;i<256;i++)expectedBuilder.append((char)Integer.parseInt(IANA_CODEPOINTS.substring(i*4,i*4+4),16));
        String expected=expectedBuilder.toString();
        var differences=new ArrayList<Integer>();for(int i=0;i<256;i++)if(expected.charAt(i)!=jdk.charAt(i))differences.add(i);
        equal(List.of(0x15,0x25),differences); // Known JDK21 LF/NEL convention differs from this explicit contract.
        var decoded=MemoryCodecs.decodeText(IBM,Values.BytesValue.of(every),BigInteger.valueOf(256));
        equal(EXACT,decoded.status());equal(expected,decoded.value().orElseThrow().value());
        equal(256,new HashSet<>(decoded.value().orElseThrow().value().chars().boxed().toList()).size());
        equal(Values.BytesValue.of(every),MemoryCodecs.encodeText(IBM,new Values.TextValue(expected),BigInteger.valueOf(256)).value().orElseThrow());
        for(String text:List.of("€","😀"))equal(UNREPRESENTABLE_TEXT,MemoryCodecs.encodeText(IBM,new Values.TextValue(text),BigInteger.ONE).status());
        equal(EXTENT_MISMATCH,MemoryCodecs.encodeText(IBM,new Values.TextValue("A"),BigInteger.TWO).status());
        equal(EXTENT_MISMATCH,MemoryCodecs.decodeText(IBM,new Values.BytesValue(List.of(0xc1)),BigInteger.TWO).status());
        var ascii=Memory.AsciiText.INSTANCE;
        equal(List.of(65,32),MemoryCodecs.encodeText(ascii,new Values.TextValue("A "),BigInteger.TWO).value().orElseThrow().octets());
        equal(INVALID_BYTES,MemoryCodecs.decodeText(ascii,new Values.BytesValue(List.of(193)),BigInteger.ONE).status());
        equal(UNREPRESENTABLE_TEXT,MemoryCodecs.encodeText(ascii,new Values.TextValue("é"),BigInteger.ONE).status());
        for(Memory.Codec missing:List.of(new Memory.ExtensionCodec("text.ebcdic.ibm1047","2",Types.known(Types.Builtin.TEXT)),
                new Memory.ExtensionCodec("IBM-1047","1",Types.known(Types.Builtin.TEXT)),
                new Memory.UnknownCodec(Types.known(Types.Builtin.TEXT),new io.github.gustavo2358.air.model.Ids.UncertaintyId(new io.github.gustavo2358.air.model.Ids.PublicationId("codec-test"),"unknown")))) {
            var raw=new Values.BytesValue(List.of(193));var result=MemoryCodecs.decodeText(missing,raw,BigInteger.ONE);
            equal(UNSUPPORTED_CODEC,result.status());equal(Optional.empty(),result.value());equal(List.of(193),raw.octets());
            equal(UNSUPPORTED_CODEC,MemoryCodecs.encodeText(missing,new Values.TextValue("A"),BigInteger.ONE).status());
        }
    }
    private static void check(String text,List<Integer> octets) {
        var extent=BigInteger.valueOf(octets.size());var raw=new Values.BytesValue(octets);
        equal(raw,MemoryCodecs.encodeText(IBM,new Values.TextValue(text),extent).value().orElseThrow());
        equal(new Values.TextValue(text),MemoryCodecs.decodeText(IBM,raw,extent).value().orElseThrow());
    }
    private static void equal(Object expected,Object actual){if(!Objects.equals(expected,actual))throw new AssertionError("expected "+expected+" got "+actual);}
}
