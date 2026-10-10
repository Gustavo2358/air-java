package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.AirShape;
import io.github.gustavo2358.air.model.AirSnapshot;
import io.github.gustavo2358.air.model.Ids;
import io.github.gustavo2358.air.model.Interactions;
import io.github.gustavo2358.air.model.Evidence;
import io.github.gustavo2358.air.model.Expressions;
import io.github.gustavo2358.air.model.Operand;
import io.github.gustavo2358.air.model.Types;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.github.gustavo2358.air.model.AirShape.*;

/**
 * Managed structural admission for the official typed snapshot access. A checked value is bound to
 * the exact transferred snapshot and options; its constructor is deliberately not public. This
 * implementation never upgrades an unfinished mandatory rule set to STRUCTURALLY_VALID.
 */
public final class SnapshotValidator {
    private SnapshotValidator() { }

    /** Supplies a fresh owned port for every validation relation or graph walk. */
    public interface Storage {
        SnapshotIdentityKeys.Storage identities();
        SnapshotDeclarations.Storage declarations();
        SnapshotLocalConstraints.Storage localConstraints();
        SnapshotGraphWalk.Storage graphWalk();
        SnapshotNominalCycles.Storage nominalCycles();
        SnapshotGrounding.Storage grounding();
        SnapshotVisibleObjects.Storage visibleObjects();
        SnapshotSignatureIndex.Storage signatures();
        SnapshotDiagnosticTemplates.Storage diagnostics();
        SnapshotReferenceLists.Storage references();
        SnapshotLocalLabels.Storage localLabels();
        SnapshotAnnotationTemplates.Storage annotations();
        SnapshotCapabilities.Storage capabilities();
        SnapshotDistinctTuples.Storage distinctTuples();
        SnapshotContextWalk.Storage contextWalk();
        SnapshotTypes.Storage types();
    }

    /** Owns the exact admitted input until close; incomplete/invalid runs are still inspectable. */
    public static final class CheckedSnapshot implements AutoCloseable {
        private AirSnapshot snapshot;
        private final ValidationOptions options;
        private final ValidationResult result;

        private CheckedSnapshot(AirSnapshot snapshot,ValidationOptions options,ValidationResult result) {
            this.snapshot=snapshot;this.options=options;this.result=result;
        }
        public synchronized AirSnapshot snapshot() {
            if(snapshot==null)throw new IllegalStateException("checked AIR snapshot is closed");
            return snapshot;
        }
        public ValidationOptions options(){return options;}
        /** Includes incomplete/invalid outcomes; callers must inspect status before consumption. */
        public ValidationResult result(){return result;}
        @Override public synchronized void close(){if(snapshot!=null){var owner=snapshot;snapshot=null;owner.close();}}
    }

    /** Transfers snapshot and all ports. Operational failure closes the transferred snapshot. */
    public static CheckedSnapshot check(AirSnapshot snapshot,ValidationOptions options,Storage storage) {
        Objects.requireNonNull(snapshot,"snapshot");Objects.requireNonNull(options,"options");Objects.requireNonNull(storage,"storage");
        try {
            var run=new Run(snapshot,options,storage);
            ValidationResult result=run.validate();
            return new CheckedSnapshot(snapshot,options,result);
        } catch(RuntimeException|Error failure) {
            try{snapshot.close();}catch(RuntimeException|Error cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}
            throw failure;
        }
    }

    private static final class Run {
        private static final class OperationalLimit extends RuntimeException {
            private static final long serialVersionUID=1L;
            OperationalLimit(String message){super(message);}
        }
        private final AirSnapshot snapshot;
        private final ValidationOptions options;
        private final Storage storage;
        private final ArrayList<ValidationIssue> retained=new ArrayList<>();
        private final Map<ValidationIssue.Kind,Long> counts=new EnumMap<>(ValidationIssue.Kind.class);
        private boolean traversalCompleted;
        private long entities,operands,operations;

        Run(AirSnapshot snapshot,ValidationOptions options,Storage storage){this.snapshot=snapshot;this.options=options;this.storage=storage;}

        ValidationResult validate() {
            if(!airVersion()) {
                issue(ValidationIssue.Kind.UNSUPPORTED_CAPABILITY,"AIR_VERSION",publicationId(),
                    "this library targets AIR 2.0.0");
                return result();
            }
            try(var keys=new SnapshotIdentityKeys(snapshot,owned(storage.identities()));
                var declarations=SnapshotDeclarations.build(snapshot,keys,owned(storage.declarations()),
                    options.maximumEntities(),options.maximumNesting(),
                    (rule,identity,node)->issue(ValidationIssue.Kind.INVALID_IR,rule.invariant(),identity,
                        switch(rule){
                            case FOREIGN_PUBLICATION->"identity belongs to a different publication";
                            case DUPLICATE_ID->"duplicate identity";
                            case OPERATION_UNIT->"operation belongs to a different unit";
                            case SEQUENCE_UNIT->"sequence owner differs from operation unit";
                            case OPERAND_OWNER->"operand occurrence owner differs from its containing site";
                        }))) {
                entities=declarations.entities();operands=declarations.operands();operations=declarations.operations();
                SnapshotLocalConstraints.scan(snapshot,keys,owned(storage.localConstraints()),owned(storage.graphWalk()),
                    options.maximumEntities(),options.maximumNesting(),
                    (rule,node,field)->issue(ValidationIssue.Kind.INVALID_IR,"MODEL_CONSTRAINT",0,
                        "AIR constructor constraint "+rule+" at node "+node+" field "+field));
                SnapshotNominalCycles.scan(snapshot,keys,declarations,owned(storage.nominalCycles()),
                    (rule,identity,node)->issue(ValidationIssue.Kind.INVALID_IR,rule.invariant(),identity,
                        "cyclic structural dependency"));
                try(var grounding=SnapshotGrounding.build(snapshot,declarations,owned(storage.grounding()),
                    owned(storage.graphWalk()),options.maximumEntities(),options.maximumNesting());
                    var visible=SnapshotVisibleObjects.build(snapshot,keys,owned(storage.visibleObjects()))) {
                  try(var signatures=new SnapshotSignatureIndex(snapshot,keys,owned(storage.signatures()));
                    var labels=new SnapshotLocalLabels(snapshot,keys,declarations,owned(storage.localLabels()));
                    var tape=new SnapshotDiagnosticTemplates(owned(storage.diagnostics()),labels);
                    var refs=new SnapshotReferenceLists(snapshot,declarations,tape,owned(storage.references()));
                    var annotations=new SnapshotAnnotationTemplates(snapshot,keys,refs,tape,owned(storage.annotations()));
                    var capabilities=SnapshotCapabilities.build(snapshot,keys,owned(storage.capabilities()),this::capabilityIssue);
                    var distinct=new SnapshotDistinctTuples(owned(storage.distinctTuples()));
                    var context=new SnapshotContextWalk(owned(storage.contextWalk()));
                    var types=SnapshotTypes.build(snapshot,keys,declarations,owned(storage.types()),
                        (rule,owner,detail)->issue(ValidationIssue.Kind.INVALID_IR,rule,owner,detail))) {
                    referenceAdmission(keys,declarations,grounding,visible,signatures,tape,refs,labels,annotations,capabilities,distinct,context,types);
                    invocationConstraints(keys,declarations,signatures,distinct,types);
                    // A deliberately narrow directly-analyzable profile has every mandatory rule
                    // discharged here. All other AIR remains explicit incomplete validation.
                    traversalCompleted=directDependencyProfile(keys,declarations,types)
                        ||correlatedConcatDependencyProfile(keys,declarations,types);
                }
                // Finish the general rule set over borrowed typed facts when this input is
                // outside the old proven slices. Invalid local grammar is never reconstructed,
                // and an operational or unsupported admission is never upgraded.
                if(!traversalCompleted&&!counts.containsKey(ValidationIssue.Kind.INVALID_IR)
                        &&!counts.containsKey(ValidationIssue.Kind.UNSUPPORTED_CAPABILITY)
                        &&!counts.containsKey(ValidationIssue.Kind.RESOURCE_LIMIT))
                    return AirValidator.validate(SnapshotValidationProgram.afterPrimitiveAdmission(snapshot,declarations,visible),options);
                }
            } catch(SnapshotDeclarations.Limit limit) {
                resourceLimit(limit.getMessage());
            } catch(SnapshotGraphWalk.Limit limit) {
                resourceLimit(limit.getMessage());
            } catch(OperationalLimit limit) {
                resourceLimit(limit.getMessage());
            }
            return result();
        }

