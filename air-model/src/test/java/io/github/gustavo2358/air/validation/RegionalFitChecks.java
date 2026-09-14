package io.github.gustavo2358.air.validation;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.math.BigInteger;
import java.util.*;
public final class RegionalFitChecks {
    private RegionalFitChecks() { }
    public static void main(String[] args) {run();RuntimeCodecChecks.run();RegionalValidationChecks.binarySliceWidth();RegionalValidationChecks.unknownExtentAccess();System.out.println("REGIONAL_FIT=PASS positives=3 negatives=4");}
    static void run() {
        for(int n:List.of(1,2,4)) {
            var result=check(RuntimeCodecChecks.IBM,n,n," ");
            if(!result.isStructurallyValid())throw new AssertionError("bounded IBM fit must validate: "+result);
        }
        for(var result:List.of(check(RuntimeCodecChecks.IBM,3,4," "),check(RuntimeCodecChecks.IBM,4,4,"€"),
                check(Memory.AsciiText.INSTANCE,4,4," "),check(new Memory.ExtensionCodec("text.ebcdic.ibm1047","2",Types.known(Types.Builtin.TEXT)),4,4," "))) {
            if(result.isStructurallyValid())throw new AssertionError("unsupported shape must remain explicit: "+result);
        }
    }
    static ValidationResult check(Memory.Codec sourceCodec,int resultLength,int destinationLength,String pad) {
        var f=new Fixtures();f.capabilities.add(Capabilities.MEMORY_REGIONS);f.capabilities.add(Capabilities.IBM1047);var src=new StorageId(f.pub,"source");var dst=new StorageId(f.pub,"destination");
        for(var id:List.of(src,dst))f.storage.add(new Memory.Region(new Memory.StorageHeader(id,Optional.of(f.unit),Memory.Lifetime.ACTIVATION,Memory.Visibility.PRIVATE,f.origin),Optional.of(BigInteger.valueOf(8)),Optional.empty()));
        var op=f.op("fit");
        var source=new Places.RegionSlice(f.operand(op,"source-place",Operand.Role.VALUE_READ),src,f.integer(op,"source-offset",0,Operand.Role.VALUE_READ),f.integer(op,"source-length",2,Operand.Role.VALUE_READ),sourceCodec,Types.known(Types.Builtin.TEXT));
        var target=new Places.RegionSlice(f.operand(op,"target-place",Operand.Role.VALUE_WRITE),dst,f.integer(op,"target-offset",0,Operand.Role.VALUE_READ),f.integer(op,"target-length",destinationLength,Operand.Role.VALUE_READ),RuntimeCodecChecks.IBM,Types.known(Types.Builtin.TEXT));
        var read=new Expressions.Read(f.operand(op,"source",Operand.Role.VALUE_READ),source);
        f.linear(new Operations.Assign(f.header(op),target,new Expressions.FitText(f.operand(op,"fit",Operand.Role.VALUE_READ),read,BigInteger.valueOf(resultLength),pad)));
        return AirValidator.validate(f.build());
    }
}
