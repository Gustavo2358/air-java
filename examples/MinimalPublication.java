import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.validation.*;
import java.util.List;
import java.util.Optional;

/** Run after scripts/check.sh: java -cp target/air-java-0.1.0-SNAPSHOT.jar examples/MinimalPublication.java */
public class MinimalPublication {
    public static Publication create() {
        PublicationId publication = new PublicationId("example-publication");
        UnitId unit = new UnitId(publication,"example-unit");
        OriginId origin = new OriginId(publication,"synthetic-origin");
        LabelId start = new LabelId(unit,"start");
        OperationId stop = new OperationId(unit,"stop");
        Evidence.Claim exact = new Evidence.Claim(new Scopes.EntityScope(List.of(stop)),
                Evidence.PrecisionStatus.EXACT,List.of());
        Evidence.Claim notApplicable = new Evidence.Claim(new Scopes.EntityScope(List.of(stop)),
                Evidence.PrecisionStatus.NOT_APPLICABLE,List.of());
        Evidence.Precision precision = new Evidence.Precision(exact,notApplicable,
                notApplicable,notApplicable,notApplicable);
        Operations.Halt halt = new Operations.Halt(new Operations.Header(stop,origin,
                Evidence.CoverageStatus.MODELED,precision,List.of()),Operations.HaltKind.NORMAL);
        Sequence sequence = new Sequence(start,List.of(),halt,origin);
        Entries.Entry entry = new Entries.Entry(new EntryId(unit,"main"),Optional.of(start),
                new Interactions.Signature(List.of(),List.of(),Optional.empty()),
                new Entries.EntryState(List.of(),List.of()),origin);
        Evidence.CoverageItem item = new Evidence.CoverageItem("synthetic-stop",origin,
                Evidence.CoverageStatus.MODELED,List.of(stop),List.of(),Optional.empty());
        Evidence.Coverage unitCoverage = new Evidence.Coverage(Evidence.InventoryStatus.COMPLETE,
                new Scopes.UnitScope(unit),List.of(item),List.of());
        Unit body = new Unit(unit,Optional.empty(),List.of(),List.of(),List.of(entry),
                List.of(sequence),List.of(),Unit.BodyAvailability.AVAILABLE,Optional.empty(),unitCoverage,origin);
        return new Publication(publication,SemanticVersion.AIR_2_0_0,
                new Capabilities.Manifest(List.of(),List.of()),List.of(),List.of(body),
                List.of(),List.of(),List.of(),
                List.of(new Origins.Unavailable(origin,"handwritten AIR fixture, no source file")),
                new Evidence.Coverage(Evidence.InventoryStatus.COMPLETE,
                        new Scopes.PublicationScope(publication),List.of(item),List.of()),
                List.of(),List.of(),List.of());
    }
    public static void main(String[] args) {
        Publication publication=create();
        ValidationResult validation=AirValidator.validate(publication);
        if(!validation.isStructurallyValid()) throw new IllegalStateException(validation.toString());
        System.out.println(validation.status()+": "+validation.statistics());
        // Application wiring later: cfgBuilder.build(publication, options).
        // File transport, when needed, belongs in an external adapter.
    }
}
