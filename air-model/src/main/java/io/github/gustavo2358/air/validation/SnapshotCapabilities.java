package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.AirShape;
import io.github.gustavo2358.air.model.AirSnapshot;
import io.github.gustavo2358.air.model.AirSnapshotBuilder;
import java.util.Objects;

import static io.github.gustavo2358.air.model.AirShape.*;

/** Exact required/name-policy capability catalogue without resident capability strings or sets. */
public final class SnapshotCapabilities implements AutoCloseable {
    public interface Storage extends AutoCloseable {
        boolean firstName(long list,long nameKey);
        void require(long nameKey,long versionKey);
        boolean required(long nameKey,long versionKey);
        void addNamePolicy(long nameKey,long versionKey);
        boolean hasNamePolicy(long nameKey,long versionKey);
        AirSnapshotBuilder.Lease claim(long bytes);
        @Override void close();
    }
    public enum Rule {
        DUPLICATE(ValidationIssue.Kind.INVALID_IR,"I-43"),
        PROFILE(ValidationIssue.Kind.SEMANTIC_OBLIGATION,"profile"),
        NAME_POLICY(ValidationIssue.Kind.SEMANTIC_OBLIGATION,"I-43"),
        UNSUPPORTED(ValidationIssue.Kind.UNSUPPORTED_CAPABILITY,"I-43"),
        MISSING(ValidationIssue.Kind.INVALID_IR,"I-43"),
        NAME_POLICY_SURFACE(ValidationIssue.Kind.UNSUPPORTED_CAPABILITY,"I-43");
        private final ValidationIssue.Kind kind;private final String code;
        Rule(ValidationIssue.Kind kind,String code){this.kind=kind;this.code=code;}
        public ValidationIssue.Kind kind(){return kind;}public String code(){return code;}
    }
    @FunctionalInterface public interface Issues {void report(Rule rule,long owner,long capability);}
    public record Counts(long required,long provided,long policies) { }
    private AirSnapshot snapshot;
    private SnapshotIdentityKeys keys;
    private Storage storage;
    private AirSnapshotBuilder.Lease control;
    private long required,provided,policies;
    private boolean failed;

