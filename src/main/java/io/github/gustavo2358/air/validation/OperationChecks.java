package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.model.Proofs.*;
import io.github.gustavo2358.air.model.Types.*;
import java.math.BigInteger;
import java.util.*;

/** Checks static operation signatures and proof obligations, not the results of analyses. */
final class OperationChecks {
    final ValidationContext c;
    final ReferenceChecks refs;
    final TypeResolver types;
    final DomainProofEngine domains;
    private final Map<ObjectId,Optional<Memory.Binding>> bindingCache=new HashMap<>();
    OperationChecks(ValidationContext c,ReferenceChecks refs,TypeResolver types,DomainProofEngine domains) {
        this.c=c; this.refs=refs; this.types=types; this.domains=domains;
    }
    void run() {
        for(Operand operand:c.index.operands.values()) { types.type(operand); checkOperand(operand); }
        for(Operation op:c.index.operations.values()) operation(op);
        for(Entries.Entry entry:c.index.entries.values()) entry(entry);
    }
    private void checkOperand(Operand operand) {
        OperandId id=operand.header().id(); ProofSite site=site(id);
        if(operand instanceof Places.Choice choice) {
            List<Optional<TypeRef>> candidateTypes=choice.candidates().stream().map(types::type).toList();
            boolean allKnown=!candidateTypes.isEmpty() && candidateTypes.stream().allMatch(t->t.isPresent() && t.get() instanceof Known);
            boolean sameKnown=allKnown && candidateTypes.stream().allMatch(t->TypeResolver.sameKnown(candidateTypes.get(0),t));
            if(choice.typeRef() instanceof Known k) {
                for(Optional<TypeRef> t:candidateTypes) types.expect(t,k.type(),id);
                if(choice.remainder() instanceof Scopes.WithinMemory && (choice.knownRemainderDomainProof().isEmpty()
                        || !domains.safety(choice.knownRemainderDomainProof().get(),SafetyProperty.CHOICE_REMAINDER_DOMAIN,id,site)))
                    c.error("I-51",id,"known choice domain requires a scoped guarantee covering the open remainder");
            } else if(choice.remainder() instanceof Scopes.NoMemory && sameKnown) c.error("I-51",id,"closed homogeneous known choice must preserve known domain");
        } else if(operand instanceof Expressions.SliceText slice) {
            if(slice.value() instanceof Expressions.Literal v && v.value() instanceof Values.TextValue text
                    && integer(slice.start()).isPresent() && integer(slice.count()).isPresent()) {
                BigInteger from=integer(slice.start()).orElseThrow(), count=integer(slice.count()).orElseThrow();
                if(from.signum()<0 || count.signum()<0 || from.add(count).compareTo(BigInteger.valueOf(text.value().codePointCount(0,text.value().length())))>0)
                    c.error("I-09/I-46",id,"literal text slice is out of bounds");
            } else if(slice.boundsProof().isEmpty() || !domains.safety(slice.boundsProof().get(),SafetyProperty.VALID_TEXT_SLICE,id,site))
                limit(id,"text slice totality requires a materialized applicable bounds premise");
        } else if(operand instanceof Places.RegionSlice region) {
            types.expect(types.type(region.offset()),Builtin.INT,id); types.expect(types.type(region.length()),Builtin.INT,id);
            checkRange(region.region(),region.offset(),region.length(),region.accessProof(),id,site);
        } else if(operand instanceof Expressions.Read read) {
            Memory.Codec codec=codec(read.place());
            if(codec instanceof Memory.UnknownCodec || codec instanceof Memory.ExtensionCodec || codec instanceof Memory.AsciiText)
                if(!domains.safety(SafetyProperty.VALID_PURE_ACCESS,read.place().header().id(),site)
                        && !domains.safety(SafetyProperty.VALID_PURE_ACCESS,id,site))
                    limit(id,"read through non-total/unknown codec requires a scoped pure-access premise");
        } else if(operand instanceof Expressions.Binary binary && (binary.operator()==Expressions.BinaryOperator.EQ || binary.operator()==Expressions.BinaryOperator.NE)) {
            Optional<TypeRef> t=types.type(binary.left());
            if(t.isPresent() && t.get() instanceof Known k && k.type() instanceof ExtensionType
                    && !domains.safety(SafetyProperty.EXTENSION_EQUALITY_DEFINED,id,site))
                c.error("I-08",id,"extension domain does not provide equality implicitly");
        }
    }
    private void operation(Operation op) {
        OperationId id=op.header().id(); OperationSite site=new OperationSite(id);
        switch(op) {
            case Operations.Assign a -> {
                role(a.destination(),Operand.Role.VALUE_WRITE); role(a.value(),Operand.Role.VALUE_READ);
                same(new OperandDomain(a.destination().header().id()),new OperandDomain(a.value().header().id()),site,id);
                codecWrite(a.destination(),a.value(),id);
            }
            case Operations.HavocMust h -> { role(h.destination(),Operand.Role.VALUE_WRITE); c.uncertainty(h.reason(),null,id); }
            case Operations.HavocMay h -> { refs.memory(h.scope(),id,0); c.uncertainty(h.reason(),null,id); }
            case Operations.Nop ignored -> { }
            case Operations.CopyBytes copy -> {
                c.capability(Capabilities.MEMORY_REGIONS,id); refs.envelope(copy.fallback(),id);
                checkRange(copy.source().region(),copy.source().offset(),copy.source().extent(),copy.source().boundsProof(),id,site);
                checkRange(copy.destination().region(),copy.destination().offset(),copy.destination().extent(),copy.destination().boundsProof(),id,site);
                for(Memory.ByteRange r:List.of(copy.source(),copy.destination())) {
                    types.expect(types.type(r.offset()),Builtin.INT,id); types.expect(types.type(r.extent()),Builtin.INT,id);
                    integer(r.extent()).ifPresent(n->{ if(copy.length().compareTo(n)>0) c.error("I-13",id,"copy length exceeds declared source/destination interval"); });
                }
            }
            case Operations.Jump j -> refs.label(j.destination(),id.unit(),id);
            case Operations.Branch b -> {
                role(b.predicate(),Operand.Role.PREDICATE); types.expect(types.type(b.predicate()),Builtin.BOOL,id);
                refs.label(b.trueDestination(),id.unit(),id); refs.label(b.falseDestination(),id.unit(),id);
            }
            case Operations.Dispatch d -> {
                Optional<TypeRef> type=types.type(d.selector());
                role(d.selector(),Operand.Role.CONTROL_TARGET);
                Set<java.lang.Object> keys=new HashSet<>();
                for(Operations.Case item:d.cases()) {
                    types.expect(type,item.value().type(),id); refs.label(item.destination(),id.unit(),id);
                    if(!keys.add(Values.semanticKey(item.value()))) c.error("I-20",id,"dispatch duplicates semantically equal literal");
                    if(item.value().type() instanceof LabelType) c.error("I-08",id,"dispatch does not admit label domain");
                }
                if(type.isEmpty() || !(type.get() instanceof Known k) || !(k.type() instanceof Builtin)) c.error("I-08",id,"dispatch requires a known core scalar domain");
                refs.label(d.defaultDestination(),id.unit(),id);
            }
            case Operations.Invoke invoke -> invocation(invoke);
            case Operations.Return ret -> returnOperation(ret);
            case Operations.Raise ignored -> { }
            case Operations.Halt ignored -> { }
            case Operations.Opaque opaque -> {
                refs.envelope(opaque.envelope(),id);
                if(op.header().uncertainties().isEmpty()) c.error("I-26",id,"opaque requires uncertainty references");
                for(Place result:opaque.valueResults()) role(result,Operand.Role.RESULT_TARGET);
            }
            case Operations.LocalInvoke l -> {
                c.capability(Capabilities.LOCAL_CONTROL,id); refs.label(l.entry(),id.unit(),id); refs.label(l.resume(),id.unit(),id);
                c.refs(l.completionPorts(),id); for(CompletionPortId port:l.completionPorts()) if(!port.unit().equals(id.unit())) c.error("I-02",id,"local completion port crosses unit");
                refs.envelope(l.fallback(),id);
            }
            case Operations.LocalBoundary l -> {
                c.capability(Capabilities.LOCAL_CONTROL,id); c.ref(l.port(),id);
                if(!l.port().unit().equals(id.unit())) c.error("I-02",id,"local completion port crosses unit");
                refs.label(l.defaultDestination(),id.unit(),id); refs.envelope(l.fallback(),id);
            }
            case Operations.LocalResume l -> { c.capability(Capabilities.LOCAL_CONTROL,id); refs.envelope(l.fallback(),id); }
            case Operations.LocalUnwind l -> { c.capability(Capabilities.LOCAL_CONTROL,id); refs.label(l.destination(),id.unit(),id); refs.envelope(l.fallback(),id); }
            case Operations.IndirectJump j -> {
                c.capability(Capabilities.INDIRECT_CONTROL,id); refs.envelope(j.fallback(),id); c.type(new Known(j.within()),id);
                if(!j.within().unit().equals(id.unit())) c.error("I-08",id,"indirect control universe crosses unit");
                role(j.target(),Operand.Role.CONTROL_TARGET); types.expect(types.type(j.target()),j.within(),id);
            }
        }
        // Computed targets inside dependency envelopes obey the same target type contract.
        Envelopes.Envelope envelope=fallback(op);
        if(envelope!=null) for(Envelopes.ResourceUse use:envelope.dependencies().known()) {
            if(use.target() instanceof Interactions.ComputedTarget t) types.expect(types.type(t.name()),Builtin.TEXT,id);
        }
    }
    private void invocation(Operations.Invoke invoke) {
        OperationId id=invoke.header().id(); InvocationSite site=new InvocationSite(id);
        refs.target(invoke.target(),id); refs.control(invoke.outcomes(),id.unit(),id); refs.effects(invoke.effectBound(),id,invoke);
        c.refs(invoke.signatureGaps(),id);
        invoke.contract().contract().ifPresent(contract->c.ref(contract,id)); invoke.contract().unknown().ifPresent(u->c.uncertainty(u,null,id));
        long normals=invoke.outcomes().known().stream().filter(Control.Normal.class::isInstance).count();
        if(normals>1) c.error("I-08",id,"invoke has more than one normal destination");
        if(normals==0 && !invoke.results().isEmpty()) c.error("I-08",id,"results require explicit normal outcome");
        Set<String> tags=new HashSet<>(); int any=0;
        for(Control.Alternative a:invoke.outcomes().known()) {
            if(a instanceof Control.Exceptional e && !tags.add(e.tag())) c.error("I-08",id,"duplicate invocation exception tag");
            if(a instanceof Control.AnyException && ++any>1) c.error("I-08",id,"duplicate catch-all outcome");
        }
        if(invoke.target() instanceof Interactions.ComputedTarget target) {
            role(target.name(),Operand.Role.CALL_TARGET); types.expect(types.type(target.name()),Builtin.TEXT,id);
        }
        for(Interactions.Argument arg:invoke.arguments()) switch(arg) {
            case Interactions.ValueArgument v -> role(v.value(),Operand.Role.ARGUMENT_VALUE);
            case Interactions.CopyArgument v -> role(v.value(),Operand.Role.ARGUMENT_VALUE);
            case Interactions.ReferenceArgument r -> role(r.place(),Operand.Role.ARGUMENT_REFERENCE);
        }
        for(Place result:invoke.results()) role(result,Operand.Role.RESULT_TARGET);
        Optional<Interactions.SignatureTarget> target=invoke.target() instanceof Interactions.InternalTarget i
                ?Optional.of(new Interactions.EntrySignature(i.entry()))
                :invoke.contract().contract().map(Interactions.ExternalSignature::new);
        Optional<Interactions.Signature> signature=target.flatMap(domains::signature);
        if(signature.isPresent()) {
            Interactions.Signature s=signature.get();
            if(s.incomplete().isEmpty() && invoke.signatureGaps().isEmpty()
                    && (invoke.arguments().size()!=s.parameters().size() || (normals>0 && invoke.results().size()!=s.results().size())))
                c.error("I-08",id,"call argument/result cardinalities contradict complete signature");
            for(Interactions.Parameter parameter:s.parameters()) {
                if(parameter.position()>=invoke.arguments().size()) continue;
                Interactions.Argument arg=invoke.arguments().get(parameter.position());
                Interactions.PassingMode actual=arg instanceof Interactions.ReferenceArgument?Interactions.PassingMode.REFERENCE:arg instanceof Interactions.CopyArgument?Interactions.PassingMode.COPY:Interactions.PassingMode.VALUE;
                if(actual!=parameter.mode()) c.error("I-08",id,"argument passing mode differs from signature");
                Operand operand=argumentOperand(arg);
                DomainSubject destination=new CallParameterDomain(id,target.orElseThrow(),parameter.position());
                if(!domains.same(new OperandDomain(operand.header().id()),destination,site)
                        && s.incomplete().isEmpty() && invoke.signatureGaps().isEmpty())
                    c.error("I-08/I-52",id,"argument transmission lacks sameDomain proof at invocation_site");
                rejectConcreteContradiction(types.type(operand),Optional.of(parameter.typeRef()),id);
            }
            for(Interactions.ResultSlot result:s.results()) {
                if(result.position()>=invoke.results().size()) continue;
                Place destination=invoke.results().get(result.position());
                if(!domains.same(new CallResultDomain(id,target.orElseThrow(),result.position()),new OperandDomain(destination.header().id()),site)
                        && s.incomplete().isEmpty() && invoke.signatureGaps().isEmpty())
                    c.error("I-08/I-52",id,"result transmission lacks sameDomain proof at invocation_site");
                rejectConcreteContradiction(types.type(destination),Optional.of(result.typeRef()),id);
            }
        }
        if(invoke.contract().unknown().isPresent())
            c.obligation("I-23",id,"declared bounds/outcomes need producer evidence; unknown contract does not imply effects=none or normal-only return");
    }
    private void returnOperation(Operations.Return ret) {
        OperationId id=ret.header().id();
        Unit unit=c.index.units.get(id.unit()); if(unit==null) return;
        List<EntryId> scope=ret.entryScope().isEmpty()?unit.entries().stream().map(Entries.Entry::id).toList():ret.entryScope();
        for(EntryId entryId:scope) {
            c.ref(entryId,id); if(!entryId.unit().equals(id.unit())) c.error("I-08",id,"return signature scope crosses unit");
            Entries.Entry entry=c.index.entries.get(entryId); if(entry==null) continue;
            Interactions.Signature signature=entry.signature();
            if(signature.incomplete().isEmpty() && signature.results().size()!=ret.values().size()) c.error("I-08",id,"return values contradict entry signature");
            for(Interactions.ResultSlot slot:signature.results()) if(slot.position()<ret.values().size())
                same(new OperandDomain(ret.values().get(slot.position()).header().id()),new ResultDomain(entryId,slot.position()),new OperationSite(id),id);
        }
        if(!ret.entryScope().isEmpty()) c.obligation("I-18",id,"producer must justify explicit entry scope of this return; validator does not compute reachability");
    }
    private void entry(Entries.Entry entry) {
        EntrySite site=new EntrySite(entry.id());
        for(Interactions.Parameter p:entry.signature().parameters()) if(p.object().isPresent()) {
            boolean hasProof=domains.same(new ObjectDomain(p.object().get()),new ParameterDomain(entry.id(),p.position()),site);
            if(!hasProof && entry.signature().incomplete().isEmpty()) c.error("I-08/I-52",entry.id(),"parameter/object binding lacks entry_site domain proof");
        }
        Map<StorageId,Values.LiteralValue> exactSeeds=new HashMap<>();
        for(Entries.InitialCondition seed:entry.state().conditions()) {
            DomainSubject destination=new OperandDomain(seed.place().header().id());
            switch(seed.value()) {
                case Entries.LiteralInitial l -> {
                    same(destination,new OperandDomain(l.value().header().id()),site,entry.id());
                    StorageId cell=exactCell(seed.place());
                    if(cell!=null) {
                        Values.LiteralValue previous=exactSeeds.putIfAbsent(cell,l.value().value());
                        if(previous!=null && !Values.semanticKey(previous).equals(Values.semanticKey(l.value().value()))) c.error("I-17",entry.id(),"conflicting literal initializers for same cell/alias");
                    } else if(codec(seed.place())!=null) limit(entry.id(),"overlapping region initializers require byte/codec consistency checks outside this validator slice");
                }
                case Entries.ParameterInitial p -> same(destination,new ParameterDomain(entry.id(),p.position()),site,entry.id());
                case Entries.Preserve ignored -> {
                    StorageId cell=exactCell(seed.place());
                    if(cell!=null && c.index.storage.get(cell).header().lifetime()==Memory.Lifetime.ACTIVATION) c.error("I-17",entry.id(),"preserve is not initialization of a new activation cell");
                }
                case Entries.ExternalUnknown ignored -> { }
                case Entries.Uninitialized ignored -> { }
            }
        }
    }
    private void same(DomainSubject a,DomainSubject b,ProofSite site,Id owner) {
        if(!domains.same(a,b,site)) c.error("I-08/I-52",owner,"missing sameDomain proof in the applicable static site");
    }
    private void rejectConcreteContradiction(Optional<TypeRef> a,Optional<TypeRef> b,Id owner) {
        if(a.isPresent() && b.isPresent() && a.get() instanceof Known && b.get() instanceof Known && !TypeResolver.sameKnown(a,b))
            c.error("I-08",owner,"partial signature cannot hide contradictory known domains");
    }
    private static Operand argumentOperand(Interactions.Argument arg) {
        return switch(arg) {
            case Interactions.ValueArgument a -> a.value();
            case Interactions.CopyArgument a -> a.value();
            case Interactions.ReferenceArgument a -> a.place();
        };
    }
    private void role(Operand operand,Operand.Role role) {
        if(operand.header().role()!=role) c.error("I-11",operand.header().id(),"expected role "+role+", got "+operand.header().role());
    }
    private void codecWrite(Place place,Expression value,OperationId id) {
        Memory.Codec codec=codec(place); if(codec==null) return;
        Optional<BigInteger> extent=extent(place);
        boolean supported=false;
        if(value instanceof Expressions.Literal literal && extent.isPresent()) {
            BigInteger n=extent.get();
            if(codec instanceof Memory.IdentityBytes && literal.value() instanceof Values.BytesValue bytes) {
                supported=true; if(!n.equals(BigInteger.valueOf(bytes.octets().size()))) c.error("I-46",id,"bytes value length differs from view extent");
            } else if(codec instanceof Memory.AsciiText && literal.value() instanceof Values.TextValue text) {
                supported=true; if(!n.equals(BigInteger.valueOf(text.value().codePointCount(0,text.value().length()))) || text.value().codePoints().anyMatch(ch->ch>127)) c.error("I-46",id,"text value does not fit exact ASCII view");
            } else if(codec instanceof Memory.BinaryCodec binary && literal.value() instanceof Values.IntValue integer) {
                supported=true;
                // Avoid constructing unbounded powers for malicious codec widths.
                BigInteger v=integer.value();
                boolean fits=binary.signed()?v.bitLength()<binary.width():v.signum()>=0 && v.bitLength()<=binary.width();
                if(!fits) c.error("I-46",id,"integer outside binary codec range");
            }
        }
        if(!supported && !domains.safety(SafetyProperty.VALID_CODEC_WRITE,place.header().id(),new OperationSite(id))) limit(id,"codec write preconditions require scoped evidence for this nonliteral/unknown view");
    }
    private void checkRange(StorageId region,Expression offset,Expression extent,Optional<PremiseId> proof,Id owner,ProofSite site) {
        c.ref(region,owner); proof.ifPresent(id->c.ref(id,owner));
        if(!(c.index.storage.get(region) instanceof Memory.Region r)) { c.error("I-13",owner,"range does not name a region"); return; }
        Optional<BigInteger> o=integer(offset),n=integer(extent);
        if(o.isPresent() && o.get().signum()<0 || n.isPresent() && n.get().signum()<0) { c.error("I-13",owner,"negative physical interval"); return; }
        if(o.isPresent() && n.isPresent() && r.extent().isPresent()) {
            if(o.get().add(n.get()).compareTo(r.extent().get())>0) c.error("I-13",owner,"physical interval exceeds region");
        } else if(proof.isEmpty() || !domains.safety(proof.get(),SafetyProperty.VALID_PURE_ACCESS,owner,site)) limit(owner,"calculated/open interval requires an applicable bounds premise");
    }
    private Memory.Binding resolvedBinding(ObjectId id) {
        Optional<Memory.Binding> cached=bindingCache.get(id); if(cached!=null) return cached.orElse(null);
        LinkedHashSet<ObjectId> path=new LinkedHashSet<>(); ObjectId current=id;
        Memory.Binding answer=null;
        while(path.add(current)) {
            Optional<Memory.Binding> known=bindingCache.get(current);
            if(known!=null) { answer=known.orElse(null); break; }
            Memory.ObjectDeclaration object=c.index.objects.get(current); if(object==null) break;
            if(object.storage() instanceof Memory.AliasBinding alias) current=alias.object();
            else { answer=object.storage(); break; }
        }
        Optional<Memory.Binding> value=Optional.ofNullable(answer);
        for(ObjectId item:path) bindingCache.put(item,value);
        return answer;
    }
    private Memory.Codec codec(Place place) {
        if(place instanceof Places.RegionSlice r) return r.codec();
        if(place instanceof Places.ObjectPlace p && resolvedBinding(p.object()) instanceof Memory.ViewBinding v) return v.codec();
        return null;
    }
    private Optional<BigInteger> extent(Place place) {
        if(place instanceof Places.RegionSlice r) return integer(r.length());
        if(place instanceof Places.ObjectPlace p && resolvedBinding(p.object()) instanceof Memory.ViewBinding v) return Optional.of(v.extent());
        return Optional.empty();
    }
    private StorageId exactCell(Place place) {
        if(place instanceof Places.ObjectPlace p && resolvedBinding(p.object()) instanceof Memory.CellBinding c) return c.storage();
        return null;
    }
    private static Optional<BigInteger> integer(Expression expression) {
        return expression instanceof Expressions.Literal l && l.value() instanceof Values.IntValue i?Optional.of(i.value()):Optional.empty();
    }
    private static ProofSite site(OperandId id) {
        return id.owner() instanceof OperationOwner o?new OperationSite(o.operation()):new EntrySite(((EntryOwner)id.owner()).entry());
    }
    private void limit(Id owner,String detail) { c.issue(ValidationIssue.Kind.VALIDATION_LIMIT,"PRECONDITION_NOT_DISCHARGED",owner,detail); }
    private static Envelopes.Envelope fallback(Operation op) {
        return switch(op) {
            case Operations.CopyBytes o -> o.fallback(); case Operations.Opaque o -> o.envelope();
            case Operations.LocalInvoke o -> o.fallback(); case Operations.LocalBoundary o -> o.fallback();
            case Operations.LocalResume o -> o.fallback(); case Operations.LocalUnwind o -> o.fallback();
            case Operations.IndirectJump o -> o.fallback(); default -> null;
        };
    }
}
