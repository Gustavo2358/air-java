package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.model.Proofs.*;
import io.github.gustavo2358.air.model.Types.*;
import java.math.BigInteger;
import java.util.*;

/** Checks static operation signatures and explicit validation obligations, not analysis results. */
final class OperationChecks {
    final ValidationContext c;
    final ReferenceChecks refs;
    final TypeResolver types;
    final DomainProofEngine domains;
    private final Map<ObjectId,Optional<Memory.Binding>> bindingCache=new HashMap<>();
    private final Map<UnitId,Boolean> returnCompatibility=new HashMap<>();

    OperationChecks(ValidationContext c,ReferenceChecks refs,TypeResolver types,
                    DomainProofEngine domains) {
        this.c=c; this.refs=refs; this.types=types; this.domains=domains;
    }

    void run() {
        for(Operand operand:c.index.operands.values()) {
            types.type(operand); checkOperand(operand);
        }
        for(Operation operation:c.index.operations.values()) operation(operation);
        for(Entries.Entry entry:c.index.entries.values()) entry(entry);
    }

    private void checkOperand(Operand operand) {
        OperandId id=operand.header().id(); ProofSite site=site(id);
        if(operand instanceof Places.Choice choice) {
            List<Optional<TypeRef>> candidateTypes=choice.candidates().stream().map(types::type).toList();
            boolean allKnown=!candidateTypes.isEmpty()
                    && candidateTypes.stream().allMatch(type -> type.isPresent()
                    && type.get() instanceof Known);
            boolean sameKnown=allKnown && candidateTypes.stream()
                    .allMatch(type -> TypeResolver.sameKnown(candidateTypes.get(0),type));
            if(choice.typeRef() instanceof Known known) {
                for(Optional<TypeRef> type:candidateTypes) types.expect(type,known.type(),id);
                if(choice.remainder() instanceof Scopes.WithinMemory
                        && !domains.universalChoiceDomain(id,known.type(),site))
                    c.error("I-51",id,
                            "known choice domain requires an applicable sameDomain premise over the whole open occurrence");
            } else if(choice.remainder() instanceof Scopes.NoMemory && sameKnown) {
                c.error("I-51",id,"closed homogeneous choice must preserve its known domain");
            }
        } else if(operand instanceof Expressions.SliceText slice) {
            Optional<BigInteger> from=integer(slice.start());
            Optional<BigInteger> count=integer(slice.count());
            if(from.isPresent() && from.get().signum()<0
                    || count.isPresent() && count.get().signum()<0) {
                c.error("I-09/I-46",id,"text slice has a negative bound");
            } else if(slice.value() instanceof Expressions.Literal literal
                    && literal.value() instanceof Values.TextValue text
                    && from.isPresent() && count.isPresent()) {
                BigInteger length=BigInteger.valueOf(text.value().codePointCount(0,text.value().length()));
                if(from.get().add(count.get()).compareTo(length)>0)
                    c.error("I-09/I-46",id,"literal text slice is out of bounds");
            } else {
                limit(id,"text slice totality cannot be discharged from literal bounds by this validator");
            }
        } else if(operand instanceof Places.RegionSlice region) {
            types.expect(types.type(region.offset()),Builtin.INT,id);
            types.expect(types.type(region.length()),Builtin.INT,id);
            checkRange(region.region(),region.offset(),region.length(),id);
        } else if(operand instanceof Expressions.Read read) {
            Memory.Codec codec=codec(read.place());
            if(codec instanceof Memory.UnknownCodec || codec instanceof Memory.ExtensionCodec
                    || codec instanceof Memory.AsciiText)
                limit(id,"read totality for ASCII/unknown/extension codec is a producer obligation not proven by this validator");
        } else if(operand instanceof Expressions.Binary binary
                && (binary.operator()==Expressions.BinaryOperator.EQ
                || binary.operator()==Expressions.BinaryOperator.NE)) {
            Optional<TypeRef> type=types.type(binary.left());
            if(type.isPresent() && type.get() instanceof Known known
                    && known.type() instanceof ExtensionType)
                limit(id,"equality for an extension domain depends on its negotiated semantic manifest");
        }
    }

