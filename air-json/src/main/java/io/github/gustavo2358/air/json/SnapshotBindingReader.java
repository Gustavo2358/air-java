package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.AirShape;
import io.github.gustavo2358.air.model.AirSnapshot;
import io.github.gustavo2358.air.model.AirSnapshotBuilder;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToLongFunction;

/**
 * Direct wire-to-snapshot binding. The current vertical owns the complete empty-inventory profile
 * and the minimal return-program profile used by the independent GOBACK oracle. Other admitted
 * variants remain explicit implementation boundaries until their typed mappings are added.
 */
final class SnapshotBindingReader {
    private final AirSnapshotBuilder target;

    SnapshotBindingReader(AirSnapshotBuilder target) { this.target = target; }

    private final class At {
        private final Json.Value value;
        private final At parent;
        private final String field;
        private final int index;
        private volatile Map<String, Json.Value> cachedObject;

        At(Json.Value value, At parent, String field, int index) {
            this.value = value; this.parent = parent; this.field = field; this.index = index;
        }

        String path() {
            var ancestors = new ArrayDeque<At>();
            for (At at = this; at.parent != null; at = at.parent) ancestors.push(at);
            var result = new StringBuilder("$");
            while (!ancestors.isEmpty()) {
                At at = ancestors.pop();
                if (at.field == null) result.append('[').append(at.index).append(']');
                else result.append('.').append(at.field);
            }
            return result.toString();
        }

        Map<String, Json.Value> object() {
            var known = cachedObject;
            if (known != null) return known;
            if (value instanceof Json.Obj object) return cachedObject = object.fields();
            if (value instanceof PagedJson.Node node && node.object()) return cachedObject = node.fields();
            throw Json.input(path(), "Expected object");
        }

        At child(String name) {
            Json.Value child = object().get(name);
            if (child == null) throw Json.input(path() + "." + name, "Required field omitted");
            return new At(child, this, name, -1);
        }

        At fields(String... names) {
            var fields = object();
            if (fields.size() == names.length) {
                boolean complete = true;
                for (String name : names) if (!fields.containsKey(name)) { complete = false; break; }
                if (complete) return this;
            }
            Set<String> required = Set.of(names);
            for (String name : fields.keySet())
                if (!required.contains(name)) throw Json.input(path() + "." + name, "Unknown field");
            for (String name : names)
                if (!fields.containsKey(name)) throw Json.input(path() + "." + name, "Required field omitted");
            return this;
        }

        String text() {
            if (value instanceof Json.Text text) return text.value();
            if (value instanceof PagedJson.Node node && node.textValue()) return node.text();
            throw Json.input(path(), "Expected string");
        }

        boolean bool() {
            if (value instanceof Json.Bool bool) return bool.value();
            if (value instanceof PagedJson.Node node && node.bool() != null) return node.bool();
            throw Json.input(path(), "Expected boolean");
        }

        boolean nil() {
            if (value == Json.Nil.INSTANCE) return true;
            return value instanceof PagedJson.Node node && !node.object() && !node.array()
                    && !node.textValue() && node.bool() == null;
        }

        List<Json.Value> array() {
            if (value instanceof Json.Arr array) return array.values();
            if (value instanceof PagedJson.Node node && node.array()) return node.values();
            throw Json.input(path(), "Expected array");
        }

        At element(List<Json.Value> values, int at) { return new At(values.get(at), this, null, at); }

        String modelText() {
            String text = text();
            if (text.isBlank()) throw Json.limit(path(), "air-java representability limit: Require.text rejects blank Text admitted by the pinned binding");
            return text;
        }

        void emptyInventory() {
            if (!array().isEmpty()) throw Json.limit(path(), "Nonempty typed inventory outside direct snapshot binding coverage");
        }
    }

