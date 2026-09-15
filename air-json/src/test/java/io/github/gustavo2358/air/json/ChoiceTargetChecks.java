package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.validation.AirValidator;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;
import static io.github.gustavo2358.air.json.InvokeOracle.*;
import static io.github.gustavo2358.air.json.InvokeChecks.*;

/** Independent model/wire oracles for a partial target domain; no type assertion for its remainder. */
final class ChoiceTargetChecks {
    public static void main(String[] args){run();}
    static void run() {
        var type=new UncertaintyId(PUB,"choice-domain");var base=invoke(true,false);
        var candidates=List.<Place>of(new Places.ObjectPlace(operand("choice-a",Operand.Role.VALUE_READ,"place"),OBJECT),
            new Places.ObjectPlace(operand("choice-b",Operand.Role.VALUE_READ,"place"),OTHER));
        var choice=new Places.Choice(operand("name-place",Operand.Role.VALUE_READ,"place"),candidates,
            new Scopes.WithinMemory(new Scopes.AllMemory(PUB,true)),new Types.UnknownType(type));
        var target=new Interactions.ComputedTarget("program","fixture.resources",new Expressions.Read(operand("name-read",Operand.Role.CALL_TARGET,"expression"),choice),Interactions.ExactName.INSTANCE,origin("target"));
        var p=publication(new Operations.Invoke(base.header(),base.action(),target,base.arguments(),base.results(),base.signature(),base.effectOperands(),base.effectBound(),base.outcomes(),base.contract()));
        var gaps=new ArrayList<>(p.uncertainties());gaps.add(new Evidence.Uncertainty(type,"TYPE_UNKNOWN",List.of(Evidence.Dimension.VALUES),new Scopes.UnitScope(UNIT),"Remaining target domain unproved",origin("target")));
        p=new Publication(p.id(),p.airVersion(),new Capabilities.Manifest(List.of(Capabilities.TARGET_POSSIBILITIES),List.of()),p.artifacts(),p.units(),p.storage(),p.resources(),p.artifactRelations(),p.origins(),p.coverage(),gaps,p.premises());
        roundTrip(p);
        equal(Json.value("choice"),at(tree(p),OP+".target.name.place.kind"));
        equal(Json.value("unknown_type"),at(tree(p),OP+".target.name.place.typeRef.kind"));
        equal(Json.value("object"),at(tree(p),OP+".target.name.place.candidates.1.kind"));
        var noCapability=new Publication(p.id(),p.airVersion(),new Capabilities.Manifest(List.of(),List.of()),p.artifacts(),p.units(),p.storage(),p.resources(),p.artifactRelations(),p.origins(),p.coverage(),p.uncertainties(),p.premises());
        if(AirValidator.validate(noCapability).isStructurallyValid())throw new AssertionError("partial target must declare its required capability");
        var malformed=edit(tree(p),OP+".target.name.place.candidates",new Json.Arr(List.of()));
        // An empty open choice is still open, never a closed empty candidate set.
        var decoded=new AirJson().decode(wire(malformed));
        var i=(Operations.Invoke)decoded.units().getFirst().sequences().getFirst().terminator();
        var read=(Expressions.Read)((Interactions.ComputedTarget)i.target()).name();
        if(!(((Places.Choice)read.place()).remainder() instanceof Scopes.WithinMemory))throw new AssertionError("remainder lost");
    }
}
