package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.validation.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static io.github.gustavo2358.air.json.ScalarAssignOracle.*;

/** AIR-only interval facts chosen by hand from AIR 03/04, independently of mappings. */
final class RegionalChecks {
    static final BigInteger HUGE = new BigInteger("184467440737095516170");
    static final Types.TypeRef BYTES = Types.known(Types.Builtin.BYTES);
    static final Memory.Codec IBM = new Memory.ExtensionCodec("text.ebcdic.ibm1047", "1", TEXT);
    static final Capabilities.Capability IBM_CAP = new Capabilities.Capability("text.ebcdic.ibm1047", "1");
    static Operand.Header operand(OperationId op, String name, Operand.Role role) {
        return new Operand.Header(new OperandId(new OperationOwner(op),name),role,origin("target"));
    }
    static Expressions.Literal integer(OperationId op, String name, BigInteger n) {
        return new Expressions.Literal(operand(op,name,Operand.Role.ADDRESS_READ),new Values.IntValue(n));
    }
    static Memory.ObjectDeclaration view(ObjectId id, Memory.Binding binding, Types.TypeRef type) {
        return new Memory.ObjectDeclaration(id,Optional.empty(),type,binding,Memory.Visibility.PRIVATE,
                origin("data"),Evidence.CoverageStatus.MODELED,precision());
    }
    static Publication regional() {
        var p=publication(); var u=p.units().getFirst(); var s=u.sequences().getFirst();
        var storage=new ArrayList<Memory.Storage>();
        storage.add(new Memory.Region(cell(CELL).header(),Optional.of(HUGE.add(BigInteger.TEN)),Optional.empty()));
        storage.add(new Memory.Region(cell(new StorageId(PUB,"unknown-extent")).header(),Optional.empty(),Optional.of(gap("unproved-storage"))));
        var objects=List.of(view(OBJECT,new Memory.ViewBinding(CELL,HUGE,BigInteger.valueOf(4),Memory.IdentityBytes.INSTANCE),BYTES),
                view(new ObjectId(UNIT,"exact-alias"),new Memory.AliasBinding(OBJECT),BYTES),
                view(new ObjectId(UNIT,"unknown-codec"),new Memory.ViewBinding(CELL,BigInteger.ZERO,BigInteger.ONE,
                        new Memory.UnknownCodec(TEXT,gap("unproved-values"))),TEXT));
        var assign=new Operations.Assign(header(ASSIGN,"assignment"),new Places.ObjectPlace(operand(ASSIGN,"write",Operand.Role.VALUE_WRITE),OBJECT),
                new Expressions.Literal(operand(ASSIGN,"bytes",Operand.Role.VALUE_READ),new Values.BytesValue(List.of(0,127,128,255))));
        var copyId=new OperationId(UNIT,"capture-bytes");
        var source=new Memory.ByteRange(CELL,integer(copyId,"source-offset",HUGE),integer(copyId,"source-extent",BigInteger.valueOf(4)));
        var destination=new Memory.ByteRange(CELL,integer(copyId,"destination-offset",BigInteger.valueOf(2)),integer(copyId,"destination-extent",BigInteger.valueOf(4)));
        var bound=new Scopes.WithinMemory(new Scopes.StorageMemory(List.of(CELL)));
        var fallback=new Envelopes.Envelope(new Envelopes.MemoryEnvelope(List.of(),bound,List.of(),bound,List.of()),
                new Control.ControlEnvelope(List.of(Control.ContinueAlternative.INSTANCE),Scopes.NoControl.INSTANCE),
                new Envelopes.DependencyEnvelope(List.of(),Scopes.NoResources.INSTANCE));
        var copy=new Operations.CopyBytes(header(copyId,"assignment"),destination,source,BigInteger.valueOf(4),fallback);
        var sliceId=new OperationId(UNIT,"slice-write");
        var slice=new Places.RegionSlice(operand(sliceId,"slice",Operand.Role.VALUE_WRITE),CELL,
                integer(sliceId,"offset",BigInteger.ZERO),integer(sliceId,"length",BigInteger.valueOf(2)),Memory.IdentityBytes.INSTANCE,BYTES);
        var sliceWrite=new Operations.Assign(header(sliceId,"assignment"),slice,
                new Expressions.Literal(operand(sliceId,"value",Operand.Role.VALUE_READ),new Values.BytesValue(List.of(193,194))));
        var unit=new io.github.gustavo2358.air.model.Unit(u.id(),u.containingUnit(),objects,u.visibleObjects(),u.entries(),
                List.of(new Sequence(s.label(),List.of(assign,copy,sliceWrite),s.terminator(),s.origin())),u.completionPorts(),u.body(),u.bodyUnavailable(),u.coverage(),u.origin());
        return new Publication(p.id(),p.airVersion(),new Capabilities.Manifest(List.of(Capabilities.MEMORY_REGIONS),List.of(Capabilities.MEMORY_REGIONS)),
                p.artifacts(),List.of(unit),storage,p.resources(),p.artifactRelations(),p.origins(),p.coverage(),p.uncertainties(),p.premises());
    }
    static void roundTrip() {
        var p=regional(); var validation=AirValidator.validate(p);
        require(validation.status()==ValidationResult.Status.STRUCTURALLY_VALID,"manual regional AIR valid: "+validation);
        var codec=new AirJson(); var bytes=codec.encode(p); var decoded=codec.decode(bytes);
        try {
            var manual=java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/test/resources/regional.canonical.json"));
            require(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(manual))
                    .equals("3ce2442ac38b8b62fc2fb51998d430162e9f726392b14db1b348f59fe89c2677"),"independent golden hash");
            require(Arrays.equals(bytes,manual),"encode equals independent complete manual wire");
            require(p.equals(codec.decode(manual)),"independent complete wire decodes to manual model");
        } catch(java.io.IOException | java.security.NoSuchAlgorithmException e){throw new AssertionError(e);}
        require(p.equals(decoded),"all regional records and order preserved");
        require(Arrays.equals(bytes,codec.encode(decoded)),"canonical bytes deterministic");
        var wire=new String(bytes,StandardCharsets.UTF_8);
        require(wire.contains("\"offset\":\"184467440737095516170\""),"BigInteger offset untruncated");
        require(wire.contains("\"kind\":\"bytes\",\"base64\":\"AH+A/w==\"") || wire.contains("\"base64\":\"AH+A/w==\",\"kind\":\"bytes\""),"manual octet oracle");
        require(wire.contains("\"kind\":\"copy_bytes\""),"copy remains an event");
        var unknown=(Memory.Region)decoded.storage().get(1);
        require(unknown.extent().isEmpty() && unknown.extentUnknown().equals(Optional.of(gap("unproved-storage"))),"unknown is not zero");
        reject(wire.replace("\"version\":\"1\"","\"version\":\"2\""),AirJsonException.Code.UNSUPPORTED_CAPABILITY);
        reject(wire.replace("\"required\":[{\"name\":\"memory.regions\",\"version\":\"1\"}]","\"required\":[]"),AirJsonException.Code.INVALID_IR);
        reject(wire.replace("\"offset\":\"184467440737095516170\",",""),AirJsonException.Code.INPUT_ERROR);
        reject(wire.replace("\"offset\":\"184467440737095516170\"","\"offset\":\"184467440737095516180\""),AirJsonException.Code.INVALID_IR);
        reject(wire.replace("\"base64\":\"AH+A/w==\"","\"base64\":\"AH+A\""),AirJsonException.Code.INVALID_IR);
        reject(wire.replace("\"base64\":\"AH+A/w==\"","\"base64\":\"0g\""),AirJsonException.Code.INPUT_ERROR);
        reject(wire.replace("\"kind\":\"bytes.identity\"","\"kind\":\"invented\""),AirJsonException.Code.INPUT_ERROR);
        var tree=Json.parse(bytes,AirJson.Limits.defaults());
        negative(tree,"publication.units.0.objects.0.storage.region.localId",Json.value("dangling"),AirJsonException.Code.INVALID_IR);
        negative(tree,"publication.units.0.objects.1.storage.object.localId",Json.value("exact-alias"),AirJsonException.Code.INVALID_IR);
        negative(tree,"publication.units.0.sequences.0.instructions.1.length",Json.value("5"),AirJsonException.Code.INVALID_IR);
        negative(tree,"publication.units.0.sequences.0.instructions.2.destination.offset.value.value",Json.value("-1"),AirJsonException.Code.INVALID_IR);
        negative(tree,"publication.units.0.sequences.0.instructions.2.destination.offset.value.value",Json.value("-0"),AirJsonException.Code.INPUT_ERROR);
        negative(tree,"publication.units.0.objects.0.storage.offset",Json.value("01"),AirJsonException.Code.INPUT_ERROR);
        for(String value:List.of("AH+A/x==","AH+A/w","AH+A/w==\n","AH-A_w=="))
            negative(tree,"publication.units.0.sequences.0.instructions.0.value.value.base64",Json.value(value),AirJsonException.Code.INPUT_ERROR);
        for(int count:List.of(1,2,5,40)) {
            var u=p.units().getFirst();var s=u.sequences().getFirst();var instructions=new ArrayList<Instruction>(s.instructions());
            for(int i=0;i<count;i++) {
                var op=new OperationId(UNIT,"extra-"+i);
                instructions.add(new Operations.Assign(header(op,"assignment"),new Places.ObjectPlace(operand(op,"write",Operand.Role.VALUE_WRITE),OBJECT),
                        new Expressions.Literal(operand(op,"value",Operand.Role.VALUE_READ),new Values.BytesValue(List.of(0,127,128,255)))));
            }
            var unit=new io.github.gustavo2358.air.model.Unit(u.id(),u.containingUnit(),u.objects(),u.visibleObjects(),u.entries(),
                    List.of(new Sequence(s.label(),instructions,s.terminator(),s.origin())),u.completionPorts(),u.body(),u.bodyUnavailable(),u.coverage(),u.origin());
            var many=new Publication(p.id(),p.airVersion(),p.capabilities(),p.artifacts(),List.of(unit),p.storage(),p.resources(),p.artifactRelations(),p.origins(),p.coverage(),p.uncertainties(),p.premises());
            require(many.equals(codec.decode(codec.encode(many))),"regional cardinality "+count);
        }
    }
    static void negative(Json.Value tree,String path,Json.Value value,AirJsonException.Code code) {
        reject(new String(Json.write(edit(tree,path.split("\\."),0,value),AirJson.Limits.defaults()),StandardCharsets.UTF_8),code);
    }
    static Publication ibmPublication() {
        var p=regional();var u=p.units().getFirst();var s=u.sequences().getFirst();
        var objects=new ArrayList<Memory.ObjectDeclaration>();
        objects.add(view(OBJECT,new Memory.ViewBinding(CELL,HUGE,BigInteger.valueOf(4),IBM),TEXT));
        objects.add(view(new ObjectId(UNIT,"exact-alias"),new Memory.AliasBinding(OBJECT),TEXT));
        objects.add(u.objects().get(2));
        var sink=new ObjectId(UNIT,"text-result");var sinkStorage=new StorageId(PUB,"text-result-storage");
        objects.add(object(sink,sinkStorage));
        var storage=new ArrayList<Memory.Storage>(p.storage());storage.add(cell(sinkStorage));
        var instructions=new ArrayList<Instruction>(s.instructions());
        instructions.set(0,assign(ASSIGN,OBJECT," Aé "));
        var op=new OperationId(UNIT,"decode-view");
        var read=new Expressions.Read(operand(op,"read",Operand.Role.VALUE_READ),new Places.ObjectPlace(operand(op,"source",Operand.Role.VALUE_READ),OBJECT));
        instructions.add(new Operations.Assign(header(op,"assignment"),new Places.ObjectPlace(operand(op,"destination",Operand.Role.VALUE_WRITE),sink),read));
        var unit=new io.github.gustavo2358.air.model.Unit(u.id(),u.containingUnit(),objects,u.visibleObjects(),u.entries(),
                List.of(new Sequence(s.label(),instructions,s.terminator(),s.origin())),u.completionPorts(),u.body(),u.bodyUnavailable(),u.coverage(),u.origin());
        return new Publication(p.id(),p.airVersion(),new Capabilities.Manifest(List.of(Capabilities.MEMORY_REGIONS,IBM_CAP),List.of()),
                p.artifacts(),List.of(unit),storage,p.resources(),p.artifactRelations(),p.origins(),p.coverage(),p.uncertainties(),p.premises());
    }
    static void ibmProfile() {
        var p=ibmPublication();var validation=AirValidator.validate(p);
        require(validation.status()==ValidationResult.Status.STRUCTURALLY_VALID,"explicit IBM1047 literal and total read valid: "+validation);
        var codec=new AirJson();var bytes=codec.encode(p);
        require(p.equals(codec.decode(bytes)),"IBM1047 identity and logical text preserved");
        var tree=Json.parse(bytes,AirJson.Limits.defaults());
        negative(tree,"publication.units.0.sequences.0.instructions.0.value.value.value",Json.value(" A€ "),AirJsonException.Code.INVALID_IR);
        negative(tree,"publication.units.0.sequences.0.instructions.0.value.value.value",Json.value("A"),AirJsonException.Code.INVALID_IR);
        negative(tree,"publication.units.0.objects.0.storage.codec.logicalType.type.kind",Json.value("bytes"),AirJsonException.Code.INVALID_IR);
        negative(tree,"publication.units.0.objects.0.storage.codec.version",Json.value("2"),AirJsonException.Code.INVALID_IR);
        negative(tree,"publication.capabilities.required.1.version",Json.value("2"),AirJsonException.Code.UNSUPPORTED_CAPABILITY);
    }
    static Json.Value edit(Json.Value node,String[] path,int at,Json.Value replacement) {
        if(at==path.length)return replacement;
        if(node instanceof Json.Obj o){var fields=new LinkedHashMap<>(o.fields());fields.put(path[at],edit(fields.get(path[at]),path,at+1,replacement));return new Json.Obj(fields);}
        if(node instanceof Json.Arr a){var values=new ArrayList<>(a.values());int index=Integer.parseInt(path[at]);values.set(index,edit(values.get(index),path,at+1,replacement));return new Json.Arr(values);}
        throw new AssertionError("invalid test path");
    }
    static void reject(String wire, AirJsonException.Code code) {
        try {new AirJson().decode(wire.getBytes(StandardCharsets.UTF_8));throw new AssertionError("accepted invalid/unsupported wire: "+code);}
        catch(AirJsonException e){require(e.code()==code,"expected "+code+" but got "+e);}
    }
    static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
