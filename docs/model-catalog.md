# Catálogo do modelo Java

Gerado a partir das assinaturas da entrega; informativo. A semântica permanece na AIR fixada.
Os records copiam listas defensivamente. Campos obrigatórios são não nulos; `Optional`
é um valor não nulo. Identidades são records tipados; `Operation`/`Expression`/`Place` são selados.

| Tipo | Campos |
| --- | --- |
| `Artifacts.InternalArtifact` | `ArtifactId artifact` |
| `Artifacts.ExternalArtifact` | `Interactions.LiteralTarget resource` |
| `Artifacts.Relation` | `RelationId id, ArtifactId source, RelationTarget destination, String kind, OriginId origin, Evidence.CoverageStatus coverage` |
| `Capabilities.Capability` | `String name, int major` |
| `Capabilities.Manifest` | `List<Capability> required, List<Capability> provided` |
| `Control.Handler` | `LabelId label` |
| `Control.Normal` | `LabelId label` |
| `Control.Exceptional` | `String tag, ExceptionDestination destination` |
| `Control.AnyException` | `ExceptionDestination destination` |
| `Control.Envelope` | `List<Alternative> known, Scopes.ControlBound remainder` |
| `Control.ExceptionOutcome` | `String tag` |
| `Control.Before` | `OperationId operation` |
| `Control.After` | `OperationId operation, OutcomeKey outcome` |
| `Control.EntryPoint` | `EntryId entry` |
| `Control.ExitPoint` | `UnitId unit, OutcomeKey outcome` |
| `Entries.CompletionPort` | `CompletionPortId id, OriginId origin` |
| `Entries.LiteralInitial` | `Expressions.Literal value` |
| `Entries.ParameterInitial` | `int position` |
| `Entries.ExternalUnknown` | `UncertaintyId reason` |
| `Entries.Uninitialized` | `UncertaintyId reason` |
| `Entries.InitialCondition` | `Place place, InitialValue value, OriginId origin, List<PremiseId> premises` |
| `Entries.EntryState` | `List<InitialCondition> conditions, List<UncertaintyId> uncertainties` |
| `Entries.Entry` | `EntryId id, Optional<LabelId> initialLabel, Interactions.Signature signature, EntryState state, OriginId origin` |
| `Envelopes.MemoryEnvelope` | `List<OperandId> knownReads, Scopes.MemoryBound otherReads, List<OperandId> knownWrites, Scopes.MemoryBound otherWrites, List<OperandId> mustOverwrite` |
| `Envelopes.ResourceUse` | `String action, Interactions.Target target, Control.ProgramPoint point, OriginId origin` |
| `Envelopes.DependencyEnvelope` | `List<ResourceUse> known, Scopes.DependencyBound remainder` |
| `Envelopes.Envelope` | `MemoryEnvelope memory, Control.Envelope control, DependencyEnvelope dependencies` |
| `Evidence.Claim` | `Scopes.FactScope scope, PrecisionStatus status, List<UncertaintyId> reasons` |
| `Evidence.Precision` | `Claim control, Claim storage, Claim effects, Claim values, Claim dependencies` |
| `Evidence.Uncertainty` | `UncertaintyId id, String code, List<Dimension> dimensions, Scopes.FactScope scope, String reason, OriginId origin` |
| `Evidence.Elimination` | `PremiseId justification, String rule` |
| `Evidence.CoverageItem` | `String sourceKey, OriginId origin, CoverageStatus status, List<Id> outputs, List<UncertaintyId> uncertainties, Optional<Elimination> elimination` |
| `Evidence.Coverage` | `InventoryStatus inventory, Scopes.FactScope scope, List<CoverageItem> items, List<UncertaintyId> uncertainties` |
| `Expressions.Literal` | `Operand.Header header, Values.LiteralValue value` |
| `Expressions.Read` | `Operand.Header header, Place place` |
| `Expressions.Unknown` | `Operand.Header header, Types.TypeRef typeRef, List<Expression> dependencies, Scopes.MemoryBound remainingReads, UncertaintyId reason` |
| `Expressions.Unary` | `Operand.Header header, UnaryOperator operator, Expression argument` |
| `Expressions.Binary` | `Operand.Header header, BinaryOperator operator, Expression left, Expression right` |
| `Expressions.Quantize` | `Operand.Header header, Expression value, int scale, Rounding rounding` |
| `Expressions.FitText` | `Operand.Header header, Expression value, BigInteger length, String pad` |
| `Expressions.SliceText` | `Operand.Header header, Expression value, Expression start, Expression count, Optional<PremiseId> boundsProof` |
| `Expressions.TrimRight` | `Operand.Header header, Expression value, String characters` |
| `Ids.PublicationId` | `String localId` |
| `Ids.UnitId` | `PublicationId publication, String localId` |
| `Ids.StorageId` | `PublicationId publication, String localId` |
| `Ids.ResourceId` | `PublicationId publication, String localId` |
| `Ids.ArtifactId` | `PublicationId publication, String localId` |
| `Ids.OriginId` | `PublicationId publication, String localId` |
| `Ids.UncertaintyId` | `PublicationId publication, String localId` |
| `Ids.PremiseId` | `PublicationId publication, String localId` |
| `Ids.ContractId` | `PublicationId publication, String localId` |
| `Ids.RelationId` | `PublicationId publication, String localId` |
| `Ids.EntryId` | `UnitId unit, String localId` |
| `Ids.LabelId` | `UnitId unit, String localId` |
| `Ids.OperationId` | `UnitId unit, String localId` |
| `Ids.ObjectId` | `UnitId unit, String localId` |
| `Ids.CompletionPortId` | `UnitId unit, String localId` |
| `Ids.OperationOwner` | `OperationId operation` |
| `Ids.EntryOwner` | `EntryId entry` |
| `Ids.OperandId` | `OperandOwner owner, String localId` |
| `Interactions.ContractName` | `ContractId contract` |
| `Interactions.UnknownName` | `UncertaintyId uncertainty` |
| `Interactions.InternalTarget` | `EntryId entry` |
| `Interactions.LiteralTarget` | `String category, String namespace, String name, NamePolicy namePolicy, OriginId origin` |
| `Interactions.ComputedTarget` | `String category, String namespace, Expression name, NamePolicy namePolicy, OriginId origin` |
| `Interactions.ResourceTarget` | `ResourceId resource` |
| `Interactions.Resource` | `ResourceId id, LiteralTarget description` |
| `Interactions.ValueArgument` | `Expression value` |
| `Interactions.CopyArgument` | `Expression value` |
| `Interactions.ReferenceArgument` | `Place place` |
| `Interactions.Parameter` | `int position, PassingMode mode, Types.TypeRef typeRef, Optional<ObjectId> object, OriginId origin` |
| `Interactions.ResultSlot` | `int position, Types.TypeRef typeRef, OriginId origin` |
| `Interactions.Signature` | `List<Parameter> parameters, List<ResultSlot> results, Optional<UncertaintyId> incomplete` |
| `Interactions.ForeignEffects` | `Scopes.MemoryBound reads, Scopes.MemoryBound writes, List<OperandId> mustOverwrite` |
| `Interactions.OutcomeEffects` | `Control.OutcomeKey outcome, ForeignEffects effects` |
| `Interactions.EffectBound` | `ForeignEffects otherwise, List<OutcomeEffects> perOutcome` |
| `Interactions.Contract` | `ContractId id, String authority, SemanticVersion version, Signature signature, Optional<EffectBound> effects, List<PremiseId> premises, List<UncertaintyId> uncertainties, OriginId origin` |
| `Interactions.ContractKnowledge` | `Optional<ContractId> contract, Optional<UncertaintyId> unknown` |
| `Interactions.EntrySignature` | `EntryId entry` |
| `Interactions.ExternalSignature` | `ContractId contract` |
| `Memory.BinaryCodec` | `boolean signed, int width, ByteOrder order` |
| `Memory.ExtensionCodec` | `String name, SemanticVersion version, Types.TypeRef logicalType, ContractId contract` |
| `Memory.UnknownCodec` | `Types.TypeRef logicalType, UncertaintyId reason` |
| `Memory.CellBinding` | `StorageId storage` |
| `Memory.ViewBinding` | `StorageId region, BigInteger offset, BigInteger extent, Codec codec` |
| `Memory.AliasBinding` | `ObjectId object` |
| `Memory.AlternativesBinding` | `List<Binding> alternatives, Scopes.MemoryBound remainder` |
| `Memory.UnknownBinding` | `Scopes.MemoryScope scope, UncertaintyId reason` |
| `Memory.StorageHeader` | `StorageId id, Optional<UnitId> owner, Lifetime lifetime, Visibility visibility, OriginId origin` |
| `Memory.Cell` | `StorageHeader header, Types.TypeRef typeRef` |
| `Memory.Region` | `StorageHeader header, Optional<BigInteger> extent, Optional<UncertaintyId> extentUnknown` |
| `Memory.ObjectDeclaration` | `ObjectId id, Optional<String> displayName, Types.TypeRef typeRef, Binding storage, Visibility visibility, OriginId origin, Evidence.CoverageStatus coverage, Evidence.Precision precision` |
| `Memory.ByteRange` | `StorageId region, Expression offset, Expression extent, Optional<PremiseId> boundsProof` |
| `Operations.Header` | `OperationId id, OriginId origin, Evidence.CoverageStatus coverage, Evidence.Precision precision, List<UncertaintyId> uncertainties` |
| `Operations.Assign` | `Header header, Place destination, Expression value` |
| `Operations.HavocMust` | `Header header, Place destination, UncertaintyId reason` |
| `Operations.HavocMay` | `Header header, Scopes.MemoryScope scope, UncertaintyId reason` |
| `Operations.Nop` | `Header header` |
| `Operations.CopyBytes` | `Header header, Memory.ByteRange destination, Memory.ByteRange source, BigInteger length, Envelopes.Envelope fallback` |
| `Operations.Jump` | `Header header, LabelId destination` |
| `Operations.Branch` | `Header header, Expression predicate, LabelId trueDestination, LabelId falseDestination` |
| `Operations.Case` | `Values.LiteralValue value, LabelId destination` |
| `Operations.Dispatch` | `Header header, Expression selector, List<Case> cases, LabelId defaultDestination` |
| `Operations.Invoke` | `Header header, String action, Interactions.Target target, List<Interactions.Argument> arguments, List<Place> results, Interactions.EffectBound effectBound, Control.Envelope outcomes, Interactions.ContractKnowledge contract, List<UncertaintyId> signatureGaps` |
| `Operations.Return` | `Header header, List<Expression> values, List<EntryId> entryScope` |
| `Operations.Raise` | `Header header, String tag, List<Expression> values` |
| `Operations.Halt` | `Header header, HaltKind haltKind` |
| `Operations.Opaque` | `Header header, String observedKind, List<Operand> knownOperands, List<Place> valueResults, Envelopes.Envelope envelope` |
| `Operations.LocalInvoke` | `Header header, LabelId entry, List<CompletionPortId> completionPorts, LabelId resume, Envelopes.Envelope fallback` |
| `Operations.LocalBoundary` | `Header header, CompletionPortId port, LabelId defaultDestination, Envelopes.Envelope fallback` |
| `Operations.LocalResume` | `Header header, Envelopes.Envelope fallback` |
| `Operations.LocalUnwind` | `Header header, int count, LabelId destination, Envelopes.Envelope fallback` |
| `Operations.IndirectJump` | `Header header, Expression target, Types.LabelType within, Envelopes.Envelope fallback` |
| `Origins.Position` | `int line, int column` |
| `Origins.Span` | `Position start, Position end, int lineBase, int columnBase, ColumnUnit columnUnit, boolean endExclusive` |
| `Origins.IncludeFrame` | `ArtifactId including, ArtifactId included, String requestedName, Optional<Span> site` |
| `Origins.Written` | `OriginId id, ArtifactId artifact, Optional<Span> span, List<IncludeFrame> includes, boolean exact` |
| `Origins.Derived` | `OriginId id, List<OriginId> inputs, String rule` |
| `Origins.Contractual` | `OriginId id, String authority, String version` |
| `Origins.Unavailable` | `OriginId id, String reason` |
| `Origins.Artifact` | `ArtifactId id, String logicalName, Optional<String> contentDigest` |
| `Places.ObjectPlace` | `Operand.Header header, ObjectId object` |
| `Places.Choice` | `Operand.Header header, List<Place> candidates, Scopes.MemoryBound remainder, Types.TypeRef typeRef, Optional<PremiseId> knownRemainderDomainProof` |
| `Places.RegionSlice` | `Operand.Header header, StorageId region, Expression offset, Expression length, Memory.Codec codec, Types.TypeRef typeRef, Optional<PremiseId> accessProof` |
| `Proofs.ObjectDomain` | `ObjectId object` |
| `Proofs.CellDomain` | `StorageId cell` |
| `Proofs.OperandDomain` | `OperandId operand` |
| `Proofs.ParameterDomain` | `EntryId entry, int position` |
| `Proofs.ResultDomain` | `EntryId entry, int position` |
| `Proofs.CallParameterDomain` | `OperationId invocation, Interactions.SignatureTarget signature, int position` |
| `Proofs.CallResultDomain` | `OperationId invocation, Interactions.SignatureTarget signature, int position` |
| `Proofs.UnitDomain` | `UnitId unit` |
| `Proofs.EntryDomain` | `EntryId entry` |
| `Proofs.OperationDomain` | `OperationId operation` |
| `Proofs.InvocationDomain` | `OperationId invocation` |
| `Proofs.Intersection` | `DomainProofScope left, DomainProofScope right` |
| `Proofs.EntrySite` | `EntryId entry` |
| `Proofs.OperationSite` | `OperationId operation` |
| `Proofs.InvocationSite` | `OperationId invocation` |
| `Proofs.SameDomain` | `DomainSubject left, DomainSubject right, DomainProofScope scope` |
| `Proofs.DisjointStorage` | `List<StorageId> storage, Scopes.FactScope scope` |
| `Proofs.SafetyAssertion` | `SafetyProperty property, List<Id> subjects, DomainProofScope scope` |
| `Proofs.Premise` | `PremiseId id, String authority, String justification, OriginId origin, Assertion assertion` |
| `Publication` | `PublicationId id, SemanticVersion airVersion, Capabilities.Manifest capabilities, List<Origins.Artifact> artifacts, List<Unit> units, List<Memory.Storage> storage, List<Interactions.Resource> resources, List<Artifacts.Relation> artifactRelations, List<Origins.Origin> origins, Evidence.Coverage coverage, List<Evidence.Uncertainty> uncertainties, List<Proofs.Premise> premises, List<Interactions.Contract> contracts` |
| `Scopes.PublicationScope` | `PublicationId publication` |
| `Scopes.UnitScope` | `UnitId unit` |
| `Scopes.EntityScope` | `List<Id> entities` |
| `Scopes.ObjectsMemory` | `List<ObjectId> objects` |
| `Scopes.StorageMemory` | `List<StorageId> storage` |
| `Scopes.VisibleMemory` | `UnitId unit, boolean includingExternal` |
| `Scopes.AllMemory` | `PublicationId publication, boolean includingEnvironment` |
| `Scopes.MemoryUnion` | `List<MemoryScope> members` |
| `Scopes.WithinMemory` | `MemoryScope scope` |
| `Scopes.LabelsControl` | `List<LabelId> labels` |
| `Scopes.UnitControl` | `UnitId unit, boolean labels, boolean normalExit, boolean exceptionalExit, boolean halt, boolean diverge, boolean externalControl` |
| `Scopes.AllControl` | `PublicationId publication` |
| `Scopes.ControlUnion` | `List<ControlScope> members` |
| `Scopes.WithinControl` | `ControlScope scope` |
| `Scopes.ResourceCategories` | `List<String> categories` |
| `SemanticVersion` | `int major, int minor, int patch` |
| `Sequence` | `LabelId label, List<Instruction> instructions, Terminator terminator, OriginId origin` |
| `Types.ExtensionType` | `String name, SemanticVersion version` |
| `Types.LabelType` | `UnitId unit, List<LabelId> labels` |
| `Types.Known` | `Type type` |
| `Types.UnknownType` | `UncertaintyId uncertainty` |
| `Unit` | `UnitId id, Optional<UnitId> containingUnit, List<Memory.ObjectDeclaration> objects, List<ObjectId> visibleObjects, List<Entries.Entry> entries, List<Sequence> sequences, List<Entries.CompletionPort> completionPorts, BodyAvailability body, Optional<UncertaintyId> bodyUnavailable, Evidence.Coverage coverage, OriginId origin` |
| `Values.BoolValue` | `boolean value` |
| `Values.IntValue` | `BigInteger value` |
| `Values.DecimalValue` | `BigInteger coefficient, int scale` |
| `Values.TextValue` | `String value` |
| `Values.BytesValue` | `List<Integer> octets` |
| `Values.LabelValue` | `LabelId label, Types.LabelType domain` |

