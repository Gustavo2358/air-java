# Catálogo do modelo Java

Catálogo informativo da API reconciliada. A semântica vem da Analysis IR fixada
em `docs/sources.lock.json`, não dos nomes de classes. Records copiam listas
defensivamente; `Optional` é uma escolha de representação Java.

## Records

| Tipo | Campos |
| --- | --- |
| `Artifacts.InternalArtifact` | `ArtifactId artifact` |
| `Artifacts.ExternalArtifact` | `Interactions.LiteralTarget resource` |
| `Artifacts.Relation` | `ArtifactRelationId id, ArtifactId source, RelationTarget destination, String kind, OriginId origin, CoverageStatus coverage` |
| `Capabilities.Capability` | `String name, String version` |
| `Capabilities.Manifest` | `List<Capability> required, List<Capability> provided` |
| `Control.Handler` | `LabelId label` |
| `Control.Normal` | `LabelId label` |
| `Control.JumpAlternative` | `LabelId label` |
| `Control.Exceptional` | `String tag, ExceptionDestination destination` |
| `Control.AnyException` | `ExceptionDestination destination` |
| `Control.InvocationOutcomes` | `List<InvocationAlternative> known, ControlBound remainder` |
| `Control.ControlEnvelope` | `List<ControlAlternative> known, ControlBound remainder` |
| `Control.ExceptionOutcome` | `String tag` |
| `Control.Before` | `OperationId operation` |
| `Control.After` | `OperationId operation, OutcomeKey outcome` |
| `Control.EntryPoint` | `EntryId entry` |
| `Control.ExitPoint` | `UnitId unit, OutcomeKey outcome` |
| `Entries.CompletionPort` | `CompletionPortId id, OriginId origin` |
| `Entries.LiteralInitial` | `Expressions.Literal value` |
| `Entries.ParameterInitial` | `BigInteger position` |
| `Entries.ExternalUnknown` | `UncertaintyId reason` |
| `Entries.Uninitialized` | `UncertaintyId reason` |
| `Entries.InitialCondition` | `Place place, InitialValue value, OriginId origin, List<PremiseId> premises` |
| `Entries.EntryState` | `List<InitialCondition> conditions, List<UncertaintyId> uncertainties` |
| `Entries.Entry` | `EntryId id, Optional<LabelId> initialLabel, Signature signature, EntryState state, OriginId origin` |
| `Envelopes.MemoryEnvelope` | `List<OperandId> knownReads, MemoryBound otherReads, List<OperandId> knownWrites, MemoryBound otherWrites, List<OperandId> mustOverwrite` |
| `Envelopes.ResourceUse` | `String action, ResourceDescription target, ProgramPoint point, OriginId origin` |
| `Envelopes.DependencyEnvelope` | `List<ResourceUse> known, DependencyBound remainder` |
| `Envelopes.Envelope` | `MemoryEnvelope memory, ControlEnvelope control, DependencyEnvelope dependencies` |
| `Evidence.Claim` | `FactScope scope, PrecisionStatus status, List<UncertaintyId> reasons` |
| `Evidence.Precision` | `Claim control, Claim storage, Claim effects, Claim values, Claim dependencies` |
| `Evidence.Uncertainty` | `UncertaintyId id, String code, List<Dimension> dimensions, FactScope scope, String reason, OriginId origin` |
| `Evidence.Elimination` | `String rule, OriginId origin` |
| `Evidence.CoverageItem` | `String sourceKey, OriginId origin, CoverageStatus status, List<Id> outputs, List<UncertaintyId> uncertainties, Optional<Elimination> elimination` |
| `Evidence.Coverage` | `InventoryStatus inventory, FactScope scope, List<CoverageItem> items, List<UncertaintyId> uncertainties` |
| `Expressions.Literal` | `Operand.Header header, LiteralValue value` |
| `Expressions.Read` | `Operand.Header header, Place place` |
| `Expressions.Unknown` | `Operand.Header header, TypeRef typeRef, List<Expression> dependencies, MemoryBound remainingReads, UncertaintyId reason` |
| `Expressions.Unary` | `Operand.Header header, UnaryOperator operator, Expression argument` |
| `Expressions.Binary` | `Operand.Header header, BinaryOperator operator, Expression left, Expression right` |
| `Expressions.Quantize` | `Operand.Header header, Expression value, BigInteger scale, Rounding rounding` |
| `Expressions.FitText` | `Operand.Header header, Expression value, BigInteger length, String pad` |
| `Expressions.SliceText` | `Operand.Header header, Expression value, Expression start, Expression count` |
| `Expressions.TrimRight` | `Operand.Header header, Expression value, String characters` |
| `Ids.PublicationId` | `String localId` |
| `Ids.UnitId` | `PublicationId publication, String localId` |
| `Ids.StorageId` | `PublicationId publication, String localId` |
| `Ids.ResourceId` | `PublicationId publication, String localId` |
| `Ids.ArtifactId` | `PublicationId publication, String localId` |
| `Ids.ArtifactRelationId` | `PublicationId publication, String localId` |
| `Ids.OriginId` | `PublicationId publication, String localId` |
| `Ids.UncertaintyId` | `PublicationId publication, String localId` |
| `Ids.PremiseId` | `PublicationId publication, String localId` |
| `Ids.EntryId` | `UnitId unit, String localId` |
| `Ids.LabelId` | `UnitId unit, String localId` |
| `Ids.OperationId` | `UnitId unit, String localId` |
| `Ids.ObjectId` | `UnitId unit, String localId` |
| `Ids.CompletionPortId` | `UnitId unit, String localId` |
| `Ids.OperationOwner` | `OperationId operation` |
| `Ids.EntryOwner` | `EntryId entry` |
| `Ids.OperandId` | `OperandOwner owner, String localId` |
| `Interactions.ExtensionName` | `String name, String version` |
| `Interactions.UnknownName` | `UncertaintyId uncertainty` |
| `Interactions.InternalTarget` | `EntryId entry` |
| `Interactions.LiteralTarget` | `String category, String namespace, String name, NamePolicy namePolicy, OriginId origin` |
| `Interactions.ComputedTarget` | `String category, String namespace, Expression name, NamePolicy namePolicy, OriginId origin` |
| `Interactions.ComputedResource` | `String category, String namespace, OperandId name, NamePolicy namePolicy, OriginId origin` |
| `Interactions.Resource` | `ResourceId id, ResourceDescription description, OriginId origin` |
| `Interactions.ValueArgument` | `Expression value` |
| `Interactions.CopyArgument` | `Expression value` |
| `Interactions.ReferenceArgument` | `Place place` |
| `Interactions.UnknownRemainder` | `UncertaintyId uncertainty` |
| `Interactions.KnownMode` | `PassingMode mode` |
| `Interactions.UnknownMode` | `UncertaintyId uncertainty` |
| `Interactions.ObjectBinding` | `ObjectId object` |
| `Interactions.UnknownParameterBinding` | `UncertaintyId uncertainty` |
| `Interactions.Parameter` | `BigInteger position, ModeKnowledge mode, TypeRef typeRef, ParameterBinding objectBinding, OriginId origin` |
| `Interactions.ResultSlot` | `BigInteger position, TypeRef typeRef, OriginId origin` |
| `Interactions.ParameterInventory` | `List<Parameter> known, UnknownBound remainder` |
| `Interactions.ResultInventory` | `List<ResultSlot> known, UnknownBound remainder` |
| `Interactions.Signature` | `ParameterInventory parameters, ResultInventory results, OriginId origin` |
| `Interactions.ForeignEffects` | `MemoryBound reads, MemoryBound writes, List<OperandId> mustOverwrite` |
| `Interactions.OutcomeEffects` | `OutcomeKey outcome, ForeignEffects effects` |
| `Interactions.EffectBound` | `ForeignEffects otherwise, List<OutcomeEffects> perOutcome` |
| `Interactions.ContractRef` | `String authority, String version, List<OriginId> evidence` |
| `Interactions.KnownContract` | `ContractRef reference` |
| `Interactions.UnknownContract` | `UncertaintyId uncertainty` |
| `Interactions.EntrySignature` | `EntryId entry` |
| `Interactions.ExternalSignature` | `Signature signature` |
| `Memory.BinaryCodec` | `boolean signed, BigInteger width, ByteOrder order` |
| `Memory.ExtensionCodec` | `String name, String version, TypeRef logicalType` |
| `Memory.UnknownCodec` | `TypeRef logicalType, UncertaintyId reason` |
| `Memory.CellBinding` | `StorageId storage` |
| `Memory.ViewBinding` | `StorageId region, BigInteger offset, BigInteger extent, Codec codec` |
| `Memory.AliasBinding` | `ObjectId object` |
| `Memory.AlternativesBinding` | `List<Binding> alternatives, MemoryBound remainder` |
| `Memory.UnknownBinding` | `MemoryScope scope, UncertaintyId reason` |
| `Memory.StorageHeader` | `StorageId id, Optional<UnitId> owner, Lifetime lifetime, Visibility visibility, OriginId origin` |
| `Memory.Cell` | `StorageHeader header, TypeRef typeRef` |
| `Memory.Region` | `StorageHeader header, Optional<BigInteger> extent, Optional<UncertaintyId> extentUnknown` |
| `Memory.ObjectDeclaration` | `ObjectId id, Optional<String> displayName, TypeRef typeRef, Binding storage, Visibility visibility, OriginId origin, CoverageStatus coverage, Precision precision` |
| `Memory.ByteRange` | `StorageId region, Expression offset, Expression extent` |
| `Operand.Header` | `OperandId id, Role role, OriginId origin` |
| `Operations.Header` | `OperationId id, OriginId origin, CoverageStatus coverage, Precision precision, List<UncertaintyId> uncertainties` |
| `Operations.Assign` | `Header header, Place destination, Expression value` |
| `Operations.HavocMust` | `Header header, Place destination, UncertaintyId reason` |
| `Operations.HavocMay` | `Header header, MemoryScope scope, UncertaintyId reason` |
| `Operations.Nop` | `Header header` |
| `Operations.CopyBytes` | `Header header, ByteRange destination, ByteRange source, BigInteger length, Envelope fallback` |
| `Operations.Jump` | `Header header, LabelId destination` |
| `Operations.Branch` | `Header header, Expression predicate, LabelId trueDestination, LabelId falseDestination` |
| `Operations.Case` | `LiteralValue value, LabelId destination` |
| `Operations.Dispatch` | `Header header, Expression selector, List<Case> cases, LabelId defaultDestination` |
| `Operations.Invoke` | `Header header, String action, Target target, List<Argument> arguments, List<Place> results, InvocationSignature signature, List<Place> effectOperands, EffectBound effectBound, InvocationOutcomes outcomes, ContractKnowledge contract` |
| `Operations.Return` | `Header header, List<Expression> values` |
| `Operations.Raise` | `Header header, String tag, List<Expression> values` |
| `Operations.Halt` | `Header header, HaltKind haltKind` |
| `Operations.Opaque` | `Header header, String observedKind, List<Operand> knownOperands, List<OperandId> valueResults, Envelope envelope` |
| `Operations.LocalInvoke` | `Header header, LabelId entry, List<CompletionPortId> completionPorts, LabelId resume, Envelope fallback` |
| `Operations.LocalBoundary` | `Header header, CompletionPortId port, LabelId defaultDestination, Envelope fallback` |
| `Operations.LocalResume` | `Header header, Envelope fallback` |
| `Operations.LocalUnwind` | `Header header, BigInteger count, LabelId destination, Envelope fallback` |
| `Operations.IndirectJump` | `Header header, Expression target, LabelType within, Envelope fallback` |
| `Origins.Position` | `BigInteger line, BigInteger column` |
| `Origins.Span` | `Position start, Position end, BigInteger lineBase, BigInteger columnBase, ColumnUnit columnUnit, boolean endExclusive` |
| `Origins.LineColumns` | `Span span` |
| `Origins.Offsets` | `BigInteger start, BigInteger end, String unit, boolean endExclusive` |
| `Origins.IncludeFrame` | `ArtifactId including, ArtifactId included, String requestedName, Optional<Location> site` |
| `Origins.Written` | `OriginId id, ArtifactId artifact, Optional<Location> location, List<IncludeFrame> includes, boolean exact` |
| `Origins.Derived` | `OriginId id, List<OriginId> inputs, String rule` |
| `Origins.Contractual` | `OriginId id, String authority, String version` |
| `Origins.Unavailable` | `OriginId id, String reason` |
| `Origins.Artifact` | `ArtifactId id, String logicalName, Optional<String> contentDigest` |
| `Places.ObjectPlace` | `Operand.Header header, ObjectId object` |
| `Places.Choice` | `Operand.Header header, List<Place> candidates, MemoryBound remainder, TypeRef typeRef` |
| `Places.RegionSlice` | `Operand.Header header, StorageId region, Expression offset, Expression length, Codec codec, TypeRef typeRef` |
| `Proofs.ObjectDomain` | `ObjectId object` |
| `Proofs.CellDomain` | `StorageId cell` |
| `Proofs.OperandDomain` | `OperandId operand` |
| `Proofs.ParameterDomain` | `EntryId entry, BigInteger position` |
| `Proofs.ResultDomain` | `EntryId entry, BigInteger position` |
| `Proofs.CallParameterDomain` | `OperationId invocation, EntryId entry, BigInteger position` |
| `Proofs.CallResultDomain` | `OperationId invocation, EntryId entry, BigInteger position` |
| `Proofs.ExternalParameterDomain` | `OperationId invocation, BigInteger position` |
| `Proofs.ExternalResultDomain` | `OperationId invocation, BigInteger position` |
| `Proofs.UnitDomain` | `UnitId unit` |
| `Proofs.EntryDomain` | `EntryId entry` |
| `Proofs.OperationDomain` | `OperationId operation` |
| `Proofs.InvocationDomain` | `OperationId invocation` |
| `Proofs.Intersection` | `DomainProofScope left, DomainProofScope right` |
| `Proofs.EntrySite` | `EntryId entry` |
| `Proofs.OperationSite` | `OperationId operation` |
| `Proofs.InvocationSite` | `OperationId invocation` |
| `Proofs.SameDomain` | `DomainSubject left, DomainSubject right, DomainProofScope scope` |
| `Proofs.DisjointStorage` | `List<StorageId> storage` |
| `Proofs.Premise` | `PremiseId id, String authority, String justification, OriginId origin, Assertion assertion` |
| `Publication` | `PublicationId id, SemanticVersion airVersion, Manifest capabilities, List<Artifact> artifacts, List<Unit> units, List<Storage> storage, List<Resource> resources, List<Relation> artifactRelations, List<Origin> origins, Coverage coverage, List<Uncertainty> uncertainties, List<Premise> premises` |
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
| `SemanticVersion` | `BigInteger major, BigInteger minor, BigInteger patch` |
| `Sequence` | `LabelId label, List<Instruction> instructions, Terminator terminator, OriginId origin` |
| `Types.ExtensionType` | `String name, String version` |
| `Types.LabelType` | `UnitId unit, List<LabelId> labels` |
| `Types.Known` | `Type type` |
| `Types.UnknownType` | `UncertaintyId uncertainty` |
| `Unit` | `UnitId id, Optional<UnitId> containingUnit, List<ObjectDeclaration> objects, List<ObjectId> visibleObjects, List<Entry> entries, List<Sequence> sequences, List<CompletionPort> completionPorts, BodyAvailability body, Optional<UncertaintyId> bodyUnavailable, Coverage coverage, OriginId origin` |
| `Values.BoolValue` | `boolean value` |
| `Values.IntValue` | `BigInteger value` |
| `Values.DecimalValue` | `BigInteger coefficient, BigInteger scale` |
| `Values.TextValue` | `String value` |
| `Values.BytesValue` | `List<Integer> octets` |
| `Values.LabelValue` | `LabelId label, LabelType domain` |