        private void referenceAdmission(SnapshotIdentityKeys keys,SnapshotDeclarations declarations,
                SnapshotGrounding grounding,SnapshotVisibleObjects visible,SnapshotSignatureIndex signatures,
                SnapshotDiagnosticTemplates tape,SnapshotReferenceLists refs,SnapshotLocalLabels labels,
                SnapshotAnnotationTemplates annotations,SnapshotCapabilities capabilities,SnapshotDistinctTuples distinct,
                SnapshotContextWalk context,SnapshotTypes types) {
            long root=snapshot.root(),publication=snapshot.field(root,PUBLICATION,0);
            // Origins precede all dependent evidence, matching the resident validator order.
            try(var origins=snapshot.elements(snapshot.field(root,PUBLICATION,8),ORIGINS_ORIGIN)) {
                while(origins.advance()) {
                    long node=origins.value(),owner=snapshot.field(node,snapshot.shape(node),0);
                    switch(snapshot.shape(node)) {
                        case ORIGINS_WRITTEN -> {
                            emit(tape,refs.reference(snapshot.field(node,ORIGINS_WRITTEN,1)),owner);
                            try(var frames=snapshot.elements(snapshot.field(node,ORIGINS_WRITTEN,3),ORIGINS_INCLUDE_FRAME)) {
                                while(frames.advance()){long frame=frames.value();emit(tape,refs.reference(snapshot.field(frame,ORIGINS_INCLUDE_FRAME,0)),owner);emit(tape,refs.reference(snapshot.field(frame,ORIGINS_INCLUDE_FRAME,1)),owner);}
                            }
                        }
                        case ORIGINS_DERIVED -> emit(tape,refs.references(snapshot.field(node,ORIGINS_DERIVED,1),IDS_ORIGIN_ID),owner);
                        case ORIGINS_CONTRACTUAL,ORIGINS_UNAVAILABLE -> { }
                        default -> throw new IllegalStateException("concrete origin required");
                    }
                }
            }
            try(var rows=snapshot.elements(snapshot.field(root,PUBLICATION,10),EVIDENCE_UNCERTAINTY)) {
                while(rows.advance()) {
                    long node=rows.value(),owner=snapshot.field(node,EVIDENCE_UNCERTAINTY,0);
                    emit(tape,refs.reference(snapshot.field(node,EVIDENCE_UNCERTAINTY,5)),owner);
                    emit(tape,annotations.scope(snapshot.field(node,EVIDENCE_UNCERTAINTY,3)),owner);
                }
            }
            try(var rows=snapshot.elements(snapshot.field(root,PUBLICATION,11),PROOFS_PREMISE)) {
                while(rows.advance()) {
                    long node=rows.value(),owner=snapshot.field(node,PROOFS_PREMISE,0);
                    emit(tape,refs.reference(snapshot.field(node,PROOFS_PREMISE,3)),owner);
                    long assertion=snapshot.field(node,PROOFS_PREMISE,4);
                    if(snapshot.shape(assertion)==PROOFS_DISJOINT_STORAGE)
                        disjointStorage(snapshot.field(assertion,PROOFS_DISJOINT_STORAGE,0),owner,keys,tape,refs,distinct);
                }
            }
            try(var rows=snapshot.elements(snapshot.field(root,PUBLICATION,5),MEMORY_STORAGE)) {
                while(rows.advance()) {
                    long node=rows.value(),header=snapshot.field(node,snapshot.shape(node),0),owner=snapshot.field(header,MEMORY_STORAGE_HEADER,0);
                    emit(tape,refs.reference(snapshot.field(header,MEMORY_STORAGE_HEADER,4)),owner);
                    optionalReference(tape,refs,snapshot.field(header,MEMORY_STORAGE_HEADER,1),IDS_UNIT_ID,owner);
                    if(snapshot.shape(node)==MEMORY_CELL)typeReference(snapshot.field(node,MEMORY_CELL,1),owner,keys,declarations,tape,refs,capabilities);
                    if(snapshot.shape(node)==MEMORY_REGION)optionalReference(tape,refs,snapshot.field(node,MEMORY_REGION,2),IDS_UNCERTAINTY_ID,owner);
                    if(snapshot.shape(node)==MEMORY_REGION)capabilities.require("memory.regions","1",owner,this::capabilityIssue);
                }
            }
            try(var rows=snapshot.elements(snapshot.field(root,PUBLICATION,6),INTERACTIONS_RESOURCE)) {
                while(rows.advance()) {
                    long node=rows.value(),owner=snapshot.field(node,INTERACTIONS_RESOURCE,0);
                    emit(tape,refs.reference(snapshot.field(node,INTERACTIONS_RESOURCE,2)),owner);
                    resourceDescription(snapshot.field(node,INTERACTIONS_RESOURCE,1),owner,0,keys,declarations,visible,tape,refs,capabilities);
                    long declaration=snapshot.field(node,INTERACTIONS_RESOURCE,3);
                    if(snapshot.size(declaration)!=0)resourceDeclaration(snapshot.element(declaration,INTERACTIONS_RESOURCE_DECLARATION,0),owner,keys,declarations,visible,tape,refs,capabilities,distinct);
                }
            }
            try(var rows=snapshot.elements(snapshot.field(root,PUBLICATION,7),ARTIFACTS_RELATION)) {
                while(rows.advance()) {
                    long node=rows.value(),owner=snapshot.field(node,ARTIFACTS_RELATION,0);
                    emit(tape,refs.reference(snapshot.field(node,ARTIFACTS_RELATION,1)),owner);
                    emit(tape,refs.reference(snapshot.field(node,ARTIFACTS_RELATION,4)),owner);
                    long destination=snapshot.field(node,ARTIFACTS_RELATION,2);
                    if(snapshot.shape(destination)==ARTIFACTS_INTERNAL_ARTIFACT)
                        emit(tape,refs.reference(snapshot.field(destination,ARTIFACTS_INTERNAL_ARTIFACT,0)),owner);
                    else resourceDescription(snapshot.field(destination,ARTIFACTS_EXTERNAL_ARTIFACT,0),owner,0,keys,declarations,visible,tape,refs,capabilities);
                }
            }
            emit(tape,annotations.coverage(snapshot.field(root,PUBLICATION,9)),publication);

            try(var units=snapshot.elements(snapshot.field(root,PUBLICATION,4),UNIT)) {
                while(units.advance()) {
                    long unit=units.value(),unitId=snapshot.field(unit,UNIT,0),unitKey=keys.key(unitId);
                    emit(tape,refs.reference(snapshot.field(unit,UNIT,10)),unitId);
                    optionalReference(tape,refs,snapshot.field(unit,UNIT,1),IDS_UNIT_ID,unitId);
                    emit(tape,refs.references(snapshot.field(unit,UNIT,3),IDS_OBJECT_ID),unitId);
                    optionalReference(tape,refs,snapshot.field(unit,UNIT,8),IDS_UNCERTAINTY_ID,unitId);
                    emit(tape,annotations.coverage(snapshot.field(unit,UNIT,9)),unitId);
                    try(var objects=snapshot.elements(snapshot.field(unit,UNIT,2),MEMORY_OBJECT_DECLARATION)) {
                        while(objects.advance()) {
                            long object=objects.value(),id=snapshot.field(object,MEMORY_OBJECT_DECLARATION,0);
                            if(keys.key(snapshot.field(id,IDS_OBJECT_ID,0))!=unitKey)
                                issue(ValidationIssue.Kind.INVALID_IR,"I-01",id,"object belongs to a different unit");
                            emit(tape,refs.reference(snapshot.field(object,MEMORY_OBJECT_DECLARATION,5)),id);
                            long type=snapshot.field(object,MEMORY_OBJECT_DECLARATION,2);
                            typeReference(type,id,keys,declarations,tape,refs,capabilities);
                            context.schedule(snapshot.field(object,MEMORY_OBJECT_DECLARATION,3),id,1,type,0);
                            emit(tape,annotations.precision(snapshot.field(object,MEMORY_OBJECT_DECLARATION,7)),id);
                        }
                    }
                    try(var ports=snapshot.elements(snapshot.field(unit,UNIT,6),ENTRIES_COMPLETION_PORT)) {
                        while(ports.advance()) {
                            long port=ports.value(),id=snapshot.field(port,ENTRIES_COMPLETION_PORT,0);
                            emit(tape,refs.reference(snapshot.field(port,ENTRIES_COMPLETION_PORT,1)),id);
                            capabilities.require("control.local","1",id,this::capabilityIssue);
                            if(keys.key(snapshot.field(id,IDS_COMPLETION_PORT_ID,0))!=unitKey)
                                issue(ValidationIssue.Kind.INVALID_IR,"I-01",id,"completion port owner differs from unit");
                        }
                    }
                    try(var entries=snapshot.elements(snapshot.field(unit,UNIT,4),ENTRIES_ENTRY)) {
                        while(entries.advance()) {
                            long entry=entries.value(),id=snapshot.field(entry,ENTRIES_ENTRY,0);
                            if(keys.key(snapshot.field(id,IDS_ENTRY_ID,0))!=unitKey)
                                issue(ValidationIssue.Kind.INVALID_IR,"I-05",id,"entry owner differs from unit");
                            emit(tape,refs.reference(snapshot.field(entry,ENTRIES_ENTRY,4)),id);
                            long signature=snapshot.field(entry,ENTRIES_ENTRY,2);
                            emit(tape,refs.reference(snapshot.field(signature,INTERACTIONS_SIGNATURE,2)),id);
                            signatures.checkEntry(entry,new SignatureReports());
                            signatureAdmission(signature,id,unitId,true,keys,declarations,tape,refs,capabilities);
                            long initial=snapshot.field(entry,ENTRIES_ENTRY,1);
                            if(snapshot.size(initial)!=0)localLabel(tape,refs,snapshot.element(initial,IDS_LABEL_ID,0),unitId,id,keys);
                            long state=snapshot.field(entry,ENTRIES_ENTRY,3);
                            emit(tape,refs.references(snapshot.field(state,ENTRIES_ENTRY_STATE,1),IDS_UNCERTAINTY_ID),id);
                            try(var conditions=snapshot.elements(snapshot.field(state,ENTRIES_ENTRY_STATE,0),ENTRIES_INITIAL_CONDITION)) {
                                while(conditions.advance()) {
                                    long condition=conditions.value();emit(tape,refs.reference(snapshot.field(condition,ENTRIES_INITIAL_CONDITION,2)),id);
                                    emit(tape,refs.references(snapshot.field(condition,ENTRIES_INITIAL_CONDITION,3),IDS_PREMISE_ID),id);
                                    long value=snapshot.field(condition,ENTRIES_INITIAL_CONDITION,1),reason=0;
                                    if(snapshot.shape(value)==ENTRIES_POSSIBLE_LITERALS)reason=snapshot.field(value,ENTRIES_POSSIBLE_LITERALS,1);
                                    else if(snapshot.shape(value)==ENTRIES_EXTERNAL_UNKNOWN)reason=snapshot.field(value,ENTRIES_EXTERNAL_UNKNOWN,0);
                                    else if(snapshot.shape(value)==ENTRIES_UNINITIALIZED)reason=snapshot.field(value,ENTRIES_UNINITIALIZED,0);
                                    if(reason!=0)emit(tape,refs.reference(reason),id);
                                    if(snapshot.shape(value)==ENTRIES_POSSIBLE_LITERALS)capabilities.require("entry.possibilities",capabilities.required("entry.possibilities","2")?"2":"1",id,this::capabilityIssue);
                                }
                            }
                        }
                    }
                    try(var sequences=snapshot.elements(snapshot.field(unit,UNIT,5),SEQUENCE)) {
                        while(sequences.advance()) {
                            long sequence=sequences.value(),label=snapshot.field(sequence,SEQUENCE,0);
                            emit(tape,refs.reference(snapshot.field(sequence,SEQUENCE,3)),label);
                            if(keys.key(snapshot.field(label,IDS_LABEL_ID,0))!=unitKey)
                                issue(ValidationIssue.Kind.INVALID_IR,"I-03",label,"sequence owner differs from unit");
                            operations(sequence,unitId,keys,declarations,visible,tape,refs,annotations,capabilities,context,types);
                        }
                    }
                }
            }
            operandAdmission(keys,declarations,grounding,visible,tape,refs,capabilities,context,types);
            context.drain(options.maximumNesting(),(node,owner,kind,type,depth)->contextReference(node,owner,kind,type,depth,keys,declarations,tape,refs,capabilities,context));
        }

        private void operations(long sequence,long unit,SnapshotIdentityKeys keys,SnapshotDeclarations declarations,
                SnapshotVisibleObjects visible,SnapshotDiagnosticTemplates tape,SnapshotReferenceLists refs,
                SnapshotAnnotationTemplates annotations,SnapshotCapabilities capabilities,SnapshotContextWalk context,SnapshotTypes types) {
            try(var rows=snapshot.elements(snapshot.field(sequence,SEQUENCE,1),INSTRUCTION)) {
                while(rows.advance())operation(rows.value(),unit,keys,declarations,visible,tape,refs,annotations,capabilities,context,types);
            }
            operation(snapshot.field(sequence,SEQUENCE,2),unit,keys,declarations,visible,tape,refs,annotations,capabilities,context,types);
        }
        private void operation(long operation,long unit,SnapshotIdentityKeys keys,SnapshotDeclarations declarations,
                SnapshotVisibleObjects visible,SnapshotDiagnosticTemplates tape,SnapshotReferenceLists refs,
                SnapshotAnnotationTemplates annotations,SnapshotCapabilities capabilities,SnapshotContextWalk context,SnapshotTypes types) {
            long header=snapshot.field(operation,snapshot.shape(operation),0),owner=snapshot.field(header,OPERATIONS_HEADER,0);
            emit(tape,refs.reference(snapshot.field(header,OPERATIONS_HEADER,1)),owner);
            emit(tape,refs.references(snapshot.field(header,OPERATIONS_HEADER,4),IDS_UNCERTAINTY_ID),owner);
            emit(tape,annotations.precision(snapshot.field(header,OPERATIONS_HEADER,3)),owner);
            switch(snapshot.shape(operation)) {
                case OPERATIONS_BRANCH -> {long predicate=snapshot.field(operation,OPERATIONS_BRANCH,1);role(predicate,Operand.Role.PREDICATE,owner);expect(types.ofNode(predicate),Types.Builtin.BOOL,owner,types);localLabel(tape,refs,snapshot.field(operation,OPERATIONS_BRANCH,2),unit,owner,keys);localLabel(tape,refs,snapshot.field(operation,OPERATIONS_BRANCH,3),unit,owner,keys);}
                case OPERATIONS_DISPATCH -> {long selector=snapshot.field(operation,OPERATIONS_DISPATCH,1);role(selector,Operand.Role.CONTROL_TARGET,owner);SnapshotTypes.Type selected=types.ofNode(selector);if(!selected.present())issue(ValidationIssue.Kind.INVALID_IR,"I-08",owner,"dispatch requires a known core scalar domain");try(var rows=snapshot.elements(snapshot.field(operation,OPERATIONS_DISPATCH,2),OPERATIONS_CASE)){while(rows.advance())localLabel(tape,refs,snapshot.field(rows.value(),OPERATIONS_CASE,1),unit,owner,keys);}localLabel(tape,refs,snapshot.field(operation,OPERATIONS_DISPATCH,3),unit,owner,keys);}
                case OPERATIONS_JUMP -> localLabel(tape,refs,snapshot.field(operation,OPERATIONS_JUMP,1),unit,owner,keys);
                case OPERATIONS_INDIRECT_JUMP -> {capabilities.require("control.indirect","1",owner,this::capabilityIssue);long target=snapshot.field(operation,OPERATIONS_INDIRECT_JUMP,1);role(target,Operand.Role.CONTROL_TARGET,owner);typeReferenceKnown(snapshot.field(operation,OPERATIONS_INDIRECT_JUMP,2),owner,keys,tape,refs,capabilities);envelope(snapshot.field(operation,OPERATIONS_INDIRECT_JUMP,3),owner,unit,false,keys,declarations,visible,tape,refs,capabilities,context);}
                case OPERATIONS_COPY_BYTES -> envelope(snapshot.field(operation,OPERATIONS_COPY_BYTES,4),owner,unit,false,keys,declarations,visible,tape,refs,capabilities,context);
                case OPERATIONS_HAVOC_MAY -> {context.schedule(snapshot.field(operation,OPERATIONS_HAVOC_MAY,1),owner,3,0,0);emit(tape,refs.reference(snapshot.field(operation,OPERATIONS_HAVOC_MAY,2)),owner);}
                case OPERATIONS_HAVOC_MUST -> emit(tape,refs.reference(snapshot.field(operation,OPERATIONS_HAVOC_MUST,2)),owner);
                case OPERATIONS_OPAQUE -> {emit(tape,refs.references(snapshot.field(operation,OPERATIONS_OPAQUE,3),IDS_OPERAND_ID),owner);envelope(snapshot.field(operation,OPERATIONS_OPAQUE,4),owner,unit,true,keys,declarations,visible,tape,refs,capabilities,context);}
                case OPERATIONS_LOCAL_INVOKE -> localInvoke(operation,owner,unit,keys,declarations,visible,tape,refs,capabilities,context);
                case OPERATIONS_LOCAL_BOUNDARY -> {capabilities.require("control.local","1",owner,this::capabilityIssue);long port=snapshot.field(operation,OPERATIONS_LOCAL_BOUNDARY,1);emit(tape,refs.reference(port),owner);if(keys.key(snapshot.field(port,IDS_COMPLETION_PORT_ID,0))!=keys.key(unit))issue(ValidationIssue.Kind.INVALID_IR,"I-08",owner,"completion port crosses unit");localLabel(tape,refs,snapshot.field(operation,OPERATIONS_LOCAL_BOUNDARY,2),unit,owner,keys);if(snapshot.size(snapshot.field(operation,OPERATIONS_LOCAL_BOUNDARY,4))!=0){capabilities.require("control.local.boundary_routes","1",owner,this::capabilityIssue);capabilities.require("control.local.resume_routes","1",owner,this::capabilityIssue);}envelope(snapshot.field(operation,OPERATIONS_LOCAL_BOUNDARY,3),owner,unit,false,keys,declarations,visible,tape,refs,capabilities,context);}
                case OPERATIONS_LOCAL_RESUME -> {capabilities.require("control.local","1",owner,this::capabilityIssue);if(snapshot.size(snapshot.field(operation,OPERATIONS_LOCAL_RESUME,2))!=0)capabilities.require("control.local.resume_routes","1",owner,this::capabilityIssue);envelope(snapshot.field(operation,OPERATIONS_LOCAL_RESUME,1),owner,unit,false,keys,declarations,visible,tape,refs,capabilities,context);}
                case OPERATIONS_LOCAL_UNWIND -> {capabilities.require("control.local","1",owner,this::capabilityIssue);if(snapshot.scalar(snapshot.field(operation,OPERATIONS_LOCAL_UNWIND,4))!=0)capabilities.require("control.local.unwind_all","1",owner,this::capabilityIssue);localLabel(tape,refs,snapshot.field(operation,OPERATIONS_LOCAL_UNWIND,2),unit,owner,keys);envelope(snapshot.field(operation,OPERATIONS_LOCAL_UNWIND,3),owner,unit,false,keys,declarations,visible,tape,refs,capabilities,context);}
                case OPERATIONS_INVOKE -> invoke(operation,owner,unit,keys,declarations,visible,tape,refs,capabilities,context,types);
                case OPERATIONS_ASSIGN -> {long destination=snapshot.field(operation,OPERATIONS_ASSIGN,1),value=snapshot.field(operation,OPERATIONS_ASSIGN,2);role(destination,Operand.Role.VALUE_WRITE,owner);role(value,Operand.Role.VALUE_READ,owner);if(!types.same(types.ofNode(destination),types.ofNode(value)))issue(ValidationIssue.Kind.INVALID_IR,"I-08/I-52",owner,"assignment lacks an exact sameDomain proof");}
                case OPERATIONS_HALT,OPERATIONS_NOP,OPERATIONS_RAISE,OPERATIONS_RETURN -> { }
                default -> throw new IllegalStateException("operation required");
            }
        }

