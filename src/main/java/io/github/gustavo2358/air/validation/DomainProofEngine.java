package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.model.Proofs.*;
import io.github.gustavo2358.air.model.Types.*;
import java.util.*;

/**
 * Decides finite sameDomain derivability from declarations and scoped premises.
 * UnknownType IDs are NEVER union-find keys. The engine does not infer value equality,
 * aliasing, purity, reachability, or the external truth of a premise.
 */
final class DomainProofEngine {
    private sealed interface Key permits SubjectKey, TypeKey {}
    private record SubjectKey(DomainSubject subject) implements Key {}
    private record TypeKey(Type type) implements Key {}
    private record Scoped(Proofs.Premise premise,SameDomain assertion,ScopeRules.Region scope) {}
    private record Safety(Proofs.Premise premise,SafetyAssertion assertion,ScopeRules.Region scope) {}
    private final ValidationContext c;
    private final TypeResolver types;
    final ScopeRules scopes;
    private final Graph global = new Graph();
    private final Set<DomainSubject> registered=new HashSet<>();
    private final Map<ScopeRules.Region,List<Scoped>> byScope=new LinkedHashMap<>();
    private final Map<Id,List<Safety>> safetyBySubject=new LinkedHashMap<>();
    private final Map<ScopeRules.Region,Graph> regionGraphs=new HashMap<>();
    private final Map<Interactions.SignatureTarget,Map<Integer,TypeRef>> parameterTypes=new HashMap<>();
    private final Map<Interactions.SignatureTarget,Map<Integer,TypeRef>> resultTypes=new HashMap<>();

