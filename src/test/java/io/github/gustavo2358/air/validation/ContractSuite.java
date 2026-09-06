package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.model.Proofs.*;
import java.math.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static io.github.gustavo2358.air.validation.Fixtures.known;

/** Dependency-free contract runner, executed by both Maven test and scripts/check.sh. */
public final class ContractSuite {
    private static int passed;
    private ContractSuite() {}
    public static void main(String[] args) throws Exception {
        test("semver parse",()->eq(new SemanticVersion(2,0,0),SemanticVersion.parse("2.0.0")));
        test("bad semver rejected",()->throwsType(IllegalArgumentException.class,()->SemanticVersion.parse("02.0.0")));
        test("negative version rejected",()->throwsType(IllegalArgumentException.class,()->new SemanticVersion(-1,0,0)));
        test("empty text retained",()->eq("",new Values.TextValue("").value()));
        test("unpaired surrogate rejected",()->throwsType(IllegalArgumentException.class,()->new Values.TextValue("\uD800")));
        test("supplementary scalar accepted",()->eq(2,new Values.TextValue("\uD83D\uDE00").value().length()));
        test("bytes input defensive copy",()->{ byte[] b={0,-1}; Values.BytesValue v=Values.BytesValue.of(b); b[0]=12; eq(List.of(0,255),v.octets()); });
        test("bytes output defensive copy",()->{ Values.BytesValue v=Values.BytesValue.of(new byte[]{1});v.toByteArray()[0]=9;eq(List.of(1),v.octets()); });
        test("invalid octet rejected",()->throwsType(IllegalArgumentException.class,()->new Values.BytesValue(List.of(256))));
        test("decimal semantic equality",()->eq(Values.semanticKey(new Values.DecimalValue(BigInteger.valueOf(120),2)),Values.semanticKey(new Values.DecimalValue(BigInteger.valueOf(12),1))));
        test("negative decimal scale rejected",()->throwsType(IllegalArgumentException.class,()->new Values.DecimalValue(BigInteger.ONE,-1)));
        test("source end before start rejected",()->throwsType(IllegalArgumentException.class,()->new Origins.Span(new Origins.Position(2,1),new Origins.Position(1,1),1,1,Origins.ColumnUnit.UNICODE_SCALAR,true)));
        test("minimal publication valid",()->valid(minimal().build()));
        test("pure core has no JSON/frontend imports",ContractSuite::architecture);
        test("publication lists immutable",()->{ Publication p=minimal().build(); throwsType(UnsupportedOperationException.class,()->p.units().clear()); });
        test("constructor snapshots source list",()->{ Fixtures f=minimal(); Publication p=f.build(); f.sequences.clear();eq(1,p.units().get(0).sequences().size()); });
        test("terminator required",()->{ Fixtures f=minimal();throwsType(NullPointerException.class,()->new Sequence(f.label("s"),List.of(),null,f.origin)); });
        test("empty closed control is not divergence",()->throwsType(IllegalArgumentException.class,()->new Control.Envelope(List.of(),Scopes.NoControl.INSTANCE)));
        test("explicit divergence representable",()->eq(1,new Control.Envelope(List.of(Control.Diverge.INSTANCE),Scopes.NoControl.INSTANCE).known().size()));
        test("missing label rejected",()->{Fixtures f=new Fixtures();f.sequence("s",List.of(),new Operations.Jump(f.header(f.op("j")),f.label("missing")));invalid(f.build(),"I-02");});
        test("duplicate operation rejected",()->{Fixtures f=minimal();f.sequence("other",List.of(),f.halt("stop"));invalid(f.build(),"I-01");});
        test("foreign publication origin rejected",()->{Fixtures f=minimal();f.origins.add(new Origins.Unavailable(new OriginId(new PublicationId("other"),"x"),"external"));invalid(f.build(),"I-01");});
        test("dangling origin rejected",()->{Fixtures f=minimal();f.origins.clear();invalid(f.build(),"I-02");});
        test("origin cycle rejected",()->{Fixtures f=minimal();OriginId a=new OriginId(f.pub,"a"),b=new OriginId(f.pub,"b");f.origins.add(new Origins.Derived(a,List.of(b),"rule"));f.origins.add(new Origins.Derived(b,List.of(a),"rule"));invalid(f.build(),"I-36");});
        test("alias cycle rejected",()->{Fixtures f=minimal();ObjectId a=new ObjectId(f.unit,"a"),b=new ObjectId(f.unit,"b");f.alias("a",b,known(Types.Builtin.TEXT));f.alias("b",a,known(Types.Builtin.TEXT));invalid(f.build(),"I-12");});
        test("unknown type declaration valid",()->{Fixtures f=minimal();f.object("x",new Types.UnknownType(f.uncertainty("type","TYPE_UNKNOWN")));valid(f.build());});
        test("unknown type reason must be TYPE_UNKNOWN",()->{Fixtures f=minimal();f.object("x",new Types.UnknownType(f.uncertainty("type","VALUE_UNKNOWN")));invalid(f.build(),"I-49");});
        test("unknown type dangling reason rejected",()->{Fixtures f=minimal();f.object("x",new Types.UnknownType(new UncertaintyId(f.pub,"missing")));invalid(f.build(),"I-02");});
        test("literal assign same known domain",()->{Fixtures f=new Fixtures();ObjectId x=f.object("x",known(Types.Builtin.TEXT));f.linear(f.assign("a",x,f.text(f.op("a"),"source","A")));valid(f.build());});
        test("assign different concrete domains rejected",()->{Fixtures f=new Fixtures();ObjectId x=f.object("x",known(Types.Builtin.INT));f.linear(f.assign("a",x,f.text(f.op("a"),"source","A")));invalid(f.build(),"I-08/I-52");});
        test("unknown self-copy preserves sameDomain",()->{Fixtures f=new Fixtures();ObjectId x=f.object("x",new Types.UnknownType(f.uncertainty("type","TYPE_UNKNOWN")));f.linear(f.assign("a",x,f.read(f.op("a"),"source",x,Operand.Role.VALUE_READ)));valid(f.build());});
        test("shared uncertainty is not domain equality",()->{Fixtures f=unknownCopy();invalid(f.build(),"I-08/I-52");});
        test("exact alias proves unknown-domain copy",()->{Fixtures f=new Fixtures();Types.TypeRef t=new Types.UnknownType(f.uncertainty("type","TYPE_UNKNOWN"));ObjectId x=f.object("x",t),y=f.alias("y",x,t);f.linear(f.assign("a",y,f.read(f.op("a"),"source",x,Operand.Role.VALUE_READ)));valid(f.build());});
        test("scoped sameDomain permits copy",()->{Fixtures f=unknownCopy();copyProof(f,new OperationDomain(f.op("a")));valid(f.build());});
        test("unit scope permits local copy",()->{Fixtures f=unknownCopy();copyProof(f,new UnitDomain(f.unit));valid(f.build());});
        test("entry scope does not extend to body",()->{Fixtures f=unknownCopy();copyProof(f,new EntryDomain(f.entry()));invalid(f.build(),"I-08/I-52");});
        test("wrong operation scope rejected at use",()->{Fixtures f=unknownCopy();copyProof(f,new OperationDomain(f.op("stop")));invalid(f.build(),"I-08/I-52");});
        test("intersection narrows without losing valid site",()->{Fixtures f=unknownCopy();copyProof(f,new Intersection(new UnitDomain(f.unit),new OperationDomain(f.op("a"))));valid(f.build());});
        test("empty intersection never proves copy",()->{Fixtures f=unknownCopy();copyProof(f,new Intersection(new OperationDomain(f.op("a")),new OperationDomain(f.op("stop"))));invalid(f.build(),"I-08/I-52");});
        test("scope invocation on noninvoke rejected even unused",()->{Fixtures f=minimal();ObjectId x=f.object("x",known(Types.Builtin.TEXT));f.proof("bad",new ObjectDomain(x),new ObjectDomain(x),new InvocationDomain(f.op("stop")));invalid(f.build(),"I-52");});
        test("unused proof dangling subject rejected",()->{Fixtures f=minimal();ObjectId x=f.object("x",known(Types.Builtin.TEXT));f.proof("bad",new ObjectDomain(x),new ObjectDomain(new ObjectId(f.unit,"missing")),PublicationDomain.INSTANCE);invalid(f.build(),"I-02");});
        test("sameDomain contradiction rejected",()->{Fixtures f=minimal();ObjectId x=f.object("x",known(Types.Builtin.TEXT)),y=f.object("y",known(Types.Builtin.INT));f.proof("bad",new ObjectDomain(x),new ObjectDomain(y),PublicationDomain.INSTANCE);invalid(f.build(),"I-52");});
        test("contradiction through unknown intermediary rejected",()->{Fixtures f=minimal();ObjectId x=f.object("x",known(Types.Builtin.TEXT)),y=f.object("y",new Types.UnknownType(f.uncertainty("u","TYPE_UNKNOWN"))),z=f.object("z",known(Types.Builtin.INT));f.proof("p",new ObjectDomain(x),new ObjectDomain(y),PublicationDomain.INSTANCE);f.proof("q",new ObjectDomain(y),new ObjectDomain(z),PublicationDomain.INSTANCE);invalid(f.build(),"I-52");});
        test("transitive scoped premises support copy",()->{Fixtures f=unknownCopy();ObjectId z=f.object("z",new Types.UnknownType(f.uncertainty("tz","TYPE_UNKNOWN")));f.proof("p",new ObjectDomain(f.objects.get(0).id()),new ObjectDomain(z),new UnitDomain(f.unit));f.proof("q",new ObjectDomain(z),new ObjectDomain(f.objects.get(1).id()),new OperationDomain(f.op("a")));valid(f.build());});
        test("unknown bool value permits branch",()->{Fixtures f=new Fixtures();UncertaintyId u=f.uncertainty("predicate","PREDICATE_UNKNOWN");Expression predicate=new Expressions.Unknown(f.operand(f.op("b"),"p",Operand.Role.PREDICATE),known(Types.Builtin.BOOL),List.of(),Scopes.NoMemory.INSTANCE,u);f.sequence("start",List.of(),new Operations.Branch(f.header(f.op("b"),u),predicate,f.label("yes"),f.label("no")));f.sequence("yes",List.of(),f.halt("h1"));f.sequence("no",List.of(),f.halt("h2"));valid(f.build());});
        test("unknown type is not boolean",()->{Fixtures f=new Fixtures();UncertaintyId type=f.uncertainty("type","TYPE_UNKNOWN"),value=f.uncertainty("value","PREDICATE_UNKNOWN");Expression predicate=new Expressions.Unknown(f.operand(f.op("b"),"p",Operand.Role.PREDICATE),new Types.UnknownType(type),List.of(),Scopes.NoMemory.INSTANCE,value);f.sequence("start",List.of(),new Operations.Branch(f.header(f.op("b"),value),predicate,f.label("end"),f.label("end")));f.sequence("end",List.of(),f.halt("h"));invalid(f.build(),"I-08");});
        test("arithmetic rejects unknown type",()->{Fixtures f=new Fixtures();ObjectId x=f.object("x",new Types.UnknownType(f.uncertainty("type","TYPE_UNKNOWN")));OperationId a=f.op("a");Expression expr=new Expressions.Binary(f.operand(a,"sum",Operand.Role.VALUE_READ),Expressions.BinaryOperator.ADD,f.read(a,"lhs",x,Operand.Role.VALUE_READ),f.read(a,"rhs",x,Operand.Role.VALUE_READ));f.linear(f.assign("a",x,expr));invalid(f.build(),"I-08");});
        test("unknown value/type reasons stay distinct",()->{Fixtures f=new Fixtures();UncertaintyId t=f.uncertainty("type","TYPE_UNKNOWN");ObjectId x=f.object("x",new Types.UnknownType(t));Expression unknown=new Expressions.Unknown(f.operand(f.op("a"),"u",Operand.Role.VALUE_READ),new Types.UnknownType(t),List.of(),Scopes.NoMemory.INSTANCE,t);f.linear(f.assign("a",x,unknown));invalid(f.build(),"I-50");});
        test("duplicate decimal dispatch keys rejected",()->{Fixtures f=new Fixtures();OperationId d=f.op("d");Expression select=new Expressions.Literal(f.operand(d,"v",Operand.Role.CONTROL_TARGET),new Values.DecimalValue(BigInteger.TEN,1));f.sequence("start",List.of(),new Operations.Dispatch(f.header(d),select,List.of(new Operations.Case(new Values.DecimalValue(BigInteger.TEN,1),f.label("end")),new Operations.Case(new Values.DecimalValue(BigInteger.ONE,0),f.label("end"))),f.label("end")));f.sequence("end",List.of(),f.halt("h"));invalid(f.build(),"I-20");});
        test("space padding accepted",()->{Fixtures f=new Fixtures();ObjectId x=f.object("x",known(Types.Builtin.TEXT));Expression fit=new Expressions.FitText(f.operand(f.op("a"),"fit",Operand.Role.VALUE_READ),f.text(f.op("a"),"source","A"),BigInteger.valueOf(8)," ");f.linear(f.assign("a",x,fit));valid(f.build());});
        test("text slice out of bounds rejected",()->{Fixtures f=sliceFixture(4,2);invalid(f.build(),"I-09/I-46");});
        test("text slice counts Unicode scalars",()->valid(sliceFixture(1,1).build()));
        test("empty closed choice rejected",()->{Fixtures f=minimal();throwsType(IllegalArgumentException.class,()->new Places.Choice(f.operand(f.op("c"),"p",Operand.Role.VALUE_READ),List.of(),Scopes.NoMemory.INSTANCE,known(Types.Builtin.TEXT),Optional.empty()));});
        test("closed heterogeneous choice retains unknown type",()->{Fixtures f=choiceFixture(false,false);valid(f.build());});
        test("heterogeneous known choice rejected",()->{Fixtures f=choiceFixture(true,false);invalid(f.build(),"I-08");});
        test("open choice does not invent known remainder domain",()->{Fixtures f=choiceFixture(true,true);invalid(f.build(),"I-51");});
        test("opaque retains known reads",()->{Fixtures f=choiceFixture(false,false);eq(3,AirValidator.validate(f.build()).statistics().operands());});
        test("opaque may have two known normal destinations",()->{Fixtures f=minimal();Control.Envelope e=new Control.Envelope(List.of(new Control.Normal(f.label("a")),new Control.Normal(f.label("b"))),Scopes.NoControl.INSTANCE);eq(2,e.known().size());});
        test("opaque without uncertainty rejected",()->{Fixtures f=new Fixtures();f.sequence("start",List.of(),new Operations.Opaque(f.header(f.op("o")),"test.unknown",List.of(),List.of(),f.envelope(null)));invalid(f.build(),"I-26");});
        test("computed target requires known text",()->invalid(invokeFixture(false).build(),"I-08"));
        test("computed text unknown preserves dependency",()->valid(invokeFixture(true).build()));
        test("two normal invocation outcomes rejected",()->{Fixtures f=invokeFixture(true);Operations.Invoke old=(Operations.Invoke)f.sequences.get(0).terminator();Operations.Invoke changed=new Operations.Invoke(old.header(),old.action(),old.target(),old.arguments(),old.results(),old.effectBound(),new Control.Envelope(List.of(new Control.Normal(f.label("end")),new Control.Normal(f.label("start"))),Scopes.NoControl.INSTANCE),old.contract(),old.signatureGaps());f.sequences.set(0,new Sequence(f.label("start"),List.of(),changed,f.origin));invalid(f.build(),"I-08");});
        test("return does not acquire implicit fallthrough",()->{Fixtures f=new Fixtures();f.sequence("a",List.of(),new Operations.Return(f.header(f.op("r")),List.of(),List.of()));f.sequence("b",List.of(),f.halt("h"));valid(f.build());eq(2,f.build().units().get(0).sequences().size());});
        test("local control capability required",()->{Fixtures f=new Fixtures();f.sequence("start",List.of(),new Operations.LocalResume(f.header(f.op("resume")),f.envelope(null)));invalid(f.build(),"I-43");});
        test("local resume modeled without stack execution",()->{Fixtures f=new Fixtures();f.capabilities.add(Capabilities.LOCAL_CONTROL);f.sequence("start",List.of(),new Operations.LocalResume(f.header(f.op("resume")),f.envelope(null)));valid(f.build());});
        test("indirect label universe is explicit",()->{Fixtures f=new Fixtures();f.capabilities.add(Capabilities.INDIRECT_CONTROL);Types.LabelType t=new Types.LabelType(f.unit,List.of(f.label("end")));OperationId j=f.op("j");Expression target=new Expressions.Literal(f.operand(j,"target",Operand.Role.CONTROL_TARGET),new Values.LabelValue(f.label("end"),t));f.sequence("start",List.of(),new Operations.IndirectJump(f.header(j),target,t,f.envelope(null)));f.sequence("end",List.of(),f.halt("h"));valid(f.build());});
        test("label literal outside domain rejected",()->{Fixtures f=minimal();Types.LabelType t=new Types.LabelType(f.unit,List.of(f.label("a")));throwsType(IllegalArgumentException.class,()->new Values.LabelValue(f.label("b"),t));});
        test("unknown required extension is not silently accepted",()->{Fixtures f=minimal();f.capabilities.add(new Capabilities.Capability("vendor.some_extension",1));eq(ValidationResult.Status.INCOMPLETE_VALIDATION,AirValidator.validate(f.build()).status());});
        test("entry literal initialization is not zero default",()->{Fixtures f=minimal();ObjectId x=f.object("x",known(Types.Builtin.TEXT));Place place=new Places.ObjectPlace(f.entryOperand("dst",Operand.Role.VALUE_WRITE),x);Expressions.Literal value=new Expressions.Literal(f.entryOperand("value",Operand.Role.VALUE_READ),new Values.TextValue("BOOT"));f.state=new Entries.EntryState(List.of(new Entries.InitialCondition(place,new Entries.LiteralInitial(value),f.origin,List.of())),List.of());valid(f.build());});
        test("conflicting alias initial states rejected",()->{Fixtures f=minimal();ObjectId x=f.object("x",known(Types.Builtin.TEXT)),y=f.alias("y",x,known(Types.Builtin.TEXT));List<Entries.InitialCondition> seeds=new ArrayList<>();for(int i=0;i<2;i++){ Place p=new Places.ObjectPlace(f.entryOperand("d"+i,Operand.Role.VALUE_WRITE),i==0?x:y);Expressions.Literal v=new Expressions.Literal(f.entryOperand("v"+i,Operand.Role.VALUE_READ),new Values.TextValue(i==0?"A":"B"));seeds.add(new Entries.InitialCondition(p,new Entries.LiteralInitial(v),f.origin,List.of())); }f.state=new Entries.EntryState(seeds,List.of());invalid(f.build(),"I-17");});
        test("missing input differs from empty inventory",()->{Fixtures f=minimal();Publication p=f.build();Evidence.Coverage coverage=new Evidence.Coverage(Evidence.InventoryStatus.UNAVAILABLE,new Scopes.PublicationScope(f.pub),List.of(),List.of());Publication bad=new Publication(p.id(),p.airVersion(),p.capabilities(),p.artifacts(),p.units(),p.storage(),p.resources(),p.artifactRelations(),p.origins(),coverage,p.uncertainties(),p.premises(),p.contracts());invalid(bad,"I-28");});
        test("validation resource limit observable",()->{ValidationResult r=AirValidator.validate(minimal().build(),new ValidationOptions(10,2,100));eq(ValidationResult.Status.INCOMPLETE_VALIDATION,r.status());check(r.issues().stream().anyMatch(i->i.rule().equals("ANALYSIS_LIMIT")),"missing limit diagnostic");});
        test("incompatible AIR version is not accepted",()->{Publication p=minimal().build();Publication v=new Publication(p.id(),new SemanticVersion(1,0,0),p.capabilities(),p.artifacts(),p.units(),p.storage(),p.resources(),p.artifactRelations(),p.origins(),p.coverage(),p.uncertainties(),p.premises(),p.contracts());eq(ValidationResult.Status.INCOMPLETE_VALIDATION,AirValidator.validate(v).status());});
        test("deterministic validation",()->{Publication p=invokeFixture(true).build();eq(AirValidator.validate(p),AirValidator.validate(p));});
        test("large scalar inventory is indexed once",()->{Fixtures f=new Fixtures();ObjectId x=f.object("x",known(Types.Builtin.TEXT));List<Instruction> instructions=new ArrayList<>();for(int i=0;i<6000;i++) instructions.add(f.assign("a"+i,x,f.text(f.op("a"+i),"value","V")));f.sequence("start",instructions,f.halt("h"));ValidationResult r=AirValidator.validate(f.build());check(r.isStructurallyValid(),r.issues().toString());eq(6001,r.statistics().operations());eq(12000,r.statistics().operands());eq(6000L,r.statistics().domainQueries());});
        test("concurrent validation has no shared mutable state",()->{Publication p=invokeFixture(true).build();List<ValidationResult> results=java.util.stream.IntStream.range(0,16).parallel().mapToObj(i->AirValidator.validate(p)).toList();check(results.stream().allMatch(results.get(0)::equals),"nondeterministic concurrent result");});
        additional();
        System.out.println("PASS: "+passed+" deterministic contract checks");
    }
    private static void additional() {
        test("open universal choice-domain premise permits copy",()->valid(openChoiceCopy(true,false).build()));
        test("candidate-only proof does not cover open choice remainder",()->invalid(openChoiceCopy(false,false).build(),"I-08/I-52"));
        test("universal choice proof cannot contradict concrete candidate",()->invalid(openChoiceCopy(true,true).build(),"I-52"));
        test("proof for another occurrence does not prove open choice",()->{Fixtures f=openChoiceCopy(false,false);copyProofForOtherOccurrence(f);invalid(f.build(),"I-08/I-52");});
        test("invocation-site sameDomain validates transmission",()->valid(transmission(true).build()));
        test("transmission missing proof is rejected",()->invalid(transmission(false).build(),"I-08/I-52"));
        test("operation scope includes its own invocation boundary",()->{Fixtures f=transmission(false);transmissionProof(f,new OperationDomain(f.op("k")));valid(f.build());});
        test("entry scope does not prove invocation transmission",()->{Fixtures f=transmission(false);transmissionProof(f,new EntryDomain(f.entry()));invalid(f.build(),"I-08/I-52");});
        test("region view outside known extent rejected",()->{Fixtures f=regionFixture(3,4,Memory.IdentityBytes.INSTANCE,known(Types.Builtin.BYTES));invalid(f.build(),"I-13");});
        test("unknown codec does not erase known logical domain",()->{Fixtures f=minimal();f.capabilities.add(Capabilities.MEMORY_REGIONS);UncertaintyId codec=f.uncertainty("codec","CODEC_UNKNOWN");StorageId rid=new StorageId(f.pub,"r");f.storage.add(new Memory.Region(new Memory.StorageHeader(rid,Optional.of(f.unit),Memory.Lifetime.ACTIVATION,Memory.Visibility.PRIVATE,f.origin),Optional.of(BigInteger.valueOf(8)),Optional.empty()));f.objects.add(new Memory.ObjectDeclaration(new ObjectId(f.unit,"view"),Optional.empty(),known(Types.Builtin.TEXT),new Memory.ViewBinding(rid,BigInteger.ZERO,BigInteger.valueOf(8),new Memory.UnknownCodec(known(Types.Builtin.TEXT),codec)),Memory.Visibility.PRIVATE,f.origin,Evidence.CoverageStatus.ABSTRACTED,f.precision()));valid(f.build());});
        test("known codec and incompatible object domain rejected",()->invalid(regionFixture(0,4,Memory.AsciiText.INSTANCE,known(Types.Builtin.BYTES)).build(),"I-49"));
        test("ASCII literal codec write checked",()->valid(regionWrite("ABCD").build()));
        test("ASCII codec rejects non-ASCII literal",()->invalid(regionWrite("ABCé").build(),"I-46"));
        test("assign does not silently pad a fixed-size view",()->invalid(regionWrite("A").build(),"I-46"));
        test("calculated slice without evidence yields incomplete validation",()->{Fixtures f=new Fixtures();ObjectId x=f.object("x",known(Types.Builtin.TEXT)),i=f.object("i",known(Types.Builtin.INT));OperationId a=f.op("a");Expression slice=new Expressions.SliceText(f.operand(a,"slice",Operand.Role.VALUE_READ),f.text(a,"text","ABC"),f.read(a,"start",i,Operand.Role.VALUE_READ),f.integer(a,"count",1,Operand.Role.VALUE_READ),Optional.empty());f.linear(f.assign("a",x,slice));eq(ValidationResult.Status.INCOMPLETE_VALIDATION,AirValidator.validate(f.build()).status());});
        test("diagnostic limit is never a successful validation",()->{Fixtures f=minimal();f.origins.clear();ValidationResult result=AirValidator.validate(f.build(),new ValidationOptions(32,1000,1));check(!result.isStructurallyValid(),"limit masked errors");check(result.issues().stream().anyMatch(i->i.rule().equals("ANALYSIS_LIMIT")),"limit omitted");});
        test("large number of unit-scoped domain premises is reusable",()->{Fixtures f=new Fixtures();Types.TypeRef t=new Types.UnknownType(f.uncertainty("type","TYPE_UNKNOWN"));ObjectId x=f.object("x",t),y=f.object("y",t);copyProof(f,new UnitDomain(f.unit));List<Instruction> list=new ArrayList<>();for(int i=0;i<1000;i++){OperationId op=f.op("a"+i);list.add(new Operations.Assign(f.header(op),f.place(op,"dst",y,Operand.Role.VALUE_WRITE),f.read(op,"src",x,Operand.Role.VALUE_READ)));}f.sequence("start",list,f.halt("h"));ValidationResult result=AirValidator.validate(f.build());check(result.isStructurallyValid(),result.toString());eq(1000L,result.statistics().domainQueries());});
        test("two independent constructions validate identically",()->eq(AirValidator.validate(transmission(true).build()),AirValidator.validate(transmission(true).build())));
    }
    private static Fixtures openChoiceCopy(boolean wholeProof,boolean contradiction) {
        Fixtures f=new Fixtures();Types.TypeRef unknown=new Types.UnknownType(f.uncertainty("type","TYPE_UNKNOWN"));
        ObjectId src=f.object("src",contradiction?known(Types.Builtin.INT):known(Types.Builtin.TEXT));
        ObjectId dst=f.object("dst",known(Types.Builtin.TEXT));OperationId a=f.op("a");
        Place candidate=f.place(a,"candidate",src,Operand.Role.VALUE_READ);
        Place choice=new Places.Choice(f.operand(a,"choice",Operand.Role.VALUE_READ),List.of(candidate),new Scopes.WithinMemory(new Scopes.VisibleMemory(f.unit,true)),unknown,Optional.empty());
        Expression value=new Expressions.Read(f.operand(a,"read",Operand.Role.VALUE_READ),choice);
        f.linear(f.assign("a",dst,value));
        if(wholeProof) f.proof("whole-choice",new OperandDomain(choice.header().id()),new ObjectDomain(dst),new OperationDomain(a));
        else f.proof("candidate-only",new ObjectDomain(src),new ObjectDomain(dst),new OperationDomain(a));
        return f;
    }
    private static void copyProofForOtherOccurrence(Fixtures f) {
        // A premise about the candidate occurrence still does not quantify over the whole choice.
        f.proof("other-occurrence",new OperandDomain(new OperandId(new OperationOwner(f.op("a")),"candidate")),new ObjectDomain(f.objects.get(1).id()),new OperationDomain(f.op("a")));
    }
    private static Fixtures transmission(boolean proof) {
        Fixtures f=invokeFixture(true);Operations.Invoke old=(Operations.Invoke)f.sequences.get(0).terminator();
        ContractId contract=new ContractId(f.pub,"signature");Types.TypeRef type=f.objects.get(0).typeRef();
        Interactions.Signature signature=new Interactions.Signature(List.of(new Interactions.Parameter(0,Interactions.PassingMode.VALUE,type,Optional.empty(),f.origin)),List.of(),Optional.empty());
        f.contracts.add(new Interactions.Contract(contract,"fixture",new SemanticVersion(1,0,0),signature,Optional.empty(),List.of(),List.of(),f.origin));
        Expression arg=f.read(f.op("k"),"argument",f.objects.get(0).id(),Operand.Role.ARGUMENT_VALUE);
        Operations.Invoke invoke=new Operations.Invoke(old.header(),old.action(),old.target(),List.of(new Interactions.ValueArgument(arg)),List.of(),old.effectBound(),old.outcomes(),new Interactions.ContractKnowledge(Optional.of(contract),Optional.empty()),List.of());
        f.sequences.set(0,new Sequence(f.label("start"),List.of(),invoke,f.origin));
        if(proof) transmissionProof(f,new InvocationDomain(f.op("k")));
        return f;
    }
    private static void transmissionProof(Fixtures f,DomainProofScope scope) {
        f.proof("transmit",new OperandDomain(new OperandId(new OperationOwner(f.op("k")),"argument")),new CallParameterDomain(f.op("k"),new Interactions.ExternalSignature(new ContractId(f.pub,"signature")),0),scope);
    }
    private static Fixtures regionFixture(int offset,int size,Memory.Codec codec,Types.TypeRef type) {
        Fixtures f=minimal();f.capabilities.add(Capabilities.MEMORY_REGIONS);StorageId region=new StorageId(f.pub,"region");
        f.storage.add(new Memory.Region(new Memory.StorageHeader(region,Optional.of(f.unit),Memory.Lifetime.ACTIVATION,Memory.Visibility.PRIVATE,f.origin),Optional.of(BigInteger.valueOf(4)),Optional.empty()));
        f.objects.add(new Memory.ObjectDeclaration(new ObjectId(f.unit,"view"),Optional.of("view"),type,new Memory.ViewBinding(region,BigInteger.valueOf(offset),BigInteger.valueOf(size),codec),Memory.Visibility.PRIVATE,f.origin,Evidence.CoverageStatus.MODELED,f.precision()));return f;
    }
    private static Fixtures regionWrite(String value) {
        Fixtures f=regionFixture(0,4,Memory.AsciiText.INSTANCE,known(Types.Builtin.TEXT));f.sequences.clear();f.linear(f.assign("a",f.objects.get(0).id(),f.text(f.op("a"),"source",value)));return f;
    }
    private static Fixtures minimal(){Fixtures f=new Fixtures();f.sequence("start",List.of(),f.halt("stop"));return f;}
    private static Fixtures unknownCopy(){Fixtures f=new Fixtures();Types.TypeRef t=new Types.UnknownType(f.uncertainty("type","TYPE_UNKNOWN"));ObjectId x=f.object("x",t),y=f.object("y",t);f.linear(f.assign("a",y,f.read(f.op("a"),"source",x,Operand.Role.VALUE_READ)));return f;}
    private static void copyProof(Fixtures f,DomainProofScope scope){f.proof("copy-domain",new ObjectDomain(f.objects.get(0).id()),new ObjectDomain(f.objects.get(1).id()),scope);}
    private static Fixtures sliceFixture(int start,int count){Fixtures f=new Fixtures();ObjectId x=f.object("x",known(Types.Builtin.TEXT));OperationId a=f.op("a");Expression slice=new Expressions.SliceText(f.operand(a,"slice",Operand.Role.VALUE_READ),f.text(a,"text","A\uD83D\uDE00"),f.integer(a,"start",start,Operand.Role.VALUE_READ),f.integer(a,"count",count,Operand.Role.VALUE_READ),Optional.empty());f.linear(f.assign("a",x,slice));return f;}
    private static Fixtures choiceFixture(boolean known,boolean open){Fixtures f=new Fixtures();ObjectId x=f.object("x",known(Types.Builtin.TEXT)),y=f.object("y",known?known(Types.Builtin.TEXT):known(Types.Builtin.INT));UncertaintyId u=f.uncertainty("type","TYPE_UNKNOWN"),gap=f.uncertainty("gap","SOURCE_SEMANTICS_UNAVAILABLE");OperationId o=f.op("o");List<Place> candidates=List.of(f.place(o,"x",x,Operand.Role.VALUE_READ),f.place(o,"y",y,Operand.Role.VALUE_READ));Types.TypeRef t=known?known(Types.Builtin.TEXT):new Types.UnknownType(u);if(known && !open) candidates=List.of(f.place(o,"x",x,Operand.Role.VALUE_READ),f.place(o,"y",f.object("z",known(Types.Builtin.INT)),Operand.Role.VALUE_READ));Place choice=new Places.Choice(f.operand(o,"choice",Operand.Role.VALUE_READ),candidates,open?new Scopes.WithinMemory(new Scopes.VisibleMemory(f.unit,true)):Scopes.NoMemory.INSTANCE,t,Optional.empty());f.sequence("start",List.of(),new Operations.Opaque(f.header(o,gap),"test.choice",List.of(choice),List.of(),f.envelope(null)));return f;}
    private static Fixtures invokeFixture(boolean text){Fixtures f=new Fixtures();Types.TypeRef t=new Types.UnknownType(f.uncertainty("type","TYPE_UNKNOWN"));ObjectId x=f.object("x",t);UncertaintyId v=f.uncertainty("target","RESOURCE_TARGET_UNKNOWN"),contract=f.uncertainty("contract","CONTRACT_UNKNOWN");OperationId k=f.op("k");Expression name=new Expressions.Unknown(f.operand(k,"name",Operand.Role.CALL_TARGET),text?known(Types.Builtin.TEXT):t,List.of(f.read(k,"read",x,Operand.Role.VALUE_READ)),Scopes.NoMemory.INSTANCE,v);f.sequence("start",List.of(),new Operations.Invoke(f.header(k,v),"call",new Interactions.ComputedTarget("program","test.program",name,Interactions.ExactName.INSTANCE,f.origin),List.of(),List.of(),f.effects(),new Control.Envelope(List.of(new Control.Normal(f.label("end")),new Control.AnyException(Control.Propagate.INSTANCE),Control.HaltAlternative.INSTANCE,Control.Diverge.INSTANCE),Scopes.NoControl.INSTANCE),new Interactions.ContractKnowledge(Optional.empty(),Optional.of(contract)),List.of(contract)));f.sequence("end",List.of(),f.halt("h"));return f;}
    private static void valid(Publication p){ValidationResult r=AirValidator.validate(p);check(r.isStructurallyValid(),r.issues().toString());}
    private static void invalid(Publication p,String rule){ValidationResult r=AirValidator.validate(p);check(r.status()==ValidationResult.Status.INVALID_IR,"expected INVALID_IR, got "+r);check(r.issues().stream().anyMatch(i->i.kind()==ValidationIssue.Kind.INVALID_IR&&i.rule().equals(rule)),"missing "+rule+" in "+r.issues());}
    private static void architecture(){try{Path root=Path.of("src/main/java");try(var paths=Files.walk(root)){for(Path p:paths.filter(x->x.toString().endsWith(".java")).toList()){String text=Files.readString(p,StandardCharsets.UTF_8);for(String banned:List.of("com.fasterxml.jackson","com.google.gson","java.nio.file","java.io.","org.antlr","cobolexplorer","org.springframework"))check(!text.contains(banned),"forbidden dependency in "+p+": "+banned);}}}catch(java.io.IOException e){throw new AssertionError(e);}}
    private static void test(String name,Runnable test){try{test.run();passed++;System.out.println("ok "+passed+" - "+name);}catch(Throwable e){System.err.println("FAIL - "+name);throw e;}}
    private static void eq(java.lang.Object expected,java.lang.Object actual){check(Objects.equals(expected,actual),"expected "+expected+", got "+actual);}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static void throwsType(Class<? extends Throwable> type,Runnable action){try{action.run();}catch(Throwable e){if(type.isInstance(e))return;throw new AssertionError("unexpected exception",e);}throw new AssertionError("expected "+type.getSimpleName());}
}