        private void localInvoke(long operation,long owner,long unit,SnapshotIdentityKeys keys,SnapshotDeclarations declarations,
                SnapshotVisibleObjects visible,SnapshotDiagnosticTemplates tape,SnapshotReferenceLists refs,
                SnapshotCapabilities capabilities,SnapshotContextWalk context) {
            capabilities.require("control.local","1",owner,this::capabilityIssue);
            localLabel(tape,refs,snapshot.field(operation,OPERATIONS_LOCAL_INVOKE,1),unit,owner,keys);
            localLabel(tape,refs,snapshot.field(operation,OPERATIONS_LOCAL_INVOKE,3),unit,owner,keys);
            emit(tape,refs.references(snapshot.field(operation,OPERATIONS_LOCAL_INVOKE,2),IDS_COMPLETION_PORT_ID),owner);
            long guard=snapshot.field(operation,OPERATIONS_LOCAL_INVOKE,5);if(snapshot.size(guard)!=0){capabilities.require("control.local.reentry_guard","1",owner,this::capabilityIssue);localLabel(tape,refs,snapshot.field(snapshot.element(guard,OPERATIONS_REENTRY_GUARD,0),OPERATIONS_REENTRY_GUARD,1),unit,owner,keys);}
            long routes=snapshot.field(operation,OPERATIONS_LOCAL_INVOKE,6);if(snapshot.size(routes)!=0)capabilities.require("control.local.resume_routes","1",owner,this::capabilityIssue);
            try(var rows=snapshot.elements(routes,OPERATIONS_RESUME_ROUTE)){while(rows.advance())localLabel(tape,refs,snapshot.field(rows.value(),OPERATIONS_RESUME_ROUTE,1),unit,owner,keys);}
            envelope(snapshot.field(operation,OPERATIONS_LOCAL_INVOKE,4),owner,unit,false,keys,declarations,visible,tape,refs,capabilities,context);
        }

        private void invoke(long operation,long owner,long unit,SnapshotIdentityKeys keys,SnapshotDeclarations declarations,
                SnapshotVisibleObjects visible,SnapshotDiagnosticTemplates tape,SnapshotReferenceLists refs,
                SnapshotCapabilities capabilities,SnapshotContextWalk context,SnapshotTypes types) {
            long target=snapshot.field(operation,OPERATIONS_INVOKE,2);
            switch(snapshot.shape(target)) {
                case INTERACTIONS_INTERNAL_TARGET -> emit(tape,refs.reference(snapshot.field(target,INTERACTIONS_INTERNAL_TARGET,0)),owner);
                case INTERACTIONS_LITERAL_TARGET -> {emit(tape,refs.reference(snapshot.field(target,INTERACTIONS_LITERAL_TARGET,4)),owner);namePolicy(snapshot.field(target,INTERACTIONS_LITERAL_TARGET,3),owner,tape,refs,capabilities);}
                case INTERACTIONS_COMPUTED_TARGET -> {
                    long name=snapshot.field(target,INTERACTIONS_COMPUTED_TARGET,2);role(name,Operand.Role.CALL_TARGET,owner);
                    var domain=types.ofNode(name);
                    if(types.unknown(domain)&&capabilities.required("target.possibilities","1"))capabilities.require("target.possibilities","1",owner,this::capabilityIssue);
                    else expect(domain,Types.Builtin.TEXT,owner,types);
                    emit(tape,refs.reference(snapshot.field(target,INTERACTIONS_COMPUTED_TARGET,4)),owner);namePolicy(snapshot.field(target,INTERACTIONS_COMPUTED_TARGET,3),owner,tape,refs,capabilities);
                }
                default -> throw new IllegalStateException("invocation target required");
            }
            long signature=snapshot.field(operation,OPERATIONS_INVOKE,5);
            if(snapshot.shape(signature)==INTERACTIONS_ENTRY_SIGNATURE)emit(tape,refs.reference(snapshot.field(signature,INTERACTIONS_ENTRY_SIGNATURE,0)),owner);
            else {long external=snapshot.field(signature,INTERACTIONS_EXTERNAL_SIGNATURE,0);emit(tape,refs.reference(snapshot.field(external,INTERACTIONS_SIGNATURE,2)),owner);signatureAdmission(external,owner,unit,false,keys,declarations,tape,refs,capabilities);}
            long effects=snapshot.field(operation,OPERATIONS_INVOKE,7);foreignEffects(snapshot.field(effects,INTERACTIONS_EFFECT_BOUND,0),owner,context,tape,refs);
            try(var rows=snapshot.elements(snapshot.field(effects,INTERACTIONS_EFFECT_BOUND,1),INTERACTIONS_OUTCOME_EFFECTS)){while(rows.advance())foreignEffects(snapshot.field(rows.value(),INTERACTIONS_OUTCOME_EFFECTS,1),owner,context,tape,refs);}
            controlEnvelope(snapshot.field(operation,OPERATIONS_INVOKE,8),owner,unit,keys,tape,refs,context,true);
            long contract=snapshot.field(operation,OPERATIONS_INVOKE,9);
            if(snapshot.shape(contract)==INTERACTIONS_KNOWN_CONTRACT)emit(tape,refs.references(snapshot.field(snapshot.field(contract,INTERACTIONS_KNOWN_CONTRACT,0),INTERACTIONS_CONTRACT_REF,2),IDS_ORIGIN_ID),owner);
            else emit(tape,refs.reference(snapshot.field(contract,INTERACTIONS_UNKNOWN_CONTRACT,0)),owner);
        }

        private void foreignEffects(long effects,long owner,SnapshotContextWalk context,SnapshotDiagnosticTemplates tape,SnapshotReferenceLists refs) {
            context.schedule(snapshot.field(effects,INTERACTIONS_FOREIGN_EFFECTS,0),owner,2,0,0);context.schedule(snapshot.field(effects,INTERACTIONS_FOREIGN_EFFECTS,1),owner,2,0,0);emit(tape,refs.references(snapshot.field(effects,INTERACTIONS_FOREIGN_EFFECTS,2),IDS_OPERAND_ID),owner);
        }

        private void envelope(long envelope,long owner,long unit,boolean allowContinue,SnapshotIdentityKeys keys,
                SnapshotDeclarations declarations,SnapshotVisibleObjects visible,SnapshotDiagnosticTemplates tape,
                SnapshotReferenceLists refs,SnapshotCapabilities capabilities,SnapshotContextWalk context) {
            long memory=snapshot.field(envelope,ENVELOPES_ENVELOPE,0);
            emit(tape,refs.references(snapshot.field(memory,ENVELOPES_MEMORY_ENVELOPE,0),IDS_OPERAND_ID),owner);
            emit(tape,refs.references(snapshot.field(memory,ENVELOPES_MEMORY_ENVELOPE,2),IDS_OPERAND_ID),owner);
            emit(tape,refs.references(snapshot.field(memory,ENVELOPES_MEMORY_ENVELOPE,4),IDS_OPERAND_ID),owner);
            context.schedule(snapshot.field(memory,ENVELOPES_MEMORY_ENVELOPE,1),owner,2,0,0);context.schedule(snapshot.field(memory,ENVELOPES_MEMORY_ENVELOPE,3),owner,2,0,0);
            controlEnvelope(snapshot.field(envelope,ENVELOPES_ENVELOPE,1),owner,unit,keys,tape,refs,context,allowContinue);
            long dependencies=snapshot.field(envelope,ENVELOPES_ENVELOPE,2);
            try(var rows=snapshot.elements(snapshot.field(dependencies,ENVELOPES_DEPENDENCY_ENVELOPE,0),ENVELOPES_RESOURCE_USE)) {
                while(rows.advance()){long use=rows.value();resourceDescription(snapshot.field(use,ENVELOPES_RESOURCE_USE,1),owner,owner,keys,declarations,visible,tape,refs,capabilities);programPoint(snapshot.field(use,ENVELOPES_RESOURCE_USE,2),owner,tape,refs);emit(tape,refs.reference(snapshot.field(use,ENVELOPES_RESOURCE_USE,3)),owner);}
            }
            issue(ValidationIssue.Kind.SEMANTIC_OBLIGATION,"I-26",owner,"fallback bounds must overapproximate source/extension semantics; shape validation cannot prove correspondence");
        }

        private void controlEnvelope(long envelope,long owner,long unit,SnapshotIdentityKeys keys,
                SnapshotDiagnosticTemplates tape,SnapshotReferenceLists refs,SnapshotContextWalk context,boolean allowContinue) {
            long known=snapshot.field(envelope,snapshot.shape(envelope),0);
            try(var rows=snapshot.elements(known,snapshot.shape(envelope)==CONTROL_INVOCATION_OUTCOMES?CONTROL_INVOCATION_ALTERNATIVE:CONTROL_CONTROL_ALTERNATIVE)) {
                while(rows.advance())controlAlternative(rows.value(),owner,unit,keys,tape,refs,allowContinue);
            }
            long remainder=snapshot.field(envelope,snapshot.shape(envelope),1);if(snapshot.shape(remainder)==SCOPES_WITHIN_CONTROL)context.schedule(snapshot.field(remainder,SCOPES_WITHIN_CONTROL,0),owner,5,unit,0);
        }

        private void controlAlternative(long alternative,long owner,long unit,SnapshotIdentityKeys keys,
                SnapshotDiagnosticTemplates tape,SnapshotReferenceLists refs,boolean allowContinue) {
            switch(snapshot.shape(alternative)) {
                case CONTROL_NORMAL -> localLabel(tape,refs,snapshot.field(alternative,CONTROL_NORMAL,0),unit,owner,keys);
                case CONTROL_JUMP_ALTERNATIVE -> localLabel(tape,refs,snapshot.field(alternative,CONTROL_JUMP_ALTERNATIVE,0),unit,owner,keys);
                case CONTROL_EXCEPTIONAL -> exceptionDestination(snapshot.field(alternative,CONTROL_EXCEPTIONAL,1),owner,unit,keys,tape,refs);
                case CONTROL_ANY_EXCEPTION -> exceptionDestination(snapshot.field(alternative,CONTROL_ANY_EXCEPTION,0),owner,unit,keys,tape,refs);
                case CONTROL_CONTINUE_ALTERNATIVE -> {if(!allowContinue)issue(ValidationIssue.Kind.INVALID_IR,"I-60",owner,"continue is allowed only in fallback of a common operation");}
                case CONTROL_HALT_ALTERNATIVE,CONTROL_DIVERGE,CONTROL_RETURN_ALTERNATIVE -> { }
                default -> throw new IllegalStateException("control alternative required");
            }
        }

