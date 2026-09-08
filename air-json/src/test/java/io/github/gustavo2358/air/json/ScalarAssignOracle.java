package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Unit;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Manual Java facts, authored independently of the wire fixture. No codec calls. */
final class ScalarAssignOracle {
    private ScalarAssignOracle() {}
    static final PublicationId PUB = new PublicationId("cp4b-scalar-manual");
    static final UnitId UNIT = new UnitId(PUB, "alpha");
    static final ObjectId OBJECT = new ObjectId(UNIT, "data-slot");
    static final StorageId CELL = new StorageId(PUB, "backing-cell");
    static final OperationId ASSIGN = new OperationId(UNIT, "set-program");
    static final OperationId RETURN = new OperationId(UNIT, "leave");
    static final Types.TypeRef TEXT = Types.known(Types.Builtin.TEXT);
    static final Scopes.FactScope SCOPE = new Scopes.UnitScope(UNIT);
    static OriginId origin(String local) { return new OriginId(PUB, local); }
    static UncertaintyId gap(String local) { return new UncertaintyId(PUB, local); }
    static Evidence.Precision precision() {
        return new Evidence.Precision(new Evidence.Claim(SCOPE, Evidence.PrecisionStatus.EXACT, List.of()),
                claim("storage"), claim("effects"), claim("values"), claim("dependencies"));
    }
    private static Evidence.Claim claim(String dimension) {
        return new Evidence.Claim(SCOPE, Evidence.PrecisionStatus.UNAVAILABLE, List.of(gap("unproved-" + dimension)));
    }
    static Operations.Header header(OperationId id, String o) {
        return new Operations.Header(id, origin(o), Evidence.CoverageStatus.MODELED, precision(),
                List.of(gap("unproved-storage"), gap("unproved-effects"), gap("unproved-values"), gap("unproved-dependencies")));
    }
    static Operations.Assign assign(OperationId id, ObjectId object, String text) {
        var owner = new OperationOwner(id);
        return new Operations.Assign(header(id, "assignment"),
                new Places.ObjectPlace(new Operand.Header(new OperandId(owner, "target-occ"), Operand.Role.VALUE_WRITE, origin("target")), object),
                new Expressions.Literal(new Operand.Header(new OperandId(owner, "source-occ"), Operand.Role.VALUE_READ, origin("literal")), new Values.TextValue(text)));
    }
    static Memory.ObjectDeclaration object(ObjectId id, StorageId storage) {
        return new Memory.ObjectDeclaration(id, Optional.of("WS-PGM"), TEXT, new Memory.CellBinding(storage),
                Memory.Visibility.PRIVATE, origin("data"), Evidence.CoverageStatus.MODELED, precision());
    }
    static Memory.Cell cell(StorageId id) {
        return new Memory.Cell(new Memory.StorageHeader(id, Optional.of(UNIT), Memory.Lifetime.PERSISTENT,
                Memory.Visibility.PRIVATE, origin("data")), TEXT);
    }
    static Publication publication() { return publication(1, 1); }
    /** Extra entities exercise cardinality only; no claim that separate cells prove disjointness. */
    static Publication publication(int objectCount, int assignCount) {
        var objects = new ArrayList<Memory.ObjectDeclaration>(); var storage = new ArrayList<Memory.Storage>();
        for (int i = 0; i < objectCount; i++) {
            var object = i == 0 ? OBJECT : new ObjectId(UNIT, "data-" + i);
            var cell = i == 0 ? CELL : new StorageId(PUB, "cell-" + i);
            objects.add(object(object, cell)); storage.add(cell(cell));
        }
        var instructions = new ArrayList<Instruction>();
        for (int i = 0; i < assignCount; i++) instructions.add(assign(i == 0 ? ASSIGN : new OperationId(UNIT, "assign-" + i),
                objects.get(i % objectCount).id(), i == 0 ? "PROGA" : "value-" + i));
        var entryId = new EntryId(UNIT, "start"); var label = new LabelId(UNIT, "body");
        var items = List.of(item("declared-data", "data", List.of(OBJECT, CELL)),
                item("scalar-assignment", "assignment", List.of(ASSIGN)), item("normal-return", "return", List.of(RETURN)),
                item("known-entry", "entry", List.of(entryId, label)));
        var signature = new Interactions.Signature(new Interactions.ParameterInventory(List.of(), Interactions.NoRemainder.INSTANCE),
                new Interactions.ResultInventory(List.of(), Interactions.NoRemainder.INSTANCE), origin("entry"));
        var entry = new Entries.Entry(entryId, Optional.of(label), signature,
                new Entries.EntryState(List.of(), List.of(gap("entry-inventory"))), origin("entry"));
        var unit = new Unit(UNIT, Optional.empty(), objects, List.of(), List.of(entry),
                List.of(new Sequence(label, instructions, new Operations.Return(header(RETURN, "return"), List.of()), origin("sequence"))),
                List.of(), Unit.BodyAvailability.AVAILABLE, Optional.empty(), coverage(SCOPE, items), origin("unit"));
        var artifact = new ArtifactId(PUB, "source");
        List<Origins.Origin> origins = List.of("data", "target", "literal", "assignment", "return", "entry", "sequence", "unit").stream()
                .<Origins.Origin>map(o -> new Origins.Written(origin(o), artifact, Optional.empty(), List.of(), false)).toList();
        var gaps = List.of(uncertainty("storage", Evidence.Dimension.STORAGE), uncertainty("effects", Evidence.Dimension.EFFECTS),
                uncertainty("values", Evidence.Dimension.VALUES), uncertainty("dependencies", Evidence.Dimension.DEPENDENCIES),
                new Evidence.Uncertainty(gap("entry-inventory"), "manual:ENTRY_INVENTORY_PARTIAL", List.of(Evidence.Dimension.CONTROL), SCOPE,
                        "Only the selected entry is inventoried.", origin("entry")));
        return new Publication(PUB, SemanticVersion.AIR_2_0_0, new Capabilities.Manifest(List.of(), List.of()),
                List.of(new Origins.Artifact(artifact, "scalar-assignment.manual", Optional.empty())), List.of(unit), storage,
                List.of(), List.of(), origins, coverage(new Scopes.PublicationScope(PUB), items), gaps, List.of());
    }
    private static Evidence.CoverageItem item(String key, String origin, List<Id> outputs) {
        return new Evidence.CoverageItem(key, origin(origin), Evidence.CoverageStatus.MODELED, outputs, List.of(), Optional.empty());
    }
    private static Evidence.Coverage coverage(Scopes.FactScope scope, List<Evidence.CoverageItem> items) {
        return new Evidence.Coverage(Evidence.InventoryStatus.PARTIAL, scope, items, List.of(gap("entry-inventory")));
    }
    private static Evidence.Uncertainty uncertainty(String local, Evidence.Dimension dimension) {
        return new Evidence.Uncertainty(gap("unproved-" + local), "manual:UNPROVED_" + dimension,
                List.of(dimension), SCOPE, "Manual transport fixture does not certify " + dimension + ".", origin("unit"));
    }
}
