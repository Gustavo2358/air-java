package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.model.Unit;
import io.github.gustavo2358.air.validation.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import static io.github.gustavo2358.air.json.InvokeOracle.*;
import static io.github.gustavo2358.air.json.AirJsonException.Code.*;
import static io.github.gustavo2358.air.json.Json.object;

/** W1B independent preservation/admission oracles; also runnable for staged RED evidence. */
final class InvokeChecks {
    private InvokeChecks() {}
    static void obligation(Publication p) {
        var result = AirValidator.validate(p);
        equal(ValidationResult.Status.STRUCTURALLY_VALID, result.status());
        require(result.diagnostics().traversalCompleted(), "complete structural traversal");
        require(result.issues().stream().anyMatch(i -> i.kind() == ValidationIssue.Kind.SEMANTIC_OBLIGATION && i.rule().equals("I-56")), "I-56 must remain visible");
        require(result.diagnostics().counts().keySet().stream().allMatch(k -> k == ValidationIssue.Kind.SEMANTIC_OBLIGATION), "obligations only");
    }
    static void literal() { roundTrip(publication(false, true)); }
    static void computed() { roundTrip(publication(true, false)); }
    static final String OP = "publication.units.0.sequences.0.terminator";
    static Interactions.Signature external(Operations.Invoke i) { return ((Interactions.ExternalSignature)i.signature()).signature(); }

