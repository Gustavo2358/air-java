package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;

/** W3-R1: positive logical storage and executable location bounds. */
public final class BoundedBindingChecks {
    private BoundedBindingChecks() { }
    static void run() {
        var cell=new Fixtures();var x=cell.object("x",Types.known(Types.Builtin.TEXT));
        cell.linear(cell.assign("set",x,cell.text(cell.op("set"),"value","A")));
        valid(cell,"logical Cell without byte layout");

        var storage=new Fixtures();var y=storage.object("y",Types.known(Types.Builtin.TEXT));
        var u=storage.object("u",Types.known(Types.Builtin.TEXT));
        var reason=storage.uncertainty("location","LOCATION_OPEN");
        bind(storage,u,new Memory.UnknownBinding(new Scopes.StorageMemory(List.of(((Memory.CellBinding)storage.objects.getFirst().storage()).storage())),reason));
        storage.linear(storage.assign("set",u,storage.text(storage.op("set"),"value","A")));
        valid(storage,"bounded StorageMemory reaches Cell");

        var objectBound=new Fixtures();var ground=objectBound.object("ground",Types.known(Types.Builtin.TEXT));
        var target=objectBound.object("target",Types.known(Types.Builtin.TEXT));
        var boundReason=objectBound.uncertainty("location","LOCATION_OPEN");
        bind(objectBound,target,new Memory.UnknownBinding(new Scopes.ObjectsMemory(List.of(ground)),boundReason));
        objectBound.linear(objectBound.assign("set",target,objectBound.text(objectBound.op("set"),"value","A")));
        valid(objectBound,"object scope reaches positive Cell");

        var all=new Fixtures();var allTarget=all.object("all",Types.known(Types.Builtin.TEXT));
        bind(all,allTarget,new Memory.UnknownBinding(new Scopes.AllMemory(all.pub,false),all.uncertainty("all-location","LOCATION_OPEN")));
        all.linear(all.assign("set",allTarget,all.text(all.op("set"),"value","A")));
        valid(all,"explicit AllMemory remains valid");

        var visible=new Fixtures();var visibleTarget=visible.object("visible",Types.known(Types.Builtin.TEXT));
        bind(visible,visibleTarget,new Memory.UnknownBinding(new Scopes.VisibleMemory(visible.unit,false),visible.uncertainty("visible-location","LOCATION_OPEN")));
        visible.linear(visible.assign("set",visibleTarget,visible.text(visible.op("set"),"value","A")));
        valid(visible,"explicit VisibleMemory remains valid");

        var self=new Fixtures();var selfTarget=self.object("self",Types.known(Types.Builtin.TEXT));
        bind(self,selfTarget,new Memory.UnknownBinding(new Scopes.ObjectsMemory(List.of(selfTarget)),self.uncertainty("self-location","LOCATION_OPEN")));
        self.linear(self.assign("set",selfTarget,self.text(self.op("set"),"value","A")));
        invalid(self,"I-13","executable self-cycle");

        var pair=new Fixtures();var first=pair.object("first",Types.known(Types.Builtin.TEXT));
        var second=pair.object("second",Types.known(Types.Builtin.TEXT));
        var pairReason=pair.uncertainty("pair-location","LOCATION_OPEN");
        bind(pair,first,new Memory.UnknownBinding(new Scopes.ObjectsMemory(List.of(second)),pairReason));
        bind(pair,second,new Memory.UnknownBinding(new Scopes.ObjectsMemory(List.of(first)),pairReason));
        pair.linear(pair.assign("set",first,pair.text(pair.op("set"),"value","A")));
        invalid(pair,"I-13","executable two-object cycle");

        var scopeSelf=new Fixtures();var scopeSelfTarget=scopeSelf.object("self",Types.known(Types.Builtin.TEXT));
        var scopeSelfReason=scopeSelf.uncertainty("self-location","LOCATION_OPEN");
        bind(scopeSelf,scopeSelfTarget,new Memory.UnknownBinding(new Scopes.ObjectsMemory(List.of(scopeSelfTarget)),scopeSelfReason));
        scopeSelf.linear(havoc(scopeSelf,"havoc",new Scopes.ObjectsMemory(List.of(scopeSelfTarget)),scopeSelfReason));
        invalid(scopeSelf,"I-13","executable scope-only self-cycle");

        var scopePair=new Fixtures();var scopeFirst=scopePair.object("first",Types.known(Types.Builtin.TEXT));
        var scopeSecond=scopePair.object("second",Types.known(Types.Builtin.TEXT));
        var scopePairReason=scopePair.uncertainty("pair-location","LOCATION_OPEN");
        bind(scopePair,scopeFirst,new Memory.UnknownBinding(new Scopes.ObjectsMemory(List.of(scopeSecond)),scopePairReason));
        bind(scopePair,scopeSecond,new Memory.UnknownBinding(new Scopes.ObjectsMemory(List.of(scopeFirst)),scopePairReason));
        scopePair.linear(havoc(scopePair,"havoc",new Scopes.ObjectsMemory(List.of(scopeFirst)),scopePairReason));
        invalid(scopePair,"I-13","executable scope-only two-object cycle");

        var scopeGrounded=new Fixtures();var scopeCell=scopeGrounded.object("cell",Types.known(Types.Builtin.TEXT));
        var scopeTarget=scopeGrounded.object("target",Types.known(Types.Builtin.TEXT));
        var scopeReason=scopeGrounded.uncertainty("location","LOCATION_OPEN");
        bind(scopeGrounded,scopeTarget,new Memory.UnknownBinding(new Scopes.ObjectsMemory(List.of(scopeCell)),scopeReason));
        scopeGrounded.linear(havoc(scopeGrounded,"havoc",new Scopes.ObjectsMemory(List.of(scopeTarget)),scopeReason));
        valid(scopeGrounded,"executable scope-only grounded object bound");

        var broadAll=new Fixtures();var allReason=broadAll.uncertainty("all","LOCATION_OPEN");
        broadAll.linear(havoc(broadAll,"havoc",new Scopes.AllMemory(broadAll.pub,false),allReason));
        valid(broadAll,"explicit executable AllMemory remains valid");

        var broadVisible=new Fixtures();var visibleReason=broadVisible.uncertainty("visible","LOCATION_OPEN");
        broadVisible.linear(havoc(broadVisible,"havoc",new Scopes.VisibleMemory(broadVisible.unit,false),visibleReason));
        valid(broadVisible,"explicit executable VisibleMemory remains valid");

        var revisit=new Fixtures();var revisitObject=revisit.object("cell",Types.known(Types.Builtin.TEXT));
        var revisitReason=revisit.uncertainty("revisit","LOCATION_OPEN");
        var revisitScope=new Scopes.ObjectsMemory(List.of(revisitObject));
        revisit.linear(havoc(revisit,"havoc",new Scopes.MemoryUnion(List.of(revisitScope,revisitScope)),revisitReason));
        valid(revisit,"executable union revisit is not a cycle");

        for(boolean reads:List.of(false,true)) {
            var opaqueCycle=cycle();var f=opaqueCycle.fixture();var scope=new Scopes.WithinMemory(new Scopes.ObjectsMemory(List.of(opaqueCycle.object())));
            var memory=new Envelopes.MemoryEnvelope(List.of(),reads?scope:Scopes.NoMemory.INSTANCE,
                    List.of(),reads?Scopes.NoMemory.INSTANCE:scope,List.of());
            var envelope=new Envelopes.Envelope(memory,
                    new Control.ControlEnvelope(List.of(Control.HaltAlternative.INSTANCE),Scopes.NoControl.INSTANCE),
                    new Envelopes.DependencyEnvelope(List.of(),Scopes.NoResources.INSTANCE));
            var id=f.op(reads?"opaque-read":"opaque-write");
            f.sequence("start",List.of(),new Operations.Opaque(f.header(id,opaqueCycle.reason()),
                    "fixture.opaque",List.of(),List.of(),envelope));
            invalid(f,"I-13","opaque executable other "+(reads?"read":"write")+" cycle");
        }

        var foreignCycle=cycle();var foreign=foreignCycle.fixture();var foreignScope=new Scopes.WithinMemory(
                new Scopes.ObjectsMemory(List.of(foreignCycle.object())));
        var foreignId=foreign.op("call");
        foreign.sequence("start",List.of(),new Operations.Invoke(foreign.header(foreignId),"call",
                new Interactions.LiteralTarget("program","fixture","PROG",Interactions.ExactName.INSTANCE,foreign.origin),
                List.of(),List.of(),new Interactions.ExternalSignature(foreign.signature(List.of(),List.of())),List.of(),
                new Interactions.EffectBound(new Interactions.ForeignEffects(Scopes.NoMemory.INSTANCE,foreignScope,List.of()),List.of()),
                new Control.InvocationOutcomes(List.of(new Control.Normal(foreign.label("end"))),Scopes.NoControl.INSTANCE),
                new Interactions.UnknownContract(foreign.uncertainty("contract","CONTRACT_UNKNOWN"))));
        foreign.sequence("end",List.of(),foreign.halt("stop"));
        invalid(foreign,"I-13","foreign executable write cycle");

        var unknownCycle=cycle();var unknown=unknownCycle.fixture();
        var knownTarget=unknown.object("known",Types.known(Types.Builtin.TEXT));
        var unknownId=unknown.op("set");
        var unknownValue=new Expressions.Unknown(unknown.operand(unknownId,"value",Operand.Role.VALUE_READ),
                Types.known(Types.Builtin.TEXT),List.of(),
                new Scopes.WithinMemory(new Scopes.ObjectsMemory(List.of(unknownCycle.object()))),
                unknown.uncertainty("value","VALUE_UNKNOWN"));
        unknown.linear(unknown.assign("set",knownTarget,unknownValue));
        invalid(unknown,"I-13","unknown expression remaining reads cycle");

        var choiceCycle=cycle();var choice=choiceCycle.fixture();
        var candidate=choice.object("candidate",Types.known(Types.Builtin.TEXT));
        var choiceId=choice.op("choice");
        var choicePlace=new Places.Choice(choice.operand(choiceId,"choice",Operand.Role.VALUE_READ),
                List.of(choice.place(choiceId,"candidate",candidate,Operand.Role.VALUE_READ)),
                new Scopes.WithinMemory(new Scopes.ObjectsMemory(List.of(choiceCycle.object()))),
                Types.known(Types.Builtin.TEXT));
        var choiceEnvelope=new Envelopes.Envelope(
                new Envelopes.MemoryEnvelope(List.of(),Scopes.NoMemory.INSTANCE,List.of(),Scopes.NoMemory.INSTANCE,List.of()),
                new Control.ControlEnvelope(List.of(Control.HaltAlternative.INSTANCE),Scopes.NoControl.INSTANCE),
                new Envelopes.DependencyEnvelope(List.of(),Scopes.NoResources.INSTANCE));
        choice.sequence("start",List.of(),new Operations.Opaque(choice.header(choiceId,choiceCycle.reason()),
                "fixture.choice",List.of(choicePlace),List.of(),choiceEnvelope));
        invalid(choice,"I-13","choice executable remainder cycle");

        var duplicate=new Fixtures();var known=duplicate.object("known",Types.known(Types.Builtin.TEXT));
        var repeated=duplicate.object("repeated",Types.known(Types.Builtin.TEXT));
        var same=new Scopes.ObjectsMemory(List.of(known));
        bind(duplicate,repeated,new Memory.UnknownBinding(new Scopes.MemoryUnion(List.of(same,same)),duplicate.uncertainty("repeat-location","LOCATION_OPEN")));
        duplicate.linear(duplicate.assign("set",repeated,duplicate.text(duplicate.op("set"),"value","A")));
        valid(duplicate,"repeated resolved member is not a cycle");

        var nominal=new Fixtures();var nominalObject=nominal.object("nominal",Types.known(Types.Builtin.TEXT));
        bind(nominal,nominalObject,new Memory.UnknownBinding(new Scopes.ObjectsMemory(List.of(nominalObject)),nominal.uncertainty("nominal-location","LOCATION_OPEN")));
        nominal.sequence("start",List.of(),nominal.halt("stop"));
        valid(nominal,"unused nominal cycle remains diagnostic");
    }
    private static Operations.HavocMay havoc(Fixtures f,String name,Scopes.MemoryScope scope,UncertaintyId reason) {
        var id=f.op(name);return new Operations.HavocMay(f.header(id,reason),scope,reason);
    }
    private record Cycle(Fixtures fixture,ObjectId object,UncertaintyId reason) { }
    private static Cycle cycle() {
        var f=new Fixtures();var object=f.object("cycle",Types.known(Types.Builtin.TEXT));
        var reason=f.uncertainty("location","LOCATION_OPEN");
        bind(f,object,new Memory.UnknownBinding(new Scopes.ObjectsMemory(List.of(object)),reason));
        return new Cycle(f,object,reason);
    }
    private static void bind(Fixtures f,ObjectId id,Memory.Binding binding) {
        for(int i=0;i<f.objects.size();i++)if(f.objects.get(i).id().equals(id)) {
            var o=f.objects.get(i);f.objects.set(i,new Memory.ObjectDeclaration(o.id(),o.displayName(),o.typeRef(),binding,o.visibility(),o.origin(),o.coverage(),o.precision()));return;
        }
        throw new AssertionError("missing object");
    }
    private static void valid(Fixtures f,String why) {
        var result=AirValidator.validate(f.build());if(!result.isStructurallyValid())throw new AssertionError(why+": "+result);
    }
    private static void invalid(Fixtures f,String rule,String why) {
        var result=AirValidator.validate(f.build());
        if(result.status()!=ValidationResult.Status.INVALID_IR||result.issues().stream().noneMatch(i->i.rule().equals(rule)))
            throw new AssertionError(why+": "+result);
    }
}
