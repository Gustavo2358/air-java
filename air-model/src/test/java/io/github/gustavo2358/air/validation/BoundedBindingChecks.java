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
