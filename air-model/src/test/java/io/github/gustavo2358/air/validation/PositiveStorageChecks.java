package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;

/** PMT W1: independent bases require no source-layout proof; integrity remains mandatory. */
public final class PositiveStorageChecks {
    private PositiveStorageChecks() { }
    public static void main(String[] args) { run(); System.out.println("POSITIVE_STORAGE_MODEL=PASS"); }
    static void run() {
        for (boolean reverse : List.of(false, true)) {
            var f=PossibleEntryChecks.fixture();
            var regional=PossibleEntryChecks.possible(f,"regional",0,4,"ABCD");
            var object=f.object("other",Types.known(Types.Builtin.TEXT));
            var cell=new Entries.InitialCondition(
                new Places.ObjectPlace(f.entryOperand("cell-place",Operand.Role.VALUE_WRITE),object),
                new Entries.LiteralInitial(new Expressions.Literal(f.entryOperand("cell-value",Operand.Role.VALUE_READ),new Values.TextValue("OTHER"))),f.origin,List.of());
            f.state=new Entries.EntryState(reverse?List.of(cell,regional):List.of(regional,cell),List.of());
            requireValid(f,"different Cell/Region bases without disjoint");
            if(!f.premises.isEmpty())throw new AssertionError("oracle must not supply a negative premise");
            // A duplicate nominal object with an exact alias does not create another base.
            var alias=f.alias("alias",object,Types.known(Types.Builtin.TEXT));
            var conflict=new Entries.InitialCondition(new Places.ObjectPlace(f.entryOperand("alias-place",Operand.Role.VALUE_WRITE),alias),
                new Entries.LiteralInitial(new Expressions.Literal(f.entryOperand("alias-value",Operand.Role.VALUE_READ),new Values.TextValue("WRONG"))),f.origin,List.of());
            f.state=new Entries.EntryState(List.of(cell,conflict,regional),List.of());
            requireInvalid(f,"I-17");
        }
        for(int count:List.of(0,1,50)) {
            var f=new Fixtures();var x=f.object("x",Types.known(Types.Builtin.TEXT));
            var assign=f.assign("set",x,f.text(f.op("set"),"literal","KEEP"));
            var reasons=new ArrayList<UncertaintyId>();
            for(int i=0;i<count;i++)reasons.add(f.uncertainty("gap-"+i,"fixture:REPRESENTATION_OMITTED"));
            var claim=new Evidence.Claim(new Scopes.EntityScope(List.of(x,assign.header().id())),
                count==0?Evidence.PrecisionStatus.EXACT:Evidence.PrecisionStatus.UNAVAILABLE,reasons);
            var precision=new Evidence.Precision(claim,claim,claim,claim,claim);
            var old=f.objects.getFirst();
            f.objects.set(0,new Memory.ObjectDeclaration(old.id(),old.displayName(),old.typeRef(),old.storage(),old.visibility(),old.origin(),
                count==0?Evidence.CoverageStatus.MODELED:Evidence.CoverageStatus.ABSTRACTED,precision));
            f.linear(new Operations.Assign(new Operations.Header(assign.header().id(),f.origin,Evidence.CoverageStatus.ABSTRACTED,precision,reasons),assign.destination(),assign.value()));
            requireValid(f,"diagnostic claims do not invalidate a coherent assign; count="+count);
            f.storage.clear();requireInvalid(f,"I-02");
        }
    }
    private static void requireValid(Fixtures f,String why) {
        var r=AirValidator.validate(f.build());if(!r.isStructurallyValid())throw new AssertionError(why+": "+r);
    }
    private static void requireInvalid(Fixtures f,String rule) {
        var r=AirValidator.validate(f.build());if(r.status()!=ValidationResult.Status.INVALID_IR||r.issues().stream().noneMatch(i->i.rule().contains(rule)))throw new AssertionError("missing "+rule+": "+r);
    }
}
