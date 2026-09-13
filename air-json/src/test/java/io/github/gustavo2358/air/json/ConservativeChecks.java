package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.validation.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static io.github.gustavo2358.air.json.ScalarAssignOracle.*;

/** Existing binding forms; no source-language semantics. */
final class ConservativeChecks {
    private ConservativeChecks() { }
    static void roundTrips() {
        for(int n:List.of(1,2,5,40))for(int form=0;form<4;form++) {
            var p=fixture(n,form); var v=AirValidator.validate(p);
            if(v.status()!=ValidationResult.Status.STRUCTURALLY_VALID)throw new AssertionError(v);
            var codec=new AirJson();var bytes=codec.encode(p);var restored=codec.decode(bytes);
            if(!p.equals(restored)||!Arrays.equals(bytes,codec.encode(restored)))throw new AssertionError("conservative round trip");
            String text=new String(bytes,StandardCharsets.UTF_8);
            String kind=switch(form){case 0->"havoc.must";case 1->"havoc.may";default->"opaque";};
            if(!text.contains("\"kind\":\""+kind+"\""))throw new AssertionError("binding operation token");
            if(form>=2&&!text.contains("\"otherWrites\":{\"kind\":\"within\""))throw new AssertionError("memory envelope lost");
            var invalid=text.replace("\"reason\":{", "\"unexpected\":{ ");
            if(form<2)expect(AirJsonException.Code.INPUT_ERROR,()->codec.decode(invalid.getBytes(StandardCharsets.UTF_8)));
        }
    }
    static Publication fixture(int n,int form) {
        var p=publication();var u=p.units().getFirst();var base=u.sequences().getFirst();
        var instructions=new ArrayList<Instruction>(base.instructions());var sequences=new ArrayList<Sequence>();
        var reason=gap("unproved-effects");
        for(int i=0;i<n;i++) {
            var op=new OperationId(UNIT,"conservative-"+i);var h=header(op,"assignment");
            var label=i==0?base.label():new LabelId(UNIT,"region-"+i);
            if(form==0)instructions.add(new Operations.HavocMust(h,new Places.ObjectPlace(new Operand.Header(new OperandId(new OperationOwner(op),"destination"),Operand.Role.VALUE_WRITE,origin("target")),OBJECT),reason));
            else if(form==1)instructions.add(new Operations.HavocMay(h,new Scopes.ObjectsMemory(List.of(OBJECT)),reason));
            else {
                var next=i+1<n?new LabelId(UNIT,"region-"+(i+1)):new LabelId(UNIT,"end");
                var memory=new Scopes.WithinMemory(form==2?new Scopes.ObjectsMemory(List.of(OBJECT)):new Scopes.StorageMemory(List.of(CELL)));
                var control=form==2?new Control.ControlEnvelope(List.of(new Control.JumpAlternative(next)),Scopes.NoControl.INSTANCE)
                    :new Control.ControlEnvelope(List.of(),new Scopes.WithinControl(new Scopes.LabelsControl(List.of(next))));
                var read=new Expressions.Read(new Operand.Header(new OperandId(new OperationOwner(op),"read"),Operand.Role.VALUE_READ,origin("literal")),
                    new Places.ObjectPlace(new Operand.Header(new OperandId(new OperationOwner(op),"place"),Operand.Role.VALUE_READ,origin("target")),OBJECT));
                var envelope=new Envelopes.Envelope(new Envelopes.MemoryEnvelope(List.of(read.header().id()),Scopes.NoMemory.INSTANCE,List.of(),memory,List.of()),control,
                    new Envelopes.DependencyEnvelope(List.of(),Scopes.AnyResource.INSTANCE));
                sequences.add(new Sequence(label,i==0?base.instructions():List.of(),new Operations.Opaque(h,"uninterpreted metadata",List.of(read),List.of(),envelope),base.origin()));
            }
        }
        if(form<2)sequences.add(new Sequence(base.label(),instructions,base.terminator(),base.origin()));
        else sequences.add(new Sequence(new LabelId(UNIT,"end"),List.of(),base.terminator(),base.origin()));
        var changed=new Unit(u.id(),u.containingUnit(),u.objects(),u.visibleObjects(),u.entries(),sequences,u.completionPorts(),u.body(),u.bodyUnavailable(),u.coverage(),u.origin());
        return new Publication(p.id(),p.airVersion(),p.capabilities(),p.artifacts(),List.of(changed),p.storage(),p.resources(),p.artifactRelations(),p.origins(),p.coverage(),p.uncertainties(),p.premises());
    }
    private static void expect(AirJsonException.Code code,Runnable action) {
        try{action.run();throw new AssertionError("expected "+code);}catch(AirJsonException e){if(e.code()!=code)throw new AssertionError(e);}
    }
}