    AirSnapshot envelope(Json.Value value) {
        var envelope = new At(value, null, null, -1)
                .fields("binding", "bindingVersion", "airVersion", "publication");
        version(envelope.child("binding"), "analysis-ir-json");
        version(envelope.child("bindingVersion"), "1.0.0");
        version(envelope.child("airVersion"), "2.0.0");
        var publication = envelope.child("publication").fields("id", "capabilities", "artifacts", "units",
                "storage", "resources", "artifactRelations", "origins", "coverage", "uncertainties", "premises");

        long id = publicationId(publication.child("id"));
        long capabilities = manifest(publication.child("capabilities"));
        long artifacts = list(publication.child("artifacts"), AirShape.ORIGINS_ARTIFACT, this::artifact);
        long units = list(publication.child("units"), AirShape.UNIT, this::unit);
        long storage = empty(publication.child("storage"), AirShape.MEMORY_STORAGE);
        long resources = empty(publication.child("resources"), AirShape.INTERACTIONS_RESOURCE);
        long relations = empty(publication.child("artifactRelations"), AirShape.ARTIFACTS_RELATION);
        long origins = list(publication.child("origins"), AirShape.ORIGINS_ORIGIN, this::origin);
        long coverage = coverage(publication.child("coverage"));
        long uncertainties = list(publication.child("uncertainties"), AirShape.EVIDENCE_UNCERTAINTY, this::uncertainty);
        long premises = empty(publication.child("premises"), AirShape.PROOFS_PREMISE);
        long root = target.record(AirShape.PUBLICATION, id, semanticVersion(), capabilities, artifacts, units,
                storage, resources, relations, origins, coverage, uncertainties, premises);
        return target.finish(root);
    }

    private void version(At value, String expected) {
        if (!value.text().equals(expected))
            throw new AirJsonException(AirJsonException.Code.VERSION_MISMATCH, value.path(), "Unsupported version");
    }

    private long semanticVersion() {
        return target.record(AirShape.SEMANTIC_VERSION, integer("2"), integer("0"), integer("0"));
    }

    private long manifest(At at) {
        at.fields("required", "provided");
        return target.record(AirShape.CAPABILITIES_MANIFEST,
                capabilities(at.child("required")), capabilities(at.child("provided")));
    }

    private long capabilities(At at) {
        var values = at.array();
        try (var output = target.list(AirShape.CAPABILITIES_CAPABILITY)) {
            for (int index = 0; index < values.size(); index++) {
                var item = at.element(values, index).fields("name", "version");
                output.add(target.record(AirShape.CAPABILITIES_CAPABILITY,
                        text(item.child("name").modelText()), text(item.child("version").modelText())));
            }
            return output.finish();
        }
    }

    private long artifact(At at) {
        at.fields("id", "logicalName", "contentDigest");
        return target.record(AirShape.ORIGINS_ARTIFACT, typedId(at.child("id"), AirShape.IDS_ARTIFACT_ID),
                text(at.child("logicalName").modelText()), optional(at.child("contentDigest"), AirShape.TEXT, value -> text(value.text())));
    }

    private long unit(At at) {
        at.fields("id", "containingUnit", "objects", "visibleObjects", "entries", "sequences",
                "completionPorts", "body", "coverage", "origin");
        var body = at.child("body");
        if (!body.child("kind").text().equals("available"))
            throw Json.limit(body.path(), "BodyKnowledge.unavailable outside direct snapshot binding coverage");
        body.fields("kind");
        if (at.child("entries").array().isEmpty()) throw invalid(at.child("entries"), "AIR-01 §2", "Available body requires at least one entry");
        if (at.child("sequences").array().isEmpty()) throw invalid(at.child("sequences"), "AIR-01 §3", "Available body requires at least one sequence");
        long entries = list(at.child("entries"), AirShape.ENTRIES_ENTRY, this::entry);
        long sequences = list(at.child("sequences"), AirShape.SEQUENCE, this::sequence);
        return target.record(AirShape.UNIT, typedId(at.child("id"), AirShape.IDS_UNIT_ID),
                optional(at.child("containingUnit"), AirShape.IDS_UNIT_ID, value -> typedId(value, AirShape.IDS_UNIT_ID)),
                empty(at.child("objects"), AirShape.MEMORY_OBJECT_DECLARATION),
                empty(at.child("visibleObjects"), AirShape.IDS_OBJECT_ID), entries, sequences,
                empty(at.child("completionPorts"), AirShape.ENTRIES_COMPLETION_PORT),
                target.scalar(AirShape.UNIT_BODY_AVAILABILITY, 0), target.optional(AirShape.IDS_UNCERTAINTY_ID, 0),
                coverage(at.child("coverage")), typedId(at.child("origin"), AirShape.IDS_ORIGIN_ID));
    }

