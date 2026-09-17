package io.github.gustavo2358.air.json;
import io.github.gustavo2358.air.model.*;
import java.util.*;
import static io.github.gustavo2358.air.json.InvokeOracle.*;
import static io.github.gustavo2358.air.json.InvokeChecks.*;
import static io.github.gustavo2358.air.json.Json.object;
import static io.github.gustavo2358.air.json.AirJsonException.Code.*;
/** Binding §9: otherwise and five distinct outcome keys; no evaluation or producer certification. */
final class OutcomeEffectsChecks {
    public static void main(String[] args){run();}
    static void run(){
        var i=invoke(false,true);var otherwise=new Interactions.ForeignEffects(Scopes.NoMemory.INSTANCE,Scopes.NoMemory.INSTANCE,List.of());
        var keys=List.<Control.OutcomeKey>of(Control.NormalOutcome.INSTANCE,new Control.ExceptionOutcome("fixture.failure"),Control.OtherExceptionOutcome.INSTANCE,Control.HaltOutcome.INSTANCE,Control.DivergeOutcome.INSTANCE);
        var expected=List.of(object("kind","normal"),object("kind","exception","tag","fixture.failure"),object("kind","other_exception"),object("kind","halt"),object("kind","diverge"));
        var values=keys.stream().map(k->new Interactions.OutcomeEffects(k,i.effectBound().otherwise())).toList();
        var p=publication(copy(i,i.target(),external(i),new Interactions.EffectBound(otherwise,values),i.outcomes(),i.contract()));
        obligation(p);roundTrip(p);var t=tree(p);
        for(int n=0;n<5;n++){equal(expected.get(n),at(t,OP+".effectBound.perOutcome."+n+".outcome"));equal(at(t,OP+".effectBound.perOutcome.0.effects"),at(t,OP+".effectBound.perOutcome."+n+".effects"));}
        equal(object("reads",object("kind","none"),"writes",object("kind","none"),"mustOverwrite",new Json.Arr(List.of())),at(t,OP+".effectBound.otherwise"));
        failure(IMPLEMENTATION_LIMIT,()->new AirJson().decode(wire(edit(t,OP+".effectBound.perOutcome.1.outcome.tag",Json.value(" ")))));
        invalid(edit(t,OP+".effectBound.perOutcome.1.outcome",object("kind","normal")),"I-60");
        failure(INPUT_ERROR,()->new AirJson().decode(wire(edit(t,OP+".effectBound.perOutcome.0.outcome",object("kind","unknown")))));
        failure(INPUT_ERROR,()->new AirJson().decode(wire(edit(t,OP+".effectBound.perOutcome.0.outcome",object("kind","normal","tag","extra")))));
        failure(INPUT_ERROR,()->new AirJson().decode(wire(edit(t,OP+".effectBound.perOutcome.0",object("outcome",object("kind","normal"))))));
        invalid(edit(t,OP+".effectBound.perOutcome.0.effects.mustOverwrite.0",operandIdWire("missing")),"I-02");
    }
}