        private void exceptionDestination(long destination,long owner,long unit,SnapshotIdentityKeys keys,SnapshotDiagnosticTemplates tape,SnapshotReferenceLists refs) {if(snapshot.shape(destination)==CONTROL_HANDLER)localLabel(tape,refs,snapshot.field(destination,CONTROL_HANDLER,0),unit,owner,keys);}
        private void programPoint(long point,long owner,SnapshotDiagnosticTemplates tape,SnapshotReferenceLists refs) {switch(snapshot.shape(point)){case CONTROL_BEFORE->emit(tape,refs.reference(snapshot.field(point,CONTROL_BEFORE,0)),owner);case CONTROL_AFTER->emit(tape,refs.reference(snapshot.field(point,CONTROL_AFTER,0)),owner);case CONTROL_ENTRY_POINT->emit(tape,refs.reference(snapshot.field(point,CONTROL_ENTRY_POINT,0)),owner);case CONTROL_EXIT_POINT->emit(tape,refs.reference(snapshot.field(point,CONTROL_EXIT_POINT,0)),owner);default->throw new IllegalStateException("program point required");}}

        private void typeReferenceKnown(long type,long owner,SnapshotIdentityKeys keys,SnapshotDiagnosticTemplates tape,SnapshotReferenceLists refs,SnapshotCapabilities capabilities) {if(snapshot.shape(type)==TYPES_LABEL_TYPE){long unit=snapshot.field(type,TYPES_LABEL_TYPE,0);emit(tape,refs.reference(unit),owner);capabilities.require("control.indirect","1",owner,this::capabilityIssue);try(var labels=snapshot.elements(snapshot.field(type,TYPES_LABEL_TYPE,1),IDS_LABEL_ID)){while(labels.advance()){long label=labels.value();emit(tape,refs.reference(label),owner);if(keys.key(snapshot.field(label,IDS_LABEL_ID,0))!=keys.key(unit))issue(ValidationIssue.Kind.INVALID_IR,"I-08",owner,"label universe crosses unit");}}}}
        private void operandAdmission(SnapshotIdentityKeys keys,SnapshotDeclarations declarations,
                SnapshotGrounding grounding,SnapshotVisibleObjects visible,SnapshotDiagnosticTemplates tape,
                SnapshotReferenceLists refs,SnapshotCapabilities capabilities,SnapshotContextWalk context,SnapshotTypes types) {
            long size=declarations.entities();
            for(long at=0;at<size;at++) {
                long node=declarations.declaration(at,SnapshotDeclarations.Fact.NODE);if(!OPERAND.accepts(snapshot.shape(node)))continue;
                long id=declarations.declaration(at,SnapshotDeclarations.Fact.IDENTITY);
                if(declarations.fact(id,SnapshotDeclarations.Fact.NODE)!=node)continue;
                emit(tape,refs.reference(snapshot.field(snapshot.field(node,snapshot.shape(node),0),OPERAND_HEADER,2)),id);
                if(snapshot.shape(node)==PLACES_OBJECT_PLACE) {
                    long object=snapshot.field(node,PLACES_OBJECT_PLACE,1);emit(tape,refs.reference(object),id);
                    long unit=unitOfOperand(id);
                    if(!visible.contains(unit,object))issue(ValidationIssue.Kind.INVALID_IR,"I-02",id,"object not explicitly visible in operand unit");
                    if(visible.firstGrounding(object)&&!grounding.groundedObject(object))issue(ValidationIssue.Kind.INVALID_IR,"I-13",id,"executable object has no independently grounded location bound");
                } else if(snapshot.shape(node)==PLACES_REGION_SLICE) {
                    capabilities.require("memory.regions","1",id,this::capabilityIssue);
                    emit(tape,refs.reference(snapshot.field(node,PLACES_REGION_SLICE,1)),id);
                    typeReference(snapshot.field(node,PLACES_REGION_SLICE,5),id,keys,declarations,tape,refs,capabilities);
                    context.schedule(snapshot.field(node,PLACES_REGION_SLICE,4),id,4,snapshot.field(node,PLACES_REGION_SLICE,5),0);
                    expect(types.ofNode(snapshot.field(node,PLACES_REGION_SLICE,2)),Types.Builtin.INT,id,types);
                    expect(types.ofNode(snapshot.field(node,PLACES_REGION_SLICE,3)),Types.Builtin.INT,id,types);
                } else if(snapshot.shape(node)==PLACES_CHOICE) {
                    typeReference(snapshot.field(node,PLACES_CHOICE,3),id,keys,declarations,tape,refs,capabilities);
                    context.schedule(snapshot.field(node,PLACES_CHOICE,2),id,2,0,0);
                } else if(snapshot.shape(node)==EXPRESSIONS_UNKNOWN) {
                    long type=snapshot.field(node,EXPRESSIONS_UNKNOWN,1),reason=snapshot.field(node,EXPRESSIONS_UNKNOWN,4);
                    typeReference(type,id,keys,declarations,tape,refs,capabilities);emit(tape,refs.reference(reason),id);
                    if(snapshot.shape(type)==TYPES_UNKNOWN_TYPE&&keys.key(snapshot.field(type,TYPES_UNKNOWN_TYPE,0))==keys.key(reason))
                        issue(ValidationIssue.Kind.INVALID_IR,"I-50",id,"type uncertainty and value uncertainty need distinct identities");
                    issue(ValidationIssue.Kind.SEMANTIC_OBLIGATION,"I-09",id,"producer must substantiate that unknown expression dependencies and remaining reads are pure");
                    context.schedule(snapshot.field(node,EXPRESSIONS_UNKNOWN,3),id,2,0,0);
                }
            }
        }

        private void role(long operand,Operand.Role expected,long owner) {
            long header=snapshot.field(operand,snapshot.shape(operand),0),actual=snapshot.scalar(snapshot.field(header,OPERAND_HEADER,1));
            if(actual!=expected.ordinal())issue(ValidationIssue.Kind.INVALID_IR,"I-11",owner,"operand role must be "+expected);
        }

        /** Whole-inventory obligations: even an orphan invocation must be checked. */
        private void invocationConstraints(SnapshotIdentityKeys keys,SnapshotDeclarations declarations,
                SnapshotSignatureIndex signatures,SnapshotDistinctTuples distinct,SnapshotTypes types) {
            for(long at=0;at<declarations.entities();at++) {
                long operation=declarations.declaration(at,SnapshotDeclarations.Fact.NODE);
                if(snapshot.shape(operation)!=OPERATIONS_INVOKE)continue;
                long owner=declarations.declaration(at,SnapshotDeclarations.Fact.IDENTITY);
                if(declarations.fact(owner,SnapshotDeclarations.Fact.NODE)!=operation)continue;
                long outcomes=snapshot.field(operation,OPERATIONS_INVOKE,8),known=snapshot.field(outcomes,CONTROL_INVOCATION_OUTCOMES,0);
                long normals=0,catchAll=0;
                try(var rows=snapshot.elements(known,CONTROL_INVOCATION_ALTERNATIVE)) {
                    while(rows.advance()) {
                        long alternative=rows.value();
                        switch(snapshot.shape(alternative)) {
                            case CONTROL_NORMAL -> {if(++normals>1)issue(ValidationIssue.Kind.INVALID_IR,"I-60",owner,"invocation has more than one normal destination");}
                            case CONTROL_ANY_EXCEPTION -> {if(++catchAll>1)issue(ValidationIssue.Kind.INVALID_IR,"I-60",owner,"duplicate invocation catch-all");}
                            case CONTROL_EXCEPTIONAL -> {if(!distinct.first(operation,4,keys.atomKey(snapshot.field(alternative,CONTROL_EXCEPTIONAL,0)),0))issue(ValidationIssue.Kind.INVALID_IR,"I-60",owner,"duplicate invocation exception tag");}
                            default -> { }
                        }
                    }
                }
                long arguments=snapshot.field(operation,OPERATIONS_INVOKE,3),results=snapshot.field(operation,OPERATIONS_INVOKE,4);
                if(normals==0&&snapshot.size(results)!=0)issue(ValidationIssue.Kind.INVALID_IR,"I-08",owner,"results require an explicit normal invocation outcome");
                try(var rows=snapshot.elements(arguments,INTERACTIONS_ARGUMENT)) {
                    while(rows.advance()) {
                        long argument=rows.value();
                        role(snapshot.field(argument,snapshot.shape(argument),0),snapshot.shape(argument)==INTERACTIONS_REFERENCE_ARGUMENT?Operand.Role.ARGUMENT_REFERENCE:Operand.Role.ARGUMENT_VALUE,owner);
                    }
                }
                try(var rows=snapshot.elements(results,PLACE)){while(rows.advance())role(rows.value(),Operand.Role.RESULT_TARGET,owner);}
                long signature=snapshot.field(operation,OPERATIONS_INVOKE,5),target=snapshot.field(operation,OPERATIONS_INVOKE,2),resolved;
                if(snapshot.shape(signature)==INTERACTIONS_ENTRY_SIGNATURE) {
                    long entry=snapshot.field(signature,INTERACTIONS_ENTRY_SIGNATURE,0),declaration=declarations.fact(entry,SnapshotDeclarations.Fact.NODE);
                    if(snapshot.shape(target)!=INTERACTIONS_INTERNAL_TARGET||keys.key(snapshot.field(target,INTERACTIONS_INTERNAL_TARGET,0))!=keys.key(entry))
                        issue(ValidationIssue.Kind.INVALID_IR,"I-55",owner,"entry signature must correspond to the internal target");
                    if(declaration==0)continue;
                    resolved=snapshot.field(declaration,ENTRIES_ENTRY,2);
                } else {
                    if(snapshot.shape(target)==INTERACTIONS_INTERNAL_TARGET)issue(ValidationIssue.Kind.INVALID_IR,"I-55",owner,"internal target requires its entry signature");
                    resolved=snapshot.field(signature,INTERACTIONS_EXTERNAL_SIGNATURE,0);
                }
                long parameters=snapshot.field(resolved,INTERACTIONS_SIGNATURE,0),slots=snapshot.field(resolved,INTERACTIONS_SIGNATURE,1);
                signatures.checkInventory(parameters,owner,new SignatureReports());signatures.checkInventory(slots,owner,new SignatureReports());
                long parameterList=snapshot.field(parameters,INTERACTIONS_PARAMETER_INVENTORY,0),resultList=snapshot.field(slots,INTERACTIONS_RESULT_INVENTORY,0);
                if(snapshot.shape(snapshot.field(parameters,INTERACTIONS_PARAMETER_INVENTORY,1))==INTERACTIONS_NO_REMAINDER&&snapshot.size(arguments)!=snapshot.size(parameterList))
                    issue(ValidationIssue.Kind.INVALID_IR,"I-08",owner,"argument cardinality contradicts the closed parameter inventory");
                if(normals>0&&snapshot.shape(snapshot.field(slots,INTERACTIONS_RESULT_INVENTORY,1))==INTERACTIONS_NO_REMAINDER&&snapshot.size(results)!=snapshot.size(resultList))
                    issue(ValidationIssue.Kind.INVALID_IR,"I-08",owner,"result cardinality contradicts the closed result inventory");
                try(var rows=snapshot.elements(parameterList,INTERACTIONS_PARAMETER)) {
                    while(rows.advance()) {
                        long parameter=rows.value(),position=boundedPosition(snapshot.field(parameter,INTERACTIONS_PARAMETER,0),snapshot.size(arguments));
                        if(position<0){issue(ValidationIssue.Kind.INVALID_IR,"I-08",owner,"known parameter position has no corresponding argument");continue;}
                        long argument=snapshot.element(arguments,INTERACTIONS_ARGUMENT,position),mode=snapshot.field(parameter,INTERACTIONS_PARAMETER,1);
                        if(snapshot.shape(mode)==INTERACTIONS_KNOWN_MODE) {
                            var actual=snapshot.shape(argument)==INTERACTIONS_VALUE_ARGUMENT?Interactions.PassingMode.VALUE:snapshot.shape(argument)==INTERACTIONS_COPY_ARGUMENT?Interactions.PassingMode.COPY:Interactions.PassingMode.REFERENCE;
                            if(snapshot.scalar(snapshot.field(mode,INTERACTIONS_KNOWN_MODE,0))!=actual.ordinal())issue(ValidationIssue.Kind.INVALID_IR,"I-08",owner,"argument passing mode differs from signature");
                        }
                        signatureType(types.ofNode(snapshot.field(argument,snapshot.shape(argument),0)),snapshot.field(parameter,INTERACTIONS_PARAMETER,2),owner,types);
                    }
                }
                if(normals>0)try(var rows=snapshot.elements(resultList,INTERACTIONS_RESULT_SLOT)) {
                    while(rows.advance()) {
                        long slot=rows.value(),position=boundedPosition(snapshot.field(slot,INTERACTIONS_RESULT_SLOT,0),snapshot.size(results));
                        if(position<0){issue(ValidationIssue.Kind.INVALID_IR,"I-08",owner,"known result position has no corresponding destination");continue;}
                        signatureType(types.ofNode(snapshot.element(results,PLACE,position)),snapshot.field(slot,INTERACTIONS_RESULT_SLOT,1),owner,types);
                    }
                }
            }
        }
        private void signatureType(SnapshotTypes.Type actual,long declared,long owner,SnapshotTypes types) {
            if(snapshot.shape(declared)==TYPES_KNOWN&&!types.sameKnown(actual,types.fromRef(declared)))
                issue(ValidationIssue.Kind.INVALID_IR,"I-08",owner,"operand does not have the declared known signature type");
        }
        /** Bounded ordinal conversion; arbitrarily large INTEGER text is never materialized. */
        private long boundedPosition(long source,long count) {
            long length=snapshot.characterCount(source),offset=0,value=0;boolean negative=false,any=false,overflow=false;
            char[] block=new char[32];
            while(offset<length) {
                int read=snapshot.readCharacters(source,offset,block,0,(int)Math.min(block.length,length-offset));
                if(read<=0)throw new IllegalStateException("integer source made no progress");
                for(int i=0;i<read;i++) {
                    char c=block[i];if(offset+i==0&&(c=='-'||c=='+')){negative=c=='-';continue;}
                    if(c<'0'||c>'9')return -1;any=true;int digit=c-'0';
                    if(!overflow){if(value>(Long.MAX_VALUE-digit)/10)overflow=true;else value=value*10+digit;}
                }
                offset+=read;
            }
            return !any||overflow||negative&&value!=0||value>=count?-1:value;
        }
        private void expect(SnapshotTypes.Type actual,Types.Builtin expected,long owner,SnapshotTypes types) {if(!types.is(actual,expected))issue(ValidationIssue.Kind.INVALID_IR,"I-08",owner,"requires known("+expected+")");}