    private AirJsonException invalid(At at, String rule, String detail) {
        return new AirJsonException(AirJsonException.Code.INVALID_IR, at.path(), detail,
                List.of(new io.github.gustavo2358.air.validation.ValidationIssue(
                        io.github.gustavo2358.air.validation.ValidationIssue.Kind.INVALID_IR, rule,
                        java.util.Optional.empty(), detail)));
    }

    private long entry(At at) {
        at.fields("id", "initialLabel", "signature", "state", "origin");
        var state = at.child("state").fields("conditions", "uncertainties");
        long entryState = target.record(AirShape.ENTRIES_ENTRY_STATE,
                empty(state.child("conditions"), AirShape.ENTRIES_INITIAL_CONDITION),
                empty(state.child("uncertainties"), AirShape.IDS_UNCERTAINTY_ID));
        return target.record(AirShape.ENTRIES_ENTRY, typedId(at.child("id"), AirShape.IDS_ENTRY_ID),
                optional(at.child("initialLabel"), AirShape.IDS_LABEL_ID, value -> typedId(value, AirShape.IDS_LABEL_ID)),
                signature(at.child("signature")), entryState, typedId(at.child("origin"), AirShape.IDS_ORIGIN_ID));
    }

    private long signature(At at) {
        at.fields("parameters", "results", "origin");
        var parameters = at.child("parameters").fields("known", "remainder");
        var results = at.child("results").fields("known", "remainder");
        long parameterInventory = target.record(AirShape.INTERACTIONS_PARAMETER_INVENTORY,
                empty(parameters.child("known"), AirShape.INTERACTIONS_PARAMETER), remainder(parameters.child("remainder")));
        long resultInventory = target.record(AirShape.INTERACTIONS_RESULT_INVENTORY,
                empty(results.child("known"), AirShape.INTERACTIONS_RESULT_SLOT), remainder(results.child("remainder")));
        return target.record(AirShape.INTERACTIONS_SIGNATURE, parameterInventory, resultInventory,
                typedId(at.child("origin"), AirShape.IDS_ORIGIN_ID));
    }

    private long remainder(At at) {
        if (!at.child("kind").text().equals("none"))
            throw Json.limit(at.path(), "Unknown signature remainder outside direct snapshot binding coverage");
        at.fields("kind");
        return target.scalar(AirShape.INTERACTIONS_NO_REMAINDER, 0);
    }

    private long sequence(At at) {
        at.fields("label", "instructions", "terminator", "origin");
        return target.record(AirShape.SEQUENCE, typedId(at.child("label"), AirShape.IDS_LABEL_ID),
                empty(at.child("instructions"), AirShape.INSTRUCTION), operation(at.child("terminator")),
                typedId(at.child("origin"), AirShape.IDS_ORIGIN_ID));
    }

    private long operation(At at) {
        if (!at.child("kind").text().equals("return"))
            throw Json.limit(at.path(), "Non-return operation outside current direct snapshot binding coverage");
        at.fields("kind", "header", "values");
        return target.record(AirShape.OPERATIONS_RETURN, header(at.child("header")),
                empty(at.child("values"), AirShape.EXPRESSION));
    }

    private long header(At at) {
        at.fields("id", "origin", "coverage", "precision", "uncertainties");
        return target.record(AirShape.OPERATIONS_HEADER, typedId(at.child("id"), AirShape.IDS_OPERATION_ID),
                typedId(at.child("origin"), AirShape.IDS_ORIGIN_ID),
                enumValue(at.child("coverage"), AirShape.EVIDENCE_COVERAGE_STATUS), precision(at.child("precision")),
                list(at.child("uncertainties"), AirShape.IDS_UNCERTAINTY_ID,
                        value -> typedId(value, AirShape.IDS_UNCERTAINTY_ID)));
    }

