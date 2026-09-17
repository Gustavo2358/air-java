package io.github.gustavo2358.air.json;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;
import static io.github.gustavo2358.air.json.ResourceBindingOracle.*;
import static io.github.gustavo2358.air.json.InvokeChecks.*;
import static io.github.gustavo2358.air.json.Json.object;
import static io.github.gustavo2358.air.json.AirJsonException.Code.*;
/** Binding §10.1: explicit visible objects, existing alias and original declaration owner. */
final class UnitVisibilityChecks {
    private UnitVisibilityChecks(){}
    public static void main(String[] args){run();}
    static Publication manual(){
        var p=publication("A3");var child=p.units().get(1);var objects=new ArrayList<>(child.objects());var o=objects.get(0);
        objects.set(0,new Memory.ObjectDeclaration(o.id(),o.displayName(),o.typeRef(),new Memory.AliasBinding(ResourceBindingOracle.object("U1","record")),o.visibility(),o.origin(),o.coverage(),o.precision()));
        var op=operation("U2",0);var invoke=new Operations.Invoke(header(op),"read",target("CLIENTDD"),List.of(),List.of(),new Interactions.ExternalSignature(signature()),List.of(),new Interactions.EffectBound(new Interactions.ForeignEffects(Scopes.NoMemory.INSTANCE,Scopes.NoMemory.INSTANCE,List.of()),List.of()),new Control.InvocationOutcomes(List.of(new Control.Normal(child.sequences().getFirst().label())),Scopes.NoControl.INSTANCE),new Interactions.UnknownContract(GAP));
        var label=new LabelId(child.id(),"global-use");var sequences=new ArrayList<Sequence>();sequences.add(new Sequence(label,List.of(),invoke,ORIGIN));sequences.addAll(child.sequences());
        var old=child.entries().getFirst();var entry=new Entries.Entry(old.id(),Optional.of(label),old.signature(),old.state(),old.origin());
        var nested=new Unit(child.id(),Optional.of(unit("U1")),objects,List.of(ResourceBindingOracle.object("U1","record")),List.of(entry),sequences,List.of(),child.body(),child.bodyUnavailable(),child.coverage(),child.origin());
        var resource=declaration("U1-F","U1","F",target("CLIENTDD"),"cobol.fd",List.of(new Interactions.ResourceUse(op,"input",ORIGIN)));
        return new Publication(p.id(),p.airVersion(),p.capabilities(),p.artifacts(),List.of(p.units().getFirst(),nested),p.storage(),List.of(resource,p.resources().get(1)),p.artifactRelations(),p.origins(),p.coverage(),p.uncertainties(),p.premises());
    }
    static void run(){
        var p=manual();var codec=new AirJson();equal(p,codec.decode(codec.encode(p)));var tree=tree(p);
        var expected=object("domain","object","publication",PUB.localId(),"unit","U1","localId","record");
        equal(expected,at(tree,"publication.units.1.visibleObjects.0"));
        equal(p,codec.decode(wire(edit(tree,"publication.units.1.visibleObjects.0",expected))));
        failure(INPUT_ERROR,()->codec.decode(wire(edit(tree,"publication.units.1.visibleObjects.0",object("domain","object","publication",PUB.localId(),"unit","U1","localId","record","extra","bad")))));
        invalid(edit(tree,"publication.units.1.visibleObjects.0",object("domain","object","publication",PUB.localId(),"unit","U1","localId","absent")),"I-02");
        invalid(edit(tree,"publication.units.1.containingUnit",object("domain","unit","publication",PUB.localId(),"localId","U2")),"I-01");
    }
}
