package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;

final class ValidationContext {
    final ValidationOptions options;
    final List<ValidationIssue> issues=new ArrayList<>();
    final PublicationIndex index;
    long domainQueries;
    final Map<ValidationIssue.Kind,Long> issueCounts=new EnumMap<>(ValidationIssue.Kind.class);
    boolean traversalCompleted=true;
    final Set<Capabilities.Capability> required;
    ValidationContext(Publication p,ValidationOptions options) { this.options=options; this.index=new PublicationIndex(p,this); this.required=new HashSet<>(p.capabilities().required()); }
    void depth(long depth) { if(depth>options.maximumNesting()) throw new Limit("nesting limit"); }
    void error(String rule,Id id,String message) { issue(ValidationIssue.Kind.INVALID_IR,rule,id,message); }
    void obligation(String rule,Id id,String message) { issue(ValidationIssue.Kind.SEMANTIC_OBLIGATION,rule,id,message); }
    void unsupported(String rule,Id id,String message) { issue(ValidationIssue.Kind.UNSUPPORTED_CAPABILITY,rule,id,message); }
    void issue(ValidationIssue.Kind kind,String rule,Id id,String message) {
        count(kind);
        if(issues.size()<options.maximumIssues())
            issues.add(new ValidationIssue(kind,rule,Optional.ofNullable(id),message));
    }
    private void count(ValidationIssue.Kind kind) {
        try { issueCounts.merge(kind,1L,Math::addExact); }
        catch(ArithmeticException overflow) { throw new Limit("diagnostic counter representability"); }
    }
    void resourceLimit(String message) {
        traversalCompleted=false;
        count(ValidationIssue.Kind.RESOURCE_LIMIT);
        // One mandatory operational marker is retained outside the message budget.
        issues.add(new ValidationIssue(ValidationIssue.Kind.RESOURCE_LIMIT,"ANALYSIS_LIMIT",
                Optional.of(index.publication.id()),message));
    }
    void query() {
        if(domainQueries==Long.MAX_VALUE) throw new Limit("query counter representability");
        domainQueries++;
    }
    void ref(Id id,Id owner) {
        if(!index.identities.contains(id)) error("I-02",owner,"dangling reference: "+id);
    }
    void refs(Iterable<? extends Id> ids,Id owner) { for(Id id:ids) ref(id,owner); }
    void uncertainty(UncertaintyId id,String requiredCode,Id owner) {
        ref(id,owner); Evidence.Uncertainty u=index.uncertainties.get(id);
        if(u!=null && requiredCode!=null && !u.code().equals(requiredCode))
            error("I-49",owner,"expected "+requiredCode+" uncertainty, got "+u.code());
    }
    void type(Types.TypeRef ref,Id owner) {
        switch(ref) {
            case Types.UnknownType u -> uncertainty(u.uncertainty(),"TYPE_UNKNOWN",owner);
            case Types.Known k -> {
                if(k.type() instanceof Types.LabelType l) {
                    ref(l.unit(),owner); refs(l.labels(),owner);
                    for(LabelId label:l.labels()) if(!label.unit().equals(l.unit())) error("I-08",owner,"label universe crosses unit");
                    capability(Capabilities.INDIRECT_CONTROL,owner);
                } else if(k.type() instanceof Types.ExtensionType e) {
                    if(e.name().equals("unknown")) error("I-50",owner,"opaque_type cannot disguise unknown_type");
                    capability(new Capabilities.Capability(e.name(),e.version()),owner);
                }
            }
        }
    }
    void capability(Capabilities.Capability required,Id owner) {
        if(!this.required.contains(required))
            error("I-43",owner,"used capability missing from required manifest: "+required);
    }
    static final class Limit extends RuntimeException {
        private static final long serialVersionUID=1L;
        Limit(String message) { super(message); }
    }
}
