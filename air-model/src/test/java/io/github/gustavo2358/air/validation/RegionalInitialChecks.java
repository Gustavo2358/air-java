package io.github.gustavo2358.air.validation;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.math.BigInteger;
import java.util.*;
public final class RegionalInitialChecks {
    private RegionalInitialChecks() { }
    public static void main(String[] args) {run();System.out.println("REGIONAL_INITIAL=PASS simultaneous equal/contradictory/reordered/bounds/codec/preserve");}
    static void run() {
        for(boolean reverse:List.of(false,true)) {
            var equal=AirValidator.validate(publication(false,reverse));if(!equal.isStructurallyValid())throw new AssertionError("equal partial-overlap seeds: "+equal);
            var different=AirValidator.validate(publication(true,reverse));
            if(different.issues().stream().noneMatch(e->e.rule().equals("I-17")))throw new AssertionError("contradiction must be I-17: "+different);
        }
        var f=fixture();var s=seed(f,"bad-length",2,4,"ABC",RuntimeCodecChecks.IBM);f.state=new Entries.EntryState(List.of(s),List.of());
        if(AirValidator.validate(f.build()).isStructurallyValid())throw new AssertionError("initial bytes must fill extent");
        f=fixture();f.state=new Entries.EntryState(List.of(seed(f,"bad-codec",0,1,"€",RuntimeCodecChecks.IBM)),List.of());
        if(AirValidator.validate(f.build()).isStructurallyValid())throw new AssertionError("codec must encode initial text");
        f=fixture();var p=seed(f,"preserve",0,4,"ABCD",RuntimeCodecChecks.IBM).place();f.state=new Entries.EntryState(List.of(new Entries.InitialCondition(p,Entries.Preserve.INSTANCE,f.origin,List.of())),List.of());
        if(AirValidator.validate(f.build()).isStructurallyValid())throw new AssertionError("activation region cannot preserve previous activation");
    }
    static Fixtures fixture() {
        var f=new Fixtures();f.capabilities.add(Capabilities.MEMORY_REGIONS);f.capabilities.add(Capabilities.IBM1047);
        f.storage.add(new Memory.Region(new Memory.StorageHeader(new StorageId(f.pub,"region"),Optional.of(f.unit),Memory.Lifetime.ACTIVATION,Memory.Visibility.PRIVATE,f.origin),Optional.of(BigInteger.TEN),Optional.empty()));
        f.sequence("start",List.of(),f.halt("stop"));return f;
    }
    static Entries.InitialCondition seed(Fixtures f,String id,int offset,int extent,String text,Memory.Codec codec) {
        var place=new Places.RegionSlice(f.entryOperand(id+"-place",Operand.Role.VALUE_WRITE),new StorageId(f.pub,"region"),
            new Expressions.Literal(f.entryOperand(id+"-offset",Operand.Role.VALUE_READ),new Values.IntValue(BigInteger.valueOf(offset))),
            new Expressions.Literal(f.entryOperand(id+"-extent",Operand.Role.VALUE_READ),new Values.IntValue(BigInteger.valueOf(extent))),codec,Types.known(Types.Builtin.TEXT));
        return new Entries.InitialCondition(place,new Entries.LiteralInitial(new Expressions.Literal(f.entryOperand(id+"-value",Operand.Role.VALUE_READ),new Values.TextValue(text))),f.origin,List.of());
    }
    static Publication publication(boolean contradiction,boolean reverse) {
        var f=fixture();var seeds=new ArrayList<Entries.InitialCondition>();
        seeds.add(seed(f,"left",0,6,"ABCDEF",RuntimeCodecChecks.IBM));seeds.add(seed(f,"right",2,6,contradiction?"XDEFGH":"CDEFGH",RuntimeCodecChecks.IBM));
        seeds.add(seed(f,"inner",3,2,"DE",RuntimeCodecChecks.IBM));if(reverse)Collections.reverse(seeds);
        f.state=new Entries.EntryState(seeds,List.of());return f.build();
    }
}