    private long precision(At at) {
        at.fields("control", "storage", "effects", "values", "dependencies");
        return target.record(AirShape.EVIDENCE_PRECISION, claim(at.child("control")), claim(at.child("storage")),
                claim(at.child("effects")), claim(at.child("values")), claim(at.child("dependencies")));
    }

    private long claim(At at) {
        at.fields("scope", "status", "reasons");
        return target.record(AirShape.EVIDENCE_CLAIM, scope(at.child("scope")),
                enumValue(at.child("status"), AirShape.EVIDENCE_PRECISION_STATUS),
                list(at.child("reasons"), AirShape.IDS_UNCERTAINTY_ID,
                        value -> typedId(value, AirShape.IDS_UNCERTAINTY_ID)));
    }

    private long coverage(At at) {
        at.fields("inventory", "scope", "items", "uncertainties");
        long inventory = enumValue(at.child("inventory"), AirShape.EVIDENCE_INVENTORY_STATUS);
        long scope = scope(at.child("scope"));
        long items = list(at.child("items"), AirShape.EVIDENCE_COVERAGE_ITEM, this::coverageItem);
        long uncertainties = list(at.child("uncertainties"), AirShape.IDS_UNCERTAINTY_ID,
                value -> typedId(value, AirShape.IDS_UNCERTAINTY_ID));
        return target.record(AirShape.EVIDENCE_COVERAGE, inventory, scope, items, uncertainties);
    }

    private long coverageItem(At at) {
        at.fields("sourceKey", "origin", "status", "outputs", "uncertainties", "elimination");
        if (!at.child("elimination").nil()) throw Json.limit(at.child("elimination").path(), "Elimination outside direct snapshot binding coverage");
        return target.record(AirShape.EVIDENCE_COVERAGE_ITEM, text(at.child("sourceKey").modelText()),
                typedId(at.child("origin"), AirShape.IDS_ORIGIN_ID),
                enumValue(at.child("status"), AirShape.EVIDENCE_COVERAGE_STATUS),
                list(at.child("outputs"), AirShape.IDS_ID, this::id),
                list(at.child("uncertainties"), AirShape.IDS_UNCERTAINTY_ID,
                        value -> typedId(value, AirShape.IDS_UNCERTAINTY_ID)),
                target.optional(AirShape.EVIDENCE_ELIMINATION, 0));
    }

    private long scope(At at) {
        return switch (at.child("kind").text()) {
            case "publication" -> {
                at.fields("kind", "publication");
                yield target.record(AirShape.SCOPES_PUBLICATION_SCOPE, publicationId(at.child("publication")));
            }
            case "unit" -> {
                at.fields("kind", "unit");
                yield target.record(AirShape.SCOPES_UNIT_SCOPE, typedId(at.child("unit"), AirShape.IDS_UNIT_ID));
            }
            case "entities" -> {
                at.fields("kind", "entities");
                if (at.child("entities").array().isEmpty())
                    throw Json.limit(at.child("entities").path(), "air-java representability limit: EntityScope requires nonempty entities; binding admits Id[]");
                yield target.record(AirShape.SCOPES_ENTITY_SCOPE,
                        list(at.child("entities"), AirShape.IDS_ID, this::id));
            }
            default -> throw Json.input(at.path(), "Unknown FactScope kind");
        };
    }

    private long uncertainty(At at) {
        at.fields("id", "code", "dimensions", "scope", "reason", "origin");
        if (at.child("dimensions").array().isEmpty())
            throw invalid(at.child("dimensions"), "AIR-06 §4", "Uncertainty must identify its affected domain");
        return target.record(AirShape.EVIDENCE_UNCERTAINTY,
                typedId(at.child("id"), AirShape.IDS_UNCERTAINTY_ID), text(at.child("code").modelText()),
                list(at.child("dimensions"), AirShape.EVIDENCE_DIMENSION,
                        value -> enumValue(value, AirShape.EVIDENCE_DIMENSION)), scope(at.child("scope")),
                text(at.child("reason").modelText()), typedId(at.child("origin"), AirShape.IDS_ORIGIN_ID));
    }