        /** Complete validation slice used by the first direct dependency pipeline. */
        private boolean directDependencyProfile(SnapshotIdentityKeys keys,SnapshotDeclarations declarations,SnapshotTypes types) {
            long root=snapshot.root(),manifest=snapshot.field(root,PUBLICATION,2);
            if(snapshot.size(snapshot.field(manifest,CAPABILITIES_MANIFEST,0))!=0
                    ||snapshot.size(snapshot.field(manifest,CAPABILITIES_MANIFEST,1))!=0
                    ||snapshot.size(snapshot.field(root,PUBLICATION,6))!=0
                    ||snapshot.size(snapshot.field(root,PUBLICATION,7))!=0
                    ||snapshot.size(snapshot.field(root,PUBLICATION,11))!=0)return false;
            try(var rows=snapshot.elements(snapshot.field(root,PUBLICATION,5),MEMORY_STORAGE)) {
                while(rows.advance()){long storage=rows.value();if(snapshot.shape(storage)!=MEMORY_CELL||!types.is(types.fromRef(snapshot.field(storage,MEMORY_CELL,1)),Types.Builtin.TEXT))return false;}
            }
            boolean foundComputedInvoke=false;
            try(var units=snapshot.elements(snapshot.field(root,PUBLICATION,4),UNIT)) {
                while(units.advance()) {
                    long unit=units.value();
                    try(var objects=snapshot.elements(snapshot.field(unit,UNIT,2),MEMORY_OBJECT_DECLARATION)) {
                        while(objects.advance()){long object=objects.value();if(!types.is(types.fromRef(snapshot.field(object,MEMORY_OBJECT_DECLARATION,2)),Types.Builtin.TEXT)||snapshot.shape(snapshot.field(object,MEMORY_OBJECT_DECLARATION,3))!=MEMORY_CELL_BINDING)return false;}
                    }
                    long entryList=snapshot.field(unit,UNIT,4);if(snapshot.size(entryList)==0)return false;
                    long initial=0;
                    // Cardinality is not a capability. Each independent Entry must discharge
                    // this slice's state/signature obligations; the shared body is read once.
                    try(var entries=snapshot.elements(entryList,ENTRIES_ENTRY)) {
                        while(entries.advance()) {
                            long entry=entries.value(),initialOptional=snapshot.field(entry,ENTRIES_ENTRY,1);
                            if(snapshot.size(initialOptional)!=1)return false;
                            long label=snapshot.element(initialOptional,IDS_LABEL_ID,0);
                            if(initial==0)initial=label;else if(keys.key(label)!=keys.key(initial))return false;
                            long signature=snapshot.field(entry,ENTRIES_ENTRY,2),parameters=snapshot.field(signature,INTERACTIONS_SIGNATURE,0),results=snapshot.field(signature,INTERACTIONS_SIGNATURE,1),state=snapshot.field(entry,ENTRIES_ENTRY,3);
                            if(snapshot.size(snapshot.field(parameters,INTERACTIONS_PARAMETER_INVENTORY,0))!=0||snapshot.shape(snapshot.field(parameters,INTERACTIONS_PARAMETER_INVENTORY,1))!=INTERACTIONS_NO_REMAINDER||snapshot.size(snapshot.field(results,INTERACTIONS_RESULT_INVENTORY,0))!=0||snapshot.shape(snapshot.field(results,INTERACTIONS_RESULT_INVENTORY,1))!=INTERACTIONS_NO_REMAINDER||snapshot.size(snapshot.field(state,ENTRIES_ENTRY_STATE,0))!=0||snapshot.size(snapshot.field(state,ENTRIES_ENTRY_STATE,1))!=0)return false;
                        }
                    }
                    long assignedObject=0,invokedObject=0;int assignments=0,invokes=0;
                    try(var sequences=snapshot.elements(snapshot.field(unit,UNIT,5),SEQUENCE)) {
                        while(sequences.advance()){
                            long sequence=sequences.value(),label=snapshot.field(sequence,SEQUENCE,0);boolean entrySequence=keys.key(label)==keys.key(initial);
                            try(var operations=snapshot.elements(snapshot.field(sequence,SEQUENCE,1),INSTRUCTION)){while(operations.advance()){long operation=operations.value();if(snapshot.shape(operation)==OPERATIONS_INVOKE||!directOperation(operation,types,declarations))return false;if(snapshot.shape(operation)==OPERATIONS_ASSIGN){if(!entrySequence||++assignments!=1)return false;assignedObject=keys.key(snapshot.field(snapshot.field(operation,OPERATIONS_ASSIGN,1),PLACES_OBJECT_PLACE,1));}}}
                            long terminator=snapshot.field(sequence,SEQUENCE,2);if(!directOperation(terminator,types,declarations))return false;
                            if(snapshot.shape(terminator)==OPERATIONS_INVOKE){if(!entrySequence||++invokes!=1)return false;long target=snapshot.field(terminator,OPERATIONS_INVOKE,2),name=snapshot.field(target,INTERACTIONS_COMPUTED_TARGET,2),place=snapshot.field(name,EXPRESSIONS_READ,1);invokedObject=keys.key(snapshot.field(place,PLACES_OBJECT_PLACE,1));}
                            else if(snapshot.shape(terminator)!=OPERATIONS_HALT&&snapshot.shape(terminator)!=OPERATIONS_RETURN)return false;
                        }
                    }
                    if(assignments!=1||invokes!=1||assignedObject!=invokedObject)return false;foundComputedInvoke=true;
                }
            }
            return foundComputedInvoke;
        }
        private boolean directOperation(long operation,SnapshotTypes types,SnapshotDeclarations declarations) {
            return switch(snapshot.shape(operation)) {
                case OPERATIONS_NOP,OPERATIONS_HALT -> true;
                case OPERATIONS_RETURN -> snapshot.size(snapshot.field(operation,OPERATIONS_RETURN,1))==0;
                case OPERATIONS_ASSIGN -> snapshot.shape(snapshot.field(operation,OPERATIONS_ASSIGN,1))==PLACES_OBJECT_PLACE
                    &&snapshot.shape(snapshot.field(operation,OPERATIONS_ASSIGN,2))==EXPRESSIONS_LITERAL
                    &&snapshot.shape(snapshot.field(snapshot.field(operation,OPERATIONS_ASSIGN,2),EXPRESSIONS_LITERAL,1))==VALUES_TEXT_VALUE;
                case OPERATIONS_INVOKE -> directInvoke(operation,types,declarations);
                default -> false;
            };
        }
        private boolean directInvoke(long invoke,SnapshotTypes types,SnapshotDeclarations declarations) {
            // Known slot transmission remains outside this certificate. AIR-04 §7.1
            // permits empty open inventories: no slot domain is invented for their
            // independent, typed arguments/results. Global invocationConstraints still
            // checks roles, cardinality and the explicit normal outcome.
            if(snapshot.size(snapshot.field(invoke,OPERATIONS_INVOKE,6))!=0)return false;
            long signature=snapshot.field(invoke,OPERATIONS_INVOKE,5);
            if(snapshot.shape(signature)!=INTERACTIONS_EXTERNAL_SIGNATURE)return false;
            long external=snapshot.field(signature,INTERACTIONS_EXTERNAL_SIGNATURE,0);
            if(!unmaterializedSignature(external))return false;
            try(var arguments=snapshot.elements(snapshot.field(invoke,OPERATIONS_INVOKE,3),INTERACTIONS_ARGUMENT)) {
                while(arguments.advance()) {
                    long argument=arguments.value(),operand=snapshot.field(argument,snapshot.shape(argument),0);
                    if(snapshot.shape(argument)==INTERACTIONS_REFERENCE_ARGUMENT) {
                        if(snapshot.shape(operand)!=PLACES_OBJECT_PLACE)return false;
                    } else if(!independentTextOperand(operand))return false;
                    if(!types.is(types.ofNode(operand),Types.Builtin.TEXT))return false;
                }
            }
            try(var results=snapshot.elements(snapshot.field(invoke,OPERATIONS_INVOKE,4),PLACE)) {
                while(results.advance())if(snapshot.shape(results.value())!=PLACES_OBJECT_PLACE
                        ||!types.is(types.ofNode(results.value()),Types.Builtin.TEXT))return false;
            }
            long effects=snapshot.field(invoke,OPERATIONS_INVOKE,7);
            if(snapshot.size(snapshot.field(effects,INTERACTIONS_EFFECT_BOUND,1))!=0
                    ||snapshot.size(snapshot.field(snapshot.field(effects,INTERACTIONS_EFFECT_BOUND,0),INTERACTIONS_FOREIGN_EFFECTS,2))!=0)return false;
            long target=snapshot.field(invoke,OPERATIONS_INVOKE,2);if(snapshot.shape(target)!=INTERACTIONS_COMPUTED_TARGET)return false;
            long name=snapshot.field(target,INTERACTIONS_COMPUTED_TARGET,2);if(snapshot.shape(name)!=EXPRESSIONS_READ||snapshot.shape(snapshot.field(name,EXPRESSIONS_READ,1))!=PLACES_OBJECT_PLACE||!types.is(types.ofNode(name),Types.Builtin.TEXT))return false;
            long namespace=snapshot.field(target,INTERACTIONS_COMPUTED_TARGET,1);return equal(namespace,"cobol.program");
        }
        private boolean independentTextOperand(long operand) {
            return switch(snapshot.shape(operand)) {
                case EXPRESSIONS_LITERAL -> snapshot.shape(snapshot.field(operand,EXPRESSIONS_LITERAL,1))==VALUES_TEXT_VALUE;
                case EXPRESSIONS_READ -> snapshot.shape(snapshot.field(operand,EXPRESSIONS_READ,1))==PLACES_OBJECT_PLACE;
                case EXPRESSIONS_UNKNOWN -> snapshot.size(snapshot.field(operand,EXPRESSIONS_UNKNOWN,2))==0;
                default -> false;
            };
        }
        private boolean unmaterializedSignature(long signature) {
            long parameters=snapshot.field(signature,INTERACTIONS_SIGNATURE,0),results=snapshot.field(signature,INTERACTIONS_SIGNATURE,1);
            return snapshot.size(snapshot.field(parameters,INTERACTIONS_PARAMETER_INVENTORY,0))==0
                &&snapshot.size(snapshot.field(results,INTERACTIONS_RESULT_INVENTORY,0))==0;
        }

