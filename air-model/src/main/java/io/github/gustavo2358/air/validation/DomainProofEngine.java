package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.model.Proofs.*;
import io.github.gustavo2358.air.model.Types.*;
import java.math.BigInteger;
import java.util.*;

/**
 * Decides finite sameDomain derivability from declarations and scoped premises.
 * UnknownType IDs are never unification keys. No validation certificate is an AIR fact.
 */
final class DomainProofEngine {
    private sealed interface Key permits SubjectKey, TypeKey {}
    private record SubjectKey(DomainSubject subject) implements Key {}
    private record TypeKey(Type type) implements Key {}
    private record Scoped(Proofs.Premise premise,SameDomain assertion,ScopeRules.Region scope) {}

    private final ValidationContext c;
    private final TypeResolver types;
    final ScopeRules scopes;
    private final Graph global = new Graph();
    private final Set<DomainSubject> registered=new HashSet<>();
    private final Map<ScopeRules.Region,List<Scoped>> byScope=new LinkedHashMap<>();
    private final Map<DomainSubject,List<Scoped>> bySubject=new LinkedHashMap<>();
    private final Map<ScopeRules.Region,Graph> regionGraphs=new HashMap<>();
    private final Map<EntryId,Map<BigInteger,TypeRef>> entryParameterTypes=new HashMap<>();
    private final Map<EntryId,Map<BigInteger,TypeRef>> entryResultTypes=new HashMap<>();
    private final Map<OperationId,Map<BigInteger,TypeRef>> invocationParameterTypes=new HashMap<>();
    private final Map<OperationId,Map<BigInteger,TypeRef>> invocationResultTypes=new HashMap<>();

    DomainProofEngine(ValidationContext c,TypeResolver types) {
        this.c=c; this.types=types; this.scopes=new ScopeRules(c);
    }

    void initialize() {
        for(ObjectId id:c.index.objects.keySet()) register(new ObjectDomain(id));
        for(Memory.Storage s:c.index.storage.values())
            if(s instanceof Memory.Cell cell) register(new CellDomain(cell.header().id()));
        for(OperandId id:c.index.operands.keySet()) register(new OperandDomain(id));

        for(Entries.Entry entry:c.index.entries.values()) {
            Map<BigInteger,TypeRef> parameters=slotMap(entry.signature().parameters().known());
            Map<BigInteger,TypeRef> results=resultSlotMap(entry.signature().results().known());
            entryParameterTypes.put(entry.id(),parameters);
            entryResultTypes.put(entry.id(),results);
            for(BigInteger position:parameters.keySet()) register(new ParameterDomain(entry.id(),position));
            for(BigInteger position:results.keySet()) register(new ResultDomain(entry.id(),position));
        }
        for(Operation operation:c.index.operations.values()) if(operation instanceof Operations.Invoke invoke) {
            SignatureSlots slots=slots(invoke.signature());
            invocationParameterTypes.put(invoke.header().id(),slots.parameters());
            invocationResultTypes.put(invoke.header().id(),slots.results());
            switch(invoke.signature()) {
                case Interactions.EntrySignature entry -> {
                    for(BigInteger position:slots.parameters().keySet())
                        register(new CallParameterDomain(invoke.header().id(),entry.entry(),position));
                    for(BigInteger position:slots.results().keySet())
                        register(new CallResultDomain(invoke.header().id(),entry.entry(),position));
                }
                case Interactions.ExternalSignature ignored -> {
                    for(BigInteger position:slots.parameters().keySet())
                        register(new ExternalParameterDomain(invoke.header().id(),position));
                    for(BigInteger position:slots.results().keySet())
                        register(new ExternalResultDomain(invoke.header().id(),position));
                }
            }
        }

        for(Memory.ObjectDeclaration object:c.index.objects.values()) {
            if(object.storage() instanceof Memory.CellBinding binding
                    && c.index.storage.get(binding.storage()) instanceof Memory.Cell cell
                    && TypeResolver.sameRef(object.typeRef(),cell.typeRef()))
                link(global,new ObjectDomain(object.id()),new CellDomain(cell.header().id()),object.id());
            if(object.storage() instanceof Memory.AliasBinding binding
                    && c.index.objects.containsKey(binding.object())
                    && TypeResolver.sameRef(object.typeRef(),c.index.objects.get(binding.object()).typeRef()))
                link(global,new ObjectDomain(object.id()),new ObjectDomain(binding.object()),object.id());
        }
        for(Operand operand:c.index.operands.values()) {
            if(operand instanceof Places.ObjectPlace place && c.index.objects.containsKey(place.object()))
                link(global,new OperandDomain(place.header().id()),new ObjectDomain(place.object()),place.header().id());
            if(operand instanceof Expressions.Read read)
                link(global,new OperandDomain(read.header().id()),new OperandDomain(read.place().header().id()),read.header().id());
        }

        for(Proofs.Premise premise:c.index.premises.values()) {
            if(!(premise.assertion() instanceof SameDomain assertion)) continue;
            checkSubject(assertion.left(),premise.id());
            checkSubject(assertion.right(),premise.id());
            ScopeRules.Region scope=scopes.scope(assertion.scope(),premise.id());
            Scoped entry=new Scoped(premise,assertion,scope);
            byScope.computeIfAbsent(scope,ignored -> new ArrayList<>()).add(entry);
            bySubject.computeIfAbsent(assertion.left(),ignored -> new ArrayList<>()).add(entry);
            bySubject.computeIfAbsent(assertion.right(),ignored -> new ArrayList<>()).add(entry);
            c.obligation("I-53",premise.id(),
                    "authority must substantiate sameDomain for every execution and choice remainder in its static scope");
        }
        for(Scoped premise:byScope.getOrDefault(ScopeRules.ALL,List.of())) apply(global,premise);
        for(ScopeRules.Region region:byScope.keySet()) {
            if(region.equals(ScopeRules.ALL) || region.equals(ScopeRules.EMPTY)) continue;
            graphForRegion(region);
        }
    }