    private void operation(Operation operation) {
        OperationId id=operation.header().id(); OperationSite site=new OperationSite(id);
        switch(operation) {
            case Operations.Assign assign -> {
                role(assign.destination(),Operand.Role.VALUE_WRITE);
                role(assign.value(),Operand.Role.VALUE_READ);
                same(new OperandDomain(assign.destination().header().id()),
                        new OperandDomain(assign.value().header().id()),site,id);
                codecWrite(assign.destination(),assign.value(),id);
            }
            case Operations.HavocMust havoc -> {
                role(havoc.destination(),Operand.Role.VALUE_WRITE);
                c.uncertainty(havoc.reason(),null,id);
            }
            case Operations.HavocMay havoc -> {
                refs.memory(havoc.scope(),id,0); c.uncertainty(havoc.reason(),null,id);
            }
            case Operations.Nop ignored -> { }
            case Operations.CopyBytes copy -> {
                c.capability(Capabilities.MEMORY_REGIONS,id);
                refs.envelope(copy.fallback(),id,true);
                checkRange(copy.source().region(),copy.source().offset(),copy.source().extent(),id);
                checkRange(copy.destination().region(),copy.destination().offset(),copy.destination().extent(),id);
                for(Memory.ByteRange range:List.of(copy.source(),copy.destination())) {
                    types.expect(types.type(range.offset()),Builtin.INT,id);
                    types.expect(types.type(range.extent()),Builtin.INT,id);
                    integer(range.extent()).ifPresent(extent -> {
                        if(copy.length().compareTo(extent)>0)
                            c.error("I-13",id,
                                    "copy length exceeds declared source/destination interval");
                    });
                }
            }
            case Operations.Jump jump -> refs.label(jump.destination(),id.unit(),id);
            case Operations.Branch branch -> {
                role(branch.predicate(),Operand.Role.PREDICATE);
                types.expect(types.type(branch.predicate()),Builtin.BOOL,id);
                refs.label(branch.trueDestination(),id.unit(),id);
                refs.label(branch.falseDestination(),id.unit(),id);
            }
            case Operations.Dispatch dispatch -> {
                Optional<TypeRef> type=types.type(dispatch.selector());
                role(dispatch.selector(),Operand.Role.CONTROL_TARGET);
                Set<java.lang.Object> keys=new HashSet<>();
                for(Operations.Case item:dispatch.cases()) {
                    types.expect(type,item.value().type(),id);
                    refs.label(item.destination(),id.unit(),id);
                    if(!keys.add(Values.semanticKey(item.value())))
                        c.error("I-20",id,"dispatch duplicates semantically equal literal");
                    if(item.value().type() instanceof LabelType)
                        c.error("I-08",id,"dispatch does not admit label domain");
                }
                if(type.isEmpty() || !(type.get() instanceof Known known)
                        || !(known.type() instanceof Builtin))
                    c.error("I-08",id,"dispatch requires a known core scalar domain");
                refs.label(dispatch.defaultDestination(),id.unit(),id);
            }
            case Operations.Invoke invoke -> invocation(invoke);
            case Operations.Return returned -> returnOperation(returned);
            case Operations.Raise ignored -> { }
            case Operations.Halt ignored -> { }
            case Operations.Opaque opaque -> opaque(opaque);
            case Operations.LocalInvoke local -> {
                c.capability(Capabilities.LOCAL_CONTROL,id);
                refs.label(local.entry(),id.unit(),id); refs.label(local.resume(),id.unit(),id);
                c.refs(local.completionPorts(),id);
                for(CompletionPortId port:local.completionPorts())
                    if(!port.unit().equals(id.unit()))
                        c.error("I-02",id,"local completion port crosses unit");
                refs.envelope(local.fallback(),id,false);
            }
            case Operations.LocalBoundary local -> {
                c.capability(Capabilities.LOCAL_CONTROL,id); c.ref(local.port(),id);
                if(!local.port().unit().equals(id.unit()))
                    c.error("I-02",id,"local completion port crosses unit");
                refs.label(local.defaultDestination(),id.unit(),id);
                refs.envelope(local.fallback(),id,false);
            }
            case Operations.LocalResume local -> {
                c.capability(Capabilities.LOCAL_CONTROL,id);
                refs.envelope(local.fallback(),id,false);
            }
            case Operations.LocalUnwind local -> {
                c.capability(Capabilities.LOCAL_CONTROL,id);
                refs.label(local.destination(),id.unit(),id);
                refs.envelope(local.fallback(),id,false);
            }
            case Operations.IndirectJump jump -> {
                c.capability(Capabilities.INDIRECT_CONTROL,id);
                refs.envelope(jump.fallback(),id,false); c.type(new Known(jump.within()),id);
                if(!jump.within().unit().equals(id.unit()))
                    c.error("I-08",id,"indirect control universe crosses unit");
                role(jump.target(),Operand.Role.CONTROL_TARGET);
                types.expect(types.type(jump.target()),jump.within(),id);
            }
        }

        Envelopes.Envelope envelope=fallback(operation);
        if(envelope!=null) for(Envelopes.ResourceUse use:envelope.dependencies().known())
            if(use.target() instanceof Interactions.ComputedResource computed) {
                Operand target=c.index.operands.get(computed.name());
                types.expect(target==null ? Optional.empty() : types.type(target),Builtin.TEXT,id);
            }
    }

