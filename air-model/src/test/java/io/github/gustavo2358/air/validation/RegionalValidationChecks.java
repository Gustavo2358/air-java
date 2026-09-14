package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.math.BigInteger;
import java.util.*;

final class RegionalValidationChecks {
    static void binarySliceWidth() {
        var f=new Fixtures();f.capabilities.add(Capabilities.MEMORY_REGIONS);
        var region=new StorageId(f.pub,"r");
        f.storage.add(new Memory.Region(new Memory.StorageHeader(region,Optional.of(f.unit),Memory.Lifetime.ACTIVATION,Memory.Visibility.PRIVATE,f.origin),Optional.of(BigInteger.valueOf(4)),Optional.empty()));
        var op=f.op("write");
        var slice=new Places.RegionSlice(f.operand(op,"slice",Operand.Role.VALUE_WRITE),region,
                f.integer(op,"offset",0,Operand.Role.ADDRESS_READ),f.integer(op,"extent",1,Operand.Role.ADDRESS_READ),
                new Memory.BinaryCodec(false,BigInteger.valueOf(16),Memory.ByteOrder.BIG),Types.known(Types.Builtin.INT));
        f.linear(new Operations.Assign(f.header(op),slice,f.integer(op,"value",65,Operand.Role.VALUE_READ)));
        var result=AirValidator.validate(f.build());
        if(result.status()!=ValidationResult.Status.INVALID_IR || result.issues().stream().noneMatch(i->i.rule().equals("I-13")))
            throw new AssertionError("16-bit codec cannot use one-octet slice: "+result);
    }
    static void unknownExtentAccess() {
        for(boolean read:List.of(false,true)) {
            var f=new Fixtures();f.capabilities.add(Capabilities.MEMORY_REGIONS);
            var region=new StorageId(f.pub,"r");var gap=f.uncertainty("extent","LAYOUT_UNKNOWN");
            f.storage.add(new Memory.Region(new Memory.StorageHeader(region,Optional.of(f.unit),Memory.Lifetime.ACTIVATION,Memory.Visibility.PRIVATE,f.origin),Optional.empty(),Optional.of(gap)));
            var object=new ObjectId(f.unit,"view");var bytes=Types.known(Types.Builtin.BYTES);
            f.objects.add(new Memory.ObjectDeclaration(object,Optional.empty(),bytes,new Memory.ViewBinding(region,BigInteger.ZERO,BigInteger.ONE,Memory.IdentityBytes.INSTANCE),Memory.Visibility.PRIVATE,f.origin,Evidence.CoverageStatus.MODELED,f.precision()));
            var op=f.op("use");
            if(read) {
                var destination=f.object("destination",bytes);f.linear(f.assign("use",destination,f.read(op,"read",object,Operand.Role.VALUE_READ)));
            } else f.linear(f.assign("use",object,new Expressions.Literal(f.operand(op,"value",Operand.Role.VALUE_READ),new Values.BytesValue(List.of(65)))));
            var result=AirValidator.validate(f.build());
            if(result.status()!=ValidationResult.Status.INCOMPLETE_VALIDATION || !result.hasIssues(ValidationIssue.Kind.VALIDATION_LIMIT))
                throw new AssertionError("pure access bounds not discharged for unknown region extent: "+result);
        }
    }
}