    boolean same(DomainSubject left,DomainSubject right,ProofSite site) {
        c.domainQueries++;
        register(left); register(right);
        Graph base=graph(site);
        if(base.root(key(left)).equals(base.root(key(right)))) return true;
        Graph graph=new Graph(base);
        normalizeChoice(graph,left,0); normalizeChoice(graph,right,0);
        return graph.root(key(left)).equals(graph.root(key(right)));
    }

    boolean universalChoiceDomain(OperandId choice,Type domain,ProofSite site) {
        DomainSubject subject=new OperandDomain(choice);
        Graph graph=graph(site);
        Key expected=graph.root(new TypeKey(canonical(domain)));
        for(Scoped proof:bySubject.getOrDefault(subject,List.of())) {
            if(!scopes.applies(proof.scope(),site)) continue;
            DomainSubject other=proof.assertion().left().equals(subject)
                    ? proof.assertion().right() : proof.assertion().left();
            if(graph.root(key(other)).equals(expected)) return true;
        }
        return false;
    }

    Optional<Interactions.Signature> signature(Interactions.InvocationSignature target) {
        return switch(target) {
            case Interactions.EntrySignature entry -> Optional.ofNullable(c.index.entries.get(entry.entry()))
                    .map(Entries.Entry::signature);
            case Interactions.ExternalSignature external -> Optional.of(external.signature());
        };
    }