## Enumerações

| Tipo | Valores |
| --- | --- |
| `Control.Propagate` | `INSTANCE` |
| `Control.HaltAlternative` | `INSTANCE` |
| `Control.Diverge` | `INSTANCE` |
| `Control.NormalOutcome` | `INSTANCE` |
| `Control.OtherExceptionOutcome` | `INSTANCE` |
| `Control.HaltOutcome` | `INSTANCE` |
| `Control.DivergeOutcome` | `INSTANCE` |
| `Entries.Preserve` | `INSTANCE` |
| `Evidence.Dimension` | `CONTROL, STORAGE, EFFECTS, VALUES, DEPENDENCIES` |
| `Evidence.PrecisionStatus` | `EXACT, CONSERVATIVE, OPEN, UNAVAILABLE, NOT_APPLICABLE` |
| `Evidence.CoverageStatus` | `MODELED, ABSTRACTED, UNSUPPORTED, INPUT_MISSING` |
| `Evidence.InventoryStatus` | `COMPLETE, PARTIAL, UNAVAILABLE` |
| `Expressions.UnaryOperator` | `NOT, NEG, TO_DECIMAL, LENGTH` |
| `Expressions.BinaryOperator` | `EQ, NE, LT, LE, GT, GE, AND, OR, ADD, SUB, MUL, CONCAT` |
| `Expressions.Rounding` | `TOWARD_ZERO, HALF_EVEN` |
| `Interactions.PassingMode` | `VALUE, REFERENCE, COPY` |
| `Interactions.ExactName` | `INSTANCE` |
| `Memory.Lifetime` | `ACTIVATION, PERSISTENT, EXTERNAL` |
| `Memory.Visibility` | `PRIVATE, SHARED, UNKNOWN` |
| `Memory.ByteOrder` | `LITTLE, BIG` |
| `Memory.IdentityBytes` | `INSTANCE` |
| `Memory.AsciiText` | `INSTANCE` |
| `Operations.HaltKind` | `NORMAL, ABNORMAL` |
| `Origins.ColumnUnit` | `UNICODE_SCALAR, UTF16_CODE_UNIT, OCTET` |
| `Proofs.PublicationDomain` | `INSTANCE` |
| `Proofs.SafetyProperty` | `VALID_PURE_ACCESS, VALID_TEXT_SLICE, VALID_CODEC_WRITE, CHOICE_REMAINDER_DOMAIN, EXTENSION_EQUALITY_DEFINED` |
| `Scopes.NoMemory` | `INSTANCE` |
| `Scopes.NoControl` | `INSTANCE` |
| `Scopes.NoResources` | `INSTANCE` |
| `Scopes.AnyResource` | `INSTANCE` |
| `Types.Builtin` | `BOOL, INT, DECIMAL, TEXT, BYTES` |
| `Unit.BodyAvailability` | `AVAILABLE, UNAVAILABLE` |