    private long origin(At at) {
        return switch (at.child("kind").text()) {
            case "written" -> {
                at.fields("kind", "id", "artifact", "location", "includes", "exact");
                yield target.record(AirShape.ORIGINS_WRITTEN, typedId(at.child("id"), AirShape.IDS_ORIGIN_ID),
                        typedId(at.child("artifact"), AirShape.IDS_ARTIFACT_ID),
                        optional(at.child("location"), AirShape.ORIGINS_LOCATION, this::location),
                        empty(at.child("includes"), AirShape.ORIGINS_INCLUDE_FRAME),
                        target.scalar(AirShape.BOOLEAN, at.child("exact").bool() ? 1 : 0));
            }
            case "derived" -> {
                at.fields("kind", "id", "inputs", "rule");
                if (at.child("inputs").array().isEmpty())
                    throw invalid(at.child("inputs"), "I-36", "AIR 06 §5: derived origin requires one or more inputs");
                yield target.record(AirShape.ORIGINS_DERIVED, typedId(at.child("id"), AirShape.IDS_ORIGIN_ID),
                        list(at.child("inputs"), AirShape.IDS_ORIGIN_ID,
                                value -> typedId(value, AirShape.IDS_ORIGIN_ID)), text(at.child("rule").modelText()));
            }
            case "contractual", "unavailable" -> throw Json.limit(at.path(), "Origin variant outside direct snapshot binding coverage");
            default -> throw Json.input(at.path(), "Unknown Origin kind");
        };
    }

    private long location(At at) {
        if (!at.child("kind").text().equals("line_columns"))
            throw Json.limit(at.path(), "Location.offsets outside direct snapshot binding coverage");
        at.fields("kind", "span");
        var span = at.child("span").fields("start", "end", "lineBase", "columnBase", "columnUnit", "endExclusive");
        BigInteger lineBase = naturalValue(span.child("lineBase"));
        BigInteger columnBase = naturalValue(span.child("columnBase"));
        var start = position(span.child("start"));
        var end = position(span.child("end"));
        if (lineBase.compareTo(BigInteger.ONE) > 0 || columnBase.compareTo(BigInteger.ONE) > 0)
            throw Json.limit(span.path(), "air-java representability limit: Span only supports bases 0 or 1; binding admits Natural");
        if (start.line.compareTo(lineBase) < 0 || end.line.compareTo(lineBase) < 0
                || start.column.compareTo(columnBase) < 0 || end.column.compareTo(columnBase) < 0
                || start.line.compareTo(end.line) > 0
                || start.line.equals(end.line) && start.column.compareTo(end.column) > 0)
            throw Json.limit(span.path(), "air-java representability limit: Physical Span coordinates violate model ordering");
        long spanNode = target.record(AirShape.ORIGINS_SPAN, start.node, end.node,
                integer(span.child("lineBase"), true), integer(span.child("columnBase"), true),
                enumValue(span.child("columnUnit"), AirShape.ORIGINS_COLUMN_UNIT),
                target.scalar(AirShape.BOOLEAN, span.child("endExclusive").bool() ? 1 : 0));
        return target.record(AirShape.ORIGINS_LINE_COLUMNS, spanNode);
    }

    private record Position(long node, BigInteger line, BigInteger column) { }
    private Position position(At at) {
        at.fields("line", "column");
        BigInteger line = naturalValue(at.child("line"));
        BigInteger column = naturalValue(at.child("column"));
        return new Position(target.record(AirShape.ORIGINS_POSITION,
                integer(at.child("line"), true), integer(at.child("column"), true)), line, column);
    }

    private long publicationId(At at) { return typedId(at, AirShape.IDS_PUBLICATION_ID); }

    private long typedId(At at, AirShape expected) {
        long id = id(at);
        if (idShape(at) != expected)
            throw invalid(at, "I-02", "AIR 01 §4: ID domain does not match reference role");
        return id;
    }

