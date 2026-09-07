package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Unit;
import io.github.gustavo2358.air.model.Ids.*;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

/** Hand-built facts from checkpoint 0B §2.1, independent of the JSON golden and codec. */
final class GobackOracle {
    private GobackOracle() {}
    static final PublicationId PUB = new PublicationId("goback-0b-manual");
    static final UnitId UNIT = new UnitId(PUB, "unit");
    static final OperationId RETURN = new OperationId(UNIT, "return");
    static OriginId origin(String local) { return new OriginId(PUB, local); }
    static UncertaintyId gap(String local) { return new UncertaintyId(PUB, local); }
    static Origins.Location location(long sl, long sc, long el, long ec) {
        return new Origins.LineColumns(new Origins.Span(
                new Origins.Position(BigInteger.valueOf(sl), BigInteger.valueOf(sc)),
                new Origins.Position(BigInteger.valueOf(el), BigInteger.valueOf(ec)),
                BigInteger.ONE, BigInteger.ZERO, Origins.ColumnUnit.UNICODE_SCALAR, false));
    }
    static Publication publication() {
        var original = new ArtifactId(PUB, "original");
        var expanded = new ArtifactId(PUB, "expanded");
        var opOrigin = origin("statement"); var entryOrigin = origin("entry");
        var unitOrigin = origin("unit-origin"); var label = new LabelId(UNIT, "sequence");
        var entryId = new EntryId(UNIT, "primary-entry");
        var scope = new Scopes.EntityScope(List.of(RETURN));
        var dimensions = List.of(Evidence.Dimension.STORAGE, Evidence.Dimension.EFFECTS,
                Evidence.Dimension.VALUES, Evidence.Dimension.DEPENDENCIES);
        var gaps = new java.util.ArrayList<Evidence.Uncertainty>();
        var claims = new java.util.ArrayList<Evidence.Claim>();
        for (var dimension : dimensions) {
            var id = gap("return/" + dimension);
            gaps.add(new Evidence.Uncertainty(id, "cobol-lower:UNPROVED_" + dimension,
                    List.of(dimension), scope, "The local control rule does not certify " + dimension
                    + "; upstream readiness retained separately.", opOrigin));
            claims.add(new Evidence.Claim(scope, Evidence.PrecisionStatus.UNAVAILABLE, List.of(id)));
        }
        var alternate = gap("entry-inventory/alternate-not-projected");
        gaps.add(new Evidence.Uncertainty(alternate, "cobol-lower:ALTERNATE_ENTRIES_NOT_PROJECTED",
                List.of(Evidence.Dimension.CONTROL), new Scopes.UnitScope(UNIT),
                "SP PRIMARY_ONLY/PARTIAL does not enumerate alternate entries; known primary start remains exact.", unitOrigin));
        var precision = new Evidence.Precision(new Evidence.Claim(scope, Evidence.PrecisionStatus.EXACT, List.of()),
                claims.get(0), claims.get(1), claims.get(2), claims.get(3));
        var header = new Operations.Header(RETURN, opOrigin, Evidence.CoverageStatus.MODELED, precision,
                dimensions.stream().map(d -> gap("return/" + d)).toList());
        var signature = new Interactions.Signature(new Interactions.ParameterInventory(List.of(), Interactions.NoRemainder.INSTANCE),
                new Interactions.ResultInventory(List.of(), Interactions.NoRemainder.INSTANCE), entryOrigin);
        var entry = new Entries.Entry(entryId, Optional.of(label), signature,
                new Entries.EntryState(List.of(), List.of()), entryOrigin);
        var items = List.of(new Evidence.CoverageItem("statement:0", opOrigin, Evidence.CoverageStatus.MODELED,
                        List.of(RETURN, label), List.of(), Optional.empty()),
                new Evidence.CoverageItem("entry:0", entryOrigin, Evidence.CoverageStatus.MODELED,
                        List.of(entryId), List.of(), Optional.empty()));
        var unit = new Unit(UNIT, Optional.empty(), List.of(), List.of(), List.of(entry),
                List.of(new Sequence(label, List.of(), new Operations.Return(header, List.of()), origin("sequence"))),
                List.of(), Unit.BodyAvailability.AVAILABLE, Optional.empty(),
                new Evidence.Coverage(Evidence.InventoryStatus.PARTIAL, new Scopes.UnitScope(UNIT), items, List.of(alternate)), unitOrigin);
        return new Publication(PUB, SemanticVersion.AIR_2_0_0, new Capabilities.Manifest(List.of(), List.of()),
                List.of(new Origins.Artifact(original, "semantic-product-entry-goback.cbl", Optional.empty()),
                        new Origins.Artifact(expanded, "<preprocessed>", Optional.empty())),
                List.of(unit), List.of(), List.of(), List.of(),
                List.of(new Origins.Written(origin("statement-original"), original, Optional.of(location(4,11,4,16)), List.of(), true),
                        new Origins.Written(origin("statement-expanded"), expanded, Optional.of(location(4,4,4,9)), List.of(), true),
                        new Origins.Derived(opOrigin, List.of(origin("statement-original"), origin("statement-expanded")), "sp-provenance/original-expanded@1"),
                        new Origins.Derived(origin("sequence"), List.of(opOrigin), "minimal-entry-goback@1/sequence"),
                        new Origins.Written(origin("entry-original"), original, Optional.of(location(3,7,4,18)), List.of(), true),
                        new Origins.Written(origin("entry-expanded"), expanded, Optional.of(location(3,0,4,11)), List.of(), true),
                        new Origins.Derived(entryOrigin, List.of(origin("entry-original"), origin("entry-expanded")), "sp-provenance/original-expanded@1"),
                        new Origins.Derived(unitOrigin, List.of(entryOrigin), "minimal-entry-goback@1/selected-unit")),
                new Evidence.Coverage(Evidence.InventoryStatus.PARTIAL, new Scopes.PublicationScope(PUB), items, List.of(alternate)), gaps, List.of());
    }
}
