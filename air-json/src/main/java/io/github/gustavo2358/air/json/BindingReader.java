package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Unit;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.validation.ValidationIssue;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import static io.github.gustavo2358.air.json.AirJsonException.Code.*;

/** Binding shape checks precede model construction; closure is a separate Validator step. */
final class BindingReader {
    private record At(Json.Value value, String path) {
        Json.Obj object() {
            if (value instanceof Json.Obj o) return o;
            throw Json.input(path, "Expected object");
        }
        At child(String key) {
            Json.Value child = object().fields().get(key);
            if (child == null) throw Json.input(path + "." + key, "Required field omitted");
            return new At(child, path + "." + key);
        }
        At fields(String... names) {
            Set<String> required = Set.of(names);
            for (String name : object().fields().keySet())
                if (!required.contains(name)) throw Json.input(path + "." + name, "Unknown field");
            for (String name : names) child(name);
            return this;
        }
        String text() {
            if (value instanceof Json.Text t) return t.value();
            throw Json.input(path, "Expected string");
        }
        boolean bool() {
            if (value instanceof Json.Bool b) return b.value();
            throw Json.input(path, "Expected boolean");
        }
        List<At> elements() {
            if (!(value instanceof Json.Arr a)) throw Json.input(path, "Expected array");
            var result = new ArrayList<At>();
            for (int i = 0; i < a.values().size(); i++) result.add(new At(a.values().get(i), path + "[" + i + "]"));
            return result;
        }
        <T> List<T> list(Function<At,T> mapper) { return elements().stream().map(mapper).toList(); }
        <T> Optional<T> optional(Function<At,T> mapper) {
            return value == Json.Nil.INSTANCE ? Optional.empty() : Optional.of(mapper.apply(this));
        }
        void empty() {
            if (!elements().isEmpty()) throw unsupported("Nonempty inventory");
        }
        String kind() { return child("kind").text(); }
        AirJsonException unsupported(String form) { return Json.limit(path, form + " outside implemented 1A/4B coverage"); }
        AirJsonException invalid(String rule, String detail) {
            return new AirJsonException(INVALID_IR, path, detail,
                    List.of(new ValidationIssue(ValidationIssue.Kind.INVALID_IR, rule, Optional.empty(), detail)));
        }
        AirJsonException representability(String restriction) {
            return Json.limit(path, "air-java representability limit: " + restriction);
        }
        AirJsonException spanRepresentability(String restriction) {
            return representability("Physical Span fields accepted; no pinned AIR invalidity rule identified; " + restriction);
        }
        /** Only for the audited Text fields backed by Require.text; never tokens or arbitrary strings. */
        String modelText() {
            String text = text();
            if (text.isBlank()) throw representability("Require.text rejects blank Text admitted by the pinned binding");
            return text;
        }
        <T> T construct(Supplier<T> constructor) {
            // Known AIR rules and Java representability gaps are checked explicitly at their sites.
            // Unexpected failures must retain their identity; they are not a codec classification.
            return constructor.get();
        }
    }
    Publication envelope(Json.Value value) {
        var e = new At(value, "$").fields("binding", "bindingVersion", "airVersion", "publication");
        version(e.child("binding"), "analysis-ir-json"); version(e.child("bindingVersion"), "1.0.0");
        version(e.child("airVersion"), "2.0.0");
        var p = e.child("publication").fields("id", "capabilities", "artifacts", "units", "storage", "resources",
                "artifactRelations", "origins", "coverage", "uncertainties", "premises");
        var manifest = manifest(p.child("capabilities"));
        var storage = p.child("storage").list(this::storage); p.child("resources").empty(); p.child("artifactRelations").empty(); p.child("premises").empty();
        var id = publicationId(p.child("id")); var artifacts = p.child("artifacts").list(this::artifact);
        var units = p.child("units").list(this::unit); var origins = p.child("origins").list(this::origin);
        var coverage = coverage(p.child("coverage")); var gaps = p.child("uncertainties").list(this::uncertainty);
        return new Publication(id, SemanticVersion.AIR_2_0_0, manifest, artifacts, units,
                storage, List.of(), List.of(), origins, coverage, gaps, List.of());
    }
    private void version(At at, String expected) {
        String actual = at.text();
        if (!expected.equals(actual)) throw new AirJsonException(VERSION_MISMATCH, at.path(), "Expected " + expected + ", received " + actual);
    }
    private Capabilities.Manifest manifest(At a) {
        a.fields("required", "provided");
        for (String name : List.of("required", "provided"))
            for (At c : a.child(name).elements()) { c.fields("name", "version"); c.child("name").text(); c.child("version").text(); }
        if (!a.child("required").elements().isEmpty() || !a.child("provided").elements().isEmpty())
            throw new AirJsonException(UNSUPPORTED_CAPABILITY, a.path(), "1A implements the empty capability manifest");
        return new Capabilities.Manifest(List.of(), List.of());
    }
    private Origins.Artifact artifact(At a) {
        a.fields("id", "logicalName", "contentDigest");
        var id = artifactId(a.child("id")); var name = a.child("logicalName").modelText(); var digest = a.child("contentDigest").optional(At::text);
        return a.construct(() -> new Origins.Artifact(id, name, digest));
    }
    private Unit unit(At a) {
        a.fields("id", "containingUnit", "objects", "visibleObjects", "entries", "sequences", "completionPorts", "body", "coverage", "origin");
        var body = a.child("body");
        switch (body.kind()) {
            case "available" -> body.fields("kind");
            case "unavailable" -> { body.fields("kind", "uncertainty"); throw body.unsupported("BodyKnowledge.unavailable"); }
            default -> throw Json.input(body.path(), "Unknown BodyKnowledge kind");
        }
        var objects = a.child("objects").list(this::objectDeclaration); a.child("visibleObjects").empty(); a.child("completionPorts").empty();
        var id = unitId(a.child("id")); var containing = a.child("containingUnit").optional(this::unitId);
        var entries = a.child("entries").list(this::entry); var sequences = a.child("sequences").list(this::sequence);
        var coverage = coverage(a.child("coverage")); var origin = originId(a.child("origin"));
        if (entries.isEmpty()) throw a.child("entries").invalid("AIR-01 §2", "Available body requires at least one entry");
        if (sequences.isEmpty()) throw a.child("sequences").invalid("AIR-01 §3", "Available body requires at least one sequence");
        return a.construct(() -> new Unit(id, containing, objects, List.of(), entries, sequences, List.of(),
                Unit.BodyAvailability.AVAILABLE, Optional.empty(), coverage, origin));
    }
    private Entries.Entry entry(At a) {
        a.fields("id", "initialLabel", "signature", "state", "origin");
        var s = a.child("state").fields("conditions", "uncertainties"); s.child("conditions").empty();
        return new Entries.Entry(entryId(a.child("id")), a.child("initialLabel").optional(this::labelId), signature(a.child("signature")),
                new Entries.EntryState(List.of(), s.child("uncertainties").list(this::uncertaintyId)), originId(a.child("origin")));
    }
    private Interactions.Signature signature(At a) {
        a.fields("parameters", "results", "origin");
        for (String name : List.of("parameters", "results")) {
            var inventory = a.child(name).fields("known", "remainder"); inventory.child("known").empty();
        }
        return new Interactions.Signature(new Interactions.ParameterInventory(List.of(), remainder(a.child("parameters").child("remainder"))),
                new Interactions.ResultInventory(List.of(), remainder(a.child("results").child("remainder"))), originId(a.child("origin")));
    }
    private Interactions.UnknownBound remainder(At a) {
        return switch (a.kind()) {
            case "none" -> { a.fields("kind"); yield Interactions.NoRemainder.INSTANCE; }
            case "unknown" -> {
                a.fields("kind", "uncertainty"); yield new Interactions.UnknownRemainder(uncertaintyId(a.child("uncertainty")));
            }
            default -> throw Json.input(a.path(), "Unknown UnknownBound kind");
        };
    }
    private Sequence sequence(At a) {
        a.fields("label", "instructions", "terminator", "origin");
        return new Sequence(labelId(a.child("label")), a.child("instructions").list(this::instruction),
                operation(a.child("terminator")), originId(a.child("origin")));
    }
    private Instruction instruction(At a) {
        String kind = operationFields(a);
        if (!Set.of("assign", "havoc.must", "havoc.may", "nop", "copy_bytes").contains(kind))
            throw a.invalid("I-04", "AIR 01 §3: terminator in instructions");
        if (!kind.equals("assign")) throw a.unsupported("Instruction " + kind);
        return new Operations.Assign(header(a.child("header")), place(a.child("destination")), expression(a.child("value")));
    }
    /** Recognize the binding's operation catalogue, never private Java kind names. */
    private String operationFields(At a) {
        String kind = a.kind();
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
            case "local.invoke" -> "entry,completionPorts,resume,fallback";
            case "local.boundary" -> "port,defaultDestination,fallback"; case "local.resume" -> "fallback";
            case "local.unwind" -> "count,destination,fallback"; case "indirect.jump" -> "target,within,fallback";
            default -> throw Json.input(a.path(), "Unknown Operation kind");
        };
        a.fields(("kind,header" + (extra.isEmpty() ? "" : "," + extra)).split(","));
        return kind;
    }
    private Terminator operation(At a) {
        String kind = operationFields(a);
        if (Set.of("assign", "havoc.must", "havoc.may", "nop", "copy_bytes").contains(kind))
            throw a.invalid("I-04", "AIR 01 §3: ordinary operation as terminator");
        if (kind.equals("invoke")) {
            a.child("arguments").empty(); a.child("results").empty();
            return new Operations.Invoke(header(a.child("header")), a.child("action").modelText(), target(a.child("target")),
                    List.of(), List.of(), invocationSignature(a.child("signature")), a.child("effectOperands").list(this::place),
                    effects(a.child("effectBound")), outcomes(a.child("outcomes")), contract(a.child("contract")));
        }
        if (!kind.equals("return")) throw a.unsupported("Operation " + kind);
        a.child("values").empty(); return new Operations.Return(header(a.child("header")), List.of());
    }
    private Interactions.Target target(At a) {
        switch (a.kind()) {
            case "literal", "computed" -> a.fields("kind", "category", "namespace", "name", "namePolicy", "origin");
            case "internal" -> { a.fields("kind", "entry"); throw a.unsupported("Target.internal"); }
            default -> throw Json.input(a.path(), "Unknown Target kind");
        }
        String category = a.child("category").modelText(), namespace = a.child("namespace").modelText();
        var policy = namePolicy(a.child("namePolicy")); var origin = originId(a.child("origin"));
        return a.kind().equals("literal")
                ? new Interactions.LiteralTarget(category, namespace, a.child("name").text(), policy, origin)
                : new Interactions.ComputedTarget(category, namespace, expression(a.child("name")), policy, origin);
    }
    private Interactions.NamePolicy namePolicy(At a) {
        return switch (a.kind()) {
            case "exact" -> { a.fields("kind"); yield Interactions.ExactName.INSTANCE; }
            case "unknown" -> {
                a.fields("kind", "uncertainty"); yield new Interactions.UnknownName(uncertaintyId(a.child("uncertainty")));
            }
            case "extension" -> { a.fields("kind", "name", "version"); throw a.unsupported("NamePolicy.extension"); }
            default -> throw Json.input(a.path(), "Unknown NamePolicy kind");
        };
    }
    private Interactions.InvocationSignature invocationSignature(At a) {
        return switch (a.kind()) {
            case "external" -> { a.fields("kind", "signature"); yield new Interactions.ExternalSignature(signature(a.child("signature"))); }
            case "entry" -> { a.fields("kind", "entry"); throw a.unsupported("InvocationSignature.entry"); }
            default -> throw Json.input(a.path(), "Unknown InvocationSignature kind");
        };
    }
    private Interactions.EffectBound effects(At a) {
        a.fields("otherwise", "perOutcome"); a.child("perOutcome").empty();
        var f = a.child("otherwise").fields("reads", "writes", "mustOverwrite");
        return new Interactions.EffectBound(new Interactions.ForeignEffects(memoryBound(f.child("reads")), memoryBound(f.child("writes")),
                f.child("mustOverwrite").list(this::operandId)), List.of());
    }
    private Scopes.MemoryBound memoryBound(At a) {
        return switch (a.kind()) {
            case "none" -> { a.fields("kind"); yield Scopes.NoMemory.INSTANCE; }
            case "within" -> { a.fields("kind", "scope"); yield new Scopes.WithinMemory(memoryScope(a.child("scope"))); }
            default -> throw Json.input(a.path(), "Unknown MemoryBound kind");
        };
    }
    private Scopes.MemoryScope memoryScope(At a) {
        return switch (a.kind()) {
            case "visible" -> {
                a.fields("kind", "unit", "includingExternal");
                yield new Scopes.VisibleMemory(unitId(a.child("unit")), a.child("includingExternal").bool());
            }
            case "all" -> {
                a.fields("kind", "publication", "includingEnvironment");
                yield new Scopes.AllMemory(publicationId(a.child("publication")), a.child("includingEnvironment").bool());
            }
            case "objects", "storage", "union" -> throw a.unsupported("MemoryScope " + a.kind());
            default -> throw Json.input(a.path(), "Unknown MemoryScope kind");
        };
    }
    private Control.InvocationOutcomes outcomes(At a) {
        a.fields("known", "remainder");
        var known = a.child("known").list(this::alternative); var remainder = controlBound(a.child("remainder"));
        if (known.isEmpty() && remainder instanceof Scopes.NoControl)
            throw a.invalid("I-60", "AIR 05 §4: empty closed outcomes are not implicit divergence");
        return new Control.InvocationOutcomes(known, remainder);
    }
    private Control.InvocationAlternative alternative(At a) {
        return switch (a.kind()) {
            case "normal" -> { a.fields("kind", "label"); yield new Control.Normal(labelId(a.child("label"))); }
            case "exception" -> {
                a.fields("kind", "tag", "destination");
                yield new Control.Exceptional(a.child("tag").modelText(), exceptionDestination(a.child("destination")));
            }
            case "any_exception" -> { a.fields("kind", "destination"); yield new Control.AnyException(exceptionDestination(a.child("destination"))); }
            case "halt" -> { a.fields("kind"); yield Control.HaltAlternative.INSTANCE; }
            case "diverge" -> { a.fields("kind"); yield Control.Diverge.INSTANCE; }
            default -> throw Json.input(a.path(), "Unknown InvocationAlternative kind");
        };
    }
    private Control.ExceptionDestination exceptionDestination(At a) {
        return switch (a.kind()) {
            case "handler" -> { a.fields("kind", "label"); yield new Control.Handler(labelId(a.child("label"))); }
            case "propagate" -> { a.fields("kind"); yield Control.Propagate.INSTANCE; }
            default -> throw Json.input(a.path(), "Unknown ExceptionDestination kind");
        };
    }
    private Scopes.ControlBound controlBound(At a) {
        return switch (a.kind()) {
            case "none" -> { a.fields("kind"); yield Scopes.NoControl.INSTANCE; }
            case "within" -> { a.fields("kind", "scope"); yield new Scopes.WithinControl(controlScope(a.child("scope"))); }
            default -> throw Json.input(a.path(), "Unknown ControlBound kind");
        };
    }
    private Scopes.ControlScope controlScope(At a) {
        return switch (a.kind()) {
            case "unit" -> {
                a.fields("kind", "unit", "labels", "normalExit", "exceptionalExit", "halt", "diverge", "externalControl");
                yield new Scopes.UnitControl(unitId(a.child("unit")), a.child("labels").bool(), a.child("normalExit").bool(),
                        a.child("exceptionalExit").bool(), a.child("halt").bool(), a.child("diverge").bool(), a.child("externalControl").bool());
            }
            case "all" -> { a.fields("kind", "publication"); yield new Scopes.AllControl(publicationId(a.child("publication"))); }
            case "labels", "union" -> throw a.unsupported("ControlScope " + a.kind());
            default -> throw Json.input(a.path(), "Unknown ControlScope kind");
        };
    }
    private Interactions.ContractKnowledge contract(At a) {
        return switch (a.kind()) {
            case "known" -> {
                a.fields("kind", "reference"); var r = a.child("reference").fields("authority", "version", "evidence");
                var evidence = r.child("evidence").list(this::originId);
                if (evidence.isEmpty()) throw r.child("evidence").invalid("AIR-01 §9", "ContractRef requires nonempty evidence origins");
                yield new Interactions.KnownContract(new Interactions.ContractRef(r.child("authority").modelText(), r.child("version").modelText(), evidence));
            }
            case "unknown" -> { a.fields("kind", "uncertainty"); yield new Interactions.UnknownContract(uncertaintyId(a.child("uncertainty"))); }
            default -> throw Json.input(a.path(), "Unknown ContractKnowledge kind");
        };
    }
    private Operations.Header header(At a) {
        a.fields("id", "origin", "coverage", "precision", "uncertainties");
        return new Operations.Header(operationId(a.child("id")), originId(a.child("origin")), coverageStatus(a.child("coverage")),
                precision(a.child("precision")), a.child("uncertainties").list(this::uncertaintyId));
    }
    private Memory.ObjectDeclaration objectDeclaration(At a) {
        a.fields("id", "displayName", "typeRef", "storage", "visibility", "origin", "coverage", "precision");
        return new Memory.ObjectDeclaration(objectId(a.child("id")), a.child("displayName").optional(At::text),
                typeRef(a.child("typeRef")), binding(a.child("storage")), visibility(a.child("visibility")),
                originId(a.child("origin")), coverageStatus(a.child("coverage")), precision(a.child("precision")));
    }
    private Types.TypeRef typeRef(At a) {
        return switch (a.kind()) {
            case "known" -> {
                a.fields("kind", "type"); var type = a.child("type");
                switch (type.kind()) {
                    case "text" -> type.fields("kind");
                    case "bool", "int", "decimal", "bytes", "opaque_type", "label" -> throw type.unsupported("Type " + type.kind());
                    default -> throw Json.input(type.path(), "Unknown Type kind");
                }
                yield Types.known(Types.Builtin.TEXT);
            }
            case "unknown_type" -> throw a.unsupported("TypeRef.unknown_type");
            default -> throw Json.input(a.path(), "Unknown TypeRef kind");
        };
    }
    private Memory.Binding binding(At a) {
        return switch (a.kind()) {
            case "cell" -> {
                a.fields("kind", "storage"); yield new Memory.CellBinding(storageId(a.child("storage")));
            }
            case "view", "alias", "alternatives", "unknown" -> throw a.unsupported("StorageBinding " + a.kind());
            default -> throw Json.input(a.path(), "Unknown StorageBinding kind");
        };
    }
    private Memory.Storage storage(At a) {
        return switch (a.kind()) {
            case "cell" -> {
                a.fields("kind", "header", "typeRef");
                yield new Memory.Cell(storageHeader(a.child("header")), typeRef(a.child("typeRef")));
            }
            case "region" -> throw a.unsupported("Storage.region");
            default -> throw Json.input(a.path(), "Unknown Storage kind");
        };
    }
    private Memory.StorageHeader storageHeader(At a) {
        a.fields("id", "owner", "lifetime", "visibility", "origin");
        var id = storageId(a.child("id")); var owner = a.child("owner").optional(this::unitId);
        var lifetime = lifetime(a.child("lifetime")); var visibility = visibility(a.child("visibility"));
        var origin = originId(a.child("origin"));
        if (lifetime == Memory.Lifetime.ACTIVATION && owner.isEmpty())
            throw a.child("owner").invalid("AIR-03 §2", "Activation storage requires its owning unit");
        return new Memory.StorageHeader(id, owner, lifetime, visibility, origin);
    }
    private Operand.Header operandHeader(At a) {
        a.fields("id", "role", "origin");
        return new Operand.Header(operandId(a.child("id")), role(a.child("role")), originId(a.child("origin")));
    }
    private Place place(At a) {
        return switch (a.kind()) {
            case "object" -> {
                a.fields("kind", "header", "object");
                yield new Places.ObjectPlace(operandHeader(a.child("header")), objectId(a.child("object")));
            }
            case "choice", "region_slice" -> throw a.unsupported("Place " + a.kind());
            default -> throw Json.input(a.path(), "Unknown Place kind");
        };
    }
    private Expression expression(At a) {
        return switch (a.kind()) {
            case "literal" -> {
                a.fields("kind", "header", "value");
                yield new Expressions.Literal(operandHeader(a.child("header")), literalValue(a.child("value")));
            }
            case "read" -> {
                a.fields("kind", "header", "place");
                yield new Expressions.Read(operandHeader(a.child("header")), place(a.child("place")));
            }
            case "unknown", "unary", "binary", "quantize", "fit_text", "slice_text", "trim_right" ->
                    throw a.unsupported("Expression " + a.kind());
            default -> throw Json.input(a.path(), "Unknown Expression kind");
        };
    }
    private Values.LiteralValue literalValue(At a) {
        return switch (a.kind()) {
            case "text" -> {
                a.fields("kind", "value"); yield new Values.TextValue(a.child("value").text());
            }
            case "bool", "int", "decimal", "bytes", "label" -> throw a.unsupported("LiteralValue " + a.kind());
            default -> throw Json.input(a.path(), "Unknown LiteralValue kind");
        };
    }
    private Memory.Lifetime lifetime(At a) { return switch (a.text()) {
        case "ACTIVATION" -> Memory.Lifetime.ACTIVATION; case "PERSISTENT" -> Memory.Lifetime.PERSISTENT;
        case "EXTERNAL" -> Memory.Lifetime.EXTERNAL;
        default -> throw Json.input(a.path(), "Unknown Lifetime token"); }; }
    private Memory.Visibility visibility(At a) { return switch (a.text()) {
        case "PRIVATE" -> Memory.Visibility.PRIVATE; case "SHARED" -> Memory.Visibility.SHARED;
        case "UNKNOWN" -> Memory.Visibility.UNKNOWN;
        default -> throw Json.input(a.path(), "Unknown Visibility token"); }; }
    private Operand.Role role(At a) { return switch (a.text()) {
        case "VALUE_READ" -> Operand.Role.VALUE_READ; case "VALUE_WRITE" -> Operand.Role.VALUE_WRITE;
        case "ADDRESS_READ" -> Operand.Role.ADDRESS_READ; case "PREDICATE" -> Operand.Role.PREDICATE;
        case "CALL_TARGET" -> Operand.Role.CALL_TARGET; case "ARGUMENT_VALUE" -> Operand.Role.ARGUMENT_VALUE;
        case "ARGUMENT_REFERENCE" -> Operand.Role.ARGUMENT_REFERENCE; case "RESULT_TARGET" -> Operand.Role.RESULT_TARGET;
        case "RESOURCE_TARGET" -> Operand.Role.RESOURCE_TARGET; case "CONTROL_TARGET" -> Operand.Role.CONTROL_TARGET;
        default -> throw Json.input(a.path(), "Unknown OperandRole token"); }; }
    private Evidence.Precision precision(At a) {
        a.fields("control", "storage", "effects", "values", "dependencies");
        return new Evidence.Precision(claim(a.child("control")), claim(a.child("storage")), claim(a.child("effects")),
                claim(a.child("values")), claim(a.child("dependencies")));
    }
    private Evidence.Claim claim(At a) {
        a.fields("scope", "status", "reasons");
        return new Evidence.Claim(scope(a.child("scope")), precisionStatus(a.child("status")), a.child("reasons").list(this::uncertaintyId));
    }
    private Evidence.Coverage coverage(At a) {
        a.fields("inventory", "scope", "items", "uncertainties");
        return new Evidence.Coverage(inventoryStatus(a.child("inventory")), scope(a.child("scope")),
                a.child("items").list(this::item), a.child("uncertainties").list(this::uncertaintyId));
    }
    private Evidence.CoverageItem item(At a) {
        a.fields("sourceKey", "origin", "status", "outputs", "uncertainties", "elimination");
        var elimination = a.child("elimination");
        if (elimination.value() != Json.Nil.INSTANCE) {
            elimination.fields("rule", "origin"); throw elimination.unsupported("Elimination");
        }
        var key = a.child("sourceKey").modelText(); var origin = originId(a.child("origin")); var status = coverageStatus(a.child("status"));
        var outputs = a.child("outputs").list(this::id); var gaps = a.child("uncertainties").list(this::uncertaintyId);
        return a.construct(() -> new Evidence.CoverageItem(key, origin, status, outputs, gaps, Optional.empty()));
    }
    private Evidence.Uncertainty uncertainty(At a) {
        a.fields("id", "code", "dimensions", "scope", "reason", "origin");
        var id = uncertaintyId(a.child("id")); var code = a.child("code").text(); var dimensions = a.child("dimensions").list(this::dimension);
        var scope = scope(a.child("scope")); var reason = a.child("reason").text(); var origin = originId(a.child("origin"));
        if (dimensions.isEmpty()) throw a.child("dimensions").invalid("AIR-06 §4", "Uncertainty must identify its affected domain");
        a.child("code").modelText(); a.child("reason").modelText();
        return a.construct(() -> new Evidence.Uncertainty(id, code, dimensions, scope, reason, origin));
    }
    private Scopes.FactScope scope(At a) {
        return switch (a.kind()) {
            case "publication" -> { a.fields("kind", "publication"); yield new Scopes.PublicationScope(publicationId(a.child("publication"))); }
            case "unit" -> { a.fields("kind", "unit"); yield new Scopes.UnitScope(unitId(a.child("unit"))); }
            case "entities" -> {
                a.fields("kind", "entities"); var entities = a.child("entities").list(this::id);
                if (entities.isEmpty()) throw a.child("entities").representability("EntityScope requires nonempty entities; binding admits Id[]");
                yield a.construct(() -> new Scopes.EntityScope(entities));
            }
            default -> throw Json.input(a.path(), "Unknown FactScope kind");
        };
    }
    private Origins.Origin origin(At a) {
        return switch (a.kind()) {
            case "written" -> {
                a.fields("kind", "id", "artifact", "location", "includes", "exact");
                yield new Origins.Written(originId(a.child("id")), artifactId(a.child("artifact")),
                        a.child("location").optional(this::location), a.child("includes").list(this::include), a.child("exact").bool());
            }
            case "derived" -> {
                a.fields("kind", "id", "inputs", "rule"); var id = originId(a.child("id"));
                var inputs = a.child("inputs").list(this::originId); var rule = a.child("rule").text();
                if (inputs.isEmpty()) throw a.child("inputs").invalid("I-36", "AIR 06 §5: derived origin requires one or more inputs");
                a.child("rule").modelText();
                yield a.construct(() -> new Origins.Derived(id, inputs, rule));
            }
            case "contractual" -> { a.fields("kind", "id", "authority", "version"); throw a.unsupported("Origin.contractual"); }
            case "unavailable" -> { a.fields("kind", "id", "reason"); throw a.unsupported("Origin.unavailable"); }
            default -> throw Json.input(a.path(), "Unknown Origin kind");
        };
    }
    private Origins.Location location(At a) {
        switch (a.kind()) {
            case "line_columns" -> a.fields("kind", "span");
            case "offsets" -> { a.fields("kind", "start", "end", "unit", "endExclusive"); throw a.unsupported("Location.offsets"); }
            default -> throw Json.input(a.path(), "Unknown Location kind");
        }
        var s = a.child("span").fields("start", "end", "lineBase", "columnBase", "columnUnit", "endExclusive");
        var start = position(s.child("start")); var end = position(s.child("end"));
        var lb = natural(s.child("lineBase")); var cb = natural(s.child("columnBase"));
        var cu = columnUnit(s.child("columnUnit")); var exclusive = s.child("endExclusive").bool();
        if (lb.compareTo(BigInteger.ONE) > 0) throw s.child("lineBase").representability("Span only supports bases 0 or 1; binding admits Natural");
        if (cb.compareTo(BigInteger.ONE) > 0) throw s.child("columnBase").representability("Span only supports bases 0 or 1; binding admits Natural");
        // Audited Java preconditions, explicitly classified by the human decision for this pin.
        if (start.line().compareTo(lb) < 0 || end.line().compareTo(lb) < 0
                || start.column().compareTo(cb) < 0 || end.column().compareTo(cb) < 0)
            throw s.spanRepresentability("air-java requires coordinates at or above the declared bases");
        if (start.line().compareTo(end.line()) > 0)
            throw s.spanRepresentability("air-java requires start.line <= end.line");
        if (start.line().equals(end.line()) && start.column().compareTo(end.column()) > 0)
            throw s.spanRepresentability("air-java requires start.column <= end.column on the same line");
        return s.construct(() -> new Origins.LineColumns(new Origins.Span(start, end, lb, cb, cu, exclusive)));
    }
    private Origins.Position position(At a) {
        a.fields("line", "column"); return new Origins.Position(natural(a.child("line")), natural(a.child("column")));
    }
    private Origins.IncludeFrame include(At a) {
        a.fields("including", "included", "requestedName", "site");
        var including = artifactId(a.child("including")); var included = artifactId(a.child("included"));
        var name = a.child("requestedName").modelText(); var site = a.child("site").optional(this::location);
        return a.construct(() -> new Origins.IncludeFrame(including, included, name, site));
    }
    private BigInteger natural(At a) {
        String s = a.text();
        if (!s.matches("0|[1-9][0-9]*")) throw Json.input(a.path(), "Expected canonical Natural string");
        return new BigInteger(s);
    }
    private Id id(At a) {
        String domain = a.child("domain").text();
        if (domain.equals("publication")) {
            a.fields("domain", "localId"); String local = a.child("localId").modelText();
            return a.construct(() -> new PublicationId(local));
        }
        boolean owned = Set.of("entry", "label", "operation", "object", "completion_port", "operand").contains(domain);
        if (!owned && !Set.of("artifact", "relation", "unit", "storage", "resource", "origin", "uncertainty", "premise").contains(domain))
            throw Json.input(a.path(), "Unknown ID domain");
        if (domain.equals("operand")) a.fields("domain", "publication", "unit", "owner", "localId");
        else if (owned) a.fields("domain", "publication", "unit", "localId");
        else a.fields("domain", "publication", "localId");
        String namespace = a.child("publication").modelText(); String local = a.child("localId").modelText();
        var publication = a.construct(() -> new PublicationId(namespace));
        String unitName = owned ? a.child("unit").modelText() : null;
        UnitId unit = owned ? a.construct(() -> new UnitId(publication, unitName)) : null;
        OperandOwner operandOwner = domain.equals("operand") ? operandOwner(a.child("owner"), unit) : null;
        return a.construct(() -> switch (domain) {
            case "artifact" -> new ArtifactId(publication, local); case "relation" -> new ArtifactRelationId(publication, local);
            case "unit" -> new UnitId(publication, local); case "storage" -> new StorageId(publication, local);
            case "resource" -> new ResourceId(publication, local); case "origin" -> new OriginId(publication, local);
            case "uncertainty" -> new UncertaintyId(publication, local); case "premise" -> new PremiseId(publication, local);
            case "entry" -> new EntryId(unit, local); case "label" -> new LabelId(unit, local);
            case "operation" -> new OperationId(unit, local); case "object" -> new ObjectId(unit, local);
            case "completion_port" -> new CompletionPortId(unit, local); case "operand" -> new OperandId(operandOwner, local);
            default -> throw new IllegalStateException("ID catalogue mismatch");
        });
    }
    private OperandOwner operandOwner(At a, UnitId unit) {
        a.fields("kind", "localId"); String kind = a.kind(), local = a.child("localId").modelText();
        return switch (kind) {
            case "operation" -> a.construct(() -> new OperationOwner(new OperationId(unit, local)));
            case "entry" -> a.construct(() -> new EntryOwner(new EntryId(unit, local)));
            default -> throw Json.input(a.path(), "Unknown Operand owner kind");
        };
    }
    private <T extends Id> T typedId(At a, Class<T> type) {
        Id id = id(a);
        if (!type.isInstance(id)) throw a.invalid("I-02", "AIR 01 §4: ID domain does not match reference role");
        return type.cast(id);
    }
    private PublicationId publicationId(At a) { return typedId(a, PublicationId.class); }
    private ArtifactId artifactId(At a) { return typedId(a, ArtifactId.class); }
    private UnitId unitId(At a) { return typedId(a, UnitId.class); }
    private EntryId entryId(At a) { return typedId(a, EntryId.class); }
    private LabelId labelId(At a) { return typedId(a, LabelId.class); }
    private OperationId operationId(At a) { return typedId(a, OperationId.class); }
    private ObjectId objectId(At a) { return typedId(a, ObjectId.class); }
    private StorageId storageId(At a) { return typedId(a, StorageId.class); }
    private OperandId operandId(At a) { return typedId(a, OperandId.class); }
    private OriginId originId(At a) { return typedId(a, OriginId.class); }
    private UncertaintyId uncertaintyId(At a) { return typedId(a, UncertaintyId.class); }
    private Evidence.Dimension dimension(At a) { return switch (a.text()) {
        case "CONTROL" -> Evidence.Dimension.CONTROL; case "STORAGE" -> Evidence.Dimension.STORAGE;
        case "EFFECTS" -> Evidence.Dimension.EFFECTS; case "VALUES" -> Evidence.Dimension.VALUES;
        case "DEPENDENCIES" -> Evidence.Dimension.DEPENDENCIES;
        default -> throw Json.input(a.path(), "Unknown Dimension token"); }; }
    private Evidence.PrecisionStatus precisionStatus(At a) { return switch (a.text()) {
        case "EXACT" -> Evidence.PrecisionStatus.EXACT; case "CONSERVATIVE" -> Evidence.PrecisionStatus.CONSERVATIVE;
        case "OPEN" -> Evidence.PrecisionStatus.OPEN; case "UNAVAILABLE" -> Evidence.PrecisionStatus.UNAVAILABLE;
        case "NOT_APPLICABLE" -> Evidence.PrecisionStatus.NOT_APPLICABLE;
        default -> throw Json.input(a.path(), "Unknown PrecisionStatus token"); }; }
    private Evidence.CoverageStatus coverageStatus(At a) { return switch (a.text()) {
        case "MODELED" -> Evidence.CoverageStatus.MODELED; case "ABSTRACTED" -> Evidence.CoverageStatus.ABSTRACTED;
        case "UNSUPPORTED" -> Evidence.CoverageStatus.UNSUPPORTED; case "INPUT_MISSING" -> Evidence.CoverageStatus.INPUT_MISSING;
        default -> throw Json.input(a.path(), "Unknown CoverageStatus token"); }; }
    private Evidence.InventoryStatus inventoryStatus(At a) { return switch (a.text()) {
        case "COMPLETE" -> Evidence.InventoryStatus.COMPLETE; case "PARTIAL" -> Evidence.InventoryStatus.PARTIAL;
        case "UNAVAILABLE" -> Evidence.InventoryStatus.UNAVAILABLE;
        default -> throw Json.input(a.path(), "Unknown InventoryStatus token"); }; }
    private Origins.ColumnUnit columnUnit(At a) { return switch (a.text()) {
        case "UNICODE_SCALAR" -> Origins.ColumnUnit.UNICODE_SCALAR; case "UTF16_CODE_UNIT" -> Origins.ColumnUnit.UTF16_CODE_UNIT;
        case "OCTET" -> Origins.ColumnUnit.OCTET;
        default -> throw Json.input(a.path(), "Unknown ColumnUnit token"); }; }
}
