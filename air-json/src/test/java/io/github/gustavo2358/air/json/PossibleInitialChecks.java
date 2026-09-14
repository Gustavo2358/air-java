package io.github.gustavo2358.air.json;

import java.util.*;
import static io.github.gustavo2358.air.json.Json.*;

/** Independent wire oracle for entry.possibilities@1; no producer of the new tag. */
public final class PossibleInitialChecks {
    private PossibleInitialChecks() { }
    public static void main(String[] args) {run();System.out.println("POSSIBLE_INITIAL_JSON=PASS");}
    static Value at(Value v,String... path) {
        for(String key:path)v=v instanceof Obj o?o.fields().get(key):((Arr)v).values().get(Integer.parseInt(key));
        return Objects.requireNonNull(v);
    }
    static Value set(Value v,Value replacement,String... path) {return set(v,replacement,path,0);}
    private static Value set(Value v,Value replacement,String[] path,int i) {
        if(i==path.length)return replacement;
        if(v instanceof Obj o) {var m=new LinkedHashMap<>(o.fields());m.put(path[i],set(m.get(path[i]),replacement,path,i+1));return new Obj(m);}
        var a=new ArrayList<>(((Arr)v).values());int index=Integer.parseInt(path[i]);a.set(index,set(a.get(index),replacement,path,i+1));return new Arr(a);
    }
    static Value fixture() {
        var codec=new AirJson();var limits=AirJson.Limits.defaults();
        var tree=Json.parse(codec.encode(InitialStateChecks.publication("literal")),limits);
        var unknown=Json.parse(codec.encode(InitialStateChecks.publication("external_unknown")),limits);
        var value=at(tree,"publication","units","0","entries","0","state","conditions","0","value","value");
        var other=set(value,new Text("other-entry-literal"),"header","id","localId");
        other=set(other,new Text("EFGH"),"value","value");
        var reason=at(unknown,"publication","units","0","entries","0","state","conditions","0","value","reason");
        var possible=new Obj(Map.of("kind",new Text("possible_literals"),"candidates",new Arr(List.of(value,other)),"remainder",reason));
        tree=set(tree,possible,"publication","units","0","entries","0","state","conditions","0","value");
        tree=set(tree,at(unknown,"publication","uncertainties"),"publication","uncertainties");
        var required=new ArrayList<>(((Arr)at(tree,"publication","capabilities","required")).values());
        required.add(new Obj(Map.of("name",new Text("entry.possibilities"),"version",new Text("1"))));
        return set(tree,new Arr(required),"publication","capabilities","required");
    }
    static void rejected(Value value) {
        try {new AirJson().decode(Json.write(value,AirJson.Limits.defaults()));throw new AssertionError("invalid possible entry accepted");}
        catch(AirJsonException expected) {
            if(expected.code()==AirJsonException.Code.RESOURCE_LIMIT)throw new AssertionError(expected);
        }
    }
    static void run() {
        var tree=fixture();var codec=new AirJson();var p=codec.decode(Json.write(tree,AirJson.Limits.defaults()));
        var wire=codec.encode(p);if(!p.equals(codec.decode(wire)))throw new AssertionError("entry possibility lost on roundtrip");
        var restored=Json.parse(wire,AirJson.Limits.defaults());
        var value=at(restored,"publication","units","0","entries","0","state","conditions","0","value");
        if(!new Text("possible_literals").equals(at(value,"kind"))||((Arr)at(value,"candidates")).values().size()!=2)
            throw new AssertionError("possibility strengthened or candidate dropped");
        rejected(set(tree,new Arr(List.of()),"publication","capabilities","required"));
        rejected(set(tree,new Arr(List.of()),"publication","units","0","entries","0","state","conditions","0","value","candidates"));
        rejected(set(tree,new Arr(List.of()),"publication","uncertainties"));
        var fields=new LinkedHashMap<>(((Obj)value).fields());fields.remove("remainder");
        rejected(set(tree,new Obj(fields),"publication","units","0","entries","0","state","conditions","0","value"));
        rejected(set(tree,new Text("foreign-entry"),"publication","units","0","entries","0","state","conditions","0","value","candidates","1","header","id","owner","localId"));
    }
}