        /** Complete, deliberately narrow diamond used by the first relational dependency slice. */
        private boolean correlatedConcatDependencyProfile(SnapshotIdentityKeys keys,SnapshotDeclarations declarations,SnapshotTypes types) {
            long root=snapshot.root(),manifest=snapshot.field(root,PUBLICATION,2),units=snapshot.field(root,PUBLICATION,4);
            if(snapshot.size(snapshot.field(manifest,CAPABILITIES_MANIFEST,0))!=0
                    ||snapshot.size(snapshot.field(manifest,CAPABILITIES_MANIFEST,1))!=0
                    ||snapshot.size(snapshot.field(root,PUBLICATION,6))!=0
                    ||snapshot.size(snapshot.field(root,PUBLICATION,7))!=0
                    ||snapshot.size(snapshot.field(root,PUBLICATION,11))!=0
                    ||snapshot.size(units)!=1)return false;
            try(var rows=snapshot.elements(snapshot.field(root,PUBLICATION,5),MEMORY_STORAGE)) {
                while(rows.advance()){long storage=rows.value();if(snapshot.shape(storage)!=MEMORY_CELL)return false;long type=snapshot.field(storage,MEMORY_CELL,1);if(!types.is(types.fromRef(type),Types.Builtin.TEXT)&&!types.is(types.fromRef(type),Types.Builtin.BOOL))return false;}
            }
            long unit=snapshot.element(units,UNIT,0),entryList=snapshot.field(unit,UNIT,4),sequences=snapshot.field(unit,UNIT,5);
            if(snapshot.size(entryList)!=1||snapshot.size(sequences)!=5)return false;
            try(var objects=snapshot.elements(snapshot.field(unit,UNIT,2),MEMORY_OBJECT_DECLARATION)) {
                while(objects.advance()){long object=objects.value(),type=snapshot.field(object,MEMORY_OBJECT_DECLARATION,2);if((!types.is(types.fromRef(type),Types.Builtin.TEXT)&&!types.is(types.fromRef(type),Types.Builtin.BOOL))||snapshot.shape(snapshot.field(object,MEMORY_OBJECT_DECLARATION,3))!=MEMORY_CELL_BINDING)return false;}
            }
            long entry=snapshot.element(entryList,ENTRIES_ENTRY,0),initialOptional=snapshot.field(entry,ENTRIES_ENTRY,1);
            long conditionObject=correlatedEntryCondition(entry,keys,types);if(snapshot.size(initialOptional)!=1||conditionObject==0)return false;
            long initial=snapshot.element(initialOptional,IDS_LABEL_ID,0),head=sequenceByLabel(sequences,keys.key(initial),keys);
            if(head==0||snapshot.size(snapshot.field(head,SEQUENCE,1))!=0)return false;
            long branch=snapshot.field(head,SEQUENCE,2);if(snapshot.shape(branch)!=OPERATIONS_BRANCH)return false;
            long predicate=snapshot.field(branch,OPERATIONS_BRANCH,1);
            if(readObject(predicate,keys)!=conditionObject)return false;
            long left=sequenceByLabel(sequences,keys.key(snapshot.field(branch,OPERATIONS_BRANCH,2)),keys);
            long right=sequenceByLabel(sequences,keys.key(snapshot.field(branch,OPERATIONS_BRANCH,3)),keys);
            if(left==0||right==0||left==right)return false;
            var leftArm=literalArm(left,keys);var rightArm=literalArm(right,keys);
            if(leftArm==null||rightArm==null||leftArm.joinLabel()!=rightArm.joinLabel()
                    ||leftArm.firstObject()!=rightArm.firstObject()||leftArm.secondObject()!=rightArm.secondObject())return false;
            long join=sequenceByLabel(sequences,leftArm.joinLabel(),keys);if(join==0||join==head||join==left||join==right)return false;
            long instructions=snapshot.field(join,SEQUENCE,1);if(snapshot.size(instructions)!=1)return false;
            long assignment=snapshot.element(instructions,INSTRUCTION,0);if(snapshot.shape(assignment)!=OPERATIONS_ASSIGN)return false;
            long destination=snapshot.field(assignment,OPERATIONS_ASSIGN,1),expression=snapshot.field(assignment,OPERATIONS_ASSIGN,2);
            if(snapshot.shape(destination)!=PLACES_OBJECT_PLACE||!fitConcatReads(expression,leftArm.firstObject(),leftArm.secondObject(),keys))return false;
            long targetObject=keys.key(snapshot.field(destination,PLACES_OBJECT_PLACE,1));
            if(targetObject==leftArm.firstObject()||targetObject==leftArm.secondObject())return false;
            long invoke=snapshot.field(join,SEQUENCE,2);if(!directInvoke(invoke,types,declarations))return false;
            long target=snapshot.field(invoke,OPERATIONS_INVOKE,2),read=snapshot.field(target,INTERACTIONS_COMPUTED_TARGET,2);
            if(keys.key(snapshot.field(snapshot.field(read,EXPRESSIONS_READ,1),PLACES_OBJECT_PLACE,1))!=targetObject)return false;
            long end=remainingSequence(sequences,head,left,right,join);
            if(end==0||snapshot.size(snapshot.field(end,SEQUENCE,1))!=0)return false;long terminator=snapshot.field(end,SEQUENCE,2);
            return snapshot.shape(terminator)==OPERATIONS_HALT||snapshot.shape(terminator)==OPERATIONS_RETURN&&snapshot.size(snapshot.field(terminator,OPERATIONS_RETURN,1))==0;
        }
        private long correlatedEntryCondition(long entry,SnapshotIdentityKeys keys,SnapshotTypes types) {
            long signature=snapshot.field(entry,ENTRIES_ENTRY,2),parameters=snapshot.field(signature,INTERACTIONS_SIGNATURE,0),results=snapshot.field(signature,INTERACTIONS_SIGNATURE,1),state=snapshot.field(entry,ENTRIES_ENTRY,3);
            if(snapshot.size(snapshot.field(parameters,INTERACTIONS_PARAMETER_INVENTORY,0))!=0
                ||snapshot.shape(snapshot.field(parameters,INTERACTIONS_PARAMETER_INVENTORY,1))!=INTERACTIONS_NO_REMAINDER
                ||snapshot.size(snapshot.field(results,INTERACTIONS_RESULT_INVENTORY,0))!=0
                ||snapshot.shape(snapshot.field(results,INTERACTIONS_RESULT_INVENTORY,1))!=INTERACTIONS_NO_REMAINDER
                ||snapshot.size(snapshot.field(state,ENTRIES_ENTRY_STATE,0))!=1)return 0;
            long condition=snapshot.element(snapshot.field(state,ENTRIES_ENTRY_STATE,0),ENTRIES_INITIAL_CONDITION,0),place=snapshot.field(condition,ENTRIES_INITIAL_CONDITION,0),value=snapshot.field(condition,ENTRIES_INITIAL_CONDITION,1);
            if(snapshot.shape(place)!=PLACES_OBJECT_PLACE||snapshot.shape(value)!=ENTRIES_EXTERNAL_UNKNOWN)return 0;
            long object=snapshot.field(place,PLACES_OBJECT_PLACE,1);return types.is(types.ofNode(place),Types.Builtin.BOOL)?keys.key(object):0;
        }
        private record LiteralArm(long firstObject,long secondObject,long joinLabel) { }
        private LiteralArm literalArm(long sequence,SnapshotIdentityKeys keys) {
            long instructions=snapshot.field(sequence,SEQUENCE,1);if(snapshot.size(instructions)!=2)return null;
            long[] objects=new long[2];
            for(int i=0;i<2;i++){
                long assignment=snapshot.element(instructions,INSTRUCTION,i);if(snapshot.shape(assignment)!=OPERATIONS_ASSIGN)return null;
                long destination=snapshot.field(assignment,OPERATIONS_ASSIGN,1),value=snapshot.field(assignment,OPERATIONS_ASSIGN,2);
                if(snapshot.shape(destination)!=PLACES_OBJECT_PLACE||snapshot.shape(value)!=EXPRESSIONS_LITERAL
                        ||snapshot.shape(snapshot.field(value,EXPRESSIONS_LITERAL,1))!=VALUES_TEXT_VALUE)return null;
                objects[i]=keys.key(snapshot.field(destination,PLACES_OBJECT_PLACE,1));
            }
            if(objects[0]==objects[1])return null;
            long terminator=snapshot.field(sequence,SEQUENCE,2);if(snapshot.shape(terminator)!=OPERATIONS_JUMP)return null;
            return objects[0]<objects[1]?new LiteralArm(objects[0],objects[1],keys.key(snapshot.field(terminator,OPERATIONS_JUMP,1)))
                :new LiteralArm(objects[1],objects[0],keys.key(snapshot.field(terminator,OPERATIONS_JUMP,1)));
        }
        private boolean fitConcatReads(long expression,long first,long second,SnapshotIdentityKeys keys) {
            if(snapshot.shape(expression)!=EXPRESSIONS_FIT_TEXT)return false;
            long length=snapshot.field(expression,EXPRESSIONS_FIT_TEXT,2);
            // The current detached dependency product uses a Java String. This is a profile
            // representability boundary on the explicit FitText result, never on TEXT in general
            // or on the (projected, non-materialized) CONCAT intermediate.
            try{if(new BigInteger(text(length)).bitLength()>31)return false;}catch(NumberFormatException malformed){return false;}
            long binary=snapshot.field(expression,EXPRESSIONS_FIT_TEXT,1);
            if(snapshot.shape(binary)!=EXPRESSIONS_BINARY||snapshot.scalar(snapshot.field(binary,EXPRESSIONS_BINARY,1))!=Expressions.BinaryOperator.CONCAT.ordinal())return false;
            long left=readObject(snapshot.field(binary,EXPRESSIONS_BINARY,2),keys),right=readObject(snapshot.field(binary,EXPRESSIONS_BINARY,3),keys);
            return left!=0&&right!=0&&left!=right&&((left==first&&right==second)||(left==second&&right==first));
        }
        private long readObject(long expression,SnapshotIdentityKeys keys) {
            if(snapshot.shape(expression)!=EXPRESSIONS_READ)return 0;long place=snapshot.field(expression,EXPRESSIONS_READ,1);
            return snapshot.shape(place)==PLACES_OBJECT_PLACE?keys.key(snapshot.field(place,PLACES_OBJECT_PLACE,1)):0;
        }
        private long sequenceByLabel(long sequences,long label,SnapshotIdentityKeys keys) {
            try(var rows=snapshot.elements(sequences,SEQUENCE)){while(rows.advance()){long sequence=rows.value();if(keys.key(snapshot.field(sequence,SEQUENCE,0))==label)return sequence;}}return 0;
        }
        private long remainingSequence(long sequences,long... used) {
            try(var rows=snapshot.elements(sequences,SEQUENCE)){outer:while(rows.advance()){long sequence=rows.value();for(long value:used)if(sequence==value)continue outer;return sequence;}}return 0;
        }
        private void contextReference(long node,long owner,long kind,long type,long depth,SnapshotIdentityKeys keys,
                SnapshotDeclarations declarations,SnapshotDiagnosticTemplates tape,SnapshotReferenceLists refs,
                SnapshotCapabilities capabilities,SnapshotContextWalk context) {
            switch((int)kind) {
                case 1 -> {
                    switch(snapshot.shape(node)) {
                        case MEMORY_CELL_BINDING -> emit(tape,refs.reference(snapshot.field(node,MEMORY_CELL_BINDING,0)),owner);
                        case MEMORY_ALIAS_BINDING -> emit(tape,refs.reference(snapshot.field(node,MEMORY_ALIAS_BINDING,0)),owner);
                        case MEMORY_VIEW_BINDING -> {capabilities.require("memory.regions","1",owner,this::capabilityIssue);emit(tape,refs.reference(snapshot.field(node,MEMORY_VIEW_BINDING,0)),owner);context.schedule(snapshot.field(node,MEMORY_VIEW_BINDING,3),owner,4,type,depth+1);}
                        case MEMORY_ALTERNATIVES_BINDING -> {try(var rows=snapshot.elements(snapshot.field(node,MEMORY_ALTERNATIVES_BINDING,0),MEMORY_BINDING)){while(rows.advance())context.schedule(rows.value(),owner,1,type,depth+1);}context.schedule(snapshot.field(node,MEMORY_ALTERNATIVES_BINDING,1),owner,2,0,depth+1);}
                        case MEMORY_UNKNOWN_BINDING -> {context.schedule(snapshot.field(node,MEMORY_UNKNOWN_BINDING,0),owner,3,0,depth+1);emit(tape,refs.reference(snapshot.field(node,MEMORY_UNKNOWN_BINDING,1)),owner);}
                        default -> throw new IllegalStateException("memory binding required");
                    }
                }
                case 2 -> {if(snapshot.shape(node)==SCOPES_WITHIN_MEMORY)context.schedule(snapshot.field(node,SCOPES_WITHIN_MEMORY,0),owner,3,0,depth+1);}
                case 3 -> {
                    switch(snapshot.shape(node)) {
                        case SCOPES_OBJECTS_MEMORY -> emit(tape,refs.references(snapshot.field(node,SCOPES_OBJECTS_MEMORY,0),IDS_OBJECT_ID),owner);
                        case SCOPES_STORAGE_MEMORY -> emit(tape,refs.references(snapshot.field(node,SCOPES_STORAGE_MEMORY,0),IDS_STORAGE_ID),owner);
                        case SCOPES_VISIBLE_MEMORY -> emit(tape,refs.reference(snapshot.field(node,SCOPES_VISIBLE_MEMORY,0)),owner);
                        case SCOPES_ALL_MEMORY -> emit(tape,refs.reference(snapshot.field(node,SCOPES_ALL_MEMORY,0)),owner);
                        case SCOPES_MEMORY_UNION -> {try(var rows=snapshot.elements(snapshot.field(node,SCOPES_MEMORY_UNION,0),SCOPES_MEMORY_SCOPE)){while(rows.advance())context.schedule(rows.value(),owner,3,0,depth+1);}}
                        default -> throw new IllegalStateException("memory scope required");
                    }
                }
                case 4 -> codecReferences(node,owner,keys,declarations,tape,refs,capabilities);
                case 5 -> {switch(snapshot.shape(node)){case SCOPES_LABELS_CONTROL->{try(var rows=snapshot.elements(snapshot.field(node,SCOPES_LABELS_CONTROL,0),IDS_LABEL_ID)){while(rows.advance())localLabel(tape,refs,rows.value(),type,owner,keys);}}case SCOPES_UNIT_CONTROL->emit(tape,refs.reference(snapshot.field(node,SCOPES_UNIT_CONTROL,0)),owner);case SCOPES_ALL_CONTROL->emit(tape,refs.reference(snapshot.field(node,SCOPES_ALL_CONTROL,0)),owner);case SCOPES_CONTROL_UNION->{try(var rows=snapshot.elements(snapshot.field(node,SCOPES_CONTROL_UNION,0),SCOPES_CONTROL_SCOPE)){while(rows.advance())context.schedule(rows.value(),owner,5,type,depth+1);}}default->throw new IllegalStateException("control scope required");}}
                default -> throw new IllegalStateException("unknown contextual reference task "+kind);
            }
        }

