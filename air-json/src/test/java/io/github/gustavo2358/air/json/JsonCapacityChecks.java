package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.validation.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static io.github.gustavo2358.air.json.AirJsonException.Code.*;

/** Physical capacity is independent of supported AIR shapes; no new mapping oracle. */
public final class JsonCapacityChecks {
    private JsonCapacityChecks() {}
    static void physicalDepth() {
        for(int n:new int[]{512,1024,2048}) {
            byte[] bytes=("{\"x\":"+"[".repeat(n)+"null"+"]".repeat(n)+"}").getBytes(StandardCharsets.UTF_8);
            Json.Value tree=Json.parse(bytes,AirJson.Limits.defaults());
            require(Arrays.equals(bytes,Json.write(tree,AirJson.Limits.defaults())),"iterative physical roundtrip "+n);
            failure(INPUT_ERROR,()->new AirJson().decode(bytes)); // complete parse, unsupported envelope field
            var limited=new AirJson.Limits(bytes.length,n-1);
            failure(RESOURCE_LIMIT,()->Json.parse(bytes,limited));
            failure(RESOURCE_LIMIT,()->Json.write(tree,limited));
            byte[] malformed=("{\"x\":"+"[".repeat(n)+"null"+"]".repeat(n-1)+"}").getBytes(StandardCharsets.UTF_8);
            failure(INPUT_ERROR,()->Json.parse(malformed,AirJson.Limits.defaults()));
        }
    }
    static void byteBudgets() {
        byte[] expected="{\"x\":\"é😀\\n\\u0000\"}".getBytes(StandardCharsets.UTF_8);
        Json.Value value=Json.object("x","é😀\n\u0000");
        var exact=new AirJson.Limits(expected.length,1);
        require(Arrays.equals(expected,Json.write(value,exact)),"exact escaped UTF-8 count");
        Json.parse(expected,exact);
        var shortBudget=new AirJson.Limits(expected.length-1,1);
        failure(RESOURCE_LIMIT,()->Json.write(value,shortBudget));
        failure(RESOURCE_LIMIT,()->Json.parse(expected,shortBudget));
    }
    static void validationBudget() {
        var p=ScalarAssignOracle.publication();
        var codec=new AirJson(AirJson.Limits.defaults(),new ValidationOptions(128,1,1));
        byte[] bytes=new AirJson().encode(p);
        for(Runnable action:List.<Runnable>of(()->codec.encode(p),()->codec.decode(bytes))) {
            AirJsonException error=failure(RESOURCE_LIMIT,action);
            var result=error.validationResult().orElseThrow();
            require(result.status()==ValidationResult.Status.INCOMPLETE_VALIDATION,"exhaustion is incomplete");
            require(!result.diagnostics().traversalCompleted(),"traversal not complete");
            require(result.hasIssues(ValidationIssue.Kind.RESOURCE_LIMIT),"typed resource issue");
            require(!result.hasIssues(ValidationIssue.Kind.UNSUPPORTED_CAPABILITY),"resource is not capability");
        }
    }
    static void series() {
        for(int n:new int[]{16,32,64}) {
            roundtrip("objects",n,ScalarAssignOracle.publication(n,1));
            roundtrip("operations",n,ScalarAssignOracle.publication(1,n));
            roundtrip("bytes",n,textPublication(n*1024));
            var p=ScalarAssignOracle.publication(); var u=p.units().get(0); var original=u.sequences().get(0);
            List<Sequence> sequences=new ArrayList<>(); sequences.add(original);
            for(int i=1;i<n;i++) sequences.add(new Sequence(new LabelId(u.id(),"s"+i),List.of(),
                    new Operations.Return(ScalarAssignOracle.header(new OperationId(u.id(),"r"+i),"return"),List.of()),original.origin()));
            var many=new io.github.gustavo2358.air.model.Unit(u.id(),u.containingUnit(),u.objects(),u.visibleObjects(),u.entries(),sequences,u.completionPorts(),u.body(),u.bodyUnavailable(),u.coverage(),u.origin());
            roundtrip("sequences",n,new Publication(p.id(),p.airVersion(),p.capabilities(),p.artifacts(),List.of(many),p.storage(),p.resources(),p.artifactRelations(),p.origins(),p.coverage(),p.uncertainties(),p.premises()));
            var coverage=new Evidence.Coverage(Evidence.InventoryStatus.COMPLETE,new Scopes.EntityScope(Collections.nCopies(n,p.id())),List.of(),List.of());
            roundtrip("references",n,new Publication(p.id(),p.airVersion(),p.capabilities(),p.artifacts(),p.units(),p.storage(),p.resources(),p.artifactRelations(),p.origins(),coverage,p.uncertainties(),p.premises()));
        }
    }
    static Publication textPublication(int n) {
        var id=new PublicationId("capacity-json");
        return new Publication(id,SemanticVersion.AIR_2_0_0,new Capabilities.Manifest(List.of(),List.of()),
                List.of(new Origins.Artifact(new ArtifactId(id,"a"),"x".repeat(n),Optional.empty())),List.of(),List.of(),List.of(),List.of(),List.of(),
                new Evidence.Coverage(Evidence.InventoryStatus.COMPLETE,new Scopes.PublicationScope(id),List.of(),List.of()),List.of(),List.of());
    }
    static void roundtrip(String dimension,int n,Publication p) {
        var codec=new AirJson(); byte[] bytes=codec.encode(p); Publication decoded=codec.decode(bytes);
        require(p.equals(decoded),"CORE-SIZE full JSON publication "+dimension+" "+n);
        var r=AirValidator.validate(decoded);
        require(r.status()==ValidationResult.Status.STRUCTURALLY_VALID,"semantic classification");
        require(r.diagnostics().traversalCompleted(),"complete validation");
        System.out.println("CAPACITY dimension=json-"+dimension+" n="+n+" bytes="+bytes.length+" entities="+r.statistics().entities()+" operations="+r.statistics().operations()+" operands="+r.statistics().operands()+" queries="+r.statistics().domainQueries()+" status="+r.status());
    }
    static AirJsonException failure(AirJsonException.Code expected,Runnable action) {
        try { action.run(); } catch(AirJsonException error) {
            require(error.code()==expected,"expected "+expected+", got "+error.code()); return error;
        }
        throw new AssertionError("expected failure "+expected);
    }
    static void require(boolean test,String message) { if(!test) throw new AssertionError(message); }
    public static void main(String[] args) {
        if(args.length==0) { physicalDepth(); byteBudgets(); validationBudget(); series(); }
        else { int n=Integer.parseInt(args[0]); roundtrip("bytes",n,textPublication(n)); }
    }
}
