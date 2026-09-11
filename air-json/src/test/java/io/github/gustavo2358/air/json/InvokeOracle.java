package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.model.Unit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Public-model facts derived from ContractSuite.literalInvokeFixture; no codec or wire input. */
final class InvokeOracle {
    private InvokeOracle() {}
    static final PublicationId PUB = new PublicationId("cp6-w1b-manual");
    static final UnitId UNIT = new UnitId(PUB, "caller");
    static final ObjectId OBJECT = new ObjectId(UNIT, "name-cell");
    static final ObjectId OTHER = new ObjectId(UNIT, "other-cell");
    static final OperationId INVOKE = new OperationId(UNIT, "interaction");
    static final LabelId START = new LabelId(UNIT, "start");
    static final LabelId END = new LabelId(UNIT, "continuation");
    static OriginId origin(String local) { return new OriginId(PUB, local); }
    static UncertaintyId gap(String local) { return new UncertaintyId(PUB, local); }
    static Evidence.Precision precision() {
        var c = new Evidence.Claim(new Scopes.UnitScope(UNIT), Evidence.PrecisionStatus.OPEN, List.of(gap("facts")));
        return new Evidence.Precision(c, c, c, c, c);
    }
    static Operations.Header header(OperationId id) {
        return new Operations.Header(id, origin("operation"), Evidence.CoverageStatus.ABSTRACTED,
                precision(), List.of(gap("facts"), gap("contract")));
    }
    static Operand.Header operand(String local, Operand.Role role, String source) {
        return new Operand.Header(new OperandId(new OperationOwner(INVOKE), local), role, origin(source));
    }
    static Places.ObjectPlace effectPlace() {
        return new Places.ObjectPlace(operand("effect-place", Operand.Role.VALUE_WRITE, "effect"), OBJECT);
    }
    static Expression read() {
        return new Expressions.Read(operand("name-read", Operand.Role.CALL_TARGET, "expression"),
                new Places.ObjectPlace(operand("name-place", Operand.Role.VALUE_READ, "place"), OBJECT));
    }
    static Interactions.Signature signature(String source) {
        return new Interactions.Signature(new Interactions.ParameterInventory(List.of(), Interactions.NoRemainder.INSTANCE),
                new Interactions.ResultInventory(List.of(), Interactions.NoRemainder.INSTANCE), origin(source));
    }
    static Operations.Invoke invoke(boolean computed, boolean knownContract) {
        Interactions.Target target = computed
                ? new Interactions.ComputedTarget("program", "fixture.resources", read(), Interactions.ExactName.INSTANCE, origin("target"))
                : new Interactions.LiteralTarget("program", "fixture.resources", "PROGA", Interactions.ExactName.INSTANCE, origin("target"));
        return new Operations.Invoke(header(INVOKE), "call", target, List.of(), List.of(),
                new Interactions.ExternalSignature(signature("signature")), knownContract ? List.of(effectPlace()) : List.of(),
                new Interactions.EffectBound(new Interactions.ForeignEffects(
                        new Scopes.WithinMemory(new Scopes.VisibleMemory(UNIT, true)),
                        new Scopes.WithinMemory(new Scopes.AllMemory(PUB, true)),
                        knownContract ? List.of(effectPlace().header().id()) : List.of()), List.of()),
                new Control.InvocationOutcomes(List.of(new Control.Normal(END),
                        new Control.Exceptional("fixture.failure", new Control.Handler(END)),
                        new Control.AnyException(Control.Propagate.INSTANCE), Control.HaltAlternative.INSTANCE, Control.Diverge.INSTANCE),
                        new Scopes.WithinControl(new Scopes.UnitControl(UNIT, true, true, true, true, true, true))),
                knownContract ? new Interactions.KnownContract(new Interactions.ContractRef("fixture.authority", "1",
                        List.of(origin("evidence-a"), origin("evidence-b")))) : new Interactions.UnknownContract(gap("contract")));
    }
    static Publication publication(boolean computed, boolean knownContract) { return publication(invoke(computed, knownContract)); }
    static Publication publication(Operations.Invoke invoke) {
        var entry = new Entries.Entry(new EntryId(UNIT, "entry"), Optional.of(START), signature("entry"),
                new Entries.EntryState(List.of(), List.of(gap("facts"))), origin("entry"));
        var objects = new ArrayList<Memory.ObjectDeclaration>(); var cells = new ArrayList<Memory.Storage>();
        for (ObjectId id : List.of(OBJECT, OTHER)) {
            var storage = new StorageId(PUB, "storage-" + id.localId());
            var type = Types.known(Types.Builtin.TEXT);
            objects.add(new Memory.ObjectDeclaration(id, Optional.of("opaque display " + id.localId()), type,
                    new Memory.CellBinding(storage), Memory.Visibility.SHARED, origin("object"), Evidence.CoverageStatus.MODELED, precision()));
            cells.add(new Memory.Cell(new Memory.StorageHeader(storage, Optional.of(UNIT), Memory.Lifetime.PERSISTENT,
                    Memory.Visibility.SHARED, origin("storage")), type));
        }
        var unit = new Unit(UNIT, Optional.empty(), objects, List.of(), List.of(entry),
                List.of(new Sequence(START, List.of(), invoke, origin("sequence")),
                        new Sequence(END, List.of(), new Operations.Return(header(new OperationId(UNIT, "return")), List.of()), origin("sequence"))),
                List.of(), Unit.BodyAvailability.AVAILABLE, Optional.empty(), coverage(new Scopes.UnitScope(UNIT)), origin("unit"));
        var artifact = new ArtifactId(PUB, "fixture");
        var origins = new ArrayList<Origins.Origin>();
        for (String source : List.of("operation", "target", "expression", "place", "signature", "effect", "entry", "object", "storage", "sequence", "unit", "evidence-a"))
            origins.add(new Origins.Written(origin(source), artifact, Optional.empty(), List.of(), false));
        origins.add(new Origins.Derived(origin("evidence-b"), List.of(origin("evidence-a")), "fixture:contract-evidence@1"));
        var uncertainties = List.of(
                new Evidence.Uncertainty(gap("facts"), "fixture:PARTIAL_FACTS", List.of(Evidence.Dimension.values()), new Scopes.UnitScope(UNIT), "Not a producer certification", origin("unit")),
                new Evidence.Uncertainty(gap("contract"), "CONTRACT_UNKNOWN", List.of(Evidence.Dimension.EFFECTS), new Scopes.EntityScope(List.of(INVOKE)), "Authority not available", origin("evidence-a")),
                new Evidence.Uncertainty(gap("signature"), "SIGNATURE_UNKNOWN", List.of(Evidence.Dimension.VALUES), new Scopes.UnitScope(UNIT), "Inventory remainder unknown", origin("signature")));
        return new Publication(PUB, SemanticVersion.AIR_2_0_0, new Capabilities.Manifest(List.of(), List.of()),
                List.of(new Origins.Artifact(artifact, "invoke.manual", Optional.empty())), List.of(unit), cells, List.of(), List.of(), origins,
                coverage(new Scopes.PublicationScope(PUB)), uncertainties, List.of());
    }
    static Evidence.Coverage coverage(Scopes.FactScope scope) {
        return new Evidence.Coverage(Evidence.InventoryStatus.PARTIAL, scope, List.of(), List.of(gap("facts")));
    }
    static Operations.Invoke copy(Operations.Invoke i, Interactions.Target target, Interactions.Signature signature,
                                  Interactions.EffectBound effects, Control.InvocationOutcomes outcomes, Interactions.ContractKnowledge contract) {
        return new Operations.Invoke(i.header(), i.action(), target, i.arguments(), i.results(),
                new Interactions.ExternalSignature(signature), i.effectOperands(), effects, outcomes, contract);
    }
}
