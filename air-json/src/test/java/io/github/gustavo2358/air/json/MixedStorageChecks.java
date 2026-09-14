package io.github.gustavo2358.air.json;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;
public final class MixedStorageChecks {
    private MixedStorageChecks() { }
    public static void main(String[] args) {run();System.out.println("MIXED_STORAGE_JSON=PASS unknown type/binding/closed negatives");}
    static void run() {
        var p=RegionalChecks.regional();var u=p.units().getFirst();var template=u.objects().getFirst();var id=new ObjectId(u.id(),"unsupported-declaration");var gap=new UncertaintyId(p.id(),"unknown-declaration-type");
        var objects=new ArrayList<>(u.objects());objects.add(new Memory.ObjectDeclaration(id,Optional.empty(),new Types.UnknownType(gap),new Memory.UnknownBinding(new Scopes.StorageMemory(List.of(p.storage().getLast().header().id())),p.uncertainties().getFirst().id()),Memory.Visibility.UNKNOWN,template.origin(),Evidence.CoverageStatus.ABSTRACTED,template.precision()));
        var uncertainties=new ArrayList<>(p.uncertainties());uncertainties.add(new Evidence.Uncertainty(gap,"TYPE_UNKNOWN",List.of(Evidence.Dimension.VALUES),new Scopes.EntityScope(List.of(id)),"type not proved",template.origin()));
        var unit=new io.github.gustavo2358.air.model.Unit(u.id(),u.containingUnit(),objects,u.visibleObjects(),u.entries(),u.sequences(),u.completionPorts(),u.body(),u.bodyUnavailable(),u.coverage(),u.origin());
        p=new Publication(p.id(),p.airVersion(),p.capabilities(),p.artifacts(),List.of(unit),p.storage(),p.resources(),p.artifactRelations(),p.origins(),p.coverage(),uncertainties,p.premises());
        var codec=new AirJson();var bytes=codec.encode(p);if(!p.equals(codec.decode(bytes))||!Arrays.equals(bytes,codec.encode(codec.decode(bytes))))throw new AssertionError("mixed facts lost");
        var tree=Json.parse(bytes,AirJson.Limits.defaults());
        RegionalChecks.negative(tree,"publication.units.0.objects.3.typeRef.uncertainty.localId",Json.value("absent"),AirJsonException.Code.INVALID_IR);
        RegionalChecks.negative(tree,"publication.units.0.objects.3.storage.reason.localId",Json.value("absent"),AirJsonException.Code.INVALID_IR);
        RegionalChecks.negative(tree,"publication.units.0.objects.3.storage.scope.storage.0.localId",Json.value("absent"),AirJsonException.Code.INVALID_IR);
        RegionalChecks.negative(tree,"publication.units.0.objects.3.typeRef.kind",Json.value("unknown"),AirJsonException.Code.INPUT_ERROR);
    }
}
