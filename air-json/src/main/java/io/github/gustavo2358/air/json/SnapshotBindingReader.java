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

/** Direct wire-to-snapshot binding for the complete resident codec admission catalogue. */
final class SnapshotBindingReader {
    private final AirSnapshotBuilder target;
    private boolean resourceBindings;

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
        long storage = list(publication.child("storage"), AirShape.MEMORY_STORAGE, this::storage);
        long resources = list(publication.child("resources"), AirShape.INTERACTIONS_RESOURCE, this::resource);
        long relations = empty(publication.child("artifactRelations"), AirShape.ARTIFACTS_RELATION);
        long origins = list(publication.child("origins"), AirShape.ORIGINS_ORIGIN, this::origin);
        long coverage = coverage(publication.child("coverage"));
        long uncertainties = list(publication.child("uncertainties"), AirShape.EVIDENCE_UNCERTAINTY, this::uncertainty);
        long premises = list(publication.child("premises"), AirShape.PROOFS_PREMISE, this::premise);
        long root = target.record(AirShape.PUBLICATION, id, semanticVersion(), capabilities, artifacts, units,
                storage, resources, relations, origins, coverage, uncertainties, premises);
        return target.finish(root);
    }

    private long premise(At at) {
        at.fields("id", "authority", "justification", "origin", "assertion");
        var assertion = at.child("assertion");
        long content = switch (assertion.child("kind").text()) {
            case "disjoint_storage" -> {
                assertion.fields("kind", "storage");
                yield target.record(AirShape.PROOFS_DISJOINT_STORAGE,
                        list(assertion.child("storage"), AirShape.IDS_STORAGE_ID,
                                value -> typedId(value, AirShape.IDS_STORAGE_ID)));
            }
            case "same_domain" -> {
                assertion.fields("kind", "left", "right", "scope");
                throw Json.limit(assertion.path(), "Assertion.same_domain outside binding implementation coverage");
            }
            default -> throw Json.input(assertion.path(), "Unknown Assertion kind");
        };
        return target.record(AirShape.PROOFS_PREMISE,
                typedId(at.child("id"), AirShape.IDS_PREMISE_ID), text(at.child("authority").modelText()),
                text(at.child("justification").modelText()), typedId(at.child("origin"), AirShape.IDS_ORIGIN_ID),
                content);
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
                capabilities(at.child("required"), true), capabilities(at.child("provided"), false));
    }

    private long capabilities(At at, boolean required) {
        var values = at.array();
        try (var output = target.list(AirShape.CAPABILITIES_CAPABILITY)) {
            for (int index = 0; index < values.size(); index++) {
                var item = at.element(values, index).fields("name", "version");
                if (required && item.child("name").text().equals("resource.bindings")
                        && item.child("version").text().equals("1")) resourceBindings = true;
                output.add(target.record(AirShape.CAPABILITIES_CAPABILITY,
                        text(item.child("name").modelText()), text(item.child("version").modelText())));
            }
            return output.finish();
        }
    }

    private long resource(At at) {
        if (resourceBindings) at.fields("id", "description", "origin", "declaration");
        else at.fields("id", "description", "origin");
        return target.record(AirShape.INTERACTIONS_RESOURCE,
                typedId(at.child("id"), AirShape.IDS_RESOURCE_ID), resourceDescription(at.child("description")),
                typedId(at.child("origin"), AirShape.IDS_ORIGIN_ID),
                resourceBindings
                        ? optional(at.child("declaration"), AirShape.INTERACTIONS_RESOURCE_DECLARATION, this::resourceDeclaration)
                        : target.optional(AirShape.INTERACTIONS_RESOURCE_DECLARATION, 0));
    }

    private long resourceDescription(At at) {
        return switch (at.child("kind").text()) {
            case "literal" -> invocationTarget(at);
            case "internal" -> {
                at.fields("kind", "entry");
                yield target.record(AirShape.INTERACTIONS_INTERNAL_TARGET,
                        typedId(at.child("entry"), AirShape.IDS_ENTRY_ID));
            }
            case "computed" -> {
                at.fields("kind", "category", "namespace", "name", "namePolicy", "origin");
                yield target.record(AirShape.INTERACTIONS_COMPUTED_RESOURCE,
                        text(at.child("category").modelText()), text(at.child("namespace").modelText()),
                        typedId(at.child("name"), AirShape.IDS_OPERAND_ID), namePolicy(at.child("namePolicy")),
                        typedId(at.child("origin"), AirShape.IDS_ORIGIN_ID));
            }
            case "local" -> {
                at.fields("kind", "category");
                if (!resourceBindings) throw invalid(at, "I-43", "resource.bindings capability required");
                yield target.record(AirShape.INTERACTIONS_LOCAL_RESOURCE, text(at.child("category").modelText()));
            }
            case "unknown" -> {
                at.fields("kind", "category", "namespace", "uncertainty");
                if (!resourceBindings) throw invalid(at, "I-43", "resource.bindings capability required");
                yield target.record(AirShape.INTERACTIONS_UNKNOWN_RESOURCE,
                        text(at.child("category").modelText()), text(at.child("namespace").modelText()),
                        typedId(at.child("uncertainty"), AirShape.IDS_UNCERTAINTY_ID));
            }
            default -> throw Json.input(at.path(), "Unknown ResourceDescription kind");
        };
    }

    private long resourceDeclaration(At at) {
        at.fields("owner", "name", "classification", "nameSource", "objects", "uses");
        long objects = list(at.child("objects"), AirShape.INTERACTIONS_RESOURCE_OBJECT, item -> {
            item.fields("object", "role");
            return target.record(AirShape.INTERACTIONS_RESOURCE_OBJECT,
                    typedId(item.child("object"), AirShape.IDS_OBJECT_ID), text(item.child("role").modelText()));
        });
        long uses = list(at.child("uses"), AirShape.INTERACTIONS_RESOURCE_USE, item -> {
            item.fields("operation", "role", "origin");
            return target.record(AirShape.INTERACTIONS_RESOURCE_USE,
                    typedId(item.child("operation"), AirShape.IDS_OPERATION_ID),
                    text(item.child("role").modelText()), typedId(item.child("origin"), AirShape.IDS_ORIGIN_ID));
        });
        return target.record(AirShape.INTERACTIONS_RESOURCE_DECLARATION,
                typedId(at.child("owner"), AirShape.IDS_UNIT_ID), text(at.child("name").modelText()),
                text(at.child("classification").modelText()), text(at.child("nameSource").modelText()),
                objects, uses);
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
                list(at.child("objects"), AirShape.MEMORY_OBJECT_DECLARATION, this::objectDeclaration),
                list(at.child("visibleObjects"), AirShape.IDS_OBJECT_ID,
                        value -> typedId(value, AirShape.IDS_OBJECT_ID)), entries, sequences,
                list(at.child("completionPorts"), AirShape.ENTRIES_COMPLETION_PORT, this::completionPort),
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
                list(state.child("conditions"), AirShape.ENTRIES_INITIAL_CONDITION, this::initialCondition),
                list(state.child("uncertainties"), AirShape.IDS_UNCERTAINTY_ID,
                        value -> typedId(value, AirShape.IDS_UNCERTAINTY_ID)));
        return target.record(AirShape.ENTRIES_ENTRY, typedId(at.child("id"), AirShape.IDS_ENTRY_ID),
                optional(at.child("initialLabel"), AirShape.IDS_LABEL_ID, value -> typedId(value, AirShape.IDS_LABEL_ID)),
                signature(at.child("signature")), entryState, typedId(at.child("origin"), AirShape.IDS_ORIGIN_ID));
    }

    private long initialCondition(At at) {
        at.fields("place", "value", "origin", "premises");
        return target.record(AirShape.ENTRIES_INITIAL_CONDITION, place(at.child("place")),
                initialValue(at.child("value")), typedId(at.child("origin"), AirShape.IDS_ORIGIN_ID),
                list(at.child("premises"), AirShape.IDS_PREMISE_ID,
                        value -> typedId(value, AirShape.IDS_PREMISE_ID)));
    }

    private long initialValue(At at) {
        return switch (at.child("kind").text()) {
            case "literal" -> {
                at.fields("kind", "value");
                if (!at.child("value").child("kind").text().equals("literal"))
                    throw Json.input(at.child("value").path(), "Initial literal requires LiteralExpression");
                yield target.record(AirShape.ENTRIES_LITERAL_INITIAL, expression(at.child("value")));
            }
            case "possible_literals" -> {
                at.fields("kind", "candidates", "remainder");
                if (at.child("candidates").array().isEmpty())
                    throw Json.input(at.child("candidates").path(), "Entry candidates must not be empty");
                long candidates = list(at.child("candidates"), AirShape.EXPRESSIONS_LITERAL, value -> {
                    if (!value.child("kind").text().equals("literal"))
                        throw Json.input(value.path(), "Entry candidate requires LiteralExpression");
                    return expression(value);
                });
                yield target.record(AirShape.ENTRIES_POSSIBLE_LITERALS, candidates,
                        typedId(at.child("remainder"), AirShape.IDS_UNCERTAINTY_ID));
            }
            case "parameter" -> {
                at.fields("kind", "position");
                yield target.record(AirShape.ENTRIES_PARAMETER_INITIAL, integer(at.child("position"), true));
            }
            case "preserve" -> { at.fields("kind"); yield target.scalar(AirShape.ENTRIES_PRESERVE, 0); }
            case "external_unknown" -> {
                at.fields("kind", "reason");
                yield target.record(AirShape.ENTRIES_EXTERNAL_UNKNOWN,
                        typedId(at.child("reason"), AirShape.IDS_UNCERTAINTY_ID));
            }
            case "uninitialized" -> {
                at.fields("kind", "reason");
                yield target.record(AirShape.ENTRIES_UNINITIALIZED,
                        typedId(at.child("reason"), AirShape.IDS_UNCERTAINTY_ID));
            }
            default -> throw Json.input(at.path(), "Unknown InitialValue kind");
        };
    }

    private long completionPort(At at) {
        at.fields("id", "origin");
        return target.record(AirShape.ENTRIES_COMPLETION_PORT,
                typedId(at.child("id"), AirShape.IDS_COMPLETION_PORT_ID),
                typedId(at.child("origin"), AirShape.IDS_ORIGIN_ID));
    }

    private long signature(At at) {
        at.fields("parameters", "results", "origin");
        var parameters = at.child("parameters").fields("known", "remainder");
        var results = at.child("results").fields("known", "remainder");
        long parameterInventory = target.record(AirShape.INTERACTIONS_PARAMETER_INVENTORY,
                list(parameters.child("known"), AirShape.INTERACTIONS_PARAMETER, this::parameter), remainder(parameters.child("remainder")));
        long resultInventory = target.record(AirShape.INTERACTIONS_RESULT_INVENTORY,
                empty(results.child("known"), AirShape.INTERACTIONS_RESULT_SLOT), remainder(results.child("remainder")));
        return target.record(AirShape.INTERACTIONS_SIGNATURE, parameterInventory, resultInventory,
                typedId(at.child("origin"), AirShape.IDS_ORIGIN_ID));
    }

    private long parameter(At at) {
        at.fields("position", "mode", "typeRef", "objectBinding", "origin");
        var mode = at.child("mode");
        if (mode.child("kind").text().equals("unknown")) {
            mode.fields("kind", "uncertainty");
            throw Json.limit(mode.path(), "Unknown parameter mode outside binding implementation coverage");
        }
        if (!mode.child("kind").text().equals("known")) throw Json.input(mode.path(), "Unknown ModeKnowledge kind");
        mode.fields("kind", "mode");
        var binding = at.child("objectBinding");
        if (!binding.child("kind").text().equals("external")) {
            if (binding.child("kind").text().equals("object")) binding.fields("kind", "object");
            else if (binding.child("kind").text().equals("unknown")) binding.fields("kind", "uncertainty");
            else throw Json.input(binding.path(), "Unknown ParameterBinding kind");
            throw Json.limit(binding.path(), "Entry parameter binding outside binding implementation coverage");
        }
        binding.fields("kind");
        return target.record(AirShape.INTERACTIONS_PARAMETER, integer(at.child("position"), true),
                target.record(AirShape.INTERACTIONS_KNOWN_MODE,
                        enumValue(mode.child("mode"), AirShape.INTERACTIONS_PASSING_MODE)),
                typeRef(at.child("typeRef")), target.scalar(AirShape.INTERACTIONS_EXTERNAL_BINDING, 0),
                typedId(at.child("origin"), AirShape.IDS_ORIGIN_ID));
    }

    private long remainder(At at) {
        return switch (at.child("kind").text()) {
            case "none" -> { at.fields("kind"); yield target.scalar(AirShape.INTERACTIONS_NO_REMAINDER, 0); }
            case "unknown" -> {
                at.fields("kind", "uncertainty");
                yield target.record(AirShape.INTERACTIONS_UNKNOWN_REMAINDER,
                        typedId(at.child("uncertainty"), AirShape.IDS_UNCERTAINTY_ID));
            }
            default -> throw Json.input(at.path(), "Unknown UnknownBound kind");
        };
    }

    private long sequence(At at) {
        at.fields("label", "instructions", "terminator", "origin");
        return target.record(AirShape.SEQUENCE, typedId(at.child("label"), AirShape.IDS_LABEL_ID),
                list(at.child("instructions"), AirShape.INSTRUCTION, this::instruction), operation(at.child("terminator")),
                typedId(at.child("origin"), AirShape.IDS_ORIGIN_ID));
    }

    private long instruction(At at) {
        String kind = operationFields(at);
        if (!Set.of("assign", "havoc.must", "havoc.may", "nop", "copy_bytes").contains(kind))
            throw invalid(at, "I-04", "AIR 01 §3: terminator in instructions");
        return switch (kind) {
            case "assign" -> {
                at.fields("kind", "header", "destination", "value");
                yield target.record(AirShape.OPERATIONS_ASSIGN, header(at.child("header")),
                        place(at.child("destination")), expression(at.child("value")));
            }
            case "copy_bytes" -> {
                at.fields("kind", "header", "destination", "source", "length", "fallback");
                yield target.record(AirShape.OPERATIONS_COPY_BYTES, header(at.child("header")),
                        byteRange(at.child("destination")), byteRange(at.child("source")),
                        integer(at.child("length"), true), conservativeEnvelope(at.child("fallback")));
            }
            case "havoc.must" -> {
                at.fields("kind", "header", "destination", "reason");
                yield target.record(AirShape.OPERATIONS_HAVOC_MUST, header(at.child("header")),
                        place(at.child("destination")), typedId(at.child("reason"), AirShape.IDS_UNCERTAINTY_ID));
            }
            case "havoc.may" -> {
                at.fields("kind", "header", "scope", "reason");
                yield target.record(AirShape.OPERATIONS_HAVOC_MAY, header(at.child("header")),
                        memoryScope(at.child("scope")), typedId(at.child("reason"), AirShape.IDS_UNCERTAINTY_ID));
            }
            case "nop" -> {
                at.fields("kind", "header");
                yield target.record(AirShape.OPERATIONS_NOP, header(at.child("header")));
            }
            default -> throw new IllegalStateException("Instruction catalogue mismatch");
        };
    }

    private long operation(At at) {
        String kind = operationFields(at);
        if (Set.of("assign", "havoc.must", "havoc.may", "nop", "copy_bytes").contains(kind))
            throw invalid(at, "I-04", "AIR 01 §3: ordinary operation as terminator");
        return switch (kind) {
            case "halt" -> {
                at.fields("kind", "header", "haltKind");
                yield target.record(AirShape.OPERATIONS_HALT, header(at.child("header")),
                        enumValue(at.child("haltKind"), AirShape.OPERATIONS_HALT_KIND));
            }
            case "return" -> {
                at.fields("kind", "header", "values");
                yield target.record(AirShape.OPERATIONS_RETURN, header(at.child("header")),
                        empty(at.child("values"), AirShape.EXPRESSION));
            }
            case "jump" -> {
                at.fields("kind", "header", "destination");
                yield target.record(AirShape.OPERATIONS_JUMP, header(at.child("header")),
                        typedId(at.child("destination"), AirShape.IDS_LABEL_ID));
            }
            case "branch" -> {
                at.fields("kind", "header", "predicate", "trueDestination", "falseDestination");
                yield target.record(AirShape.OPERATIONS_BRANCH, header(at.child("header")),
                        expression(at.child("predicate")),
                        typedId(at.child("trueDestination"), AirShape.IDS_LABEL_ID),
                        typedId(at.child("falseDestination"), AirShape.IDS_LABEL_ID));
            }
            case "invoke" -> {
                at.fields("kind", "header", "action", "target", "arguments", "results", "signature",
                        "effectOperands", "effectBound", "outcomes", "contract");
                yield target.record(AirShape.OPERATIONS_INVOKE, header(at.child("header")),
                        text(at.child("action").modelText()), invocationTarget(at.child("target")),
                        list(at.child("arguments"), AirShape.INTERACTIONS_ARGUMENT, this::argument),
                        list(at.child("results"), AirShape.PLACE, this::place),
                        invocationSignature(at.child("signature")),
                        list(at.child("effectOperands"), AirShape.PLACE, this::place),
                        effects(at.child("effectBound")), outcomes(at.child("outcomes")),
                        contract(at.child("contract")));
            }
            case "opaque" -> {
                at.fields("kind", "header", "observedKind", "knownOperands", "valueResults", "envelope");
                yield target.record(AirShape.OPERATIONS_OPAQUE, header(at.child("header")),
                        text(at.child("observedKind").modelText()),
                        list(at.child("knownOperands"), AirShape.OPERAND, value -> {
                            String operandKind = value.child("kind").text();
                            return Set.of("object", "region_slice", "choice").contains(operandKind)
                                    ? place(value) : expression(value);
                        }),
                        list(at.child("valueResults"), AirShape.IDS_OPERAND_ID,
                                value -> typedId(value, AirShape.IDS_OPERAND_ID)),
                        conservativeEnvelope(at.child("envelope")));
            }
            case "local.invoke" -> {
                boolean guard = at.object().containsKey("reentryGuard");
                boolean routes = at.object().containsKey("resumeRoutes");
                at.fields(("kind,header,entry,completionPorts,resume,fallback" + (guard ? ",reentryGuard" : "")
                        + (routes ? ",resumeRoutes" : "")).split(","));
                yield target.record(AirShape.OPERATIONS_LOCAL_INVOKE, header(at.child("header")),
                        typedId(at.child("entry"), AirShape.IDS_LABEL_ID),
                        list(at.child("completionPorts"), AirShape.IDS_COMPLETION_PORT_ID,
                                value -> typedId(value, AirShape.IDS_COMPLETION_PORT_ID)),
                        typedId(at.child("resume"), AirShape.IDS_LABEL_ID), conservativeEnvelope(at.child("fallback")),
                        target.optional(AirShape.OPERATIONS_REENTRY_GUARD, guard ? reentryGuard(at.child("reentryGuard")) : 0),
                        routes ? list(at.child("resumeRoutes"), AirShape.OPERATIONS_RESUME_ROUTE, this::resumeRoute)
                                : emptyList(AirShape.OPERATIONS_RESUME_ROUTE));
            }
            case "local.boundary" -> {
                boolean key = at.object().containsKey("resumeKey");
                at.fields(("kind,header,port,defaultDestination,fallback" + (key ? ",resumeKey" : "")).split(","));
                yield target.record(AirShape.OPERATIONS_LOCAL_BOUNDARY, header(at.child("header")),
                        typedId(at.child("port"), AirShape.IDS_COMPLETION_PORT_ID),
                        typedId(at.child("defaultDestination"), AirShape.IDS_LABEL_ID),
                        conservativeEnvelope(at.child("fallback")), optionalText(at, "resumeKey", key));
            }
            case "local.resume" -> {
                boolean key = at.object().containsKey("resumeKey");
                at.fields(("kind,header,fallback" + (key ? ",resumeKey" : "")).split(","));
                yield target.record(AirShape.OPERATIONS_LOCAL_RESUME, header(at.child("header")),
                        conservativeEnvelope(at.child("fallback")), optionalText(at, "resumeKey", key));
            }
            case "local.unwind" -> {
                boolean all = at.object().containsKey("all");
                at.fields(("kind,header,count,destination,fallback" + (all ? ",all" : "")).split(","));
                yield target.record(AirShape.OPERATIONS_LOCAL_UNWIND, header(at.child("header")),
                        integer(at.child("count"), true), typedId(at.child("destination"), AirShape.IDS_LABEL_ID),
                        conservativeEnvelope(at.child("fallback")),
                        target.scalar(AirShape.BOOLEAN, all && at.child("all").bool() ? 1 : 0));
            }
            case "dispatch", "raise", "indirect.jump" ->
                    throw Json.limit(at.path(), "Operation " + kind + " outside binding implementation coverage");
            default -> throw new IllegalStateException("Terminator catalogue mismatch");
        };
    }

    private String operationFields(At at) {
        String kind = at.child("kind").text();
        String extra = switch (kind) {
            case "return", "raise" -> kind.equals("return") ? "values" : "tag,values";
            case "halt" -> "haltKind"; case "jump" -> "destination";
            case "assign" -> "destination,value"; case "havoc.must" -> "destination,reason";
            case "havoc.may" -> "scope,reason"; case "nop" -> "";
            case "branch" -> "predicate,trueDestination,falseDestination";
            case "dispatch" -> "selector,cases,defaultDestination";
            case "invoke" -> "action,target,arguments,results,signature,effectOperands,effectBound,outcomes,contract";
            case "opaque" -> "observedKind,knownOperands,valueResults,envelope";
            case "copy_bytes" -> "destination,source,length,fallback";
            case "local.invoke" -> "entry,completionPorts,resume,fallback"
                    + (at.object().containsKey("reentryGuard") ? ",reentryGuard" : "")
                    + (at.object().containsKey("resumeRoutes") ? ",resumeRoutes" : "");
            case "local.boundary" -> "port,defaultDestination,fallback"
                    + (at.object().containsKey("resumeKey") ? ",resumeKey" : "");
            case "local.resume" -> "fallback" + (at.object().containsKey("resumeKey") ? ",resumeKey" : "");
            case "local.unwind" -> "count,destination,fallback" + (at.object().containsKey("all") ? ",all" : "");
            case "indirect.jump" -> "target,within,fallback";
            default -> throw Json.input(at.path(), "Unknown Operation kind");
        };
        at.fields(("kind,header" + (extra.isEmpty() ? "" : "," + extra)).split(","));
        return kind;
    }

    private long argument(At at) {
        return switch (at.child("kind").text()) {
            case "value" -> { at.fields("kind", "value"); yield target.record(AirShape.INTERACTIONS_VALUE_ARGUMENT, expression(at.child("value"))); }
            case "copy" -> { at.fields("kind", "value"); yield target.record(AirShape.INTERACTIONS_COPY_ARGUMENT, expression(at.child("value"))); }
            case "reference" -> { at.fields("kind", "place"); yield target.record(AirShape.INTERACTIONS_REFERENCE_ARGUMENT, place(at.child("place"))); }
            default -> throw Json.input(at.path(), "Unknown Argument kind");
        };
    }

    private long invocationTarget(At at) {
        return switch (at.child("kind").text()) {
            case "literal" -> {
                at.fields("kind", "category", "namespace", "name", "namePolicy", "origin");
                yield target.record(AirShape.INTERACTIONS_LITERAL_TARGET,
                        text(at.child("category").modelText()), text(at.child("namespace").modelText()),
                        text(at.child("name").text()), namePolicy(at.child("namePolicy")),
                        typedId(at.child("origin"), AirShape.IDS_ORIGIN_ID));
            }
            case "computed" -> {
                at.fields("kind", "category", "namespace", "name", "namePolicy", "origin");
                yield target.record(AirShape.INTERACTIONS_COMPUTED_TARGET,
                        text(at.child("category").modelText()), text(at.child("namespace").modelText()),
                        expression(at.child("name")), namePolicy(at.child("namePolicy")),
                        typedId(at.child("origin"), AirShape.IDS_ORIGIN_ID));
            }
            case "internal" -> {
                at.fields("kind", "entry");
                throw Json.limit(at.path(), "Target.internal outside binding implementation coverage");
            }
            default -> throw Json.input(at.path(), "Unknown Target kind");
        };
    }

    private long namePolicy(At at) {
        return switch (at.child("kind").text()) {
            case "exact" -> { at.fields("kind"); yield target.scalar(AirShape.INTERACTIONS_EXACT_NAME, 0); }
            case "unknown" -> {
                at.fields("kind", "uncertainty");
                yield target.record(AirShape.INTERACTIONS_UNKNOWN_NAME,
                        typedId(at.child("uncertainty"), AirShape.IDS_UNCERTAINTY_ID));
            }
            case "extension" -> {
                at.fields("kind", "name", "version");
                yield target.record(AirShape.INTERACTIONS_EXTENSION_NAME,
                        text(at.child("name").text()), text(at.child("version").text()));
            }
            default -> throw Json.input(at.path(), "Unknown NamePolicy kind");
        };
    }

    private long invocationSignature(At at) {
        return switch (at.child("kind").text()) {
            case "external" -> {
                at.fields("kind", "signature");
                yield target.record(AirShape.INTERACTIONS_EXTERNAL_SIGNATURE, signature(at.child("signature")));
            }
            case "entry" -> {
                at.fields("kind", "entry");
                throw Json.limit(at.path(), "InvocationSignature.entry outside binding implementation coverage");
            }
            default -> throw Json.input(at.path(), "Unknown InvocationSignature kind");
        };
    }

    private long effects(At at) {
        at.fields("otherwise", "perOutcome");
        return target.record(AirShape.INTERACTIONS_EFFECT_BOUND, foreignEffects(at.child("otherwise")),
                list(at.child("perOutcome"), AirShape.INTERACTIONS_OUTCOME_EFFECTS, item -> {
                    item.fields("outcome", "effects");
                    return target.record(AirShape.INTERACTIONS_OUTCOME_EFFECTS,
                            outcomeKey(item.child("outcome")), foreignEffects(item.child("effects")));
                }));
    }

    private long foreignEffects(At at) {
        at.fields("reads", "writes", "mustOverwrite");
        return target.record(AirShape.INTERACTIONS_FOREIGN_EFFECTS, memoryBound(at.child("reads")),
                memoryBound(at.child("writes")),
                list(at.child("mustOverwrite"), AirShape.IDS_OPERAND_ID,
                        value -> typedId(value, AirShape.IDS_OPERAND_ID)));
    }

    private long outcomeKey(At at) {
        return switch (at.child("kind").text()) {
            case "normal" -> { at.fields("kind"); yield target.scalar(AirShape.CONTROL_NORMAL_OUTCOME, 0); }
            case "exception" -> {
                at.fields("kind", "tag");
                yield target.record(AirShape.CONTROL_EXCEPTION_OUTCOME, text(at.child("tag").modelText()));
            }
            case "other_exception" -> { at.fields("kind"); yield target.scalar(AirShape.CONTROL_OTHER_EXCEPTION_OUTCOME, 0); }
            case "halt" -> { at.fields("kind"); yield target.scalar(AirShape.CONTROL_HALT_OUTCOME, 0); }
            case "diverge" -> { at.fields("kind"); yield target.scalar(AirShape.CONTROL_DIVERGE_OUTCOME, 0); }
            default -> throw Json.input(at.path(), "Unknown OutcomeKey kind");
        };
    }

    private long outcomes(At at) {
        at.fields("known", "remainder");
        var knownValues = at.child("known").array();
        if (knownValues.isEmpty() && at.child("remainder").child("kind").text().equals("none"))
            throw invalid(at, "I-60", "AIR 05 §4: empty closed outcomes are not implicit divergence");
        return target.record(AirShape.CONTROL_INVOCATION_OUTCOMES,
                list(at.child("known"), AirShape.CONTROL_INVOCATION_ALTERNATIVE, this::alternative),
                controlBound(at.child("remainder")));
    }

    private long alternative(At at) {
        return switch (at.child("kind").text()) {
            case "normal" -> {
                at.fields("kind", "label");
                yield target.record(AirShape.CONTROL_NORMAL, typedId(at.child("label"), AirShape.IDS_LABEL_ID));
            }
            case "exception" -> {
                at.fields("kind", "tag", "destination");
                yield target.record(AirShape.CONTROL_EXCEPTIONAL, text(at.child("tag").modelText()),
                        exceptionDestination(at.child("destination")));
            }
            case "any_exception" -> {
                at.fields("kind", "destination");
                yield target.record(AirShape.CONTROL_ANY_EXCEPTION, exceptionDestination(at.child("destination")));
            }
            case "halt" -> { at.fields("kind"); yield target.scalar(AirShape.CONTROL_HALT_ALTERNATIVE, 0); }
            case "diverge" -> { at.fields("kind"); yield target.scalar(AirShape.CONTROL_DIVERGE, 0); }
            default -> throw Json.input(at.path(), "Unknown InvocationAlternative kind");
        };
    }

    private long exceptionDestination(At at) {
        return switch (at.child("kind").text()) {
            case "handler" -> {
                at.fields("kind", "label");
                yield target.record(AirShape.CONTROL_HANDLER, typedId(at.child("label"), AirShape.IDS_LABEL_ID));
            }
            case "propagate" -> { at.fields("kind"); yield target.scalar(AirShape.CONTROL_PROPAGATE, 0); }
            default -> throw Json.input(at.path(), "Unknown ExceptionDestination kind");
        };
    }

    private long contract(At at) {
        return switch (at.child("kind").text()) {
            case "known" -> {
                at.fields("kind", "reference");
                var reference = at.child("reference").fields("authority", "version", "evidence");
                if (reference.child("evidence").array().isEmpty())
                    throw invalid(reference.child("evidence"), "AIR-01 §9", "ContractRef requires nonempty evidence origins");
                long contract = target.record(AirShape.INTERACTIONS_CONTRACT_REF,
                        text(reference.child("authority").modelText()), text(reference.child("version").modelText()),
                        list(reference.child("evidence"), AirShape.IDS_ORIGIN_ID,
                                value -> typedId(value, AirShape.IDS_ORIGIN_ID)));
                yield target.record(AirShape.INTERACTIONS_KNOWN_CONTRACT, contract);
            }
            case "unknown" -> {
                at.fields("kind", "uncertainty");
                yield target.record(AirShape.INTERACTIONS_UNKNOWN_CONTRACT,
                        typedId(at.child("uncertainty"), AirShape.IDS_UNCERTAINTY_ID));
            }
            default -> throw Json.input(at.path(), "Unknown ContractKnowledge kind");
        };
    }

    private long reentryGuard(At at) {
        at.fields("activationKey", "destination");
        return target.record(AirShape.OPERATIONS_REENTRY_GUARD, nonblankText(at.child("activationKey")),
                typedId(at.child("destination"), AirShape.IDS_LABEL_ID));
    }

    private long resumeRoute(At at) {
        at.fields("key", "destination");
        return target.record(AirShape.OPERATIONS_RESUME_ROUTE, nonblankText(at.child("key")),
                typedId(at.child("destination"), AirShape.IDS_LABEL_ID));
    }

    private long optionalText(At parent, String name, boolean present) {
        return target.optional(AirShape.TEXT, present ? nonblankText(parent.child(name)) : 0);
    }

    private long nonblankText(At at) {
        String value = at.text();
        if (value.isBlank()) throw Json.input(at.path(), "Text key must not be blank");
        return text(value);
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

    private long objectDeclaration(At at) {
        at.fields("id", "displayName", "typeRef", "storage", "visibility", "origin", "coverage", "precision");
        return target.record(AirShape.MEMORY_OBJECT_DECLARATION,
                typedId(at.child("id"), AirShape.IDS_OBJECT_ID),
                optional(at.child("displayName"), AirShape.TEXT, value -> text(value.modelText())),
                typeRef(at.child("typeRef")), binding(at.child("storage")),
                enumValue(at.child("visibility"), AirShape.MEMORY_VISIBILITY),
                typedId(at.child("origin"), AirShape.IDS_ORIGIN_ID),
                enumValue(at.child("coverage"), AirShape.EVIDENCE_COVERAGE_STATUS), precision(at.child("precision")));
    }

    private long storage(At at) {
        return switch (at.child("kind").text()) {
            case "cell" -> {
                at.fields("kind", "header", "typeRef");
                yield target.record(AirShape.MEMORY_CELL, storageHeader(at.child("header")), typeRef(at.child("typeRef")));
            }
            case "region" -> {
                at.fields("kind", "header", "extent");
                var extent = at.child("extent");
                yield switch (extent.child("kind").text()) {
                    case "known" -> {
                        extent.fields("kind", "value");
                        yield target.record(AirShape.MEMORY_REGION, storageHeader(at.child("header")),
                                target.optional(AirShape.INTEGER, integer(extent.child("value"), true)),
                                target.optional(AirShape.IDS_UNCERTAINTY_ID, 0));
                    }
                    case "unknown" -> {
                        extent.fields("kind", "uncertainty");
                        yield target.record(AirShape.MEMORY_REGION, storageHeader(at.child("header")),
                                target.optional(AirShape.INTEGER, 0),
                                target.optional(AirShape.IDS_UNCERTAINTY_ID,
                                        typedId(extent.child("uncertainty"), AirShape.IDS_UNCERTAINTY_ID)));
                    }
                    default -> throw Json.input(extent.path(), "Unknown ExtentKnowledge kind");
                };
            }
            default -> throw Json.input(at.path(), "Unknown Storage kind");
        };
    }

    private long storageHeader(At at) {
        at.fields("id", "owner", "lifetime", "visibility", "origin");
        if (at.child("lifetime").text().equals("ACTIVATION") && at.child("owner").nil())
            throw invalid(at.child("owner"), "AIR-03 §2", "Activation storage requires its owning unit");
        return target.record(AirShape.MEMORY_STORAGE_HEADER,
                typedId(at.child("id"), AirShape.IDS_STORAGE_ID),
                optional(at.child("owner"), AirShape.IDS_UNIT_ID, value -> typedId(value, AirShape.IDS_UNIT_ID)),
                enumValue(at.child("lifetime"), AirShape.MEMORY_LIFETIME),
                enumValue(at.child("visibility"), AirShape.MEMORY_VISIBILITY),
                typedId(at.child("origin"), AirShape.IDS_ORIGIN_ID));
    }

    private long binding(At at) {
        return switch (at.child("kind").text()) {
            case "cell" -> {
                at.fields("kind", "storage");
                yield target.record(AirShape.MEMORY_CELL_BINDING, typedId(at.child("storage"), AirShape.IDS_STORAGE_ID));
            }
            case "view" -> {
                at.fields("kind", "region", "offset", "extent", "codec");
                yield target.record(AirShape.MEMORY_VIEW_BINDING,
                        typedId(at.child("region"), AirShape.IDS_STORAGE_ID), integer(at.child("offset"), true),
                        integer(at.child("extent"), true), codec(at.child("codec")));
            }
            case "alias" -> {
                at.fields("kind", "object");
                yield target.record(AirShape.MEMORY_ALIAS_BINDING, typedId(at.child("object"), AirShape.IDS_OBJECT_ID));
            }
            case "unknown" -> {
                at.fields("kind", "scope", "reason");
                yield target.record(AirShape.MEMORY_UNKNOWN_BINDING, memoryScope(at.child("scope")),
                        typedId(at.child("reason"), AirShape.IDS_UNCERTAINTY_ID));
            }
            default -> throw Json.limit(at.path(), "Object binding outside current direct snapshot binding coverage");
        };
    }

    private long typeRef(At at) {
        if (at.child("kind").text().equals("unknown_type")) {
            at.fields("kind", "uncertainty");
            return target.record(AirShape.TYPES_UNKNOWN_TYPE,
                    typedId(at.child("uncertainty"), AirShape.IDS_UNCERTAINTY_ID));
        }
        if (!at.child("kind").text().equals("known")) throw Json.input(at.path(), "Unknown TypeRef kind");
        at.fields("kind", "type");
        var type = at.child("type");
        String kind = type.child("kind").text();
        int ordinal = switch (kind) {
            case "bool" -> 0; case "int" -> 1; case "decimal" -> 2; case "text" -> 3; case "bytes" -> 4;
            default -> throw Json.limit(type.path(), "Non-builtin type outside current direct snapshot binding coverage");
        };
        type.fields("kind");
        return target.record(AirShape.TYPES_KNOWN, target.scalar(AirShape.TYPES_BUILTIN, ordinal));
    }

    private static final class PlaceFrame {
        final At at; final List<Json.Value> candidates; final List<Long> values = new ArrayList<>(); int next;
        PlaceFrame(At at) {
            this.at = at;
            candidates = at.child("kind").text().equals("choice")
                    ? at.fields("kind", "header", "candidates", "remainder", "typeRef").child("candidates").array()
                    : List.of();
        }
    }

    private long place(At root) {
        var stack = new ArrayDeque<PlaceFrame>(); stack.push(new PlaceFrame(root));
        while (!stack.isEmpty()) {
            var frame = stack.peek(); var at = frame.at;
            if (frame.next < frame.candidates.size()) {
                stack.push(new PlaceFrame(at.element(frame.candidates, frame.next++))); continue;
            }
            long result = switch (at.child("kind").text()) {
                case "object" -> {
                    at.fields("kind", "header", "object");
                    yield target.record(AirShape.PLACES_OBJECT_PLACE, operandHeader(at.child("header")),
                            typedId(at.child("object"), AirShape.IDS_OBJECT_ID));
                }
                case "region_slice" -> {
                    at.fields("kind", "header", "region", "offset", "length", "codec", "typeRef");
                    if (!at.child("offset").child("kind").text().equals("literal")
                            || !at.child("length").child("kind").text().equals("literal"))
                        throw Json.limit(at.path(), "Calculated physical bound outside binding implementation coverage");
                    yield target.record(AirShape.PLACES_REGION_SLICE, operandHeader(at.child("header")),
                            typedId(at.child("region"), AirShape.IDS_STORAGE_ID), expression(at.child("offset")),
                            expression(at.child("length")), codec(at.child("codec")), typeRef(at.child("typeRef")));
                }
                case "choice" -> target.record(AirShape.PLACES_CHOICE, operandHeader(at.child("header")),
                        longList(AirShape.PLACE, frame.values), memoryBound(at.child("remainder")),
                        typeRef(at.child("typeRef")));
                default -> throw Json.input(at.path(), "Unknown Place kind");
            };
            stack.pop();
            if (stack.isEmpty()) return result;
            stack.peek().values.add(result);
        }
        throw new IllegalStateException("Place frame invariant");
    }

    private static final class ExpressionFrame {
        final At at; final List<At> dependencies; final List<Long> values = new ArrayList<>(); int next;
        ExpressionFrame(At at) {
            this.at = at; String kind = at.child("kind").text();
            dependencies = switch (kind) {
                case "unknown" -> elements(at.fields("kind", "header", "typeRef", "dependencies", "remainingReads", "reason"), "dependencies");
                case "parse_integer" -> List.of(at.fields("kind", "header", "value", "onInvalid").child("value"), at.child("onInvalid"));
                case "format_decimal" -> List.of(at.fields("kind", "header", "value", "parts").child("value"));
                case "integer_digits" -> List.of(at.fields("kind", "header", "value", "digits").child("value"));
                case "wrap_integer" -> List.of(at.fields("kind", "header", "value", "width", "signed").child("value"));
                case "fit_decimal" -> List.of(at.fields("kind", "header", "value", "digits", "scale", "absolute").child("value"));
                case "fill_text" -> List.of(at.fields("kind", "header", "character", "length").child("character"));
                case "fit_text" -> List.of(at.fields("kind", "header", "value", "length", "pad").child("value"));
                case "slice_text" -> List.of(at.fields("kind", "header", "value", "start", "count").child("value"), at.child("start"), at.child("count"));
                case "unary" -> List.of(at.fields("kind", "header", "operator", "argument").child("argument"));
                case "binary" -> List.of(at.fields("kind", "header", "operator", "left", "right").child("left"), at.child("right"));
                default -> List.of();
            };
        }
        private static List<At> elements(At parent, String name) {
            var array = parent.child(name); var values = array.array(); var result = new ArrayList<At>(values.size());
            for (int index = 0; index < values.size(); index++) result.add(array.element(values, index));
            return result;
        }
    }

    private long expression(At root) {
        var stack = new ArrayDeque<ExpressionFrame>(); stack.push(new ExpressionFrame(root));
        while (!stack.isEmpty()) {
            var frame = stack.peek(); var at = frame.at;
            if (frame.next < frame.dependencies.size()) {
                stack.push(new ExpressionFrame(frame.dependencies.get(frame.next++))); continue;
            }
            long result = switch (at.child("kind").text()) {
                case "literal" -> {
                    at.fields("kind", "header", "value");
                    yield target.record(AirShape.EXPRESSIONS_LITERAL, operandHeader(at.child("header")), literalValue(at.child("value")));
                }
                case "read" -> {
                    at.fields("kind", "header", "place");
                    yield target.record(AirShape.EXPRESSIONS_READ, operandHeader(at.child("header")), place(at.child("place")));
                }
                case "unknown" -> target.record(AirShape.EXPRESSIONS_UNKNOWN, operandHeader(at.child("header")),
                        typeRef(at.child("typeRef")), longList(AirShape.EXPRESSION, frame.values),
                        memoryBound(at.child("remainingReads")), typedId(at.child("reason"), AirShape.IDS_UNCERTAINTY_ID));
                case "parse_integer" -> target.record(AirShape.EXPRESSIONS_PARSE_INTEGER,
                        operandHeader(at.child("header")), frame.values.get(0), frame.values.get(1));
                case "format_decimal" -> target.record(AirShape.EXPRESSIONS_FORMAT_DECIMAL,
                        operandHeader(at.child("header")), frame.values.getFirst(),
                        decimalParts(at.child("parts")));
                case "integer_digits" -> {
                    if (naturalValue(at.child("digits")).signum() == 0)
                        throw Json.input(at.child("digits").path(), "positive digit length required");
                    yield target.record(AirShape.EXPRESSIONS_INTEGER_DIGITS, operandHeader(at.child("header")),
                            frame.values.getFirst(), integer(at.child("digits"), true));
                }
                case "wrap_integer" -> {
                    if (naturalValue(at.child("width")).signum() == 0)
                        throw Json.input(at.child("width").path(), "positive bit width required");
                    yield target.record(AirShape.EXPRESSIONS_WRAP_INTEGER, operandHeader(at.child("header")),
                            frame.values.getFirst(), integer(at.child("width"), true), bool(at.child("signed")));
                }
                case "fit_decimal" -> {
                    if (naturalValue(at.child("digits")).signum() == 0)
                        throw Json.input(at.child("digits").path(), "positive decimal precision required");
                    yield target.record(AirShape.EXPRESSIONS_FIT_DECIMAL, operandHeader(at.child("header")),
                            frame.values.getFirst(), integer(at.child("digits"), true),
                            integer(at.child("scale"), false), bool(at.child("absolute")));
                }
                case "fill_text" -> target.record(AirShape.EXPRESSIONS_FILL_TEXT, operandHeader(at.child("header")),
                        frame.values.getFirst(), integer(at.child("length"), true));
                case "fit_text" -> {
                    String pad = at.child("pad").text();
                    if (pad.codePointCount(0, pad.length()) != 1)
                        throw Json.input(at.child("pad").path(), "fit pad requires exactly one scalar");
                    yield target.record(AirShape.EXPRESSIONS_FIT_TEXT, operandHeader(at.child("header")),
                            frame.values.getFirst(), integer(at.child("length"), true), text(pad));
                }
                case "slice_text" -> target.record(AirShape.EXPRESSIONS_SLICE_TEXT, operandHeader(at.child("header")),
                        frame.values.get(0), frame.values.get(1), frame.values.get(2));
                case "binary" -> target.record(AirShape.EXPRESSIONS_BINARY, operandHeader(at.child("header")),
                        binaryOperator(at.child("operator")), frame.values.get(0), frame.values.get(1));
                case "unary" -> target.record(AirShape.EXPRESSIONS_UNARY, operandHeader(at.child("header")),
                        unaryOperator(at.child("operator")), frame.values.getFirst());
                case "quantize", "trim_right" -> throw Json.limit(at.path(), "Expression " + at.child("kind").text() + " outside binding implementation coverage");
                default -> throw Json.input(at.path(), "Unknown Expression kind");
            };
            stack.pop();
            if (stack.isEmpty()) return result;
            stack.peek().values.add(result);
        }
        throw new IllegalStateException("Expression frame invariant");
    }

    private long decimalPart(At at) {
        at.fields("kind", "count", "text", "negative");
        BigInteger count = naturalValue(at.child("count")); String kind = at.child("kind").text();
        String value = at.child("text").text(), negative = at.child("negative").text();
        int size = value.codePointCount(0, value.length()), other = negative.codePointCount(0, negative.length());
        boolean valid = count.signum() > 0 && switch (kind) {
            case "DIGITS", "SUPPRESS_SPACE", "SUPPRESS_STAR" -> size == 0 && other == 0;
            case "INSERT" -> size > 0 && other == 0;
            case "RADIX" -> count.equals(BigInteger.ONE) && size == 1 && other == 0;
            case "SIGN" -> count.equals(BigInteger.ONE) && size > 0 && size == other;
            case "FLOAT_SIGN" -> count.equals(BigInteger.ONE) && size == 1 && other == 1;
            default -> throw Json.input(at.child("kind").path(), "unknown decimal format segment");
        };
        if (!valid) throw Json.input(at.path(), "incoherent decimal format segment");
        return target.record(AirShape.DECIMAL_TEXT_PART, enumValue(at.child("kind"), AirShape.DECIMAL_TEXT_KIND),
                integer(at.child("count"), true), text(value), text(negative));
    }

    private long decimalParts(At at) {
        var values = at.array(); BigInteger digits = BigInteger.ZERO; boolean point = false;
        boolean star = false, space = false, floating = false;
        try (var output = target.list(AirShape.DECIMAL_TEXT_PART)) {
            for (int index = 0; index < values.size(); index++) {
                var part = at.element(values, index); String kind = part.child("kind").text();
                BigInteger count = naturalValue(part.child("count"));
                boolean digit = kind.equals("DIGITS") || kind.equals("SUPPRESS_SPACE") || kind.equals("SUPPRESS_STAR");
                if (digit) digits = digits.add(count);
                if (kind.equals("RADIX")) { if (point) throw Json.input(part.path(), "duplicate decimal radix"); point = true; }
                if (kind.equals("FLOAT_SIGN")) { if (floating) throw Json.input(part.path(), "duplicate floating sign"); floating = true; }
                star |= kind.equals("SUPPRESS_STAR"); space |= kind.equals("SUPPRESS_SPACE");
                output.add(decimalPart(part));
            }
            if (digits.signum() <= 0 || star && (space || floating))
                throw Json.input(at.path(), "incoherent decimal suppression");
            return output.finish();
        }
    }

    private long binaryOperator(At at) {
        String name = switch (at.text()) {
            case "concat" -> "CONCAT"; case "eq" -> "EQ"; case "ne" -> "NE";
            case "and" -> "AND"; case "or" -> "OR"; case "lt" -> "LT"; case "le" -> "LE";
            case "gt" -> "GT"; case "ge" -> "GE"; case "mul" -> "MUL";
            case "add", "sub" -> throw Json.limit(at.path(), "binary operator " + at.text() + " outside binding implementation coverage");
            default -> throw Json.input(at.path(), "Unknown binary operator");
        };
        return enumNamed(AirShape.EXPRESSIONS_BINARY_OPERATOR, name);
    }

    private long unaryOperator(At at) {
        String name = switch (at.text()) {
            case "not" -> "NOT"; case "abs" -> "ABS"; case "to_decimal" -> "TO_DECIMAL";
            case "to_int" -> "TO_INT"; case "is_digits" -> "IS_DIGITS";
            case "neg", "length" -> throw Json.limit(at.path(), "unary operator " + at.text() + " outside binding implementation coverage");
            default -> throw Json.input(at.path(), "Unknown unary operator");
        };
        return enumNamed(AirShape.EXPRESSIONS_UNARY_OPERATOR, name);
    }

    private long literalValue(At at) {
        return switch (at.child("kind").text()) {
            case "text" -> {
                at.fields("kind", "value");
                yield target.record(AirShape.VALUES_TEXT_VALUE, text(at.child("value").text()));
            }
            case "int" -> {
                at.fields("kind", "value");
                yield target.record(AirShape.VALUES_INT_VALUE, integer(at.child("value"), false));
            }
            case "bytes" -> {
                at.fields("kind", "base64");
                String encoded = at.child("base64").text();
                if (!encoded.matches("(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?"))
                    throw Json.input(at.child("base64").path(), "Expected canonical padded base64");
                byte[] bytes = java.util.Base64.getDecoder().decode(encoded);
                if (!java.util.Base64.getEncoder().encodeToString(bytes).equals(encoded))
                    throw Json.input(at.child("base64").path(), "Nonzero base64 padding bits");
                try (var octets = target.list(AirShape.SMALL_INTEGER)) {
                    for (byte value : bytes) octets.add(target.scalar(AirShape.SMALL_INTEGER, value & 255));
                    yield target.record(AirShape.VALUES_BYTES_VALUE, octets.finish());
                }
            }
            case "decimal" -> {
                at.fields("kind", "coefficient", "scale");
                yield target.record(AirShape.VALUES_DECIMAL_VALUE,
                        integer(at.child("coefficient"), false), integer(at.child("scale"), true));
            }
            default -> throw Json.limit(at.path(), "Literal outside current direct snapshot binding coverage");
        };
    }

    private long operandHeader(At at) {
        at.fields("id", "role", "origin");
        return target.record(AirShape.OPERAND_HEADER, typedId(at.child("id"), AirShape.IDS_OPERAND_ID),
                enumValue(at.child("role"), AirShape.OPERAND_ROLE),
                typedId(at.child("origin"), AirShape.IDS_ORIGIN_ID));
    }

    private long codec(At at) {
        return switch (at.child("kind").text()) {
            case "bytes.identity" -> {
                at.fields("kind");
                yield target.scalar(AirShape.MEMORY_IDENTITY_BYTES, 0);
            }
            case "text.ascii" -> {
                at.fields("kind");
                yield target.scalar(AirShape.MEMORY_ASCII_TEXT, 0);
            }
            case "unsigned.binary", "signed.twos_complement" -> {
                at.fields("kind", "width", "order");
                BigInteger width = naturalValue(at.child("width"));
                if (width.signum() == 0 || width.mod(BigInteger.valueOf(8)).signum() != 0)
                    throw invalid(at.child("width"), "I-46", "Binary width must be positive and divisible by eight");
                yield target.record(AirShape.MEMORY_BINARY_CODEC,
                        target.scalar(AirShape.BOOLEAN,
                                at.child("kind").text().equals("signed.twos_complement") ? 1 : 0),
                        integer(at.child("width"), true), enumValue(at.child("order"), AirShape.MEMORY_BYTE_ORDER));
            }
            case "extension" -> {
                at.fields("kind", "name", "version", "logicalType");
                yield target.record(AirShape.MEMORY_EXTENSION_CODEC,
                        text(at.child("name").modelText()), text(at.child("version").modelText()),
                        typeRef(at.child("logicalType")));
            }
            case "unknown" -> {
                at.fields("kind", "logicalType", "reason");
                yield target.record(AirShape.MEMORY_UNKNOWN_CODEC, typeRef(at.child("logicalType")),
                        typedId(at.child("reason"), AirShape.IDS_UNCERTAINTY_ID));
            }
            default -> throw Json.limit(at.path(), "Codec outside current direct snapshot binding coverage");
        };
    }

    private long byteRange(At at) {
        at.fields("region", "offset", "extent");
        if (!at.child("offset").child("kind").text().equals("literal")
                || !at.child("extent").child("kind").text().equals("literal"))
            throw Json.limit(at.path(), "Calculated physical bound outside binding implementation coverage");
        return target.record(AirShape.MEMORY_BYTE_RANGE, typedId(at.child("region"), AirShape.IDS_STORAGE_ID),
                expression(at.child("offset")), expression(at.child("extent")));
    }

    private long conservativeEnvelope(At at) {
        at.fields("memory", "control", "dependencies");
        var memory = at.child("memory").fields("knownReads", "otherReads", "knownWrites", "otherWrites", "mustOverwrite");
        long memoryEnvelope = target.record(AirShape.ENVELOPES_MEMORY_ENVELOPE,
                list(memory.child("knownReads"), AirShape.IDS_OPERAND_ID,
                        value -> typedId(value, AirShape.IDS_OPERAND_ID)), memoryBound(memory.child("otherReads")),
                list(memory.child("knownWrites"), AirShape.IDS_OPERAND_ID,
                        value -> typedId(value, AirShape.IDS_OPERAND_ID)), memoryBound(memory.child("otherWrites")),
                list(memory.child("mustOverwrite"), AirShape.IDS_OPERAND_ID,
                        value -> typedId(value, AirShape.IDS_OPERAND_ID)));
        var control = at.child("control").fields("known", "remainder");
        long controlEnvelope = target.record(AirShape.CONTROL_CONTROL_ENVELOPE,
                list(control.child("known"), AirShape.CONTROL_CONTROL_ALTERNATIVE, this::controlAlternative),
                controlBound(control.child("remainder")));
        var dependencies = at.child("dependencies").fields("known", "remainder");
        long dependencyEnvelope = target.record(AirShape.ENVELOPES_DEPENDENCY_ENVELOPE,
                empty(dependencies.child("known"), AirShape.ENVELOPES_RESOURCE_USE),
                dependencyBound(dependencies.child("remainder")));
        return target.record(AirShape.ENVELOPES_ENVELOPE, memoryEnvelope, controlEnvelope, dependencyEnvelope);
    }

    private long controlAlternative(At at) {
        return switch (at.child("kind").text()) {
            case "continue" -> { at.fields("kind"); yield target.scalar(AirShape.CONTROL_CONTINUE_ALTERNATIVE, 0); }
            case "return" -> { at.fields("kind"); yield target.scalar(AirShape.CONTROL_RETURN_ALTERNATIVE, 0); }
            case "jump" -> {
                at.fields("kind", "label");
                yield target.record(AirShape.CONTROL_JUMP_ALTERNATIVE, typedId(at.child("label"), AirShape.IDS_LABEL_ID));
            }
            case "normal", "exception", "any_exception", "halt", "diverge" -> alternative(at);
            default -> throw Json.limit(at.path(), "Control alternative outside current direct snapshot binding coverage");
        };
    }

    private long memoryBound(At at) {
        return switch (at.child("kind").text()) {
            case "none" -> { at.fields("kind"); yield target.scalar(AirShape.SCOPES_NO_MEMORY, 0); }
            case "within" -> {
                at.fields("kind", "scope");
                yield target.record(AirShape.SCOPES_WITHIN_MEMORY, memoryScope(at.child("scope")));
            }
            default -> throw Json.input(at.path(), "Unknown MemoryBound kind");
        };
    }

    private static final class MemoryScopeFrame {
        final At at; final List<Json.Value> members; final List<Long> values = new ArrayList<>(); int next;
        MemoryScopeFrame(At at) {
            this.at = at;
            if (at.child("kind").text().equals("union")) {
                members = at.fields("kind", "members").child("members").array();
                if (members.isEmpty())
                    throw Json.limit(at.path(), "air-java representability limit: MemoryUnion requires nonempty members");
            } else members = List.of();
        }
    }

    private long memoryScope(At root) {
        var stack = new ArrayDeque<MemoryScopeFrame>(); stack.push(new MemoryScopeFrame(root));
        while (!stack.isEmpty()) {
            var frame = stack.peek();
            if (frame.next < frame.members.size()) {
                stack.push(new MemoryScopeFrame(frame.at.element(frame.members, frame.next++))); continue;
            }
            long result = frame.at.child("kind").text().equals("union")
                    ? target.record(AirShape.SCOPES_MEMORY_UNION,
                            longList(AirShape.SCOPES_MEMORY_SCOPE, frame.values))
                    : memoryScopeLeaf(frame.at);
            stack.pop();
            if (stack.isEmpty()) return result;
            stack.peek().values.add(result);
        }
        throw new IllegalStateException("MemoryScope frame invariant");
    }

    private long memoryScopeLeaf(At at) {
        return switch (at.child("kind").text()) {
            case "storage" -> {
                at.fields("kind", "storage");
                yield target.record(AirShape.SCOPES_STORAGE_MEMORY,
                        list(at.child("storage"), AirShape.IDS_STORAGE_ID,
                                value -> typedId(value, AirShape.IDS_STORAGE_ID)));
            }
            case "objects" -> {
                at.fields("kind", "objects");
                yield target.record(AirShape.SCOPES_OBJECTS_MEMORY,
                        list(at.child("objects"), AirShape.IDS_OBJECT_ID,
                                value -> typedId(value, AirShape.IDS_OBJECT_ID)));
            }
            case "visible" -> {
                at.fields("kind", "unit", "includingExternal");
                yield target.record(AirShape.SCOPES_VISIBLE_MEMORY, typedId(at.child("unit"), AirShape.IDS_UNIT_ID),
                        target.scalar(AirShape.BOOLEAN, at.child("includingExternal").bool() ? 1 : 0));
            }
            case "all" -> {
                at.fields("kind", "publication", "includingEnvironment");
                yield target.record(AirShape.SCOPES_ALL_MEMORY, publicationId(at.child("publication")),
                        target.scalar(AirShape.BOOLEAN, at.child("includingEnvironment").bool() ? 1 : 0));
            }
            default -> throw Json.input(at.path(), "Unknown MemoryScope kind");
        };
    }

    private long controlBound(At at) {
        return switch (at.child("kind").text()) {
            case "none" -> { at.fields("kind"); yield target.scalar(AirShape.SCOPES_NO_CONTROL, 0); }
            case "within" -> {
                at.fields("kind", "scope");
                yield target.record(AirShape.SCOPES_WITHIN_CONTROL, controlScope(at.child("scope")));
            }
            default -> throw Json.input(at.path(), "Unknown ControlBound kind");
        };
    }

    private long controlScope(At at) {
        return switch (at.child("kind").text()) {
            case "all" -> {
                at.fields("kind", "publication");
                yield target.record(AirShape.SCOPES_ALL_CONTROL, publicationId(at.child("publication")));
            }
            case "unit" -> {
                at.fields("kind", "unit", "labels", "normalExit", "exceptionalExit", "halt", "diverge", "externalControl");
                yield target.record(AirShape.SCOPES_UNIT_CONTROL, typedId(at.child("unit"), AirShape.IDS_UNIT_ID),
                        bool(at.child("labels")), bool(at.child("normalExit")), bool(at.child("exceptionalExit")),
                        bool(at.child("halt")), bool(at.child("diverge")), bool(at.child("externalControl")));
            }
            case "labels" -> {
                at.fields("kind", "labels");
                yield target.record(AirShape.SCOPES_LABELS_CONTROL,
                        list(at.child("labels"), AirShape.IDS_LABEL_ID,
                                value -> typedId(value, AirShape.IDS_LABEL_ID)));
            }
            case "union" -> {
                at.fields("kind", "members");
                yield target.record(AirShape.SCOPES_CONTROL_UNION,
                        list(at.child("members"), AirShape.SCOPES_CONTROL_SCOPE, this::controlScope));
            }
            default -> throw Json.limit(at.path(), "ControlScope outside current direct snapshot binding coverage");
        };
    }

    private long dependencyBound(At at) {
        return switch (at.child("kind").text()) {
            case "none" -> { at.fields("kind"); yield target.scalar(AirShape.SCOPES_NO_RESOURCES, 0); }
            case "any_resource" -> { at.fields("kind"); yield target.scalar(AirShape.SCOPES_ANY_RESOURCE, 0); }
            case "categories" -> {
                at.fields("kind", "categories");
                yield target.record(AirShape.SCOPES_RESOURCE_CATEGORIES,
                        list(at.child("categories"), AirShape.TEXT, value -> text(value.modelText())));
            }
            default -> throw Json.limit(at.path(), "DependencyBound outside current direct snapshot binding coverage");
        };
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
                        list(at.child("includes"), AirShape.ORIGINS_INCLUDE_FRAME, this::include),
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

    private long include(At at) {
        at.fields("including", "included", "requestedName", "site");
        return target.record(AirShape.ORIGINS_INCLUDE_FRAME,
                typedId(at.child("including"), AirShape.IDS_ARTIFACT_ID),
                typedId(at.child("included"), AirShape.IDS_ARTIFACT_ID),
                text(at.child("requestedName").modelText()),
                optional(at.child("site"), AirShape.ORIGINS_LOCATION, this::location));
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
        if (shape == AirShape.IDS_OPERAND_ID) {
            at.fields("domain", "publication", "unit", "owner", "localId");
            long publication = target.record(AirShape.IDS_PUBLICATION_ID, text(at.child("publication").modelText()));
            long unit = target.record(AirShape.IDS_UNIT_ID, publication, text(at.child("unit").modelText()));
            var owner = at.child("owner").fields("kind", "localId");
            long ownerId = switch (owner.child("kind").text()) {
                case "operation" -> target.record(AirShape.IDS_OPERATION_OWNER,
                        target.record(AirShape.IDS_OPERATION_ID, unit, text(owner.child("localId").modelText())));
                case "entry" -> target.record(AirShape.IDS_ENTRY_OWNER,
                        target.record(AirShape.IDS_ENTRY_ID, unit, text(owner.child("localId").modelText())));
                default -> throw Json.input(owner.path(), "Unknown Operand owner kind");
            };
            return target.record(AirShape.IDS_OPERAND_ID, ownerId, text(at.child("localId").modelText()));
        }
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
        return emptyList(element);
    }

    private long emptyList(AirShape element) {
        try (var output = target.list(element)) { return output.finish(); }
    }

    private long longList(AirShape element, List<Long> values) {
        try (var output = target.list(element)) {
            for (long value : values) output.add(value);
            return output.finish();
        }
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

    private long bool(At at) { return target.scalar(AirShape.BOOLEAN, at.bool() ? 1 : 0); }

    private long enumValue(At at, AirShape shape) {
        String value = at.text();
        return enumNamed(shape, value, at.path());
    }

    private long enumNamed(AirShape shape, String value) { return enumNamed(shape, value, "$"); }

    private long enumNamed(AirShape shape, String value, String path) {
        for (int ordinal = 0; ordinal < shape.enumCount(); ordinal++)
            if (shape.enumName(ordinal).equals(value)) return target.scalar(shape, ordinal);
        throw Json.input(path, "Unknown " + shape.modelName() + " token");
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
