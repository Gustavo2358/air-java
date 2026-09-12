package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Unit;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import static io.github.gustavo2358.air.json.Json.*;

/** Explicit semantic-to-wire mapping. Field names and variants come from the pinned binding. */
final class BindingWriter {
    private static Arr empty(List<?> items, String path) {
        if (!items.isEmpty()) throw limit(path, "Binding form outside implemented 1A/4B coverage");
        return new Arr(List.of());
    }
    Value envelope(Publication p) {
        return object("binding", "analysis-ir-json", "bindingVersion", "1.0.0", "airVersion", "2.0.0",
                "publication", object("id", id(p.id()), "capabilities", manifest(p.capabilities()),
                "artifacts", array(p.artifacts(), this::artifact), "units", array(p.units(), this::unit),
                "storage", array(p.storage(), this::storage),
                "resources", empty(p.resources(), "$.publication.resources"),
                "artifactRelations", empty(p.artifactRelations(), "$.publication.artifactRelations"),
                "origins", array(p.origins(), this::origin), "coverage", coverage(p.coverage()),
                "uncertainties", array(p.uncertainties(), this::uncertainty),
                "premises", array(p.premises(), this::premise)));
    }
    private Value premise(Proofs.Premise p) {
        if (!(p.assertion() instanceof Proofs.DisjointStorage d))
            throw limit("$.publication.premises.assertion", "Only Assertion.disjoint_storage implemented");
        return object("id", id(p.id()), "authority", p.authority(), "justification", p.justification(),
                "origin", id(p.origin()), "assertion", object("kind", "disjoint_storage", "storage", array(d.storage(), this::id)));
    }
    private Value manifest(Capabilities.Manifest manifest) {
        if (!manifest.required().isEmpty() || !manifest.provided().isEmpty())
            throw new AirJsonException(AirJsonException.Code.UNSUPPORTED_CAPABILITY,
                    "$.publication.capabilities", "1A implements the empty capability manifest");
        return object("required", new Arr(List.of()), "provided", new Arr(List.of()));
    }
    private Value artifact(Origins.Artifact a) {
        return object("id", id(a.id()), "logicalName", a.logicalName(), "contentDigest", optional(a.contentDigest(), Json::value));
    }
    private Value unit(Unit u) {
        if (u.body() != Unit.BodyAvailability.AVAILABLE)
            throw limit("$.publication.units.body", "BodyKnowledge.unavailable not implemented");
        return object("id", id(u.id()), "containingUnit", optional(u.containingUnit(), this::id),
                "objects", array(u.objects(), this::objectDeclaration),
                "visibleObjects", empty(u.visibleObjects(), "$.publication.units.visibleObjects"),
                "entries", array(u.entries(), this::entry), "sequences", array(u.sequences(), this::sequence),
                "completionPorts", empty(u.completionPorts(), "$.publication.units.completionPorts"),
                "body", object("kind", "available"), "coverage", coverage(u.coverage()), "origin", id(u.origin()));
    }
    private Value entry(Entries.Entry e) {
        return object("id", id(e.id()), "initialLabel", optional(e.initialLabel(), this::id),
                "signature", signature(e.signature()), "state", object(
                "conditions", empty(e.state().conditions(), "$.publication.units.entries.state.conditions"),
                "uncertainties", array(e.state().uncertainties(), this::id)), "origin", id(e.origin()));
    }
    private Value signature(Interactions.Signature s) {
        return object("parameters", object("known", empty(s.parameters().known(), "$.signature.parameters.known"),
                        "remainder", remainder(s.parameters().remainder())),
                "results", object("known", empty(s.results().known(), "$.signature.results.known"),
                        "remainder", remainder(s.results().remainder())), "origin", id(s.origin()));
    }
    private Value remainder(Interactions.UnknownBound r) {
        return switch (r) {
            case Interactions.NoRemainder ignored -> object("kind", "none");
            case Interactions.UnknownRemainder u -> object("kind", "unknown", "uncertainty", id(u.uncertainty()));
        };
    }
    private Value sequence(Sequence s) {
        return object("label", id(s.label()), "instructions", array(s.instructions(), this::instruction),
                "terminator", operation(s.terminator()), "origin", id(s.origin()));
    }
    private Value operation(Terminator t) {
        if (t instanceof Operations.Jump j)
            return object("kind", "jump", "header", header(j.header()), "destination", id(j.destination()));
        if (t instanceof Operations.Branch b)
            return object("kind", "branch", "header", header(b.header()), "predicate", expression(b.predicate()),
                    "trueDestination", id(b.trueDestination()), "falseDestination", id(b.falseDestination()));
        if (t instanceof Operations.Invoke i)
            return object("kind", "invoke", "header", header(i.header()), "action", i.action(), "target", target(i.target()),
                    "arguments", empty(i.arguments(), "$.invoke.arguments"), "results", empty(i.results(), "$.invoke.results"),
                    "signature", invocationSignature(i.signature()), "effectOperands", array(i.effectOperands(), this::place),
                    "effectBound", effects(i.effectBound()), "outcomes", outcomes(i.outcomes()), "contract", contract(i.contract()));
        if (!(t instanceof Operations.Return r)) throw limit("$.sequence.terminator", "Operation " + t.kind() + " not implemented");
        return object("kind", "return", "header", header(r.header()), "values", empty(r.values(), "$.return.values"));
    }
    private Value target(Interactions.Target t) {
        return switch (t) {
            case Interactions.LiteralTarget l -> object("kind", "literal", "category", l.category(), "namespace", l.namespace(),
                    "name", l.name(), "namePolicy", namePolicy(l.namePolicy()), "origin", id(l.origin()));
            case Interactions.ComputedTarget c -> object("kind", "computed", "category", c.category(), "namespace", c.namespace(),
                    "name", expression(c.name()), "namePolicy", namePolicy(c.namePolicy()), "origin", id(c.origin()));
            case Interactions.InternalTarget ignored -> throw limit("$.invoke.target", "Target.internal not implemented");
        };
    }
    private Value namePolicy(Interactions.NamePolicy p) {
        return switch (p) {
            case Interactions.ExactName ignored -> object("kind", "exact");
            case Interactions.UnknownName u -> object("kind", "unknown", "uncertainty", id(u.uncertainty()));
            case Interactions.ExtensionName ignored -> throw limit("$.target.namePolicy", "NamePolicy.extension not implemented");
        };
    }
    private Value invocationSignature(Interactions.InvocationSignature s) {
        if (!(s instanceof Interactions.ExternalSignature e)) throw limit("$.invoke.signature", "InvocationSignature.entry not implemented");
        return object("kind", "external", "signature", signature(e.signature()));
    }
    private Value effects(Interactions.EffectBound e) {
        var f = e.otherwise();
        return object("otherwise", object("reads", memoryBound(f.reads()), "writes", memoryBound(f.writes()),
                "mustOverwrite", array(f.mustOverwrite(), this::id)), "perOutcome", empty(e.perOutcome(), "$.effectBound.perOutcome"));
    }
    private Value memoryBound(Scopes.MemoryBound b) {
        return switch (b) {
            case Scopes.NoMemory ignored -> object("kind", "none");
            case Scopes.WithinMemory w -> object("kind", "within", "scope", memoryScope(w.scope()));
        };
    }
    private Value memoryScope(Scopes.MemoryScope s) {
        return switch (s) {
            case Scopes.VisibleMemory v -> object("kind", "visible", "unit", id(v.unit()), "includingExternal", v.includingExternal());
            case Scopes.AllMemory a -> object("kind", "all", "publication", id(a.publication()), "includingEnvironment", a.includingEnvironment());
            default -> throw limit("$.memory.scope", "Only visible/all MemoryScope implemented");
        };
    }
    private Value outcomes(Control.InvocationOutcomes o) {
        return object("known", array(o.known(), this::alternative), "remainder", controlBound(o.remainder()));
    }
    private Value alternative(Control.InvocationAlternative a) {
        return switch (a) {
            case Control.Normal n -> object("kind", "normal", "label", id(n.label()));
            case Control.Exceptional e -> object("kind", "exception", "tag", e.tag(), "destination", exceptionDestination(e.destination()));
            case Control.AnyException e -> object("kind", "any_exception", "destination", exceptionDestination(e.destination()));
            case Control.HaltAlternative ignored -> object("kind", "halt");
            case Control.Diverge ignored -> object("kind", "diverge");
        };
    }
    private Value exceptionDestination(Control.ExceptionDestination d) {
        return switch (d) {
            case Control.Handler h -> object("kind", "handler", "label", id(h.label()));
            case Control.Propagate ignored -> object("kind", "propagate");
        };
    }
    private Value controlBound(Scopes.ControlBound b) {
        return switch (b) {
            case Scopes.NoControl ignored -> object("kind", "none");
            case Scopes.WithinControl w -> object("kind", "within", "scope", controlScope(w.scope()));
        };
    }
    private Value controlScope(Scopes.ControlScope s) {
        return switch (s) {
            case Scopes.UnitControl u -> object("kind", "unit", "unit", id(u.unit()), "labels", u.labels(), "normalExit", u.normalExit(),
                    "exceptionalExit", u.exceptionalExit(), "halt", u.halt(), "diverge", u.diverge(), "externalControl", u.externalControl());
            case Scopes.AllControl a -> object("kind", "all", "publication", id(a.publication()));
            default -> throw limit("$.control.scope", "Only unit/all ControlScope implemented");
        };
    }
    private Value contract(Interactions.ContractKnowledge k) {
        return switch (k) {
            case Interactions.KnownContract c -> object("kind", "known", "reference", object("authority", c.reference().authority(),
                    "version", c.reference().version(), "evidence", array(c.reference().evidence(), this::id)));
            case Interactions.UnknownContract u -> object("kind", "unknown", "uncertainty", id(u.uncertainty()));
        };
    }
    private Value header(Operations.Header h) {
        return object("id", id(h.id()), "origin", id(h.origin()), "coverage", coverageStatus(h.coverage()),
                "precision", precision(h.precision()), "uncertainties", array(h.uncertainties(), this::id));
    }
    private Value objectDeclaration(Memory.ObjectDeclaration o) {
        return object("id", id(o.id()), "displayName", optional(o.displayName(), Json::value), "typeRef", typeRef(o.typeRef()),
                "storage", binding(o.storage()), "visibility", visibility(o.visibility()), "origin", id(o.origin()),
                "coverage", coverageStatus(o.coverage()), "precision", precision(o.precision()));
    }
    private Value typeRef(Types.TypeRef t) {
        if (!(t instanceof Types.Known k)) throw limit("$.typeRef", "Only known(text/bool) implemented");
        String kind;
        if (k.type() == Types.Builtin.TEXT) kind = "text";
        else if (k.type() == Types.Builtin.BOOL) kind = "bool";
        else throw limit("$.typeRef", "Only known(text/bool) implemented");
        return object("kind", "known", "type", object("kind", kind));
    }
    private Value binding(Memory.Binding b) {
        if (!(b instanceof Memory.CellBinding c)) throw limit("$.object.storage", "Only StorageBinding.cell implemented");
        return object("kind", "cell", "storage", id(c.storage()));
    }
    private Value storage(Memory.Storage s) {
        if (!(s instanceof Memory.Cell c)) throw limit("$.publication.storage", "Only Storage.cell implemented");
        return object("kind", "cell", "header", storageHeader(c.header()), "typeRef", typeRef(c.typeRef()));
    }
    private Value storageHeader(Memory.StorageHeader h) {
        return object("id", id(h.id()), "owner", optional(h.owner(), this::id), "lifetime", lifetime(h.lifetime()),
                "visibility", visibility(h.visibility()), "origin", id(h.origin()));
    }
    private Value operandHeader(Operand.Header h) {
        return object("id", id(h.id()), "role", role(h.role()), "origin", id(h.origin()));
    }
    private Value place(Place p) {
        if (!(p instanceof Places.ObjectPlace o)) throw limit("$.place", "Only Place.object implemented");
        return object("kind", "object", "header", operandHeader(o.header()), "object", id(o.object()));
    }
    private Value literalValue(Values.LiteralValue v) {
        if (!(v instanceof Values.TextValue t)) throw limit("$.literal.value", "Only LiteralValue.text implemented");
        return object("kind", "text", "value", t.value());
    }
    private static final class ExpressionFrame {
        final Expression expression;
        final List<Value> dependencies = new ArrayList<>();
        int next;
        ExpressionFrame(Expression expression) { this.expression = expression; }
    }
    private Value expression(Expression expression) {
        // Unknown dependencies can nest: use postorder frames, never the JVM call stack.
        var stack = new ArrayDeque<ExpressionFrame>(); stack.push(new ExpressionFrame(expression));
        while (!stack.isEmpty()) {
            var frame = stack.peek(); var e = frame.expression;
            if (e instanceof Expressions.Unknown u && frame.next < u.dependencies().size()) {
                stack.push(new ExpressionFrame(u.dependencies().get(frame.next++))); continue;
            }
            Value result;
            if (e instanceof Expressions.Unknown u)
                result = object("kind", "unknown", "header", operandHeader(u.header()), "typeRef", typeRef(u.typeRef()),
                        "dependencies", new Arr(frame.dependencies), "remainingReads", memoryBound(u.remainingReads()), "reason", id(u.reason()));
            else if (e instanceof Expressions.Read r)
                result = object("kind", "read", "header", operandHeader(r.header()), "place", place(r.place()));
            else if (e instanceof Expressions.Literal l)
                result = object("kind", "literal", "header", operandHeader(l.header()), "value", literalValue(l.value()));
            else throw limit("$.expression", "Only Expression.literal/read/unknown implemented");
            stack.pop();
            if (stack.isEmpty()) return result;
            stack.peek().dependencies.add(result);
        }
        throw new IllegalStateException("Expression frame invariant");
    }
    private Value instruction(Instruction i) {
        if (!(i instanceof Operations.Assign a)) throw limit("$.sequence.instructions", "Only Instruction.assign implemented");
        return object("kind", "assign", "header", header(a.header()), "destination", place(a.destination()), "value", expression(a.value()));
    }
    // Binding §10.4: closed tables; Java enum spelling never supplies wire tokens.
    private String lifetime(Memory.Lifetime value) { return switch (value) {
        case ACTIVATION -> "ACTIVATION"; case PERSISTENT -> "PERSISTENT"; case EXTERNAL -> "EXTERNAL";
    }; }
    private String visibility(Memory.Visibility value) { return switch (value) {
        case PRIVATE -> "PRIVATE"; case SHARED -> "SHARED"; case UNKNOWN -> "UNKNOWN";
    }; }
    private String role(Operand.Role value) { return switch (value) {
        case VALUE_READ -> "VALUE_READ"; case VALUE_WRITE -> "VALUE_WRITE"; case ADDRESS_READ -> "ADDRESS_READ";
        case PREDICATE -> "PREDICATE"; case CALL_TARGET -> "CALL_TARGET"; case ARGUMENT_VALUE -> "ARGUMENT_VALUE";
        case ARGUMENT_REFERENCE -> "ARGUMENT_REFERENCE"; case RESULT_TARGET -> "RESULT_TARGET";
        case RESOURCE_TARGET -> "RESOURCE_TARGET"; case CONTROL_TARGET -> "CONTROL_TARGET";
    }; }
    private Value precision(Evidence.Precision p) {
        return object("control", claim(p.control()), "storage", claim(p.storage()), "effects", claim(p.effects()),
                "values", claim(p.values()), "dependencies", claim(p.dependencies()));
    }
    private Value claim(Evidence.Claim c) {
        return object("scope", scope(c.scope()), "status", precisionStatus(c.status()), "reasons", array(c.reasons(), this::id));
    }
    private Value uncertainty(Evidence.Uncertainty u) {
        return object("id", id(u.id()), "code", u.code(), "dimensions", array(u.dimensions(), d -> value(dimension(d))),
                "scope", scope(u.scope()), "reason", u.reason(), "origin", id(u.origin()));
    }
    private Value coverage(Evidence.Coverage c) {
        return object("inventory", inventoryStatus(c.inventory()), "scope", scope(c.scope()),
                "items", array(c.items(), this::item), "uncertainties", array(c.uncertainties(), this::id));
    }
    private Value item(Evidence.CoverageItem i) {
        if (i.elimination().isPresent()) throw limit("$.coverage.items.elimination", "Elimination not implemented");
        return object("sourceKey", i.sourceKey(), "origin", id(i.origin()), "status", coverageStatus(i.status()),
                "outputs", array(i.outputs(), this::id), "uncertainties", array(i.uncertainties(), this::id), "elimination", null);
    }
    private Value scope(Scopes.FactScope scope) {
        return switch (scope) {
            case Scopes.PublicationScope p -> object("kind", "publication", "publication", id(p.publication()));
            case Scopes.UnitScope u -> object("kind", "unit", "unit", id(u.unit()));
            case Scopes.EntityScope e -> object("kind", "entities", "entities", array(e.entities(), this::id));
        };
    }
    private Value origin(Origins.Origin o) {
        return switch (o) {
            case Origins.Written w -> object("kind", "written", "id", id(w.id()), "artifact", id(w.artifact()),
                    "location", optional(w.location(), this::location), "includes", array(w.includes(), this::include), "exact", w.exact());
            case Origins.Derived d -> object("kind", "derived", "id", id(d.id()), "inputs", array(d.inputs(), this::id), "rule", d.rule());
            case Origins.Contractual ignored -> throw limit("$.origins", "Origin.contractual not implemented");
            case Origins.Unavailable ignored -> throw limit("$.origins", "Origin.unavailable not implemented");
        };
    }
    private Value location(Origins.Location location) {
        if (!(location instanceof Origins.LineColumns l)) throw limit("$.location", "Location.offsets not implemented");
        var s = l.span();
        return object("kind", "line_columns", "span", object("start", position(s.start()), "end", position(s.end()),
                "lineBase", s.lineBase(), "columnBase", s.columnBase(), "columnUnit", columnUnit(s.columnUnit()), "endExclusive", s.endExclusive()));
    }
    private Value position(Origins.Position p) { return object("line", p.line(), "column", p.column()); }
    private Value include(Origins.IncludeFrame f) {
        return object("including", id(f.including()), "included", id(f.included()), "requestedName", f.requestedName(),
                "site", optional(f.site(), this::location));
    }
    Value id(Id id) {
        return switch (id) {
            case PublicationId p -> object("domain", "publication", "localId", p.localId());
            case ArtifactId a -> global("artifact", a);
            case ArtifactRelationId r -> global("relation", r);
            case UnitId u -> global("unit", u);
            case StorageId s -> global("storage", s);
            case ResourceId r -> global("resource", r);
            case OriginId o -> global("origin", o);
            case UncertaintyId u -> global("uncertainty", u);
            case PremiseId p -> global("premise", p);
            case EntryId e -> owned("entry", e, e.unit());
            case LabelId l -> owned("label", l, l.unit());
            case OperationId o -> owned("operation", o, o.unit());
            case ObjectId o -> owned("object", o, o.unit());
            case CompletionPortId c -> owned("completion_port", c, c.unit());
            case OperandId o -> {
                Value owner = switch (o.owner()) {
                    case OperationOwner op -> object("kind", "operation", "localId", op.operation().localId());
                    case EntryOwner en -> object("kind", "entry", "localId", en.entry().localId());
                };
                yield object("domain", "operand", "publication", o.publication().localId(),
                        "unit", o.owner().unit().localId(), "owner", owner, "localId", o.localId());
            }
        };
    }
    // Binding §10.4 fixes these lexemes independently of Java enum spellings.
    private String dimension(Evidence.Dimension value) { return switch (value) {
        case CONTROL -> "CONTROL"; case STORAGE -> "STORAGE"; case EFFECTS -> "EFFECTS";
        case VALUES -> "VALUES"; case DEPENDENCIES -> "DEPENDENCIES";
    }; }
    private String precisionStatus(Evidence.PrecisionStatus value) { return switch (value) {
        case EXACT -> "EXACT"; case CONSERVATIVE -> "CONSERVATIVE"; case OPEN -> "OPEN";
        case UNAVAILABLE -> "UNAVAILABLE"; case NOT_APPLICABLE -> "NOT_APPLICABLE";
    }; }
    private String coverageStatus(Evidence.CoverageStatus value) { return switch (value) {
        case MODELED -> "MODELED"; case ABSTRACTED -> "ABSTRACTED";
        case UNSUPPORTED -> "UNSUPPORTED"; case INPUT_MISSING -> "INPUT_MISSING";
    }; }
    private String inventoryStatus(Evidence.InventoryStatus value) { return switch (value) {
        case COMPLETE -> "COMPLETE"; case PARTIAL -> "PARTIAL"; case UNAVAILABLE -> "UNAVAILABLE";
    }; }
    private String columnUnit(Origins.ColumnUnit value) { return switch (value) {
        case UNICODE_SCALAR -> "UNICODE_SCALAR"; case UTF16_CODE_UNIT -> "UTF16_CODE_UNIT"; case OCTET -> "OCTET";
    }; }
    private Value global(String domain, Id i) {
        return object("domain", domain, "publication", i.publication().localId(), "localId", i.localId());
    }
    private Value owned(String domain, Id i, UnitId unit) {
        return object("domain", domain, "publication", i.publication().localId(), "unit", unit.localId(), "localId", i.localId());
    }
}
