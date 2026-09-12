package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Unit;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.validation.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static io.github.gustavo2358.air.json.W2cOracle.*;
import static io.github.gustavo2358.air.json.InvokeChecks.at;
import static io.github.gustavo2358.air.json.InvokeChecks.edit;
import static io.github.gustavo2358.air.json.InvokeChecks.wire;
import static io.github.gustavo2358.air.json.InvokeChecks.failure;
import static io.github.gustavo2358.air.json.AirJsonException.Code.*;
import static io.github.gustavo2358.air.json.Json.object;

/** W2C independent model and wire comparisons, runnable by gap before implementation. */
final class W2cChecks {
    private W2cChecks() {}
    static final String TERM = "publication.units.0.sequences.0.terminator";
    static final String PRED = TERM + ".predicate";
    static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    static void equal(Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError("expected=" + expected + " actual=" + actual);
    }
    static void bytes(byte[] expected, byte[] actual) { require(Arrays.equals(expected, actual), "canonical bytes differ"); }
    static ValidationResult valid(Publication p) {
        var result = AirValidator.validate(p);
        equal(ValidationResult.Status.STRUCTURALLY_VALID, result.status());
        require(result.diagnostics().traversalCompleted(), "structural traversal incomplete");
        return result;
    }
    static void roundTrip(Publication p) {
        var before = valid(p); var codec = new AirJson(); var bytes = codec.encode(p); var restored = codec.decode(bytes);
        equal(p, restored); bytes(bytes, codec.encode(restored)); equal(before, AirValidator.validate(restored));
    }
    static void form(String form) {
        var p = publication(form); valid(p);
        var codec = new AirJson(); var expected = wire(W2cWireOracle.document(form));
        try { bytes(expected, codec.encode(p)); equal(p, codec.decode(expected)); roundTrip(p); }
        catch (AirJsonException e) { throw new AssertionError("supported " + form + " unexpectedly rejected: " + e.code() + " at " + e.path(), e); }
    }
    static void jump() { form("jump"); }
    static void branch() { form("branch"); }
    static void bool() { form("bool"); }
    static void unknown() { form("unknown"); form("unknown-empty"); }
    static void premise() { form("premise"); }
    static void complete() { form("full"); }
    static void wireOracle() {
        for (String f : List.of("jump", "branch", "bool", "unknown", "unknown-empty", "premise", "full")) form(f);
        for (Class<?> oracle : List.of(W2cOracle.class, W2cWireOracle.class)) {
            try (var in = oracle.getResourceAsStream(oracle.getSimpleName() + ".class")) {
                String constants = new String(java.util.Objects.requireNonNull(in).readAllBytes(), StandardCharsets.ISO_8859_1);
                for (String forbidden : List.of("BindingReader", "BindingWriter", "AirJson", "java/nio/file"))
                    require(!constants.contains(forbidden), "independent oracle depends on " + forbidden);
                if (oracle == W2cWireOracle.class) require(!constants.contains("W2cOracle"), "wire expected derives from model oracle");
            } catch (java.io.IOException e) { throw new AssertionError(e); }
        }
    }
    static void obligations() {
        var p = publication("full"); var before = valid(p);
        var after = valid(new AirJson().decode(new AirJson().encode(p)));
        for (String rule : List.of("I-09", "I-59")) {
            var expected = before.issues().stream().filter(i -> i.rule().equals(rule)).toList();
            equal(1, expected.size()); equal(ValidationIssue.Kind.SEMANTIC_OBLIGATION, expected.get(0).kind());
            equal(expected, after.issues().stream().filter(i -> i.rule().equals(rule)).toList());
        }
        equal(before, after); equal(2L, after.diagnostics().count(ValidationIssue.Kind.SEMANTIC_OBLIGATION));
        // Truncated retention must not erase the full obligation count or turn it into a certificate.
        var limited = AirValidator.validate(p, new ValidationOptions(10000, 1000, 1));
        equal(2L, limited.diagnostics().count(ValidationIssue.Kind.SEMANTIC_OBLIGATION));
        equal(1, limited.issues().size());
    }
    static Publication predicate(Publication p, Expression expression) {
        var u = p.units().get(0); var sequences = new ArrayList<>(u.sequences()); var first = sequences.get(0);
        var b = (Operations.Branch)first.terminator();
        sequences.set(0, new Sequence(first.label(), first.instructions(), new Operations.Branch(b.header(), expression,
                b.trueDestination(), b.falseDestination()), first.origin()));
        return copy(p, List.of(sequences(u, sequences)), p.storage(), p.premises());
    }
    static void openReadsAndOrder() {
        var p = publication("full"); var b = (Operations.Branch)p.units().get(0).sequences().get(0).terminator();
        var unknown = (Expressions.Unknown)b.predicate();
        for (Scopes.MemoryBound bound : List.of(new Scopes.WithinMemory(new Scopes.VisibleMemory(UNIT, true)),
                new Scopes.WithinMemory(new Scopes.AllMemory(PUB, false)))) {
            var changed = predicate(p, new Expressions.Unknown(unknown.header(), unknown.typeRef(), unknown.dependencies(), bound, unknown.reason()));
            roundTrip(changed);
            var scope = bound instanceof Scopes.WithinMemory w && w.scope() instanceof Scopes.VisibleMemory
                    ? object("kind", "visible", "unit", W2cWireOracle.global("unit", "unit"), "includingExternal", true)
                    : object("kind", "all", "publication", object("domain", "publication", "localId", "cp6-w2c-manual"), "includingEnvironment", false);
            bytes(wire(edit(W2cWireOracle.document("full"), PRED + ".remainingReads", object("kind", "within", "scope", scope))), new AirJson().encode(changed));
        }
        var reversed = new ArrayList<>(p.units().get(0).sequences()); java.util.Collections.reverse(reversed);
        roundTrip(copy(p, List.of(sequences(p.units().get(0), reversed)), p.storage(), p.premises()));
        var dependencies = List.<Expression>of(read("if", "second", Operand.Role.VALUE_READ, PGM),
                new Expressions.Literal(operand("if", "literal", Operand.Role.VALUE_READ, "read"), new Values.TextValue(" é😀 ")),
                read("if", "first", Operand.Role.VALUE_READ, FLAG));
        roundTrip(predicate(p, new Expressions.Unknown(unknown.header(), unknown.typeRef(), dependencies, unknown.remainingReads(), unknown.reason())));
    }
    private static int negatives;
    static AirJsonException reject(Json.Value tree, AirJsonException.Code code, String rule) {
        var e = failure(code, () -> new AirJson().decode(wire(tree)));
        if (rule != null) require(e.issues().stream().anyMatch(i -> i.kind() == ValidationIssue.Kind.INVALID_IR && i.rule().equals(rule)), "missing expected rule " + rule);
        else require(e.issues().isEmpty(), "physical/coverage failure must not invent AIR rules");
        negatives++;
        return e;
    }
    static void invalid(Json.Value tree, String rule) {
        var error = reject(tree, INVALID_IR, rule);
        // The independent rule is fixed above. If wire shape is materializable, test the public writer too.
        Publication p;
        try { p = new BindingReader().envelope(tree); }
        catch (AirJsonException local) { equal(INVALID_IR, local.code()); return; }
        var encoded = failure(INVALID_IR, () -> new AirJson().encode(p));
        equal(AirValidator.validate(p), error.validationResult().orElseThrow());
        equal(error.validationResult(), encoded.validationResult());
    }
    static void branchNegatives() {
        int start = negatives; var full = W2cWireOracle.document("full"); var jump = W2cWireOracle.document("jump");
        for (String field : List.of("trueDestination", "falseDestination")) {
            invalid(edit(full, TERM + "." + field + ".localId", Json.value("missing")), "I-02");
            invalid(edit(full, TERM + "." + field + ".unit", Json.value("foreign")), "I-02");
        }
        invalid(edit(jump, TERM + ".destination.localId", Json.value("missing")), "I-02");
        invalid(edit(jump, TERM + ".destination.unit", Json.value("foreign")), "I-02");
        invalid(edit(full, PRED + ".header.role", Json.value("VALUE_READ")), "I-11");
        invalid(edit(full, PRED + ".typeRef", W2cWireOracle.type("text")), "I-08");
        invalid(edit(full, "publication.units.0.sequences.0.instructions", W2cWireOracle.list(at(full, TERM))), "I-04");
        invalid(edit(jump, TERM, object("kind", "nop", "header", at(jump, TERM + ".header"))), "I-04");
        reject(edit(full, TERM + ".kind", Json.value("future.branch")), INPUT_ERROR, null);
        reject(edit(jump, TERM + ".kind", Json.value("future.jump")), INPUT_ERROR, null);
        System.out.println("W2C NEGATIVE branch/jump cases=" + (negatives - start));
    }
    static void unknownNegatives() {
        int start = negatives; var full = W2cWireOracle.document("full");
        invalid(edit(full, PRED + ".reason.localId", Json.value("missing")), "I-02");
        invalid(edit(full, PRED + ".dependencies.0.place.object.localId", Json.value("missing")), "I-02");
        for (String site : List.of(PRED, PRED + ".dependencies.0", PRED + ".dependencies.0.place"))
            invalid(edit(full, site + ".header.id.owner.localId", Json.value("then")), "I-11");
        var assignment = W2cWireOracle.document("unknown");
        invalid(edit(assignment, "publication.units.0.sequences.0.instructions.0.value.header.role", Json.value("CALL_TARGET")), "I-11");
        reject(edit(full, PRED + ".typeRef", object("kind", "unknown_type", "uncertainty", W2cWireOracle.global("uncertainty", "facts"))), IMPLEMENTATION_LIMIT, null);
        reject(edit(full, PRED + ".kind", Json.value("future.unknown")), INPUT_ERROR, null);
        reject(edit(full, PRED + ".dependencies.0", object("kind", "unary", "header", at(full, PRED + ".header"), "operator", "not", "operand", at(full, PRED + ".dependencies.0"))), IMPLEMENTATION_LIMIT, null);
        for (String field : List.of("dependencies", "remainingReads", "reason")) {
            reject(edit(full, PRED + "." + field, null), INPUT_ERROR, null);
            reject(edit(full, PRED + "." + field, Json.Nil.INSTANCE), INPUT_ERROR, null);
        }
        reject(edit(full, PRED + ".dependencies", object("kind", "none")), INPUT_ERROR, null);
        reject(edit(full, PRED + ".header.role", Json.value("NOT_A_ROLE")), INPUT_ERROR, null);
        // Model unknown_type is legitimate in general, but it cannot satisfy this BOOL predicate.
        var p = publication("full"); var u = (Expressions.Unknown)((Operations.Branch)p.units().get(0).sequences().get(0).terminator()).predicate();
        var invalidModel = predicate(p, new Expressions.Unknown(u.header(), new Types.UnknownType(gap("facts")), u.dependencies(), u.remainingReads(), u.reason()));
        require(AirValidator.validate(invalidModel).issues().stream().anyMatch(i -> i.rule().equals("I-08")), "unknown_type cannot satisfy BOOL");
        failure(IMPLEMENTATION_LIMIT, () -> new AirJson().encode(invalidModel));
        System.out.println("W2C NEGATIVE unknown cases=" + (negatives - start));
    }
    static Json.Value sameDomainWire() {
        return object("kind", "same_domain", "left", object("kind", "object", "object", W2cWireOracle.owned("object", "FLAG")),
                "right", object("kind", "object", "object", W2cWireOracle.owned("object", "WS-PGM")), "scope", object("kind", "publication"));
    }
    static void premiseNegatives() {
        int start = negatives; var full = W2cWireOracle.document("full"); String premise = "publication.premises.0";
        invalid(edit(full, premise + ".id", W2cWireOracle.global("storage", "independent")), "I-02");
        invalid(edit(full, "publication.premises", W2cWireOracle.list(at(full, premise), at(full, premise))), "I-01");
        invalid(edit(full, premise + ".id.publication", Json.value("foreign")), "I-01");
        invalid(edit(full, premise + ".origin.localId", Json.value("missing")), "I-02");
        invalid(edit(full, premise + ".assertion.storage.0.localId", Json.value("missing")), "I-02");
        invalid(edit(full, premise + ".assertion.storage.0.publication", Json.value("foreign")), "I-02");
        invalid(edit(full, premise + ".assertion.storage.0", W2cWireOracle.owned("object", "WS-PGM")), "I-02");
        for (Json.Arr members : List.of(W2cWireOracle.list(), W2cWireOracle.list(W2cWireOracle.global("storage", "cell-FLAG")),
                W2cWireOracle.list(W2cWireOracle.global("storage", "cell-FLAG"), W2cWireOracle.global("storage", "cell-FLAG"))))
            invalid(edit(full, premise + ".assertion.storage", members), "I-58");
        reject(edit(full, premise + ".assertion.storage", object("kind", "storage")), INPUT_ERROR, null);
        reject(edit(full, premise + ".assertion.kind", Json.value("unknown_assertion")), INPUT_ERROR, null);
        reject(edit(full, premise + ".assertion", sameDomainWire()), IMPLEMENTATION_LIMIT, null);
        for (String field : List.of("authority", "justification")) {
            reject(edit(full, premise + "." + field, Json.value(true)), INPUT_ERROR, null);
            for (String blank : List.of("", "  ", "\t\n")) reject(edit(full, premise + "." + field, Json.value(blank)), IMPLEMENTATION_LIMIT, null);
        }
        reject(edit(full, premise + ".id.localId", Json.value("")), IMPLEMENTATION_LIMIT, null);
        invalid(edit(full, "publication.coverage.scope", object("kind", "entities", "entities", W2cWireOracle.list(W2cWireOracle.global("premise", "missing")))), "I-02");
        var p = publication("premise"); var pr = p.premises().get(0);
        var same = new Proofs.Premise(pr.id(), pr.authority(), pr.justification(), pr.origin(),
                new Proofs.SameDomain(new Proofs.ObjectDomain(FLAG), new Proofs.ObjectDomain(PGM), Proofs.PublicationDomain.INSTANCE));
        var model = copy(p, p.units(), p.storage(), List.of(same)); valid(model);
        failure(IMPLEMENTATION_LIMIT, () -> new AirJson().encode(model));
        // An unsupported second item must make the entire Publication fail, in both directions.
        reject(edit(full, "publication.premises", W2cWireOracle.list(at(full, premise), edit(at(full, premise), "assertion", sameDomainWire()))), IMPLEMENTATION_LIMIT, null);
        failure(IMPLEMENTATION_LIMIT, () -> new AirJson().encode(copy(p, p.units(), p.storage(), List.of(pr, same))));
        System.out.println("W2C NEGATIVE premise cases=" + (negatives - start));
    }
    static void closedShapes() {
        int start = negatives;
        for (String form : List.of("jump", "full")) {
            var tree = W2cWireOracle.document(form);
            var paths = new ArrayList<>(List.of(TERM, TERM + ".header", TERM + ".header.id"));
            if (form.equals("jump")) paths.add(TERM + ".destination");
            else paths.addAll(List.of(TERM + ".trueDestination", TERM + ".falseDestination", PRED, PRED + ".header", PRED + ".typeRef",
                    PRED + ".typeRef.type", PRED + ".remainingReads", PRED + ".reason", PRED + ".dependencies.0", PRED + ".dependencies.0.place",
                    "publication.premises.0", "publication.premises.0.id", "publication.premises.0.assertion", "publication.premises.0.assertion.storage.0"));
            for (String path : paths) {
                reject(edit(tree, path + ".extra", Json.value("unexpected")), INPUT_ERROR, null);
                for (String field : ((Json.Obj)at(tree, path)).fields().keySet()) reject(edit(tree, path + "." + field, null), INPUT_ERROR, null);
            }
        }
        System.out.println("W2C NEGATIVE closed objects cases=" + (negatives - start));
    }
    static Operations.Header identified(String id) {
        var h = header("if");
        return new Operations.Header(new OperationId(UNIT, id), h.origin(), h.coverage(), h.precision(), h.uncertainties());
    }
    static void composition() {
        for (boolean computed : List.of(false, true)) {
            var p = publication("full"); var u = p.units().get(0); var seq = new ArrayList<>(u.sequences());
            var assign = new Operations.Assign(header("assign"), new Places.ObjectPlace(operand("assign", "destination", Operand.Role.VALUE_WRITE, "place"), PGM),
                    new Expressions.Literal(operand("assign", "value", Operand.Role.VALUE_READ, "read"), new Values.TextValue("PROGA   ")));
            var then = seq.get(1); seq.set(1, new Sequence(then.label(), List.of(assign), then.terminator(), then.origin()));
            Interactions.Target target = computed
                    ? new Interactions.ComputedTarget("program", "fixture", read("merge", "call-target", Operand.Role.CALL_TARGET, PGM), Interactions.ExactName.INSTANCE, origin("read"))
                    : new Interactions.LiteralTarget("program", "fixture", "PROGA", Interactions.ExactName.INSTANCE, origin("read"));
            var invoke = new Operations.Invoke(header("merge"), "call", target, List.of(), List.of(),
                    new Interactions.ExternalSignature(u.entries().get(0).signature()), List.of(),
                    new Interactions.EffectBound(new Interactions.ForeignEffects(new Scopes.WithinMemory(new Scopes.VisibleMemory(UNIT, true)),
                            new Scopes.WithinMemory(new Scopes.AllMemory(PUB, false)), List.of()), List.of()),
                    new Control.InvocationOutcomes(List.of(new Control.Normal(label("Lreturn")), new Control.AnyException(Control.Propagate.INSTANCE)),
                            new Scopes.WithinControl(new Scopes.UnitControl(UNIT, true, true, true, true, true, true))),
                    new Interactions.KnownContract(new Interactions.ContractRef("fixture.authority", "1", List.of(origin("premise"), origin("read")))));
            seq.set(3, new Sequence(label("Lmerge"), List.of(), invoke, origin("sequence")));
            seq.add(new Sequence(label("Lreturn"), List.of(), new Operations.Return(identified("return"), List.of()), origin("sequence")));
            var combined = copy(p, List.of(sequences(u, seq)), p.storage(), p.premises()); roundTrip(combined);
            equal(3L, valid(combined).diagnostics().count(ValidationIssue.Kind.SEMANTIC_OBLIGATION));
            require(valid(combined).issues().stream().anyMatch(i -> i.rule().equals("I-56")), "Invoke obligation missing in W1/W2 composition");
        }
    }
    static void foreignUnit() {
        var p = publication("full"); var u = p.units().get(0); var foreign = new UnitId(PUB, "foreign");
        var foreignLabel = new LabelId(foreign, "end"); var foreignOp = new OperationId(foreign, "return"); var h = header("merge");
        var other = new Unit(foreign, java.util.Optional.empty(), List.of(), List.of(),
                List.of(new Entries.Entry(new EntryId(foreign, "entry"), java.util.Optional.of(foreignLabel), u.entries().get(0).signature(), u.entries().get(0).state(), origin("entry"))),
                List.of(new Sequence(foreignLabel, List.of(), new Operations.Return(new Operations.Header(foreignOp, h.origin(), h.coverage(), h.precision(), h.uncertainties()), List.of()), origin("sequence"))),
                List.of(), Unit.BodyAvailability.AVAILABLE, java.util.Optional.empty(), coverage(new Scopes.UnitScope(foreign)), origin("unit"));
        var both = copy(p, List.of(u, other), p.storage(), p.premises()); valid(both);
        var b = (Operations.Branch)u.sequences().get(0).terminator();
        for (Terminator bad : List.of(new Operations.Branch(b.header(), b.predicate(), foreignLabel, b.falseDestination()),
                new Operations.Branch(b.header(), b.predicate(), b.trueDestination(), foreignLabel), new Operations.Jump(b.header(), foreignLabel))) {
            var seq = new ArrayList<>(u.sequences()); seq.set(0, new Sequence(label("Lif"), List.of(), bad, origin("sequence")));
            var invalid = copy(p, List.of(sequences(u, seq), other), p.storage(), p.premises());
            var error = failure(INVALID_IR, () -> new AirJson().encode(invalid));
            require(error.issues().stream().anyMatch(i -> i.rule().equals("I-02")), "existing foreign label must reject by ownership");
            invalid(new BindingWriter().envelope(invalid), "I-02");
        }
    }
    static Publication scaled(String dimension, int n) {
        var p = publication("full"); var u = p.units().get(0); var b = (Operations.Branch)u.sequences().get(0).terminator();
        var unknown = (Expressions.Unknown)b.predicate();
        if (dimension.equals("dependencies")) {
            var deps = new ArrayList<Expression>();
            for (int j = 0; j < n; j++) deps.add(read("if", "dependency-" + j, Operand.Role.VALUE_READ, j % 2 == 0 ? FLAG : PGM));
            return predicate(p, new Expressions.Unknown(unknown.header(), unknown.typeRef(), deps, unknown.remainingReads(), unknown.reason()));
        }
        if (dimension.equals("premises")) {
            var premises = new ArrayList<Proofs.Premise>();
            for (int j = 0; j < n; j++) premises.add(W2cOracle.premise("premise-" + j, List.of(cell("WS-PGM"), cell("FLAG"))));
            return copy(p, p.units(), p.storage(), premises);
        }
        if (dimension.equals("members")) {
            var cells = new ArrayList<>(p.storage()); var ids = new ArrayList<StorageId>();
            for (int j = n - 1; j >= 0; j--) {
                var id = cell("member-" + j); ids.add(id);
                cells.add(new Memory.Cell(new Memory.StorageHeader(id, java.util.Optional.of(UNIT), Memory.Lifetime.ACTIVATION,
                        Memory.Visibility.PRIVATE, origin("storage")), Types.known(Types.Builtin.TEXT)));
            }
            return copy(p, p.units(), cells, List.of(W2cOracle.premise("independent", ids)));
        }
        if (!dimension.equals("controls")) throw new AssertionError("unknown scale dimension");
        var seq = new ArrayList<>(u.sequences());
        for (int j = 0; j < n; j++) {
            String name = "branch-" + j;
            seq.add(new Sequence(label(name), List.of(), new Operations.Branch(identified(name), W2cOracle.unknown(name, Operand.Role.PREDICATE, List.of(), Scopes.NoMemory.INSTANCE),
                    label("jump-" + j), label("Lmerge")), origin("sequence")));
            seq.add(new Sequence(label("jump-" + j), List.of(), new Operations.Jump(identified("jump-" + j), label("Lmerge")), origin("sequence")));
        }
        return copy(p, List.of(sequences(u, seq)), p.storage(), p.premises());
    }
    static long nodes(Json.Value root) {
        var pending = new java.util.ArrayDeque<Json.Value>(); pending.push(root); long count = 0;
        while (!pending.isEmpty()) {
            var value = pending.pop(); count++;
            if (value instanceof Json.Obj o) o.fields().values().forEach(pending::push);
            else if (value instanceof Json.Arr a) a.values().forEach(pending::push);
        }
        return count;
    }
    static void scale() {
        // Subtract the constant fixture overhead before comparing independently varied dimensions.
        for (String dimension : List.of("controls", "dependencies", "premises", "members")) {
            long previousNodes = 0; int previousBytes = 0;
            for (int n : new int[]{256, 512}) {
                var p = scaled(dimension, n); roundTrip(p); var r = valid(p);
                var encoded = new AirJson().encode(p); long nodeCount = nodes(Json.parse(encoded, AirJson.Limits.defaults()));
                if (dimension.equals("controls")) { equal(4 + 2 * n, r.statistics().operations()); equal(3 + n, r.statistics().operands()); }
                if (dimension.equals("dependencies")) { equal(4, r.statistics().operations()); equal(1 + 2 * n, r.statistics().operands()); }
                if (dimension.equals("premises")) { equal(n, p.premises().size()); equal((long)n + 1, r.diagnostics().count(ValidationIssue.Kind.SEMANTIC_OBLIGATION)); }
                if (dimension.equals("members")) equal(n, ((Proofs.DisjointStorage)p.premises().get(0).assertion()).storage().size());
                equal(0L, r.statistics().domainQueries());
                if (previousBytes > 0) {
                    var baseline = scaled(dimension, 2); var baseBytes = new AirJson().encode(baseline);
                    long baseNodes = nodes(Json.parse(baseBytes, AirJson.Limits.defaults()));
                    double nodesRatio = (double)(nodeCount - baseNodes) / (previousNodes - baseNodes);
                    double bytesRatio = (double)(encoded.length - baseBytes.length) / (previousBytes - baseBytes.length);
                    require(nodesRatio > 1.98 && nodesRatio < 2.03, "nonlinear physical node growth " + dimension);
                    require(bytesRatio > 1.98 && bytesRatio < 2.05, "nonlinear byte growth " + dimension);
                }
                System.out.println("W2C SCALE dimension=" + dimension + " n=" + n + " bytes=" + encoded.length + " nodes=" + nodeCount
                        + " entities=" + r.statistics().entities() + " operations=" + r.statistics().operations() + " operands=" + r.statistics().operands()
                        + " domainQueries=" + r.statistics().domainQueries() + " obligations=" + r.diagnostics().count(ValidationIssue.Kind.SEMANTIC_OBLIGATION));
                previousNodes = nodeCount; previousBytes = encoded.length;
            }
        }
    }
    static void nestedUnknown() {
        var p = publication("full"); Expression expression = read("if", "leaf", Operand.Role.VALUE_READ, FLAG);
        for (int n = 0; n < 600; n++) expression = new Expressions.Unknown(operand("if", "nested-" + n, n == 599 ? Operand.Role.PREDICATE : Operand.Role.VALUE_READ, "predicate"),
                Types.known(Types.Builtin.BOOL), List.of(expression), Scopes.NoMemory.INSTANCE, gap("predicate-value-unknown"));
        p = predicate(p, expression); valid(p);
        var bytes = new AirJson().encode(p); var decoded = new AirJson().decode(bytes); valid(decoded);
        bytes(bytes, new AirJson().encode(decoded));
        Expression restored = ((Operations.Branch)decoded.units().get(0).sequences().get(0).terminator()).predicate();
        for (int n = 599; n >= 0; n--) {
            require(restored instanceof Expressions.Unknown, "nested Unknown lost"); var u = (Expressions.Unknown)restored;
            equal("nested-" + n, u.header().id().localId()); equal(1, u.dependencies().size()); equal(Types.known(Types.Builtin.BOOL), u.typeRef());
            equal(gap("predicate-value-unknown"), u.reason()); equal(Scopes.NoMemory.INSTANCE, u.remainingReads()); restored = u.dependencies().get(0);
        }
        equal(read("if", "leaf", Operand.Role.VALUE_READ, FLAG), restored);
        equal(601L, valid(decoded).diagnostics().count(ValidationIssue.Kind.SEMANTIC_OBLIGATION));
    }
    static void frozenW1() {
        // SHA-256 exports from unmodified main, before this codec implementation; frozen evidence receipt.
        var expected = java.util.Map.ofEntries(
                java.util.Map.entry("goback.json", "fa299c2e5f3fae75afe365363b9f16925f0cfea591f631768ace82f0fb9a1075"),
                java.util.Map.entry("invoke-false-false.json", "f004e85f0252047761488b4ffa6d4d9f83f42ea8561d3c8533c5dd6f9c2c1d22"),
                java.util.Map.entry("invoke-false-true.json", "f8a9bfc24e94fb9f4d60120419bc444072780458c08366587f8d95fd745bc378"),
                java.util.Map.entry("invoke-true-false.json", "f694780ce0a928663f8c1b679a282c79a3afdb0f51ca38e87ef13b275a595c98"),
                java.util.Map.entry("invoke-true-true.json", "59b638e03ec64de4bda0e19ed6bb1730250628f417a4039a337f016666ef4af8"),
                java.util.Map.entry("scalar.json", "40b9cec1bcc1c1e40cf3b9e3c48e834835e478e1d84bccafa575d63497ef3b60"));
        var publications = new java.util.LinkedHashMap<String, Publication>();
        publications.put("goback.json", GobackOracle.publication()); publications.put("scalar.json", ScalarAssignOracle.publication());
        for (boolean c : List.of(false, true)) for (boolean k : List.of(false, true))
            publications.put("invoke-" + c + "-" + k + ".json", InvokeOracle.publication(c, k));
        try {
            for (var item : publications.entrySet()) {
                var output = new AirJson().encode(item.getValue());
                equal(expected.get(item.getKey()), java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(output)));
                bytes(output, new AirJson().encode(new AirJson().decode(output)));
            }
        } catch (java.security.NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
    static Json.Value mapping(String name, Class<?> argument, Object value) throws Exception {
        var method = BindingWriter.class.getDeclaredMethod(name, argument); method.setAccessible(true);
        try { return (Json.Value) method.invoke(new BindingWriter(), value); }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException failure) throw failure;
            throw e;
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            String mode = args[0];
            if (mode.equals("red-branch-mapping")) {
                var p = publication("branch"); valid(p);
                equal(at(W2cWireOracle.document("branch"), TERM), mapping("operation", Terminator.class, p.units().get(0).sequences().get(0).terminator()));
            } else if (mode.equals("red-unknown-mapping")) {
                var p = publication("unknown"); valid(p);
                var value = ((Operations.Assign)p.units().get(0).sequences().get(0).instructions().get(0)).value();
                equal(at(W2cWireOracle.document("unknown"), "publication.units.0.sequences.0.instructions.0.value"), mapping("expression", Expression.class, value));
            } else if (mode.equals("export")) {
                Files.createDirectories(Path.of(args[1]));
                for (String f : List.of("jump", "branch", "bool", "unknown", "unknown-empty", "premise", "full"))
                    Files.write(Path.of(args[1], f + ".json"), new AirJson().encode(publication(f)));
            } else if (mode.equals("challenge")) { focused(); }
            else { form(mode); }
            System.out.println("W2C " + mode + " PASS"); return;
        }
        focused(); scale(); nestedUnknown();
        System.out.println("W2C PASS");
    }
    private static void focused() {
        jump(); branch(); bool(); unknown(); premise(); complete(); wireOracle(); obligations(); openReadsAndOrder();
        branchNegatives(); unknownNegatives(); premiseNegatives(); closedShapes(); foreignUnit(); composition(); frozenW1();
    }
}
