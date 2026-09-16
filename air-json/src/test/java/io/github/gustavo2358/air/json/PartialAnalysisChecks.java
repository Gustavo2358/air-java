package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.validation.*;
import java.util.*;
import static io.github.gustavo2358.air.json.ScalarAssignOracle.*;

public final class PartialAnalysisChecks {
    private PartialAnalysisChecks() { }
    public static void main(String[] args) { run();System.out.println("EP_PARTIAL_CODEC=PASS"); }
    static void run() {
        var p=RegionalChecks.regional();var u=p.units().getFirst();var sequence=u.sequences().getFirst();var instructions=new ArrayList<>(sequence.instructions());
        var assignment=(Operations.Assign)instructions.getFirst();
        var unknown=new Expressions.Unknown(assignment.value().header(),Types.known(Types.Builtin.BYTES),List.of(),Scopes.NoMemory.INSTANCE,gap("unproved-values"));
        instructions.set(0,new Operations.Assign(assignment.header(),assignment.destination(),unknown));
        var unit=new io.github.gustavo2358.air.model.Unit(u.id(),u.containingUnit(),u.objects(),u.visibleObjects(),u.entries(),List.of(new Sequence(sequence.label(),instructions,sequence.terminator(),sequence.origin())),u.completionPorts(),u.body(),u.bodyUnavailable(),u.coverage(),u.origin());
        p=new Publication(p.id(),p.airVersion(),p.capabilities(),p.artifacts(),List.of(unit),p.storage(),p.resources(),p.artifactRelations(),p.origins(),p.coverage(),p.uncertainties(),p.premises());
        var codec=new AirJson();var tree=new BindingWriter().envelope(p);var bytes=Json.write(tree,AirJson.Limits.defaults());
        try{codec.decode(bytes);throw new AssertionError("strict decode accepted incomplete input");}catch(AirJsonException e){if(e.code()!=AirJsonException.Code.INCOMPLETE_VALIDATION)throw e;}
        try{codec.encode(p);throw new AssertionError("strict writer certified incomplete input");}catch(AirJsonException e){if(e.code()!=AirJsonException.Code.INCOMPLETE_VALIDATION)throw e;}
        var partial=codec.decodeForPartialAnalysis(bytes);
        if(!p.equals(partial.publication())||partial.validation().status()!=ValidationResult.Status.INCOMPLETE_VALIDATION||!partial.validation().unprovedOperationPreconditions().orElseThrow().equals(Set.of(assignment.header().id())))throw new AssertionError("partial decode changes facts, status or scope");
        var bad=RegionalChecks.set(tree,"publication.units.0.objects.0.storage.region.localId",Json.value("missing"));
        try{codec.decodeForPartialAnalysis(Json.write(bad,AirJson.Limits.defaults()));throw new AssertionError("partial admission ignored structural invalidity");}catch(AirJsonException e){if(e.code()!=AirJsonException.Code.INVALID_IR)throw e;}
        try{new AirJson(new AirJson.Limits(1,128),ValidationOptions.defaults()).decodeForPartialAnalysis(bytes);throw new AssertionError("partial admission ignored resource limit");}catch(AirJsonException e){if(e.code()!=AirJsonException.Code.RESOURCE_LIMIT)throw e;}
    }
}
