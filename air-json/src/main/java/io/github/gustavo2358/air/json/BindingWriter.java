package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Unit;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.List;
import static io.github.gustavo2358.air.json.Json.*;

/** Explicit semantic-to-wire mapping. Field names and variants come from the pinned binding. */
final class BindingWriter {
    private static Arr empty(List<?> items, String path) {
        if (!items.isEmpty()) throw limit(path, "Binding form outside implemented 1A coverage");
        return new Arr(List.of());
    }
    Value envelope(Publication p) {
        return object("binding", "analysis-ir-json", "bindingVersion", "1.0.0", "airVersion", "2.0.0",
                "publication", object("id", id(p.id()), "capabilities", manifest(p.capabilities()),
                "artifacts", array(p.artifacts(), this::artifact), "units", array(p.units(), this::unit),
                "storage", empty(p.storage(), "$.publication.storage"),
                "resources", empty(p.resources(), "$.publication.resources"),
                "artifactRelations", empty(p.artifactRelations(), "$.publication.artifactRelations"),
                "origins", array(p.origins(), this::origin), "coverage", coverage(p.coverage()),
                "uncertainties", array(p.uncertainties(), this::uncertainty),
                "premises", empty(p.premises(), "$.publication.premises")));
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
                "objects", empty(u.objects(), "$.publication.units.objects"),
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
        if (!(r instanceof Interactions.NoRemainder)) throw limit("$.signature.remainder", "UnknownBound.unknown not implemented");
        return object("kind", "none");
    }
    private Value sequence(Sequence s) {
        return object("label", id(s.label()), "instructions", empty(s.instructions(), "$.sequence.instructions"),
                "terminator", operation(s.terminator()), "origin", id(s.origin()));
    }
    private Value operation(Terminator t) {
        if (!(t instanceof Operations.Return r)) throw limit("$.sequence.terminator", "Operation " + t.kind() + " not implemented");
        return object("kind", "return", "header", header(r.header()), "values", empty(r.values(), "$.return.values"));
    }
    private Value header(Operations.Header h) {
        return object("id", id(h.id()), "origin", id(h.origin()), "coverage", coverageStatus(h.coverage()),
                "precision", precision(h.precision()), "uncertainties", array(h.uncertainties(), this::id));
    }
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