    private void opaque(Operations.Opaque opaque) {
        OperationId id=opaque.header().id();
        refs.envelope(opaque.envelope(),id,false);
        if(opaque.header().uncertainties().isEmpty())
            c.error("I-26",id,"opaque requires uncertainty references");
        c.refs(opaque.valueResults(),id);
        Set<OperandId> writes=new HashSet<>(opaque.envelope().memory().knownWrites());
        writes.addAll(opaque.envelope().memory().mustOverwrite());
        for(OperandId result:opaque.valueResults()) {
            Operand operand=c.index.operands.get(result);
            if(!(operand instanceof Place place))
                c.error("I-11",id,"opaque value result must reference a materialized Place occurrence");
            else role(place,Operand.Role.RESULT_TARGET);
            if(!result.owner().equals(new OperationOwner(id)))
                c.error("I-11",id,"opaque result belongs to another operation");
            if(!writes.contains(result))
                c.error("I-26",id,"opaque value result must also be declared by its memory envelope");
        }
    }

    private void invocation(Operations.Invoke invoke) {
        OperationId id=invoke.header().id(); InvocationSite site=new InvocationSite(id);
        refs.target(invoke.target(),id);
        refs.invocationSignature(invoke.signature(),invoke.target(),id);
        refs.outcomes(invoke.outcomes(),id.unit(),id);
        refs.effects(invoke.effectBound(),id,invoke);
        refs.contract(invoke.contract(),id);
        c.obligation("I-56",id,
                "producer/authority must substantiate materialized signature, effects and outcomes; structural validation does not compare them with external semantics or a body");

        long normalCount=invoke.outcomes().known().stream()
                .filter(Control.Normal.class::isInstance).count();
        if(normalCount==0 && !invoke.results().isEmpty())
            c.error("I-08",id,"results require an explicit normal invocation outcome");
        if(invoke.target() instanceof Interactions.ComputedTarget target) {
            role(target.name(),Operand.Role.CALL_TARGET);
            types.expect(types.type(target.name()),Builtin.TEXT,id);
        }
        for(Interactions.Argument argument:invoke.arguments()) switch(argument) {
            case Interactions.ValueArgument value -> role(value.value(),Operand.Role.ARGUMENT_VALUE);
            case Interactions.CopyArgument copy -> role(copy.value(),Operand.Role.ARGUMENT_VALUE);
            case Interactions.ReferenceArgument reference ->
                    role(reference.place(),Operand.Role.ARGUMENT_REFERENCE);
        }
        for(Place result:invoke.results()) role(result,Operand.Role.RESULT_TARGET);

        Optional<Interactions.Signature> signature=domains.signature(invoke.signature());
        if(signature.isPresent()) validateInvocationSignature(invoke,signature.get(),normalCount>0,site);
        if(invoke.contract() instanceof Interactions.UnknownContract)
            c.obligation("I-23/I-56",id,
                    "unknown authority does not turn materialized effects or outcomes into empty facts");
    }