## Enumerações e singletons

| Tipo | Valores |
| --- | --- |
| `Control.Propagate` | `INSTANCE` |
| `Control.HaltAlternative` | `INSTANCE` |
| `Control.Diverge` | `INSTANCE` |
| `Control.ReturnAlternative` | `INSTANCE` |
| `Control.ContinueAlternative` | `INSTANCE` |
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
| `Interactions.NoRemainder` | `INSTANCE` |
| `Interactions.ExternalBinding` | `INSTANCE` |
| `Memory.Lifetime` | `ACTIVATION, PERSISTENT, EXTERNAL` |
| `Memory.Visibility` | `PRIVATE, SHARED, UNKNOWN` |
| `Memory.ByteOrder` | `LITTLE, BIG` |
| `Memory.IdentityBytes` | `INSTANCE` |
| `Memory.AsciiText` | `INSTANCE` |
| `Operand.Role` | `VALUE_READ, VALUE_WRITE, ADDRESS_READ, PREDICATE, CALL_TARGET, ARGUMENT_VALUE, ARGUMENT_REFERENCE, RESULT_TARGET, RESOURCE_TARGET, CONTROL_TARGET` |
| `Operations.HaltKind` | `NORMAL, ABNORMAL` |
| `Origins.ColumnUnit` | `UNICODE_SCALAR, UTF16_CODE_UNIT, OCTET` |
| `Proofs.PublicationDomain` | `INSTANCE` |
| `Scopes.NoMemory` | `INSTANCE` |
| `Scopes.NoControl` | `INSTANCE` |
| `Scopes.NoResources` | `INSTANCE` |
| `Scopes.AnyResource` | `INSTANCE` |
| `Types.Builtin` | `BOOL, INT, DECIMAL, TEXT, BYTES` |
| `Unit.BodyAvailability` | `AVAILABLE, UNAVAILABLE` |

## Somas seladas relevantes

- `Target`: `InternalTarget | LiteralTarget | ComputedTarget`.
- `ResourceDescription`: `InternalTarget | LiteralTarget | ComputedResource`.
- `InvocationSignature`: `EntrySignature | ExternalSignature`.
- `ContractKnowledge`: `KnownContract | UnknownContract`.
- `Assertion`: `SameDomain | DisjointStorage`.
- `Location`: `LineColumns | Offsets`.
- `InvocationAlternative`: normal, exception, any-exception, halt ou diverge.
- `ControlAlternative`: toda alternativa de invocação, jump, return ou continue.

Não há inventário Java de contratos nem forma executável por `ResourceId`. O
catálogo JSON futuro pertence à especificação DRAFT externa.
