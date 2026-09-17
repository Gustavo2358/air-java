package io.github.gustavo2358.air.json;
import io.github.gustavo2358.air.model.*;
import java.util.*;
import java.math.BigInteger;
import static io.github.gustavo2358.air.json.InvokeOracle.*;
import static io.github.gustavo2358.air.json.InvokeChecks.*;
import static io.github.gustavo2358.air.json.Json.object;
import static io.github.gustavo2358.air.json.AirJsonException.Code.*;

/** Binding pin §9, §10.4: explicit known external parameter transport, no language semantics. */
final class SignatureParameterChecks {
    private SignatureParameterChecks() { }
    public static void main(String[] args){run();}
    static void run() {
        for(var mode:List.of(Interactions.PassingMode.VALUE,Interactions.PassingMode.REFERENCE,Interactions.PassingMode.COPY)) {
            var i=invoke(false,false);
            Interactions.Argument arg=switch(mode) {
                case VALUE->new Interactions.ValueArgument(new Expressions.Literal(operand("argument",Operand.Role.ARGUMENT_VALUE,"expression"),new Values.TextValue("R001")));
                case COPY->new Interactions.CopyArgument(new Expressions.Literal(operand("argument",Operand.Role.ARGUMENT_VALUE,"expression"),new Values.TextValue("R001")));
                case REFERENCE->new Interactions.ReferenceArgument(new Places.ObjectPlace(operand("argument",Operand.Role.ARGUMENT_REFERENCE,"place"),OTHER));
            };
            var parameter=new Interactions.Parameter(BigInteger.ZERO,new Interactions.KnownMode(mode),Types.known(Types.Builtin.TEXT),Interactions.ExternalBinding.INSTANCE,origin("signature"));
            var signature=new Interactions.ExternalSignature(new Interactions.Signature(new Interactions.ParameterInventory(List.of(parameter),Interactions.NoRemainder.INSTANCE),new Interactions.ResultInventory(List.of(),Interactions.NoRemainder.INSTANCE),origin("signature")));
            var p=publication(new Operations.Invoke(i.header(),i.action(),i.target(),List.of(arg),List.of(),signature,i.effectOperands(),i.effectBound(),i.outcomes(),i.contract()));
            obligation(p);roundTrip(p);var tree=tree(p);String path=OP+".signature.signature.parameters.known.0";
            String token=switch(mode){case VALUE->"VALUE";case COPY->"COPY";case REFERENCE->"REFERENCE";};
            var expected=object("position","0","mode",object("kind","known","mode",token),"typeRef",object("kind","known","type",object("kind","text")),"objectBinding",object("kind","external"),"origin",globalId("origin","signature"));
            equal(expected,at(tree,path));equal(p,new AirJson().decode(wire(edit(tree,path,expected))));
            for(var bad:List.of("-1","01","+0"))failure(INPUT_ERROR,()->new AirJson().decode(wire(edit(tree,path+".position",Json.value(bad)))));
            failure(INPUT_ERROR,()->new AirJson().decode(wire(edit(tree,path+".mode.mode",Json.value("NOT_A_MODE")))));
            failure(INPUT_ERROR,()->new AirJson().decode(wire(edit(tree,path+".objectBinding",object("kind","external","object",ownedId("object","other-cell"))))));
            invalid(edit(tree,path+".origin",globalId("origin","missing")),"I-02");
        }
    }
}
