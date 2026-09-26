package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.*;
import java.util.*;
import static io.github.gustavo2358.air.json.Json.*;
import static io.github.gustavo2358.air.json.PossibleInitialChecks.*;

/** Independent binding shapes for a coverage-bearing nop and independent entry bases. */
public final class PositiveProjectionChecks {
    private PositiveProjectionChecks() { }
    public static void main(String[] args) { run(); System.out.println("POSITIVE_PROJECTION_JSON=PASS"); }
    static void run() {
        var codec=new AirJson();
        var tree=Json.parse(codec.encode(ScalarAssignOracle.publication()),AirJson.Limits.defaults());
        var original=at(tree,"publication","units","0","sequences","0","instructions","0");
        var header=set(at(original,"header"),new Text("ABSTRACTED"),"coverage");
        var nop=new Obj(Map.of("kind",new Text("nop"),"header",header));
        var wire=set(tree,nop,"publication","units","0","sequences","0","instructions","0");
        var restored=codec.decode(Json.write(wire,AirJson.Limits.defaults()));
        var instruction=restored.units().getFirst().sequences().getFirst().instructions().getFirst();
        if(!(instruction instanceof Operations.Nop)||!instruction.header().uncertainties().equals(ScalarAssignOracle.publication().units().getFirst().sequences().getFirst().instructions().getFirst().header().uncertainties()))
            throw new AssertionError("nop must preserve diagnostic header without fake effects/operands");
        if(!restored.equals(codec.decode(codec.encode(restored))))throw new AssertionError("nop lost on roundtrip");
        var malformed=new LinkedHashMap<>(nop.fields());malformed.put("destination",at(original,"destination"));
        rejected(set(wire,new Obj(malformed),"publication","units","0","sequences","0","instructions","0"));
        rejected(set(wire,new Text("missing"),"publication","units","0","sequences","0","instructions","0","header","uncertainties","0","localId"));

        // Existing v1 initial candidates on one cell + another independent cell, without a premise.
        tree=fixture();
        var storage=at(tree,"publication","storage","0");
        var otherStorage=set(storage,new Text("other-base"),"header","id","localId");
        tree=set(tree,new Arr(List.of(storage,otherStorage)),"publication","storage");
        var object=at(tree,"publication","units","0","objects","0");
        var otherObject=set(object,new Text("other-object"),"id","localId");
        otherObject=set(otherObject,new Text("other-base"),"storage","storage","localId");
        tree=set(tree,new Arr(List.of(object,otherObject)),"publication","units","0","objects");
        var first=at(tree,"publication","units","0","entries","0","state","conditions","0");
        var second=set(first,new Text("other-object"),"place","object","localId");
        second=set(second,new Text("other-place"),"place","header","id","localId");
        second=set(second,new Text("other-literal-a"),"value","candidates","0","header","id","localId");
        second=set(second,new Text("other-literal-b"),"value","candidates","1","header","id","localId");
        for(var conditions:List.of(List.of(first,second),List.of(second,first))) {
            wire=set(tree,new Arr(conditions),"publication","units","0","entries","0","state","conditions");
            restored=codec.decode(Json.write(wire,AirJson.Limits.defaults()));
            if(!restored.premises().isEmpty()||!restored.equals(codec.decode(codec.encode(restored))))throw new AssertionError("positive bases roundtrip");
            rejected(set(wire,new Text("missing-base"),"publication","units","0","objects","1","storage","storage","localId"));
        }
    }
}