    private AirShape idShape(At at) {
        return switch (at.child("domain").text()) {
            case "publication" -> AirShape.IDS_PUBLICATION_ID;
            case "artifact" -> AirShape.IDS_ARTIFACT_ID;
            case "relation" -> AirShape.IDS_ARTIFACT_RELATION_ID;
            case "unit" -> AirShape.IDS_UNIT_ID;
            case "storage" -> AirShape.IDS_STORAGE_ID;
            case "resource" -> AirShape.IDS_RESOURCE_ID;
            case "origin" -> AirShape.IDS_ORIGIN_ID;
            case "uncertainty" -> AirShape.IDS_UNCERTAINTY_ID;
            case "premise" -> AirShape.IDS_PREMISE_ID;
            case "entry" -> AirShape.IDS_ENTRY_ID;
            case "label" -> AirShape.IDS_LABEL_ID;
            case "operation" -> AirShape.IDS_OPERATION_ID;
            case "object" -> AirShape.IDS_OBJECT_ID;
            case "completion_port" -> AirShape.IDS_COMPLETION_PORT_ID;
            case "operand" -> AirShape.IDS_OPERAND_ID;
            default -> throw Json.input(at.path(), "Unknown ID domain");
        };
    }

    private long id(At at) {
        AirShape shape = idShape(at);
        if (shape == AirShape.IDS_PUBLICATION_ID) {
            at.fields("domain", "localId");
            return target.record(shape, text(at.child("localId").modelText()));
        }
        if (shape == AirShape.IDS_OPERAND_ID)
            throw Json.limit(at.path(), "OperandId outside current direct snapshot binding coverage");
        boolean owned = shape == AirShape.IDS_ENTRY_ID || shape == AirShape.IDS_LABEL_ID
                || shape == AirShape.IDS_OPERATION_ID || shape == AirShape.IDS_OBJECT_ID
                || shape == AirShape.IDS_COMPLETION_PORT_ID;
        if (owned) at.fields("domain", "publication", "unit", "localId");
        else at.fields("domain", "publication", "localId");
        long publication = target.record(AirShape.IDS_PUBLICATION_ID, text(at.child("publication").modelText()));
        long owner = publication;
        if (owned) owner = target.record(AirShape.IDS_UNIT_ID, publication, text(at.child("unit").modelText()));
        return target.record(shape, owner, text(at.child("localId").modelText()));
    }

    private long empty(At at, AirShape element) {
        at.emptyInventory();
        try (var output = target.list(element)) { return output.finish(); }
    }

    private long list(At at, AirShape element, ToLongFunction<At> mapping) {
        var values = at.array();
        try (var output = target.list(element)) {
            for (int index = 0; index < values.size(); index++) output.add(mapping.applyAsLong(at.element(values, index)));
            return output.finish();
        }
    }

    private long optional(At at, AirShape element, ToLongFunction<At> mapping) {
        return target.optional(element, at.nil() ? 0 : mapping.applyAsLong(at));
    }

    private long enumValue(At at, AirShape shape) {
        String value = at.text();
        for (int ordinal = 0; ordinal < shape.enumCount(); ordinal++)
            if (shape.enumName(ordinal).equals(value)) return target.scalar(shape, ordinal);
        throw Json.input(at.path(), "Unknown " + shape.modelName() + " token");
    }

    private long integer(String value) {
        try (var output = target.text(AirShape.INTEGER)) {
            char[] characters = value.toCharArray();
            output.append(characters, 0, characters.length);
            return output.finish();
        }
    }

    private BigInteger naturalValue(At at) {
        String value = at.text();
        if (!value.matches("0|[1-9][0-9]*")) throw Json.input(at.path(), "Expected canonical Natural string");
        return new BigInteger(value);
    }

    private long integer(At at, boolean natural) {
        String value = at.text();
        if (natural ? !value.matches("0|[1-9][0-9]*") : !value.matches("0|-?[1-9][0-9]*"))
            throw Json.input(at.path(), natural ? "Expected canonical Natural string" : "Expected canonical Integer string");
        return integer(value);
    }

    private long text(String value) {
        try (var output = target.text(AirShape.TEXT)) {
            char[] characters = value.toCharArray();
            output.append(characters, 0, characters.length);
            return output.finish();
        }
    }
}
