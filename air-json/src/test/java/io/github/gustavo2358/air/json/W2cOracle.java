package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Unit;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Model-only facts. No wire, codec, SP or source-language input. */
final class W2cOracle {
    private W2cOracle() {}
    static final PublicationId PUB = new PublicationId("cp6-w2c-manual");
    static final UnitId UNIT = new UnitId(PUB, "unit");
    static final ObjectId FLAG = new ObjectId(UNIT, "FLAG");
    static final ObjectId PGM = new ObjectId(UNIT, "WS-PGM");
    static OriginId origin(String s) { return new OriginId(PUB, s); }
    static UncertaintyId gap(String s) { return new UncertaintyId(PUB, s); }
    static LabelId label(String s) { return new LabelId(UNIT, s); }
    static StorageId cell(String s) { return new StorageId(PUB, "cell-" + s); }
    static Operations.Header header(String s) {
        return new Operations.Header(new OperationId(UNIT, s), origin("op-" + s), Evidence.CoverageStatus.ABSTRACTED,
                precision(), List.of(gap("facts"), gap("predicate-value-unknown")));
    }
    static Evidence.Precision precision() {
        var scope = new Scopes.UnitScope(UNIT);
        var exact = new Evidence.Claim(scope, Evidence.PrecisionStatus.EXACT, List.of());
        var open = new Evidence.Claim(scope, Evidence.PrecisionStatus.OPEN, List.of(gap("facts")));
        var unavailable = new Evidence.Claim(scope, Evidence.PrecisionStatus.UNAVAILABLE, List.of(gap("facts")));
        return new Evidence.Precision(exact, open, unavailable, open, unavailable);
    }
    static Evidence.Coverage coverage(Scopes.FactScope s) {
        return new Evidence.Coverage(Evidence.InventoryStatus.PARTIAL, s, List.of(), List.of(gap("facts")));
    }
    static Operand.Header operand(String op, String id, Operand.Role role, String source) {
        return new Operand.Header(new OperandId(new OperationOwner(new OperationId(UNIT, op)), id), role, origin(source));
    }
    static Expressions.Read read(String op, String id, Operand.Role role, ObjectId object) {
        return new Expressions.Read(operand(op, id, role, "read"),
                new Places.ObjectPlace(operand(op, id + "-place", Operand.Role.VALUE_READ, "place"), object));
    }
    static Expressions.Unknown unknown(String op, Operand.Role role, List<Expression> deps, Scopes.MemoryBound bound) {
        return new Expressions.Unknown(operand(op, "unknown", role, "predicate"), Types.known(Types.Builtin.BOOL),
                deps, bound, gap("predicate-value-unknown"));
    }
    static Proofs.Premise premise(String id, List<StorageId> storage) {
        return new Proofs.Premise(new PremiseId(PUB, id), "fixture.authority  é", "Producer-declared separation; no inference\n😀",
                origin("premise"), new Proofs.DisjointStorage(storage));
    }
    static Publication publication(String form) {
        boolean boolCell = List.of("bool", "branch", "unknown", "unknown-empty").contains(form);
        var objects = new ArrayList<Memory.ObjectDeclaration>(); var cells = new ArrayList<Memory.Storage>();
        for (ObjectId id : List.of(FLAG, PGM)) {
            var type = Types.known(id.equals(FLAG) && boolCell ? Types.Builtin.BOOL : Types.Builtin.TEXT);
            objects.add(new Memory.ObjectDeclaration(id, Optional.of(id.localId()), type, new Memory.CellBinding(cell(id.localId())),
                    Memory.Visibility.PRIVATE, origin("object"), Evidence.CoverageStatus.MODELED, precision()));
            cells.add(new Memory.Cell(new Memory.StorageHeader(cell(id.localId()), Optional.of(UNIT), Memory.Lifetime.ACTIVATION,
                    Memory.Visibility.PRIVATE, origin("storage")), type));
        }
        var instructions = new ArrayList<Instruction>();
        Terminator first = new Operations.Return(header("if"), List.of());
        if (form.equals("jump")) first = new Operations.Jump(header("if"), label("Lmerge"));
        if (form.equals("branch")) first = new Operations.Branch(header("if"), read("if", "predicate-read", Operand.Role.PREDICATE, FLAG), label("Lthen"), label("Lelse"));
        if (form.equals("full")) first = new Operations.Branch(header("if"),
                unknown("if", Operand.Role.PREDICATE, List.of(read("if", "dependency", Operand.Role.VALUE_READ, FLAG)), Scopes.NoMemory.INSTANCE), label("Lthen"), label("Lelse"));
        if (form.startsWith("unknown")) instructions.add(new Operations.Assign(header("assign"),
                new Places.ObjectPlace(operand("assign", "destination", Operand.Role.VALUE_WRITE, "place"), FLAG),
                unknown("assign", Operand.Role.VALUE_READ, form.equals("unknown-empty") ? List.of() : List.of(read("assign", "dependency", Operand.Role.VALUE_READ, PGM)), Scopes.NoMemory.INSTANCE)));
        var sequences = List.of(new Sequence(label("Lif"), instructions, first, origin("sequence")),
                new Sequence(label("Lthen"), List.of(), form.equals("full") ? new Operations.Jump(header("then"), label("Lmerge")) : new Operations.Return(header("then"), List.of()), origin("sequence")),
                new Sequence(label("Lelse"), List.of(), form.equals("full") ? new Operations.Jump(header("else"), label("Lmerge")) : new Operations.Return(header("else"), List.of()), origin("sequence")),
                new Sequence(label("Lmerge"), List.of(), new Operations.Return(header("merge"), List.of()), origin("sequence")));
        var signature = new Interactions.Signature(new Interactions.ParameterInventory(List.of(), Interactions.NoRemainder.INSTANCE),
                new Interactions.ResultInventory(List.of(), Interactions.NoRemainder.INSTANCE), origin("entry"));
        var entry = new Entries.Entry(new EntryId(UNIT, "entry"), Optional.of(label("Lif")), signature,
                new Entries.EntryState(List.of(), List.of(gap("facts"))), origin("entry"));
        var unit = new Unit(UNIT, Optional.empty(), objects, List.of(), List.of(entry), sequences, List.of(), Unit.BodyAvailability.AVAILABLE,
                Optional.empty(), coverage(new Scopes.UnitScope(UNIT)), origin("unit"));
        var artifact = new ArtifactId(PUB, "fixture"); var origins = new ArrayList<Origins.Origin>();
        for (String s : List.of("op-if", "op-then", "op-else", "op-merge", "op-assign", "read", "place", "predicate", "premise", "object", "storage", "sequence", "entry", "unit"))
            origins.add(new Origins.Written(origin(s), artifact, Optional.empty(), List.of(), false));
        var uncertainties = List.of(new Evidence.Uncertainty(gap("facts"), "fixture:PARTIAL", List.of(Evidence.Dimension.values()),
                        new Scopes.UnitScope(UNIT), "Not a producer certification", origin("unit")),
                new Evidence.Uncertainty(gap("predicate-value-unknown"), "fixture:VALUE_UNKNOWN", List.of(Evidence.Dimension.VALUES),
                        new Scopes.UnitScope(UNIT), "Value remains unknown", origin("predicate")));
        // Reverse lexical order intentionally: physical lists must never be sorted as mathematical sets.
        var premises = List.of("premise", "full").contains(form) ? List.of(premise("independent", List.of(cell("WS-PGM"), cell("FLAG")))) : List.<Proofs.Premise>of();
        return new Publication(PUB, SemanticVersion.AIR_2_0_0, new Capabilities.Manifest(List.of(), List.of()),
                List.of(new Origins.Artifact(artifact, "w2c.manual", Optional.empty())), List.of(unit), cells, List.of(), List.of(), origins,
                coverage(new Scopes.PublicationScope(PUB)), uncertainties, premises);
    }
    static Publication copy(Publication p, List<Unit> units, List<Memory.Storage> cells, List<Proofs.Premise> premises) {
        return new Publication(p.id(), p.airVersion(), p.capabilities(), p.artifacts(), units, cells, p.resources(), p.artifactRelations(),
                p.origins(), p.coverage(), p.uncertainties(), premises);
    }
    static Unit sequences(Unit u, List<Sequence> sequences) {
        return new Unit(u.id(), u.containingUnit(), u.objects(), u.visibleObjects(), u.entries(), sequences, u.completionPorts(), u.body(),
                u.bodyUnavailable(), u.coverage(), u.origin());
    }
}
