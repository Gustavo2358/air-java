package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.math.BigInteger;
import java.util.*;

public final class PossibleEntryChecks {
    private PossibleEntryChecks() { }
    public static void main(String[] args) {run();System.out.println("POSSIBLE_ENTRY_MODEL=PASS");}
    static Entries.InitialCondition possible(Fixtures f,String id,int start,int length,String text) {
        var seed=RegionalInitialChecks.seed(f,id,start,length,text,RuntimeCodecChecks.IBM);
        var reason=f.uncertainty(id+"-remainder","ENTRY_LIFECYCLE_OPEN");
        return new Entries.InitialCondition(seed.place(),new Entries.PossibleLiterals(List.of(((Entries.LiteralInitial)seed.value()).value()),reason),seed.origin(),seed.premises());
    }
    static Fixtures fixture() {var f=RegionalInitialChecks.fixture();f.capabilities.add(Capabilities.ENTRY_POSSIBILITIES);return f;}
    static void valid(Fixtures f) {var r=AirValidator.validate(f.build());if(!r.isStructurallyValid())throw new AssertionError(r);}
    static void refused(Fixtures f,String rule) {var r=AirValidator.validate(f.build());if(r.issues().stream().noneMatch(i->i.rule().equals(rule)))throw new AssertionError(rule+": "+r);}
    static void run() {
        for(boolean reverse:List.of(false,true)) {
            var f=fixture();var a=possible(f,"a",0,4,"ABCD");var b=RegionalInitialChecks.seed(f,"b",4,4,"EFGH",RuntimeCodecChecks.IBM);
            f.state=new Entries.EntryState(reverse?List.of(b,a):List.of(a,b),List.of());valid(f);
            f=fixture();a=possible(f,"a",0,4,"ABCD");b=RegionalInitialChecks.seed(f,"b",3,4,"DEFG",RuntimeCodecChecks.IBM);
            f.state=new Entries.EntryState(reverse?List.of(b,a):List.of(a,b),List.of());refused(f,"I-17");
        }
        var f=fixture();var a=possible(f,"a",0,4,"ABCD");f.state=new Entries.EntryState(List.of(a),List.of());valid(f);
        f.capabilities.remove(Capabilities.ENTRY_POSSIBILITIES);refused(f,"I-43");
        f=fixture();a=possible(f,"bad-fit",0,4,"ABC");f.state=new Entries.EntryState(List.of(a),List.of());
        if(AirValidator.validate(f.build()).isStructurallyValid())throw new AssertionError("inexact candidate encoding accepted");
        f=fixture();a=possible(f,"bad-codec",0,1,"€");f.state=new Entries.EntryState(List.of(a),List.of());
        if(AirValidator.validate(f.build()).isStructurallyValid())throw new AssertionError("unsupported candidate bytes accepted");
        // Distinct StorageId bases are independent without a physical source-layout proof.
        f=fixture();a=possible(f,"region",0,4,"ABCD");var object=f.object("other",Types.known(Types.Builtin.TEXT));
        var place=new Places.ObjectPlace(f.entryOperand("other-place",Operand.Role.VALUE_WRITE),object);
        var b=new Entries.InitialCondition(place,new Entries.LiteralInitial(new Expressions.Literal(f.entryOperand("other-value",Operand.Role.VALUE_READ),new Values.TextValue("OTHER"))),f.origin,List.of());
        f.state=new Entries.EntryState(List.of(a,b),List.of());valid(f);
        f.premises.add(new Proofs.Premise(new PremiseId(f.pub,"separated"),"oracle","independent allocations",f.origin,
            new Proofs.DisjointStorage(List.of(new StorageId(f.pub,"region"),new StorageId(f.pub,"other-cell")))));valid(f);
        // A candidate list is immutable and has no small cardinality ceiling.
        f=fixture();a=possible(f,"many",0,4,"0000");var candidates=new ArrayList<Expressions.Literal>();
        for(int i=0;i<257;i++)candidates.add(new Expressions.Literal(f.entryOperand("candidate-"+i,Operand.Role.VALUE_READ),new Values.TextValue(String.format("%04d",i))));
        var v=new Entries.PossibleLiterals(candidates,((Entries.PossibleLiterals)a.value()).remainder());candidates.clear();
        f.state=new Entries.EntryState(List.of(new Entries.InitialCondition(a.place(),v,a.origin(),a.premises())),List.of());valid(f);
        if(v.candidates().size()!=257)throw new AssertionError("mutable candidates");
        try{v.candidates().clear();throw new AssertionError("mutable list");}catch(UnsupportedOperationException expected){ }
        var original=f.uncertainties.getFirst();f.uncertainties.set(0,new Evidence.Uncertainty(original.id(),original.code(),List.of(Evidence.Dimension.EFFECTS),original.scope(),original.reason(),original.origin()));refused(f,"I-17");
    }
}