    public static SnapshotCapabilities build(AirSnapshot snapshot,SnapshotIdentityKeys keys,Storage storage,Issues issues) {
        var result=new SnapshotCapabilities(snapshot,keys,storage);
        try{result.index(Objects.requireNonNull(issues));return result;}catch(RuntimeException|Error failure){result.closeSuppressed(failure);throw failure;}
    }
    private SnapshotCapabilities(AirSnapshot snapshot,SnapshotIdentityKeys keys,Storage storage) {
        this.snapshot=Objects.requireNonNull(snapshot);this.keys=Objects.requireNonNull(keys);this.storage=Objects.requireNonNull(storage);
        try{snapshot.root();control=Objects.requireNonNull(storage.claim(512));}
        catch(RuntimeException|Error failure){closeSuppressed(failure);throw failure;}
    }
    private void index(Issues issues) {
        long root=snapshot.root(),owner=snapshot.field(root,PUBLICATION,0),manifest=snapshot.field(root,PUBLICATION,2);
        collectPolicies(root);
        long requiredList=snapshot.field(manifest,CAPABILITIES_MANIFEST,0),providedList=snapshot.field(manifest,CAPABILITIES_MANIFEST,1);
        indexList(requiredList,true,owner,issues);indexList(providedList,false,owner,issues);
    }
    private void collectPolicies(long root) {
        try(var resources=snapshot.elements(snapshot.field(root,PUBLICATION,6),INTERACTIONS_RESOURCE)) {
            while(resources.advance())policyFromDescription(snapshot.field(resources.value(),INTERACTIONS_RESOURCE,1));
        }
        try(var units=snapshot.elements(snapshot.field(root,PUBLICATION,4),UNIT)) {
            while(units.advance())try(var sequences=snapshot.elements(snapshot.field(units.value(),UNIT,5),SEQUENCE)) {
                while(sequences.advance()) {
                    long operation=snapshot.field(sequences.value(),SEQUENCE,2);
                    if(snapshot.shape(operation)==OPERATIONS_INVOKE)policyFromDescription(snapshot.field(operation,OPERATIONS_INVOKE,2));
                }
            }
        }
    }
    private void policyFromDescription(long description) {
        AirShape shape=snapshot.shape(description);if(shape!=INTERACTIONS_LITERAL_TARGET&&shape!=INTERACTIONS_COMPUTED_TARGET&&shape!=INTERACTIONS_COMPUTED_RESOURCE)return;
        long policy=snapshot.field(description,shape,3);if(snapshot.shape(policy)!=INTERACTIONS_EXTENSION_NAME)return;
        storage.addNamePolicy(keys.atomKey(snapshot.field(policy,INTERACTIONS_EXTENSION_NAME,0)),keys.atomKey(snapshot.field(policy,INTERACTIONS_EXTENSION_NAME,1)));policies=Math.incrementExact(policies);
    }
    private void indexList(long list,boolean isRequired,long owner,Issues issues) {
        try(var rows=snapshot.elements(list,CAPABILITIES_CAPABILITY)) {
            while(rows.advance()) {
                long capability=rows.value(),name=snapshot.field(capability,CAPABILITIES_CAPABILITY,0),version=snapshot.field(capability,CAPABILITIES_CAPABILITY,1);
                long nameKey=keys.atomKey(name),versionKey=keys.atomKey(version);
                if(!storage.firstName(list,nameKey))issues.report(Rule.DUPLICATE,owner,capability);
                if(isRequired){storage.require(nameKey,versionKey);required=Math.incrementExact(required);}else provided=Math.incrementExact(provided);
                if(prefix(name,"AIR-"))issues.report(Rule.PROFILE,owner,capability);
                else if(storage.hasNamePolicy(nameKey,versionKey))issues.report(Rule.NAME_POLICY,owner,capability);
                else if(isRequired&&!standard(name,version))issues.report(Rule.UNSUPPORTED,owner,capability);
            }
        }
    }
    /** Require an extension surface; declared=false diagnoses missing required capability. */
    public boolean require(long name,long version,long owner,Issues issues) {
        open();Objects.requireNonNull(issues);
        try {
            long a=keys.atomKey(name),b=keys.atomKey(version);
            if(storage.hasNamePolicy(a,b)){issues.report(Rule.NAME_POLICY_SURFACE,owner,name);return false;}
            if(!storage.required(a,b)){issues.report(Rule.MISSING,owner,name);return false;}
            return true;
        }catch(RuntimeException|Error failure){failed=true;throw failure;}
    }
    public boolean required(String name,String version) {
        open();long root=snapshot.root(),manifest=snapshot.field(root,PUBLICATION,2),list=snapshot.field(manifest,CAPABILITIES_MANIFEST,0);
        try(var rows=snapshot.elements(list,CAPABILITIES_CAPABILITY)) {
            while(rows.advance()) {long cap=rows.value();if(equal(snapshot.field(cap,CAPABILITIES_CAPABILITY,0),name)&&equal(snapshot.field(cap,CAPABILITIES_CAPABILITY,1),version))return true;}
        }
        return false;
    }
    public boolean require(String name,String version,long owner,Issues issues) {
        open();Objects.requireNonNull(issues);
        long root=snapshot.root(),manifest=snapshot.field(root,PUBLICATION,2),list=snapshot.field(manifest,CAPABILITIES_MANIFEST,0),found=0;
        try(var rows=snapshot.elements(list,CAPABILITIES_CAPABILITY)) {
            while(rows.advance()) {long cap=rows.value();if(equal(snapshot.field(cap,CAPABILITIES_CAPABILITY,0),name)&&equal(snapshot.field(cap,CAPABILITIES_CAPABILITY,1),version)){found=cap;break;}}
        }
        if(found==0){issues.report(Rule.MISSING,owner,0);return false;}
        long a=keys.atomKey(snapshot.field(found,CAPABILITIES_CAPABILITY,0)),b=keys.atomKey(snapshot.field(found,CAPABILITIES_CAPABILITY,1));
        if(storage.hasNamePolicy(a,b)){issues.report(Rule.NAME_POLICY_SURFACE,owner,found);return false;}
        return true;
    }
    public Counts counts(){open();return new Counts(required,provided,policies);}
    private boolean standard(long name,long version) {
        if(equal(name,"entry.possibilities"))return equal(version,"1")||equal(version,"2");
        if(!equal(version,"1"))return false;
        return equal(name,"resource.bindings")||equal(name,"target.possibilities")
            ||equal(name,"memory.regions")||equal(name,"text.ebcdic.ibm1047")||equal(name,"control.local.boundary_routes")
            ||equal(name,"control.local.resume_routes")||equal(name,"control.local.unwind_all")
            ||equal(name,"control.local.reentry_guard")||equal(name,"control.local")||equal(name,"control.indirect");
    }
    private boolean prefix(long source,String value){if(snapshot.characterCount(source)<value.length())return false;char[] chars=value.toCharArray(),actual=new char[chars.length];snapshot.readCharacters(source,0,actual,0,actual.length);return java.util.Arrays.equals(chars,actual);}
    private boolean equal(long source,String value){if(snapshot.characterCount(source)!=value.length())return false;char[] chars=value.toCharArray(),actual=new char[chars.length];if(actual.length!=0)snapshot.readCharacters(source,0,actual,0,actual.length);return java.util.Arrays.equals(chars,actual);}
    private void open(){if(storage==null||failed)throw new IllegalStateException("capability catalogue is closed or aborted");}
    private void closeSuppressed(Throwable failure){try{close();}catch(RuntimeException|Error cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}}
    @Override public void close(){Storage owner=storage;storage=null;AirSnapshotBuilder.Lease lease=control;control=null;snapshot=null;keys=null;Throwable failure=null;try{if(owner!=null)owner.close();}catch(RuntimeException|Error cleanup){failure=cleanup;}try{if(lease!=null)lease.close();}catch(RuntimeException|Error cleanup){if(failure==null)failure=cleanup;else if(failure!=cleanup)failure.addSuppressed(cleanup);}if(failure instanceof RuntimeException error)throw error;if(failure instanceof Error error)throw error;}
}