    private void validateInvocationSignature(Operations.Invoke invoke,Interactions.Signature signature,
                                             boolean hasNormal,InvocationSite site) {
        OperationId id=invoke.header().id();
        Interactions.ParameterInventory parameters=signature.parameters();
        Interactions.ResultInventory results=signature.results();
        boolean parametersClosed=parameters.remainder() instanceof Interactions.NoRemainder;
        boolean resultsClosed=results.remainder() instanceof Interactions.NoRemainder;
        boolean exactValues=invoke.header().precision().values().status()
                ==Evidence.PrecisionStatus.EXACT;
        if(parametersClosed && invoke.arguments().size()!=parameters.known().size())
            c.error("I-08",id,"argument cardinality contradicts the closed parameter inventory");
        if(hasNormal && resultsClosed && invoke.results().size()!=results.known().size())
            c.error("I-08",id,"result cardinality contradicts the closed result inventory");

        for(Interactions.Parameter parameter:parameters.known()) {
            Optional<Interactions.Argument> argument=at(invoke.arguments(),parameter.position());
            if(argument.isEmpty()) {
                c.error("I-08",id,"known parameter position has no corresponding argument");
                continue;
            }
            Interactions.PassingMode actual=mode(argument.get());
            if(parameter.mode() instanceof Interactions.KnownMode known && actual!=known.mode())
                c.error("I-08",id,"argument passing mode differs from signature");
            Operand operand=argumentOperand(argument.get());
            DomainSubject destination=parameterSubject(invoke,parameter.position());
            requireConcreteSignatureType(types.type(operand),parameter.typeRef(),id);
            if(exactValues && parameter.mode() instanceof Interactions.KnownMode
                    && !domains.same(new OperandDomain(operand.header().id()),destination,site))
                c.error("I-08/I-52",id,
                        "argument transmission lacks sameDomain proof at invocation_site");
        }

        if(hasNormal) for(Interactions.ResultSlot result:results.known()) {
            Optional<Place> destination=at(invoke.results(),result.position());
            if(destination.isEmpty()) {
                c.error("I-08",id,"known result position has no corresponding destination");
                continue;
            }
            if(exactValues && !domains.same(resultSubject(invoke,result.position()),
                    new OperandDomain(destination.get().header().id()),site))
                c.error("I-08/I-52",id,
                        "result transmission lacks sameDomain proof at invocation_site");
            requireConcreteSignatureType(types.type(destination.get()),result.typeRef(),id);
        }
    }

    private DomainSubject parameterSubject(Operations.Invoke invoke,BigInteger position) {
        return switch(invoke.signature()) {
            case Interactions.EntrySignature entry ->
                    new CallParameterDomain(invoke.header().id(),entry.entry(),position);
            case Interactions.ExternalSignature ignored ->
                    new ExternalParameterDomain(invoke.header().id(),position);
        };
    }
    private DomainSubject resultSubject(Operations.Invoke invoke,BigInteger position) {
        return switch(invoke.signature()) {
            case Interactions.EntrySignature entry ->
                    new CallResultDomain(invoke.header().id(),entry.entry(),position);
            case Interactions.ExternalSignature ignored ->
                    new ExternalResultDomain(invoke.header().id(),position);
        };
    }

    private void returnOperation(Operations.Return returned) {
        OperationId id=returned.header().id(); Unit unit=c.index.units.get(id.unit());
        if(unit==null || unit.entries().isEmpty()) return;
        List<Entries.Entry> entries=unit.entries();
        if(entries.size()>1 && !returnCompatibility.computeIfAbsent(unit.id(),
                ignored -> compatibleResultInventories(entries))) {
            limit(id,"return is shared by entries whose result inventories are not statically compatible");
            c.obligation("I-61",id,
                    "producer must cover every activation entry that can execute this return; validator does not select one");
            return;
        }
        if(entries.get(0).signature().results().remainder() instanceof Interactions.NoRemainder
                && entries.get(0).signature().results().known().isEmpty() && returned.values().isEmpty()) return;
        for(Entries.Entry entry:entries) validateReturnForEntry(returned,entry);
    }

    private boolean compatibleResultInventories(List<Entries.Entry> entries) {
        Interactions.ResultInventory first=entries.get(0).signature().results();
        if(!(first.remainder() instanceof Interactions.NoRemainder)) return false;
        for(int i=1;i<entries.size();i++) {
            Interactions.ResultInventory other=entries.get(i).signature().results();
            if(!(other.remainder() instanceof Interactions.NoRemainder)
                    || other.known().size()!=first.known().size()) return false;
            for(int j=0;j<first.known().size();j++) {
                Interactions.ResultSlot a=first.known().get(j),b=other.known().get(j);
                if(!a.position().equals(b.position()) || !TypeResolver.sameRef(a.typeRef(),b.typeRef()))
                    return false;
            }
        }
        return true;
    }