        private void resourceDeclaration(long declaration,long owner,SnapshotIdentityKeys keys,
                SnapshotDeclarations declarations,SnapshotVisibleObjects visible,SnapshotDiagnosticTemplates tape,
                SnapshotReferenceLists refs,SnapshotCapabilities capabilities,SnapshotDistinctTuples distinct) {
            capabilities.require("resource.bindings","1",owner,this::capabilityIssue);
            long unit=snapshot.field(declaration,INTERACTIONS_RESOURCE_DECLARATION,0);
            emit(tape,refs.reference(unit),owner);
            if(!qualified(snapshot.field(declaration,INTERACTIONS_RESOURCE_DECLARATION,2))
                    ||!qualified(snapshot.field(declaration,INTERACTIONS_RESOURCE_DECLARATION,3)))
                issue(ValidationIssue.Kind.INVALID_IR,"I-RB-03",owner,"resource classification/nameSource must be qualified");
            long objectList=snapshot.field(declaration,INTERACTIONS_RESOURCE_DECLARATION,4);
            try(var objects=snapshot.elements(objectList,INTERACTIONS_RESOURCE_OBJECT)) {
                while(objects.advance()) {
                    long row=objects.value(),object=snapshot.field(row,INTERACTIONS_RESOURCE_OBJECT,0);
                    emit(tape,refs.reference(object),owner);
                    if(!visible.contains(unit,object))issue(ValidationIssue.Kind.INVALID_IR,"I-RB-01",owner,"associated object absent or not explicitly visible to resource owner");
                    if(!distinct.first(objectList,2,keys.key(object),keys.atomKey(snapshot.field(row,INTERACTIONS_RESOURCE_OBJECT,1))))
                        issue(ValidationIssue.Kind.INVALID_IR,"I-RB-02",owner,"duplicate object/role association");
                }
            }
            long useList=snapshot.field(declaration,INTERACTIONS_RESOURCE_DECLARATION,5);
            try(var uses=snapshot.elements(useList,INTERACTIONS_RESOURCE_USE)) {
                while(uses.advance()) {long use=uses.value(),operation=snapshot.field(use,INTERACTIONS_RESOURCE_USE,0);emit(tape,refs.reference(operation),owner);emit(tape,refs.reference(snapshot.field(use,INTERACTIONS_RESOURCE_USE,2)),owner);if(!distinct.first(useList,3,keys.key(operation),keys.atomKey(snapshot.field(use,INTERACTIONS_RESOURCE_USE,1))))issue(ValidationIssue.Kind.INVALID_IR,"I-RB-02",owner,"duplicate operation/role association");}
            }
        }

        private void disjointStorage(long list,long owner,SnapshotIdentityKeys keys,SnapshotDiagnosticTemplates tape,
                SnapshotReferenceLists refs,SnapshotDistinctTuples distinct) {
            emit(tape,refs.references(list,IDS_STORAGE_ID),owner);boolean duplicate=false;
            try(var rows=snapshot.elements(list,IDS_STORAGE_ID)){while(rows.advance())if(!distinct.first(list,1,keys.key(rows.value()),0))duplicate=true;}
            if(snapshot.size(list)<2||duplicate)issue(ValidationIssue.Kind.INVALID_IR,"I-58",owner,"disjoint_storage requires at least two distinct storage bases");
        }

        private void resourceDescription(long description,long owner,long operation,SnapshotIdentityKeys keys,
                SnapshotDeclarations declarations,SnapshotVisibleObjects visible,SnapshotDiagnosticTemplates tape,
                SnapshotReferenceLists refs,SnapshotCapabilities capabilities) {
            switch(snapshot.shape(description)) {
                case INTERACTIONS_LOCAL_RESOURCE -> {
                    capabilities.require("resource.bindings","1",owner,this::capabilityIssue);
                    if(operation!=0)issue(ValidationIssue.Kind.INVALID_IR,"I-RB-03",owner,"local resource is declarative, not an executable envelope target");
                }
                case INTERACTIONS_UNKNOWN_RESOURCE -> {
                    capabilities.require("resource.bindings","1",owner,this::capabilityIssue);
                    emit(tape,refs.reference(snapshot.field(description,INTERACTIONS_UNKNOWN_RESOURCE,2)),owner);
                    if(operation!=0)issue(ValidationIssue.Kind.INVALID_IR,"I-RB-03",owner,"unknown resource is declarative, not an executable envelope target");
                }
                case INTERACTIONS_INTERNAL_TARGET -> emit(tape,refs.reference(snapshot.field(description,INTERACTIONS_INTERNAL_TARGET,0)),owner);
                case INTERACTIONS_LITERAL_TARGET -> {emit(tape,refs.reference(snapshot.field(description,INTERACTIONS_LITERAL_TARGET,4)),owner);namePolicy(snapshot.field(description,INTERACTIONS_LITERAL_TARGET,3),owner,tape,refs,capabilities);}
                case INTERACTIONS_COMPUTED_RESOURCE -> {
                    emit(tape,refs.reference(snapshot.field(description,INTERACTIONS_COMPUTED_RESOURCE,4)),owner);
                    emit(tape,refs.reference(snapshot.field(description,INTERACTIONS_COMPUTED_RESOURCE,2)),owner);
                    namePolicy(snapshot.field(description,INTERACTIONS_COMPUTED_RESOURCE,3),owner,tape,refs,capabilities);
                }
                default -> throw new IllegalStateException("resource description required");
            }
        }

        private void namePolicy(long policy,long owner,SnapshotDiagnosticTemplates tape,SnapshotReferenceLists refs,
                SnapshotCapabilities capabilities) {
            if(snapshot.shape(policy)==INTERACTIONS_EXTENSION_NAME)
                capabilities.declared(snapshot.field(policy,INTERACTIONS_EXTENSION_NAME,0),snapshot.field(policy,INTERACTIONS_EXTENSION_NAME,1),owner,this::capabilityIssue);
            else if(snapshot.shape(policy)==INTERACTIONS_UNKNOWN_NAME)
                emit(tape,refs.reference(snapshot.field(policy,INTERACTIONS_UNKNOWN_NAME,0)),owner);
        }

        private void signatureAdmission(long signature,long owner,long unit,boolean entry,SnapshotIdentityKeys keys,
                SnapshotDeclarations declarations,SnapshotDiagnosticTemplates tape,SnapshotReferenceLists refs,
                SnapshotCapabilities capabilities) {
            long parameters=snapshot.field(signature,INTERACTIONS_SIGNATURE,0),results=snapshot.field(signature,INTERACTIONS_SIGNATURE,1);
            long parameterRemainder=snapshot.field(parameters,INTERACTIONS_PARAMETER_INVENTORY,1);
            if(snapshot.shape(parameterRemainder)==INTERACTIONS_UNKNOWN_REMAINDER)emit(tape,refs.reference(snapshot.field(parameterRemainder,INTERACTIONS_UNKNOWN_REMAINDER,0)),owner);
            long resultRemainder=snapshot.field(results,INTERACTIONS_RESULT_INVENTORY,1);
            if(snapshot.shape(resultRemainder)==INTERACTIONS_UNKNOWN_REMAINDER)emit(tape,refs.reference(snapshot.field(resultRemainder,INTERACTIONS_UNKNOWN_REMAINDER,0)),owner);
            try(var rows=snapshot.elements(snapshot.field(parameters,INTERACTIONS_PARAMETER_INVENTORY,0),INTERACTIONS_PARAMETER)) {
                while(rows.advance()) {
                    long parameter=rows.value();typeReference(snapshot.field(parameter,INTERACTIONS_PARAMETER,2),owner,keys,declarations,tape,refs,capabilities);
                    emit(tape,refs.reference(snapshot.field(parameter,INTERACTIONS_PARAMETER,4)),owner);
                    long mode=snapshot.field(parameter,INTERACTIONS_PARAMETER,1);
                    if(snapshot.shape(mode)==INTERACTIONS_UNKNOWN_MODE)emit(tape,refs.reference(snapshot.field(mode,INTERACTIONS_UNKNOWN_MODE,0)),owner);
                    long binding=snapshot.field(parameter,INTERACTIONS_PARAMETER,3);
                    if(snapshot.shape(binding)==INTERACTIONS_OBJECT_BINDING) {
                        long object=snapshot.field(binding,INTERACTIONS_OBJECT_BINDING,0);emit(tape,refs.reference(object),owner);
                        if(!entry)issue(ValidationIssue.Kind.INVALID_IR,"I-55",owner,"external signature cannot bind an object in the called unit");
                        else if(keys.key(snapshot.field(object,IDS_OBJECT_ID,0))!=keys.key(unit))issue(ValidationIssue.Kind.INVALID_IR,"I-08",owner,"parameter initialization object is not owned by entry unit");
                    } else if(snapshot.shape(binding)==INTERACTIONS_UNKNOWN_PARAMETER_BINDING) {
                        emit(tape,refs.reference(snapshot.field(binding,INTERACTIONS_UNKNOWN_PARAMETER_BINDING,0)),owner);
                        if(!entry)issue(ValidationIssue.Kind.INVALID_IR,"I-55",owner,"external signature binding is not applicable, not unknown");
                    } else if(snapshot.shape(binding)==INTERACTIONS_EXTERNAL_BINDING&&entry)
                        issue(ValidationIssue.Kind.INVALID_IR,"I-55",owner,"entry parameter must materialize or explicitly lack its object binding");
                }
            }
            try(var rows=snapshot.elements(snapshot.field(results,INTERACTIONS_RESULT_INVENTORY,0),INTERACTIONS_RESULT_SLOT)) {
                while(rows.advance()) {long result=rows.value();typeReference(snapshot.field(result,INTERACTIONS_RESULT_SLOT,1),owner,keys,declarations,tape,refs,capabilities);emit(tape,refs.reference(snapshot.field(result,INTERACTIONS_RESULT_SLOT,2)),owner);}
            }
        }

        private void typeReference(long ref,long owner,SnapshotIdentityKeys keys,SnapshotDeclarations declarations,
                SnapshotDiagnosticTemplates tape,SnapshotReferenceLists refs,SnapshotCapabilities capabilities) {
            if(snapshot.shape(ref)==TYPES_UNKNOWN_TYPE) {
                long uncertainty=snapshot.field(ref,TYPES_UNKNOWN_TYPE,0);emit(tape,refs.reference(uncertainty),owner);
                long declaration=declarations.fact(uncertainty,SnapshotDeclarations.Fact.NODE);
                if(declaration!=0&&!equal(snapshot.field(declaration,EVIDENCE_UNCERTAINTY,1),"TYPE_UNKNOWN"))
                    issue(ValidationIssue.Kind.INVALID_IR,"I-49",owner,"unknown_type requires TYPE_UNKNOWN uncertainty");
                return;
            }
            long type=snapshot.field(ref,TYPES_KNOWN,0);
            if(snapshot.shape(type)==TYPES_LABEL_TYPE) {
                long unit=snapshot.field(type,TYPES_LABEL_TYPE,0);emit(tape,refs.reference(unit),owner);
                capabilities.require("control.indirect","1",owner,this::capabilityIssue);
                try(var labels=snapshot.elements(snapshot.field(type,TYPES_LABEL_TYPE,1),IDS_LABEL_ID)) {
                    while(labels.advance()) {long label=labels.value();emit(tape,refs.reference(label),owner);if(keys.key(snapshot.field(label,IDS_LABEL_ID,0))!=keys.key(unit))issue(ValidationIssue.Kind.INVALID_IR,"I-08",owner,"label universe crosses unit");}
                }
            } else if(snapshot.shape(type)==TYPES_EXTENSION_TYPE) {
                if(equal(snapshot.field(type,TYPES_EXTENSION_TYPE,0),"unknown"))issue(ValidationIssue.Kind.INVALID_IR,"I-50",owner,"opaque_type cannot disguise unknown_type");
                capabilities.require(snapshot.field(type,TYPES_EXTENSION_TYPE,0),snapshot.field(type,TYPES_EXTENSION_TYPE,1),owner,this::capabilityIssue);
            }
        }

