package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.validation.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.*;
import static io.github.gustavo2358.air.json.ScalarAssignOracle.*;
import static io.github.gustavo2358.air.json.AirJsonException.Code.*;

/** Independent field/identity oracles for AIR 05.7 and JSON 7/10. No execution interpreter. */
final class LocalControlChecks {
    private LocalControlChecks() { }
    private static final AirJson CODEC = new AirJson();
    private static final CompletionPortId PORT = new CompletionPortId(UNIT,"completed/α");
    private static final CompletionPortId UNUSED = new CompletionPortId(UNIT,"other-port");
    private static final BigInteger HUGE = BigInteger.ONE.shiftLeft(100).add(BigInteger.ONE);
    private static final String BASE = "publication.units.0.";
    private static LabelId label(String local) { return new LabelId(UNIT,local); }
    private static Operations.Header h(String name) { return header(new OperationId(UNIT,name),"sequence"); }
    private static Sequence seq(String name,Terminator operation) {
        return new Sequence(label(name),List.of(),operation,origin("sequence"));
    }
    private static Envelopes.Envelope fallback() {
        var memory = new Scopes.WithinMemory(new Scopes.ObjectsMemory(List.of(OBJECT)));
        return new Envelopes.Envelope(new Envelopes.MemoryEnvelope(List.of(),memory,List.of(),memory,List.of()),
            new Control.ControlEnvelope(List.of(new Control.JumpAlternative(label("ordinary"))),new Scopes.WithinControl(new Scopes.AllControl(PUB))),
            new Envelopes.DependencyEnvelope(List.of(),Scopes.AnyResource.INSTANCE));
    }
    static Publication fixture() {
        var p=publication();var u=p.units().getFirst();var base=u.sequences().getFirst();
        var sequences=List.of(
            new Sequence(base.label(),base.instructions(),new Operations.LocalInvoke(h("call-a"),label("shared"),List.of(PORT),label("call-b"),fallback()),base.origin()),
            seq("call-b",new Operations.LocalInvoke(h("call-b"),label("shared"),List.of(PORT,UNUSED),label("ordinary"),fallback())),
            seq("shared",new Operations.LocalBoundary(h("boundary-a"),PORT,label("second-boundary"),fallback())),
            seq("second-boundary",new Operations.LocalBoundary(h("boundary-b"),PORT,label("ordinary"),fallback())),
            seq("ordinary",base.terminator()),
            seq("resume",new Operations.LocalResume(h("resume"),fallback())),
            seq("unwind",new Operations.LocalUnwind(h("unwind"),HUGE,label("ordinary"),fallback())));
        var changed=new Unit(u.id(),u.containingUnit(),u.objects(),u.visibleObjects(),u.entries(),sequences,
            List.of(new Entries.CompletionPort(PORT,origin("data")),new Entries.CompletionPort(UNUSED,origin("sequence"))),
            u.body(),u.bodyUnavailable(),u.coverage(),u.origin());
        return withUnit(p,changed,new Capabilities.Manifest(List.of(Capabilities.LOCAL_CONTROL),List.of(Capabilities.LOCAL_CONTROL)));
    }
    private static Publication withUnit(Publication p,Unit unit,Capabilities.Manifest manifest) {
        return new Publication(p.id(),p.airVersion(),manifest,p.artifacts(),List.of(unit),p.storage(),p.resources(),
            p.artifactRelations(),p.origins(),p.coverage(),p.uncertainties(),p.premises());
    }
    private static Publication withSequences(Publication p,List<Sequence> sequences) {
        var u=p.units().getFirst();return withUnit(p,new Unit(u.id(),u.containingUnit(),u.objects(),u.visibleObjects(),u.entries(),
            sequences,u.completionPorts(),u.body(),u.bodyUnavailable(),u.coverage(),u.origin()),p.capabilities());
    }
    private static byte[] golden() {
        try { return Files.readAllBytes(Path.of("src/test/resources/local-control.canonical.json")); }
        catch(java.io.IOException failure) { throw new AssertionError(failure); }
    }
    static void wireOracle() {
        var p=fixture();var expected=golden();
        equal(ValidationResult.Status.STRUCTURALLY_VALID,AirValidator.validate(p).status());
        equal(p,CODEC.decode(expected));
        bytes(expected,CODEC.encode(p));
        bytes(expected,CODEC.encode(CODEC.decode(expected)));
        // Distinct calls refer to the same body; neither destinations nor ports are normalized.
        var sequences=CODEC.decode(expected).units().getFirst().sequences();
        var a=(Operations.LocalInvoke)sequences.get(0).terminator();var b=(Operations.LocalInvoke)sequences.get(1).terminator();
        equal(a.entry(),b.entry());require(!a.resume().equals(b.resume()),"call resumes merged");
        require(!a.header().id().equals(b.header().id()),"call operations merged");
        var x=(Operations.LocalBoundary)sequences.get(2).terminator();var y=(Operations.LocalBoundary)sequences.get(3).terminator();
        equal(x.port(),y.port());require(!x.defaultDestination().equals(y.defaultDestination()),"boundary defaults merged");
        equal(HUGE,((Operations.LocalUnwind)sequences.get(6).terminator()).count());
        equal(AirValidator.validate(p),AirValidator.validate(CODEC.decode(expected)));
    }
    static void roundTrips() {
        var p=fixture();roundTrip(p);
        var reversed=new ArrayList<>(p.units().getFirst().sequences());Collections.reverse(reversed);roundTrip(withSequences(p,reversed));
        // Empty port sets and cyclic references are legal transport, not a bounded stack policy.
        var sequences=new ArrayList<>(p.units().getFirst().sequences());
        var first=sequences.getFirst();var old=(Operations.LocalInvoke)first.terminator();
        for(var ports:List.of(List.<CompletionPortId>of(),List.of(UNUSED,PORT))) {
            sequences.set(0,new Sequence(first.label(),first.instructions(),new Operations.LocalInvoke(old.header(),first.label(),ports,first.label(),old.fallback()),first.origin()));
            roundTrip(withSequences(p,sequences));
        }
        for(var count:List.of(BigInteger.ZERO,BigInteger.ONE,HUGE)) {
            sequences.set(6,seq("unwind",new Operations.LocalUnwind(h("unwind"),count,label("ordinary"),fallback())));
            roundTrip(withSequences(p,sequences));
        }
        var u=p.units().getFirst();var ports=new ArrayList<>(u.completionPorts());Collections.reverse(ports);
        roundTrip(withUnit(p,new Unit(u.id(),u.containingUnit(),u.objects(),u.visibleObjects(),u.entries(),u.sequences(),ports,
            u.body(),u.bodyUnavailable(),u.coverage(),u.origin()),p.capabilities()));
        var partial=CODEC.decodeForPartialAnalysis(golden());equal(p,partial.publication());
        equal(AirValidator.validate(p),partial.validation());bytes(golden(),CODEC.encodeForPartialAnalysis(p).bytes());
    }
    static void closureAndCapabilities() {
        var p=fixture();var tree=Json.parse(golden(),AirJson.Limits.defaults());
        reject(INVALID_IR,change(tree,BASE+"completionPorts",new Json.Arr(List.of())),"I-02");
        var ports=(Json.Arr)at(tree,BASE+"completionPorts");
        reject(INVALID_IR,change(tree,BASE+"completionPorts",new Json.Arr(List.of(ports.values().getFirst(),ports.values().getFirst()))),"I-01");
        for(var path:List.of(BASE+"sequences.0.terminator.entry",BASE+"sequences.0.terminator.resume",BASE+"sequences.2.terminator.defaultDestination",BASE+"sequences.6.terminator.destination")) {
            reject(INVALID_IR,change(tree,path+".localId",Json.value("absent-label")),"I-02");
            reject(INVALID_IR,change(tree,path+".unit",Json.value("foreign-unit")),null);
            reject(INVALID_IR,change(tree,path+".domain",Json.value("completion_port")),null);
        }
        for(var path:List.of(BASE+"sequences.0.terminator.completionPorts.0",BASE+"sequences.2.terminator.port")) {
            reject(INVALID_IR,change(tree,path+".localId",Json.value("absent-port")),"I-02");
            reject(INVALID_IR,change(tree,path+".unit",Json.value("foreign-unit")),null);
            reject(INVALID_IR,change(tree,path+".domain",Json.value("label")),null);
        }
        reject(INVALID_IR,change(tree,BASE+"completionPorts.0.id.unit",Json.value("foreign-unit")),null);
        reject(INVALID_IR,change(tree,BASE+"completionPorts.0.origin.localId",Json.value("absent-origin")),"I-02");
        reject(INVALID_IR,change(tree,"publication.capabilities.required",new Json.Arr(List.of())),"I-43");
        for(var field:List.of("required","provided")) {
            reject(UNSUPPORTED_CAPABILITY,change(tree,"publication.capabilities."+field+".0.version",Json.value("2")),null);
            reject(UNSUPPORTED_CAPABILITY,change(tree,"publication.capabilities."+field+".0.name",Json.value("control.indirect")),null);
        }
        // In-memory writer is subject to the same actual AIR checks.
        expect(INVALID_IR,()->CODEC.encode(withUnit(p,p.units().getFirst(),new Capabilities.Manifest(List.of(),List.of()))),"I-43");
        var seqs=new ArrayList<>(p.units().getFirst().sequences());var s=seqs.get(2);
        seqs.set(2,seq("shared",new Operations.LocalBoundary(s.terminator().header(),new CompletionPortId(UNIT,"absent-port"),label("ordinary"),fallback())));
        expect(INVALID_IR,()->CODEC.encode(withSequences(p,seqs)),"I-02");
    }
    static void malformedAndLimits() {
        var tree=Json.parse(golden(),AirJson.Limits.defaults());
        for(var count:List.of("-1","-0","+1","01","1.0","1e2",""))
            reject(INPUT_ERROR,change(tree,BASE+"sequences.6.terminator.count",Json.value(count)),null);
        for(int n:new int[]{0,2,5,6}) {
            String op=BASE+"sequences."+n+".terminator";
            var fields=new LinkedHashMap<>(((Json.Obj)at(tree,op)).fields());fields.remove("fallback");
            reject(INPUT_ERROR,change(tree,op,new Json.Obj(fields)),null);
            fields=new LinkedHashMap<>(((Json.Obj)at(tree,op)).fields());fields.put("inferredResume",Json.value("ordinary"));
            reject(INPUT_ERROR,change(tree,op,new Json.Obj(fields)),null);
            reject(INPUT_ERROR,change(tree,op+".fallback",Json.Nil.INSTANCE),null);
            reject(INVALID_IR,change(tree,BASE+"sequences.4.instructions",new Json.Arr(List.of(at(tree,op)))),"I-04");
        }
        var fields=new LinkedHashMap<>(((Json.Obj)at(tree,BASE+"completionPorts.0")).fields());fields.remove("origin");
        reject(INPUT_ERROR,change(tree,BASE+"completionPorts.0",new Json.Obj(fields)),null);
        // All nested fallback facts are parsed and validated; no opaque JSON escape hatch.
        reject(INPUT_ERROR,change(tree,BASE+"sequences.5.terminator.fallback.control.remainder.kind",Json.value("unknown-kind")),null);
        reject(INVALID_IR,change(tree,BASE+"sequences.0.terminator.fallback.memory.otherWrites.scope.objects.0.localId",Json.value("absent-object")),"I-02");
        var limited=new AirJson(new AirJson.Limits(10,128),ValidationOptions.defaults());
        expect(RESOURCE_LIMIT,()->limited.decode(golden()),null);expect(RESOURCE_LIMIT,()->limited.encode(fixture()),null);
    }
    private static void roundTrip(Publication p) {var wire=CODEC.encode(p);equal(p,CODEC.decode(wire));bytes(wire,CODEC.encode(CODEC.decode(wire)));}
    private static Json.Value at(Json.Value node,String path) {
        for(String part:path.split("\\."))node=node instanceof Json.Obj o?o.fields().get(part):((Json.Arr)node).values().get(Integer.parseInt(part));return node;
    }
    private static Json.Value change(Json.Value node,String path,Json.Value value) {
        String[] parts=path.split("\\.",2);String key=parts[0];
        if(node instanceof Json.Obj o) {var fields=new LinkedHashMap<>(o.fields());fields.put(key,parts.length==1?value:change(fields.get(key),parts[1],value));return new Json.Obj(fields);}
        var values=new ArrayList<>(((Json.Arr)node).values());int i=Integer.parseInt(key);values.set(i,parts.length==1?value:change(values.get(i),parts[1],value));return new Json.Arr(values);
    }
    private static void reject(AirJsonException.Code code,Json.Value tree,String rule) {expect(code,()->CODEC.decode(Json.write(tree,AirJson.Limits.defaults())),rule);}
    private static void expect(AirJsonException.Code code,Runnable action,String rule) {
        try {action.run();throw new AssertionError("expected "+code+" / "+rule);}catch(AirJsonException failure) {
            equal(code,failure.code());if(rule!=null)require(failure.issues().stream().anyMatch(i->i.rule().equals(rule)),"missing "+rule+": "+failure.issues());
        }
    }
    private static void bytes(byte[] expected,byte[] actual) {require(Arrays.equals(expected,actual),"wire differs from independent oracle");}
    private static void equal(Object expected,Object actual) {require(Objects.equals(expected,actual),"expected "+expected+", got "+actual);}
    private static void require(boolean yes,String message) {if(!yes)throw new AssertionError(message);}
}