    private void validateReturnForEntry(Operations.Return returned,Entries.Entry entry) {
        OperationId id=returned.header().id();
        Interactions.ResultInventory results=entry.signature().results();
        boolean closed=results.remainder() instanceof Interactions.NoRemainder;
        if(closed && results.known().size()!=returned.values().size())
            c.error("I-08",id,"return values contradict the entry result inventory");
        for(Interactions.ResultSlot slot:results.known()) {
            Optional<Expression> value=at(returned.values(),slot.position());
            if(value.isEmpty()) {
                c.error("I-08",id,"known entry result position has no returned value");
                continue;
            }
            same(new OperandDomain(value.get().header().id()),
                    new ResultDomain(entry.id(),slot.position()),new OperationSite(id),id);
            requireConcreteSignatureType(types.type(value.get()),slot.typeRef(),id);
        }
    }

    private void entry(Entries.Entry entry) {
        EntrySite site=new EntrySite(entry.id());
        for(Interactions.Parameter parameter:entry.signature().parameters().known())
            if(parameter.objectBinding() instanceof Interactions.ObjectBinding object) {
                Memory.ObjectDeclaration declaration=c.index.objects.get(object.object());
                if(declaration!=null)
                    requireConcreteSignatureType(Optional.of(declaration.typeRef()),
                            parameter.typeRef(),entry.id());
                boolean proof=domains.same(new ObjectDomain(object.object()),
                        new ParameterDomain(entry.id(),parameter.position()),site);
                if(!proof) c.error("I-08/I-52",entry.id(),
                        "parameter/object binding lacks entry_site sameDomain proof");
            }

        Map<StorageId,Values.LiteralValue> exactSeeds=new HashMap<>();
        for(Entries.InitialCondition seed:entry.state().conditions()) {
            DomainSubject destination=new OperandDomain(seed.place().header().id());
            switch(seed.value()) {
                case Entries.LiteralInitial literal -> {
                    same(destination,new OperandDomain(literal.value().header().id()),site,entry.id());
                    StorageId cell=exactCell(seed.place());
                    if(cell!=null) {
                        Values.LiteralValue previous=exactSeeds.putIfAbsent(cell,literal.value().value());
                        if(previous!=null && !Values.semanticKey(previous)
                                .equals(Values.semanticKey(literal.value().value())))
                            c.error("I-17",entry.id(),
                                    "conflicting literal initializers for the same cell/alias");
                    } else if(codec(seed.place())!=null) {
                        limit(entry.id(),
                                "overlapping region initializers require byte/codec consistency checks outside this validator slice");
                    }
                }
                case Entries.ParameterInitial parameter -> same(destination,
                        new ParameterDomain(entry.id(),parameter.position()),site,entry.id());
                case Entries.Preserve ignored -> {
                    StorageId cell=exactCell(seed.place());
                    if(cell!=null && c.index.storage.get(cell).header().lifetime()==Memory.Lifetime.ACTIVATION)
                        c.error("I-17",entry.id(),
                                "preserve is not initialization of a new activation cell");
                }
                case Entries.ExternalUnknown ignored -> { }
                case Entries.Uninitialized ignored -> { }
            }
        }
    }

    private void same(DomainSubject left,DomainSubject right,ProofSite site,Id owner) {
        if(!domains.same(left,right,site))
            c.error("I-08/I-52",owner,"missing sameDomain proof in the applicable static site");
    }

    private void requireConcreteSignatureType(Optional<TypeRef> actual,TypeRef declared,Id owner) {
        if(declared instanceof Known known) types.expect(actual,known.type(),owner);
    }

    private static Interactions.PassingMode mode(Interactions.Argument argument) {
        return switch(argument) {
            case Interactions.ValueArgument ignored -> Interactions.PassingMode.VALUE;
            case Interactions.CopyArgument ignored -> Interactions.PassingMode.COPY;
            case Interactions.ReferenceArgument ignored -> Interactions.PassingMode.REFERENCE;
        };
    }
    private static Operand argumentOperand(Interactions.Argument argument) {
        return switch(argument) {
            case Interactions.ValueArgument value -> value.value();
            case Interactions.CopyArgument copy -> copy.value();
            case Interactions.ReferenceArgument reference -> reference.place();
        };
    }
    private static <T> Optional<T> at(List<T> values,BigInteger position) {
        if(position.compareTo(BigInteger.valueOf(values.size()))>=0) return Optional.empty();
        return Optional.of(values.get(position.intValueExact()));
    }

    private void role(Operand operand,Operand.Role role) {
        if(operand.header().role()!=role)
            c.error("I-11",operand.header().id(),"expected role "+role+", got "+operand.header().role());
    }