        private void codecReferences(long codec,long owner,SnapshotIdentityKeys keys,SnapshotDeclarations declarations,
                SnapshotDiagnosticTemplates tape,SnapshotReferenceLists refs,SnapshotCapabilities capabilities) {
            if(snapshot.shape(codec)==MEMORY_UNKNOWN_CODEC) {typeReference(snapshot.field(codec,MEMORY_UNKNOWN_CODEC,0),owner,keys,declarations,tape,refs,capabilities);emit(tape,refs.reference(snapshot.field(codec,MEMORY_UNKNOWN_CODEC,1)),owner);}
            else if(snapshot.shape(codec)==MEMORY_EXTENSION_CODEC) {capabilities.require(snapshot.field(codec,MEMORY_EXTENSION_CODEC,0),snapshot.field(codec,MEMORY_EXTENSION_CODEC,1),owner,this::capabilityIssue);typeReference(snapshot.field(codec,MEMORY_EXTENSION_CODEC,2),owner,keys,declarations,tape,refs,capabilities);}
        }

        private boolean qualified(long value) {
            long length=snapshot.characterCount(value);if(length<3)return false;boolean dot=false;
            char[] block=new char[256];long offset=0;
            while(offset<length){int count=snapshot.readCharacters(value,offset,block,0,(int)Math.min(block.length,length-offset));for(int i=0;i<count;i++){char c=block[i];long at=offset+i;if(Character.isWhitespace(c))return false;if(c=='.'){if(at==0||at==length-1)return false;dot=true;}}offset+=count;}
            return dot;
        }

        private boolean equal(long source,String value){if(snapshot.characterCount(source)!=value.length())return false;char[] chars=value.toCharArray(),actual=new char[chars.length];if(actual.length!=0)snapshot.readCharacters(source,0,actual,0,actual.length);return java.util.Arrays.equals(chars,actual);}
        private void capabilityIssue(SnapshotCapabilities.Rule rule,long owner,long capability) {
            String detail=switch(rule){
                case DUPLICATE->"duplicate capability name";
                case PROFILE->"declared profile requires separate oracle evidence";
                case NAME_POLICY->"name interpretation requires the declared external policy; structural validation does not interpret it";
                case UNSUPPORTED->"extension semantic contract is not implemented by this validator";
                case MISSING->"used capability missing from required manifest";
                case NAME_POLICY_SURFACE->"name-policy capability cannot supply another extension surface";
            };
            issue(rule.kind(),rule.code(),owner,detail);
        }
        private long unitOfOperand(long operand) {
            long owner=snapshot.field(operand,IDS_OPERAND_ID,0),site=snapshot.field(owner,snapshot.shape(owner),0);
            return snapshot.field(site,snapshot.shape(site),0);
        }
        private void localLabel(SnapshotDiagnosticTemplates tape,SnapshotReferenceLists refs,long label,long unit,long owner,SnapshotIdentityKeys keys) {
            emit(tape,refs.reference(label),owner);
            if(keys.key(snapshot.field(label,IDS_LABEL_ID,0))!=keys.key(unit))
                issue(ValidationIssue.Kind.INVALID_IR,"I-02",owner,"local control target crosses unit");
        }
        private void optionalReference(SnapshotDiagnosticTemplates tape,SnapshotReferenceLists refs,long optional,AirShape element,long owner) {
            if(snapshot.size(optional)!=0)emit(tape,refs.reference(snapshot.element(optional,element,0)),owner);
        }
        private void emit(SnapshotDiagnosticTemplates tape,long root,long owner) {
            tape.emit(root,owner,new TemplateReports());
        }
        private void retain(ValidationIssue.Kind kind,String rule,long owner,String detail) {
            if(retained.size()<options.maximumIssues())retained.add(new ValidationIssue(kind,rule,Optional.of(id(owner)),detail));
        }
        private final class TemplateReports implements SnapshotDiagnosticTemplates.Reports {
            public long remaining(){return Math.max(0L,(long)options.maximumIssues()-retained.size());}
            public void occurrences(ValidationIssue.Kind kind,long owner,long amount){add(kind,amount);}
            public void retain(ValidationIssue.Kind kind,int token,long owner,long anchor,int field,long detail) {
                String rule=token==SnapshotLocalLabels.Rule.DANGLING.token()||token==SnapshotLocalLabels.Rule.CROSS_UNIT.token()?"I-02":annotationRule(token);
                String message=switch(token) {
                    case 1->"dangling reference";case 2->"local control target crosses unit";
                    case 100,101,102,103,104->"open/unavailable precision requires reasons";
                    case 110->"partial/unavailable inventory needs explicit reason";
                    case 111->"duplicate source inventory key";
                    case 112->"coverage item disappeared without output, uncertainty or justified elimination";
                    case 113->"incomplete coverage item lacks uncertainty";
                    default->throw new IllegalStateException("unknown snapshot diagnostic token "+token);
                };
                Run.this.retain(kind,rule,owner,message);
            }
        }
        private final class SignatureReports implements SnapshotSignatureIndex.Reports {
            public long remaining(){return Math.max(0L,(long)options.maximumIssues()-retained.size());}
            public void occurrences(SnapshotSignatureIndex.Rule rule,long owner,long amount){add(ValidationIssue.Kind.INVALID_IR,amount);}
            public void retain(SnapshotSignatureIndex.Rule rule,long owner,long row,long position,long ordinal) {
                Run.this.retain(ValidationIssue.Kind.INVALID_IR,rule.code(),owner,
                    rule==SnapshotSignatureIndex.Rule.POSITIONS?"signature positions must be unique/ordered and contiguous when closed":"entry state references a parameter position not materialized in the signature");
            }
        }
        private static String annotationRule(int token) {
            for(var rule:SnapshotAnnotationTemplates.Rule.values())if(rule.token()==token)return rule.code();
            throw new IllegalStateException("unknown annotation rule token "+token);
        }
        private void add(ValidationIssue.Kind kind,long amount) {
            if(amount<0)throw new IllegalStateException("negative diagnostic count");
            try{counts.merge(kind,amount,Math::addExact);}catch(ArithmeticException overflow){throw new OperationalLimit("diagnostic counter representability");}
        }

        private boolean airVersion() {
            long version=snapshot.field(snapshot.root(),PUBLICATION,1);
            try(var keys=new SnapshotIdentityKeys(snapshot,owned(storage.identities()))) {
                return keys.integerEqualsNatural(snapshot.field(version,SEMANTIC_VERSION,0),2)
                    &&keys.integerEqualsNatural(snapshot.field(version,SEMANTIC_VERSION,1),0)
                    &&keys.integerEqualsNatural(snapshot.field(version,SEMANTIC_VERSION,2),0);
            }
        }
        private void resourceLimit(String detail) {
            traversalCompleted=false;
            count(ValidationIssue.Kind.RESOURCE_LIMIT);
            retained.add(new ValidationIssue(ValidationIssue.Kind.RESOURCE_LIMIT,"ANALYSIS_LIMIT",
                Optional.of(publicationId()),detail));
        }
        private void issue(ValidationIssue.Kind kind,String rule,long subject,String detail) {
            count(kind);
            if(retained.size()<options.maximumIssues())retained.add(new ValidationIssue(kind,rule,
                subject==0?Optional.empty():Optional.of(id(subject)),detail));
        }
        private void issue(ValidationIssue.Kind kind,String rule,Ids.Id subject,String detail) {
            count(kind);
            if(retained.size()<options.maximumIssues())retained.add(new ValidationIssue(kind,rule,Optional.ofNullable(subject),detail));
        }
        private void count(ValidationIssue.Kind kind) {
            try{counts.merge(kind,1L,Math::addExact);}catch(ArithmeticException overflow){throw new OperationalLimit("diagnostic counter representability");}
        }
        private ValidationResult result() {
            return new ValidationResult(retained,
                new ValidationResult.Statistics(Math.toIntExact(entities),Math.toIntExact(operands),Math.toIntExact(operations),0),
                new ValidationResult.Diagnostics(counts,traversalCompleted));
        }
        private Ids.PublicationId publicationId(){return (Ids.PublicationId)id(snapshot.field(snapshot.root(),PUBLICATION,0));}
        private Ids.Id id(long source) {
            AirShape shape=snapshot.shape(source);
            return switch(shape) {
                case IDS_PUBLICATION_ID->new Ids.PublicationId(text(snapshot.field(source,shape,0)));
                case IDS_UNIT_ID->new Ids.UnitId((Ids.PublicationId)id(snapshot.field(source,shape,0)),text(snapshot.field(source,shape,1)));
                case IDS_STORAGE_ID->new Ids.StorageId((Ids.PublicationId)id(snapshot.field(source,shape,0)),text(snapshot.field(source,shape,1)));
                case IDS_RESOURCE_ID->new Ids.ResourceId((Ids.PublicationId)id(snapshot.field(source,shape,0)),text(snapshot.field(source,shape,1)));
                case IDS_ARTIFACT_ID->new Ids.ArtifactId((Ids.PublicationId)id(snapshot.field(source,shape,0)),text(snapshot.field(source,shape,1)));
                case IDS_ORIGIN_ID->new Ids.OriginId((Ids.PublicationId)id(snapshot.field(source,shape,0)),text(snapshot.field(source,shape,1)));
                case IDS_UNCERTAINTY_ID->new Ids.UncertaintyId((Ids.PublicationId)id(snapshot.field(source,shape,0)),text(snapshot.field(source,shape,1)));
                case IDS_PREMISE_ID->new Ids.PremiseId((Ids.PublicationId)id(snapshot.field(source,shape,0)),text(snapshot.field(source,shape,1)));
                case IDS_ARTIFACT_RELATION_ID->new Ids.ArtifactRelationId((Ids.PublicationId)id(snapshot.field(source,shape,0)),text(snapshot.field(source,shape,1)));
                case IDS_ENTRY_ID->new Ids.EntryId((Ids.UnitId)id(snapshot.field(source,shape,0)),text(snapshot.field(source,shape,1)));
                case IDS_LABEL_ID->new Ids.LabelId((Ids.UnitId)id(snapshot.field(source,shape,0)),text(snapshot.field(source,shape,1)));
                case IDS_OPERATION_ID->new Ids.OperationId((Ids.UnitId)id(snapshot.field(source,shape,0)),text(snapshot.field(source,shape,1)));
                case IDS_OBJECT_ID->new Ids.ObjectId((Ids.UnitId)id(snapshot.field(source,shape,0)),text(snapshot.field(source,shape,1)));
                case IDS_COMPLETION_PORT_ID->new Ids.CompletionPortId((Ids.UnitId)id(snapshot.field(source,shape,0)),text(snapshot.field(source,shape,1)));
                case IDS_OPERAND_ID->new Ids.OperandId(owner(snapshot.field(source,shape,0)),text(snapshot.field(source,shape,1)));
                default->throw new IllegalArgumentException("AIR Id source required");
            };
        }
        private Ids.OperandOwner owner(long source) {
            return switch(snapshot.shape(source)) {
                case IDS_OPERATION_OWNER->new Ids.OperationOwner((Ids.OperationId)id(snapshot.field(source,IDS_OPERATION_OWNER,0)));
                case IDS_ENTRY_OWNER->new Ids.EntryOwner((Ids.EntryId)id(snapshot.field(source,IDS_ENTRY_OWNER,0)));
                default->throw new IllegalArgumentException("AIR OperandOwner source required");
            };
        }
        private String text(long source) {
            long length=snapshot.characterCount(source);
            if(length>Integer.MAX_VALUE)throw new OperationalLimit("retained diagnostic text representability");
            var result=new StringBuilder((int)length);var block=new char[1024];long offset=0;
            while(offset<length){int count=snapshot.readCharacters(source,offset,block,0,(int)Math.min(block.length,length-offset));result.append(block,0,count);offset+=count;}
            return result.toString();
        }
        private static <T>T owned(T value){return Objects.requireNonNull(value,"snapshot validation storage port");}
    }
}
