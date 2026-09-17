package io.github.gustavo2358.air.json;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.validation.*;
import java.util.*;
import static io.github.gustavo2358.air.json.ResourceBindingOracle.*;
final class ResourceBindingChecks {
    private ResourceBindingChecks(){}
    static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static Publication resources(Publication p,List<Interactions.Resource> rs){return new Publication(p.id(),p.airVersion(),p.capabilities(),p.artifacts(),p.units(),p.storage(),rs,p.artifactRelations(),p.origins(),p.coverage(),p.uncertainties(),p.premises());}
    static void run(){
        for(String example:List.of("A1","A2","A3","A4","A6")){
            var p=publication(example);var validation=AirValidator.validate(p);
            require(validation.isStructurallyValid(),example+": "+validation.issues());
            var codec=new AirJson();var bytes=codec.encode(p);var q=codec.decode(bytes);
            require(p.equals(q),example+" roundtrip");require(Arrays.equals(bytes,codec.encode(q)),example+" deterministic");
            require(q.resources().size()==switch(example){case "A2","A3"->2;case "A4"->0;case "A6"->4;default->1;},"independent declaration counts");
            if(example.equals("A1"))require(q.resources().get(0).declaration().orElseThrow().uses().isEmpty(),"A1 no use");
            if(example.equals("A2"))require(q.resources().get(0).declaration().orElseThrow().objects().get(0).object().equals(object("U1","record")),"A2 FROM does not choose declaration");
            if(example.equals("A3"))require(!q.resources().get(0).declaration().orElseThrow().owner().equals(q.resources().get(1).declaration().orElseThrow().owner()),"A3 owners distinct");
            if(example.equals("A6"))require(q.resources().stream().filter(r->r.description() instanceof Interactions.LocalResource).count()==1,"A6 work has no external target");
        }
        var p=publication("A1");var r=p.resources().get(0);var d=r.declaration().orElseThrow();
        var dangling=new Interactions.ResourceDeclaration(new UnitId(PUB,"absent"),d.name(),d.classification(),d.nameSource(),d.objects(),d.uses());
        invalid(resources(p,List.of(new Interactions.Resource(r.id(),r.description(),r.origin(),Optional.of(dangling)))),"I-RB-01");
        var duplicate=new Interactions.ResourceDeclaration(d.owner(),d.name(),d.classification(),d.nameSource(),List.of(d.objects().get(0),d.objects().get(0)),d.uses());
        invalid(resources(p,List.of(new Interactions.Resource(r.id(),r.description(),r.origin(),Optional.of(duplicate)))),"I-RB-02");
        var noCap=new Publication(p.id(),p.airVersion(),new Capabilities.Manifest(List.of(),List.of()),p.artifacts(),p.units(),p.storage(),p.resources(),p.artifactRelations(),p.origins(),p.coverage(),p.uncertainties(),p.premises());
        invalid(noCap,"I-43");
        var wire=new String(new AirJson().encode(p),java.nio.charset.StandardCharsets.UTF_8);
        require(wire.contains("\"declaration\""),"explicit declaration wire");
        try{new AirJson().decode(wire.replace("\"owner\":","\"missingOwner\":").getBytes(java.nio.charset.StandardCharsets.UTF_8));throw new AssertionError("missing owner accepted");}catch(AirJsonException expected){require(expected.code()==AirJsonException.Code.INPUT_ERROR,"shape rejection");}
    }
    static void wireOracle(){
        var p=publication("A2");var tree=InvokeChecks.tree(p);
        var declaration=Json.object("owner",global("unit","U1"),"name","F","classification","cobol.fd","nameSource","cobol.assignment-name",
            "objects",new Json.Arr(List.of(Json.object("object",owned("object","record"),"role","record"))),
            "uses",new Json.Arr(List.of(Json.object("operation",owned("operation","io-0"),"role","output","origin",global("origin","source")))));
        require(declaration.equals(InvokeChecks.at(tree,"publication.resources.0.declaration")),"independently authored declaration fields and IDs");
        var target=Json.object("kind","literal","category","file","namespace","cobol.external-file-name","name","CLIENTDD","namePolicy",Json.object("kind","exact"),"origin",global("origin","source"));
        require(target.equals(InvokeChecks.at(tree,"publication.resources.0.description")),"independent source-only descriptor");
        var decoded=new AirJson().decode(InvokeChecks.wire(InvokeChecks.edit(tree,"publication.resources.0.declaration",declaration)));
        require(decoded.equals(p),"reader accepts independently authored declaration");
        require(decoded.resources().get(1).declaration().orElseThrow().uses().isEmpty(),"FROM owner G has no operational use");
        var a6=InvokeChecks.tree(publication("A6"));
        require(Json.object("kind","local","category","file").equals(InvokeChecks.at(a6,"publication.resources.3.description")),"local has no name/namespace");
    }
    static Json.Value global(String domain,String local){return Json.object("domain",domain,"publication","fd-w1-manual","localId",local);}
    static Json.Value owned(String domain,String local){return Json.object("domain",domain,"publication","fd-w1-manual","unit","U1","localId",local);}
    static void adversarial(){
        var p=publication("A2");var r=p.resources().get(0);var d=r.declaration().orElseThrow();
        var missingUse=new Interactions.ResourceDeclaration(d.owner(),d.name(),d.classification(),d.nameSource(),d.objects(),List.of(new Interactions.ResourceUse(operation("absent",0),"output",ORIGIN)));
        invalid(resources(p,List.of(new Interactions.Resource(r.id(),r.description(),r.origin(),Optional.of(missingUse)))),"I-RB-01");
        var duplicatedUse=new Interactions.ResourceDeclaration(d.owner(),d.name(),d.classification(),d.nameSource(),d.objects(),List.of(d.uses().get(0),d.uses().get(0)));
        invalid(resources(p,List.of(new Interactions.Resource(r.id(),r.description(),r.origin(),Optional.of(duplicatedUse)))),"I-RB-02");
        var malformed=new Interactions.ResourceDeclaration(d.owner(),d.name(),"FD",d.nameSource(),d.objects(),d.uses());
        invalid(resources(p,List.of(new Interactions.Resource(r.id(),r.description(),r.origin(),Optional.of(malformed)))),"I-RB-03");
        var other=publication("A3");var foreign=new Interactions.ResourceDeclaration(unit("U1"),"F","cobol.fd","cobol.assignment-name",List.of(new Interactions.ResourceObject(object("U2","record"),"record")),List.of());
        invalid(resources(other,List.of(new Interactions.Resource(r.id(),r.description(),r.origin(),Optional.of(foreign)))),"I-RB-01");
        var unknown=new Interactions.Resource(r.id(),new Interactions.UnknownResource("file","cobol.external-file-name",GAP),ORIGIN,r.declaration());
        var qp=resources(p,List.of(unknown));require(qp.equals(new AirJson().decode(new AirJson().encode(qp))),"unknown descriptor preserves local uncertainty");
        invalid(resources(p,List.of(new Interactions.Resource(r.id(),new Interactions.UnknownResource("file","cobol.external-file-name",SIGNATURE),ORIGIN,r.declaration()))),"I-RB-03");
        var absent=new Interactions.Resource(r.id(),new Interactions.UnknownResource("file","cobol.external-file-name",new UncertaintyId(PUB,"absent")),ORIGIN,r.declaration());
        invalid(resources(p,List.of(absent)),"I-02");
        var tree=InvokeChecks.tree(p);
        for(String path:List.of("publication.resources.0.declaration","publication.resources.0.declaration.uses.0","publication.resources.0.declaration.objects.0")) {
            var value=(Json.Obj)InvokeChecks.at(tree,path);
            for(String field:value.fields().keySet()){
                var fields=new LinkedHashMap<>(value.fields());fields.remove(field);
                try{new AirJson().decode(InvokeChecks.wire(InvokeChecks.edit(tree,path,new Json.Obj(fields))));throw new AssertionError("omitted field accepted: "+path+"."+field);}catch(AirJsonException expected){require(expected.code()==AirJsonException.Code.INPUT_ERROR,"missing shape rejected");}
            }
        }
    }
    static void invalid(Publication p,String rule){var v=AirValidator.validate(p);require(v.issues().stream().anyMatch(i->i.kind()==ValidationIssue.Kind.INVALID_IR&&i.rule().equals(rule)),"expected "+rule+": "+v.issues());}
    public static void main(String[] args){run();wireOracle();adversarial();System.out.println("PASS FD-W1 A1/A2/A3/A4/A6 model/validator/codec and negative associations");}
}
