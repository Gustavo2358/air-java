package io.github.gustavo2358.air.json;

import java.util.ArrayList;
import java.util.List;
import static io.github.gustavo2358.air.json.Json.object;

/** Binding 1.0.0 at 51b4d9a: independent literal wire facts, only physical JSON constructors.
 * No model, W2cOracle, BindingReader, BindingWriter or AirJson calls supply expected fields. */
final class W2cWireOracle {
    private W2cWireOracle() {}
    static Json.Arr list(Json.Value... values) { return new Json.Arr(List.of(values)); }
    static Json.Value global(String domain, String local) {
        return object("domain", domain, "publication", "cp6-w2c-manual", "localId", local);
    }
    static Json.Value owned(String domain, String local) {
        return object("domain", domain, "publication", "cp6-w2c-manual", "unit", "unit", "localId", local);
    }
    static Json.Value operand(String op, String local, String role, String origin) {
        return object("id", object("domain", "operand", "publication", "cp6-w2c-manual", "unit", "unit", "owner",
                object("kind", "operation", "localId", op), "localId", local), "role", role, "origin", global("origin", origin));
    }
    static Json.Value type(String kind) { return object("kind", "known", "type", object("kind", kind)); }
    static Json.Value scope() { return object("kind", "unit", "unit", global("unit", "unit")); }
    static Json.Value claim(String status) {
        return object("scope", scope(), "status", status, "reasons", status.equals("EXACT") ? list() : list(global("uncertainty", "facts")));
    }
    static Json.Value precision() {
        return object("control", claim("EXACT"), "storage", claim("OPEN"), "effects", claim("UNAVAILABLE"), "values", claim("OPEN"), "dependencies", claim("UNAVAILABLE"));
    }
    static Json.Value coverage(Json.Value scope) {
        return object("inventory", "PARTIAL", "scope", scope, "items", list(), "uncertainties", list(global("uncertainty", "facts")));
    }
    static Json.Value header(String op) {
        return object("id", owned("operation", op), "origin", global("origin", "op-" + op), "coverage", "ABSTRACTED", "precision", precision(),
                "uncertainties", list(global("uncertainty", "facts"), global("uncertainty", "predicate-value-unknown")));
    }
    static Json.Value read(String op, String local, String role, String object) {
        return object("kind", "read", "header", operand(op, local, role, "read"), "place",
                object("kind", "object", "header", operand(op, local + "-place", "VALUE_READ", "place"), "object", owned("object", object)));
    }
    static Json.Value unknown(String op, String role, Json.Arr dependencies) {
        return object("kind", "unknown", "header", operand(op, "unknown", role, "predicate"), "typeRef", type("bool"),
                "dependencies", dependencies, "remainingReads", object("kind", "none"), "reason", global("uncertainty", "predicate-value-unknown"));
    }
    static Json.Value jump(String op) { return object("kind", "jump", "header", header(op), "destination", owned("label", "Lmerge")); }
    static Json.Value branch(Json.Value predicate) {
        return object("kind", "branch", "header", header("if"), "predicate", predicate,
                "trueDestination", owned("label", "Lthen"), "falseDestination", owned("label", "Lelse"));
    }
    static Json.Value premise() {
        return object("id", global("premise", "independent"), "authority", "fixture.authority  é", "justification", "Producer-declared separation; no inference\n😀",
                "origin", global("origin", "premise"), "assertion", object("kind", "disjoint_storage", "storage",
                        list(global("storage", "cell-WS-PGM"), global("storage", "cell-FLAG"))));
    }
    static Json.Value ret(String op) { return object("kind", "return", "header", header(op), "values", list()); }
    static Json.Value sequence(String label, Json.Arr instructions, Json.Value terminator) {
        return object("label", owned("label", label), "instructions", instructions, "terminator", terminator, "origin", global("origin", "sequence"));
    }
    static Json.Value document(String form) {
        boolean boolCell = List.of("bool", "branch", "unknown", "unknown-empty").contains(form);
        Json.Value first = ret("if"); Json.Arr instructions = list();
        switch (form) {
            case "jump" -> first = jump("if");
            case "branch" -> first = branch(read("if", "predicate-read", "PREDICATE", "FLAG"));
            case "full" -> first = branch(unknown("if", "PREDICATE", list(read("if", "dependency", "VALUE_READ", "FLAG"))));
            case "unknown", "unknown-empty" -> instructions = list(object("kind", "assign", "header", header("assign"), "destination",
                    object("kind", "object", "header", operand("assign", "destination", "VALUE_WRITE", "place"), "object", owned("object", "FLAG")),
                    "value", unknown("assign", "VALUE_READ", form.equals("unknown") ? list(read("assign", "dependency", "VALUE_READ", "WS-PGM")) : list())));
            case "bool", "premise" -> { }
            default -> throw new AssertionError("Unknown oracle fixture");
        }
        var cells = new ArrayList<Json.Value>(); var objects = new ArrayList<Json.Value>();
        for (String local : List.of("FLAG", "WS-PGM")) {
            var type = type(boolCell && local.equals("FLAG") ? "bool" : "text");
            cells.add(object("kind", "cell", "header", object("id", global("storage", "cell-" + local), "owner", global("unit", "unit"),
                    "lifetime", "ACTIVATION", "visibility", "PRIVATE", "origin", global("origin", "storage")), "typeRef", type));
            objects.add(object("id", owned("object", local), "displayName", local, "typeRef", type, "storage", object("kind", "cell", "storage", global("storage", "cell-" + local)),
                    "visibility", "PRIVATE", "origin", global("origin", "object"), "coverage", "MODELED", "precision", precision()));
        }
        var inventory = object("known", list(), "remainder", object("kind", "none"));
        var entry = object("id", owned("entry", "entry"), "initialLabel", owned("label", "Lif"),
                "signature", object("parameters", inventory, "results", inventory, "origin", global("origin", "entry")),
                "state", object("conditions", list(), "uncertainties", list(global("uncertainty", "facts"))), "origin", global("origin", "entry"));
        var unit = object("id", global("unit", "unit"), "containingUnit", null, "objects", new Json.Arr(objects), "visibleObjects", list(), "entries", list(entry),
                "sequences", list(sequence("Lif", instructions, first), sequence("Lthen", list(), form.equals("full") ? jump("then") : ret("then")),
                        sequence("Lelse", list(), form.equals("full") ? jump("else") : ret("else")), sequence("Lmerge", list(), ret("merge"))),
                "completionPorts", list(), "body", object("kind", "available"), "coverage", coverage(scope()), "origin", global("origin", "unit"));
        var origins = new ArrayList<Json.Value>();
        for (String local : List.of("op-if", "op-then", "op-else", "op-merge", "op-assign", "read", "place", "predicate", "premise", "object", "storage", "sequence", "entry", "unit"))
            origins.add(object("kind", "written", "id", global("origin", local), "artifact", global("artifact", "fixture"), "location", null, "includes", list(), "exact", false));
        var uncertainties = list(object("id", global("uncertainty", "facts"), "code", "fixture:PARTIAL", "dimensions",
                        list(Json.value("CONTROL"), Json.value("STORAGE"), Json.value("EFFECTS"), Json.value("VALUES"), Json.value("DEPENDENCIES")),
                        "scope", scope(), "reason", "Not a producer certification", "origin", global("origin", "unit")),
                object("id", global("uncertainty", "predicate-value-unknown"), "code", "fixture:VALUE_UNKNOWN", "dimensions", list(Json.value("VALUES")),
                        "scope", scope(), "reason", "Value remains unknown", "origin", global("origin", "predicate")));
        return object("binding", "analysis-ir-json", "bindingVersion", "1.0.0", "airVersion", "2.0.0", "publication", object(
                "id", object("domain", "publication", "localId", "cp6-w2c-manual"), "capabilities", object("required", list(), "provided", list()),
                "artifacts", list(object("id", global("artifact", "fixture"), "logicalName", "w2c.manual", "contentDigest", null)), "units", list(unit), "storage", new Json.Arr(cells),
                "resources", list(), "artifactRelations", list(), "origins", new Json.Arr(origins), "coverage", coverage(object("kind", "publication", "publication", object("domain", "publication", "localId", "cp6-w2c-manual"))),
                "uncertainties", uncertainties, "premises", List.of("full", "premise").contains(form) ? list(premise()) : list()));
    }
}