    private void codecWrite(Place place,Expression value,OperationId id) {
        Memory.Codec codec=codec(place); if(codec==null) return;
        Optional<BigInteger> extent=extent(place);
        boolean discharged=false;
        if(value instanceof Expressions.Literal literal && extent.isPresent()) {
            BigInteger size=extent.get();
            if(codec instanceof Memory.IdentityBytes
                    && literal.value() instanceof Values.BytesValue bytes) {
                discharged=true;
                if(!size.equals(BigInteger.valueOf(bytes.octets().size())))
                    c.error("I-46",id,"bytes value length differs from view extent");
            } else if(codec instanceof Memory.AsciiText
                    && literal.value() instanceof Values.TextValue text) {
                discharged=true;
                if(!size.equals(BigInteger.valueOf(text.value().codePointCount(0,text.value().length())))
                        || text.value().codePoints().anyMatch(codePoint -> codePoint>127))
                    c.error("I-46",id,"text value does not fit exact ASCII view");
            } else if(codec instanceof Memory.BinaryCodec binary
                    && literal.value() instanceof Values.IntValue integer) {
                discharged=true;
                BigInteger bits=BigInteger.valueOf(integer.value().bitLength());
                boolean fits=binary.signed()
                        ? bits.compareTo(binary.width())<0
                        : integer.value().signum()>=0 && bits.compareTo(binary.width())<=0;
                if(!fits) c.error("I-46",id,"integer outside binary codec range");
            }
        }
        if(!discharged)
            limit(id,"codec write preconditions cannot be discharged for this nonliteral or unsupported view");
    }

    private void checkRange(StorageId region,Expression offset,Expression extent,Id owner) {
        c.ref(region,owner);
        if(!(c.index.storage.get(region) instanceof Memory.Region storage)) {
            c.error("I-13",owner,"range does not name a region"); return;
        }
        Optional<BigInteger> start=integer(offset),length=integer(extent);
        if(start.isPresent() && start.get().signum()<0
                || length.isPresent() && length.get().signum()<0) {
            c.error("I-13",owner,"negative physical interval"); return;
        }
        if(start.isPresent() && length.isPresent() && storage.extent().isPresent()) {
            if(start.get().add(length.get()).compareTo(storage.extent().get())>0)
                c.error("I-13",owner,"physical interval exceeds region");
        } else {
            limit(owner,"calculated or open interval totality is not discharged by this validator");
        }
    }

    private Memory.Binding resolvedBinding(ObjectId id) {
        Optional<Memory.Binding> cached=bindingCache.get(id);
        if(cached!=null) return cached.orElse(null);
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
        if(place instanceof Places.RegionSlice region) return region.codec();
        if(place instanceof Places.ObjectPlace object
                && resolvedBinding(object.object()) instanceof Memory.ViewBinding view)
            return view.codec();
        return null;
    }
    private Optional<BigInteger> extent(Place place) {
        if(place instanceof Places.RegionSlice region) return integer(region.length());
        if(place instanceof Places.ObjectPlace object
                && resolvedBinding(object.object()) instanceof Memory.ViewBinding view)
            return Optional.of(view.extent());
        return Optional.empty();
    }
    private StorageId exactCell(Place place) {
        if(place instanceof Places.ObjectPlace object
                && resolvedBinding(object.object()) instanceof Memory.CellBinding cell)
            return cell.storage();
        return null;
    }
    private static Optional<BigInteger> integer(Expression expression) {
        return expression instanceof Expressions.Literal literal
                && literal.value() instanceof Values.IntValue integer
                ? Optional.of(integer.value()) : Optional.empty();
    }
    private static ProofSite site(OperandId id) {
        return id.owner() instanceof OperationOwner operation
                ? new OperationSite(operation.operation())
                : new EntrySite(((EntryOwner)id.owner()).entry());
    }
    private void limit(Id owner,String detail) {
        c.issue(ValidationIssue.Kind.VALIDATION_LIMIT,"PRECONDITION_NOT_DISCHARGED",owner,detail);
    }
    private static Envelopes.Envelope fallback(Operation operation) {
        return switch(operation) {
            case Operations.CopyBytes copy -> copy.fallback();
            case Operations.Opaque opaque -> opaque.envelope();
            case Operations.LocalInvoke local -> local.fallback();
            case Operations.LocalBoundary local -> local.fallback();
            case Operations.LocalResume local -> local.fallback();
            case Operations.LocalUnwind local -> local.fallback();
            case Operations.IndirectJump jump -> jump.fallback();
            default -> null;
        };
    }
}
