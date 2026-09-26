package io.github.gustavo2358.air.json;
import io.github.gustavo2358.air.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class TextPredicateChecks {
    private TextPredicateChecks() { }
    public static void main(String[] args){run();System.out.println("TEXT_PREDICATE_JSON=PASS");}
    static void run() {
        var p=W2cOracle.publication("full");var u=p.units().getFirst();var s=u.sequences().getFirst();var old=(Operations.Branch)s.terminator();
        java.util.function.Function<String,Operand.Header> h=id->W2cOracle.operand("if",id,Operand.Role.VALUE_READ,"predicate");
        var left=new Expressions.Literal(h.apply("left"),new Values.TextValue(" A😀 "));
        var right=new Expressions.Literal(h.apply("right"),new Values.TextValue(" A😀 "));
        var eq=new Expressions.Binary(h.apply("eq"),Expressions.BinaryOperator.EQ,left,right);
        var ne=new Expressions.Binary(h.apply("ne"),Expressions.BinaryOperator.NE,new Expressions.Literal(h.apply("a"),new Values.TextValue("A")),new Expressions.Literal(h.apply("b"),new Values.TextValue("B")));
        var and=new Expressions.Binary(h.apply("and"),Expressions.BinaryOperator.AND,eq,ne);
        var or=new Expressions.Binary(h.apply("or"),Expressions.BinaryOperator.OR,and,old.predicate());
        var not=new Expressions.Unary(W2cOracle.operand("if","not",Operand.Role.PREDICATE,"predicate"),Expressions.UnaryOperator.NOT,or);
        var sequences=new ArrayList<>(u.sequences());sequences.set(0,new Sequence(s.label(),s.instructions(),new Operations.Branch(old.header(),not,old.trueDestination(),old.falseDestination()),s.origin()));
        var expected=W2cOracle.copy(p,List.of(W2cOracle.sequences(u,sequences)),p.storage(),p.premises());
        var codec=new AirJson();var bytes=codec.encode(expected);var restored=codec.decode(bytes);
        if(!expected.equals(restored)||!Arrays.equals(bytes,codec.encode(restored)))throw new AssertionError("predicate facts changed in transport");
        String wire=new String(bytes,StandardCharsets.UTF_8);
        for(String token:List.of("eq","ne","and","or","not"))if(!wire.contains("\"operator\":\""+token+"\""))throw new AssertionError("normative token "+token);
        for(String bad:List.of(wire.replace("\"operator\":\"not\"","\"operator\":\"NOT\""),wire.replace("\"argument\":","\"unknownArgument\":"),wire.replace("\"operator\":\"eq\"","\"operator\":\"bogus\""))) {
            try{codec.decode(bad.getBytes(StandardCharsets.UTF_8));throw new AssertionError("malformed predicate accepted");}
            catch(AirJsonException e){if(e.code()!=AirJsonException.Code.INPUT_ERROR)throw new AssertionError("shape diagnostic",e);}
        }
        try{codec.decode(wire.replace("\"operator\":\"eq\"","\"operator\":\"and\"").getBytes(StandardCharsets.UTF_8));throw new AssertionError("text used as bool");}
        catch(AirJsonException e){if(e.code()!=AirJsonException.Code.INVALID_IR)throw new AssertionError("type diagnostic",e);}
    }
}
