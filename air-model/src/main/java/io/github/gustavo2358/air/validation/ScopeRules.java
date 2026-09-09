package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.model.Proofs.*;
import java.util.*;

/** Finite static scope algebra from AIR 02 section 1.4; never consults a CFG or dynamic activation. */
final class ScopeRules {
    enum Kind { ALL, UNIT, ENTRY, OPERATION, INVOCATION, EMPTY }
    record Region(Kind kind, Optional<Id> id) {}
    static final Region ALL=new Region(Kind.ALL,Optional.empty()), EMPTY=new Region(Kind.EMPTY,Optional.empty());
    final ValidationContext c;
    ScopeRules(ValidationContext c) { this.c=c; }
    Region scope(DomainProofScope scope,Id owner) {
        Region[] result={ALL};
        Walk.run(scope,0,s -> s instanceof Intersection i ? List.of(i.left(),i.right()) : List.of(),
                new Walk.Visitor<DomainProofScope>() {
            public boolean enter(DomainProofScope s,long depth) {
                c.depth(depth);
                Region leaf=switch(s) {
                    case PublicationDomain ignored -> ALL;
                    case UnitDomain u -> { c.ref(u.unit(),owner); yield region(Kind.UNIT,u.unit()); }
                    case EntryDomain e -> { c.ref(e.entry(),owner); yield region(Kind.ENTRY,e.entry()); }
                    case OperationDomain o -> { c.ref(o.operation(),owner); yield region(Kind.OPERATION,o.operation()); }
                    case InvocationDomain i -> {
                        c.ref(i.invocation(),owner);
                        if(!(c.index.operations.get(i.invocation()) instanceof Operations.Invoke)) c.error("I-52",owner,"invocation scope requires an invoke");
                        yield region(Kind.INVOCATION,i.invocation());
                    }
                    case Intersection ignored -> ALL;
                };
                // Static scope intersection is associative; still visit every leaf for diagnostics.
                result[0]=intersect(result[0],leaf); return true;
            }
        });
        return result[0];
    }
    Region intersect(Region a,Region b) {
        if(a.kind()==Kind.EMPTY || b.kind()==Kind.EMPTY) return EMPTY;
        if(a.kind()==Kind.ALL) return b; if(b.kind()==Kind.ALL) return a;
        if(a.equals(b)) return a;
        if(a.kind()==Kind.UNIT) return ownerUnit(b).filter(a.id().orElseThrow()::equals).isPresent()?b:EMPTY;
        if(b.kind()==Kind.UNIT) return intersect(b,a);
        if(a.kind()==Kind.OPERATION && b.kind()==Kind.INVOCATION && a.id().equals(b.id())) return b;
        if(b.kind()==Kind.OPERATION && a.kind()==Kind.INVOCATION && a.id().equals(b.id())) return a;
        return EMPTY;
    }
    boolean applies(Region region,ProofSite site) {
        if(region.kind()==Kind.ALL) return true;
        if(region.kind()==Kind.EMPTY) return false;
        return switch(site) {
            case EntrySite e -> region.equals(region(Kind.ENTRY,e.entry())) || region.equals(region(Kind.UNIT,e.entry().unit()));
            case OperationSite o -> region.equals(region(Kind.OPERATION,o.operation())) || region.equals(region(Kind.UNIT,o.operation().unit()));
            case InvocationSite i -> region.equals(region(Kind.INVOCATION,i.invocation())) || region.equals(region(Kind.OPERATION,i.invocation())) || region.equals(region(Kind.UNIT,i.invocation().unit()));
        };
    }
    List<Region> matching(ProofSite site) {
        List<Region> list=new ArrayList<>(); list.add(ALL);
        switch(site) {
            case EntrySite e -> { list.add(region(Kind.UNIT,e.entry().unit())); list.add(region(Kind.ENTRY,e.entry())); }
            case OperationSite o -> { list.add(region(Kind.UNIT,o.operation().unit())); list.add(region(Kind.OPERATION,o.operation())); }
            case InvocationSite i -> { list.add(region(Kind.UNIT,i.invocation().unit())); list.add(region(Kind.OPERATION,i.invocation())); list.add(region(Kind.INVOCATION,i.invocation())); }
        }
        return List.copyOf(list);
    }
    private Optional<UnitId> ownerUnit(Region region) {
        if(region.id().isEmpty()) return Optional.empty();
        Id id=region.id().get();
        if(id instanceof UnitId u) return Optional.of(u);
        if(id instanceof EntryId e) return Optional.of(e.unit());
        if(id instanceof OperationId o) return Optional.of(o.unit());
        return Optional.empty();
    }
    static Region region(Kind kind,Id id) { return new Region(kind,Optional.of(id)); }
}