    /** Wire facts authored from binding §§4/7/9/10, independently of BindingWriter/Reader. */
    static void wireOracle() {
        for (boolean computed : List.of(false, true)) {
            var p = publication(computed, !computed);
            var tree = tree(p); var term = at(tree, OP);
            equal(new java.util.HashSet<>(List.of("kind", "header", "action", "target", "arguments", "results", "signature",
                    "effectOperands", "effectBound", "outcomes", "contract")), ((Json.Obj)term).fields().keySet());
            equal(Json.value("invoke"), at(term, "kind")); equal(Json.value("call"), at(term, "action"));
            var target = object("kind", computed ? "computed" : "literal", "category", "program", "namespace", "fixture.resources",
                    "name", computed ? object("kind", "read", "header", operandWire("name-read", "CALL_TARGET", "expression"),
                            "place", object("kind", "object", "header", operandWire("name-place", "VALUE_READ", "place"), "object", ownedId("object", "name-cell"))) : Json.value("PROGA"),
                    "namePolicy", object("kind", "exact"), "origin", globalId("origin", "target"));
            equal(target, at(term, "target"));
            var empty = new Json.Arr(List.of());
            equal(empty, at(term, "arguments")); equal(empty, at(term, "results"));
            var inventory = object("known", empty, "remainder", object("kind", "none"));
            equal(object("kind", "external", "signature", object("parameters", inventory, "results", inventory,
                    "origin", globalId("origin", "signature"))), at(term, "signature"));
            var overwrite = new Json.Arr(computed ? List.of() : List.of(operandIdWire("effect-place")));
            equal(object("otherwise", object("reads", object("kind", "within", "scope", object("kind", "visible", "unit", globalId("unit", "caller"), "includingExternal", true)),
                    "writes", object("kind", "within", "scope", object("kind", "all", "publication", object("domain", "publication", "localId", "cp6-w1b-manual"), "includingEnvironment", true)),
                    "mustOverwrite", overwrite), "perOutcome", empty), at(term, "effectBound"));
            equal(new Json.Arr(computed ? List.of() : List.of(object("kind", "object", "header", operandWire("effect-place", "VALUE_WRITE", "effect"),
                    "object", ownedId("object", "name-cell")))), at(term, "effectOperands"));
            equal(object("known", new Json.Arr(List.of(object("kind", "normal", "label", ownedId("label", "continuation")),
                    object("kind", "exception", "tag", "fixture.failure", "destination", object("kind", "handler", "label", ownedId("label", "continuation"))),
                    object("kind", "any_exception", "destination", object("kind", "propagate")), object("kind", "halt"), object("kind", "diverge"))),
                    "remainder", object("kind", "within", "scope", object("kind", "unit", "unit", globalId("unit", "caller"),
                            "labels", true, "normalExit", true, "exceptionalExit", true, "halt", true, "diverge", true, "externalControl", true))), at(term, "outcomes"));
            equal(computed ? object("kind", "unknown", "uncertainty", globalId("uncertainty", "contract"))
                    : object("kind", "known", "reference", object("authority", "fixture.authority", "version", "1", "evidence",
                            new Json.Arr(List.of(globalId("origin", "evidence-a"), globalId("origin", "evidence-b"))))), at(term, "contract"));
            // Feed independently authored target back into the public decoder, not just the writer oracle.
            equal(p, new AirJson().decode(wire(edit(tree, OP + ".target", target))));
            equal(p.origins(), new AirJson().decode(wire(tree)).origins());
        }
        try (var in = InvokeOracle.class.getResourceAsStream("InvokeOracle.class")) {
            String constants = new String(java.util.Objects.requireNonNull(in).readAllBytes(), StandardCharsets.ISO_8859_1);
            for (String forbidden : List.of("BindingWriter", "BindingReader", "AirJson", "java/nio/file", "canonical.json"))
                require(!constants.contains(forbidden), "independent public-model oracle: " + forbidden);
        } catch (java.io.IOException error) { throw new AssertionError(error); }
    }
    static Json.Value globalId(String domain, String local) {
        return object("domain", domain, "publication", "cp6-w1b-manual", "localId", local);
    }
    static Json.Value ownedId(String domain, String local) {
        return object("domain", domain, "publication", "cp6-w1b-manual", "unit", "caller", "localId", local);
    }
    static Json.Value operandIdWire(String local) {
        return object("domain", "operand", "publication", "cp6-w1b-manual", "unit", "caller",
                "owner", object("kind", "operation", "localId", "interaction"), "localId", local);
    }
    static Json.Value operandWire(String local, String role, String origin) {
        return object("id", operandIdWire(local), "role", role, "origin", globalId("origin", origin));
    }
    static void namesAndPolicies() {
        var i = invoke(false, true);
        for (String name : List.of("", "PROGA   ", " aA ", "e\u0301", "é", "😀\u0000\n\"\\/")) {
            var target = new Interactions.LiteralTarget("table", "opaque namespace", name,
                    new Interactions.UnknownName(gap("facts")), origin("target"));
            var changed = new Operations.Invoke(i.header(), "execute", target, i.arguments(), i.results(), i.signature(),
                    i.effectOperands(), i.effectBound(), i.outcomes(), i.contract());
            roundTrip(publication(changed));
        }
        // Literal expression remains supported as a computed name, without interpretation or trimming.
        var literal = new Expressions.Literal(operand("literal-name", Operand.Role.CALL_TARGET, "expression"), new Values.TextValue("PROGA   "));
        roundTrip(publication(copy(i, new Interactions.ComputedTarget("program", "fixture.resources", literal,
                Interactions.ExactName.INSTANCE, origin("target")), external(i), i.effectBound(), i.outcomes(), i.contract())));
        // Read coverage is shared with Assign; it is not an Invoke-specific expression API.
        var scalar = ScalarAssignOracle.publication(); var unit = scalar.units().get(0); var seq = unit.sequences().get(0);
        var assign = (Operations.Assign)seq.instructions().get(0);
        var readPlace = new Places.ObjectPlace(new Operand.Header(new OperandId(assign.value().header().id().owner(), "read-place"),
                Operand.Role.VALUE_READ, assign.value().header().origin()), ScalarAssignOracle.OBJECT);
        var readAssign = new Operations.Assign(assign.header(), assign.destination(), new Expressions.Read(assign.value().header(), readPlace));
        var updated = withUnit(scalar, new Unit(unit.id(), unit.containingUnit(), unit.objects(), unit.visibleObjects(), unit.entries(),
                List.of(new Sequence(seq.label(), List.of(readAssign), seq.terminator(), seq.origin())), unit.completionPorts(),
                unit.body(), unit.bodyUnavailable(), unit.coverage(), unit.origin()));
        require(AirValidator.validate(updated).isStructurallyValid(), "Assign Read structural validity");
        equal(updated, new AirJson().decode(new AirJson().encode(updated)));
    }
    static void boundsAndOutcomes() {
        var i = invoke(true, true);
        for (Scopes.MemoryBound reads : List.of(Scopes.NoMemory.INSTANCE, new Scopes.WithinMemory(new Scopes.VisibleMemory(UNIT, false)),
                new Scopes.WithinMemory(new Scopes.AllMemory(PUB, false)))) {
            var effects = new Interactions.EffectBound(new Interactions.ForeignEffects(reads,
                    i.effectBound().otherwise().writes(), i.effectBound().otherwise().mustOverwrite()), List.of());
            roundTrip(publication(copy(i, i.target(), external(i), effects, i.outcomes(), i.contract())));
        }
        var none = new Interactions.EffectBound(new Interactions.ForeignEffects(Scopes.NoMemory.INSTANCE, Scopes.NoMemory.INSTANCE, List.of()), List.of());
        roundTrip(publication(copy(i, i.target(), external(i), none, i.outcomes(), i.contract())));
        var finite = new ArrayList<>(i.outcomes().known());
        finite.add(new Control.Exceptional("propagated", Control.Propagate.INSTANCE));
        finite.add(new Control.AnyException(new Control.Handler(END)));
        for (Control.InvocationAlternative a : finite)
            roundTrip(publication(copy(i, i.target(), external(i), i.effectBound(),
                    new Control.InvocationOutcomes(List.of(a), Scopes.NoControl.INSTANCE), i.contract())));
        for (int mask = 0; mask < 64; mask++) {
            var scope = new Scopes.UnitControl(UNIT, (mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0,
                    (mask & 8) != 0, (mask & 16) != 0, (mask & 32) != 0);
            roundTrip(publication(copy(i, i.target(), external(i), i.effectBound(),
                    new Control.InvocationOutcomes(i.outcomes().known(), new Scopes.WithinControl(scope)), i.contract())));
        }
        roundTrip(publication(copy(i, i.target(), external(i), i.effectBound(),
                new Control.InvocationOutcomes(List.of(), new Scopes.WithinControl(new Scopes.AllControl(PUB))), i.contract())));
    }
    static void signatures() {
        var i = invoke(false, true);
        var open = new Interactions.UnknownRemainder(gap("signature"));
        for (Interactions.UnknownBound parameters : List.of(Interactions.NoRemainder.INSTANCE, open))
            for (Interactions.UnknownBound results : List.of(Interactions.NoRemainder.INSTANCE, open)) {
                var s = new Interactions.Signature(new Interactions.ParameterInventory(List.of(), parameters),
                        new Interactions.ResultInventory(List.of(), results), origin("signature"));
                roundTrip(publication(copy(i, i.target(), s, i.effectBound(), i.outcomes(), i.contract())));
            }
    }
    static void existingBytes() throws java.io.IOException {
        require(Arrays.equals(Files.readAllBytes(Path.of("src/test/resources/goback.canonical.json")),
                new AirJson().encode(GobackOracle.publication())), "existing GOBACK bytes changed");
        require(Arrays.equals(Files.readAllBytes(Path.of("src/test/resources/scalar-assign.canonical.json")),
                new AirJson().encode(ScalarAssignOracle.publication())), "existing scalar Assign bytes changed");
    }
    static void invalidReferences() {
        var good = tree(publication(true, true));
        for (String path : List.of("target.name.place.object.localId", "target.origin.localId", "signature.signature.origin.localId",
                "effectBound.otherwise.mustOverwrite.0.localId", "effectBound.otherwise.reads.scope.unit.localId",
                "effectBound.otherwise.writes.scope.publication.localId", "outcomes.known.0.label.localId", "contract.reference.evidence.0.localId"))
            invalid(edit(good, OP + "." + path, Json.value("missing-identity")), "I-02");
        invalid(edit(good, OP + ".target.name.header.role", Json.value("VALUE_READ")), "I-11");
        invalid(edit(good, OP + ".target.name.place.header.id.owner.localId", Json.value("return")), "I-11");
        var unknown = tree(publication(true, false));
        invalid(edit(unknown, OP + ".contract.uncertainty.localId", Json.value("missing")), "I-02");
        invalid(edit(unknown, OP + ".contract.uncertainty.localId", Json.value("facts")), "I-49");
        invalid(edit(good, OP + ".outcomes.known", new Json.Arr(List.of(at(good, OP + ".outcomes.known.0"), at(good, OP + ".outcomes.known.0")))), "I-60");
    }
    static void invalid(Json.Value tree, String rule) {
        var p = new BindingReader().envelope(tree);
        var expected = AirValidator.validate(p);
        equal(ValidationResult.Status.INVALID_IR, expected.status());
        require(expected.issues().stream().anyMatch(i -> i.kind() == ValidationIssue.Kind.INVALID_IR && i.rule().equals(rule)), "invalid rule " + rule);
        for (Runnable action : List.<Runnable>of(() -> new AirJson().encode(p), () -> new AirJson().decode(wire(tree)))) {
            var error = failure(INVALID_IR, action);
            equal(expected, error.validationResult().orElseThrow());
        }
    }
    static void limitsAndCapabilities() {
        var p = publication(true, false); var bytes = new AirJson().encode(p);
        var limited = new AirJson(AirJson.Limits.defaults(), new ValidationOptions(128, 1, 1));
        for (Runnable action : List.<Runnable>of(() -> limited.encode(p), () -> limited.decode(bytes))) {
            var result = failure(RESOURCE_LIMIT, action).validationResult().orElseThrow();
            require(result.hasIssues(ValidationIssue.Kind.RESOURCE_LIMIT), "resource kind preserved");
            require(!result.diagnostics().traversalCompleted(), "resource traversal incomplete");
        }
        var required = new Capabilities.Capability("fixture.unsupported", "1");
        var unsupported = new Publication(p.id(), p.airVersion(), new Capabilities.Manifest(List.of(required), List.of()),
                p.artifacts(), p.units(), p.storage(), p.resources(), p.artifactRelations(), p.origins(), p.coverage(), p.uncertainties(), p.premises());
        require(AirValidator.validate(unsupported).hasIssues(ValidationIssue.Kind.UNSUPPORTED_CAPABILITY), "Validator unsupported capability");
        failure(UNSUPPORTED_CAPABILITY, () -> new AirJson().encode(unsupported));
        var tree = edit(tree(p), "publication.capabilities.required", new Json.Arr(List.of(object("name", "fixture.unsupported", "version", "1"))));
        failure(UNSUPPORTED_CAPABILITY, () -> new AirJson().decode(wire(tree)));
    }
    static Publication incompletePublication() {
        var p = publication(false, true); var u = p.units().get(0); var e = u.entries().get(0);
        var s = new Interactions.Signature(e.signature().parameters(), new Interactions.ResultInventory(List.of(),
                new Interactions.UnknownRemainder(gap("signature"))), origin("signature"));
        var extra = new Entries.Entry(new EntryId(UNIT, "second-entry"), Optional.of(END), s, e.state(), origin("entry"));
        return withUnit(p, new Unit(u.id(), u.containingUnit(), u.objects(), u.visibleObjects(), List.of(e, extra), u.sequences(),
                u.completionPorts(), u.body(), u.bodyUnavailable(), u.coverage(), u.origin()));
    }
    static void incomplete() {
        var p = incompletePublication();
        var result = AirValidator.validate(p);
        equal(ValidationResult.Status.INCOMPLETE_VALIDATION, result.status());
        require(result.hasIssues(ValidationIssue.Kind.VALIDATION_LIMIT), "non-obligation validation limit");
        require(!result.hasIssues(ValidationIssue.Kind.RESOURCE_LIMIT), "not a resource limit");
        require(result.issues().stream().anyMatch(i -> i.kind() == ValidationIssue.Kind.VALIDATION_LIMIT && i.rule().equals("PRECONDITION_NOT_DISCHARGED")), "identified shared-return rule");
        byte[] mapped = wire(tree(p)); equal(p, new BindingReader().envelope(tree(p)));
        for (Runnable action : List.<Runnable>of(() -> new AirJson().encode(p), () -> new AirJson().decode(mapped)))
            equal(result, failure(INCOMPLETE_VALIDATION, action).validationResult().orElseThrow());
        // I-56 is retained first; the blocking kind is beyond maximumIssues, but counts still block.
        var options = new ValidationOptions(Integer.MAX_VALUE, Integer.MAX_VALUE, 1);
        var shortResult = AirValidator.validate(p, options);
        equal(ValidationIssue.Kind.SEMANTIC_OBLIGATION, shortResult.issues().get(0).kind());
        require(shortResult.hasIssues(ValidationIssue.Kind.VALIDATION_LIMIT), "unretained limit still counted");
        var codec = new AirJson(AirJson.Limits.defaults(), options);
        failure(INCOMPLETE_VALIDATION, () -> codec.encode(p)); failure(INCOMPLETE_VALIDATION, () -> codec.decode(mapped));
    }
    static void physical() {
        for (boolean computed : List.of(false, true)) {
            var t = tree(publication(computed, !computed));
            physicalFields(t, at(t, OP), OP);
        }
        var t = tree(publication(true, true));
        for (String path : List.of("kind", "target.kind", "target.namePolicy.kind", "target.name.kind", "target.name.place.kind", "signature.kind",
                "signature.signature.parameters.remainder.kind", "effectBound.otherwise.reads.kind", "effectBound.otherwise.reads.scope.kind",
                "outcomes.known.0.kind", "outcomes.known.1.destination.kind", "outcomes.remainder.kind", "outcomes.remainder.scope.kind", "contract.kind"))
            failure(INPUT_ERROR, () -> new AirJson().decode(wire(edit(t, OP + "." + path, Json.value("unrecognized-variant")))));
        for (String invalidAlternative : List.of("return", "continue", "jump"))
            failure(INPUT_ERROR, () -> new AirJson().decode(wire(edit(t, OP + ".outcomes.known.0", object("kind", invalidAlternative)))));
        failure(INVALID_IR, () -> new AirJson().decode(wire(edit(t, OP + ".contract.reference.evidence", new Json.Arr(List.of())))));
        var empty = edit(edit(t, OP + ".outcomes.known", new Json.Arr(List.of())), OP + ".outcomes.remainder", object("kind", "none"));
        failure(INVALID_IR, () -> new AirJson().decode(wire(empty)));
        for (String path : List.of("action", "target.category", "target.namespace", "outcomes.known.1.tag", "contract.reference.authority", "contract.reference.version"))
            failure(IMPLEMENTATION_LIMIT, () -> new AirJson().decode(wire(edit(t, OP + "." + path, Json.value(" ")))));
    }
    static void physicalFields(Json.Value root, Json.Value node, String path) {
        if (node instanceof Json.Obj obj) {
            var extra = new LinkedHashMap<>(obj.fields()); extra.put("unexpected", Json.Nil.INSTANCE);
            failure(INPUT_ERROR, () -> new AirJson().decode(wire(edit(root, path, new Json.Obj(extra)))));
            String whole = new String(wire(obj), StandardCharsets.UTF_8);
            String full = new String(wire(root), StandardCharsets.UTF_8);
            String key = obj.fields().keySet().iterator().next();
            for (String duplicate : List.of(key, String.format(java.util.Locale.ROOT, "\\u%04x", (int)key.charAt(0)) + key.substring(1))) {
                String changed = whole.substring(0, whole.length() - 1) + ",\"" + duplicate + "\":null}";
                failure(INPUT_ERROR, () -> new AirJson().decode(full.replace(whole, changed).getBytes(StandardCharsets.UTF_8)));
            }
            for (var e : obj.fields().entrySet()) {
                String child = path + "." + e.getKey();
                failure(INPUT_ERROR, () -> new AirJson().decode(wire(edit(root, child, null))));
                failure(INPUT_ERROR, () -> new AirJson().decode(wire(edit(root, child, Json.Nil.INSTANCE))));
                Json.Value wrong = e.getValue() instanceof Json.Text ? new Json.Bool(true) : Json.value("wrong-type");
                failure(INPUT_ERROR, () -> new AirJson().decode(wire(edit(root, child, wrong))));
                physicalFields(root, e.getValue(), child);
            }
        } else if (node instanceof Json.Arr a) for (int j = 0; j < a.values().size(); j++) physicalFields(root, a.values().get(j), path + "." + j);
    }
    static void unsupported() {
        var t = tree(publication(true, true));
        var pairs = List.of(
                new Object[]{"target", object("kind", "internal", "entry", ownedId("entry", "entry"))},
                new Object[]{"signature", object("kind", "entry", "entry", ownedId("entry", "entry"))},
                new Object[]{"target.namePolicy", object("kind", "extension", "name", "fixture.policy", "version", "1")},
                new Object[]{"target.name.kind", Json.value("trim_right")},
                new Object[]{"target.name.kind", Json.value("fit_text")},
                new Object[]{"target.name.place.kind", Json.value("choice")},
                new Object[]{"effectBound.otherwise.reads.scope", object("kind", "objects", "objects", new Json.Arr(List.of(ownedId("object", "name-cell"))))},
                new Object[]{"effectBound.otherwise.reads.scope", object("kind", "storage", "storage", new Json.Arr(List.of(globalId("storage", "storage-name-cell"))))},
                new Object[]{"effectBound.otherwise.reads.scope", object("kind", "union", "members", new Json.Arr(List.of()))},
                new Object[]{"outcomes.remainder.scope", object("kind", "labels", "labels", new Json.Arr(List.of(ownedId("label", "continuation"))))},
                new Object[]{"outcomes.remainder.scope", object("kind", "union", "members", new Json.Arr(List.of()))});
        for (Object[] pair : pairs)
            failure(IMPLEMENTATION_LIMIT, () -> new AirJson().decode(wire(edit(t, OP + "." + pair[0], (Json.Value)pair[1]))));
        for (String inventory : List.of("arguments", "results", "signature.signature.parameters.known", "signature.signature.results.known", "effectBound.perOutcome"))
            failure(IMPLEMENTATION_LIMIT, () -> new AirJson().decode(wire(edit(t, OP + "." + inventory, new Json.Arr(List.of(object("kind", "unimplemented")))))));
        var i = invoke(true, true);
        var externalTarget = new Interactions.InternalTarget(new EntryId(UNIT, "entry"));
        failure(IMPLEMENTATION_LIMIT, () -> new AirJson().encode(publication(copy(i, externalTarget, external(i), i.effectBound(), i.outcomes(), i.contract()))));
        var args = new Operations.Invoke(i.header(), i.action(), i.target(), List.of(new Interactions.ReferenceArgument(effectPlace())), i.results(), i.signature(), i.effectOperands(), i.effectBound(), i.outcomes(), i.contract());
        failure(IMPLEMENTATION_LIMIT, () -> new AirJson().encode(publication(args)));
        var effects = new Interactions.EffectBound(i.effectBound().otherwise(), List.of(new Interactions.OutcomeEffects(Control.NormalOutcome.INSTANCE, i.effectBound().otherwise())));
        failure(IMPLEMENTATION_LIMIT, () -> new AirJson().encode(publication(copy(i, i.target(), external(i), effects, i.outcomes(), i.contract()))));
        for (Scopes.MemoryScope scope : List.of(new Scopes.ObjectsMemory(List.of(OBJECT)),
                new Scopes.StorageMemory(List.of(new StorageId(PUB, "storage-name-cell"))), new Scopes.MemoryUnion(List.of(new Scopes.AllMemory(PUB, true))))) {
            var bound = new Interactions.EffectBound(new Interactions.ForeignEffects(new Scopes.WithinMemory(scope),
                    i.effectBound().otherwise().writes(), List.of()), List.of());
            failure(IMPLEMENTATION_LIMIT, () -> new AirJson().encode(publication(copy(i, i.target(), external(i), bound, i.outcomes(), i.contract()))));
        }
        for (Scopes.ControlScope scope : List.of(new Scopes.LabelsControl(List.of(END)), new Scopes.ControlUnion(List.of(new Scopes.AllControl(PUB))))) {
            var outcomes = new Control.InvocationOutcomes(i.outcomes().known(), new Scopes.WithinControl(scope));
            failure(IMPLEMENTATION_LIMIT, () -> new AirJson().encode(publication(copy(i, i.target(), external(i), i.effectBound(), outcomes, i.contract()))));
        }
        var extension = new Interactions.LiteralTarget("program", "fixture.resources", "name", new Interactions.ExtensionName("fixture.policy", "1"), origin("target"));
        failure(IMPLEMENTATION_LIMIT, () -> new AirJson().encode(publication(copy(i, extension, external(i), i.effectBound(), i.outcomes(), i.contract()))));
        var internalSignature = new Operations.Invoke(i.header(), i.action(), i.target(), i.arguments(), i.results(),
                new Interactions.EntrySignature(new EntryId(UNIT, "entry")), i.effectOperands(), i.effectBound(), i.outcomes(), i.contract());
        failure(IMPLEMENTATION_LIMIT, () -> new AirJson().encode(publication(internalSignature)));
        var results = new Operations.Invoke(i.header(), i.action(), i.target(), i.arguments(), List.of(effectPlace()), i.signature(),
                i.effectOperands(), i.effectBound(), i.outcomes(), i.contract());
        failure(IMPLEMENTATION_LIMIT, () -> new AirJson().encode(publication(results)));
    }
    static void scale() {
        long previousBytes = 0;
        for (int n : new int[]{500, 1000}) {
            var p = publication(false, false); var u = p.units().get(0); var i = invoke(false, false);
            var seqs = new ArrayList<Sequence>();
            for (int j = 0; j < n; j++) {
                var op = new Operations.Invoke(header(new OperationId(UNIT, "invoke-" + j)), i.action(), i.target(), List.of(), List.of(),
                        i.signature(), List.of(), i.effectBound(), i.outcomes(), i.contract());
                seqs.add(new Sequence(j == 0 ? START : new LabelId(UNIT, "site-" + j), List.of(), op, origin("sequence")));
            }
            seqs.add(u.sequences().get(1));
            p = withUnit(p, new Unit(u.id(), u.containingUnit(), u.objects(), u.visibleObjects(), u.entries(), seqs,
                    u.completionPorts(), u.body(), u.bodyUnavailable(), u.coverage(), u.origin()));
            // The original occurrence remains referenced by fixture uncertainty scope; give it the first site.
            var first = (Operations.Invoke)seqs.get(0).terminator();
            seqs.set(0, new Sequence(START, List.of(), new Operations.Invoke(header(INVOKE), first.action(), first.target(), first.arguments(), first.results(),
                    first.signature(), first.effectOperands(), first.effectBound(), first.outcomes(), first.contract()), origin("sequence")));
            p = withUnit(p, new Unit(u.id(), u.containingUnit(), u.objects(), u.visibleObjects(), u.entries(), seqs,
                    u.completionPorts(), u.body(), u.bodyUnavailable(), u.coverage(), u.origin()));
            byte[] bytes = new AirJson().encode(p); var decoded = new AirJson().decode(bytes); equal(p, decoded);
            require(Arrays.equals(bytes, new AirJson().encode(decoded)), "scale canonical bytes");
            var r = AirValidator.validate(decoded); equal(n + 1, r.statistics().operations()); equal(0, r.statistics().operands());
            equal(2L * n, r.diagnostics().count(ValidationIssue.Kind.SEMANTIC_OBLIGATION));
            if (previousBytes > 0) require(bytes.length > previousBytes * 1.9 && bytes.length < previousBytes * 2.1, "linear Invoke wire size");
            System.out.println("W1B SCALE invokes=" + n + " bytes=" + bytes.length + " entities=" + r.statistics().entities() + " obligations=" + r.diagnostics().count(ValidationIssue.Kind.SEMANTIC_OBLIGATION));
            previousBytes = bytes.length;
        }
    }
    static Publication withUnit(Publication p, Unit u) {
        return new Publication(p.id(), p.airVersion(), p.capabilities(), p.artifacts(), List.of(u), p.storage(), p.resources(),
                p.artifactRelations(), p.origins(), p.coverage(), p.uncertainties(), p.premises());
    }
    static Json.Value tree(Publication p) { return new BindingWriter().envelope(p); }
    static byte[] wire(Json.Value v) { return Json.write(v, AirJson.Limits.defaults()); }
    static Json.Value at(Json.Value value, String path) {
        for (String key : path.split("\\.")) value = value instanceof Json.Obj o ? o.fields().get(key) : ((Json.Arr)value).values().get(Integer.parseInt(key));
        return value;
    }
    static Json.Value edit(Json.Value value, String path, Json.Value replacement) { return edit(value, path.split("\\."), 0, replacement); }
    static Json.Value edit(Json.Value value, String[] path, int depth, Json.Value replacement) {
        if (depth == path.length) return replacement;
        if (value instanceof Json.Obj o) {
            var f = new LinkedHashMap<>(o.fields());
            if (replacement == null && depth == path.length - 1) f.remove(path[depth]);
            else f.put(path[depth], edit(f.get(path[depth]), path, depth + 1, replacement));
            return new Json.Obj(f);
        }
        var a = new ArrayList<>(((Json.Arr)value).values()); int index = Integer.parseInt(path[depth]);
        a.set(index, edit(a.get(index), path, depth + 1, replacement)); return new Json.Arr(a);
    }
    static AirJsonException failure(AirJsonException.Code code, Runnable action) {
        try { action.run(); } catch (AirJsonException error) { equal(code, error.code()); return error; }
        throw new AssertionError("expected " + code + " rejection");
    }
    static void roundTrip(Publication p) {
        obligation(p);
        byte[] bytes = new AirJson().encode(p);
        var decoded = new AirJson().decode(bytes);
        equal(p, decoded); obligation(decoded);
        equal(AirValidator.validate(p), AirValidator.validate(decoded));
        require(Arrays.equals(bytes, new AirJson().encode(decoded)), "canonical re-encode bytes");
        require(Arrays.equals(bytes, new AirJson().encode(p)), "independent codec bytes");
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("existing-bytes")) { existingBytes(); return; }
        var p = publication(false, true); obligation(p);
        System.out.println("Validator=STRUCTURALLY_VALID; traversalCompleted=true; SEMANTIC_OBLIGATION I-56 present");
        literal(); computed(); wireOracle(); namesAndPolicies(); boundsAndOutcomes(); signatures();
        invalidReferences(); limitsAndCapabilities(); incomplete(); physical(); unsupported(); scale();
        if (args.length == 1) {
            Path output = Path.of(args[0]); Files.createDirectories(output);
            Files.write(output.resolve("literal.json"), new AirJson().encode(publication(false, true)));
            Files.write(output.resolve("computed.json"), new AirJson().encode(publication(true, false)));
        }
        System.out.println("W1B invoke checks PASS");
    }
    static void equal(Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError("expected " + expected + ", got " + actual);
    }
    static void require(boolean ok, String detail) { if (!ok) throw new AssertionError(detail); }
}
