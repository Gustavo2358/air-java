package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;
import java.util.function.LongFunction;
import static io.github.gustavo2358.air.model.AirShape.*;

/** Borrowed native bodies and canonical declaration rows; no second typed-ID catalogue. */
final class SnapshotValidationProgram implements ValidationProgram {
    private final AirSnapshot snapshot;
    private final SnapshotDeclarations declarations;
    private final SnapshotVisibleObjects visible;
    private final SnapshotOccurrenceReader reader;
    private final long[] counts=new long[AirShape.values().length];
    private SnapshotValidationProgram(AirSnapshot snapshot,SnapshotDeclarations declarations,SnapshotVisibleObjects visible) {
        this.snapshot=Objects.requireNonNull(snapshot);this.declarations=Objects.requireNonNull(declarations);
        this.visible=Objects.requireNonNull(visible);
        reader=new SnapshotOccurrenceReader(snapshot,this::open);
        for(long at=0;at<declarations.entities();at++)
            counts[snapshot.shape(declarations.declaration(at,SnapshotDeclarations.Fact.IDENTITY)).ordinal()]++;
    }
    /**
     * Internal handoff ONLY after immutable primitive I-01/I-03/I-11 and nominal-cycle passes,
     * with no invalid/unsupported/operational issue. Not a public validity certificate: all
     * remaining general AIR rules still run. SnapshotValidator is the production caller.
     */
    static SnapshotValidationProgram afterPrimitiveAdmission(AirSnapshot snapshot,SnapshotDeclarations declarations,SnapshotVisibleObjects visible) {
        return new SnapshotValidationProgram(snapshot,declarations,visible);
    }
    boolean objectVisible(UnitId unit,ObjectId object){open();return visible.contains(unit,object);}
    private void open(){snapshot.shape(snapshot.root());}
    private long field(long node,int at){return snapshot.field(node,snapshot.shape(node),at);}
    private <T> T read(long node,Class<T> type){return reader.read(node,type);}
    private <T> List<T> list(long node,AirShape shape,LongFunction<T> read) {
        return new AbstractList<>() {
            @Override public int size(){open();return Math.toIntExact(snapshot.size(node));}
            @Override public T get(int at){Objects.checkIndex(at,size());return read.apply(snapshot.element(node,shape,at));}
        };
    }
    private <T> List<T> list(long node,AirShape shape,Class<T> type){return list(node,shape,h->read(h,type));}
    private <T> Optional<T> optional(long node,AirShape shape,Class<T> type){return snapshot.size(node)==0?Optional.empty():Optional.of(read(snapshot.element(node,shape,0),type));}
    public PublicationId id(){return read(field(snapshot.root(),0),PublicationId.class);}
    public SemanticVersion airVersion(){return read(field(snapshot.root(),1),SemanticVersion.class);}
    public Capabilities.Manifest capabilities(){return read(field(snapshot.root(),2),Capabilities.Manifest.class);}
    public List<Origins.Artifact> artifacts(){return list(field(snapshot.root(),3),ORIGINS_ARTIFACT,Origins.Artifact.class);}
    public List<UnitView> units(){return list(field(snapshot.root(),4),UNIT,this::unit);}
    public List<Memory.Storage> storage(){return list(field(snapshot.root(),5),MEMORY_STORAGE,Memory.Storage.class);}
    public List<Interactions.Resource> resources(){return list(field(snapshot.root(),6),INTERACTIONS_RESOURCE,Interactions.Resource.class);}
    public List<Artifacts.Relation> artifactRelations(){return list(field(snapshot.root(),7),ARTIFACTS_RELATION,Artifacts.Relation.class);}
    public List<Origins.Origin> origins(){return list(field(snapshot.root(),8),ORIGINS_ORIGIN,Origins.Origin.class);}
    public CoverageView coverage(){return coverage(field(snapshot.root(),9));}
    public List<Evidence.Uncertainty> uncertainties(){return list(field(snapshot.root(),10),EVIDENCE_UNCERTAINTY,Evidence.Uncertainty.class);}
    public List<Proofs.Premise> premises(){return list(field(snapshot.root(),11),PROOFS_PREMISE,Proofs.Premise.class);}
    private UnitView unit(long h) {
        return new UnitView() {
            public UnitId id(){return read(field(h,0),UnitId.class);}
            public Optional<UnitId> containingUnit(){return optional(field(h,1),IDS_UNIT_ID,UnitId.class);}
            public List<Memory.ObjectDeclaration> objects(){return list(field(h,2),MEMORY_OBJECT_DECLARATION,Memory.ObjectDeclaration.class);}
            public List<ObjectId> visibleObjects(){return list(field(h,3),IDS_OBJECT_ID,ObjectId.class);}
            public List<Entries.Entry> entries(){return list(field(h,4),ENTRIES_ENTRY,Entries.Entry.class);}
            public List<SequenceView> sequences(){return list(field(h,5),SEQUENCE,SnapshotValidationProgram.this::sequence);}
            public List<Entries.CompletionPort> completionPorts(){return list(field(h,6),ENTRIES_COMPLETION_PORT,Entries.CompletionPort.class);}
            public Unit.BodyAvailability body(){return read(field(h,7),Unit.BodyAvailability.class);}
            public Optional<UncertaintyId> bodyUnavailable(){return optional(field(h,8),IDS_UNCERTAINTY_ID,UncertaintyId.class);}
            public CoverageView coverage(){return SnapshotValidationProgram.this.coverage(field(h,9));}
            public OriginId origin(){return read(field(h,10),OriginId.class);}
        };
    }
    private SequenceView sequence(long h) {
        return new SequenceView() {
            public LabelId label(){return read(field(h,0),LabelId.class);}
            public List<Instruction> instructions(){return list(field(h,1),INSTRUCTION,Instruction.class);}
            public Terminator terminator(){return read(field(h,2),Terminator.class);}
            public OriginId origin(){return read(field(h,3),OriginId.class);}
        };
    }
    private CoverageView coverage(long h) {
        return new CoverageView() {
            public Evidence.InventoryStatus inventory(){return read(field(h,0),Evidence.InventoryStatus.class);}
            public Scopes.FactScope scope(){return read(field(h,1),Scopes.FactScope.class);}
            public List<Evidence.CoverageItem> items(){return list(field(h,2),EVIDENCE_COVERAGE_ITEM,Evidence.CoverageItem.class);}
            public List<UncertaintyId> uncertainties(){return list(field(h,3),IDS_UNCERTAINTY_ID,UncertaintyId.class);}
        };
    }
    <K extends Id,V> AddressMap<K,V> map(Class<V> type){
        AirShape identity;
        if(type==Memory.ObjectDeclaration.class)identity=IDS_OBJECT_ID;
        else if(type==Memory.Storage.class)identity=IDS_STORAGE_ID;
        else if(type==Entries.Entry.class)identity=IDS_ENTRY_ID;
        else if(type==Operation.class)identity=IDS_OPERATION_ID;
        else if(type==Operand.class)identity=IDS_OPERAND_ID;
        else if(type==Origins.Origin.class)identity=IDS_ORIGIN_ID;
        else if(type==Evidence.Uncertainty.class)identity=IDS_UNCERTAINTY_ID;
        else if(type==Proofs.Premise.class)identity=IDS_PREMISE_ID;
        else if(type==Interactions.Resource.class)identity=IDS_RESOURCE_ID;
        else if(type==Artifacts.Relation.class)identity=IDS_ARTIFACT_RELATION_ID;
        else throw new IllegalArgumentException("uncatalogued validation declaration domain");
        return new AddressMap<>(identity,SnapshotDeclarations.Fact.NODE,h->read(h,type));
    }
    AddressMap<UnitId,UnitView> unitMap(){return new AddressMap<>(IDS_UNIT_ID,SnapshotDeclarations.Fact.NODE,this::unit);}
    AddressMap<LabelId,SequenceView> sequenceMap(){return new AddressMap<>(IDS_LABEL_ID,SnapshotDeclarations.Fact.NODE,this::sequence);}
    AddressMap<OperationId,LabelId> sequenceOfMap(){return new AddressMap<>(IDS_OPERATION_ID,SnapshotDeclarations.Fact.SEQUENCE,h->read(h,LabelId.class));}
    Set<Id> identities(){return Collections.unmodifiableSet(new AbstractSet<>() {
        public int size(){open();return cardinality(declarations.entities());}
        public boolean contains(Object key){open();return key instanceof Id id&&declarations.fact(id,SnapshotDeclarations.Fact.NODE)!=0;}
        public Iterator<Id> iterator(){return identityRows(null);}
    });}
    private static int cardinality(long count){return (int)Math.min(Integer.MAX_VALUE,count);}
    private boolean domain(Id id,AirShape shape){
        return switch(shape) {
            case IDS_UNIT_ID->id instanceof UnitId;case IDS_LABEL_ID->id instanceof LabelId;
            case IDS_OBJECT_ID->id instanceof ObjectId;case IDS_STORAGE_ID->id instanceof StorageId;
            case IDS_ENTRY_ID->id instanceof EntryId;case IDS_OPERATION_ID->id instanceof OperationId;
            case IDS_OPERAND_ID->id instanceof OperandId;case IDS_ORIGIN_ID->id instanceof OriginId;
            case IDS_UNCERTAINTY_ID->id instanceof UncertaintyId;case IDS_PREMISE_ID->id instanceof PremiseId;
            case IDS_RESOURCE_ID->id instanceof ResourceId;case IDS_ARTIFACT_RELATION_ID->id instanceof ArtifactRelationId;
            default->throw new IllegalArgumentException("validation index domain required");
        };
    }
    private <K extends Id> Iterator<K> identityRows(AirShape domain) {
        return new Iterator<>() {
            private long next,identity;
            public boolean hasNext(){
                open();while(identity==0&&next<declarations.entities()) {
                    long candidate=declarations.declaration(next++,SnapshotDeclarations.Fact.IDENTITY);
                    if(domain==null||snapshot.shape(candidate)==domain)identity=candidate;
                }
                return identity!=0;
            }
            @SuppressWarnings("unchecked")
            public K next(){if(!hasNext())throw new NoSuchElementException();long node=identity;identity=0;return (K)read(node,Id.class);}
        };
    }
    final class AddressMap<K extends Id,V> extends AbstractMap<K,V> {
        private final AirShape domain;
        private final SnapshotDeclarations.Fact fact;
        private final LongFunction<V> decode;
        AddressMap(AirShape domain,SnapshotDeclarations.Fact fact,LongFunction<V> decode){this.domain=domain;this.fact=fact;this.decode=decode;}
        private long address(Object key){open();return key instanceof Id id&&domain(id,domain)?declarations.fact(id,fact):0;}
        @Override public V get(Object key){long node=address(key);return node==0?null:decode.apply(node);}
        @Override public boolean containsKey(Object key){return address(key)!=0;}
        @Override public int size(){open();return cardinality(counts[domain.ordinal()]);}
        @Override public Set<K> keySet(){return Collections.unmodifiableSet(new AbstractSet<>() {
            public int size(){return AddressMap.this.size();}public boolean contains(Object key){return containsKey(key);}
            public Iterator<K> iterator(){return identityRows(domain);}
        });}
        @Override public Set<Map.Entry<K,V>> entrySet(){
            return new AbstractSet<>() {
                @Override public int size(){return AddressMap.this.size();}
                @Override public Iterator<Map.Entry<K,V>> iterator(){
                    Iterator<K> rows=identityRows(domain);
                    return new Iterator<>() {
                        public boolean hasNext(){open();return rows.hasNext();}
                        public Map.Entry<K,V> next(){open();K key=rows.next();return new SimpleImmutableEntry<>(key,get(key));}
                    };
                }
            };
        }
    }
}
