package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.*;
import java.util.*;
import static io.github.gustavo2358.air.json.InvokeOracle.*;
import static io.github.gustavo2358.air.json.InvokeChecks.*;
import static io.github.gustavo2358.air.json.Json.object;
import static io.github.gustavo2358.air.json.AirJsonException.Code.*;

/** RF-W3: existing binding §9, independently authored argument/result wire forms. */
final class InvocationOperandsChecks {
    private InvocationOperandsChecks() { }
    public static void main(String[] args) { run(); }
    static void run() {
        for (boolean computed : List.of(false,true)) {
            var i=invoke(computed,false);
            var args=List.<Interactions.Argument>of(
                new Interactions.ValueArgument(new Expressions.Literal(operand("arg-value",Operand.Role.ARGUMENT_VALUE,"expression"),new Values.TextValue("DATA"))),
                new Interactions.CopyArgument(new Expressions.Unknown(operand("arg-copy",Operand.Role.ARGUMENT_VALUE,"expression"),Types.known(Types.Builtin.TEXT),List.of(),Scopes.NoMemory.INSTANCE,gap("facts"))),
                new Interactions.ReferenceArgument(new Places.ObjectPlace(operand("arg-reference",Operand.Role.ARGUMENT_REFERENCE,"place"),OTHER)));
            var result=new Places.ObjectPlace(operand("result",Operand.Role.RESULT_TARGET,"place"),OTHER);
            var open=new Interactions.UnknownRemainder(gap("signature"));
            var signature=new Interactions.ExternalSignature(new Interactions.Signature(new Interactions.ParameterInventory(List.of(),open),new Interactions.ResultInventory(List.of(),open),origin("signature")));
            var p=publication(new Operations.Invoke(i.header(),i.action(),i.target(),args,List.of(result),signature,i.effectOperands(),i.effectBound(),i.outcomes(),i.contract()));
            roundTrip(p);
            var tree=tree(p); var term=at(tree,OP);
            equal(Json.value("value"),at(term,"arguments.0.kind"));
            equal(object("kind","reference","place",object("kind","object","header",operandWire("arg-reference","ARGUMENT_REFERENCE","place"),"object",ownedId("object","other-cell"))),at(term,"arguments.2"));
            equal(Json.value("copy"),at(term,"arguments.1.kind"));
            equal(Json.value("unknown"),at(term,"arguments.1.value.kind"));
            equal(new Json.Arr(List.of(object("kind","object","header",operandWire("result","RESULT_TARGET","place"),"object",ownedId("object","other-cell")))),at(term,"results"));
            equal(p,new AirJson().decode(wire(edit(tree,OP+".arguments.2",at(term,"arguments.2")))));
            invalid(edit(tree,OP+".arguments.0.value.header.role",Json.value("CALL_TARGET")),"I-11");
            invalid(edit(tree,OP+".results.0.header.role",Json.value("VALUE_READ")),"I-11");
            failure(INPUT_ERROR,()->new AirJson().decode(wire(edit(tree,OP+".arguments.0.kind",Json.value("unknown-mode")))));
            failure(INPUT_ERROR,()->new AirJson().decode(wire(edit(tree,OP+".arguments.0",object("kind","value","place",at(term,"results.0"))))));
            // The destination may have unknown binding: retain its bound and uncertainty verbatim.
            var u=p.units().getFirst();var objects=u.objects().stream().map(o->o.id().equals(OTHER)?new Memory.ObjectDeclaration(o.id(),o.displayName(),o.typeRef(),new Memory.UnknownBinding(new Scopes.AllMemory(PUB,true),gap("facts")),o.visibility(),o.origin(),o.coverage(),o.precision()):o).toList();
            roundTrip(withUnit(p,new io.github.gustavo2358.air.model.Unit(u.id(),u.containingUnit(),objects,u.visibleObjects(),u.entries(),u.sequences(),u.completionPorts(),u.body(),u.bodyUnavailable(),u.coverage(),u.origin())));
        }
    }
}