    private Graph graph(ProofSite site) {
        ScopeRules.Region region=switch(site) {
            case EntrySite entry -> ScopeRules.region(ScopeRules.Kind.ENTRY,entry.entry());
            case OperationSite operation -> ScopeRules.region(ScopeRules.Kind.OPERATION,operation.operation());
            case InvocationSite invocation -> ScopeRules.region(ScopeRules.Kind.INVOCATION,invocation.invocation());
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
        Graph graph=new Graph(parent);
        for(Scoped proof:list) apply(graph,proof);
        regionGraphs.put(region,graph);
        return graph;
    }

    private void apply(Graph graph,Scoped proof) {
        link(graph,proof.assertion().left(),proof.assertion().right(),proof.premise().id());
        expandUniversal(graph,proof.assertion().left(),proof.premise().id(),0);
        expandUniversal(graph,proof.assertion().right(),proof.premise().id(),0);
    }

    private void expandUniversal(Graph graph,DomainSubject subject,Id premise,int depth) {
        c.depth(depth);
        if(!(subject instanceof OperandDomain domain)) return;
        Operand operand=c.index.operands.get(domain.operand());
        if(operand instanceof Expressions.Read read)
            expandUniversal(graph,new OperandDomain(read.place().header().id()),premise,depth+1);
        if(operand instanceof Places.Choice choice) for(Place candidate:choice.candidates()) {
            DomainSubject candidateDomain=new OperandDomain(candidate.header().id());
            link(graph,subject,candidateDomain,premise);
            expandUniversal(graph,candidateDomain,premise,depth+1);
        }
    }

    private void normalizeChoice(Graph graph,DomainSubject subject,int depth) {
        c.depth(depth);
        if(!(subject instanceof OperandDomain domain)) return;
        Operand operand=c.index.operands.get(domain.operand());
        if(operand instanceof Expressions.Read read) {
            normalizeChoice(graph,new OperandDomain(read.place().header().id()),depth+1);
            return;
        }
        if(!(operand instanceof Places.Choice choice)
                || !(choice.remainder() instanceof Scopes.NoMemory) || choice.candidates().isEmpty()) return;
        for(Place candidate:choice.candidates())
            normalizeChoice(graph,new OperandDomain(candidate.header().id()),depth+1);
        Key first=graph.root(key(new OperandDomain(choice.candidates().get(0).header().id())));
        if(choice.candidates().stream().allMatch(candidate ->
                graph.root(key(new OperandDomain(candidate.header().id()))).equals(first)))
            graph.union(key(subject),first);
    }

    private void register(DomainSubject subject) {
        if(!registered.add(subject)) return;
        global.ensure(key(subject));
        type(subject).filter(Known.class::isInstance).map(Known.class::cast)
                .ifPresent(known -> global.union(key(subject),new TypeKey(canonical(known.type()))));
    }

    Optional<TypeRef> type(DomainSubject subject) {
        return switch(subject) {
            case ObjectDomain object -> Optional.ofNullable(c.index.objects.get(object.object()))
                    .map(Memory.ObjectDeclaration::typeRef);
            case CellDomain storage -> c.index.storage.get(storage.cell()) instanceof Memory.Cell cell
                    ? Optional.of(cell.typeRef()) : Optional.empty();
            case OperandDomain operand -> Optional.ofNullable(c.index.operands.get(operand.operand())).flatMap(types::type);
            case ParameterDomain parameter -> slot(entryParameterTypes.get(parameter.entry()),parameter.position());
            case ResultDomain result -> slot(entryResultTypes.get(result.entry()),result.position());
            case CallParameterDomain parameter -> slot(invocationParameterTypes.get(parameter.invocation()),parameter.position());
            case CallResultDomain result -> slot(invocationResultTypes.get(result.invocation()),result.position());
            case ExternalParameterDomain parameter -> slot(invocationParameterTypes.get(parameter.invocation()),parameter.position());
            case ExternalResultDomain result -> slot(invocationResultTypes.get(result.invocation()),result.position());
        };
    }

    private void checkSubject(DomainSubject subject,Id owner) {
        switch(subject) {
            case ObjectDomain object -> c.ref(object.object(),owner);
            case CellDomain storage -> {
                c.ref(storage.cell(),owner);
                if(!(c.index.storage.get(storage.cell()) instanceof Memory.Cell))
                    c.error("I-52",owner,"cell subject must name a cell");
            }
            case OperandDomain operand -> c.ref(operand.operand(),owner);
            case ParameterDomain parameter -> {
                c.ref(parameter.entry(),owner);
                if(type(subject).isEmpty()) c.error("I-52",owner,"unknown parameter slot");
            }
            case ResultDomain result -> {
                c.ref(result.entry(),owner);
                if(type(subject).isEmpty()) c.error("I-52",owner,"unknown result slot");
            }
            case CallParameterDomain parameter -> checkInternalCallSubject(
                    parameter.invocation(),parameter.entry(),type(subject),owner);
            case CallResultDomain result -> checkInternalCallSubject(
                    result.invocation(),result.entry(),type(subject),owner);
            case ExternalParameterDomain parameter -> checkExternalCallSubject(
                    parameter.invocation(),type(subject),owner);
            case ExternalResultDomain result -> checkExternalCallSubject(
                    result.invocation(),type(subject),owner);
        }
        register(subject);
    }

    private void checkInternalCallSubject(OperationId id,EntryId entry,Optional<TypeRef> slot,Id owner) {
        c.ref(id,owner); c.ref(entry,owner);
        if(!(c.index.operations.get(id) instanceof Operations.Invoke invoke)) {
            c.error("I-52",owner,"call subject requires invoke"); return;
        }
        boolean target=invoke.target() instanceof Interactions.InternalTarget internal
                && internal.entry().equals(entry);
        boolean signature=invoke.signature() instanceof Interactions.EntrySignature internal
                && internal.entry().equals(entry);
        if(!target || !signature || slot.isEmpty())
            c.error("I-52",owner,"internal signature subject is not materialized at that invocation and slot");
    }

    private void checkExternalCallSubject(OperationId id,Optional<TypeRef> slot,Id owner) {
        c.ref(id,owner);
        if(!(c.index.operations.get(id) instanceof Operations.Invoke invoke)) {
            c.error("I-52",owner,"call subject requires invoke"); return;
        }
        if(invoke.target() instanceof Interactions.InternalTarget
                || !(invoke.signature() instanceof Interactions.ExternalSignature) || slot.isEmpty())
            c.error("I-52",owner,"external signature subject is not materialized at that invocation and slot");
    }

    private SignatureSlots slots(Interactions.InvocationSignature target) {
        Optional<Interactions.Signature> signature=signature(target);
        if(signature.isEmpty()) return new SignatureSlots(Map.of(),Map.of());
        return new SignatureSlots(slotMap(signature.get().parameters().known()),
                resultSlotMap(signature.get().results().known()));
    }
    private static Map<BigInteger,TypeRef> slotMap(List<Interactions.Parameter> slots) {
        Map<BigInteger,TypeRef> result=new LinkedHashMap<>();
        for(Interactions.Parameter slot:slots) result.putIfAbsent(slot.position(),slot.typeRef());
        return Map.copyOf(result);
    }
    private static Map<BigInteger,TypeRef> resultSlotMap(List<Interactions.ResultSlot> slots) {
        Map<BigInteger,TypeRef> result=new LinkedHashMap<>();
        for(Interactions.ResultSlot slot:slots) result.putIfAbsent(slot.position(),slot.typeRef());
        return Map.copyOf(result);
    }
    private static Optional<TypeRef> slot(Map<BigInteger,TypeRef> slots,BigInteger position) {
        return slots==null ? Optional.empty() : Optional.ofNullable(slots.get(position));
    }
    private record SignatureSlots(Map<BigInteger,TypeRef> parameters,Map<BigInteger,TypeRef> results) {}

    private static Key key(DomainSubject subject) { return new SubjectKey(subject); }
    private static Type canonical(Type type) {
        if(type instanceof LabelType labels) return new LabelType(labels.unit(),labels.labels().stream()
                .sorted(Comparator.comparing(LabelId::localId)).toList());
        return type;
    }
    private void link(Graph graph,DomainSubject left,DomainSubject right,Id owner) {
        register(left); register(right);
        if(!graph.union(key(left),key(right)))
            c.error("I-52",owner,"sameDomain chain contradicts concrete domains");
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
            if(key instanceof TypeKey type) concrete.putIfAbsent(key,type.type());
            if(base!=null) {
                Type type=base.concrete.get(key);
                if(type!=null) concrete.putIfAbsent(key,type);
            }
            return key;
        }
        Key root(Key key) {
            key=ensure(key); Key root=key;
            while(!parents.get(root).equals(root)) root=parents.get(root);
            while(!key.equals(root)) {
                Key next=parents.get(key); parents.put(key,root); key=next;
            }
            return root;
        }
        boolean union(Key left,Key right) {
            Key x=root(left),y=root(right); if(x.equals(y)) return true;
            Type tx=concrete.get(x),ty=concrete.get(y);
            boolean valid=tx==null || ty==null || TypeResolver.sameType(tx,ty);
            if(sizes.get(x)<sizes.get(y)) {
                Key swap=x; x=y; y=swap;
                Type typeSwap=tx; tx=ty; ty=typeSwap;
            }
            parents.put(y,x); sizes.put(x,sizes.get(x)+sizes.get(y));
            if(tx==null && ty!=null) concrete.put(x,ty);
            return valid;
        }
    }
}
