package io.github.gustavo2358.air.json;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
public final class MemoryScopeChecks {
    private MemoryScopeChecks() { }
    public static void main(String[] args) {run();System.out.println("MEMORY_SCOPE_JSON=PASS unions/within/nested/plural/closed-shape");}
    static void run() {
        var p=RegionalChecks.regional();var u=p.units().getFirst();
        for(int n:List.of(1,16,257)) {
            var members=new ArrayList<Scopes.MemoryScope>();for(int i=0;i<n;i++)members.add(new Scopes.ObjectsMemory(List.of(u.objects().getFirst().id())));
            Scopes.MemoryScope scope=new Scopes.MemoryUnion(members);
            for(int depth=0;depth<16;depth++)scope=new Scopes.MemoryUnion(List.of(scope,new Scopes.StorageMemory(List.of(p.storage().getFirst().header().id()))));
            var expected=havoc(p,scope);var codec=new AirJson();var bytes=codec.encode(expected);
            if(!expected.equals(codec.decode(bytes))||!Arrays.equals(bytes,codec.encode(codec.decode(bytes))))throw new AssertionError("union shape facts and order must survive");
            var text=new String(bytes,StandardCharsets.UTF_8);
            RegionalChecks.negative(Json.parse(bytes,AirJson.Limits.defaults()),"publication.units.0.sequences.0.instructions.0.scope.members",new Json.Arr(List.of()),AirJsonException.Code.IMPLEMENTATION_LIMIT);
            for(var bad:List.of(text.replace("\"members\":","\"missing\":"),text.replace("\"kind\":\"union\"","\"kind\":\"union\",\"extra\":null"))) {
                if(bad.equals(text))throw new AssertionError("unchanged negative");
                try{codec.decode(bad.getBytes(StandardCharsets.UTF_8));throw new AssertionError("bad union accepted");}catch(AirJsonException e){if(e.code()!=AirJsonException.Code.INPUT_ERROR)throw new AssertionError(e);}
            }
        }
    }
    static Publication havoc(Publication p,Scopes.MemoryScope scope) {
        var u=p.units().getFirst();var s=u.sequences().getFirst();var h=s.instructions().getFirst().header();var reason=p.uncertainties().getFirst().id();
        var op=new Operations.HavocMay(h,scope,reason);
        var unit=new io.github.gustavo2358.air.model.Unit(u.id(),u.containingUnit(),u.objects(),u.visibleObjects(),u.entries(),List.of(new Sequence(s.label(),List.of(op),s.terminator(),s.origin())),u.completionPorts(),u.body(),u.bodyUnavailable(),u.coverage(),u.origin());
        return new Publication(p.id(),p.airVersion(),p.capabilities(),p.artifacts(),List.of(unit),p.storage(),p.resources(),p.artifactRelations(),p.origins(),p.coverage(),p.uncertainties(),p.premises());
    }
}
