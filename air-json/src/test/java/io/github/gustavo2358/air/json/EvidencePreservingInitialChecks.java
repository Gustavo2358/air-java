package io.github.gustavo2358.air.json;

import java.util.*;
import static io.github.gustavo2358.air.json.Json.*;
import static io.github.gustavo2358.air.json.PossibleInitialChecks.*;

/** Independent v2 wire oracle: coexistence is not a claim of simultaneous exact contents. */
public final class EvidencePreservingInitialChecks {
    private EvidencePreservingInitialChecks() { }
    public static void main(String[] args) { run(); System.out.println("EP_INITIAL_JSON=PASS"); }
    static void run() {
        var tree=fixture();
        var required=new ArrayList<>(((Arr)at(tree,"publication","capabilities","required")).values());
        required.replaceAll(c->new Text("entry.possibilities").equals(at(c,"name"))?set(c,new Text("2"),"version"):c);
        tree=set(tree,new Arr(required),"publication","capabilities","required");
        var first=at(tree,"publication","units","0","entries","0","state","conditions","0");
        var second=set(first,new Text("second-place"),"place","header","id","localId");
        second=set(second,new Text("second-a"),"value","candidates","0","header","id","localId");
        second=set(second,new Text("second-b"),"value","candidates","1","header","id","localId");
        second=set(second,new Text("IJKL"),"value","candidates","0","value","value");
        for(var conditions:List.of(List.of(first,second),List.of(second,first))) {
            var wire=set(tree,new Arr(conditions),"publication","units","0","entries","0","state","conditions");
            var codec=new AirJson();var decoded=codec.decode(Json.write(wire,AirJson.Limits.defaults()));
            if(!decoded.equals(codec.decode(codec.encode(decoded))))throw new AssertionError("v2 possible entries changed on roundtrip");
            var legacy=new ArrayList<>(required);
            legacy.replaceAll(c->new Text("entry.possibilities").equals(at(c,"name"))?set(c,new Text("1"),"version"):c);
            rejected(set(wire,new Arr(legacy),"publication","capabilities","required"));
        }
        var unsupported=new ArrayList<>(required);
        unsupported.replaceAll(c->new Text("entry.possibilities").equals(at(c,"name"))?set(c,new Text("3"),"version"):c);
        rejected(set(tree,new Arr(unsupported),"publication","capabilities","required"));
        rejected(set(tree,new Arr(List.of()),"publication","units","0","entries","0","state","conditions","0","value","candidates"));
        var missing=new LinkedHashMap<>(((Obj)at(first,"value")).fields());missing.remove("remainder");
        rejected(set(tree,new Obj(missing),"publication","units","0","entries","0","state","conditions","0","value"));
    }
}
