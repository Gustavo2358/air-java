package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.AirShape;
import io.github.gustavo2358.air.model.AirSnapshot;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Objects;

/** Admission boundary laws while mandatory snapshot passes are assembled. */
final class SnapshotValidatorChecks {
    private SnapshotValidatorChecks() { }

    static void checkedSnapshotIsBoundAndIncompletePassesCannotCertify() {
        var source=new SnapshotGraphChecks.Source();var fixture=new SnapshotGraphChecks.Fixture(source);
        SnapshotValidator.CheckedSnapshot checked=SnapshotValidator.check(AirSnapshot.attach(source,fixture.root),
            ValidationOptions.defaults(),new Stores());
        eq(ValidationResult.Status.STRUCTURALLY_VALID,checked.result().status());
        eq(1,checked.result().statistics().entities());eq(0,checked.result().statistics().operands());
        eq(AirShape.PUBLICATION,checked.snapshot().shape(fixture.root));
        checked.close();fails(IllegalStateException.class,checked::snapshot);checked.close();
        var unproved=calculatedSlice();var expected=AirValidator.validate(unproved);
        eq(ValidationResult.Status.INCOMPLETE_VALIDATION,expected.status());
        try(var incomplete=SnapshotValidator.check(AirSnapshot.fromPublication(unproved),ValidationOptions.defaults(),new Stores())) {
            eq(expected,incomplete.result());
        }
    }

    static void duplicateAndLocalFailuresRemainInvalidWithoutFalseCompletion() {
        var source=new SnapshotGraphChecks.Source();var fixture=new SnapshotGraphChecks.Fixture(source);
        long artifactId=source.record(AirShape.IDS_ARTIFACT_ID,fixture.id,source.text("a"));
        long artifact=source.record(AirShape.ORIGINS_ARTIFACT,artifactId,source.text("a"),source.optional());
        source.replaceField(fixture.root,3,source.list(artifact,artifact));
        long first=source.record(AirShape.CAPABILITIES_CAPABILITY,source.text(" "),source.text("1"));
        long second=source.record(AirShape.CAPABILITIES_CAPABILITY,source.text("ok"),source.text(""));
        long manifest=source.child(fixture.root,2);source.replaceField(manifest,0,source.list(first,second));
        try(var checked=SnapshotValidator.check(AirSnapshot.attach(source,fixture.root),ValidationOptions.defaults(),new Stores())) {
            eq(ValidationResult.Status.INVALID_IR,checked.result().status());
            eq(3L,checked.result().diagnostics().count(ValidationIssue.Kind.INVALID_IR));
            eq(2L,checked.result().diagnostics().count(ValidationIssue.Kind.UNSUPPORTED_CAPABILITY));
            eq(5,checked.result().issues().size());
            eq("I-01",checked.result().issues().get(0).rule());
            eq("MODEL_CONSTRAINT",checked.result().issues().get(1).rule());
            eq("MODEL_CONSTRAINT",checked.result().issues().get(2).rule());
            eq("I-43",checked.result().issues().get(3).rule());
            eq("I-43",checked.result().issues().get(4).rule());
            eq(false,checked.result().diagnostics().traversalCompleted());
        }
    }

