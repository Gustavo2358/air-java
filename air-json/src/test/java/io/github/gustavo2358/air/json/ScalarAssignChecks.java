package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Unit;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.validation.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import static io.github.gustavo2358.air.json.AirJsonException.Code.*;
import static io.github.gustavo2358.air.json.ScalarAssignOracle.*;

/** Physical mutations and independent model negatives. No semantic DTO or runtime instrumentation. */
final class ScalarAssignChecks {
    private ScalarAssignChecks() {}
    private static final AirJson CODEC = new AirJson();
    private static final Publication EXPECTED = publication();
    private static final byte[] GOLDEN = readGolden();
    private static final Json.Value TREE = Json.parse(GOLDEN, AirJson.Limits.defaults());
    private static final String OBJECT_PATH = "publication.units.0.objects.0";
    private static final String CELL_PATH = "publication.storage.0";
    private static final String SEQ = "publication.units.0.sequences.0";
    private static final String OP = SEQ + ".instructions.0";
    private static final String DEST = OP + ".destination";
    private static final String VALUE = OP + ".value";
    private static byte[] readGolden() {
        try { return Files.readAllBytes(Path.of("src/test/resources/scalar-assign.canonical.json")); }
        catch (java.io.IOException error) { throw new AssertionError(error); }
    }
    static void independentOracle() {
        try (var in = ScalarAssignOracle.class.getResourceAsStream("ScalarAssignOracle.class")) {
            if (in == null) throw new AssertionError("Missing oracle class");
            String constants = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
            for (String dependency : List.of("AirJson", "BindingReader", "BindingWriter", "Json", "ScalarAssignChecks", "java/nio/file"))
                require(!constants.contains("io/github/gustavo2358/air/json/" + dependency)
                        && !constants.contains("java/nio/file"), "Manual model oracle depends on transport or golden input");
        } catch (java.io.IOException error) { throw new AssertionError(error); }
    }
    static void golden() {
        equal(EXPECTED, CODEC.decode(GOLDEN)); bytes(GOLDEN, CODEC.encode(EXPECTED));
        equal(EXPECTED, CODEC.decode(CODEC.encode(EXPECTED))); bytes(GOLDEN, CODEC.encode(CODEC.decode(GOLDEN)));
        bytes(CODEC.encode(EXPECTED), CODEC.encode(EXPECTED));
        equal(14554, GOLDEN.length); require(GOLDEN[GOLDEN.length - 1] == '}', "No final newline");
        try { equal("40b9cec1bcc1c1e40cf3b9e3c48e834835e478e1d84bccafa575d63497ef3b60",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(GOLDEN))); }
        catch (java.security.NoSuchAlgorithmException error) { throw new AssertionError(error); }
    }
    static void partialEvidence() {
        var p = CODEC.decode(GOLDEN); var u = p.units().get(0);
        equal(Evidence.InventoryStatus.PARTIAL, p.coverage().inventory());
        equal(Evidence.InventoryStatus.PARTIAL, u.coverage().inventory());
        equal(EXPECTED.origins(), p.origins()); equal(EXPECTED.coverage(), p.coverage());
        equal(EXPECTED.uncertainties(), p.uncertainties()); equal(5, p.uncertainties().size());
        equal(List.of(gap("entry-inventory")), u.entries().get(0).state().uncertainties());
        equal(precision(), u.objects().get(0).precision()); equal(precision(), assign(p).header().precision());
        equal(origin("target"), assign(p).destination().header().origin());
        equal(origin("literal"), assign(p).value().header().origin());
        equal(new OperationOwner(ASSIGN), assign(p).destination().header().id().owner());
        equal(Operand.Role.VALUE_WRITE, assign(p).destination().header().role());
    }
    static void textAndNullable() {
        for (String text : List.of("PROGA", "", " A ", "aA", "e\u0301", "é", "😀\uFEFF\u0000\n\"\\/")) {
            var p = withInstructions(List.of(ScalarAssignOracle.assign(ASSIGN, OBJECT, text)));
            byte[] input = changed(VALUE + ".value.value", Json.value(text));
            equal(p, CODEC.decode(input)); bytes(input, CODEC.encode(p));
            for (Optional<String> name : List.of(Optional.<String>empty(), Optional.of(text))) {
                var o = EXPECTED.units().get(0).objects().get(0);
                var renamed = new Memory.ObjectDeclaration(o.id(), name, o.typeRef(), o.storage(), o.visibility(), o.origin(), o.coverage(), o.precision());
                var renamedPublication = replace(EXPECTED, List.of(renamed), EXPECTED.storage(), instructions(EXPECTED));
                roundTrip(renamedPublication);
                equal(OBJECT, ((Places.ObjectPlace)assign(CODEC.decode(CODEC.encode(renamedPublication))).destination()).object());
            }
        }
        for (Memory.Lifetime lifetime : List.of(Memory.Lifetime.PERSISTENT, Memory.Lifetime.EXTERNAL)) {
            var c = cell(CELL); var h = c.header();
            var global = new Memory.Cell(new Memory.StorageHeader(h.id(), Optional.empty(), lifetime, h.visibility(), h.origin()), c.typeRef());
            roundTrip(replace(EXPECTED, objects(), List.of(global), instructions(EXPECTED)));
        }
    }
    static void dangling() {
        var o = objects().get(0); var missing = new StorageId(PUB, "absent-cell");
        var bad = new Memory.ObjectDeclaration(o.id(), o.displayName(), o.typeRef(), new Memory.CellBinding(missing), o.visibility(), o.origin(), o.coverage(), o.precision());
        invalid("I-02", replace(EXPECTED, List.of(bad), EXPECTED.storage(), instructions(EXPECTED)),
                changed(OBJECT_PATH + ".storage.storage.localId", Json.value("absent-cell")));
        var a = assign(EXPECTED); var d = (Places.ObjectPlace)a.destination();
        invalid("I-02", withInstructions(List.of(new Operations.Assign(a.header(), new Places.ObjectPlace(d.header(), new ObjectId(UNIT, "absent-object")), a.value()))),
                changed(DEST + ".object.localId", Json.value("absent-object")));
    }
    static void storageAndDomain() {
        var c = cell(CELL); var h = c.header();
        var badOwner = new Memory.Cell(new Memory.StorageHeader(h.id(), Optional.of(new UnitId(PUB, "absent-unit")), h.lifetime(), h.visibility(), h.origin()), TEXT);
        invalid("I-02", replace(EXPECTED, objects(), List.of(badOwner), instructions(EXPECTED)),
                changed(CELL_PATH + ".header.owner.localId", Json.value("absent-unit")));
        // Unsupported domains/kinds remain model-level negatives; they do not expand transport.
        var wrongDomain = replace(EXPECTED, objects(), List.of(new Memory.Cell(h, Types.known(Types.Builtin.INT))), instructions(EXPECTED));
        validatorInvalid("I-49", wrongDomain); failure(IMPLEMENTATION_LIMIT, () -> CODEC.encode(wrongDomain));
        var wrongKind = replace(EXPECTED, objects(), List.of(new Memory.Region(h, Optional.of(BigInteger.TEN), Optional.empty())), instructions(EXPECTED));
        validatorInvalid("I-13", wrongKind); failure(IMPLEMENTATION_LIMIT, () -> CODEC.encode(wrongKind));
        localInvalid("I-02", OBJECT_PATH + ".storage.storage", changed(OBJECT_PATH + ".storage.storage", at(OBJECT_PATH + ".id")));
        localInvalid("I-02", CELL_PATH + ".header.id", changed(CELL_PATH + ".header.id", at(OBJECT_PATH + ".id")));
        localInvalid("I-02", DEST + ".object", changed(DEST + ".object", at(CELL_PATH + ".header.id")));
        Json.Value noOwner = edit(TREE, CELL_PATH + ".header.owner", Json.Nil.INSTANCE);
        localInvalid("AIR-03 §2", CELL_PATH + ".header.owner", wire(edit(noOwner, CELL_PATH + ".header.lifetime", Json.value("ACTIVATION"))));
    }
    static void operandOwners() {
        for (OperandOwner owner : List.of(new OperationOwner(RETURN), new EntryOwner(new EntryId(UNIT, "start")))) {
            for (boolean destination : List.of(true, false)) {
                var a = assign(EXPECTED); Operand operand = destination ? a.destination() : a.value();
                var h = operand.header(); var replaced = new Operand.Header(new OperandId(owner, h.id().localId()), h.role(), h.origin());
                var bad = destination ? new Operations.Assign(a.header(), new Places.ObjectPlace(replaced, OBJECT), a.value())
                        : new Operations.Assign(a.header(), a.destination(), new Expressions.Literal(replaced, new Values.TextValue("PROGA")));
                Json.Value idOwner = owner instanceof OperationOwner ? Json.object("kind", "operation", "localId", "leave")
                        : Json.object("kind", "entry", "localId", "start");
                invalid("I-11", withInstructions(List.of(bad)), changed((destination ? DEST : VALUE) + ".header.id.owner", idOwner));
            }
        }
        localInvalid("I-02", DEST + ".header.id", changed(DEST + ".header.id", at(OP + ".header.id")));
    }
    static void roles() {
        for (boolean destination : List.of(true, false)) {
            var a = assign(EXPECTED); var h = (destination ? a.destination() : a.value()).header();
            var role = destination ? Operand.Role.VALUE_READ : Operand.Role.VALUE_WRITE;
            var replaced = new Operand.Header(h.id(), role, h.origin());
            var bad = destination ? new Operations.Assign(a.header(), new Places.ObjectPlace(replaced, OBJECT), a.value())
                    : new Operations.Assign(a.header(), a.destination(), new Expressions.Literal(replaced, new Values.TextValue("PROGA")));
            invalid("I-11", withInstructions(List.of(bad)), changed((destination ? DEST : VALUE) + ".header.role", Json.value(destination ? "VALUE_READ" : "VALUE_WRITE")));
        }
    }
    static void literalConflict() {
        var a = assign(EXPECTED);
        var bad = withInstructions(List.of(new Operations.Assign(a.header(), a.destination(),
                new Expressions.Literal(a.value().header(), new Values.IntValue(BigInteger.ONE)))));
        validatorInvalid("I-08/I-52", bad); failure(IMPLEMENTATION_LIMIT, () -> CODEC.encode(bad));
    }
    static void slots() {
        localInvalid("I-04", SEQ + ".terminator", changed(SEQ + ".terminator", at(OP)));
        localInvalid("I-04", OP, changed(SEQ + ".instructions", new Json.Arr(List.of(at(SEQ + ".terminator")))));
    }
    static void fields() {
        // Recursively challenge every field of the supported tree, including all new definitions.
        requiredFields(TREE, ""); unknownFields(TREE, "");
    }
    static void wrongPhysicalValues() {
        for (String path : List.of(OBJECT_PATH + ".id", OBJECT_PATH + ".typeRef", OBJECT_PATH + ".storage", OBJECT_PATH + ".visibility",
                OBJECT_PATH + ".origin", OBJECT_PATH + ".coverage", OBJECT_PATH + ".precision", CELL_PATH + ".header", CELL_PATH + ".typeRef",
                CELL_PATH + ".header.id", CELL_PATH + ".header.lifetime", CELL_PATH + ".header.visibility", CELL_PATH + ".header.origin",
                DEST + ".header", DEST + ".object", DEST + ".header.id", DEST + ".header.role", DEST + ".header.origin", VALUE + ".value", VALUE + ".value.value"))
            failsAt(INPUT_ERROR, path, changed(path, Json.Nil.INSTANCE));
        for (String path : List.of(OBJECT_PATH + ".visibility", CELL_PATH + ".header.visibility", CELL_PATH + ".header.lifetime", DEST + ".header.role", VALUE + ".header.role"))
            for (String bad : List.of("", "private", "value_write", "FUTURE", "PERSISTENT ")) failsAt(INPUT_ERROR, path, changed(path, Json.value(bad)));
        for (String path : List.of(OBJECT_PATH + ".typeRef", OBJECT_PATH + ".typeRef.type", OBJECT_PATH + ".storage", CELL_PATH, DEST, VALUE, VALUE + ".value"))
            failsAt(INPUT_ERROR, path, changed(path + ".kind", Json.value("future-kind")));
        // Newly supported inventories no longer accept arbitrary elements under an unsupported blanket.
        failsAt(INPUT_ERROR, "publication.storage.0", changed("publication.storage", new Json.Arr(List.of(Json.object("kind", "deferred-element")))));
        failsAt(INPUT_ERROR, OBJECT_PATH + ".kind", changed(OBJECT_PATH, Json.object("kind", "deferred-element")));
    }
    static void unsupported() {
        var nop = withInstructions(List.of(new Operations.Nop(assign(EXPECTED).header())));
        equal(List.of(), AirValidator.validate(nop).issues());
        failure(IMPLEMENTATION_LIMIT, () -> CODEC.encode(nop));
        failsAt(IMPLEMENTATION_LIMIT, OP, changed(OP, Json.object("kind", "nop", "header", at(OP + ".header"))));
        for (String kind : List.of("bool", "int", "decimal", "bytes", "opaque_type", "label"))
            failsAt(IMPLEMENTATION_LIMIT, OBJECT_PATH + ".typeRef.type", changed(OBJECT_PATH + ".typeRef.type.kind", Json.value(kind)));
        failsAt(IMPLEMENTATION_LIMIT, OBJECT_PATH + ".typeRef", changed(OBJECT_PATH + ".typeRef", Json.object("kind", "unknown_type", "uncertainty", at("publication.uncertainties.0.id"))));
        for (String kind : List.of("view", "alias", "alternatives", "unknown"))
            failsAt(IMPLEMENTATION_LIMIT, OBJECT_PATH + ".storage", changed(OBJECT_PATH + ".storage.kind", Json.value(kind)));
        failsAt(IMPLEMENTATION_LIMIT, CELL_PATH, changed(CELL_PATH + ".kind", Json.value("region")));
        for (String kind : List.of("choice", "region_slice")) failsAt(IMPLEMENTATION_LIMIT, DEST, changed(DEST + ".kind", Json.value(kind)));
        for (String kind : List.of("read", "unknown", "unary", "binary", "quantize", "fit_text", "slice_text", "trim_right"))
            failsAt(IMPLEMENTATION_LIMIT, VALUE, changed(VALUE + ".kind", Json.value(kind)));
        for (String kind : List.of("bool", "int", "decimal", "bytes", "label"))
            failsAt(IMPLEMENTATION_LIMIT, VALUE + ".value", changed(VALUE + ".value.kind", Json.value(kind)));
    }
    static void enumTables() {
        var lifetimes = List.of(Memory.Lifetime.ACTIVATION, Memory.Lifetime.PERSISTENT, Memory.Lifetime.EXTERNAL);
        var lifetimeTokens = List.of("ACTIVATION", "PERSISTENT", "EXTERNAL");
        var visibilities = List.of(Memory.Visibility.PRIVATE, Memory.Visibility.SHARED, Memory.Visibility.UNKNOWN);
        var visibilityTokens = List.of("PRIVATE", "SHARED", "UNKNOWN");
        for (int i = 0; i < lifetimes.size(); i++) {
            var h = cell(CELL).header();
            var c = new Memory.Cell(new Memory.StorageHeader(CELL, h.owner(), lifetimes.get(i), h.visibility(), h.origin()), TEXT);
            var p = replace(EXPECTED, objects(), List.of(c), instructions(EXPECTED));
            equal(p, CODEC.decode(changed(CELL_PATH + ".header.lifetime", Json.value(lifetimeTokens.get(i)))));
            bytes(changed(CELL_PATH + ".header.lifetime", Json.value(lifetimeTokens.get(i))), CODEC.encode(p));
        }
        for (int i = 0; i < visibilities.size(); i++) {
            var h = cell(CELL).header(); var o = objects().get(0);
            var c = new Memory.Cell(new Memory.StorageHeader(CELL, h.owner(), h.lifetime(), visibilities.get(i), h.origin()), TEXT);
            var object = new Memory.ObjectDeclaration(o.id(), o.displayName(), TEXT, o.storage(), visibilities.get(i), o.origin(), o.coverage(), o.precision());
            var p = replace(EXPECTED, List.of(object), List.of(c), instructions(EXPECTED));
            byte[] input = wire(edit(edit(TREE, CELL_PATH + ".header.visibility", Json.value(visibilityTokens.get(i))), OBJECT_PATH + ".visibility", Json.value(visibilityTokens.get(i))));
            equal(p, CODEC.decode(input)); bytes(input, CODEC.encode(p));
        }
        var roles = List.of(Operand.Role.VALUE_READ, Operand.Role.VALUE_WRITE, Operand.Role.ADDRESS_READ, Operand.Role.PREDICATE,
                Operand.Role.CALL_TARGET, Operand.Role.ARGUMENT_VALUE, Operand.Role.ARGUMENT_REFERENCE, Operand.Role.RESULT_TARGET,
                Operand.Role.RESOURCE_TARGET, Operand.Role.CONTROL_TARGET);
        var tokens = List.of("VALUE_READ", "VALUE_WRITE", "ADDRESS_READ", "PREDICATE", "CALL_TARGET", "ARGUMENT_VALUE",
                "ARGUMENT_REFERENCE", "RESULT_TARGET", "RESOURCE_TARGET", "CONTROL_TARGET");
        for (int i = 0; i < roles.size(); i++) {
            var a = assign(EXPECTED); var h = a.destination().header();
            var p = withInstructions(List.of(new Operations.Assign(a.header(), new Places.ObjectPlace(new Operand.Header(h.id(), roles.get(i), h.origin()), OBJECT), a.value())));
            byte[] input = changed(DEST + ".header.role", Json.value(tokens.get(i)));
            // Internal mapping check for tokens that are physically valid but not valid Assign roles.
            equal(p, new BindingReader().envelope(Json.parse(input, AirJson.Limits.defaults())));
            bytes(input, wire(new BindingWriter().envelope(p)));
            if (roles.get(i) != Operand.Role.VALUE_WRITE) invalid("I-11", p, input);
        }
    }
    static void order() {
        var first = ScalarAssignOracle.assign(new OperationId(UNIT, "z-first"), OBJECT, "FIRST");
        var second = ScalarAssignOracle.assign(ASSIGN, OBJECT, "SECOND");
        var p = withInstructions(List.of(first, second));
        var decoded = CODEC.decode(CODEC.encode(p)); equal(List.of(first, second), instructions(decoded));
        var reversed = withInstructions(List.of(second, first)); roundTrip(reversed);
        require(!Arrays.equals(CODEC.encode(p), CODEC.encode(reversed)), "Instruction order is semantic");
        // Independent physical array oracle; does not derive the expected order from the writer.
        Json.Value firstWire = edit(at(OP), "header.id.localId", Json.value("z-first"));
        firstWire = edit(firstWire, "destination.header.id.owner.localId", Json.value("z-first"));
        firstWire = edit(firstWire, "value.header.id.owner.localId", Json.value("z-first"));
        firstWire = edit(firstWire, "value.value.value", Json.value("FIRST"));
        Json.Value secondWire = edit(at(OP), "value.value.value", Json.value("SECOND"));
        byte[] input = changed(SEQ + ".instructions", new Json.Arr(List.of(firstWire, secondWire)));
        equal(p, CODEC.decode(input)); bytes(input, CODEC.encode(p));
    }
    static void limits() {
        equal(new AirJson.Limits(16 * 1024 * 1024, 128), AirJson.Limits.defaults());
        var limited = new AirJson(new AirJson.Limits(GOLDEN.length - 1, 128), ValidationOptions.defaults());
        failure(IMPLEMENTATION_LIMIT, () -> limited.encode(EXPECTED)); failure(IMPLEMENTATION_LIMIT, () -> limited.decode(GOLDEN));
        var boundedValidator = new AirJson(AirJson.Limits.defaults(), new ValidationOptions(128, 10, 100));
        failure(INCOMPLETE_VALIDATION, () -> boundedValidator.encode(EXPECTED));
        failure(INCOMPLETE_VALIDATION, () -> boundedValidator.decode(GOLDEN));
    }
    static void scale() {
        var small = probe(1000, 1000); var large = probe(2000, 2000);
        require(large.bytes() > small.bytes() * 1.9 && large.bytes() < small.bytes() * 2.1, "Nonlinear wire size");
        require(large.nodes() > small.nodes() * 1.9 && large.nodes() < small.nodes() * 2.1, "Nonlinear physical traversal");
        equal(2 * small.queries(), large.queries());
        // 10,000 occurrences remain references to ONE definition, with explicit test-only byte limit.
        probe(1, 10000);
    }
    private record Probe(int bytes, long nodes, long queries) {}
    private static Probe probe(int objectCount, int assignCount) {
        var codec = new AirJson(new AirJson.Limits(64 * 1024 * 1024, 128), ValidationOptions.defaults());
        var p = publication(objectCount, assignCount); long start = System.nanoTime();
        byte[] output = codec.encode(p); var decoded = codec.decode(output); equal(p, decoded);
        bytes(output, codec.encode(decoded));
        if (output.length > AirJson.Limits.defaults().maximumDocumentBytes())
            failure(IMPLEMENTATION_LIMIT, () -> CODEC.decode(output));
        var result = AirValidator.validate(decoded); equal(List.of(), result.issues());
        equal(assignCount + 1, result.statistics().operations()); equal(assignCount * 2, result.statistics().operands());
        equal(objectCount, decoded.units().get(0).objects().size()); equal(objectCount, decoded.storage().size());
        long[] counts = new long[5]; count(Json.parse(output, new AirJson.Limits(64 * 1024 * 1024, 128)), counts);
        equal((long)objectCount, counts[1]); equal((long)objectCount, counts[2]);
        equal((long)assignCount + 1, counts[3]); equal((long)assignCount * 2, counts[4]);
        System.out.println("SCALE objects=" + objectCount + " storage=" + objectCount + " operations=" + result.statistics().operations()
                + " operands=" + result.statistics().operands() + " inputBytes=" + output.length + " outputBytes=" + output.length
                + " nodes=" + counts[0] + " entities=" + result.statistics().entities() + " domainQueries=" + result.statistics().domainQueries()
                + " observedMs=" + (System.nanoTime() - start) / 1_000_000);
        return new Probe(output.length, counts[0], result.statistics().domainQueries());
    }
    private static void count(Json.Value node, long[] counts) {
        counts[0]++;
        if (node instanceof Json.Obj o) {
            if (o.fields().containsKey("displayName")) counts[1]++;
            if (o.fields().containsKey("header")) {
                String domain = ((Json.Text)at(node, "header.id.domain")).value();
                switch (domain) { case "storage" -> counts[2]++; case "operation" -> counts[3]++; case "operand" -> counts[4]++; default -> throw new AssertionError(domain); }
            }
            o.fields().values().forEach(v -> count(v, counts));
        } else if (node instanceof Json.Arr a) a.values().forEach(v -> count(v, counts));
    }
    private static List<Memory.ObjectDeclaration> objects() { return EXPECTED.units().get(0).objects(); }
    private static List<Instruction> instructions(Publication p) { return p.units().get(0).sequences().get(0).instructions(); }
    private static Operations.Assign assign(Publication p) { return (Operations.Assign)instructions(p).get(0); }
    private static Publication withInstructions(List<Instruction> instructions) { return replace(EXPECTED, objects(), EXPECTED.storage(), instructions); }
    private static Publication replace(Publication p, List<Memory.ObjectDeclaration> objects, List<Memory.Storage> storage, List<Instruction> instructions) {
        var u = p.units().get(0); var s = u.sequences().get(0);
        var unit = new Unit(u.id(), u.containingUnit(), objects, u.visibleObjects(), u.entries(),
                List.of(new Sequence(s.label(), instructions, s.terminator(), s.origin())), u.completionPorts(), u.body(), u.bodyUnavailable(), u.coverage(), u.origin());
        return new Publication(p.id(), p.airVersion(), p.capabilities(), p.artifacts(), List.of(unit), storage, p.resources(),
                p.artifactRelations(), p.origins(), p.coverage(), p.uncertainties(), p.premises());
    }
    private static ValidationResult validatorInvalid(String rule, Publication p) {
        var r = AirValidator.validate(p); equal(ValidationResult.Status.INVALID_IR, r.status());
        require(r.issues().stream().anyMatch(i -> i.rule().equals(rule)), "Missing rule " + rule + ": " + r.issues()); return r;
    }
    private static void invalid(String rule, Publication p, byte[] input) {
        var expected = validatorInvalid(rule, p);
        var encoded = failure(INVALID_IR, () -> CODEC.encode(p)); var decoded = failure(INVALID_IR, () -> CODEC.decode(input));
        equal(expected.issues(), encoded.issues()); equal(expected.issues(), decoded.issues()); equal("$", decoded.path()); equal("$", encoded.path());
    }
    private static void localInvalid(String rule, String path, byte[] input) {
        var error = failsAt(INVALID_IR, path, input);
        require(error.issues().stream().anyMatch(i -> i.rule().equals(rule) && !i.detail().isBlank()), "Missing local AIR rule/detail");
    }
    private static AirJsonException failsAt(AirJsonException.Code code, String path, byte[] input) {
        var e = failure(code, () -> CODEC.decode(input)); equal(jsonPath(path), e.path()); return e;
    }
    private static AirJsonException failure(AirJsonException.Code code, Runnable action) {
        try { action.run(); } catch (AirJsonException e) { equal(code, e.code()); return e; }
        throw new AssertionError("Expected " + code);
    }
    private static void roundTrip(Publication p) { equal(p, CODEC.decode(CODEC.encode(p))); }
    private static String jsonPath(String path) { return "$" + (path.isEmpty() ? "" : "." + path.replaceAll("\\.([0-9]+)", "[$1]")); }
    private static void equal(Object expected, Object actual) { if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + ", actual " + actual); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void bytes(byte[] expected, byte[] actual) { require(Arrays.equals(expected, actual), "Canonical bytes differ"); }
    private static byte[] wire(Json.Value tree) { return Json.write(tree, AirJson.Limits.defaults()); }
    private static byte[] changed(String path, Json.Value replacement) { return wire(edit(TREE, path, replacement)); }
    private static Json.Value at(String path) { return at(TREE, path); }
    private static Json.Value at(Json.Value node, String path) {
        for (String part : path.split("\\.")) node = node instanceof Json.Obj o ? o.fields().get(part) : ((Json.Arr)node).values().get(Integer.parseInt(part));
        return node;
    }
    private static Json.Value edit(Json.Value tree, String path, Json.Value replacement) { return edit(tree, path.split("\\."), 0, replacement); }
    private static Json.Value edit(Json.Value node, String[] path, int i, Json.Value replacement) {
        if (i == path.length) return replacement;
        if (node instanceof Json.Obj o) {
            var fields = new LinkedHashMap<>(o.fields());
            if (i == path.length - 1 && replacement == null) fields.remove(path[i]);
            else fields.put(path[i], edit(fields.get(path[i]), path, i + 1, replacement));
            return new Json.Obj(fields);
        }
        var values = new ArrayList<>(((Json.Arr)node).values()); int index = Integer.parseInt(path[i]);
        values.set(index, edit(values.get(index), path, i + 1, replacement)); return new Json.Arr(values);
    }
    private static String join(String path, String key) { return path.isEmpty() ? key : path + "." + key; }
    private static void requiredFields(Json.Value node, String path) {
        if (node instanceof Json.Obj o) for (var e : o.fields().entrySet()) {
            failsAt(INPUT_ERROR, join(path, e.getKey()), changed(join(path, e.getKey()), null)); requiredFields(e.getValue(), join(path, e.getKey()));
        }
        else if (node instanceof Json.Arr a) for (int i = 0; i < a.values().size(); i++) requiredFields(a.values().get(i), join(path, Integer.toString(i)));
    }
    private static void unknownFields(Json.Value node, String path) {
        if (node instanceof Json.Obj o) {
            var f = new LinkedHashMap<>(o.fields()); f.put("__unexpected", Json.Nil.INSTANCE);
            byte[] input = path.isEmpty() ? wire(new Json.Obj(f)) : changed(path, new Json.Obj(f));
            failsAt(INPUT_ERROR, join(path, "__unexpected"), input);
            for (var e : o.fields().entrySet()) unknownFields(e.getValue(), join(path, e.getKey()));
        } else if (node instanceof Json.Arr a) for (int i = 0; i < a.values().size(); i++) unknownFields(a.values().get(i), join(path, Integer.toString(i)));
    }
}
