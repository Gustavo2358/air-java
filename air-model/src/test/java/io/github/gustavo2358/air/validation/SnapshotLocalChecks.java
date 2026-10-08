package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import java.util.*;
import static io.github.gustavo2358.air.model.AirShape.*;
import static io.github.gustavo2358.air.validation.SnapshotLocalConstraints.Rule.*;

/** Manual constructor oracles; scalar/index laws do not assert full AIR validity. */
final class SnapshotLocalChecks {
    private SnapshotLocalChecks() { }
    private record TextField(AirShape shape,int slot) { }
    static void fieldSpecificTextAndNumberRules() {
        var s=new SnapshotGraphChecks.Source();var f=new SnapshotGraphChecks.Fixture(s);
        var fields=new ArrayList<TextField>();
        for(var shape: new AirShape[]{IDS_PUBLICATION_ID,IDS_UNIT_ID,IDS_OBJECT_ID,IDS_STORAGE_ID,IDS_LABEL_ID,IDS_ENTRY_ID,IDS_COMPLETION_PORT_ID,IDS_OPERATION_ID,IDS_OPERAND_ID,IDS_ORIGIN_ID,IDS_UNCERTAINTY_ID,IDS_PREMISE_ID,IDS_ARTIFACT_ID,IDS_RESOURCE_ID,IDS_ARTIFACT_RELATION_ID})
            fields.add(new TextField(shape,shape==IDS_PUBLICATION_ID?0:1));
        add(fields,CAPABILITIES_CAPABILITY,0,1);add(fields,TYPES_EXTENSION_TYPE,0,1);add(fields,MEMORY_EXTENSION_CODEC,0,1);add(fields,INTERACTIONS_EXTENSION_NAME,0,1);
        add(fields,ARTIFACTS_RELATION,3);add(fields,CONTROL_EXCEPTIONAL,0);add(fields,CONTROL_EXCEPTION_OUTCOME,0);add(fields,OPERATIONS_RAISE,1);
        add(fields,EVIDENCE_UNCERTAINTY,1,4);add(fields,EVIDENCE_ELIMINATION,0);add(fields,EVIDENCE_COVERAGE_ITEM,0);
        add(fields,INTERACTIONS_LITERAL_TARGET,0,1);add(fields,INTERACTIONS_COMPUTED_TARGET,0,1);add(fields,INTERACTIONS_COMPUTED_RESOURCE,0,1);add(fields,INTERACTIONS_UNKNOWN_RESOURCE,0,1);
        add(fields,INTERACTIONS_LOCAL_RESOURCE,0);add(fields,INTERACTIONS_RESOURCE_OBJECT,1);add(fields,INTERACTIONS_RESOURCE_USE,1);add(fields,INTERACTIONS_RESOURCE_DECLARATION,1,2,3);
        add(fields,INTERACTIONS_CONTRACT_REF,0,1);add(fields,ENVELOPES_RESOURCE_USE,0);add(fields,OPERATIONS_INVOKE,1);add(fields,OPERATIONS_OPAQUE,1);add(fields,OPERATIONS_REENTRY_GUARD,0);add(fields,OPERATIONS_RESUME_ROUTE,0);
        add(fields,ORIGINS_OFFSETS,2);add(fields,ORIGINS_INCLUDE_FRAME,2);add(fields,ORIGINS_DERIVED,2);add(fields,ORIGINS_CONTRACTUAL,1,2);add(fields,ORIGINS_UNAVAILABLE,1);add(fields,ORIGINS_ARTIFACT,1);add(fields,PROOFS_PREMISE,1,2);
        try(var snapshot=AirSnapshot.attach(s,f.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var check=new SnapshotLocalConstraints(snapshot,keys,new Store())) {
            for(var field:fields)for(String text:new String[]{"", " \t\n\u2003", "x\uD800"}) {
                long node=record(s,field.shape());s.replaceField(node,field.slot(),s.text(text));
                rejected(check,node,field.shape(),text.endsWith("\uD800")?UNICODE:NONBLANK);
            }
            for(var field:fields) {
                long node=record(s,field.shape());s.replaceField(node,field.slot(),s.text("\u00A0\uD83D\uDE00"));accepted(check,node,field.shape());
            }
            long pad=record(s,EXPRESSIONS_FIT_TEXT);s.replaceField(pad,2,s.integer("0"));s.replaceField(pad,3,s.text("\uD83D\uDE00"));accepted(check,pad,EXPRESSIONS_FIT_TEXT);
            s.replaceField(pad,3,s.text(""));rejected(check,pad,EXPRESSIONS_FIT_TEXT,ONE_SCALAR);
            s.replaceField(pad,3,s.text("ab"));rejected(check,pad,EXPRESSIONS_FIT_TEXT,ONE_SCALAR);
            for(var shape:new AirShape[]{VALUES_TEXT_VALUE,EXPRESSIONS_TRIM_RIGHT}) {
                int slot=shape==VALUES_TEXT_VALUE?0:2;long node=record(s,shape);s.replaceField(node,slot,s.text(""));accepted(check,node,shape);
                s.replaceField(node,slot,s.text("\uDC00"));rejected(check,node,shape,UNICODE);
            }
            for(var shape:new AirShape[]{OPERATIONS_LOCAL_RESUME,OPERATIONS_LOCAL_BOUNDARY}) {
                int slot=shape==OPERATIONS_LOCAL_RESUME?2:4;long node=record(s,shape);s.replaceField(node,slot,s.optional());accepted(check,node,shape);
                s.replaceField(node,slot,s.optional(s.text(" \t")));rejected(check,node,shape,NONBLANK);
            }
            // Legacy constructors deliberately impose no content rule on these fields/categories.
            long artifact=record(s,ORIGINS_ARTIFACT);s.replaceField(artifact,2,s.optional(s.text("\uD800")));accepted(check,artifact,ORIGINS_ARTIFACT);
            long object=record(s,MEMORY_OBJECT_DECLARATION);s.replaceField(object,1,s.optional(s.text("")));accepted(check,object,MEMORY_OBJECT_DECLARATION);
            long categories=s.record(SCOPES_RESOURCE_CATEGORIES,s.list(s.text(""),s.text("\uD800")));accepted(check,categories,SCOPES_RESOURCE_CATEGORIES);
            var nonnegative=new TextField[]{new TextField(SEMANTIC_VERSION,0),new TextField(SEMANTIC_VERSION,1),new TextField(SEMANTIC_VERSION,2),new TextField(VALUES_DECIMAL_VALUE,1),new TextField(EXPRESSIONS_QUANTIZE,2),new TextField(EXPRESSIONS_FILL_TEXT,2),new TextField(EXPRESSIONS_FIT_TEXT,2),new TextField(OPERATIONS_COPY_BYTES,3),new TextField(OPERATIONS_LOCAL_UNWIND,1),new TextField(ENTRIES_PARAMETER_INITIAL,0),new TextField(INTERACTIONS_PARAMETER,0),new TextField(INTERACTIONS_RESULT_SLOT,0),new TextField(PROOFS_PARAMETER_DOMAIN,1),new TextField(PROOFS_RESULT_DOMAIN,1),new TextField(PROOFS_CALL_PARAMETER_DOMAIN,2),new TextField(PROOFS_CALL_RESULT_DOMAIN,2),new TextField(PROOFS_EXTERNAL_PARAMETER_DOMAIN,1),new TextField(PROOFS_EXTERNAL_RESULT_DOMAIN,1),new TextField(ORIGINS_POSITION,0),new TextField(ORIGINS_POSITION,1),new TextField(MEMORY_VIEW_BINDING,1),new TextField(MEMORY_VIEW_BINDING,2)};
            for(var field:nonnegative) {
                long node=record(s,field.shape());s.replaceField(node,field.slot(),s.integer("-"+"9".repeat(4096)));rejected(check,node,field.shape(),NONNEGATIVE);
                s.replaceField(node,field.slot(),s.integer("0"));accepted(check,node,field.shape());
            }
            for(var shape:new AirShape[]{EXPRESSIONS_WRAP_INTEGER,EXPRESSIONS_FIT_DECIMAL,EXPRESSIONS_INTEGER_DIGITS}) {
                long node=record(s,shape);s.replaceField(node,2,s.integer("0"));rejected(check,node,shape,POSITIVE);
                s.replaceField(node,2,s.integer("1"+"0".repeat(4096)));accepted(check,node,shape);
                if(shape==EXPRESSIONS_FIT_DECIMAL){s.replaceField(node,3,s.integer("-999999"));accepted(check,node,shape);}
            }
            long binary=record(s,MEMORY_BINARY_CODEC);s.replaceField(binary,1,s.integer("0"));rejected(check,binary,MEMORY_BINARY_CODEC,POSITIVE);
            s.replaceField(binary,1,s.integer("9"));rejected(check,binary,MEMORY_BINARY_CODEC,MULTIPLE_EIGHT);
            s.replaceField(binary,1,s.integer("1"+"0".repeat(4096)));accepted(check,binary,MEMORY_BINARY_CODEC);
        }
    }
    static void conditionalCollectionsAndExactCoordinates() {
        var s=new SnapshotGraphChecks.Source();var f=new SnapshotGraphChecks.Fixture(s);
        try(var snapshot=AirSnapshot.attach(s,f.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var check=new SnapshotLocalConstraints(snapshot,keys,new Store())) {
            for(var shape:new AirShape[]{SCOPES_ENTITY_SCOPE,SCOPES_OBJECTS_MEMORY,SCOPES_STORAGE_MEMORY,SCOPES_MEMORY_UNION,ENTRIES_POSSIBLE_LITERALS,ORIGINS_DERIVED,EVIDENCE_UNCERTAINTY,INTERACTIONS_CONTRACT_REF}) {
                int slot=shape==ORIGINS_DERIVED?1:shape==EVIDENCE_UNCERTAINTY||shape==INTERACTIONS_CONTRACT_REF?2:0;
                long node=record(s,shape);s.replaceField(node,slot,s.list());rejected(check,node,shape,NONEMPTY);
            }
            for(var shape:new AirShape[]{CONTROL_INVOCATION_OUTCOMES,CONTROL_CONTROL_ENVELOPE,MEMORY_ALTERNATIVES_BINDING,PLACES_CHOICE}) {
                int slot=shape==PLACES_CHOICE?1:0;long node=record(s,shape);s.replaceField(node,slot,s.list());s.replaceField(node,slot+1,s.scalar(shape==CONTROL_INVOCATION_OUTCOMES||shape==CONTROL_CONTROL_ENVELOPE?SCOPES_NO_CONTROL:SCOPES_NO_MEMORY,0));
                rejected(check,node,shape,CLOSED_EMPTY);
                long remainder=shape==CONTROL_INVOCATION_OUTCOMES||shape==CONTROL_CONTROL_ENVELOPE?s.record(SCOPES_WITHIN_CONTROL):s.record(SCOPES_WITHIN_MEMORY);
                s.replaceField(node,slot+1,remainder);accepted(check,node,shape);
            }
            long storage=record(s,MEMORY_STORAGE_HEADER);s.replaceField(storage,1,s.optional());s.replaceField(storage,2,s.scalar(MEMORY_LIFETIME,0));rejected(check,storage,MEMORY_STORAGE_HEADER,ACTIVATION_OWNER);
            s.replaceField(storage,2,s.scalar(MEMORY_LIFETIME,1));accepted(check,storage,MEMORY_STORAGE_HEADER);
            long region=record(s,MEMORY_REGION);s.replaceField(region,1,s.optional());s.replaceField(region,2,s.optional());rejected(check,region,MEMORY_REGION,EXTENT_EXCLUSIVE);
            s.replaceField(region,1,s.optional(s.integer("0")));accepted(check,region,MEMORY_REGION);
            s.replaceField(region,2,s.optional(placeholder(s,IDS_UNCERTAINTY_ID)));rejected(check,region,MEMORY_REGION,EXTENT_EXCLUSIVE);s.replaceField(region,1,s.optional());accepted(check,region,MEMORY_REGION);s.replaceField(region,2,s.optional());
            s.replaceField(region,1,s.optional(s.integer("-1")));rejected(check,region,MEMORY_REGION,NONNEGATIVE);
            long unit=record(s,UNIT);s.replaceField(unit,4,s.list());s.replaceField(unit,5,s.list());s.replaceField(unit,7,s.scalar(UNIT_BODY_AVAILABILITY,0));s.replaceField(unit,8,s.optional());rejected(check,unit,UNIT,UNIT_BODY);
            s.replaceField(unit,7,s.scalar(UNIT_BODY_AVAILABILITY,1));rejected(check,unit,UNIT,UNIT_BODY);s.replaceField(unit,8,s.optional(placeholder(s,IDS_UNCERTAINTY_ID)));accepted(check,unit,UNIT);
            long offset=record(s,ORIGINS_OFFSETS);s.replaceField(offset,0,s.integer("1"+"0".repeat(4096)+"3"));s.replaceField(offset,1,s.integer("1"+"0".repeat(4096)+"2"));rejected(check,offset,ORIGINS_OFFSETS,ORDER);
            s.replaceField(offset,1,s.integer("2"+"0".repeat(4097)));accepted(check,offset,ORIGINS_OFFSETS);
            long start=s.record(ORIGINS_POSITION,s.integer("1"),s.integer("2")),end=s.record(ORIGINS_POSITION,s.integer("1"),s.integer("1"));
            long span=s.record(ORIGINS_SPAN,start,end,s.integer("1"),s.integer("1"),s.scalar(ORIGINS_COLUMN_UNIT,0),s.scalar(BOOLEAN,1));rejected(check,span,ORIGINS_SPAN,ORDER);
            s.replaceField(end,0,s.integer("2"));accepted(check,span,ORIGINS_SPAN);s.replaceField(span,2,s.integer("2"));rejected(check,span,ORIGINS_SPAN,BASE);
            s.replaceField(span,2,s.integer("1"));s.replaceField(start,1,s.integer("0"));rejected(check,span,ORIGINS_SPAN,COORDINATE);
            for(int octet:new int[]{-1,256,Integer.MIN_VALUE,Integer.MAX_VALUE}) {
                long bytes=s.record(VALUES_BYTES_VALUE,s.list(s.scalar(SMALL_INTEGER,0),s.scalar(SMALL_INTEGER,octet)));rejected(check,bytes,VALUES_BYTES_VALUE,OCTET);
            }
            long bytes=s.record(VALUES_BYTES_VALUE,s.list(s.scalar(SMALL_INTEGER,0),s.scalar(SMALL_INTEGER,255)));accepted(check,bytes,VALUES_BYTES_VALUE);
        }
        eq(0,s.activeCursors);eq(0,s.indexedListReads);
    }
    static void decimalPartsAndFormatsWithoutExpandedArithmetic() {
        var s=new SnapshotGraphChecks.Source();var f=new SnapshotGraphChecks.Fixture(s);
        try(var snapshot=AirSnapshot.attach(s,f.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var check=new SnapshotLocalConstraints(snapshot,keys,new Store())) {
            String[] valid={"","","","x",".","++","+"},negative={"","","","","","--","-"};
            for(int kind=0;kind<7;kind++) {
                long part=part(s,kind,"1",valid[kind],negative[kind]);accepted(check,part,DECIMAL_TEXT_PART);
                s.replaceField(part,1,s.integer("0"));rejected(check,part,DECIMAL_TEXT_PART,POSITIVE);
                s.replaceField(part,1,s.integer("1"));s.replaceField(part,2,s.text(kind<3?"x":""));rejected(check,part,DECIMAL_TEXT_PART,DECIMAL_PART);
                if(kind>=4){long counted=part(s,kind,"2",valid[kind],negative[kind]);rejected(check,counted,DECIMAL_TEXT_PART,DECIMAL_PART);}
            }
            long huge=part(s,0,"1"+"0".repeat(100_000),"","");accepted(check,huge,DECIMAL_TEXT_PART);
            long radix=part(s,4,"1",".",""),space=part(s,1,"1","",""),star=part(s,2,"1","",""),floating=part(s,6,"1","+","-");
            for(long[] parts:new long[][]{{},{radix},{huge,radix,radix},{huge,floating,floating},{star,space},{star,floating}}) {
                long node=record(s,EXPRESSIONS_FORMAT_DECIMAL);s.replaceField(node,2,s.list(parts));rejected(check,node,EXPRESSIONS_FORMAT_DECIMAL,DECIMAL_FORMAT);
            }
            long format=record(s,EXPRESSIONS_FORMAT_DECIMAL);s.replaceField(format,2,s.list(huge,radix,space,floating));accepted(check,format,EXPRESSIONS_FORMAT_DECIMAL);
        }
    }
    static void labelMembershipAndSharedFoldCost() {
        for(int n:new int[]{16,64,256,1024,4096}) {
            var s=new SnapshotGraphChecks.Source();var f=new SnapshotGraphChecks.Fixture(s);long u=s.record(IDS_UNIT_ID,f.id,s.text("u"));long[] labels=new long[n];
            for(int i=0;i<n;i++)labels[i]=s.record(IDS_LABEL_ID,u,s.text("L"+i));
            long list=s.list(labels),domain=s.record(TYPES_LABEL_TYPE,u,list);var port=new Store();
            try(var snapshot=AirSnapshot.attach(s,f.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var check=new SnapshotLocalConstraints(snapshot,keys,port)) {
                long value=s.record(VALUES_LABEL_VALUE,labels[n-1],domain);int cursors=s.cursors;
                for(int i=0;i<n;i++){accepted(check,value,VALUES_LABEL_VALUE);accepted(check,domain,TYPES_LABEL_TYPE);}
                long outside=s.record(IDS_LABEL_ID,u,s.text("outside"));s.replaceField(value,0,outside);rejected(check,value,VALUES_LABEL_VALUE,LABEL_MEMBER);
                eq(1,s.cursors-cursors);eq((long)n,port.adds);eq((long)n+1,port.contains);
                System.out.println("SNAPSHOT_LOCAL_MEMBERSHIP_COST labels="+n+" queries="+n+" indexed="+port.adds+" cursors="+(s.cursors-cursors));
            }
            eq(0L,port.claimed);eq(true,port.closed);eq(0,s.indexedListReads);
        }
        var s=new SnapshotGraphChecks.Source();var f=new SnapshotGraphChecks.Fixture(s);long u=s.record(IDS_UNIT_ID,f.id,s.text("u"));
        long a=s.record(IDS_LABEL_ID,u,s.text("a")),same=s.record(IDS_LABEL_ID,s.record(IDS_UNIT_ID,s.record(IDS_PUBLICATION_ID,s.text("P")),s.text("u")),s.text("a"));
        try(var snapshot=AirSnapshot.attach(s,f.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var check=new SnapshotLocalConstraints(snapshot,keys,new Store())) {
            long domain=s.record(TYPES_LABEL_TYPE,u,s.list(a,same));rejected(check,domain,TYPES_LABEL_TYPE,LABEL_DUPLICATE);
        }
        var input=new SnapshotGraphChecks.Source();var fixture=new SnapshotGraphChecks.Fixture(input);var storage=new Store();
        try(var snapshot=AirSnapshot.attach(input,fixture.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var check=new SnapshotLocalConstraints(snapshot,keys,storage)) {
            long[] parts=new long[100_000];Arrays.fill(parts,part(input,0,"1","",""));long list=input.list(parts);
            long format=record(input,EXPRESSIONS_FORMAT_DECIMAL);input.replaceField(format,2,list);int cursors=input.cursors;
            for(int i=0;i<4096;i++)accepted(check,format,EXPRESSIONS_FORMAT_DECIMAL);eq(1,input.cursors-cursors);
            long bytes=input.record(VALUES_BYTES_VALUE,input.list(input.scalar(SMALL_INTEGER,255)));cursors=input.cursors;
            for(int i=0;i<4096;i++)accepted(check,bytes,VALUES_BYTES_VALUE);eq(1,input.cursors-cursors);
        }
    }
    static void wholeGraphDeadContentAndFailureOwnership() {
        var s=new SnapshotGraphChecks.Source();var f=new SnapshotGraphChecks.Fixture(s);long cap=s.record(CAPABILITIES_CAPABILITY,s.text(" "),s.text("1"));long manifest=s.child(f.root,2);s.replaceField(manifest,0,s.list(cap));var port=new Store();var walk=new SnapshotGraphChecks.Store();
        try(var snapshot=AirSnapshot.attach(s,f.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store())) {
            var failure=fails(SnapshotLocalConstraints.Invalid.class,()->SnapshotLocalConstraints.scan(snapshot,keys,port,walk,1000,1000));eq(NONBLANK,failure.rule());eq(cap,failure.node());eq(0,failure.field());eq(PUBLICATION,snapshot.shape(f.root));
        }
        eq(true,port.closed);eq(true,walk.closed);eq(0L,port.claimed);eq(0L,walk.claimed);
        for(int remaining=0;remaining<4;remaining++) {
            var input=new SnapshotGraphChecks.Source();var fixture=new SnapshotGraphChecks.Fixture(input);var owner=new Store();owner.remaining=remaining;
            long u=input.record(IDS_UNIT_ID,fixture.id,input.text("u")),a=input.record(IDS_LABEL_ID,u,input.text("a")),b=input.record(IDS_LABEL_ID,u,input.text("b"));
            long domain=input.record(TYPES_LABEL_TYPE,u,input.list(a,b));
            try(var snapshot=AirSnapshot.attach(input,fixture.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store());var check=new SnapshotLocalConstraints(snapshot,keys,owner)) {
                eq(owner.failure,fails(IllegalStateException.class,()->check.node(domain,TYPES_LABEL_TYPE,null)));
                fails(IllegalStateException.class,()->check.node(domain,TYPES_LABEL_TYPE,null));eq(0,input.activeCursors);eq(PUBLICATION,snapshot.shape(fixture.root));
            }
            eq(0L,owner.claimed);eq(true,owner.closed);
        }
        for(boolean deny:new boolean[]{true,false}) {
            var input=new SnapshotGraphChecks.Source();var fixture=new SnapshotGraphChecks.Fixture(input);var owner=new Store();owner.deny=deny;owner.fail=!deny;owner.closeFailure=true;var graph=new SnapshotGraphChecks.Store();
            long u=input.record(IDS_UNIT_ID,fixture.id,input.text("u")),domain=input.record(TYPES_LABEL_TYPE,u,input.list(input.record(IDS_LABEL_ID,u,input.text("a"))));
            try(var snapshot=AirSnapshot.attach(input,fixture.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store())) {
                if(deny)eq(owner.failure,fails(IllegalStateException.class,()->SnapshotLocalConstraints.scan(snapshot,keys,owner,graph,1000,1000)));
                else {try(var check=new SnapshotLocalConstraints(snapshot,keys,owner)){eq(owner.failure,fails(IllegalStateException.class,()->check.node(domain,TYPES_LABEL_TYPE,null)));fails(IllegalStateException.class,()->check.node(domain,TYPES_LABEL_TYPE,null));}catch(IllegalStateException cleanup){eq(owner.cleanup,cleanup);}}
                eq(PUBLICATION,snapshot.shape(fixture.root));eq(0,input.activeCursors);
            }
            eq(0L,owner.claimed);eq(true,owner.closed);if(deny){eq(true,graph.closed);eq(1,owner.failure.getSuppressed().length);eq(owner.cleanup,owner.failure.getSuppressed()[0]);}
        }
        var resident=new Fixtures();resident.linear(new Operations.Nop(resident.header(resident.op("nop"))));
        try(var snapshot=AirSnapshot.fromPublication(resident.build());var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store())) {
            var counts=SnapshotLocalConstraints.scan(snapshot,keys,new Store(),new SnapshotGraphChecks.Store(),10_000,1000);eq(true,counts.nodes()>50);
        }
    }
    static void wholeGraphCollectsIndependentLocalFailures() {
        var s=new SnapshotGraphChecks.Source();var f=new SnapshotGraphChecks.Fixture(s);
        long first=s.record(CAPABILITIES_CAPABILITY,s.text(" "),s.text("1"));
        long second=s.record(CAPABILITIES_CAPABILITY,s.text("ok"),s.text(""));
        long manifest=s.child(f.root,2);s.replaceField(manifest,0,s.list(first,second));
        var found=new ArrayList<String>();
        try(var snapshot=AirSnapshot.attach(s,f.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store())) {
            var counts=SnapshotLocalConstraints.scan(snapshot,keys,new Store(),new SnapshotGraphChecks.Store(),1000,1000,
                (rule,node,field)->found.add(rule+":"+node+":"+field));
            eq(true,counts.nodes()>2);
        }
        eq(List.of(NONBLANK+":"+first+":0",NONBLANK+":"+second+":1"),found);

        var input=new SnapshotGraphChecks.Source();var fixture=new SnapshotGraphChecks.Fixture(input);
        long invalid=input.record(CAPABILITIES_CAPABILITY,input.text(" "),input.text("1"));
        input.replaceField(input.child(fixture.root,2),0,input.list(invalid));
        var callbackFailure=new IllegalStateException("diagnostic sink failure");
        var owner=new Store();
        try(var snapshot=AirSnapshot.attach(input,fixture.root);var keys=new SnapshotIdentityKeys(snapshot,new SnapshotAtomChecks.Store())) {
            eq(callbackFailure,fails(IllegalStateException.class,()->SnapshotLocalConstraints.scan(snapshot,keys,owner,
                new SnapshotGraphChecks.Store(),1000,1000,(rule,node,field)->{throw callbackFailure;})));
        }
        eq(true,owner.closed);eq(0L,owner.claimed);
    }
    private static void add(List<TextField> fields,AirShape shape,int...slots){for(int slot:slots)fields.add(new TextField(shape,slot));}
    private static long part(SnapshotGraphChecks.Source s,int kind,String count,String text,String negative){return s.record(DECIMAL_TEXT_PART,s.scalar(DECIMAL_TEXT_KIND,kind),s.integer(count),s.text(text),s.text(negative));}
    private static long record(SnapshotGraphChecks.Source s,AirShape shape){long[] fields=new long[shape.fieldCount()];for(int i=0;i<fields.length;i++)fields[i]=placeholder(s,shape.field(i).value());return s.record(shape,fields);}
    private static long placeholder(SnapshotGraphChecks.Source s,AirShape shape) {
        return switch(shape.form()) {
            case TEXT->s.text("x");case INTEGER->s.integer("1");case BOOLEAN,SMALL_INTEGER,ENUM->s.scalar(shape,0);case LIST->s.list(placeholder(s,TYPES_BUILTIN));case OPTIONAL->s.optional();
            case RECORD->s.record(shape);case UNION->{AirShape concrete=null;for(var candidate:AirShape.values())if(candidate.form()!=AirShape.Form.UNION&&shape.accepts(candidate)){concrete=candidate;break;}if(concrete==null)throw new AssertionError(shape);yield placeholder(s,concrete);}
        };
    }
    private static void accepted(SnapshotLocalConstraints check,long node,AirShape shape){check.node(node,shape,null);}
    private static void rejected(SnapshotLocalConstraints check,long node,AirShape shape,SnapshotLocalConstraints.Rule rule) {
        var error=fails(SnapshotLocalConstraints.Invalid.class,()->check.node(node,shape,null));eq(rule,error.rule());eq(node,error.node());
    }
    private record Member(long list,long key) { }
    private record Fact(long list,int kind) { }
    static final class Store implements SnapshotLocalConstraints.Storage {
        final Set<Member> members=new HashSet<>();final Set<Fact> facts=new HashSet<>();long claimed,adds,contains,remaining=Long.MAX_VALUE;boolean closed,deny,fail,closeFailure;
        final IllegalStateException failure=new IllegalStateException("local storage failure"),cleanup=new IllegalStateException("local storage cleanup");
        private void work(){if(fail||remaining--==0)throw failure;}
        public boolean known(long source,int kind){work();return facts.contains(new Fact(source,kind));}
        public void remember(long source,int kind){work();facts.add(new Fact(source,kind));}
        public boolean addLabel(long list,long key){work();adds++;return members.add(new Member(list,key));}
        public boolean containsLabel(long list,long key){work();contains++;return members.contains(new Member(list,key));}
        public AirSnapshotBuilder.Lease claim(long bytes){if(deny)throw failure;claimed+=bytes;return ()->claimed-=bytes;}
        public void close(){closed=true;members.clear();facts.clear();if(closeFailure)throw cleanup;}
    }
    private static <T extends Throwable>T fails(Class<T> type,Runnable action){try{action.run();}catch(Throwable error){if(type.isInstance(error))return type.cast(error);throw new AssertionError(error);}throw new AssertionError("missing "+type);}
    private static void eq(Object expected,Object actual){if(!Objects.equals(expected,actual))throw new AssertionError("expected="+expected+" actual="+actual);}
}
