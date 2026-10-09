package io.github.gustavo2358.air.validation;

import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import java.util.*;
import java.util.function.LongFunction;
import static io.github.gustavo2358.air.model.AirShape.*;

/** Borrowed native bodies. Address/ID indexes remain resident metadata, not a global spill claim. */
final class SnapshotValidationProgram implements ValidationProgram {
    private final AirSnapshot snapshot;
    private final SnapshotDeclarations declarations;
    private final SnapshotOccurrenceReader reader;
    SnapshotValidationProgram(AirSnapshot snapshot,SnapshotDeclarations declarations) {
        this.snapshot=Objects.requireNonNull(snapshot);this.declarations=Objects.requireNonNull(declarations);
        reader=new SnapshotOccurrenceReader(snapshot,this::open);
    }
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
    <K,V> AddressMap<K,V> map(Class<V> type){return new AddressMap<>(h->read(h,type));}
    AddressMap<UnitId,UnitView> unitMap(){return new AddressMap<>(this::unit);}
    AddressMap<LabelId,SequenceView> sequenceMap(){return new AddressMap<>(this::sequence);}
    final class AddressMap<K,V> extends AbstractMap<K,V> {
        private final Map<K,Long> addresses=new LinkedHashMap<>();
        private final LongFunction<V> decode;
        AddressMap(LongFunction<V> decode){this.decode=decode;}
        void address(K key,long node){addresses.putIfAbsent(key,node);}
        @Override public V get(Object key){open();var node=addresses.get(key);return node==null?null:decode.apply(node);}
        @Override public boolean containsKey(Object key){open();return addresses.containsKey(key);}
        @Override public int size(){open();return addresses.size();}
        @Override public Set<K> keySet(){open();return Collections.unmodifiableSet(addresses.keySet());}
        @Override public Set<Map.Entry<K,V>> entrySet(){
            return new AbstractSet<>() {
                @Override public int size(){return AddressMap.this.size();}
                @Override public Iterator<Map.Entry<K,V>> iterator(){
                    var rows=addresses.entrySet().iterator();
                    return new Iterator<>() {
                        public boolean hasNext(){open();return rows.hasNext();}
                        public Map.Entry<K,V> next(){open();var row=rows.next();return new SimpleImmutableEntry<>(row.getKey(),decode.apply(row.getValue()));}
                    };
                }
            };
        }
    }
    @SuppressWarnings("unchecked")
    private static <K,V> void address(Map<K,V> map,K key,long node){((SnapshotValidationProgram.AddressMap<K,V>)map).address(key,node);}
    void index(PublicationIndex index) {
        for(long at=0;at<declarations.entities();at++) {
            long node=declarations.declaration(at,SnapshotDeclarations.Fact.NODE),identity=declarations.declaration(at,SnapshotDeclarations.Fact.IDENTITY);
            var id=read(identity,Id.class);index.add(id);
            switch(id) {
                case UnitId unit->address(index.units,unit,node);
                case ObjectId object->address(index.objects,object,node);
                case StorageId storage->address(index.storage,storage,node);
                case EntryId entry->address(index.entries,entry,node);
                case LabelId label->address(index.sequences,label,node);
                case OperationId operation->{address(index.operations,operation,node);long label=declarations.declaration(at,SnapshotDeclarations.Fact.SEQUENCE);if(label!=0)index.sequenceOf.put(operation,read(label,LabelId.class));}
                case OperandId operand->address(index.operands,operand,node);
                case OriginId origin->address(index.origins,origin,node);
                case UncertaintyId uncertainty->address(index.uncertainties,uncertainty,node);
                case PremiseId premise->address(index.premises,premise,node);
                case ResourceId resource->address(index.resources,resource,node);
                case ArtifactRelationId relation->address(index.artifactRelations,relation,node);
                default->{ }
            }
        }
    }
}