    DomainProofEngine(ValidationContext c,TypeResolver types) {
        this.c=c; this.types=types; this.scopes=new ScopeRules(c);
    }
    void initialize() {
        for(ObjectId id:c.index.objects.keySet()) register(new ObjectDomain(id));
        for(Memory.Storage s:c.index.storage.values()) if(s instanceof Memory.Cell cell) register(new CellDomain(cell.header().id()));
        for(OperandId id:c.index.operands.keySet()) register(new OperandDomain(id));
        for(Entries.Entry e:c.index.entries.values()) {
            for(Interactions.Parameter p:e.signature().parameters()) register(new ParameterDomain(e.id(),p.position()));
            for(Interactions.ResultSlot r:e.signature().results()) register(new ResultDomain(e.id(),r.position()));
        }
        for(Memory.ObjectDeclaration o:c.index.objects.values()) {
            if(o.storage() instanceof Memory.CellBinding b && c.index.storage.get(b.storage()) instanceof Memory.Cell cell
                    && TypeResolver.sameRef(o.typeRef(),cell.typeRef()))
                link(global,new ObjectDomain(o.id()),new CellDomain(cell.header().id()),o.id());
            if(o.storage() instanceof Memory.AliasBinding b && c.index.objects.containsKey(b.object())
                    && TypeResolver.sameRef(o.typeRef(),c.index.objects.get(b.object()).typeRef()))
                link(global,new ObjectDomain(o.id()),new ObjectDomain(b.object()),o.id());
        }
        for(Operand operand:c.index.operands.values()) {
            if(operand instanceof Places.ObjectPlace p && c.index.objects.containsKey(p.object()))
                link(global,new OperandDomain(p.header().id()),new ObjectDomain(p.object()),p.header().id());
            if(operand instanceof Expressions.Read r)
                link(global,new OperandDomain(r.header().id()),new OperandDomain(r.place().header().id()),r.header().id());
        }
        for(Proofs.Premise p:c.index.premises.values()) {
            if(p.assertion() instanceof SameDomain sd) {
                checkSubject(sd.left(),p.id()); checkSubject(sd.right(),p.id());
                ScopeRules.Region scope=scopes.scope(sd.scope(),p.id());
                Scoped entry=new Scoped(p,sd,scope);
                byScope.computeIfAbsent(scope,ignored -> new ArrayList<>()).add(entry);
                c.obligation("I-53",p.id(),"authority must substantiate sameDomain for all executions and choice remainder in its static scope");
            } else if(p.assertion() instanceof SafetyAssertion sa) {
                c.refs(sa.subjects(),p.id());
                ScopeRules.Region region=scopes.scope(sa.scope(),p.id());
                for(Id subject:sa.subjects()) safetyBySubject.computeIfAbsent(subject,ignored->new ArrayList<>()).add(new Safety(p,sa,region));
                c.obligation("I-09/I-46",p.id(),"truth of safety/domain evidence is a producer obligation, not certified by identity checks");
            }
        }
        for(Scoped p:byScope.getOrDefault(ScopeRules.ALL,List.of())) apply(global,p);
        // Check contradictions even when no assign uses the premise. Empty scope is vacuous.
        for(ScopeRules.Region region:byScope.keySet()) {
            if(region.equals(ScopeRules.ALL) || region.equals(ScopeRules.EMPTY)) continue;
            graphForRegion(region);
        }
    }
    boolean same(DomainSubject a,DomainSubject b,ProofSite site) {
        c.domainQueries++;
        register(a); register(b);
        Graph base=graph(site);
        if(base.root(key(a)).equals(base.root(key(b)))) return true;
        // Choice-derived relationships are scoped to this query. Never mutate a cached parent graph.
        Graph graph=new Graph(base);
        normalizeChoice(graph,a,0); normalizeChoice(graph,b,0);
        return graph.root(key(a)).equals(graph.root(key(b)));
    }
    boolean safety(SafetyProperty property,Id subject,ProofSite site) {
        return safetyBySubject.getOrDefault(subject,List.of()).stream().anyMatch(s ->
                s.assertion().property()==property && scopes.applies(s.scope(),site));
    }
    boolean safety(PremiseId id,SafetyProperty property,Id subject,ProofSite site) {
        c.ref(id,subject);
        return safetyBySubject.getOrDefault(subject,List.of()).stream().anyMatch(s ->
                s.premise().id().equals(id) && s.assertion().property()==property && scopes.applies(s.scope(),site));
    }
    private Graph graph(ProofSite site) {
        ScopeRules.Region region=switch(site) {
            case EntrySite e -> ScopeRules.region(ScopeRules.Kind.ENTRY,e.entry());
            case OperationSite o -> ScopeRules.region(ScopeRules.Kind.OPERATION,o.operation());
            case InvocationSite i -> ScopeRules.region(ScopeRules.Kind.INVOCATION,i.invocation());
        };
        return graphForRegion(region);
    }
    private Graph graphForRegion(ScopeRules.Region region) {
        if(region.equals(ScopeRules.ALL) || region.equals(ScopeRules.EMPTY)) return global;
        Graph cached=regionGraphs.get(region); if(cached!=null) return cached;
        Id id=region.id().orElseThrow();
        Graph parent=global;
        if(region.kind()==ScopeRules.Kind.INVOCATION)
            parent=graphForRegion(ScopeRules.region(ScopeRules.Kind.OPERATION,id));
        else if(region.kind()==ScopeRules.Kind.OPERATION)
            parent=graphForRegion(ScopeRules.region(ScopeRules.Kind.UNIT,((OperationId)id).unit()));
        else if(region.kind()==ScopeRules.Kind.ENTRY)
            parent=graphForRegion(ScopeRules.region(ScopeRules.Kind.UNIT,((EntryId)id).unit()));
        List<Scoped> list=byScope.getOrDefault(region,List.of());
        if(list.isEmpty()) { regionGraphs.put(region,parent); return parent; }
        Graph graph=new Graph(parent); for(Scoped proof:list) apply(graph,proof);
        regionGraphs.put(region,graph); return graph;
    }
    private void apply(Graph graph,Scoped p) {
        link(graph,p.assertion().left(),p.assertion().right(),p.premise().id());
        expandUniversal(graph,p.assertion().left(),p.premise().id(),0);
        expandUniversal(graph,p.assertion().right(),p.premise().id(),0);
    }
    private void expandUniversal(Graph graph,DomainSubject subject,Id premise,int depth) {
        c.depth(depth);
        if(subject instanceof OperandDomain d) {
            Operand operand=c.index.operands.get(d.operand());
            if(operand instanceof Expressions.Read r) expandUniversal(graph,new OperandDomain(r.place().header().id()),premise,depth+1);
            if(operand instanceof Places.Choice choice) for(Place candidate:choice.candidates()) {
                DomainSubject cd=new OperandDomain(candidate.header().id());
                link(graph,subject,cd,premise); expandUniversal(graph,cd,premise,depth+1);
            }
        }
    }
    private void normalizeChoice(Graph graph,DomainSubject subject,int depth) {
        c.depth(depth);
        if(!(subject instanceof OperandDomain d)) return;
        Operand operand=c.index.operands.get(d.operand());
        if(operand instanceof Expressions.Read r) { normalizeChoice(graph,new OperandDomain(r.place().header().id()),depth+1); return; }
        if(!(operand instanceof Places.Choice choice) || !(choice.remainder() instanceof Scopes.NoMemory) || choice.candidates().isEmpty()) return;
        for(Place p:choice.candidates()) normalizeChoice(graph,new OperandDomain(p.header().id()),depth+1);
        Key first=graph.root(key(new OperandDomain(choice.candidates().get(0).header().id())));
        if(choice.candidates().stream().allMatch(p->graph.root(key(new OperandDomain(p.header().id()))).equals(first)))
            graph.union(key(subject),first);
    }
    private void register(DomainSubject subject) {
        if(!registered.add(subject)) return;
        global.ensure(key(subject));
        type(subject).filter(Known.class::isInstance).map(Known.class::cast)
                .ifPresent(k -> global.union(key(subject),new TypeKey(canonical(k.type()))));
    }
    Optional<TypeRef> type(DomainSubject subject) {
        return switch(subject) {
            case ObjectDomain o -> Optional.ofNullable(c.index.objects.get(o.object())).map(Memory.ObjectDeclaration::typeRef);
            case CellDomain s -> c.index.storage.get(s.cell()) instanceof Memory.Cell cell ? Optional.of(cell.typeRef()):Optional.empty();
            case OperandDomain o -> Optional.ofNullable(c.index.operands.get(o.operand())).flatMap(types::type);
            case ParameterDomain p -> slot(new Interactions.EntrySignature(p.entry()),p.position(),false);
            case ResultDomain r -> slot(new Interactions.EntrySignature(r.entry()),r.position(),true);
            case CallParameterDomain p -> slot(p.signature(),p.position(),false);
            case CallResultDomain r -> slot(r.signature(),r.position(),true);
        };
    }
    Optional<Interactions.Signature> signature(Interactions.SignatureTarget target) {
        return switch(target) {
            case Interactions.EntrySignature e -> Optional.ofNullable(c.index.entries.get(e.entry())).map(Entries.Entry::signature);
            case Interactions.ExternalSignature x -> Optional.ofNullable(c.index.contracts.get(x.contract())).map(Interactions.Contract::signature);
        };
    }
    private Optional<TypeRef> slot(Interactions.SignatureTarget target,int position,boolean result) {
        Map<Interactions.SignatureTarget,Map<Integer,TypeRef>> cache=result?resultTypes:parameterTypes;
        Map<Integer,TypeRef> slots=cache.get(target);
        if(slots==null) {
            slots=new HashMap<>(); Optional<Interactions.Signature> sig=signature(target);
            if(sig.isPresent()) {
                if(result) for(Interactions.ResultSlot r:sig.get().results()) slots.putIfAbsent(r.position(),r.typeRef());
                else for(Interactions.Parameter p:sig.get().parameters()) slots.putIfAbsent(p.position(),p.typeRef());
            }
            cache.put(target,slots);
        }
        return Optional.ofNullable(slots.get(position));
    }
    private void checkSubject(DomainSubject subject,Id owner) {
        switch(subject) {
            case ObjectDomain o -> c.ref(o.object(),owner);
            case CellDomain s -> { c.ref(s.cell(),owner); if(!(c.index.storage.get(s.cell()) instanceof Memory.Cell)) c.error("I-52",owner,"cell subject must name a cell"); }
            case OperandDomain o -> c.ref(o.operand(),owner);
            case ParameterDomain p -> { c.ref(p.entry(),owner); if(type(subject).isEmpty()) c.error("I-52",owner,"unknown parameter slot"); }
            case ResultDomain r -> { c.ref(r.entry(),owner); if(type(subject).isEmpty()) c.error("I-52",owner,"unknown result slot"); }
            case CallParameterDomain p -> checkCallSubject(p.invocation(),p.signature(),type(subject),owner);
            case CallResultDomain r -> checkCallSubject(r.invocation(),r.signature(),type(subject),owner);
        }
        register(subject);
    }
    private void checkCallSubject(OperationId id,Interactions.SignatureTarget signature,Optional<TypeRef> slot,Id owner) {
        c.ref(id,owner);
        if(!(c.index.operations.get(id) instanceof Operations.Invoke invoke)) { c.error("I-52",owner,"call subject requires invoke"); return; }
        boolean linked=switch(signature) {
            case Interactions.EntrySignature e -> invoke.target() instanceof Interactions.InternalTarget i && i.entry().equals(e.entry());
            case Interactions.ExternalSignature x -> invoke.contract().contract().filter(x.contract()::equals).isPresent();
        };
        if(!linked || slot.isEmpty()) c.error("I-52",owner,"signature subject is not bound to that invocation and slot");
    }
    private static Key key(DomainSubject s) { return new SubjectKey(s); }
    private static Type canonical(Type t) {
        if(t instanceof LabelType l) return new LabelType(l.unit(),l.labels().stream().sorted(Comparator.comparing(LabelId::localId)).toList());
        return t;
    }
    private void link(Graph graph,DomainSubject a,DomainSubject b,Id owner) {
        register(a); register(b);
        if(!graph.union(key(a),key(b))) c.error("I-52",owner,"sameDomain chain contradicts concrete domains");
    }
    private static final class Graph {
        private final Map<Key,Key> parents=new HashMap<>();
        private final Map<Key,Integer> sizes=new HashMap<>();
        private final Map<Key,Type> concrete=new HashMap<>();
        private final Graph base;
        Graph() { this.base=null; }
        Graph(Graph base) { this.base=base; }
        Key ensure(Key key) {
            if(base!=null) key=base.root(key);
            parents.putIfAbsent(key,key); sizes.putIfAbsent(key,1);
            if(key instanceof TypeKey t) concrete.putIfAbsent(key,t.type());
            if(base!=null) { Type t=base.concrete.get(key); if(t!=null) concrete.putIfAbsent(key,t); }
            return key;
        }
        Key root(Key key) {
            key=ensure(key); Key r=key;
            while(!parents.get(r).equals(r)) r=parents.get(r);
            while(!key.equals(r)) { Key next=parents.get(key); parents.put(key,r); key=next; }
            return r;
        }
        boolean union(Key a,Key b) {
            Key x=root(a),y=root(b); if(x.equals(y)) return true;
            Type tx=concrete.get(x),ty=concrete.get(y);
            boolean ok=tx==null || ty==null || TypeResolver.sameType(tx,ty);
            if(sizes.get(x)<sizes.get(y)) { Key z=x;x=y;y=z; Type zt=tx;tx=ty;ty=zt; }
            parents.put(y,x); sizes.put(x,sizes.get(x)+sizes.get(y));
            if(tx==null && ty!=null) concrete.put(x,ty);
            return ok;
        }
    }
}
