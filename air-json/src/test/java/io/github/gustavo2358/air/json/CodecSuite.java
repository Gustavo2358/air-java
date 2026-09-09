package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Unit;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.validation.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import static io.github.gustavo2358.air.json.AirJsonException.Code.*;

/** Independent AIR/golden oracles plus directed physical and preservation counterexamples. */
public final class CodecSuite {
    private CodecSuite() {}
    private static final AirJson CODEC = new AirJson();
    private static final Publication EXPECTED = GobackOracle.publication();
    private static byte[] golden;
    private static String text;
    private static Json.Value tree;
    private static int checks;
    public static void main(String[] args) throws Exception {
        boolean assertions = false; assert assertions = true;
        if (!assertions) throw new AssertionError("Transport suite requires -ea");
        golden = Files.readAllBytes(Path.of("src/test/resources/goback.canonical.json"));
        text = new String(golden, StandardCharsets.UTF_8);
        tree = Json.parse(golden, AirJson.Limits.defaults());
        check("CORE-SIZE physical JSON stack and malformed distinction", JsonCapacityChecks::physicalDepth);
        check("CORE-SIZE exact UTF-8 byte budgets", JsonCapacityChecks::byteBudgets);
        check("CORE-SIZE JSON independent cardinality series", JsonCapacityChecks::series);
        check("CORE-SIZE codec operational result remains explicit", JsonCapacityChecks::validationBudget);
        check("manual AIR oracle valid and complete", () -> {
            equal(ValidationResult.Status.STRUCTURALLY_VALID, AirValidator.validate(EXPECTED).status());
            equal(2, EXPECTED.artifacts().size()); equal(8, EXPECTED.origins().size()); equal(5, EXPECTED.uncertainties().size());
            equal(2, EXPECTED.coverage().items().size());
        });
        check("zero known publication remains explicitly complete", () -> {
            var id = new PublicationId("empty-publication");
            var p = new Publication(id, SemanticVersion.AIR_2_0_0, new Capabilities.Manifest(List.of(),List.of()),
                    List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),
                    new Evidence.Coverage(Evidence.InventoryStatus.COMPLETE,new Scopes.PublicationScope(id),List.of(),List.of()),List.of(),List.of());
            roundTrip(p);
        });
        check("encode equals independent canonical golden", () -> bytes(golden, CODEC.encode(EXPECTED)));
        check("decode equals independent complete Publication", () -> equal(EXPECTED, CODEC.decode(golden)));
        check("AIR round trip preserves every record and array", () -> roundTrip(EXPECTED));
        check("JSON round trip canonical and deterministic", () -> {
            bytes(golden, CODEC.encode(CODEC.decode(golden)));
            bytes(CODEC.encode(EXPECTED), CODEC.encode(EXPECTED));
            require(golden[golden.length - 1] == '}', "golden final newline");
        });
        check("property order whitespace and equivalent escapes recanonicalize", () -> {
            String input = " \r\n{\n\"publication\":" + text.substring(text.indexOf("\"publication\":{") + 14, text.length() - 1)
                    + ", \"bindingVersion\":\"1.0.0\",\"binding\":\"analysis-ir-json\",\"airVersion\":\"2.0.0\"}\t\n";
            input = input.replace("semantic-product", "\\u0073emantic-product").replace("minimal-entry-goback@1/", "minimal-entry-goback@1\\/");
            bytes(golden, CODEC.encode(CODEC.decode(utf8(input))));
        });
        check("PARTIAL and dimensional claims remain independent", () -> {
            var p = CODEC.decode(golden); var h = p.units().get(0).sequences().get(0).terminator().header();
            equal(Evidence.InventoryStatus.PARTIAL, p.coverage().inventory());
            equal(Evidence.InventoryStatus.PARTIAL, p.units().get(0).coverage().inventory());
            equal(EXPECTED.coverage(), p.coverage()); equal(EXPECTED.uncertainties(), p.uncertainties());
            equal(Evidence.PrecisionStatus.EXACT, h.precision().control().status());
            for (var d : List.of(Evidence.Dimension.STORAGE, Evidence.Dimension.EFFECTS, Evidence.Dimension.VALUES, Evidence.Dimension.DEPENDENCIES)) {
                equal(Evidence.PrecisionStatus.UNAVAILABLE, h.precision().claim(d).status());
                equal(List.of(GobackOracle.gap("return/" + d)), h.precision().claim(d).reasons());
                equal(new Scopes.EntityScope(List.of(GobackOracle.RETURN)), h.precision().claim(d).scope());
            }
            require(p.units().get(0).sequences().get(0).terminator() instanceof Operations.Return, "Return changed");
        });
        check("empty containers and closed signatures remain present", () -> {
            var p = CODEC.decode(golden); var u = p.units().get(0); var e = u.entries().get(0);
            equal(List.of(), p.storage()); equal(List.of(), p.resources()); equal(List.of(), p.artifactRelations()); equal(List.of(), p.premises());
            equal(List.of(), u.objects()); equal(List.of(), u.visibleObjects()); equal(List.of(), u.completionPorts());
            equal(List.of(), e.state().conditions()); equal(List.of(), e.state().uncertainties());
            equal(Interactions.NoRemainder.INSTANCE, e.signature().parameters().remainder());
            equal(Interactions.NoRemainder.INSTANCE, e.signature().results().remainder());
        });
        check("provenance null approximate and include chains round trip", CodecSuite::provenance);
        check("arbitrary natural coordinates beyond primitive ranges", () -> {
            BigInteger huge = new BigInteger("1234567890123456789012345678901234567890");
            var p = withFirstWritten(Optional.of(new Origins.LineColumns(new Origins.Span(new Origins.Position(huge, huge),
                    new Origins.Position(huge, huge.add(BigInteger.ONE)), BigInteger.ZERO, BigInteger.ZERO,
                    Origins.ColumnUnit.UTF16_CODE_UNIT, true))), List.of(), false);
            roundTrip(p); require(new String(CODEC.encode(p), StandardCharsets.UTF_8).contains("\"" + huge + "\""), "integer not string");
        });
        check("Unicode scalar text and canonical escaping", CodecSuite::unicode);
        check("canonical property ordering uses Unicode scalars", () -> {
            bytes(utf8("{\"\ue000\":\"bmp\",\"😀\":\"supplementary\"}"), Json.write(
                    Json.object("😀", "supplementary", "\ue000", "bmp"), AirJson.Limits.defaults()));
            equal(-1, Integer.signum(Json.compareScalars("a", "aa")));
        });
        check("producer codes reason text and source names are opaque", () -> {
            var gaps = EXPECTED.uncertainties().stream().map(u -> new Evidence.Uncertainty(u.id(), "other-producer:RENAMED_" + u.dimensions().get(0),
                    u.dimensions(), u.scope(), "Outro texto sem semântica / Ω", u.origin())).toList();
            var artifacts = EXPECTED.artifacts().stream().map(a -> new Origins.Artifact(a.id(), "outro/" + a.logicalName(), a.contentDigest())).toList();
            var p = copy(EXPECTED, artifacts, EXPECTED.units(), EXPECTED.origins(), EXPECTED.coverage(), gaps);
            roundTrip(p); equal(EXPECTED.units(), CODEC.decode(CODEC.encode(p)).units());
        });
        check("array permutations preserve physical inventory order", CodecSuite::arrayOrder);
        check("BOM rejected", () -> fails(INPUT_ERROR, utf8("\ufeff" + text)));
        check("invalid UTF-8 rejected without replacement", () -> {
            for (byte[] bad : List.of(new byte[]{(byte)0xc0,(byte)0xaf}, new byte[]{(byte)0xed,(byte)0xa0,(byte)0x80},
                    new byte[]{(byte)0xf4,(byte)0x90,(byte)0x80,(byte)0x80}, new byte[]{(byte)0x80}, new byte[]{(byte)0xe2,(byte)0x82}))
                fails(INPUT_ERROR, concat(utf8("{\""), bad, utf8("\":null}")));
        });
        check("trailing garbage second document and non-JSON whitespace rejected", () -> {
            for (String suffix : List.of("x", "{}", "[]", "null", "\u00a0", "\ufeff")) fails(INPUT_ERROR, utf8(text + suffix));
        });
        check("array string null boolean and empty document roots rejected", () -> {
            for (String root : List.of("[]", "\"text\"", "null", "true", "", " \r\n")) fails(INPUT_ERROR, utf8(root));
        });
        check("duplicate property rejected at every object depth", () -> duplicateEveryObject(tree, false));
        check("escaped duplicate key rejected at every object depth", () -> duplicateEveryObject(tree, true));
        check("isolated surrogates raw and escaped rejected", () -> {
            for (String bad : List.of("\\ud800", "\\udfff", "\\ud800x", "\\ud800\\ud800", "\\udc00\\ud800"))
                fails(INPUT_ERROR, utf8(text.replace("<preprocessed>", bad)));
            var artifacts = new ArrayList<>(EXPECTED.artifacts());
            var a = artifacts.get(0);
            artifacts.set(0, new Origins.Artifact(a.id(), a.logicalName(), Optional.of("bad" + (char)0xd800)));
            var invalid = copy(EXPECTED, artifacts, EXPECTED.units(), EXPECTED.origins(), EXPECTED.coverage(), EXPECTED.uncertainties());
            failure(INPUT_ERROR, () -> CODEC.encode(invalid));
        });
        check("invalid string escapes controls commas and truncation rejected", () -> {
            for (String bad : List.of("\\x", "\\uZZZZ", "\\u１２３４", "\n", "\t")) fails(INPUT_ERROR, utf8(text.replace("<preprocessed>", bad)));
            for (String bad : List.of("{\"a\":true,}", "{\"a\":[true,]}", "{\"a\":tru}", "{\"a\":\"abc\\", "{\"a\" true}")) fails(INPUT_ERROR, utf8(bad));
            for (int cut : List.of(1, 10, 100, text.length() - 1)) fails(INPUT_ERROR, utf8(text.substring(0, cut)));
        });
        check("every catalogued field required including nullable fields", () -> requiredFields(tree, ""));
        check("unknown fields rejected throughout supported tree", () -> unknownFields(tree, ""));
        check("kind absent unknown or null never inferred", () -> {
            String path = "publication.units.0.body";
            fails(INPUT_ERROR, changed(path, Json.object()));
            fails(INPUT_ERROR, changed(path, Json.object("kind", "Available")));
            fails(INPUT_ERROR, changed(path, Json.object("kind", null)));
            fails(INPUT_ERROR, changed("publication.units.0.sequences.0.terminator.kind", Json.value("future-op")));
        });
        check("null rejected wherever field is not nullable", () -> {
            for (String path : List.of("publication.units", "publication.capabilities", "publication.id", "publication.coverage", "publication.origins.0.exact",
                    "publication.units.0.entries.0.signature.parameters.remainder", "publication.units.0.sequences.0.terminator.values"))
                fails(INPUT_ERROR, changed(path, Json.Nil.INSTANCE));
        });
        check("integer JSON numbers forbidden", () -> {
            for (String number : List.of("4", "0", "1", "-0", "1.0", "1e3"))
                fails(INPUT_ERROR, utf8(text.replaceFirst("\"line\":\"4\"", "\"line\":" + number)));
        });
        check("canonical Natural lexemes enforced", () -> {
            for (String bad : List.of("01", "-0", "+1", "-1", "1.0", "1e3", "", " 1", "１"))
                fails(INPUT_ERROR, changed("publication.origins.0.location.span.start.line", Json.value(bad)));
        });
        check("all envelope versions require exact identity", () -> {
            for (String path : List.of("binding", "bindingVersion", "airVersion"))
                for (String bad : List.of("2.0.1", "1.0.1", "2", "2.0.0-rc", "")) fails(VERSION_MISMATCH, changed(path, Json.value(bad)));
            var p = new Publication(EXPECTED.id(), new SemanticVersion(BigInteger.TWO,BigInteger.ZERO,BigInteger.ONE), EXPECTED.capabilities(), EXPECTED.artifacts(),
                    EXPECTED.units(), List.of(), List.of(), List.of(), EXPECTED.origins(), EXPECTED.coverage(), EXPECTED.uncertainties(), List.of());
            failure(VERSION_MISMATCH, () -> CODEC.encode(p));
        });
        check("incomplete ID and foreign or wrong domain IDs rejected", () -> {
            fails(INPUT_ERROR, changed("publication.units.0.id", Json.object("domain", "unit", "localId", "unit")));
            fails(INPUT_ERROR, changed("publication.units.0.id", Json.value("unit")));
            fails(INVALID_IR, changed("publication.units.0.id.domain", Json.value("artifact")));
            fails(INVALID_IR, changed("publication.units.0.id.publication", Json.value("foreign")));
            fails(INVALID_IR, changed("publication.units.0.entries.0.initialLabel.unit", Json.value("wrong-owner")));
            fails(INVALID_IR, changed("publication.units.0.sequences.0.terminator.header.id.unit", Json.value("wrong-owner")));
        });
        check("duplicate definitions dangling references and missing evidence rejected", () -> {
            fails(INVALID_IR, changed("publication.artifacts.1.id", at("publication.artifacts.0.id")));
            fails(INVALID_IR, changed("publication.units.0.origin.localId", Json.value("missing")));
            fails(INVALID_IR, changed("publication.uncertainties", new Json.Arr(List.of())));
            fails(INVALID_IR, changed("publication.origins", new Json.Arr(List.of())));
            fails(INVALID_IR, changed("publication.coverage.items.0.outputs.0.localId", Json.value("missing")));
        });
        check("PARTIAL without reason is INVALID_IR with AIR rule", () -> {
            var error = fails(INVALID_IR, changed("publication.coverage.uncertainties", new Json.Arr(List.of())));
            require(error.issues().stream().anyMatch(i -> i.rule().equals("I-28")), "missing I-28 diagnostic");
        });
        check("local AIR constraints and Java representability limits stay distinct", () -> {
            fails(INVALID_IR, changed("publication.origins.2.inputs", new Json.Arr(List.of())));
            fails(INVALID_IR, changed("publication.units.0.entries", new Json.Arr(List.of())));
            fails(IMPLEMENTATION_LIMIT, changed("publication.origins.0.location.span.lineBase", Json.value("2")));
            fails(INVALID_IR, changed("publication.units.0.entries.0.initialLabel", Json.Nil.INSTANCE));
        });
        check("closed tokens are checked explicitly", () -> {
            for (String path : List.of("publication.coverage.inventory", "publication.coverage.items.0.status", "publication.uncertainties.0.dimensions.0",
                    "publication.units.0.sequences.0.terminator.header.precision.control.status", "publication.origins.0.location.span.columnUnit"))
                fails(INPUT_ERROR, changed(path, Json.value("UNKNOWN_TOKEN")));
        });
        check("process timestamp and private model fields forbidden", () -> {
            for (String name : List.of("timestamp", "process", "metadata", "contracts", "airVersion")) {
                var fields = new LinkedHashMap<>(((Json.Obj) at("publication")).fields()); fields.put(name, Json.value("unexpected"));
                fails(INPUT_ERROR, changed("publication", new Json.Obj(fields)));
            }
        });
        check("valid Halt unsupported and Return never silently changed", () -> {
            var h = Json.object("kind", "halt", "header", at("publication.units.0.sequences.0.terminator.header"), "haltKind", "NORMAL");
            fails(IMPLEMENTATION_LIMIT, changed("publication.units.0.sequences.0.terminator", h));
            var u = EXPECTED.units().get(0); var s = u.sequences().get(0);
            var halt = new Sequence(s.label(), List.of(), new Operations.Halt(s.terminator().header(), Operations.HaltKind.NORMAL), s.origin());
            failure(IMPLEMENTATION_LIMIT, () -> CODEC.encode(withSequences(List.of(halt))));
        });
        check("ordinary operation as terminator and Return as instruction invalid", () -> {
            fails(INVALID_IR, changed("publication.units.0.sequences.0.instructions", new Json.Arr(List.of(at("publication.units.0.sequences.0.terminator")))));
            fails(INVALID_IR, changed("publication.units.0.sequences.0.terminator", Json.object("kind", "nop", "header", at("publication.units.0.sequences.0.terminator.header"))));
        });
        check("deferred forms fail explicitly without partial materialization", CodecSuite::unsupported);
        check("capability nonempty manifest distinguished from invalid AIR", () -> fails(UNSUPPORTED_CAPABILITY,
                changed("publication.capabilities.required", new Json.Arr(List.of(Json.object("name", "control.local", "version", "1"))))));
        check("document and depth limits explicit in both directions", () -> {
            var small = new AirJson(new AirJson.Limits(20,128), ValidationOptions.defaults());
            failure(RESOURCE_LIMIT, () -> small.decode(golden)); failure(RESOURCE_LIMIT, () -> small.encode(EXPECTED));
            var shallow = new AirJson(new AirJson.Limits(1_000_000,2), ValidationOptions.defaults());
            failure(RESOURCE_LIMIT, () -> shallow.decode(golden)); failure(RESOURCE_LIMIT, () -> shallow.encode(EXPECTED));
            fails(INPUT_ERROR, utf8("{\"deep\":" + "[".repeat(300) + "null" + "]".repeat(300) + "}"));
        });
        check("Validator limits remain INCOMPLETE_VALIDATION", () -> {
            var small = new AirJson(AirJson.Limits.defaults(), new ValidationOptions(128,1,10));
            var error = failure(RESOURCE_LIMIT, () -> small.decode(golden));
            require(error.issues().stream().anyMatch(i -> i.kind() == ValidationIssue.Kind.RESOURCE_LIMIT), "missing validation limit");
            failure(RESOURCE_LIMIT, () -> small.encode(EXPECTED));
        });
        check("all complete ID wire shapes including future operand references", CodecSuite::ids);
        check("all supported wire tokens have independent binding oracles", CodecSuite::tokens);
        check("writer never derives wire tokens from runtime enum", CodecSuite::noRuntimeTokens);
        check("local AIR failures carry pinned rule and site", () -> {
            localRule("I-36", "$.publication.origins[2].inputs", changed("publication.origins.2.inputs", new Json.Arr(List.of())));
            localRule("AIR-01 §2", "$.publication.units[0].entries", changed("publication.units.0.entries", new Json.Arr(List.of())));
            localRule("AIR-01 §3", "$.publication.units[0].sequences", changed("publication.units.0.sequences", new Json.Arr(List.of())));
            localRule("AIR-06 §4", "$.publication.uncertainties[0].dimensions", changed("publication.uncertainties.0.dimensions", new Json.Arr(List.of())));
            localRule("I-02", "$.publication.units[0].id", changed("publication.units.0.id.domain", Json.value("artifact")));
            localRule("I-04", "$.publication.units[0].sequences[0].instructions[0]",
                    changed("publication.units.0.sequences.0.instructions", new Json.Arr(List.of(at("publication.units.0.sequences.0.terminator")))));
            localRule("I-04", "$.publication.units[0].sequences[0].terminator",
                    changed("publication.units.0.sequences.0.terminator", Json.object("kind", "nop", "header", at("publication.units.0.sequences.0.terminator.header"))));
        });
        check("Validator issues preserve exact rule subject detail and site", () -> {
            for (String path : List.of("publication.coverage.uncertainties", "publication.units.0.entries.0.initialLabel.unit")) {
                var bytes = changed(path, path.endsWith(".unit") ? Json.value("foreign-owner") : new Json.Arr(List.of()));
                var p = new BindingReader().envelope(Json.parse(bytes, AirJson.Limits.defaults()));
                var original = AirValidator.validate(p).issues();
                require(!original.isEmpty(), "invalid oracle has no Validator issues");
                require(original.stream().anyMatch(i -> i.subject().isPresent() && !i.rule().isBlank() && !i.detail().isBlank()), "incomplete original issue");
                var decoded = fails(INVALID_IR, bytes); var encoded = failure(INVALID_IR, () -> CODEC.encode(p));
                equal(original, decoded.issues()); equal(original, encoded.issues());
                equal("$", decoded.path()); equal("$", encoded.path());
            }
        });
        check("Natural span bases beyond Java range are representability limits", () -> {
            for (String field : List.of("lineBase", "columnBase")) {
                String path = "publication.origins.0.location.span." + field;
                for (String base : List.of("2", "123456789012345678901234567890"))
                    representability("$.publication.origins[0].location.span." + field, changed(path, Json.value(base)));
                for (String bad : List.of("-1", "-0", "+1", "01", "1.0")) fails(INPUT_ERROR, changed(path, Json.value(bad)));
                fails(INPUT_ERROR, utf8(text.replaceAll("\"" + field + "\":\"[01]\"", "\"" + field + "\":2")));
            }
        });
        check("empty FactScope entities is a Java representability limit", () -> {
            // Complete zero-known coverage isolates Id[] from claims requiring evidence.
            var coverage = Json.object("inventory", "COMPLETE", "scope", Json.object("kind", "entities", "entities", new Json.Arr(List.of())),
                    "items", new Json.Arr(List.of()), "uncertainties", new Json.Arr(List.of()));
            representability("$.publication.coverage.scope.entities", changed("publication.coverage", coverage));
        });
        check("admitted blank Text is a field-specific representability limit", () -> {
            // Binding §§3/4/10.3 gives these Text fields no nonBlank lexical predicate.
            for (String blank : List.of("", " ", "\t\n", "\u2003")) {
                for (String path : List.of("publication.artifacts.0.logicalName", "publication.coverage.items.0.sourceKey",
                        "publication.uncertainties.0.code", "publication.uncertainties.0.reason", "publication.origins.2.rule",
                        "publication.id.localId", "publication.units.0.id.publication", "publication.units.0.id.localId",
                        "publication.units.0.entries.0.id.unit"))
                    representability("$." + path.replaceAll("\\.([0-9]+)", "[$1]"), changed(path, Json.value(blank)));
                var frame = Json.object("including", at("publication.artifacts.0.id"), "included", at("publication.artifacts.1.id"),
                        "requestedName", blank, "site", null);
                representability("$.publication.origins[0].includes[0].requestedName",
                        changed("publication.origins.0.includes", new Json.Arr(List.of(frame))));
                // contentDigest is Text? but has no Java nonBlank restriction: preserve it.
                byte[] input = changed("publication.artifacts.0.contentDigest", Json.value(blank));
                var p = CODEC.decode(input); equal(Optional.of(blank), p.artifacts().get(0).contentDigest());
                bytes(input, CODEC.encode(p));
            }
        });
        check("unexpected constructor exceptions are never classified generically", CodecSuite::unexpectedConstructor);
        check("Span coordinates below declared bases are representability limits", () -> {
            for (String field : List.of("start.line", "end.line", "start.column", "end.column")) {
                // Bases 1 keep this case independent of the existing base >1 limit.
                var span = edit(span("1", "1", "1", "1", "1", "1"), field.split("\\."), 0, Json.value("0"));
                spanLimit(span);
            }
        });
        check("Span inverted lines are representability limits", () -> spanLimit(span("10", "4", "9", "20", "1", "0")));
        check("Span inverted columns on same line are representability limits", () -> spanLimit(span("4", "17", "4", "16", "1", "0")));
        check("coherent spans preserve equality bases and lexicographic order", () -> {
            for (boolean included : List.of(false, true)) for (String base : List.of("0", "1")) {
                // Equality is allowed for either end convention; a later line may have a smaller column.
                for (Json.Value span : List.of(span(base, base, base, base, base, base), span("4", "20", "5", "1", base, base)))
                    for (boolean exclusive : List.of(false, true)) {
                        byte[] input = spanInput(edit(span, new String[]{"endExclusive"}, 0, Json.value(exclusive)), included);
                        var p = CODEC.decode(input); bytes(input, CODEC.encode(p)); roundTrip(p);
                    }
            }
        });
        check("coherent arbitrary bases reach only their Java representation limit", () -> {
            for (boolean included : List.of(false, true)) {
                String huge = "123456789012345678901234567890";
                for (String base : List.of("2", huge)) {
                    representability(spanPath(included) + ".lineBase", spanInput(span(base, "0", base, "1", base, "0"), included));
                    representability(spanPath(included) + ".columnBase", spanInput(span("4", base, "5", base, "1", base), included));
                }
                // Both conditions may coexist; the first explicit base boundary is still a limit.
                representability(spanPath(included) + ".lineBase", spanInput(span("1", "0", "2", "0", "2", "0"), included));
                representability(spanPath(included) + ".columnBase", spanInput(span("4", "2", "4", "3", "1", "3"), included));
            }
        });
        check("invalid Span Natural lexemes remain physical errors at both sites", () -> {
            var coherent = span("1", "1", "1", "1", "1", "1");
            for (boolean included : List.of(false, true))
                for (String field : List.of("start.line", "start.column", "end.line", "end.column", "lineBase", "columnBase")) {
                    for (String bad : List.of("-1", "01", "+1", "-0", "1.0", "1e3"))
                        fails(INPUT_ERROR, spanInput(edit(coherent, field.split("\\."), 0, Json.value(bad)), included));
                    // The marker occurs only at this coordinate, including inside IncludeFrame.site.
                    String marked = new String(spanInput(edit(coherent, field.split("\\."), 0, Json.value("number-marker")), included), StandardCharsets.UTF_8);
                    fails(INPUT_ERROR, utf8(marked.replace("\"number-marker\"", "1")));
                }
        });
        check("4B manual scalar AIR oracle validates independently", () -> {
            var expected = ScalarAssignOracle.publication();
            var result = AirValidator.validate(expected);
            equal(ValidationResult.Status.STRUCTURALLY_VALID, result.status());
            equal(List.of(), result.issues());
            equal(2, result.statistics().operations()); equal(2, result.statistics().operands());
        });
        check("4B Java oracle has no transport or golden dependency", ScalarAssignChecks::independentOracle);
        check("4B independent golden and model satisfy all four equalities", ScalarAssignChecks::golden);
        check("4B PARTIAL inventories origins precision and uncertainties preserved", ScalarAssignChecks::partialEvidence);
        check("4B text and nullable fields preserve exact values", ScalarAssignChecks::textAndNullable);
        check("4B object and cell references must close", ScalarAssignChecks::dangling);
        check("4B storage kind domain and owner remain validated", ScalarAssignChecks::storageAndDomain);
        check("4B operand operation and entry owners cannot be repaired", ScalarAssignChecks::operandOwners);
        check("4B Assign destination and source roles remain validated", ScalarAssignChecks::roles);
        check("4B conflicting literal domain remains a Validator rule", ScalarAssignChecks::literalConflict);
        check("4B Assign terminator and Return instruction fail I-04", ScalarAssignChecks::slots);
        check("4B all supported fields required and unknown fields rejected", ScalarAssignChecks::fields);
        check("4B null tokens kinds and ID shapes preserve physical taxonomy", ScalarAssignChecks::wrongPhysicalValues);
        check("4B recognized unsupported forms remain IMPLEMENTATION_LIMIT", ScalarAssignChecks::unsupported);
        check("4B every Lifetime Visibility and OperandRole token has a literal oracle", ScalarAssignChecks::enumTables);
        check("4B ordered Assigns preserve independent wire and model arrays", ScalarAssignChecks::order);
        check("4B default representability and opt-in operational budgets", ScalarAssignChecks::limits);
        check("4B scale N and 2N and repeated references preserve linear structure", ScalarAssignChecks::scale);
        System.out.println("PASS: " + checks + " deterministic transport checks");
    }
    private static void check(String name, Runnable body) {
        body.run(); System.out.println("json-ok " + (++checks) + " - " + name);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void equal(Object expected, Object actual) { if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + ", actual " + actual); }
    private static void bytes(byte[] expected, byte[] actual) { require(Arrays.equals(expected, actual), "canonical bytes differ"); }
    private static byte[] utf8(String s) { return s.getBytes(StandardCharsets.UTF_8); }
    private static byte[] concat(byte[]... chunks) {
        var out = new java.io.ByteArrayOutputStream(); for (byte[] c : chunks) out.writeBytes(c); return out.toByteArray();
    }
    private static AirJsonException failure(AirJsonException.Code expected, Runnable action) {
        try { action.run(); } catch (AirJsonException e) {
            equal(expected, e.code()); require(!e.path().isEmpty(), "missing error site");
            if (expected == INVALID_IR) {
                require(!e.issues().isEmpty(), "INVALID_IR missing AIR rule");
                require(e.issues().stream().allMatch(i -> !i.rule().isBlank() && !i.detail().isBlank()), "incomplete AIR diagnostic");
            }
            return e;
        }
        throw new AssertionError("Expected " + expected + ", operation succeeded");
    }
    private static AirJsonException fails(AirJsonException.Code code, byte[] input) { return failure(code, () -> CODEC.decode(input)); }
    private static void representability(String path, byte[] input) {
        var error = fails(IMPLEMENTATION_LIMIT, input);
        equal(path, error.path()); equal(List.of(), error.issues());
        require(error.getMessage().contains("air-java representability limit"), "missing representability diagnostic");
    }
    private static Json.Value span(String sl, String sc, String el, String ec, String lb, String cb) {
        return Json.object("start", Json.object("line", sl, "column", sc), "end", Json.object("line", el, "column", ec),
                "lineBase", lb, "columnBase", cb, "columnUnit", "UNICODE_SCALAR", "endExclusive", false);
    }
    private static byte[] spanInput(Json.Value span, boolean included) {
        var location = Json.object("kind", "line_columns", "span", span);
        if (!included) return changed("publication.origins.0.location", location);
        var frame = Json.object("including", at("publication.artifacts.0.id"), "included", at("publication.artifacts.1.id"),
                "requestedName", "COPYBOOK", "site", location);
        return changed("publication.origins.0.includes", new Json.Arr(List.of(frame)));
    }
    private static String spanPath(boolean included) {
        return "$.publication.origins[0]." + (included ? "includes[0].site.span" : "location.span");
    }
    private static void spanLimit(Json.Value span) {
        for (boolean included : List.of(false, true)) for (boolean exclusive : List.of(false, true)) {
            byte[] input = spanInput(edit(span, new String[]{"endExclusive"}, 0, Json.value(exclusive)), included);
            representability(spanPath(included), input);
            var error = fails(IMPLEMENTATION_LIMIT, input);
            require(error.getMessage().contains("Physical Span fields accepted"), "missing physical/binding boundary");
            require(error.getMessage().contains("no pinned AIR invalidity rule identified"), "Java restriction presented as AIR rule");
        }
    }
    /** Fault injection at the private construction boundary, independent of constructor messages. */
    private static void unexpectedConstructor() {
        var sentinel = new IllegalArgumentException("unanalysed constructor failure");
        try {
            Class<?> at = Class.forName("io.github.gustavo2358.air.json.BindingReader$At");
            var constructor = at.getDeclaredConstructor(Json.Value.class, String.class); constructor.setAccessible(true);
            var construct = at.getDeclaredMethod("construct", java.util.function.Supplier.class); construct.setAccessible(true);
            var site = constructor.newInstance(tree, "$.injected");
            java.util.function.Supplier<Object> fail = () -> { throw sentinel; };
            try { construct.invoke(site, fail); throw new AssertionError("Unexpected constructor succeeded"); }
            catch (java.lang.reflect.InvocationTargetException error) {
                require(error.getCause() == sentinel, "Unexpected constructor exception was classified generically: " + error.getCause());
            }
        } catch (ReflectiveOperationException error) { throw new AssertionError("Cannot inject constructor failure", error); }
    }
    private static void localRule(String rule, String path, byte[] input) {
        var e = fails(INVALID_IR, input);
        equal(path, e.path()); equal(1, e.issues().size());
        var issue = e.issues().get(0);
        equal(ValidationIssue.Kind.INVALID_IR, issue.kind()); equal(rule, issue.rule());
        require(!issue.detail().isBlank(), "missing local AIR detail");
    }
    private static void roundTrip(Publication p) { equal(p, CODEC.decode(CODEC.encode(p))); }
    private static Json.Value at(String path) {
        return at(tree, path);
    }
    private static Json.Value at(Json.Value node, String path) {
        Json.Value v = node;
        for (String part : path.split("\\.")) v = v instanceof Json.Obj o ? o.fields().get(part) : ((Json.Arr)v).values().get(Integer.parseInt(part));
        return v;
    }
    /** Literal expected spellings from binding §10.4, never enum names or declaration order. */
    private static void tokens() {
        tokenCases("publication.uncertainties.0.dimensions.0",
                List.of(Evidence.Dimension.CONTROL, Evidence.Dimension.STORAGE, Evidence.Dimension.EFFECTS,
                        Evidence.Dimension.VALUES, Evidence.Dimension.DEPENDENCIES),
                List.of("CONTROL", "STORAGE", "EFFECTS", "VALUES", "DEPENDENCIES"), p -> p.uncertainties().get(0).dimensions().get(0));
        tokenCases("publication.units.0.sequences.0.terminator.header.precision.control.status",
                List.of(Evidence.PrecisionStatus.EXACT, Evidence.PrecisionStatus.CONSERVATIVE, Evidence.PrecisionStatus.OPEN,
                        Evidence.PrecisionStatus.UNAVAILABLE, Evidence.PrecisionStatus.NOT_APPLICABLE),
                List.of("EXACT", "CONSERVATIVE", "OPEN", "UNAVAILABLE", "NOT_APPLICABLE"),
                p -> p.units().get(0).sequences().get(0).terminator().header().precision().control().status());
        var coverage = List.of(Evidence.CoverageStatus.MODELED, Evidence.CoverageStatus.ABSTRACTED,
                Evidence.CoverageStatus.UNSUPPORTED, Evidence.CoverageStatus.INPUT_MISSING);
        var coverageTokens = List.of("MODELED", "ABSTRACTED", "UNSUPPORTED", "INPUT_MISSING");
        tokenCases("publication.coverage.items.0.status", coverage, coverageTokens, p -> p.coverage().items().get(0).status());
        tokenCases("publication.units.0.sequences.0.terminator.header.coverage", coverage, coverageTokens,
                p -> p.units().get(0).sequences().get(0).terminator().header().coverage());
        tokenCases("publication.coverage.inventory",
                List.of(Evidence.InventoryStatus.COMPLETE, Evidence.InventoryStatus.PARTIAL, Evidence.InventoryStatus.UNAVAILABLE),
                List.of("COMPLETE", "PARTIAL", "UNAVAILABLE"), p -> p.coverage().inventory());
        tokenCases("publication.origins.0.location.span.columnUnit",
                List.of(Origins.ColumnUnit.UNICODE_SCALAR, Origins.ColumnUnit.UTF16_CODE_UNIT, Origins.ColumnUnit.OCTET),
                List.of("UNICODE_SCALAR", "UTF16_CODE_UNIT", "OCTET"),
                p -> ((Origins.LineColumns)((Origins.Written)p.origins().get(0)).location().orElseThrow()).span().columnUnit());
    }
    private static void tokenCases(String path, List<?> meanings, List<String> lexemes,
            java.util.function.Function<Publication,Object> extract) {
        equal(meanings.size(), lexemes.size());
        for (int i = 0; i < lexemes.size(); i++) {
            // Isolate binding mapping; changing one claim need not produce structurally valid AIR.
            var p = new BindingReader().envelope(Json.parse(changed(path, Json.value(lexemes.get(i))), AirJson.Limits.defaults()));
            equal(meanings.get(i), extract.apply(p));
            equal(Json.value(lexemes.get(i)), at(new BindingWriter().envelope(p), path));
        }
    }
    /** Compiled writer guard: identical current spellings cannot mask a return to runtime authority. */
    private static void noRuntimeTokens() {
        try (var in = new java.io.DataInputStream(java.util.Objects.requireNonNull(
                BindingWriter.class.getResourceAsStream("BindingWriter.class")))) {
            equal(0xcafebabe, in.readInt()); in.readUnsignedShort(); in.readUnsignedShort();
            int count = in.readUnsignedShort(); int[] tags = new int[count]; Object[] pool = new Object[count];
            for (int i = 1; i < count; i++) {
                tags[i] = in.readUnsignedByte();
                switch (tags[i]) {
                    case 1 -> pool[i] = in.readUTF();
                    case 3, 4 -> in.readInt();
                    case 5, 6 -> { in.readLong(); i++; }
                    case 7, 8, 16, 19, 20 -> pool[i] = in.readUnsignedShort();
                    case 9, 10, 11, 12, 17, 18 -> pool[i] = new int[]{in.readUnsignedShort(), in.readUnsignedShort()};
                    case 15 -> { in.readUnsignedByte(); in.readUnsignedShort(); }
                    default -> throw new AssertionError("Unknown classfile tag: " + tags[i]);
                }
            }
            for (int i = 1; i < count; i++) {
                if (tags[i] != 10 && tags[i] != 11 && tags[i] != 18) continue;
                int[] ref = (int[])pool[i], nameType = (int[])pool[ref[1]];
                String name = (String)pool[nameType[0]], descriptor = (String)pool[nameType[1]];
                require(!name.equals("name") && !name.equals("toString"), "Runtime token authority: " + name);
                if (tags[i] == 18) {
                    require(!(descriptor.contains("io/github/gustavo2358/air/model/") && descriptor.endsWith("Ljava/lang/String;")),
                            "Runtime model-to-string concatenation");
                } else {
                    String owner = (String)pool[(Integer)pool[ref[0]]];
                    require(!(owner.equals("java/lang/String") && name.equals("valueOf")), "Runtime String.valueOf token authority");
                }
            }
        } catch (java.io.IOException error) { throw new AssertionError("Cannot inspect compiled writer", error); }
    }
    private static Json.Value edit(Json.Value node, String[] path, int i, Json.Value replacement) {
        if (i == path.length) return replacement;
        if (node instanceof Json.Obj o) {
            var fields = new LinkedHashMap<>(o.fields());
            if (i == path.length - 1 && replacement == null) fields.remove(path[i]);
            else fields.put(path[i], edit(fields.get(path[i]), path, i+1, replacement));
            return new Json.Obj(fields);
        }
        var values = new ArrayList<>(((Json.Arr)node).values()); int index = Integer.parseInt(path[i]);
        values.set(index, edit(values.get(index), path, i+1, replacement)); return new Json.Arr(values);
    }
    private static byte[] changed(String path, Json.Value replacement) {
        return Json.write(edit(tree, path.split("\\."),0,replacement), AirJson.Limits.defaults());
    }
    private static String join(String path, String child) { return path.isEmpty() ? child : path + "." + child; }
    private static void requiredFields(Json.Value node, String path) {
        if (node instanceof Json.Obj o) for (var entry : o.fields().entrySet()) {
            fails(INPUT_ERROR, changed(join(path,entry.getKey()), null)); requiredFields(entry.getValue(), join(path,entry.getKey()));
        }
        if (node instanceof Json.Arr a) for (int i=0;i<a.values().size();i++) requiredFields(a.values().get(i),join(path,Integer.toString(i)));
    }
    private static void unknownFields(Json.Value node, String path) {
        if (node instanceof Json.Obj o) {
            var f = new LinkedHashMap<>(o.fields()); f.put("__unexpected", Json.Nil.INSTANCE);
            if (path.isEmpty()) fails(INPUT_ERROR, Json.write(new Json.Obj(f),AirJson.Limits.defaults()));
            else fails(INPUT_ERROR,changed(path,new Json.Obj(f)));
            for(var e:o.fields().entrySet()) unknownFields(e.getValue(),join(path,e.getKey()));
        }
        if (node instanceof Json.Arr a) for(int i=0;i<a.values().size();i++) unknownFields(a.values().get(i),join(path,Integer.toString(i)));
    }
    private static void duplicateEveryObject(Json.Value node, boolean escaped) {
        if (node instanceof Json.Obj o) {
            String whole = new String(Json.write(o, AirJson.Limits.defaults()),StandardCharsets.UTF_8);
            String key = o.fields().keySet().iterator().next();
            String duplicate = escaped ? "\\u" + String.format(java.util.Locale.ROOT,"%04x",(int)key.charAt(0)) + key.substring(1) : key;
            String sameValue = new String(Json.write(o.fields().get(key),AirJson.Limits.defaults()),StandardCharsets.UTF_8);
            String mutated = whole.substring(0,whole.length()-1) + ",\"" + duplicate + "\":" + sameValue + "}";
            require(text.contains(whole), "mutation site missing");
            var error = fails(INPUT_ERROR,utf8(text.replace(whole,mutated)));
            require(error.getMessage().contains("Duplicate property"), "not rejected by duplicate-property rule");
            o.fields().values().forEach(v -> duplicateEveryObject(v,escaped));
        } else if (node instanceof Json.Arr a) a.values().forEach(v -> duplicateEveryObject(v,escaped));
    }
    private static Publication copy(Publication p, List<Origins.Artifact> artifacts, List<Unit> units, List<Origins.Origin> origins,
            Evidence.Coverage coverage, List<Evidence.Uncertainty> gaps) {
        return new Publication(p.id(),p.airVersion(),p.capabilities(),artifacts,units,p.storage(),p.resources(),p.artifactRelations(),origins,coverage,gaps,p.premises());
    }
    private static Unit sequences(Unit u, List<Sequence> seqs) {
        return new Unit(u.id(),u.containingUnit(),u.objects(),u.visibleObjects(),u.entries(),seqs,u.completionPorts(),u.body(),u.bodyUnavailable(),u.coverage(),u.origin());
    }
    private static Publication withSequences(List<Sequence> seqs) {
        return copy(EXPECTED,EXPECTED.artifacts(),List.of(sequences(EXPECTED.units().get(0),seqs)),EXPECTED.origins(),EXPECTED.coverage(),EXPECTED.uncertainties());
    }
    private static Publication withFirstWritten(Optional<Origins.Location> location,List<Origins.IncludeFrame> includes,boolean exact) {
        var origins = new ArrayList<>(EXPECTED.origins()); var w = (Origins.Written)origins.get(0);
        origins.set(0,new Origins.Written(w.id(),w.artifact(),location,includes,exact));
        return copy(EXPECTED,EXPECTED.artifacts(),EXPECTED.units(),origins,EXPECTED.coverage(),EXPECTED.uncertainties());
    }
    private static Publication renamedArtifact(String name) {
        var artifacts = new ArrayList<>(EXPECTED.artifacts()); var a=artifacts.get(0);
        artifacts.set(0,new Origins.Artifact(a.id(),name,Optional.of("")));
        return copy(EXPECTED,artifacts,EXPECTED.units(),EXPECTED.origins(),EXPECTED.coverage(),EXPECTED.uncertainties());
    }
    private static void provenance() {
        var original=EXPECTED.artifacts().get(0).id(); var expanded=EXPECTED.artifacts().get(1).id();
        var frames=List.of(new Origins.IncludeFrame(original,expanded,"Z/primeiro",Optional.empty()),
                new Origins.IncludeFrame(expanded,original,"A/segundo",Optional.of(GobackOracle.location(1,0,1,7))));
        var p=withFirstWritten(Optional.empty(),frames,false); roundTrip(p);
        var w=(Origins.Written)CODEC.decode(CODEC.encode(p)).origins().get(0);
        equal(Optional.empty(),w.location()); equal(false,w.exact()); equal(frames,w.includes());
    }
    private static void unicode() {
        String chars="a\"\\/\b\f\n\r\t\u0000\u001f á e\u0301 é <>& 😀 \u2028\u2029";
        var p=renamedArtifact(chars); roundTrip(p);
        String encoded=new String(CODEC.encode(p),StandardCharsets.UTF_8);
        require(encoded.contains("a\\\"\\\\/\\b\\f\\n\\r\\t\\u0000\\u001f á e\u0301 é <>& 😀 \u2028\u2029"),"noncanonical escaping or Unicode normalization");
        bytes(CODEC.encode(p),CODEC.encode(CODEC.decode(utf8(encoded.replace("😀","\\ud83d\\ude00")))));
        for(int c=0;c<32;c++) roundTrip(renamedArtifact("control:"+(char)c));
    }
    private static <T> List<T> reversed(List<T> input) { var a=new ArrayList<>(input); Collections.reverse(a); return a; }
    private static void arrayOrder() {
        var c=EXPECTED.coverage(); var coverage=new Evidence.Coverage(c.inventory(),c.scope(),reversed(c.items()),c.uncertainties());
        var p=copy(EXPECTED,reversed(EXPECTED.artifacts()),EXPECTED.units(),reversed(EXPECTED.origins()),coverage,reversed(EXPECTED.uncertainties()));
        roundTrip(p); require(!Arrays.equals(golden,CODEC.encode(p)),"array permutation normalized away");
        var u=EXPECTED.units().get(0); var s=u.sequences().get(0); var h=s.terminator().header();
        var other=new Sequence(new LabelId(u.id(),"a-sequence"),List.of(),new Operations.Return(new Operations.Header(new OperationId(u.id(),"a-return"),h.origin(),
                h.coverage(),h.precision(),h.uncertainties()),List.of()),s.origin());
        var entries=new ArrayList<>(u.entries()); var e=entries.get(0);
        entries.add(new Entries.Entry(new EntryId(u.id(),"a-entry"),e.initialLabel(),e.signature(),e.state(),e.origin()));
        var unit=new Unit(u.id(),u.containingUnit(),List.of(),List.of(),entries,List.of(s,other),List.of(),u.body(),u.bodyUnavailable(),u.coverage(),u.origin());
        roundTrip(withSequences(List.of(s,other)));
        roundTrip(copy(EXPECTED,EXPECTED.artifacts(),List.of(unit),EXPECTED.origins(),EXPECTED.coverage(),EXPECTED.uncertainties()));
        var unit2=new Unit(new UnitId(GobackOracle.PUB,"a-unit"),Optional.empty(),List.of(),List.of(),
                List.of(new Entries.Entry(new EntryId(new UnitId(GobackOracle.PUB,"a-unit"),"entry"),Optional.of(new LabelId(new UnitId(GobackOracle.PUB,"a-unit"),"label")),
                        e.signature(),e.state(),e.origin())),List.of(new Sequence(new LabelId(new UnitId(GobackOracle.PUB,"a-unit"),"label"),List.of(),
                        new Operations.Return(new Operations.Header(new OperationId(new UnitId(GobackOracle.PUB,"a-unit"),"return"),h.origin(),h.coverage(),h.precision(),h.uncertainties()),List.of()),s.origin())),
                List.of(),Unit.BodyAvailability.AVAILABLE,Optional.empty(),u.coverage(),u.origin());
        var units=List.of(u,unit2);
        var many=copy(EXPECTED,EXPECTED.artifacts(),units,EXPECTED.origins(),EXPECTED.coverage(),EXPECTED.uncertainties());
        roundTrip(many);
    }
    private static void unsupported() {
        fails(IMPLEMENTATION_LIMIT,changed("publication.origins.0.location",Json.object("kind","offsets","start","0","end","9","unit","octet","endExclusive",true)));
        fails(IMPLEMENTATION_LIMIT,changed("publication.origins.0",Json.object("kind","contractual","id",at("publication.origins.0.id"),"authority","test","version","1")));
        fails(IMPLEMENTATION_LIMIT,changed("publication.units.0.entries.0.signature.parameters.remainder",Json.object("kind","unknown","uncertainty",at("publication.uncertainties.0.id"))));
        // Nonempty containers never become empty successful Publications, regardless of deferred element form.
        for(String path:List.of("publication.resources","publication.artifactRelations","publication.premises",
                "publication.units.0.visibleObjects","publication.units.0.completionPorts",
                "publication.units.0.entries.0.state.conditions","publication.units.0.entries.0.signature.parameters.known",
                "publication.units.0.sequences.0.terminator.values"))
            fails(IMPLEMENTATION_LIMIT,changed(path,new Json.Arr(List.of(Json.object("kind","deferred-element")))));
        var w=(Origins.Written)EXPECTED.origins().get(0);
        var p=withFirstWritten(Optional.of(new Origins.Offsets(BigInteger.ZERO,BigInteger.TEN,"octet",true)),List.of(),true);
        failure(IMPLEMENTATION_LIMIT,()->CODEC.encode(p)); equal(w,EXPECTED.origins().get(0));
    }
    private static void ids() {
        var pub=GobackOracle.PUB; var unit=GobackOracle.UNIT;
        List<Id> ids=List.of(pub,new ArtifactId(pub,"a"),new ArtifactRelationId(pub,"r"),unit,new StorageId(pub,"s"),new ResourceId(pub,"r"),
                new OriginId(pub,"o"),new UncertaintyId(pub,"u"),new PremiseId(pub,"p"),new EntryId(unit,"e"),new LabelId(unit,"l"),
                new OperationId(unit,"op"),new ObjectId(unit,"ob"),new CompletionPortId(unit,"c"),
                new OperandId(new OperationOwner(new OperationId(unit,"op")),"operand"),new OperandId(new EntryOwner(new EntryId(unit,"e")),"operand"));
        var claim=new Evidence.Coverage(Evidence.InventoryStatus.COMPLETE,new Scopes.EntityScope(ids),List.of(),List.of());
        var p=copy(EXPECTED,EXPECTED.artifacts(),EXPECTED.units(),EXPECTED.origins(),claim,EXPECTED.uncertainties());
        equal(ids,((Scopes.EntityScope)new BindingReader().envelope(new BindingWriter().envelope(p)).coverage().scope()).entities());
        // Only transport of IDs is implemented here; missing entities still fail public closure checks.
        failure(INVALID_IR,()->CODEC.encode(p));
    }
}
