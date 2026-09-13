package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.validation.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static io.github.gustavo2358.air.json.ScalarAssignOracle.*;

/** Independent existing AIR int domain; no arithmetic or integer literal codec. */
final class IntegerTypeChecks {
    static void roundTrip() {
        var p=publication(1,0);var u=p.units().getFirst();var s=u.sequences().getFirst();
        var type=Types.known(Types.Builtin.INT);var object=u.objects().getFirst();var cell=(Memory.Cell)p.storage().getFirst();
        var op=ASSIGN;var owner=new OperationOwner(op);
        var place=new Places.ObjectPlace(new Operand.Header(new OperandId(owner,"read-place"),Operand.Role.VALUE_READ,origin("target")),OBJECT);
        var read=new Expressions.Read(new Operand.Header(new OperandId(owner,"read"),Operand.Role.VALUE_READ,origin("literal")),place);
        var unknown=new Expressions.Unknown(new Operand.Header(new OperandId(owner,"unknown"),Operand.Role.VALUE_READ,origin("literal")),type,List.of(read),Scopes.NoMemory.INSTANCE,gap("unproved-values"));
        var assign=new Operations.Assign(header(op,"assignment"),new Places.ObjectPlace(new Operand.Header(new OperandId(owner,"write"),Operand.Role.VALUE_WRITE,origin("target")),OBJECT),unknown);
        var integerObject=new Memory.ObjectDeclaration(object.id(),object.displayName(),type,object.storage(),object.visibility(),object.origin(),object.coverage(),object.precision());
        var unit=new io.github.gustavo2358.air.model.Unit(u.id(),u.containingUnit(),List.of(integerObject),u.visibleObjects(),u.entries(),
            List.of(new Sequence(s.label(),List.of(assign),s.terminator(),s.origin())),u.completionPorts(),u.body(),u.bodyUnavailable(),u.coverage(),u.origin());
        var changed=new Publication(p.id(),p.airVersion(),p.capabilities(),p.artifacts(),List.of(unit),List.of(new Memory.Cell(cell.header(),type)),p.resources(),p.artifactRelations(),p.origins(),p.coverage(),p.uncertainties(),p.premises());
        var validation=AirValidator.validate(changed);if(validation.status()!=ValidationResult.Status.STRUCTURALLY_VALID)throw new AssertionError(validation);
        var codec=new AirJson();var bytes=codec.encode(changed);
        if(!changed.equals(codec.decode(bytes))||!Arrays.equals(bytes,codec.encode(codec.decode(bytes))))throw new AssertionError("integer facts round trip");
        var text=new String(bytes,StandardCharsets.UTF_8);
        if(!text.contains("\"type\":{\"kind\":\"int\"}"))throw new AssertionError("normative int token");
        try {codec.decode(text.replace("\"kind\":\"int\"","\"kind\":\"int\",\"extra\":null").getBytes(StandardCharsets.UTF_8));throw new AssertionError("unknown int field accepted");}
        catch(AirJsonException e){if(e.code()!=AirJsonException.Code.INPUT_ERROR)throw new AssertionError(e);}
    }
}
