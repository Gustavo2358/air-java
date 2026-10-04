package io.github.gustavo2358.air.json;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.math.BigInteger;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
/** Wire oracle written independently of the new writer. */
public final class NumericExpressionChecks {
    public static void main(String[] args) { run();System.out.println("NUMERIC_JSON=PASS"); }
    static void run() {
        var codec=new AirJson();
        var wire=new String(codec.encode(ScalarAssignOracle.publication(1,1)),StandardCharsets.UTF_8)
            .replace("\"type\":{\"kind\":\"text\"}","\"type\":{\"kind\":\"decimal\"}")
            .replace("\"kind\":\"text\",\"value\":\"PROGA\"","\"kind\":\"decimal\",\"coefficient\":\"-12345\",\"scale\":\"2\"");
        var p=codec.decode(wire.getBytes(StandardCharsets.UTF_8));
        var bytes=codec.encode(p);
        if(!p.equals(codec.decode(bytes))||!Arrays.equals(bytes,codec.encode(codec.decode(bytes))))throw new AssertionError("canonical decimal roundtrip");
        var value=((io.github.gustavo2358.air.model.Expressions.Literal)((io.github.gustavo2358.air.model.Operations.Assign)p.units().getFirst().sequences().getFirst().instructions().getFirst()).value()).value();
        if(!value.equals(new io.github.gustavo2358.air.model.Values.DecimalValue(new java.math.BigInteger("-12345"),java.math.BigInteger.TWO)))throw new AssertionError("decimal coefficient/scale changed");
        var unit=p.units().getFirst();var sequence=unit.sequences().getFirst();
        var assign=(Operations.Assign)sequence.instructions().getFirst();
        java.util.function.Function<String,Operand.Header> header=id->new Operand.Header(new OperandId(new OperationOwner(assign.header().id()),id),Operand.Role.VALUE_READ,assign.header().origin());
        var fit=new Expressions.FitDecimal(header.apply("fit"),assign.value(),BigInteger.valueOf(5),BigInteger.valueOf(-2),true);
        var integer=new Expressions.Unary(header.apply("to-int"),Expressions.UnaryOperator.TO_INT,fit);
        var absolute=new Expressions.Unary(header.apply("absolute"),Expressions.UnaryOperator.ABS,integer);
        var product=new Expressions.Binary(header.apply("multiply"),Expressions.BinaryOperator.MUL,absolute,
            new Expressions.Literal(header.apply("factor"),new Values.IntValue(BigInteger.TEN)));
        var wrapped=new Expressions.WrapInteger(header.apply("wrap"),product,BigInteger.valueOf(16),true);
        var converted=new Expressions.Unary(header.apply("to-decimal"),Expressions.UnaryOperator.TO_DECIMAL,wrapped);
        var changed=new Operations.Assign(assign.header(),assign.destination(),converted);
        var updated=new Unit(unit.id(),unit.containingUnit(),unit.objects(),unit.visibleObjects(),unit.entries(),
            List.of(new Sequence(sequence.label(),List.of(changed),sequence.terminator(),sequence.origin())),unit.completionPorts(),unit.body(),unit.bodyUnavailable(),unit.coverage(),unit.origin());
        var expected=new Publication(p.id(),p.airVersion(),p.capabilities(),p.artifacts(),List.of(updated),p.storage(),p.resources(),p.artifactRelations(),p.origins(),p.coverage(),p.uncertainties(),p.premises());
        var encoded=codec.encode(expected);
        if(!expected.equals(codec.decode(encoded)))throw new AssertionError("fit_decimal/wrap_integer/abs/mul/conversions roundtrip");
        var fitWire=new String(encoded,StandardCharsets.UTF_8);
        for(var malformed:List.of(fitWire.replace("\"digits\":\"5\"","\"digits\":\"0\""),
                fitWire.replace("\"scale\":\"-2\"","\"scale\":\"-02\""),
                fitWire.replace("\"absolute\":true","\"absolute\":null"),
                fitWire.replace("\"width\":\"16\"","\"width\":\"0\""),
                fitWire.replace("\"signed\":true","\"signed\":null"))) {
            try{codec.decode(malformed.getBytes(StandardCharsets.UTF_8));throw new AssertionError("malformed numeric fit accepted");}
            catch(AirJsonException e){if(e.code()!=AirJsonException.Code.INPUT_ERROR)throw e;}
        }
        try{codec.decode(fitWire.replace("\"operator\":\"to_int\"","\"operator\":\"to_decimal\"").getBytes(StandardCharsets.UTF_8));throw new AssertionError("wrong numeric argument type admitted");}
        catch(AirJsonException e){if(e.code()!=AirJsonException.Code.INVALID_IR)throw e;}
        var textNumber=new Expressions.Literal(header.apply("text-number"),new Values.TextValue("00052"));
        var fallback=new Expressions.Literal(header.apply("explicit-invalid"),new Values.IntValue(BigInteger.valueOf(7)));
        var parsed=new Expressions.ParseInteger(header.apply("parsed-number"),textNumber,fallback);
        var parseAssign=new Operations.Assign(assign.header(),assign.destination(),new Expressions.Unary(header.apply("parsed-decimal"),Expressions.UnaryOperator.TO_DECIMAL,parsed));
        var parseUnit=new Unit(unit.id(),unit.containingUnit(),unit.objects(),unit.visibleObjects(),unit.entries(),
            List.of(new Sequence(sequence.label(),List.of(parseAssign),sequence.terminator(),sequence.origin())),unit.completionPorts(),unit.body(),unit.bodyUnavailable(),unit.coverage(),unit.origin());
        var parsePublication=new Publication(p.id(),p.airVersion(),p.capabilities(),p.artifacts(),List.of(parseUnit),p.storage(),p.resources(),p.artifactRelations(),p.origins(),p.coverage(),p.uncertainties(),p.premises());
        var parseWire=new String(codec.encode(parsePublication),StandardCharsets.UTF_8);
        if(!parseWire.contains("\"kind\":\"parse_integer\"")||!parseWire.contains("\"onInvalid\":"))throw new AssertionError("explicit parse wire");
        if(!parsePublication.equals(codec.decode(parseWire.getBytes(StandardCharsets.UTF_8))))throw new AssertionError("parse_integer roundtrip");
        for(var invalid:List.of(parseWire.replace("\"kind\":\"text\",\"value\":\"00052\"","\"kind\":\"int\",\"value\":\"52\""),
                parseWire.replace("\"kind\":\"int\",\"value\":\"7\"","\"kind\":\"text\",\"value\":\"7\""))) {
            try{codec.decode(invalid.getBytes(StandardCharsets.UTF_8));throw new AssertionError("parse type mismatch accepted");}
            catch(AirJsonException e){if(e.code()!=AirJsonException.Code.INVALID_IR)throw e;}
        }
        var predicate=new Expressions.Unary(header.apply("valid-digits"),Expressions.UnaryOperator.IS_DIGITS,textNumber);
        if(!Operands.children(parsed).equals(List.of(textNumber,fallback))||!Operands.children(predicate).equals(List.of(textNumber)))throw new AssertionError("parse traversal lost operands");
        var textPublication=ScalarAssignOracle.publication(1,1);
        var textUnit=textPublication.units().getFirst();var textSequence=textUnit.sequences().getFirst();
        var textAssign=(Operations.Assign)textSequence.instructions().getFirst();
        var character=new Expressions.Literal(header.apply("fill-character"),new Values.TextValue("😀"));
        var fill=new Expressions.FillText(textAssign.value().header(),character,new BigInteger("1000000000000000000000"));
        var fillAssign=new Operations.Assign(textAssign.header(),textAssign.destination(),fill);
        var fillUnit=new Unit(textUnit.id(),textUnit.containingUnit(),textUnit.objects(),textUnit.visibleObjects(),textUnit.entries(),
            List.of(new Sequence(textSequence.label(),List.of(fillAssign),textSequence.terminator(),textSequence.origin())),textUnit.completionPorts(),textUnit.body(),textUnit.bodyUnavailable(),textUnit.coverage(),textUnit.origin());
        var fillPublication=new Publication(textPublication.id(),textPublication.airVersion(),textPublication.capabilities(),textPublication.artifacts(),List.of(fillUnit),textPublication.storage(),textPublication.resources(),textPublication.artifactRelations(),textPublication.origins(),textPublication.coverage(),textPublication.uncertainties(),textPublication.premises());
        var fillWire=new String(codec.encode(fillPublication),StandardCharsets.UTF_8);
        if(!fillPublication.equals(codec.decode(fillWire.getBytes(StandardCharsets.UTF_8)))||fillWire.length()>20000)throw new AssertionError("fill expands descriptor or loses roundtrip");
        for(var invalid:List.of(fillWire.replace("😀","AB"),fillWire.replace("😀",""))) {
            try{codec.decode(invalid.getBytes(StandardCharsets.UTF_8));throw new AssertionError("invalid fill character admitted");}
            catch(AirJsonException e){if(e.code()!=AirJsonException.Code.INVALID_IR)throw e;}
        }
        var numericHeader=new Operand.Header(new OperandId(new OperationOwner(textAssign.header().id()),"digits-input"),Operand.Role.VALUE_READ,textAssign.header().origin());
        var digits=new Expressions.IntegerDigits(textAssign.value().header(),new Expressions.Literal(numericHeader,new Values.IntValue(BigInteger.valueOf(-23))),BigInteger.valueOf(5));
        var digitAssign=new Operations.Assign(textAssign.header(),textAssign.destination(),digits);
        var digitUnit=new Unit(textUnit.id(),textUnit.containingUnit(),textUnit.objects(),textUnit.visibleObjects(),textUnit.entries(),
            List.of(new Sequence(textSequence.label(),List.of(digitAssign),textSequence.terminator(),textSequence.origin())),textUnit.completionPorts(),textUnit.body(),textUnit.bodyUnavailable(),textUnit.coverage(),textUnit.origin());
        var digitsPublication=new Publication(textPublication.id(),textPublication.airVersion(),textPublication.capabilities(),textPublication.artifacts(),List.of(digitUnit),textPublication.storage(),textPublication.resources(),textPublication.artifactRelations(),textPublication.origins(),textPublication.coverage(),textPublication.uncertainties(),textPublication.premises());
        var format=new Expressions.FormatDecimal(digits.header(),
            new Expressions.Literal(numericHeader,new Values.DecimalValue(BigInteger.valueOf(-123456),BigInteger.valueOf(3))),
            List.of(new DecimalText.Part(DecimalText.Kind.SIGN,BigInteger.ONE,"+","-"),
                new DecimalText.Part(DecimalText.Kind.DIGITS,BigInteger.valueOf(4),"",""),
                new DecimalText.Part(DecimalText.Kind.RADIX,BigInteger.ONE,".",""),
                new DecimalText.Part(DecimalText.Kind.DIGITS,BigInteger.TWO,"","")));
        var formattedAssign=new Operations.Assign(textAssign.header(),textAssign.destination(),format);
        var formattedUnit=new Unit(textUnit.id(),textUnit.containingUnit(),textUnit.objects(),textUnit.visibleObjects(),textUnit.entries(),
            List.of(new Sequence(textSequence.label(),List.of(formattedAssign),textSequence.terminator(),textSequence.origin())),textUnit.completionPorts(),textUnit.body(),textUnit.bodyUnavailable(),textUnit.coverage(),textUnit.origin());
        var formattedPublication=new Publication(textPublication.id(),textPublication.airVersion(),textPublication.capabilities(),textPublication.artifacts(),List.of(formattedUnit),textPublication.storage(),textPublication.resources(),textPublication.artifactRelations(),textPublication.origins(),textPublication.coverage(),textPublication.uncertainties(),textPublication.premises());
        var formattedWire=new String(codec.encode(formattedPublication),StandardCharsets.UTF_8);
        if(!formattedPublication.equals(codec.decode(formattedWire.getBytes(StandardCharsets.UTF_8))))throw new AssertionError("format_decimal roundtrip");
        if(!DecimalText.describe(format.parts()).equals(new DecimalText.Shape(BigInteger.valueOf(6),BigInteger.TWO,BigInteger.valueOf(8))))throw new AssertionError("typed format extent");
        for(var bad:List.of(formattedWire.replace("\"count\":\"4\"","\"count\":\"0\""),
                formattedWire.replace("\"kind\":\"RADIX\"","\"kind\":\"PICTURE\""))) {
            try{codec.decode(bad.getBytes(StandardCharsets.UTF_8));throw new AssertionError("invalid decimal format admitted");}
            catch(AirJsonException e){if(e.code()!=AirJsonException.Code.INPUT_ERROR)throw e;}
        }
        new Expressions.FormatDecimal(format.header(),format.value(),List.of(
            new DecimalText.Part(DecimalText.Kind.DIGITS,new BigInteger("1000000000000000000000"),"","")));
        var digitsWire=new String(codec.encode(digitsPublication),StandardCharsets.UTF_8);
        if(!digitsPublication.equals(codec.decode(digitsWire.getBytes(StandardCharsets.UTF_8))))throw new AssertionError("integer_digits roundtrip");
        for(var bad:List.of(digitsWire.replace("\"digits\":\"5\"","\"digits\":\"0\""),digitsWire.replace("\"digits\":\"5\"","\"digits\":\"05\""))) {
            try{codec.decode(bad.getBytes(StandardCharsets.UTF_8));throw new AssertionError("malformed integer_digits admitted");}
            catch(AirJsonException e){if(e.code()!=AirJsonException.Code.INPUT_ERROR)throw e;}
        }
        try{codec.decode(digitsWire.replace("\"kind\":\"int\",\"value\":\"-23\"","\"kind\":\"text\",\"value\":\"23\"").getBytes(StandardCharsets.UTF_8));throw new AssertionError("text argument admitted");}
        catch(AirJsonException e){if(e.code()!=AirJsonException.Code.INVALID_IR)throw e;}
        new Expressions.FitDecimal(header.apply("large-descriptor"),assign.value(),new BigInteger("1000000000000000000000"),BigInteger.ZERO,false);
        for(var bad:new String[]{wire.replace("\"scale\":\"2\"","\"scale\":\"-2\""),wire.replace("\"coefficient\":\"-12345\"","\"coefficient\":\"-012345\"")}) {
            try{codec.decode(bad.getBytes(StandardCharsets.UTF_8));throw new AssertionError("malformed decimal accepted");}
            catch(AirJsonException e){if(e.code()!=AirJsonException.Code.INPUT_ERROR)throw e;}
        }
    }
}
