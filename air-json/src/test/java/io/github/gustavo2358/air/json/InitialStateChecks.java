package io.github.gustavo2358.air.json;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class InitialStateChecks {
    private InitialStateChecks() { }
    public static void main(String[] args) {run();System.out.println("INITIAL_JSON=PASS literal/preserve/external_unknown/uninitialized; closed fields");}
    static void run() {
        for(String kind:List.of("literal","preserve","external_unknown","uninitialized")) {
            var expected=publication(kind);var codec=new AirJson();var bytes=codec.encode(expected);var restored=codec.decode(bytes);
            if(!expected.equals(restored)||!Arrays.equals(bytes,codec.encode(restored)))throw new AssertionError("initial state must roundtrip "+kind);
            var text=new String(bytes,StandardCharsets.UTF_8);
            for(String bad:List.of(text.replace("\"conditions\":[{","\"conditions\":[{\"extra\":null,"),text.replace("\"place\":","\"absentPlace\":"))) {
                if(bad.equals(text))throw new AssertionError("negative mutation did not change input");
                try{codec.decode(bad.getBytes(StandardCharsets.UTF_8));throw new AssertionError("initial condition malformed accepted");}catch(AirJsonException e){if(e.code()!=AirJsonException.Code.INPUT_ERROR)throw new AssertionError(e);}
            }
        }
    }
    static Publication publication(String kind) {
        var p=ScalarAssignOracle.publication(1,1);var u=p.units().getFirst();var e=u.entries().getFirst();var origin=e.origin();var reason=new UncertaintyId(p.id(),"initial-unknown");
        var place=new Places.ObjectPlace(new Operand.Header(new OperandId(new EntryOwner(e.id()),"initial-place"),Operand.Role.VALUE_WRITE,origin),u.objects().getFirst().id());
        Entries.InitialValue value=switch(kind) {
            case "literal"->new Entries.LiteralInitial(new Expressions.Literal(new Operand.Header(new OperandId(new EntryOwner(e.id()),"initial-value"),Operand.Role.VALUE_READ,origin),new Values.TextValue("ABCD")));
            case "preserve"->Entries.Preserve.INSTANCE;case "external_unknown"->new Entries.ExternalUnknown(reason);default->new Entries.Uninitialized(reason);
        };
        var entry=new Entries.Entry(e.id(),e.initialLabel(),e.signature(),new Entries.EntryState(List.of(new Entries.InitialCondition(place,value,origin,List.of())),List.of()),origin);
        var unit=new io.github.gustavo2358.air.model.Unit(u.id(),u.containingUnit(),u.objects(),u.visibleObjects(),List.of(entry),u.sequences(),u.completionPorts(),u.body(),u.bodyUnavailable(),u.coverage(),u.origin());
        var storage=p.storage().stream().map(s->{var c=(Memory.Cell)s;var h=c.header();return (Memory.Storage)new Memory.Cell(new Memory.StorageHeader(h.id(),h.owner(),Memory.Lifetime.PERSISTENT,h.visibility(),h.origin()),c.typeRef());}).toList();
        var uncertainties=new ArrayList<>(p.uncertainties());if(kind.endsWith("unknown")||kind.equals("uninitialized"))uncertainties.add(new Evidence.Uncertainty(reason,"INITIAL_UNKNOWN",List.of(Evidence.Dimension.VALUES),new Scopes.UnitScope(u.id()),"initial content unknown",origin));
        return new Publication(p.id(),p.airVersion(),p.capabilities(),p.artifacts(),List.of(unit),storage,p.resources(),p.artifactRelations(),p.origins(),p.coverage(),uncertainties,p.premises());
    }
}
