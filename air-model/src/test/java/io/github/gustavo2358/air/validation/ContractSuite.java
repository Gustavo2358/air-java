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
        test("CORE-SIZE iterative semantic nesting",CapacityChecks::nesting);
        test("CORE-SIZE retention continues validation",CapacityChecks::retention);
        test("CORE-SIZE independent cardinality series",CapacityChecks::series);
        test("CORE-SIZE indexed reference lookup",CapacityChecks::indexedReferences);
        test("CORE-SIZE operational failure and counters",CapacityChecks::operational);
        test("CORE-SIZE omitted kinds retain classification",CapacityChecks::omittedKinds);
        test("CORE-SIZE recursive families use explicit frames",CapacityChecks::recursiveFamilies);
        test("semver parse",()->eq(SemanticVersion.AIR_2_0_0,SemanticVersion.parse("2.0.0")));
        test("bad semver rejected",()->throwsType(IllegalArgumentException.class,()->SemanticVersion.parse("02.0.0")));
        test("negative version rejected",()->throwsType(IllegalArgumentException.class,()->new SemanticVersion(BigInteger.valueOf(-1),BigInteger.ZERO,BigInteger.ZERO)));
        test("empty text retained",()->eq("",new Values.TextValue("").value()));
        test("unpaired surrogate rejected",()->throwsType(IllegalArgumentException.class,()->new Values.TextValue("\uD800")));
        test("supplementary scalar accepted",()->eq(2,new Values.TextValue("\uD83D\uDE00").value().length()));
        test("bytes input defensive copy",()->{ byte[] b={0,-1}; Values.BytesValue v=Values.BytesValue.of(b); b[0]=12; eq(List.of(0,255),v.octets()); });
        test("bytes output defensive copy",()->{ Values.BytesValue v=Values.BytesValue.of(new byte[]{1});v.toByteArray()[0]=9;eq(List.of(1),v.octets()); });
        test("invalid octet rejected",()->throwsType(IllegalArgumentException.class,()->new Values.BytesValue(List.of(256))));
        test("decimal semantic equality",()->eq(Values.semanticKey(new Values.DecimalValue(BigInteger.valueOf(120),BigInteger.TWO)),Values.semanticKey(new Values.DecimalValue(BigInteger.valueOf(12),BigInteger.ONE))));
        test("negative decimal scale rejected",()->throwsType(IllegalArgumentException.class,()->new Values.DecimalValue(BigInteger.ONE,BigInteger.valueOf(-1))));
        test("source end before start rejected",()->throwsType(IllegalArgumentException.class,()->new Origins.Span(new Origins.Position(BigInteger.TWO,BigInteger.ONE),new Origins.Position(BigInteger.ONE,BigInteger.ONE),BigInteger.ONE,BigInteger.ONE,Origins.ColumnUnit.UNICODE_SCALAR,true)));
        test("minimal publication valid",()->valid(minimal().build()));
        test("pure core has no JSON/frontend imports",ContractSuite::architecture);
        test("publication lists immutable",()->{ Publication p=minimal().build(); throwsType(UnsupportedOperationException.class,()->p.units().clear()); });
        test("constructor snapshots source list",()->{ Fixtures f=minimal(); Publication p=f.build(); f.sequences.clear();eq(1,p.units().get(0).sequences().size()); });
        test("terminator required",()->{ Fixtures f=minimal();throwsType(NullPointerException.class,()->new Sequence(f.label("s"),List.of(),null,f.origin)); });
        test("empty closed control is not divergence",()->throwsType(IllegalArgumentException.class,()->new Control.ControlEnvelope(List.of(),Scopes.NoControl.INSTANCE)));
        test("explicit divergence representable",()->eq(1,new Control.ControlEnvelope(List.of(Control.Diverge.INSTANCE),Scopes.NoControl.INSTANCE).known().size()));
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
        test("duplicate decimal dispatch keys rejected",()->{Fixtures f=new Fixtures();OperationId d=f.op("d");Expression select=new Expressions.Literal(f.operand(d,"v",Operand.Role.CONTROL_TARGET),new Values.DecimalValue(BigInteger.TEN,BigInteger.ONE));f.sequence("start",List.of(),new Operations.Dispatch(f.header(d),select,List.of(new Operations.Case(new Values.DecimalValue(BigInteger.TEN,BigInteger.ONE),f.label("end")),new Operations.Case(new Values.DecimalValue(BigInteger.ONE,BigInteger.ZERO),f.label("end"))),f.label("end")));f.sequence("end",List.of(),f.halt("h"));invalid(f.build(),"I-20");});
        test("space padding accepted",()->{Fixtures f=new Fixtures();ObjectId x=f.object("x",known(Types.Builtin.TEXT));Expression fit=new Expressions.FitText(f.operand(f.op("a"),"fit",Operand.Role.VALUE_READ),f.text(f.op("a"),"source","A"),BigInteger.valueOf(8)," ");f.linear(f.assign("a",x,fit));valid(f.build());});
        test("text slice out of bounds rejected",()->{Fixtures f=sliceFixture(4,2);invalid(f.build(),"I-09/I-46");});
        test("text slice counts Unicode scalars",()->valid(sliceFixture(1,1).build()));
        test("empty closed choice rejected",()->{Fixtures f=minimal();throwsType(IllegalArgumentException.class,()->new Places.Choice(f.operand(f.op("c"),"p",Operand.Role.VALUE_READ),List.of(),Scopes.NoMemory.INSTANCE,known(Types.Builtin.TEXT)));});
        test("closed heterogeneous choice retains unknown type",()->{Fixtures f=choiceFixture(false,false);valid(f.build());});
        test("heterogeneous known choice rejected",()->{Fixtures f=choiceFixture(true,false);invalid(f.build(),"I-08");});
        test("open choice does not invent known remainder domain",()->{Fixtures f=choiceFixture(true,true);invalid(f.build(),"I-51");});
        test("opaque retains known reads",()->{Fixtures f=choiceFixture(false,false);eq(3,AirValidator.validate(f.build()).statistics().operands());});
        test("opaque may have two known normal destinations",()->{Fixtures f=minimal();Control.ControlEnvelope e=new Control.ControlEnvelope(List.of(new Control.Normal(f.label("a")),new Control.Normal(f.label("b"))),Scopes.NoControl.INSTANCE);eq(2,e.known().size());});
        test("opaque without uncertainty rejected",()->{Fixtures f=new Fixtures();f.sequence("start",List.of(),new Operations.Opaque(f.header(f.op("o")),"test.unknown",List.of(),List.of(),f.envelope(null)));invalid(f.build(),"I-26");});
        test("computed target requires known text",()->invalid(invokeFixture(false).build(),"I-08"));
        test("computed text unknown preserves dependency",()->valid(invokeFixture(true).build()));
        test("two normal invocation outcomes rejected",()->{Fixtures f=invokeFixture(true);Operations.Invoke old=(Operations.Invoke)f.sequences.get(0).terminator();Operations.Invoke changed=new Operations.Invoke(old.header(),old.action(),old.target(),old.arguments(),old.results(),old.signature(),old.effectOperands(),old.effectBound(),new Control.InvocationOutcomes(List.of(new Control.Normal(f.label("end")),new Control.Normal(f.label("start"))),Scopes.NoControl.INSTANCE),old.contract());f.sequences.set(0,new Sequence(f.label("start"),List.of(),changed,f.origin));invalid(f.build(),"I-60");});
        test("return does not acquire implicit fallthrough",()->{Fixtures f=new Fixtures();f.sequence("a",List.of(),new Operations.Return(f.header(f.op("r")),List.of()));f.sequence("b",List.of(),f.halt("h"));valid(f.build());eq(2,f.build().units().get(0).sequences().size());});
        test("local control capability required",()->{Fixtures f=new Fixtures();f.sequence("start",List.of(),new Operations.LocalResume(f.header(f.op("resume")),f.envelope(null)));invalid(f.build(),"I-43");});
        test("local resume modeled without stack execution",()->{Fixtures f=new Fixtures();f.capabilities.add(Capabilities.LOCAL_CONTROL);f.sequence("start",List.of(),new Operations.LocalResume(f.header(f.op("resume")),f.envelope(null)));valid(f.build());});
        test("indirect label universe is explicit",()->{Fixtures f=new Fixtures();f.capabilities.add(Capabilities.INDIRECT_CONTROL);Types.LabelType t=new Types.LabelType(f.unit,List.of(f.label("end")));OperationId j=f.op("j");Expression target=new Expressions.Literal(f.operand(j,"target",Operand.Role.CONTROL_TARGET),new Values.LabelValue(f.label("end"),t));f.sequence("start",List.of(),new Operations.IndirectJump(f.header(j),target,t,f.envelope(null)));f.sequence("end",List.of(),f.halt("h"));valid(f.build());});
        test("label literal outside domain rejected",()->{Fixtures f=minimal();Types.LabelType t=new Types.LabelType(f.unit,List.of(f.label("a")));throwsType(IllegalArgumentException.class,()->new Values.LabelValue(f.label("b"),t));});
        test("unknown required extension is not silently accepted",()->{Fixtures f=minimal();f.capabilities.add(new Capabilities.Capability("vendor.some_extension","1"));eq(ValidationResult.Status.INCOMPLETE_VALIDATION,AirValidator.validate(f.build()).status());});
        test("entry literal initialization is not zero default",()->{Fixtures f=minimal();ObjectId x=f.object("x",known(Types.Builtin.TEXT));Place place=new Places.ObjectPlace(f.entryOperand("dst",Operand.Role.VALUE_WRITE),x);Expressions.Literal value=new Expressions.Literal(f.entryOperand("value",Operand.Role.VALUE_READ),new Values.TextValue("BOOT"));f.state=new Entries.EntryState(List.of(new Entries.InitialCondition(place,new Entries.LiteralInitial(value),f.origin,List.of())),List.of());valid(f.build());});
        test("conflicting alias initial states rejected",()->{Fixtures f=minimal();ObjectId x=f.object("x",known(Types.Builtin.TEXT)),y=f.alias("y",x,known(Types.Builtin.TEXT));List<Entries.InitialCondition> seeds=new ArrayList<>();for(int i=0;i<2;i++){ Place p=new Places.ObjectPlace(f.entryOperand("d"+i,Operand.Role.VALUE_WRITE),i==0?x:y);Expressions.Literal v=new Expressions.Literal(f.entryOperand("v"+i,Operand.Role.VALUE_READ),new Values.TextValue(i==0?"A":"B"));seeds.add(new Entries.InitialCondition(p,new Entries.LiteralInitial(v),f.origin,List.of())); }f.state=new Entries.EntryState(seeds,List.of());invalid(f.build(),"I-17");});
        test("missing input differs from empty inventory",()->{Fixtures f=minimal();Publication p=f.build();Evidence.Coverage coverage=new Evidence.Coverage(Evidence.InventoryStatus.UNAVAILABLE,new Scopes.PublicationScope(f.pub),List.of(),List.of());Publication bad=new Publication(p.id(),p.airVersion(),p.capabilities(),p.artifacts(),p.units(),p.storage(),p.resources(),p.artifactRelations(),p.origins(),coverage,p.uncertainties(),p.premises());invalid(bad,"I-28");});
        test("validation resource limit observable",()->{ValidationResult r=AirValidator.validate(minimal().build(),new ValidationOptions(10,2,100));eq(ValidationResult.Status.INCOMPLETE_VALIDATION,r.status());check(r.issues().stream().anyMatch(i->i.rule().equals("ANALYSIS_LIMIT")),"missing limit diagnostic");});
        test("incompatible AIR version is not accepted",()->{Publication p=minimal().build();Publication v=new Publication(p.id(),new SemanticVersion(BigInteger.ONE,BigInteger.ZERO,BigInteger.ZERO),p.capabilities(),p.artifacts(),p.units(),p.storage(),p.resources(),p.artifactRelations(),p.origins(),p.coverage(),p.uncertainties(),p.premises());eq(ValidationResult.Status.INCOMPLETE_VALIDATION,AirValidator.validate(v).status());});
        test("deterministic validation",()->{Publication p=invokeFixture(true).build();eq(AirValidator.validate(p),AirValidator.validate(p));});
        test("large scalar inventory is indexed once",()->{Fixtures f=new Fixtures();ObjectId x=f.object("x",known(Types.Builtin.TEXT));List<Instruction> instructions=new ArrayList<>();for(int i=0;i<6000;i++) instructions.add(f.assign("a"+i,x,f.text(f.op("a"+i),"value","V")));f.sequence("start",instructions,f.halt("h"));ValidationResult r=AirValidator.validate(f.build());check(r.isStructurallyValid(),r.issues().toString());eq(6001,r.statistics().operations());eq(12000,r.statistics().operands());eq(6000L,r.statistics().domainQueries());});
        test("concurrent validation has no shared mutable state",()->{Publication p=invokeFixture(true).build();List<ValidationResult> results=java.util.stream.IntStream.range(0,16).parallel().mapToObj(i->AirValidator.validate(p)).toList();check(results.stream().allMatch(results.get(0)::equals),"nondeterministic concurrent result");});
        additional();
        reconciliation();
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
        test("calculated slice without private certificate yields incomplete validation",()->{Fixtures f=new Fixtures();ObjectId x=f.object("x",known(Types.Builtin.TEXT)),i=f.object("i",known(Types.Builtin.INT));OperationId a=f.op("a");Expression slice=new Expressions.SliceText(f.operand(a,"slice",Operand.Role.VALUE_READ),f.text(a,"text","ABC"),f.read(a,"start",i,Operand.Role.VALUE_READ),f.integer(a,"count",1,Operand.Role.VALUE_READ));f.linear(f.assign("a",x,slice));eq(ValidationResult.Status.INCOMPLETE_VALIDATION,AirValidator.validate(f.build()).status());});
        test("diagnostic limit is never a successful validation",()->{Fixtures f=minimal();f.origins.clear();ValidationResult result=AirValidator.validate(f.build(),new ValidationOptions(32,1000,1));check(!result.isStructurallyValid(),"limit masked errors");check(result.diagnostics().traversalCompleted(),"retention must complete traversal");check(result.diagnostics().count(ValidationIssue.Kind.INVALID_IR)>result.issues().size(),"omitted issues not counted");});
        test("large number of unit-scoped domain premises is reusable",()->{Fixtures f=new Fixtures();Types.TypeRef t=new Types.UnknownType(f.uncertainty("type","TYPE_UNKNOWN"));ObjectId x=f.object("x",t),y=f.object("y",t);copyProof(f,new UnitDomain(f.unit));List<Instruction> list=new ArrayList<>();for(int i=0;i<1000;i++){OperationId op=f.op("a"+i);list.add(new Operations.Assign(f.header(op),f.place(op,"dst",y,Operand.Role.VALUE_WRITE),f.read(op,"src",x,Operand.Role.VALUE_READ)));}f.sequence("start",list,f.halt("h"));ValidationResult result=AirValidator.validate(f.build());check(result.isStructurallyValid(),result.toString());eq(1000L,result.statistics().domainQueries());});
        test("two independent constructions validate identically",()->eq(AirValidator.validate(transmission(true).build()),AirValidator.validate(transmission(true).build())));
    }

    private static void reconciliation() {
        contractsAndTargets();
        storageAndSafety();
        signaturesAndOutcomes();
        returnsIdentitiesAndProvenance();
    }

    private static void contractsAndTargets() {
        test("Publication has no contracts inventory",()->check(
                Arrays.stream(Publication.class.getRecordComponents())
                        .noneMatch(component -> component.getName().equals("contracts")),
                "legacy contracts component remains"));
        test("legacy contract entity and id are absent",()->{
            classAbsent("io.github.gustavo2358.air.model.Ids$ContractId");
            classAbsent("io.github.gustavo2358.air.model.Interactions$Contract");
            classAbsent("io.github.gustavo2358.air.model.Interactions$ContractName");
        });
        test("ContractRef requires evidence",()->throwsType(IllegalArgumentException.class,
                ()->new Interactions.ContractRef("fixture.authority","revision-1",List.of())));
        test("ContractRef dangling evidence is rejected",()->{
            Fixtures f=invokeFixture(true);
            Operations.Invoke old=(Operations.Invoke)f.sequences.get(0).terminator();
            Interactions.ContractRef reference=new Interactions.ContractRef(
                    "fixture.authority","revision-1",List.of(new OriginId(f.pub,"missing")));
            replaceTerminator(f,0,copyInvoke(old,old.signature(),old.arguments(),old.results(),
                    old.effectOperands(),old.effectBound(),old.outcomes(),
                    new Interactions.KnownContract(reference)));
            invalid(f.build(),"I-02");
        });
        test("unknown contract requires CONTRACT_UNKNOWN",()->{
            Fixtures f=literalInvokeFixture();
            UncertaintyId wrong=f.uncertainty("contract-wrong","TYPE_UNKNOWN");
            Operations.Invoke old=(Operations.Invoke)f.sequences.get(0).terminator();
            replaceTerminator(f,0,copyInvoke(old,old.signature(),old.arguments(),old.results(),
                    old.effectOperands(),old.effectBound(),old.outcomes(),
                    new Interactions.UnknownContract(wrong)));
            invalid(f.build(),"I-49");
        });
        test("external signature is materialized at invoke",()->{
            Operations.Invoke invoke=(Operations.Invoke)transmission(true).sequences.get(0).terminator();
            check(invoke.signature() instanceof Interactions.ExternalSignature,
                    "external signature was not embedded");
            eq(1,((Interactions.ExternalSignature)invoke.signature())
                    .signature().parameters().known().size());
        });
        test("same contract authority does not merge invocation sites",()->
                invalid(twoSiteTransmission(false).build(),"I-08/I-52"));
        test("each same-authority invocation can be proven independently",()->
                valid(twoSiteTransmission(true).build()));
        test("materialized contract truth remains a semantic obligation",()->{
            ValidationResult result=AirValidator.validate(literalInvokeFixture().build());
            issue(result,ValidationIssue.Kind.SEMANTIC_OBLIGATION,"I-56");
        });

        test("Target admits exactly internal literal computed",()->eq(
                Set.of("InternalTarget","LiteralTarget","ComputedTarget"),
                Arrays.stream(Interactions.Target.class.getPermittedSubclasses())
                        .map(Class::getSimpleName).collect(java.util.stream.Collectors.toSet())));
        test("ResourceTarget is absent",()->classAbsent(
                "io.github.gustavo2358.air.model.Interactions$ResourceTarget"));
        test("internal target and matching entry signature are valid",()->
                valid(internalInvokeFixture().build()));
        test("internal target may reference declared entry with unavailable body",()->
                valid(internalUnavailableInvoke()));
        test("internal signature must identify exactly its target entry",()->{
            Fixtures f=internalInvokeFixture();
            Operations.Invoke old=(Operations.Invoke)f.sequences.get(0).terminator();
            replaceTerminator(f,0,copyInvoke(old,
                    new Interactions.EntrySignature(new EntryId(f.unit,"other-entry")),
                    old.arguments(),old.results(),old.effectOperands(),old.effectBound(),
                    old.outcomes(),old.contract()));
            invalid(f.build(),"I-55");
        });
        test("literal target and embedded external signature are valid",()->
                valid(literalInvokeFixture().build()));
        test("computed target remains valid with known text abstraction",()->
                valid(invokeFixture(true).build()));
        test("ResourceId remains declarative rather than executable",()->{
            Fixtures f=minimal();
            ResourceId id=new ResourceId(f.pub,"declared");
            f.resources.add(new Interactions.Resource(id,
                    new Interactions.LiteralTarget("program","fixture","DECLARED",
                            Interactions.ExactName.INSTANCE,f.origin),f.origin));
            valid(f.build());
        });
        test("contract authority does not supply extension name policy",()->{
            Fixtures f=literalInvokeFixture();Operations.Invoke old=(Operations.Invoke)f.sequences.get(0).terminator();
            Interactions.Target target=new Interactions.LiteralTarget("program","fixture","callee",
                    new Interactions.ExtensionName("fixture.name-policy","1"),f.origin);
            Operations.Invoke changed=new Operations.Invoke(old.header(),old.action(),target,
                    old.arguments(),old.results(),old.signature(),old.effectOperands(),old.effectBound(),
                    old.outcomes(),old.contract());
            replaceTerminator(f,0,changed);
            invalid(f.build(),"I-43");
        });
    }

    private static void storageAndSafety() {
        test("disjoint_storage with two distinct bases is structurally valid",()->{
            Fixtures f=disjointFixture(DisjointCase.VALID);
            ValidationResult result=AirValidator.validate(f.build());
            check(result.isStructurallyValid(),result.toString());
            issue(result,ValidationIssue.Kind.SEMANTIC_OBLIGATION,"I-59");
        });
        test("disjoint_storage dangling base is rejected",()->
                invalid(disjointFixture(DisjointCase.DANGLING).build(),"I-02"));
        test("disjoint_storage duplicate base is rejected",()->
                invalid(disjointFixture(DisjointCase.DUPLICATE).build(),"I-58"));
        test("disjoint_storage singleton is rejected",()->
                invalid(disjointFixture(DisjointCase.SINGLETON).build(),"I-58"));
        test("disjoint_storage has no selective scope",()->
                recordOmits(Proofs.DisjointStorage.class,"scope"));
        test("different StorageIds do not invent domain independence",()->{
            Fixtures f=unknownCopy();
            eq(2,f.storage.size());
            check(f.premises.isEmpty(),"fixture unexpectedly has a premise");
            invalid(f.build(),"I-08/I-52");
        });
        test("disjoint_storage does not resolve type or alias facts",()->{
            Fixtures f=unknownCopy();
            f.premises.add(disjointPremise(f,List.of(
                    f.storage.get(0).header().id(),f.storage.get(1).header().id())));
            invalid(f.build(),"I-08/I-52");
        });

        test("private safety API is absent",()->{
            classAbsent("io.github.gustavo2358.air.model.Proofs$SafetyAssertion");
            classAbsent("io.github.gustavo2358.air.model.Proofs$SafetyProperty");
            productionSourceOmits(List.of("VALID_PURE_ACCESS","VALID_TEXT_SLICE",
                    "VALID_CODEC_WRITE","CHOICE_REMAINDER_DOMAIN",
                    "EXTENSION_EQUALITY_DEFINED"));
        });
        test("private proof pointer fields are absent",()->{
            recordOmits(Expressions.SliceText.class,"boundsProof");
            recordOmits(Places.RegionSlice.class,"accessProof");
            recordOmits(Places.Choice.class,"knownRemainderDomainProof");
            recordOmits(Memory.ByteRange.class,"boundsProof");
            recordOmits(Memory.ExtensionCodec.class,"contract");
        });
        test("unknown expression purity remains a semantic obligation",()->{
            ValidationResult result=AirValidator.validate(invokeFixture(true).build());
            issue(result,ValidationIssue.Kind.SEMANTIC_OBLIGATION,"I-09");
        });
        test("removing safety tokens does not permit invalid slice bounds",()->
                invalid(sliceFixture(4,2).build(),"I-09/I-46"));
        test("removing safety tokens does not permit invalid codec write",()->
                invalid(regionWrite("ABCé").build(),"I-46"));
        test("extension equality without interpreted manifest is incomplete",()->{
            Fixtures f=extensionEquality();
            ValidationResult result=AirValidator.validate(f.build());
            eq(ValidationResult.Status.INCOMPLETE_VALIDATION,result.status());
            issue(result,ValidationIssue.Kind.VALIDATION_LIMIT,"PRECONDITION_NOT_DISCHARGED");
        });
    }

    private static void signaturesAndOutcomes() {
        test("unknown_type at known position does not open arity",()->{
            Fixtures f=literalInvokeFixture();
            UncertaintyId type=f.uncertainty("parameter-type","TYPE_UNKNOWN");
            Interactions.Parameter parameter=externalParameter(BigInteger.ZERO,
                    new Interactions.KnownMode(Interactions.PassingMode.VALUE),
                    new Types.UnknownType(type),f.origin);
            Interactions.Signature signature=f.signature(List.of(parameter),List.of());
            Operations.Invoke old=(Operations.Invoke)f.sequences.get(0).terminator();
            replaceTerminator(f,0,copyInvoke(old,new Interactions.ExternalSignature(signature),
                    List.of(),old.results(),old.effectOperands(),old.effectBound(),old.outcomes(),old.contract()));
            invalid(f.build(),"I-08");
        });
        test("parameter and result remainders are independent values",()->{
            Fixtures f=minimal();
            UncertaintyId arity=f.uncertainty("parameter-arity","SOURCE_SEMANTICS_UNAVAILABLE");
            Interactions.Signature signature=new Interactions.Signature(
                    new Interactions.ParameterInventory(List.of(),
                            new Interactions.UnknownRemainder(arity)),
                    new Interactions.ResultInventory(List.of(),Interactions.NoRemainder.INSTANCE),
                    f.origin);
            check(signature.parameters().remainder() instanceof Interactions.UnknownRemainder,
                    "parameter remainder was lost");
            check(signature.results().remainder() instanceof Interactions.NoRemainder,
                    "result remainder was coupled to parameters");
        });
        test("open result remainder does not open closed parameter arity",()->{
            Fixtures f=literalInvokeFixture();
            UncertaintyId resultArity=f.uncertainty("result-arity","SOURCE_SEMANTICS_UNAVAILABLE");
            Interactions.Signature signature=new Interactions.Signature(
                    new Interactions.ParameterInventory(List.of(),Interactions.NoRemainder.INSTANCE),
                    new Interactions.ResultInventory(List.of(),new Interactions.UnknownRemainder(resultArity)),
                    f.origin);
            OperationId id=f.op("call");
            Expression argument=new Expressions.Literal(f.operand(id,"extra",Operand.Role.ARGUMENT_VALUE),
                    new Values.TextValue("extra"));
            Operations.Invoke old=(Operations.Invoke)f.sequences.get(0).terminator();
            replaceTerminator(f,0,copyInvoke(old,new Interactions.ExternalSignature(signature),
                    List.of(new Interactions.ValueArgument(argument)),old.results(),old.effectOperands(),
                    old.effectBound(),old.outcomes(),old.contract()));
            invalid(f.build(),"I-08");
        });
        test("open result remainder independently admits unknown extra result",()->{
            Fixtures f=literalInvokeFixture();
            ObjectId result=f.object("result",known(Types.Builtin.TEXT));
            UncertaintyId resultArity=f.uncertainty("result-arity","SOURCE_SEMANTICS_UNAVAILABLE");
            Interactions.Signature signature=new Interactions.Signature(
                    new Interactions.ParameterInventory(List.of(),Interactions.NoRemainder.INSTANCE),
                    new Interactions.ResultInventory(List.of(),new Interactions.UnknownRemainder(resultArity)),
                    f.origin);
            Operations.Invoke old=(Operations.Invoke)f.sequences.get(0).terminator();
            Place destination=f.place(f.op("call"),"result",result,Operand.Role.RESULT_TARGET);
            replaceTerminator(f,0,copyInvoke(old,new Interactions.ExternalSignature(signature),
                    old.arguments(),List.of(destination),old.effectOperands(),old.effectBound(),
                    old.outcomes(),old.contract()));
            valid(f.build());
        });
        test("open parameter remainder does not waive known-slot transmission",()->{
            Fixtures f=transmission(false);
            UncertaintyId arity=f.uncertainty("parameter-arity","SOURCE_SEMANTICS_UNAVAILABLE");
            Operations.Invoke old=(Operations.Invoke)f.sequences.get(0).terminator();
            Interactions.Signature previous=
                    ((Interactions.ExternalSignature)old.signature()).signature();
            Interactions.Signature signature=new Interactions.Signature(
                    new Interactions.ParameterInventory(previous.parameters().known(),
                            new Interactions.UnknownRemainder(arity)),
                    previous.results(),previous.origin());
            replaceTerminator(f,0,copyInvoke(old,new Interactions.ExternalSignature(signature),
                    old.arguments(),old.results(),old.effectOperands(),old.effectBound(),
                    old.outcomes(),old.contract()));
            invalid(f.build(),"I-08/I-52");
        });
        test("open result remainder does not waive known-slot transmission",()->{
            Fixtures f=literalInvokeFixture();
            Types.TypeRef signatureType=new Types.UnknownType(
                    f.uncertainty("signature-result-type","TYPE_UNKNOWN"));
            Types.TypeRef destinationType=new Types.UnknownType(
                    f.uncertainty("destination-result-type","TYPE_UNKNOWN"));
            UncertaintyId arity=f.uncertainty("result-arity","SOURCE_SEMANTICS_UNAVAILABLE");
            Interactions.Signature signature=new Interactions.Signature(
                    new Interactions.ParameterInventory(List.of(),Interactions.NoRemainder.INSTANCE),
                    new Interactions.ResultInventory(List.of(new Interactions.ResultSlot(
                            BigInteger.ZERO,signatureType,f.origin)),
                            new Interactions.UnknownRemainder(arity)),f.origin);
            Operations.Invoke old=(Operations.Invoke)f.sequences.get(0).terminator();
            ObjectId result=f.object("result",destinationType);
            Place destination=f.place(f.op("call"),"result",result,Operand.Role.RESULT_TARGET);
            replaceTerminator(f,0,copyInvoke(old,new Interactions.ExternalSignature(signature),
                    old.arguments(),List.of(destination),old.effectOperands(),old.effectBound(),
                    old.outcomes(),old.contract()));
            invalid(f.build(),"I-08/I-52");
        });
        test("sameDomain does not promote an argument to a required concrete type",()->{
            Fixtures f=transmission(true);
            Operations.Invoke old=(Operations.Invoke)f.sequences.get(0).terminator();
            Interactions.Parameter previous=((Interactions.ExternalSignature)old.signature())
                    .signature().parameters().known().get(0);
            Interactions.Parameter concrete=new Interactions.Parameter(previous.position(),
                    previous.mode(),known(Types.Builtin.TEXT),previous.objectBinding(),previous.origin());
            Interactions.Signature signature=f.signature(List.of(concrete),List.of());
            replaceTerminator(f,0,copyInvoke(old,new Interactions.ExternalSignature(signature),
                    old.arguments(),old.results(),old.effectOperands(),old.effectBound(),
                    old.outcomes(),old.contract()));
            invalid(f.build(),"I-08");
        });
        test("sameDomain does not promote a result destination to a required concrete type",()->{
            Fixtures f=literalInvokeFixture();
            Types.TypeRef destinationType=new Types.UnknownType(
                    f.uncertainty("destination-result-type","TYPE_UNKNOWN"));
            ObjectId result=f.object("result",destinationType);
            OperationId id=f.op("call");
            Place destination=f.place(id,"result",result,Operand.Role.RESULT_TARGET);
            Interactions.Signature signature=f.signature(List.of(),List.of(
                    new Interactions.ResultSlot(BigInteger.ZERO,known(Types.Builtin.TEXT),f.origin)));
            Operations.Invoke old=(Operations.Invoke)f.sequences.get(0).terminator();
            replaceTerminator(f,0,copyInvoke(old,new Interactions.ExternalSignature(signature),
                    old.arguments(),List.of(destination),old.effectOperands(),old.effectBound(),
                    old.outcomes(),old.contract()));
            f.proof("result-domain",new ExternalResultDomain(id,BigInteger.ZERO),
                    new OperandDomain(destination.header().id()),new InvocationDomain(id));
            invalid(f.build(),"I-08");
        });
        test("closed partial signature remains conservative under open value precision",()->{
            Fixtures f=transmission(false);
            UncertaintyId reason=f.uncertainty("transmission-precision",
                    "SOURCE_SEMANTICS_UNAVAILABLE");
            Operations.Invoke old=(Operations.Invoke)f.sequences.get(0).terminator();
            Evidence.Precision previous=old.header().precision();
            Evidence.Claim openValues=new Evidence.Claim(new Scopes.UnitScope(f.unit),
                    Evidence.PrecisionStatus.OPEN,List.of(reason));
            Evidence.Precision precision=new Evidence.Precision(previous.control(),
                    previous.storage(),previous.effects(),openValues,previous.dependencies());
            Operations.Header header=new Operations.Header(old.header().id(),old.header().origin(),
                    old.header().coverage(),precision,old.header().uncertainties());
            Operations.Invoke conservative=new Operations.Invoke(header,old.action(),old.target(),
                    old.arguments(),old.results(),old.signature(),old.effectOperands(),
                    old.effectBound(),old.outcomes(),old.contract());
            replaceTerminator(f,0,conservative);
            valid(f.build());
        });
        test("unknown passing mode remains a present parameter",()->{
            Fixtures f=literalInvokeFixture();
            UncertaintyId mode=f.uncertainty("mode","SOURCE_SEMANTICS_UNAVAILABLE");
            Interactions.Parameter parameter=externalParameter(BigInteger.ZERO,
                    new Interactions.UnknownMode(mode),known(Types.Builtin.TEXT),f.origin);
            Interactions.Signature signature=f.signature(List.of(parameter),List.of());
            OperationId id=f.op("call");
            Expression argument=new Expressions.Literal(f.operand(id,"argument",Operand.Role.ARGUMENT_VALUE),
                    new Values.TextValue("value"));
            Operations.Invoke old=(Operations.Invoke)f.sequences.get(0).terminator();
            replaceTerminator(f,0,copyInvoke(old,new Interactions.ExternalSignature(signature),
                    List.of(new Interactions.ValueArgument(argument)),old.results(),old.effectOperands(),
                    old.effectBound(),old.outcomes(),old.contract()));
            valid(f.build());
            eq(BigInteger.ZERO,parameter.position());
            check(parameter.mode() instanceof Interactions.UnknownMode,"unknown mode was erased");
        });
        test("external signature cannot invent called-unit object binding",()->{
            Fixtures f=literalInvokeFixture();
            ObjectId object=f.object("bound",known(Types.Builtin.TEXT));
            Interactions.Parameter parameter=new Interactions.Parameter(BigInteger.ZERO,
                    new Interactions.KnownMode(Interactions.PassingMode.VALUE),known(Types.Builtin.TEXT),
                    new Interactions.ObjectBinding(object),f.origin);
            Interactions.Signature signature=f.signature(List.of(parameter),List.of());
            Operations.Invoke old=(Operations.Invoke)f.sequences.get(0).terminator();
            replaceTerminator(f,0,copyInvoke(old,new Interactions.ExternalSignature(signature),
                    old.arguments(),old.results(),old.effectOperands(),old.effectBound(),old.outcomes(),old.contract()));
            invalid(f.build(),"I-55");
        });
        test("mathematical signature position is not capped by int",()->{
            Fixtures f=minimal();
            BigInteger position=BigInteger.ONE.shiftLeft(80);
            UncertaintyId arity=f.uncertainty("arity","SOURCE_SEMANTICS_UNAVAILABLE");
            UncertaintyId binding=f.uncertainty("binding","SOURCE_SEMANTICS_UNAVAILABLE");
            Interactions.Parameter parameter=new Interactions.Parameter(position,
                    new Interactions.KnownMode(Interactions.PassingMode.VALUE),known(Types.Builtin.TEXT),
                    new Interactions.UnknownParameterBinding(binding),f.origin);
            f.signature=new Interactions.Signature(
                    new Interactions.ParameterInventory(List.of(parameter),
                            new Interactions.UnknownRemainder(arity)),
                    new Interactions.ResultInventory(List.of(),Interactions.NoRemainder.INSTANCE),f.origin);
            valid(f.build());
            eq(position,f.signature.parameters().known().get(0).position());
        });
        test("proof cannot reference an unmaterialized external position",()->{
            Fixtures f=literalInvokeFixture();
            OperationId id=f.op("call");
            f.proof("missing-position",new ExternalParameterDomain(id,BigInteger.ZERO),
                    new ExternalParameterDomain(id,BigInteger.ZERO),new InvocationDomain(id));
            invalid(f.build(),"I-52");
        });

        test("all invocation outcome kinds and open remainder are preserved",()->{
            Fixtures f=literalInvokeFixture();Operations.Invoke old=(Operations.Invoke)f.sequences.get(0).terminator();
            Control.InvocationOutcomes outcomes=new Control.InvocationOutcomes(List.of(
                    new Control.Normal(f.label("end")),
                    new Control.Exceptional("io",Control.Propagate.INSTANCE),
                    new Control.AnyException(Control.Propagate.INSTANCE),
                    Control.HaltAlternative.INSTANCE,Control.Diverge.INSTANCE),
                    new Scopes.WithinControl(new Scopes.AllControl(f.pub)));
            replaceTerminator(f,0,copyInvoke(old,old.signature(),old.arguments(),old.results(),
                    old.effectOperands(),old.effectBound(),outcomes,old.contract()));
            valid(f.build());
            eq(5,outcomes.known().size());
            check(outcomes.remainder() instanceof Scopes.WithinControl,"outcome remainder was closed");
        });
        test("InvocationOutcomes rejects duplicate exception tag",()->{
            Fixtures f=literalInvokeFixture();
            Operations.Invoke old=(Operations.Invoke)f.sequences.get(0).terminator();
            Control.InvocationOutcomes outcomes=new Control.InvocationOutcomes(List.of(
                    new Control.Exceptional("io",Control.Propagate.INSTANCE),
                    new Control.Exceptional("io",Control.Propagate.INSTANCE)),Scopes.NoControl.INSTANCE);
            replaceTerminator(f,0,copyInvoke(old,old.signature(),old.arguments(),old.results(),
                    old.effectOperands(),old.effectBound(),outcomes,old.contract()));
            invalid(f.build(),"I-60");
        });
        test("InvocationOutcomes rejects duplicate catch-all",()->{
            Fixtures f=literalInvokeFixture();
            Operations.Invoke old=(Operations.Invoke)f.sequences.get(0).terminator();
            Control.InvocationOutcomes outcomes=new Control.InvocationOutcomes(List.of(
                    new Control.AnyException(Control.Propagate.INSTANCE),
                    new Control.AnyException(Control.Propagate.INSTANCE)),Scopes.NoControl.INSTANCE);
            replaceTerminator(f,0,copyInvoke(old,old.signature(),old.arguments(),old.results(),
                    old.effectOperands(),old.effectBound(),outcomes,old.contract()));
            invalid(f.build(),"I-60");
        });
        test("outcome-specific effect keys are unique",()->{
            Fixtures f=literalInvokeFixture();
            Operations.Invoke old=(Operations.Invoke)f.sequences.get(0).terminator();
            Interactions.OutcomeEffects normal=new Interactions.OutcomeEffects(
                    Control.NormalOutcome.INSTANCE,old.effectBound().otherwise());
            Interactions.EffectBound effects=new Interactions.EffectBound(old.effectBound().otherwise(),
                    List.of(normal,normal));
            replaceTerminator(f,0,copyInvoke(old,old.signature(),old.arguments(),old.results(),
                    old.effectOperands(),effects,old.outcomes(),old.contract()));
            invalid(f.build(),"I-60");
        });
        test("InvocationAlternative excludes jump return continue",()->eq(
                Set.of("Normal","Exceptional","AnyException","HaltAlternative","Diverge"),
                Arrays.stream(Control.InvocationAlternative.class.getPermittedSubclasses())
                        .map(Class::getSimpleName).collect(java.util.stream.Collectors.toSet())));
        test("generic ControlEnvelope preserves jump and return",()->
                valid(opaqueControlFixture(new Control.ControlEnvelope(List.of(
                        new Control.JumpAlternative(new LabelId(new UnitId(new PublicationId("fixture"),"unit"),"end")),
                        Control.ReturnAlternative.INSTANCE),Scopes.NoControl.INSTANCE)).build()));
        test("continue is accepted only by common-operation fallback",()->
                valid(copyBytesContinueFixture().build()));
        test("continue in opaque terminator is rejected",()->
                invalid(opaqueControlFixture(new Control.ControlEnvelope(
                        List.of(Control.ContinueAlternative.INSTANCE),Scopes.NoControl.INSTANCE)).build(),"I-60"));
        test("empty closed invocation outcomes are not implicit divergence",()->
                throwsType(IllegalArgumentException.class,()->new Control.InvocationOutcomes(
                        List.of(),Scopes.NoControl.INSTANCE)));
        test("effect-only place occurrence is materialized once",()->{
            Fixtures f=effectOperandFixture();
            ValidationResult result=AirValidator.validate(f.build());
            check(result.isStructurallyValid(),result.toString());
            check(result.statistics().operands()>=1,"effect-only operand was not indexed");
        });
        test("opaque result references one pre-existing occurrence",()->{
            Fixtures f=opaqueResultFixture();
            ValidationResult result=AirValidator.validate(f.build());
            check(result.isStructurallyValid(),result.toString());
            eq(1,result.statistics().operands());
        });
    }

    private static void returnsIdentitiesAndProvenance() {
        test("Return has no entryScope component",()->recordOmits(Operations.Return.class,"entryScope"));
        test("incompatible multi-entry return records limit without selecting entry",()->{
            ValidationResult result=AirValidator.validate(multiEntryReturn(false));
            eq(ValidationResult.Status.INCOMPLETE_VALIDATION,result.status());
            issue(result,ValidationIssue.Kind.VALIDATION_LIMIT,"PRECONDITION_NOT_DISCHARGED");
            issue(result,ValidationIssue.Kind.SEMANTIC_OBLIGATION,"I-61");
            check(result.issues().stream().noneMatch(i->i.kind()==ValidationIssue.Kind.INVALID_IR),
                    "validator arbitrarily rejected one entry");
        });
        test("compatible multi-entry return validates all entries",()->
                valid(multiEntryReturn(true)));
        test("open result remainder does not waive a known return slot",()->
                invalid(openRemainderReturn(false).build(),"I-08/I-52"));
        test("open result remainder preserves a proven return slot",()->
                valid(openRemainderReturn(true).build()));

        test("ArtifactRelationId closes a valid relation",()->{
            Fixtures f=artifactRelationFixture(false);
            valid(f.build());
            check(f.artifactRelations.get(0).id() instanceof ArtifactRelationId,
                    "relation lacks ArtifactRelationId");
        });
        test("artifact relation dangling destination is rejected",()->
                invalid(artifactRelationFixture(true).build(),"I-02"));
        test("artifact relation id cannot cross publication",()->{
            Fixtures f=artifactRelationFixture(false);
            Artifacts.Relation old=f.artifactRelations.get(0);
            f.artifactRelations.set(0,new Artifacts.Relation(
                    new ArtifactRelationId(new PublicationId("other"),"relation"),old.source(),
                    old.destination(),old.kind(),old.origin(),old.coverage()));
            invalid(f.build(),"I-01");
        });
        test("entry owner is checked",()->{
            Fixtures f=minimal();Publication p=f.build();Unit base=p.units().get(0);
            EntryId wrong=new EntryId(new UnitId(f.pub,"other-unit"),"entry");
            Entries.Entry entry=new Entries.Entry(wrong,Optional.of(base.sequences().get(0).label()),
                    f.signature,f.state,f.origin);
            Unit changed=unitWith(base,List.of(entry),base.sequences(),base.completionPorts());
            invalid(Fixtures.withUnits(p,List.of(changed)),"I-05");
        });
        test("sequence label owner is checked",()->{
            Fixtures f=minimal();Publication p=f.build();Unit base=p.units().get(0);
            Sequence old=base.sequences().get(0);
            Sequence wrong=new Sequence(new LabelId(new UnitId(f.pub,"other-unit"),"start"),
                    old.instructions(),old.terminator(),old.origin());
            Unit changed=unitWith(base,base.entries(),List.of(wrong),base.completionPorts());
            invalid(Fixtures.withUnits(p,List.of(changed)),"I-03");
        });
        test("operation owner is checked",()->{
            Fixtures f=new Fixtures();
            OperationId wrong=new OperationId(new UnitId(f.pub,"other-unit"),"halt");
            f.sequence("start",List.of(),new Operations.Halt(f.header(wrong),Operations.HaltKind.NORMAL));
            invalid(f.build(),"I-03");
        });
        test("object owner is checked",()->{
            Fixtures f=minimal();ObjectId original=f.object("object",known(Types.Builtin.TEXT));
            Memory.ObjectDeclaration old=f.objects.stream().filter(o->o.id().equals(original)).findFirst().orElseThrow();
            f.objects.remove(old);
            f.objects.add(new Memory.ObjectDeclaration(new ObjectId(new UnitId(f.pub,"other-unit"),"object"),
                    old.displayName(),old.typeRef(),old.storage(),old.visibility(),old.origin(),
                    old.coverage(),old.precision()));
            invalid(f.build(),"I-01");
        });
        test("operand owner is checked",()->{
            Fixtures f=new Fixtures();ObjectId target=f.object("target",known(Types.Builtin.TEXT));
            OperationId a=f.op("assign"),other=f.op("other");
            Place destination=new Places.ObjectPlace(f.operand(other,"destination",Operand.Role.VALUE_WRITE),target);
            Expression value=f.text(a,"value","x");
            f.linear(new Operations.Assign(f.header(a),destination,value));
            invalid(f.build(),"I-11");
        });
        test("same localId in distinct identity domains does not collide",()->
                valid(sameLocalIdsFixture().build()));
        test("entry-owned and operation-owned operand localIds do not collide",()->
                valid(distinctOperandOwnersFixture().build()));

        test("completion port requires local-control capability",()->{
            Fixtures f=minimal();
            f.completionPorts.add(new Entries.CompletionPort(
                    new CompletionPortId(f.unit,"port"),f.origin));
            invalid(f.build(),"I-43");
        });
        test("completion port owner closes under local-control capability",()->{
            Fixtures f=minimal();f.capabilities.add(Capabilities.LOCAL_CONTROL);
            f.completionPorts.add(new Entries.CompletionPort(
                    new CompletionPortId(f.unit,"port"),f.origin));
            valid(f.build());
        });
        test("entry without label is valid only for unavailable body",()->
                valid(unavailableBody(false)));
        test("unavailable body cannot publish dangling entry label",()->
                invalid(unavailableBody(true),"I-02"));
        test("available body entry without label is rejected",()->{
            Fixtures f=minimal();Publication p=f.build();Unit base=p.units().get(0);
            Entries.Entry entry=new Entries.Entry(f.entry(),Optional.empty(),f.signature,f.state,f.origin);
            Unit changed=unitWith(base,List.of(entry),base.sequences(),base.completionPorts());
            invalid(Fixtures.withUnits(p,List.of(changed)),"I-05");
        });

        test("offset provenance survives without fabricated line columns",()->{
            Fixtures f=minimal();ArtifactId artifact=new ArtifactId(f.pub,"source");
            OriginId writtenId=new OriginId(f.pub,"offset-origin");
            BigInteger end=BigInteger.ONE.shiftLeft(72);
            Origins.Offsets offsets=new Origins.Offsets(BigInteger.ZERO,end,"octet",true);
            f.artifacts.add(new Origins.Artifact(artifact,"source.bin",Optional.empty()));
            f.origins.add(new Origins.Written(writtenId,artifact,Optional.of(offsets),List.of(),true));
            Publication publication=f.build();valid(publication);
            Origins.Written written=(Origins.Written)publication.origins().stream()
                    .filter(origin->origin.id().equals(writtenId)).findFirst().orElseThrow();
            check(written.location().orElseThrow() instanceof Origins.Offsets,
                    "offset location was converted to line/column");
            eq(end,((Origins.Offsets)written.location().orElseThrow()).end());
        });
        test("coverage elimination uses rule and origin, not safety premise",()->{
            Fixtures f=minimal();Publication p=f.build();
            Evidence.CoverageItem item=new Evidence.CoverageItem("removed",f.origin,
                    Evidence.CoverageStatus.MODELED,List.of(),List.of(),
                    Optional.of(new Evidence.Elimination("source construct eliminated",f.origin)));
            Evidence.Coverage coverage=new Evidence.Coverage(Evidence.InventoryStatus.COMPLETE,
                    new Scopes.PublicationScope(f.pub),List.of(item),List.of());
            Publication changed=new Publication(p.id(),p.airVersion(),p.capabilities(),p.artifacts(),
                    p.units(),p.storage(),p.resources(),p.artifactRelations(),p.origins(),coverage,
                    p.uncertainties(),p.premises());
            valid(changed);
        });
        test("computed dependency name must be observed before owning operation",()->
                invalid(computedDependencyWrongPoint().build(),"I-11"));
    }

    private enum DisjointCase { VALID, DANGLING, DUPLICATE, SINGLETON }

    private static Operations.Invoke copyInvoke(Operations.Invoke old,
                                                 Interactions.InvocationSignature signature,
                                                 List<Interactions.Argument> arguments,
                                                 List<Place> results,
                                                 List<Place> effectOperands,
                                                 Interactions.EffectBound effectBound,
                                                 Control.InvocationOutcomes outcomes,
                                                 Interactions.ContractKnowledge contract) {
        return new Operations.Invoke(old.header(),old.action(),old.target(),arguments,results,
                signature,effectOperands,effectBound,outcomes,contract);
    }

    private static void replaceTerminator(Fixtures f,int index,Terminator terminator) {
        Sequence old=f.sequences.get(index);
        f.sequences.set(index,new Sequence(old.label(),old.instructions(),terminator,old.origin()));
    }

    private static Interactions.ContractRef contract(Fixtures f) {
        return new Interactions.ContractRef("fixture.authority","revision-1",List.of(f.origin));
    }

    private static Control.InvocationOutcomes normalOutcomes(Fixtures f,String label) {
        return new Control.InvocationOutcomes(List.of(new Control.Normal(f.label(label))),
                Scopes.NoControl.INSTANCE);
    }

    private static Interactions.Parameter externalParameter(BigInteger position,
                                                            Interactions.ModeKnowledge mode,
                                                            Types.TypeRef type,
                                                            OriginId origin) {
        return new Interactions.Parameter(position,mode,type,Interactions.ExternalBinding.INSTANCE,origin);
    }

    private static Fixtures literalInvokeFixture() {
        Fixtures f=new Fixtures();OperationId id=f.op("call");
        Operations.Invoke invoke=new Operations.Invoke(f.header(id),"call",
                new Interactions.LiteralTarget("program","fixture","CALLEE",
                        Interactions.ExactName.INSTANCE,f.origin),
                List.of(),List.of(),new Interactions.ExternalSignature(f.signature(List.of(),List.of())),
                List.of(),f.effects(),normalOutcomes(f,"end"),
                new Interactions.KnownContract(contract(f)));
        f.sequence("start",List.of(),invoke);f.sequence("end",List.of(),f.halt("halt"));
        return f;
    }

    private static Fixtures internalInvokeFixture() {
        Fixtures f=new Fixtures();OperationId id=f.op("call");
        Operations.Invoke invoke=new Operations.Invoke(f.header(id),"call",
                new Interactions.InternalTarget(f.entry()),List.of(),List.of(),
                new Interactions.EntrySignature(f.entry()),List.of(),f.effects(),
                normalOutcomes(f,"end"),new Interactions.KnownContract(contract(f)));
        f.sequence("start",List.of(),invoke);f.sequence("end",List.of(),f.halt("halt"));
        return f;
    }

    private static Publication internalUnavailableInvoke() {
        Fixtures f=new Fixtures();UnitId calleeUnit=new UnitId(f.pub,"callee");
        EntryId calleeEntry=new EntryId(calleeUnit,"entry");
        UncertaintyId bodyReason=new UncertaintyId(f.pub,"callee-body");
        f.uncertainties.add(new Evidence.Uncertainty(bodyReason,"INPUT_MISSING",
                List.of(Evidence.Dimension.CONTROL),new Scopes.UnitScope(calleeUnit),
                "callee body unavailable",f.origin));
        OperationId id=f.op("call");
        Operations.Invoke invoke=new Operations.Invoke(f.header(id),"call",
                new Interactions.InternalTarget(calleeEntry),List.of(),List.of(),
                new Interactions.EntrySignature(calleeEntry),List.of(),f.effects(),
                normalOutcomes(f,"end"),new Interactions.KnownContract(contract(f)));
        f.sequence("start",List.of(),invoke);f.sequence("end",List.of(),f.halt("halt"));
        Publication publication=f.build();
        Entries.Entry entry=new Entries.Entry(calleeEntry,Optional.empty(),
                f.signature(List.of(),List.of()),new Entries.EntryState(List.of(),List.of()),f.origin);
        Unit callee=new Unit(calleeUnit,Optional.empty(),List.of(),List.of(),List.of(entry),
                List.of(),List.of(),Unit.BodyAvailability.UNAVAILABLE,Optional.of(bodyReason),
                f.coverage(new Scopes.UnitScope(calleeUnit)),f.origin);
        return Fixtures.withUnits(publication,List.of(publication.units().get(0),callee));
    }

    private static Fixtures twoSiteTransmission(boolean proveSecond) {
        Fixtures f=new Fixtures();
        Types.TypeRef type=new Types.UnknownType(f.uncertainty("type","TYPE_UNKNOWN"));
        ObjectId source=f.object("source",type);
        Interactions.ContractRef reference=contract(f);
        for(int i=1;i<=2;i++) {
            String suffix=Integer.toString(i);OperationId id=f.op("call-"+suffix);
            Interactions.Parameter parameter=externalParameter(BigInteger.ZERO,
                    new Interactions.KnownMode(Interactions.PassingMode.VALUE),type,f.origin);
            Interactions.Signature signature=f.signature(List.of(parameter),List.of());
            Expression argument=f.read(id,"argument-"+suffix,source,Operand.Role.ARGUMENT_VALUE);
            Operations.Invoke invoke=new Operations.Invoke(f.header(id),"call",
                    new Interactions.LiteralTarget("program","fixture","SHARED",
                            Interactions.ExactName.INSTANCE,f.origin),
                    List.of(new Interactions.ValueArgument(argument)),List.of(),
                    new Interactions.ExternalSignature(signature),List.of(),f.effects(),
                    normalOutcomes(f,"end"),new Interactions.KnownContract(reference));
            f.sequence("site-"+suffix,List.of(),invoke);
            if(i==1 || proveSecond)
                f.proof("transmit-"+suffix,new OperandDomain(argument.header().id()),
                        new ExternalParameterDomain(id,BigInteger.ZERO),new InvocationDomain(id));
        }
        f.sequence("end",List.of(),f.halt("halt"));
        return f;
    }

    private static Fixtures disjointFixture(DisjointCase variant) {
        Fixtures f=minimal();
        f.object("left",known(Types.Builtin.TEXT));
        f.object("right",known(Types.Builtin.TEXT));
        StorageId left=f.storage.get(0).header().id();
        StorageId right=f.storage.get(1).header().id();
        List<StorageId> storage=switch(variant) {
            case VALID -> List.of(left,right);
            case DANGLING -> List.of(left,new StorageId(f.pub,"missing"));
            case DUPLICATE -> List.of(left,left);
            case SINGLETON -> List.of(left);
        };
        f.premises.add(disjointPremise(f,storage));
        return f;
    }

    private static Proofs.Premise disjointPremise(Fixtures f,List<StorageId> storage) {
        return new Proofs.Premise(new PremiseId(f.pub,"disjoint"),"fixture authority",
                "bases are physically separate",f.origin,new Proofs.DisjointStorage(storage));
    }

    private static Fixtures extensionEquality() {
        Fixtures f=new Fixtures();
        Capabilities.Capability capability=new Capabilities.Capability("fixture.extension","7");
        f.capabilities.add(capability);
        Types.TypeRef type=known(new Types.ExtensionType(capability.name(),capability.version()));
        UncertaintyId leftReason=f.uncertainty("left-value","EXTERNAL_VALUE_UNKNOWN");
        UncertaintyId rightReason=f.uncertainty("right-value","EXTERNAL_VALUE_UNKNOWN");
        OperationId id=f.op("compare");
        Expression left=new Expressions.Unknown(f.operand(id,"left",Operand.Role.VALUE_READ),type,
                List.of(),Scopes.NoMemory.INSTANCE,leftReason);
        Expression right=new Expressions.Unknown(f.operand(id,"right",Operand.Role.VALUE_READ),type,
                List.of(),Scopes.NoMemory.INSTANCE,rightReason);
        Expression comparison=new Expressions.Binary(f.operand(id,"comparison",Operand.Role.VALUE_READ),
                Expressions.BinaryOperator.EQ,left,right);
        ObjectId result=f.object("result",known(Types.Builtin.BOOL));
        f.linear(f.assign("compare",result,comparison));
        return f;
    }

    private static Envelopes.Envelope envelope(Control.ControlEnvelope control,
                                                Envelopes.MemoryEnvelope memory,
                                                List<Envelopes.ResourceUse> resources) {
        return new Envelopes.Envelope(memory,control,
                new Envelopes.DependencyEnvelope(resources,Scopes.NoResources.INSTANCE));
    }

    private static Envelopes.MemoryEnvelope noMemoryEnvelope() {
        return new Envelopes.MemoryEnvelope(List.of(),Scopes.NoMemory.INSTANCE,List.of(),
                Scopes.NoMemory.INSTANCE,List.of());
    }

    private static Fixtures opaqueControlFixture(Control.ControlEnvelope control) {
        Fixtures f=new Fixtures();UncertaintyId gap=f.uncertainty("opaque","SOURCE_SEMANTICS_UNAVAILABLE");
        f.sequence("start",List.of(),new Operations.Opaque(f.header(f.op("opaque"),gap),
                "fixture.opaque",List.of(),List.of(),envelope(control,noMemoryEnvelope(),List.of())));
        f.sequence("end",List.of(),f.halt("halt"));
        return f;
    }

    private static Fixtures copyBytesContinueFixture() {
        Fixtures f=new Fixtures();f.capabilities.add(Capabilities.MEMORY_REGIONS);
        StorageId region=new StorageId(f.pub,"region");
        f.storage.add(new Memory.Region(new Memory.StorageHeader(region,Optional.of(f.unit),
                Memory.Lifetime.ACTIVATION,Memory.Visibility.PRIVATE,f.origin),
                Optional.of(BigInteger.valueOf(8)),Optional.empty()));
        OperationId id=f.op("copy");
        Memory.ByteRange destination=new Memory.ByteRange(region,
                f.integer(id,"destination-offset",BigInteger.ZERO.longValueExact(),Operand.Role.VALUE_READ),
                f.integer(id,"destination-extent",1,Operand.Role.VALUE_READ));
        Memory.ByteRange source=new Memory.ByteRange(region,
                f.integer(id,"source-offset",1,Operand.Role.VALUE_READ),
                f.integer(id,"source-extent",1,Operand.Role.VALUE_READ));
        Control.ControlEnvelope control=new Control.ControlEnvelope(
                List.of(Control.ContinueAlternative.INSTANCE),Scopes.NoControl.INSTANCE);
        Operations.CopyBytes copy=new Operations.CopyBytes(f.header(id),destination,source,
                BigInteger.ONE,envelope(control,noMemoryEnvelope(),List.of()));
        f.sequence("start",List.of(copy),f.halt("halt"));
        return f;
    }

    private static Fixtures effectOperandFixture() {
        Fixtures f=new Fixtures();ObjectId object=f.object("effect",known(Types.Builtin.TEXT));
        OperationId id=f.op("call");Place effect=f.place(id,"effect-only",object,Operand.Role.VALUE_WRITE);
        Interactions.ForeignEffects foreign=new Interactions.ForeignEffects(Scopes.NoMemory.INSTANCE,
                Scopes.NoMemory.INSTANCE,List.of(effect.header().id()));
        Interactions.EffectBound effects=new Interactions.EffectBound(foreign,List.of());
        Operations.Invoke invoke=new Operations.Invoke(f.header(id),"call",
                new Interactions.LiteralTarget("program","fixture","EFFECT",
                        Interactions.ExactName.INSTANCE,f.origin),List.of(),List.of(),
                new Interactions.ExternalSignature(f.signature(List.of(),List.of())),List.of(effect),
                effects,normalOutcomes(f,"end"),new Interactions.KnownContract(contract(f)));
        f.sequence("start",List.of(),invoke);f.sequence("end",List.of(),f.halt("halt"));
        return f;
    }

    private static Fixtures opaqueResultFixture() {
        Fixtures f=new Fixtures();ObjectId object=f.object("result",known(Types.Builtin.TEXT));
        UncertaintyId gap=f.uncertainty("opaque","SOURCE_SEMANTICS_UNAVAILABLE");
        OperationId id=f.op("opaque");Place result=f.place(id,"result",object,Operand.Role.RESULT_TARGET);
        Envelopes.MemoryEnvelope memory=new Envelopes.MemoryEnvelope(List.of(),Scopes.NoMemory.INSTANCE,
                List.of(result.header().id()),Scopes.NoMemory.INSTANCE,List.of());
        Control.ControlEnvelope control=new Control.ControlEnvelope(List.of(Control.Diverge.INSTANCE),
                Scopes.NoControl.INSTANCE);
        Operations.Opaque opaque=new Operations.Opaque(f.header(id,gap),"fixture.opaque",
                List.of(result),List.of(result.header().id()),envelope(control,memory,List.of()));
        f.sequence("start",List.of(),opaque);
        return f;
    }

    private static Publication multiEntryReturn(boolean compatible) {
        Fixtures f=new Fixtures();OperationId id=f.op("return");
        List<Expression> values=compatible ? List.of() : List.of(
                new Expressions.Literal(f.operand(id,"value",Operand.Role.VALUE_READ),
                        new Values.TextValue("value")));
        f.sequence("start",List.of(),new Operations.Return(f.header(id),values));
        Publication basePublication=f.build();Unit base=basePublication.units().get(0);
        Interactions.Signature first=compatible ? f.signature(List.of(),List.of())
                : f.signature(List.of(),List.of(new Interactions.ResultSlot(BigInteger.ZERO,
                        known(Types.Builtin.TEXT),f.origin)));
        Interactions.Signature second=compatible ? f.signature(List.of(),List.of())
                : f.signature(List.of(),List.of(new Interactions.ResultSlot(BigInteger.ZERO,
                        known(Types.Builtin.INT),f.origin)));
        Entries.Entry a=new Entries.Entry(new EntryId(f.unit,"entry-a"),Optional.of(f.label("start")),
                first,f.state,f.origin);
        Entries.Entry b=new Entries.Entry(new EntryId(f.unit,"entry-b"),Optional.of(f.label("start")),
                second,f.state,f.origin);
        return Fixtures.withUnits(basePublication,List.of(unitWith(base,List.of(a,b),
                base.sequences(),base.completionPorts())));
    }

    private static Fixtures openRemainderReturn(boolean proof) {
        Fixtures f=new Fixtures();
        Types.TypeRef sourceType=new Types.UnknownType(f.uncertainty("return-source-type","TYPE_UNKNOWN"));
        Types.TypeRef resultType=new Types.UnknownType(f.uncertainty("return-result-type","TYPE_UNKNOWN"));
        UncertaintyId arity=f.uncertainty("return-result-arity","SOURCE_SEMANTICS_UNAVAILABLE");
        ObjectId source=f.object("return-source",sourceType);
        OperationId id=f.op("return");
        Expression value=f.read(id,"return-value",source,Operand.Role.VALUE_READ);
        f.signature=new Interactions.Signature(
                new Interactions.ParameterInventory(List.of(),Interactions.NoRemainder.INSTANCE),
                new Interactions.ResultInventory(List.of(new Interactions.ResultSlot(
                        BigInteger.ZERO,resultType,f.origin)),new Interactions.UnknownRemainder(arity)),
                f.origin);
        f.sequence("start",List.of(),new Operations.Return(f.header(id),List.of(value)));
        if(proof) f.proof("return-domain",new OperandDomain(value.header().id()),
                new ResultDomain(f.entry(),BigInteger.ZERO),new OperationDomain(id));
        return f;
    }

    private static Unit unitWith(Unit base,List<Entries.Entry> entries,List<Sequence> sequences,
                                 List<Entries.CompletionPort> completionPorts) {
        return new Unit(base.id(),base.containingUnit(),base.objects(),base.visibleObjects(),entries,
                sequences,completionPorts,base.body(),base.bodyUnavailable(),base.coverage(),base.origin());
    }

    private static Fixtures artifactRelationFixture(boolean dangling) {
        Fixtures f=minimal();ArtifactId source=new ArtifactId(f.pub,"source");
        ArtifactId destination=new ArtifactId(f.pub,"destination");
        f.artifacts.add(new Origins.Artifact(source,"source",Optional.empty()));
        if(!dangling) f.artifacts.add(new Origins.Artifact(destination,"destination",Optional.empty()));
        f.artifactRelations.add(new Artifacts.Relation(new ArtifactRelationId(f.pub,"relation"),source,
                new Artifacts.InternalArtifact(destination),"includes",f.origin,
                Evidence.CoverageStatus.MODELED));
        return f;
    }

    private static Fixtures sameLocalIdsFixture() {
        Fixtures f=new Fixtures();ObjectId object=f.object("same",known(Types.Builtin.TEXT));
        ArtifactId artifact=new ArtifactId(f.pub,"same");
        f.artifacts.add(new Origins.Artifact(artifact,"same",Optional.empty()));
        f.resources.add(new Interactions.Resource(new ResourceId(f.pub,"same"),
                new Interactions.LiteralTarget("program","fixture","SAME",
                        Interactions.ExactName.INSTANCE,f.origin),f.origin));
        f.artifactRelations.add(new Artifacts.Relation(new ArtifactRelationId(f.pub,"same"),artifact,
                new Artifacts.InternalArtifact(artifact),"self",f.origin,Evidence.CoverageStatus.MODELED));
        f.premises.add(new Proofs.Premise(new PremiseId(f.pub,"same"),"fixture authority",
                "reflexive domain",f.origin,new Proofs.SameDomain(new ObjectDomain(object),
                new ObjectDomain(object),PublicationDomain.INSTANCE)));
        f.sequence("same",List.of(),f.halt("same"));
        return f;
    }

    private static Fixtures distinctOperandOwnersFixture() {
        Fixtures f=new Fixtures();ObjectId object=f.object("object",known(Types.Builtin.TEXT));
        Place initialPlace=new Places.ObjectPlace(f.entryOperand("same",Operand.Role.VALUE_WRITE),object);
        Expressions.Literal initialValue=new Expressions.Literal(
                f.entryOperand("entry-value",Operand.Role.VALUE_READ),new Values.TextValue("entry"));
        f.state=new Entries.EntryState(List.of(new Entries.InitialCondition(initialPlace,
                new Entries.LiteralInitial(initialValue),f.origin,List.of())),List.of());
        OperationId id=f.op("assign");
        Place operationPlace=new Places.ObjectPlace(f.operand(id,"same",Operand.Role.VALUE_WRITE),object);
        Expression operationValue=new Expressions.Literal(f.operand(id,"operation-value",
                Operand.Role.VALUE_READ),new Values.TextValue("operation"));
        f.linear(new Operations.Assign(f.header(id),operationPlace,operationValue));
        return f;
    }

    private static Publication unavailableBody(boolean withLabel) {
        Fixtures f=new Fixtures();
        UncertaintyId unavailable=f.uncertainty("body","INPUT_MISSING");
        Entries.Entry entry=new Entries.Entry(f.entry(),
                withLabel ? Optional.of(f.label("missing")) : Optional.empty(),
                f.signature,f.state,f.origin);
        Unit unit=new Unit(f.unit,Optional.empty(),List.of(),List.of(),List.of(entry),List.of(),List.of(),
                Unit.BodyAvailability.UNAVAILABLE,Optional.of(unavailable),
                f.coverage(new Scopes.UnitScope(f.unit)),f.origin);
        return new Publication(f.pub,SemanticVersion.AIR_2_0_0,
                new Capabilities.Manifest(List.of(),List.of()),List.of(),List.of(unit),List.of(),
                List.of(),List.of(),f.origins,f.coverage(new Scopes.PublicationScope(f.pub)),
                f.uncertainties,List.of());
    }

    private static Fixtures computedDependencyWrongPoint() {
        Fixtures f=new Fixtures();UncertaintyId gap=f.uncertainty("opaque","SOURCE_SEMANTICS_UNAVAILABLE");
        OperationId id=f.op("opaque");
        Expression name=new Expressions.Literal(f.operand(id,"resource",Operand.Role.RESOURCE_TARGET),
                new Values.TextValue("RESOURCE"));
        Interactions.ComputedResource target=new Interactions.ComputedResource("file","fixture",
                name.header().id(),Interactions.ExactName.INSTANCE,f.origin);
        Envelopes.ResourceUse use=new Envelopes.ResourceUse("read",target,
                new Control.After(id,Control.HaltOutcome.INSTANCE),f.origin);
        Control.ControlEnvelope control=new Control.ControlEnvelope(List.of(Control.Diverge.INSTANCE),
                Scopes.NoControl.INSTANCE);
        f.sequence("start",List.of(),new Operations.Opaque(f.header(id,gap),"fixture.opaque",
                List.of(name),List.of(),envelope(control,noMemoryEnvelope(),List.of(use))));
        return f;
    }

    private static void issue(ValidationResult result,ValidationIssue.Kind kind,String rule) {
        check(result.issues().stream().anyMatch(issue -> issue.kind()==kind && issue.rule().equals(rule)),
                "missing "+kind+" "+rule+" in "+result.issues());
    }

    private static void classAbsent(String name) {
        try {
            Class.forName(name);
            throw new AssertionError("legacy public class remains: "+name);
        } catch(ClassNotFoundException expected) {
            // Required absence.
        }
    }

    private static void recordOmits(Class<?> type,String component) {
        check(Arrays.stream(type.getRecordComponents()).noneMatch(item -> item.getName().equals(component)),
                type.getSimpleName()+" still exposes "+component);
    }

    private static void productionSourceOmits(List<String> tokens) {
        try(var files=Files.walk(Path.of("src/main/java"))) {
            for(Path file:files.filter(path->path.toString().endsWith(".java")).toList()) {
                String source=Files.readString(file,StandardCharsets.UTF_8);
                for(String token:tokens) check(!source.contains(token),
                        "legacy semantic token remains in "+file+": "+token);
            }
        } catch(java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private static Fixtures openChoiceCopy(boolean wholeProof,boolean contradiction) {
        Fixtures f=new Fixtures();Types.TypeRef unknown=new Types.UnknownType(f.uncertainty("type","TYPE_UNKNOWN"));
        ObjectId src=f.object("src",contradiction?known(Types.Builtin.INT):known(Types.Builtin.TEXT));
        ObjectId dst=f.object("dst",known(Types.Builtin.TEXT));OperationId a=f.op("a");
        Place candidate=f.place(a,"candidate",src,Operand.Role.VALUE_READ);
        Place choice=new Places.Choice(f.operand(a,"choice",Operand.Role.VALUE_READ),List.of(candidate),new Scopes.WithinMemory(new Scopes.VisibleMemory(f.unit,true)),unknown);
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
        Types.TypeRef type=f.objects.get(0).typeRef();
        Interactions.Parameter parameter=new Interactions.Parameter(BigInteger.ZERO,
                new Interactions.KnownMode(Interactions.PassingMode.VALUE),type,
                Interactions.ExternalBinding.INSTANCE,f.origin);
        Interactions.Signature signature=f.signature(List.of(parameter),List.of());
        Expression arg=f.read(f.op("k"),"argument",f.objects.get(0).id(),Operand.Role.ARGUMENT_VALUE);
        Interactions.ContractRef reference=new Interactions.ContractRef("fixture.authority","revision-1",List.of(f.origin));
        Operations.Invoke invoke=new Operations.Invoke(old.header(),old.action(),old.target(),
                List.of(new Interactions.ValueArgument(arg)),List.of(),
                new Interactions.ExternalSignature(signature),List.of(),old.effectBound(),old.outcomes(),
                new Interactions.KnownContract(reference));
        f.sequences.set(0,new Sequence(f.label("start"),List.of(),invoke,f.origin));
        if(proof) transmissionProof(f,new InvocationDomain(f.op("k")));
        return f;
    }
    private static void transmissionProof(Fixtures f,DomainProofScope scope) {
        f.proof("transmit",new OperandDomain(new OperandId(new OperationOwner(f.op("k")),"argument")),new ExternalParameterDomain(f.op("k"),BigInteger.ZERO),scope);
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
    private static Fixtures sliceFixture(int start,int count){Fixtures f=new Fixtures();ObjectId x=f.object("x",known(Types.Builtin.TEXT));OperationId a=f.op("a");Expression slice=new Expressions.SliceText(f.operand(a,"slice",Operand.Role.VALUE_READ),f.text(a,"text","A\uD83D\uDE00"),f.integer(a,"start",start,Operand.Role.VALUE_READ),f.integer(a,"count",count,Operand.Role.VALUE_READ));f.linear(f.assign("a",x,slice));return f;}
    private static Fixtures choiceFixture(boolean known,boolean open){Fixtures f=new Fixtures();ObjectId x=f.object("x",known(Types.Builtin.TEXT)),y=f.object("y",known?known(Types.Builtin.TEXT):known(Types.Builtin.INT));UncertaintyId u=f.uncertainty("type","TYPE_UNKNOWN"),gap=f.uncertainty("gap","SOURCE_SEMANTICS_UNAVAILABLE");OperationId o=f.op("o");List<Place> candidates=List.of(f.place(o,"x",x,Operand.Role.VALUE_READ),f.place(o,"y",y,Operand.Role.VALUE_READ));Types.TypeRef t=known?known(Types.Builtin.TEXT):new Types.UnknownType(u);if(known && !open) candidates=List.of(f.place(o,"x",x,Operand.Role.VALUE_READ),f.place(o,"y",f.object("z",known(Types.Builtin.INT)),Operand.Role.VALUE_READ));Place choice=new Places.Choice(f.operand(o,"choice",Operand.Role.VALUE_READ),candidates,open?new Scopes.WithinMemory(new Scopes.VisibleMemory(f.unit,true)):Scopes.NoMemory.INSTANCE,t);f.sequence("start",List.of(),new Operations.Opaque(f.header(o,gap),"test.choice",List.of(choice),List.of(),f.envelope(null)));return f;}
    private static Fixtures invokeFixture(boolean text){Fixtures f=new Fixtures();Types.TypeRef t=new Types.UnknownType(f.uncertainty("type","TYPE_UNKNOWN"));ObjectId x=f.object("x",t);UncertaintyId v=f.uncertainty("target","RESOURCE_TARGET_UNKNOWN"),contract=f.uncertainty("contract","CONTRACT_UNKNOWN");OperationId k=f.op("k");Expression name=new Expressions.Unknown(f.operand(k,"name",Operand.Role.CALL_TARGET),text?known(Types.Builtin.TEXT):t,List.of(f.read(k,"read",x,Operand.Role.VALUE_READ)),Scopes.NoMemory.INSTANCE,v);f.sequence("start",List.of(),new Operations.Invoke(f.header(k,v),"call",new Interactions.ComputedTarget("program","test.program",name,Interactions.ExactName.INSTANCE,f.origin),List.of(),List.of(),new Interactions.ExternalSignature(f.signature(List.of(),List.of())),List.of(),f.effects(),new Control.InvocationOutcomes(List.of(new Control.Normal(f.label("end")),new Control.AnyException(Control.Propagate.INSTANCE),Control.HaltAlternative.INSTANCE,Control.Diverge.INSTANCE),Scopes.NoControl.INSTANCE),new Interactions.UnknownContract(contract)));f.sequence("end",List.of(),f.halt("h"));return f;}
    private static void valid(Publication p){ValidationResult r=AirValidator.validate(p);check(r.isStructurallyValid(),r.issues().toString());}
    private static void invalid(Publication p,String rule){ValidationResult r=AirValidator.validate(p);check(r.status()==ValidationResult.Status.INVALID_IR,"expected INVALID_IR, got "+r);check(r.issues().stream().anyMatch(i->i.kind()==ValidationIssue.Kind.INVALID_IR&&i.rule().equals(rule)),"missing "+rule+" in "+r.issues());}
    private static void architecture(){try{Path root=Path.of("src/main/java");try(var paths=Files.walk(root)){for(Path p:paths.filter(x->x.toString().endsWith(".java")).toList()){String text=Files.readString(p,StandardCharsets.UTF_8);for(String banned:List.of("com.fasterxml.jackson","com.google.gson","java.nio.file","java.io.","org.antlr","cobolexplorer","org.springframework"))check(!text.contains(banned),"forbidden dependency in "+p+": "+banned);}}}catch(java.io.IOException e){throw new AssertionError(e);}}
    private static void test(String name,Runnable test){try{test.run();passed++;System.out.println("ok "+passed+" - "+name);}catch(Throwable e){System.err.println("FAIL - "+name);throw e;}}
    private static void eq(java.lang.Object expected,java.lang.Object actual){check(Objects.equals(expected,actual),"expected "+expected+", got "+actual);}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static void throwsType(Class<? extends Throwable> type,Runnable action){try{action.run();}catch(Throwable e){if(type.isInstance(e))return;throw new AssertionError("unexpected exception",e);}throw new AssertionError("expected "+type.getSimpleName());}
}