    static void contextualResourcesSignaturesAndTypesAreAdmittedTogether() {
        var f=new Fixtures();
        var wrong=f.uncertainty("wrong-type","VALUE_UNKNOWN");
        var object=f.object("item",new Types.UnknownType(wrong));
        var operation=f.op("nop");f.linear(new Operations.Nop(f.header(operation)));
        var storage=f.storage.get(0).header().id();
        f.premises.add(new Proofs.Premise(new PremiseId(f.pub,"disjoint"),"fixture","duplicate",f.origin,
            new Proofs.DisjointStorage(List.of(storage,storage))));
        f.capabilities.add(Capabilities.RESOURCE_BINDINGS);
        f.resources.add(new Interactions.Resource(new ResourceId(f.pub,"file"),new Interactions.LocalResource("file"),f.origin,
            Optional.of(new Interactions.ResourceDeclaration(f.unit,"FILE-A","not-qualified","bad source",
                List.of(new Interactions.ResourceObject(object,"record"),new Interactions.ResourceObject(object,"record")),
                List.of(new Interactions.ResourceUse(operation,"read",f.origin),new Interactions.ResourceUse(operation,"read",f.origin))))));
        f.signature=f.signature(List.of(new Interactions.Parameter(BigInteger.ZERO,
            new Interactions.KnownMode(Interactions.PassingMode.VALUE),Fixtures.known(Types.Builtin.TEXT),
            Interactions.ExternalBinding.INSTANCE,f.origin)),List.of());
        try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(f.build()),ValidationOptions.defaults(),new Stores())) {
            eq(ValidationResult.Status.INVALID_IR,checked.result().status());
            eq(7L,checked.result().diagnostics().count(ValidationIssue.Kind.INVALID_IR));
            eq(List.of("I-58","I-49","I-RB-03","I-RB-02","I-RB-02","I-49","I-55"),checked.result().issues().stream().map(ValidationIssue::rule).toList());
            eq(false,checked.result().diagnostics().traversalCompleted());
        }
    }

    static void operandTypesResolveBottomUpWithoutResidentOperands() {
        var f=new Fixtures();var text=f.object("text",Fixtures.known(Types.Builtin.TEXT));var integer=f.object("integer",Fixtures.known(Types.Builtin.INT));var op=f.op("a");
        var expression=new Expressions.Binary(f.operand(op,"sum",Operand.Role.VALUE_READ),Expressions.BinaryOperator.ADD,
            f.read(op,"left",text,Operand.Role.VALUE_READ),f.read(op,"right",integer,Operand.Role.VALUE_READ));
        f.linear(f.assign("a",text,expression));
        try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(f.build()),ValidationOptions.defaults(),new Stores())) {
            eq(ValidationResult.Status.INVALID_IR,checked.result().status());
            eq(2L,checked.result().diagnostics().count(ValidationIssue.Kind.INVALID_IR));
            eq(List.of("I-08","I-08"),checked.result().issues().stream().map(ValidationIssue::rule).toList());
        }
    }

    static void directVariableCallProfileCanIssueAValidCertificate() {
        var publication=directVariableCall(false);
        try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(publication),ValidationOptions.defaults(),new Stores())) {
            eq(ValidationResult.Status.STRUCTURALLY_VALID,checked.result().status());
            eq(true,checked.result().diagnostics().traversalCompleted());eq(List.of(),checked.result().issues());
        }
    }
    static void declaredNamePoliciesAreNotOtherExtensionSurfaces() {
        for(boolean declared:new boolean[]{true,false}) {
            var f=new Fixtures();var operation=f.op("call");var reason=f.uncertainty("contract","CONTRACT_UNKNOWN");
            if(declared)f.capabilities.add(new Capabilities.Capability("fixture.name-policy","1"));
            var call=new Operations.Invoke(f.header(operation),"call",
                new Interactions.LiteralTarget("program","fixture.program","X",new Interactions.ExtensionName("fixture.name-policy","1"),f.origin),
                List.of(),List.of(),new Interactions.ExternalSignature(f.signature(List.of(),List.of())),List.of(),f.effects(),
                new Control.InvocationOutcomes(List.of(Control.HaltAlternative.INSTANCE),Scopes.NoControl.INSTANCE),new Interactions.UnknownContract(reason));
            f.sequence("start",List.of(),call);
            var publication=f.build();
            eq(declared?ValidationResult.Status.STRUCTURALLY_VALID:ValidationResult.Status.INVALID_IR,AirValidator.validate(publication).status());
            try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(publication),ValidationOptions.defaults(),new Stores())) {
                eq(declared?0L:1L,checked.result().diagnostics().count(ValidationIssue.Kind.INVALID_IR));
                eq(0L,checked.result().diagnostics().count(ValidationIssue.Kind.UNSUPPORTED_CAPABILITY));
                if(!declared&&checked.result().issues().stream().noneMatch(i->i.rule().equals("I-43")))throw new AssertionError("missing declared-policy obligation");
            }
        }
    }
    static void targetPossibilitiesAdmitOnlyUnknownDomainsWithTheDeclaredCapability() {
        for(boolean declared:new boolean[]{false,true})for(boolean unknown:new boolean[]{false,true}) {
            var f=new Fixtures();var operation=f.op("call");var reason=f.uncertainty("type","TYPE_UNKNOWN");
            var valueReason=f.uncertainty("value","VALUE_UNKNOWN");
            var contract=f.uncertainty("contract","CONTRACT_UNKNOWN");
            var type=unknown?new Types.UnknownType(reason):new Types.Known(Types.Builtin.INT);
            if(declared)f.capabilities.add(Capabilities.TARGET_POSSIBILITIES);
            var name=new Expressions.Unknown(f.operand(operation,"name",Operand.Role.CALL_TARGET),type,List.of(),Scopes.NoMemory.INSTANCE,valueReason);
            var call=new Operations.Invoke(f.header(operation),"call",new Interactions.ComputedTarget("program","cobol.program",name,Interactions.ExactName.INSTANCE,f.origin),
                List.of(),List.of(),new Interactions.ExternalSignature(f.signature(List.of(),List.of())),List.of(),f.effects(),
                new Control.InvocationOutcomes(List.of(Control.HaltAlternative.INSTANCE),Scopes.NoControl.INSTANCE),new Interactions.UnknownContract(contract));
            f.sequence("start",List.of(),call);var publication=f.build();
            boolean admitted=declared&&unknown;
            eq(admitted?ValidationResult.Status.STRUCTURALLY_VALID:ValidationResult.Status.INVALID_IR,AirValidator.validate(publication).status());
            try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(publication),ValidationOptions.defaults(),new Stores())) {
                eq(admitted?0L:1L,checked.result().diagnostics().count(ValidationIssue.Kind.INVALID_IR));
                if(!admitted&&checked.result().issues().stream().noneMatch(i->i.rule().equals("I-08")))throw new AssertionError("target type must stay diagnosed");
            }
        }
    }

    static void multipleDefinitionsCannotBorrowTheDirectCertificate() {
        try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(directVariableCall(true)),ValidationOptions.defaults(),new Stores())) {
            // This no longer borrows the narrow certificate: the full rule engine discharges it.
            eq(ValidationResult.Status.STRUCTURALLY_VALID,checked.result().status());eq(true,checked.result().diagnostics().traversalCompleted());
        }
    }
    static void generalTypedAdmissionRunsEveryExistingMandatoryRuleWithoutOwningBodies() {
        for(var publication:List.of(directVariableCall(true),directVariableCall(false,"cics.program"))) {
            eq(ValidationResult.Status.STRUCTURALLY_VALID,AirValidator.validate(publication).status());
            try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(publication),ValidationOptions.defaults(),new Stores())) {
                eq(ValidationResult.Status.STRUCTURALLY_VALID,checked.result().status());
                eq(true,checked.result().diagnostics().traversalCompleted());
            }
        }
        for(boolean proof:new boolean[]{false,true}) {
            var f=new Fixtures();var unknown=new Types.UnknownType(f.uncertainty("type","TYPE_UNKNOWN"));
            var left=f.object("left",unknown);var right=f.object("right",unknown);
            f.linear(f.assign("copy",right,f.read(f.op("copy"),"value",left,Operand.Role.VALUE_READ)));
            if(proof)f.proof("domain",new Proofs.ObjectDomain(left),new Proofs.ObjectDomain(right),new Proofs.UnitDomain(f.unit));
            var publication=f.build();var expected=AirValidator.validate(publication);
            eq(proof?ValidationResult.Status.STRUCTURALLY_VALID:ValidationResult.Status.INVALID_IR,expected.status());
            try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(publication),ValidationOptions.defaults(),new Stores())) {
                eq(expected,checked.result());
                if(!proof&&checked.result().issues().stream().noneMatch(i->i.rule().equals("I-08/I-52")))throw new AssertionError("shared unknown_type must never prove sameDomain");
            }
        }
    }

    static void generalIndexesBorrowCanonicalIdentityRowsWithoutReadingTheirText() {
        var raw=new SnapshotGraphChecks.Source();var f=new SnapshotGraphChecks.Fixture(raw);
        long original=raw.record(AirShape.IDS_ORIGIN_ID,f.id,raw.text("original"));
        long[] origins=new long[9];origins[0]=raw.record(AirShape.ORIGINS_UNAVAILABLE,original,raw.text("fixture"));
        var expected=new ArrayList<OriginId>();expected.add(new OriginId(new PublicationId("P"),"original"));
        for(int n=1;n<origins.length;n++) {
            String local="𝄞x".repeat(1365)+n;expected.add(new OriginId(new PublicationId("P"),local));
            long id=raw.record(AirShape.IDS_ORIGIN_ID,f.id,raw.text(local));
            origins[n]=raw.record(AirShape.ORIGINS_DERIVED,id,raw.list(original),raw.text("fixture"));
        }
        raw.replaceField(f.root,8,raw.list(origins));long[] reads={0};
        var source=new AirSnapshot.Source() {
            public AirShape shape(long h){return raw.shape(h);}public long length(long h){return raw.length(h);}
            public long child(long h,long at){
                if(raw.shape(h)!=AirShape.LIST)return raw.child(h,at);
                try(var rows=raw.elements(h)){for(long n=0;n<=at;n++)if(!rows.advance())throw new IndexOutOfBoundsException();return rows.value();}
            }
            public long scalar(long h){return raw.scalar(h);}
            public int characters(long h,long at,char[] out,int start,int count){reads[0]+=count;return raw.characters(h,at,out,start,count);}
            public AirSnapshot.Elements elements(long h){return raw.elements(h);}public void close(){raw.close();}
        };
        try(var snapshot=AirSnapshot.attach(source,f.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());
            var declarations=SnapshotDeclarations.build(snapshot,keys,new SnapshotDeclarationChecks.Store(),Long.MAX_VALUE,Long.MAX_VALUE,(r,i,n)->{throw new AssertionError(r);})) {
            SnapshotNominalCycles.scan(snapshot,keys,declarations,new SnapshotCycleChecks.Store(),(r,i,n)->{throw new AssertionError(r);});
            reads[0]=0;var program=SnapshotValidationProgram.afterPrimitiveAdmission(snapshot,declarations);
            var index=new PublicationIndex(program,new ValidationContext(program,ValidationOptions.defaults()));index.build();
            eq(0L,reads[0]); // Catalog construction/cardinality must not decode full identity strings.
            eq(10,index.identities.size());eq(9,index.origins.size());eq(0L,reads[0]);
            for(var id:expected)eq(true,index.identities.contains(id));eq(0L,reads[0]);
            eq(false,index.identities.contains(new OriginId(new PublicationId("Q"),expected.getLast().localId())));
            eq(false,index.identities.contains(new ArtifactId(new PublicationId("P"),expected.getLast().localId())));
            eq(false,index.origins.containsKey(new OriginId(new PublicationId("P"),"missing")));
            eq(null,index.origins.get(new OriginId(new PublicationId("P"),"missing")));
            eq(expected,new ArrayList<>(index.origins.keySet()));
            eq(expected,index.origins.values().stream().map(Origins.Origin::id).toList());
            eq(expected.getLast(),index.origins.get(expected.getLast()).id());
            fails(UnsupportedOperationException.class,()->index.origins.keySet().clear());
            fails(UnsupportedOperationException.class,()->index.origins.entrySet().iterator().next().setValue(index.origins.get(expected.getFirst())));
            closeSnapshot(snapshot);fails(IllegalStateException.class,()->index.identities.contains(expected.getFirst()));
            fails(IllegalStateException.class,()->index.origins.get(expected.getFirst()));
        }
        // The general handoff may reuse primitive cycle checks only after they actually ran.
        for(String rule:List.of("I-36","I-12","I-01")) {
            var fixture=new Fixtures();fixture.sequence("start",List.of(),fixture.halt("stop"));
            if(rule.equals("I-36")) {
                var a=new OriginId(fixture.pub,"a");var b=new OriginId(fixture.pub,"b");
                fixture.origins.add(new Origins.Derived(a,List.of(b),"fixture"));
                fixture.origins.add(new Origins.Derived(b,List.of(a),"fixture"));
            } else if(rule.equals("I-12")) {
                fixture.alias("a",new ObjectId(fixture.unit,"b"),Fixtures.known(Types.Builtin.TEXT));
                fixture.alias("b",new ObjectId(fixture.unit,"a"),Fixtures.known(Types.Builtin.TEXT));
            }
            var publication=fixture.build();
            if(rule.equals("I-01")) {
                var unit=publication.units().getFirst();
                var changed=new Unit(unit.id(),Optional.of(unit.id()),unit.objects(),unit.visibleObjects(),unit.entries(),unit.sequences(),unit.completionPorts(),unit.body(),unit.bodyUnavailable(),unit.coverage(),unit.origin());
                publication=new Publication(publication.id(),publication.airVersion(),publication.capabilities(),publication.artifacts(),List.of(changed),publication.storage(),publication.resources(),publication.artifactRelations(),publication.origins(),publication.coverage(),publication.uncertainties(),publication.premises());
            }
            eq(ValidationResult.Status.INVALID_IR,AirValidator.validate(publication).status());
            try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(publication),ValidationOptions.defaults(),new Stores())) {
                eq(ValidationResult.Status.INVALID_IR,checked.result().status());
                if(checked.result().issues().stream().noneMatch(issue->issue.rule().equals(rule)))throw new AssertionError("missing primitive cycle "+rule);
                eq(false,checked.result().diagnostics().traversalCompleted());
            }
        }
    }

    private static void closeSnapshot(AirSnapshot snapshot){snapshot.close();}

    static void invocationOutcomeContradictionsCannotBorrowACompleteCertificate() {
        var base=directVariableCall(false);var call=(Operations.Invoke)base.units().getFirst().sequences().getFirst().terminator();
        for(int size:new int[]{1,4,16,64,256}) {
            var known=new ArrayList<Control.InvocationAlternative>();known.add(new Control.Normal(new LabelId(call.header().id().unit(),"end")));
            for(int i=0;i<size;i++)known.add(new Control.Exceptional(i==0?"Aa":i==1?"BB":"tag-"+i,Control.Propagate.INSTANCE));
            var unique=new Control.InvocationOutcomes(known,Scopes.NoControl.INSTANCE);
            var valid=withCall(base,call,call.arguments(),call.results(),call.signature(),unique);
            eq(ValidationResult.Status.STRUCTURALLY_VALID,AirValidator.validate(valid).status());
            try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(valid),ValidationOptions.defaults(),new Stores())) {
                eq(ValidationResult.Status.STRUCTURALLY_VALID,checked.result().status());
            }
            for(var repeated:List.of(known.getFirst(),known.getLast(),new Control.AnyException(Control.Propagate.INSTANCE))) {
                var bad=new ArrayList<>(known);if(repeated instanceof Control.AnyException)bad.add(repeated);bad.add(repeated);
                var changed=withCall(base,call,call.arguments(),call.results(),call.signature(),new Control.InvocationOutcomes(bad,Scopes.NoControl.INSTANCE));
                // The independent obligation is I-60 at the full invocation identity, not the label's spelling.
                eq(ValidationResult.Status.INVALID_IR,AirValidator.validate(changed).status());
                try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(changed),ValidationOptions.defaults(),new Stores())) {
                    eq(ValidationResult.Status.INVALID_IR,checked.result().status());
                    if(checked.result().issues().stream().noneMatch(issue->issue.rule().equals("I-60")&&issue.subject().filter(call.header().id()::equals).isPresent()))
                        throw new AssertionError("missing invocation I-60: "+checked.result());
                }
            }
        }
    }

    static void invocationSignatureCardinalityAndRolesCannotBorrowACompleteCertificate() {
        var base=directVariableCall(false);var call=(Operations.Invoke)base.units().getFirst().sequences().getFirst().terminator();
        var header=new Operand.Header(new OperandId(new OperationOwner(call.header().id()),"argument"),Operand.Role.ARGUMENT_VALUE,call.header().origin());
        var argument=new Interactions.ValueArgument(new Expressions.Literal(header,new Values.TextValue("ARG")));
        var parameter=new Interactions.Parameter(BigInteger.ZERO,new Interactions.KnownMode(Interactions.PassingMode.VALUE),new Types.Known(Types.Builtin.TEXT),Interactions.ExternalBinding.INSTANCE,call.header().origin());
        var signature=new Interactions.ExternalSignature(new Interactions.Signature(new Interactions.ParameterInventory(List.of(parameter),Interactions.NoRemainder.INSTANCE),new Interactions.ResultInventory(List.of(),Interactions.NoRemainder.INSTANCE),call.header().origin()));
        var wrongRole=new Interactions.ValueArgument(new Expressions.Literal(new Operand.Header(header.id(),Operand.Role.VALUE_READ,header.origin()),new Values.TextValue("ARG")));
        var valid=withCall(base,call,List.of(argument),call.results(),signature,call.outcomes());
        eq(ValidationResult.Status.STRUCTURALLY_VALID,AirValidator.validate(valid).status());
        try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(valid),ValidationOptions.defaults(),new Stores())) {
            // General rules prove this known transmission; contradictions below still fail.
            eq(ValidationResult.Status.STRUCTURALLY_VALID,checked.result().status());
            eq(0L,checked.result().diagnostics().count(ValidationIssue.Kind.INVALID_IR));
        }
        for(int mutation=0;mutation<4;mutation++) {
            var arguments=mutation==0?List.<Interactions.Argument>of(argument):mutation==1?List.<Interactions.Argument>of():mutation==2?List.<Interactions.Argument>of(new Interactions.CopyArgument(argument.value())):List.<Interactions.Argument>of(wrongRole);
            var selected=mutation==0?call.signature():signature;
            var publication=withCall(base,call,arguments,call.results(),selected,call.outcomes());
            eq(ValidationResult.Status.INVALID_IR,AirValidator.validate(publication).status());
            try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(publication),ValidationOptions.defaults(),new Stores())) {
                eq(ValidationResult.Status.INVALID_IR,checked.result().status());
                String rule=mutation==3?"I-11":"I-08";
                if(checked.result().issues().stream().noneMatch(issue->issue.rule().equals(rule)))throw new AssertionError("missing "+rule+": "+checked.result());
            }
        }
        var object=base.units().getFirst().objects().getFirst().id();
        var destination=new Places.ObjectPlace(new Operand.Header(new OperandId(new OperationOwner(call.header().id()),"result"),Operand.Role.RESULT_TARGET,call.header().origin()),object);
        var slot=new Interactions.ResultSlot(BigInteger.ZERO,new Types.Known(Types.Builtin.TEXT),call.header().origin());
        var resultSignature=new Interactions.ExternalSignature(new Interactions.Signature(new Interactions.ParameterInventory(List.of(),Interactions.NoRemainder.INSTANCE),new Interactions.ResultInventory(List.of(slot),Interactions.NoRemainder.INSTANCE),call.header().origin()));
        for(int mutation=0;mutation<5;mutation++) {
            var results=mutation==1?List.<Place>of():List.<Place>of(mutation==2?new Places.ObjectPlace(new Operand.Header(destination.header().id(),Operand.Role.VALUE_WRITE,destination.header().origin()),object):destination);
            var selected=mutation==0?call.signature():mutation==3?new Interactions.ExternalSignature(new Interactions.Signature(new Interactions.ParameterInventory(List.of(),Interactions.NoRemainder.INSTANCE),new Interactions.ResultInventory(List.of(new Interactions.ResultSlot(BigInteger.ZERO,new Types.Known(Types.Builtin.INT),call.header().origin())),Interactions.NoRemainder.INSTANCE),call.header().origin())):resultSignature;
            var outcomes=mutation==4?new Control.InvocationOutcomes(List.of(Control.HaltAlternative.INSTANCE),Scopes.NoControl.INSTANCE):call.outcomes();
            var publication=withCall(base,call,call.arguments(),results,selected,outcomes);
            eq(ValidationResult.Status.INVALID_IR,AirValidator.validate(publication).status());
            try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(publication),ValidationOptions.defaults(),new Stores())) {
                eq(ValidationResult.Status.INVALID_IR,checked.result().status());
                String rule=mutation==2?"I-11":"I-08";
                if(checked.result().issues().stream().noneMatch(issue->issue.rule().equals(rule)))throw new AssertionError("missing result "+rule+": "+checked.result());
            }
        }
        var huge=new Interactions.Parameter(BigInteger.TEN.pow(4096),parameter.mode(),parameter.typeRef(),parameter.objectBinding(),parameter.origin());
        var open=new Interactions.ExternalSignature(new Interactions.Signature(new Interactions.ParameterInventory(List.of(huge),new Interactions.UnknownRemainder(base.uncertainties().getFirst().id())),new Interactions.ResultInventory(List.of(),Interactions.NoRemainder.INSTANCE),call.header().origin()));
        var unboundedPosition=withCall(base,call,List.of(argument),List.of(),open,call.outcomes());
        eq(ValidationResult.Status.INVALID_IR,AirValidator.validate(unboundedPosition).status());
        try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(unboundedPosition),ValidationOptions.defaults(),new Stores())) {
            eq(ValidationResult.Status.INVALID_IR,checked.result().status());
            eq(1L,checked.result().diagnostics().count(ValidationIssue.Kind.INVALID_IR));
        }
    }

    static void openUnmaterializedSignaturePreservesIndependentTypedArgumentsAndResults() {
        var base=directVariableCall(false);var call=(Operations.Invoke)base.units().getFirst().sequences().getFirst().terminator();
        var origin=call.header().origin();var owner=new OperationOwner(call.header().id());
        var object=base.units().getFirst().objects().getFirst().id();var reason=base.uncertainties().getFirst().id();
        var signature=new Interactions.ExternalSignature(new Interactions.Signature(
            new Interactions.ParameterInventory(List.of(),new Interactions.UnknownRemainder(reason)),
            new Interactions.ResultInventory(List.of(),new Interactions.UnknownRemainder(reason)),origin));
        for(int count:new int[]{0,1,4,16,64}) {
            var arguments=new ArrayList<Interactions.Argument>();var results=new ArrayList<Place>();
            for(int i=0;i<count;i++) {
                arguments.add(new Interactions.ReferenceArgument(new Places.ObjectPlace(new Operand.Header(
                    new OperandId(owner,"reference-"+i),Operand.Role.ARGUMENT_REFERENCE,origin),object)));
                arguments.add(new Interactions.ValueArgument(new Expressions.Unknown(new Operand.Header(
                    new OperandId(owner,"unknown-"+i),Operand.Role.ARGUMENT_VALUE,origin),
                    new Types.Known(Types.Builtin.TEXT),List.of(),Scopes.NoMemory.INSTANCE,reason)));
                arguments.add(new Interactions.CopyArgument(new Expressions.Literal(new Operand.Header(
                    new OperandId(owner,"copy-"+i),Operand.Role.ARGUMENT_VALUE,origin),new Values.TextValue("ARG"))));
                arguments.add(new Interactions.ValueArgument(new Expressions.Read(new Operand.Header(
                    new OperandId(owner,"read-"+i),Operand.Role.ARGUMENT_VALUE,origin),new Places.ObjectPlace(new Operand.Header(
                    new OperandId(owner,"read-place-"+i),Operand.Role.VALUE_READ,origin),object))));
                results.add(new Places.ObjectPlace(new Operand.Header(new OperandId(owner,"result-"+i),Operand.Role.RESULT_TARGET,origin),object));
            }
            var publication=withCall(base,call,arguments,results,signature,call.outcomes());
            // AIR-04 invocation transmission applies to materialized positions only. Open,
            // empty inventories do not invent slot domains or erase BEFORE-target evidence.
            eq(ValidationResult.Status.STRUCTURALLY_VALID,AirValidator.validate(publication).status());
            try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(publication),ValidationOptions.defaults(),new Stores())) {
                eq(ValidationResult.Status.STRUCTURALLY_VALID,checked.result().status());
                eq(true,checked.result().diagnostics().traversalCompleted());
            }
            if(count>0) {
                var contradiction=withCall(base,call,arguments,results,call.signature(),call.outcomes());
                eq(ValidationResult.Status.INVALID_IR,AirValidator.validate(contradiction).status());
                try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(contradiction),ValidationOptions.defaults(),new Stores())) {
                    eq(ValidationResult.Status.INVALID_IR,checked.result().status());
                    if(checked.result().issues().stream().noneMatch(issue->issue.rule().equals("I-08")))
                        throw new AssertionError("closed signature contradiction lost: "+checked.result());
                }
            }
        }
        var compound=new Expressions.Binary(new Operand.Header(new OperandId(owner,"compound"),Operand.Role.ARGUMENT_VALUE,origin),
            Expressions.BinaryOperator.CONCAT,
            new Expressions.Literal(new Operand.Header(new OperandId(owner,"left"),Operand.Role.VALUE_READ,origin),new Values.TextValue("A")),
            new Expressions.Literal(new Operand.Header(new OperandId(owner,"right"),Operand.Role.VALUE_READ,origin),new Values.TextValue("B")));
        var outside=withCall(base,call,List.of(new Interactions.ValueArgument(compound)),List.of(),signature,call.outcomes());
        eq(ValidationResult.Status.STRUCTURALLY_VALID,AirValidator.validate(outside).status());
        try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(outside),ValidationOptions.defaults(),new Stores())) {
            eq(ValidationResult.Status.STRUCTURALLY_VALID,checked.result().status());
        }
    }
    private static Publication calculatedSlice() {
        var f=new Fixtures();var target=f.object("target",Fixtures.known(Types.Builtin.TEXT));var index=f.object("index",Fixtures.known(Types.Builtin.INT));
        var operation=f.op("slice");
        var value=new Expressions.SliceText(f.operand(operation,"value",Operand.Role.VALUE_READ),f.text(operation,"text","ABC"),
            f.read(operation,"index",index,Operand.Role.VALUE_READ),f.integer(operation,"count",1,Operand.Role.VALUE_READ));
        f.linear(f.assign("slice",target,value));return f.build();
    }

    private static Publication withCall(Publication base,Operations.Invoke previous,List<Interactions.Argument> arguments,List<Place> results,Interactions.InvocationSignature signature,Control.InvocationOutcomes outcomes) {
        var call=new Operations.Invoke(previous.header(),previous.action(),previous.target(),arguments,results,signature,previous.effectOperands(),previous.effectBound(),outcomes,previous.contract());
        var unit=base.units().getFirst();var sequences=new ArrayList<Sequence>();
        for(var sequence:unit.sequences())sequences.add(sequence.terminator()==previous?new Sequence(sequence.label(),sequence.instructions(),call,sequence.origin()):sequence);
        var changed=new Unit(unit.id(),unit.containingUnit(),unit.objects(),unit.visibleObjects(),unit.entries(),sequences,unit.completionPorts(),unit.body(),unit.bodyUnavailable(),unit.coverage(),unit.origin());
        return Fixtures.withUnits(base,List.of(changed));
    }

    static void sharedLabelEntriesHaveIndependentCompleteAdmission() {
        var base=directVariableCall(false);var first=base.units().getFirst().entries().getFirst();
        for(int count:new int[]{1,4,16,64}) {
            var entries=new ArrayList<Entries.Entry>();
            for(int i=0;i<count;i++)entries.add(new Entries.Entry(new EntryId(first.id().unit(),"entry-"+i),first.initialLabel(),first.signature(),first.state(),first.origin()));
            for(var order:List.of(entries,entries.reversed())) {
                var publication=withEntries(base,order);
                eq(ValidationResult.Status.STRUCTURALLY_VALID,AirValidator.validate(publication).status());
                try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(publication),ValidationOptions.defaults(),new Stores())) {
                    eq(ValidationResult.Status.STRUCTURALLY_VALID,checked.result().status());
                    eq(true,checked.result().diagnostics().traversalCompleted());eq(List.of(),checked.result().issues());
                }
            }
        }
    }

    static void sharedLabelAdmissionChecksEveryLaterEntryAndKeepsOtherSeedsIncomplete() {
        var base=directVariableCall(false);var first=base.units().getFirst().entries().getFirst();
        var next=new EntryId(first.id().unit(),"second");
        var distinct=new Entries.Entry(next,Optional.of(new LabelId(first.id().unit(),"end")),first.signature(),first.state(),first.origin());
        var state=new Entries.Entry(next,first.initialLabel(),first.signature(),new Entries.EntryState(List.of(),List.of(base.uncertainties().getFirst().id())),first.origin());
        var parameter=new Interactions.Parameter(BigInteger.ZERO,new Interactions.KnownMode(Interactions.PassingMode.VALUE),new Types.Known(Types.Builtin.TEXT),Interactions.ExternalBinding.INSTANCE,first.origin());
        var signature=new Interactions.Signature(new Interactions.ParameterInventory(List.of(parameter),Interactions.NoRemainder.INSTANCE),first.signature().results(),first.origin());
        var badSignature=new Entries.Entry(next,first.initialLabel(),signature,first.state(),first.origin());
        var bound=new Interactions.Parameter(BigInteger.ZERO,parameter.mode(),parameter.typeRef(),new Interactions.ObjectBinding(base.units().getFirst().objects().getFirst().id()),first.origin());
        var boundSignature=new Interactions.Signature(new Interactions.ParameterInventory(List.of(bound),Interactions.NoRemainder.INSTANCE),first.signature().results(),first.origin());
        var validSignature=new Entries.Entry(next,first.initialLabel(),boundSignature,first.state(),first.origin());
        var missing=new Entries.Entry(next,Optional.of(new LabelId(first.id().unit(),"missing-label")),first.signature(),first.state(),first.origin());
        var foreign=new UnitId(new PublicationId("foreign"),first.id().unit().localId());
        var foreignEntry=new Entries.Entry(new EntryId(foreign,"second"),first.initialLabel(),first.signature(),first.state(),first.origin());
        for(var later:List.of(distinct,state,validSignature,first,badSignature,foreignEntry,missing)) {
            var publication=withEntries(base,List.of(first,later));
            try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(publication),ValidationOptions.defaults(),new Stores())) {
                if(later==distinct||later==state||later==validSignature) {
                    eq(ValidationResult.Status.STRUCTURALLY_VALID,AirValidator.validate(publication).status());
                    eq(ValidationResult.Status.STRUCTURALLY_VALID,checked.result().status());eq(true,checked.result().diagnostics().traversalCompleted());
                } else {
                    eq(ValidationResult.Status.INVALID_IR,checked.result().status());
                    String rule=later==badSignature?"I-55":later==missing?"I-02":"I-01";
                    if(checked.result().issues().stream().noneMatch(issue->issue.rule().equals(rule)))throw new AssertionError("missing rule "+rule+": "+checked.result());
                }
            }
        }
    }

    private static Publication withEntries(Publication base,List<Entries.Entry> entries) {
        var unit=base.units().getFirst();
        var changed=new Unit(unit.id(),unit.containingUnit(),unit.objects(),unit.visibleObjects(),entries,unit.sequences(),unit.completionPorts(),unit.body(),unit.bodyUnavailable(),unit.coverage(),unit.origin());
        return new Publication(base.id(),base.airVersion(),base.capabilities(),base.artifacts(),List.of(changed),base.storage(),base.resources(),base.artifactRelations(),base.origins(),base.coverage(),base.uncertainties(),base.premises());
    }

    static void cicsNamesCannotBorrowTheCobolDependencyCertificate() {
        try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(directVariableCall(false,"cics.program")),ValidationOptions.defaults(),new Stores())) {
            // Structural validation does not interpret the external namespace or borrow COBOL policy.
            eq(ValidationResult.Status.STRUCTURALLY_VALID,checked.result().status());eq(true,checked.result().diagnostics().traversalCompleted());
        }
    }

    static void correlatedConcatDiamondCanIssueAValidCertificate() {
        try(var checked=SnapshotValidator.check(AirSnapshot.fromPublication(correlatedConcatDiamond()),ValidationOptions.defaults(),new Stores())) {
            eq(ValidationResult.Status.STRUCTURALLY_VALID,checked.result().status());
            eq(true,checked.result().diagnostics().traversalCompleted());eq(List.of(),checked.result().issues());
        }
    }

    private static Publication correlatedConcatDiamond() {
        var f=new Fixtures();var x=f.object("x",Fixtures.known(Types.Builtin.TEXT));var y=f.object("y",Fixtures.known(Types.Builtin.TEXT));var z=f.object("z",Fixtures.known(Types.Builtin.TEXT));var conditionObject=f.object("condition",Fixtures.known(Types.Builtin.BOOL));
        var reason=f.uncertainty("branch-input","VALUE_UNKNOWN");var initialPlace=new Places.ObjectPlace(f.entryOperand("condition",Operand.Role.VALUE_WRITE),conditionObject);
        f.state=new Entries.EntryState(List.of(new Entries.InitialCondition(initialPlace,new Entries.ExternalUnknown(reason),f.origin,List.of())),List.of(reason));
        var choose=f.op("choose");var condition=f.read(choose,"condition",conditionObject,Operand.Role.PREDICATE);
        f.sequence("start",List.of(),new Operations.Branch(f.header(choose),condition,f.label("left"),f.label("right")));
        f.sequence("left",List.of(f.assign("seed-A",x,f.text(f.op("seed-A"),"value","A")),f.assign("seed-X",y,f.text(f.op("seed-X"),"value","X"))),new Operations.Jump(f.header(f.op("left-jump")),f.label("join")));
        f.sequence("right",List.of(f.assign("seed-B",x,f.text(f.op("seed-B"),"value","B")),f.assign("seed-Y",y,f.text(f.op("seed-Y"),"value","Y"))),new Operations.Jump(f.header(f.op("right-jump")),f.label("join")));
        var fit=f.op("fit-concat");var left=f.read(fit,"x",x,Operand.Role.VALUE_READ);var right=f.read(fit,"y",y,Operand.Role.VALUE_READ);
        var concat=new Expressions.Binary(f.operand(fit,"concat",Operand.Role.VALUE_READ),Expressions.BinaryOperator.CONCAT,left,right);
        var value=new Expressions.FitText(f.operand(fit,"fit",Operand.Role.VALUE_READ),concat,BigInteger.valueOf(2)," ");
        var assign=f.assign("fit-concat",z,value);var call=f.op("call");var contract=f.uncertainty("contract","CONTRACT_UNKNOWN");
        var invoke=new Operations.Invoke(f.header(call),"call",new Interactions.ComputedTarget("program","cobol.program",f.read(call,"name",z,Operand.Role.CALL_TARGET),Interactions.ExactName.INSTANCE,f.origin),
            List.of(),List.of(),new Interactions.ExternalSignature(f.signature(List.of(),List.of())),List.of(),f.effects(),
            new Control.InvocationOutcomes(List.of(new Control.Normal(f.label("end")),new Control.AnyException(Control.Propagate.INSTANCE),Control.HaltAlternative.INSTANCE,Control.Diverge.INSTANCE),Scopes.NoControl.INSTANCE),new Interactions.UnknownContract(contract));
        f.sequence("join",List.of(assign),invoke);f.sequence("end",List.of(),f.halt("halt"));return f.build();
    }

    private static Publication directVariableCall(boolean duplicateDefinition) {return directVariableCall(duplicateDefinition,"cobol.program");}
    private static Publication directVariableCall(boolean duplicateDefinition,String namespace) {
        var f=new Fixtures();var target=f.object("target",Fixtures.known(Types.Builtin.TEXT));
        var set=f.op("set");var assign=new Operations.Assign(f.header(set),f.place(set,"destination",target,Operand.Role.VALUE_WRITE),f.text(set,"value","PROGA"));
        var call=f.op("call");var contract=f.uncertainty("contract","CONTRACT_UNKNOWN");
        var invoke=new Operations.Invoke(f.header(call),"call",
            new Interactions.ComputedTarget("program",namespace,f.read(call,"name",target,Operand.Role.CALL_TARGET),Interactions.ExactName.INSTANCE,f.origin),
            List.of(),List.of(),new Interactions.ExternalSignature(f.signature(List.of(),List.of())),List.of(),f.effects(),
            new Control.InvocationOutcomes(List.of(new Control.Normal(f.label("end")),new Control.AnyException(Control.Propagate.INSTANCE),Control.HaltAlternative.INSTANCE,Control.Diverge.INSTANCE),Scopes.NoControl.INSTANCE),
            new Interactions.UnknownContract(contract));
        var instructions=new java.util.ArrayList<Instruction>();instructions.add(assign);
        if(duplicateDefinition){var second=f.op("set-again");instructions.add(new Operations.Assign(f.header(second),f.place(second,"destination",target,Operand.Role.VALUE_WRITE),f.text(second,"value","PROGB")));}
        f.sequence("start",instructions,invoke);f.sequence("end",List.of(),f.halt("halt"));return f.build();
    }

    static final class Stores implements SnapshotValidator.Storage {
        public SnapshotIdentityKeys.Storage identities(){return new SnapshotAtomChecks.Store();}
        public SnapshotDeclarations.Storage declarations(){return new SnapshotDeclarationChecks.Store();}
        public SnapshotLocalConstraints.Storage localConstraints(){return new SnapshotLocalChecks.Store();}
        public SnapshotGraphWalk.Storage graphWalk(){return new SnapshotGraphChecks.Store();}
        public SnapshotNominalCycles.Storage nominalCycles(){return new SnapshotCycleChecks.Store();}
        public SnapshotGrounding.Storage grounding(){return new SnapshotGroundingChecks.Store();}
        public SnapshotVisibleObjects.Storage visibleObjects(){return new SnapshotVisibleChecks.Store();}
        public SnapshotSignatureIndex.Storage signatures(){return new SnapshotSignatureChecks.Store();}
        public SnapshotDiagnosticTemplates.Storage diagnostics(){return new SnapshotDiagnosticChecks.Store();}
        public SnapshotReferenceLists.Storage references(){return new SnapshotReferenceChecks.Store();}
        public SnapshotLocalLabels.Storage localLabels(){return new SnapshotLabelChecks.Store();}
        public SnapshotAnnotationTemplates.Storage annotations(){return new SnapshotAnnotationChecks.Store();}
        public SnapshotCapabilities.Storage capabilities(){return new SnapshotCapabilityChecks.Store();}
        public SnapshotDistinctTuples.Storage distinctTuples(){return new DistinctStore();}
        public SnapshotContextWalk.Storage contextWalk(){return new ContextStore();}
        public SnapshotTypes.Storage types(){return new TypeStore();}
    }
    private record Tuple(long list,long family,long first,long second){ }
    static final class DistinctStore implements SnapshotDistinctTuples.Storage {
        private final java.util.Set<Tuple> values=new java.util.HashSet<>();long claimed;
        public boolean first(long list,long family,long first,long second){return values.add(new Tuple(list,family,first,second));}
        public AirSnapshotBuilder.Lease claim(long bytes){claimed+=bytes;return ()->claimed-=bytes;}
        public void close(){values.clear();}
    }
    private record Work(long node,long owner,long kind,long context,long depth){ }
    static final class ContextStore implements SnapshotContextWalk.Storage {
        private final java.util.Set<Work> seen=new java.util.HashSet<>();private final java.util.ArrayDeque<Work> queue=new java.util.ArrayDeque<>();private Work current;long claimed;
        public boolean schedule(long node,long owner,long kind,long context,long depth){var work=new Work(node,owner,kind,context,depth);if(!seen.add(work))return false;queue.add(work);return true;}
        public boolean advance(){current=queue.poll();return current!=null;}public long node(){return current.node();}public long owner(){return current.owner();}public long kind(){return current.kind();}public long context(){return current.context();}public long depth(){return current.depth();}
        public AirSnapshotBuilder.Lease claim(long bytes){claimed+=bytes;return ()->claimed-=bytes;}public void close(){seen.clear();queue.clear();current=null;}
    }
    static final class TypeStore implements SnapshotTypes.Storage {
        private final java.util.Map<Long,long[]> types=new java.util.HashMap<>();private final java.util.Set<Long> lists=new java.util.HashSet<>();private final java.util.Set<Tuple> labels=new java.util.HashSet<>();long claimed;
        public void put(long key,long kind,long value){types.put(key,new long[]{kind,value});}public long kind(long key){return types.getOrDefault(key,new long[2])[0];}public long value(long key){return types.getOrDefault(key,new long[2])[1];}
        public boolean beginLabels(long list){return lists.add(list);}public void addLabel(long list,long key){labels.add(new Tuple(list,1,key,0));}public boolean containsLabel(long list,long key){return labels.contains(new Tuple(list,1,key,0));}
        public AirSnapshotBuilder.Lease claim(long bytes){claimed+=bytes;return ()->claimed-=bytes;}public void close(){types.clear();lists.clear();labels.clear();}
    }
    private static <T extends Throwable>T fails(Class<T> type,Runnable action){try{action.run();}catch(Throwable error){if(type.isInstance(error))return type.cast(error);throw new AssertionError(error);}throw new AssertionError("missing "+type);}
    private static void eq(Object expected,Object actual){if(!Objects.equals(expected,actual))throw new AssertionError("expected="+expected+" actual="+actual);}
}
