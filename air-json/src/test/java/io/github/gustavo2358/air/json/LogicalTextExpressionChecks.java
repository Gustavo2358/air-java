package io.github.gustavo2358.air.json;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class LogicalTextExpressionChecks {
    private LogicalTextExpressionChecks() { }
    public static void main(String[] args) {run();System.out.println("LOGICAL_TEXT_JSON=PASS roundtrip/fields/bounds");}
    static void run() {
        var p=ScalarAssignOracle.publication(1,1);var u=p.units().getFirst();var s=u.sequences().getFirst();var assign=(Operations.Assign)s.instructions().getFirst();
        var fit=new Expressions.FitText(new Operand.Header(new OperandId(new OperationOwner(assign.header().id()),"fit"),Operand.Role.VALUE_READ,assign.value().header().origin()),assign.value(),BigInteger.valueOf(4)," ");
        java.util.function.Function<String,Operand.Header> h=id->new Operand.Header(new OperandId(new OperationOwner(assign.header().id()),id),Operand.Role.VALUE_READ,assign.header().origin());
        var read=new Expressions.Read(h.apply("root-read"),new Places.ObjectPlace(h.apply("root-place"),u.objects().getFirst().id()));
        var bounded=new Expressions.FitText(h.apply("bounded"),read,BigInteger.valueOf(8)," ");
        var slice=new Expressions.SliceText(h.apply("slice"),bounded,new Expressions.Literal(h.apply("start"),new Values.IntValue(BigInteger.valueOf(2))),new Expressions.Literal(h.apply("count"),new Values.IntValue(BigInteger.valueOf(4))));
        var concat=new Expressions.Binary(h.apply("concat"),Expressions.BinaryOperator.CONCAT,slice,fit);
        var changed=new Operations.Assign(assign.header(),assign.destination(),concat);
        var unit=new io.github.gustavo2358.air.model.Unit(u.id(),u.containingUnit(),u.objects(),u.visibleObjects(),u.entries(),List.of(new Sequence(s.label(),List.of(changed),s.terminator(),s.origin())),u.completionPorts(),u.body(),u.bodyUnavailable(),u.coverage(),u.origin());
        var expected=new Publication(p.id(),p.airVersion(),p.capabilities(),p.artifacts(),List.of(unit),p.storage(),p.resources(),p.artifactRelations(),p.origins(),p.coverage(),p.uncertainties(),p.premises());
        var codec=new AirJson();var bytes=codec.encode(expected);var restored=codec.decode(bytes);
        if(!expected.equals(restored)||!Arrays.equals(bytes,codec.encode(restored)))throw new AssertionError("fit facts must roundtrip exactly");
        var text=new String(bytes,StandardCharsets.UTF_8);
        if(!text.contains("\"kind\":\"fit_text\"")||!text.contains("\"length\":\"4\"")||!text.contains("\"pad\":\" \""))throw new AssertionError("normative fit fields");
        if(!text.contains("\"operator\":\"concat\"")||!text.contains("\"kind\":\"slice_text\""))throw new AssertionError("normative composition tokens");
        for(var invalid:List.of(text.replace("\"value\":\"2\"","\"value\":\"-1\""),text.replace("\"value\":\"4\"","\"value\":\"40\""))) {
            try {codec.decode(invalid.getBytes(StandardCharsets.UTF_8));throw new AssertionError("invalid slice bounds accepted");}
            catch(AirJsonException e){if(e.code()!=AirJsonException.Code.INVALID_IR)throw new AssertionError("bounds require INVALID_IR",e);}
        }
        for(String bad:List.of(text.replace("\"operator\":\"concat\"","\"operator\":\"CONCAT\""),text.replace("\"length\":\"4\"","\"length\":\"04\""),text.replace("\"length\":\"4\"","\"length\":\"-1\""),text.replace("\"pad\":\" \"","\"pad\":\"AB\""),text.replace("\"pad\":\" \"","\"pad\":null"),text.replace("\"kind\":\"fit_text\"","\"kind\":\"fit_text\",\"extra\":null"))) {
            try{codec.decode(bad.getBytes(StandardCharsets.UTF_8));throw new AssertionError("malformed fit accepted");}catch(AirJsonException e){if(e.code()!=AirJsonException.Code.INPUT_ERROR)throw new AssertionError("shape must reject as INPUT_ERROR",e);}
        }
    }
}
