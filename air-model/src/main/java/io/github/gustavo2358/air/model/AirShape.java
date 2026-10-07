package io.github.gustavo2358.air.model;

/** Complete Publication-reachable typed shapes, not a JSON schema or new AIR variants. */
public enum AirShape {
    TEXT(Form.TEXT, "java.lang.String"),
    INTEGER(Form.INTEGER, "java.math.BigInteger"),
    BOOLEAN(Form.BOOLEAN, "boolean"),
    SMALL_INTEGER(Form.SMALL_INTEGER, "java.lang.Integer"),
    LIST(Form.LIST, "java.util.List"),
    OPTIONAL(Form.OPTIONAL, "java.util.Optional"),
    ARTIFACTS_EXTERNAL_ARTIFACT(Form.RECORD, "io.github.gustavo2358.air.model.Artifacts.ExternalArtifact"),
    ARTIFACTS_INTERNAL_ARTIFACT(Form.RECORD, "io.github.gustavo2358.air.model.Artifacts.InternalArtifact"),
    ARTIFACTS_RELATION(Form.RECORD, "io.github.gustavo2358.air.model.Artifacts.Relation"),
    ARTIFACTS_RELATION_TARGET(Form.UNION, "io.github.gustavo2358.air.model.Artifacts.RelationTarget"),
    CAPABILITIES_CAPABILITY(Form.RECORD, "io.github.gustavo2358.air.model.Capabilities.Capability"),
    CAPABILITIES_MANIFEST(Form.RECORD, "io.github.gustavo2358.air.model.Capabilities.Manifest"),
    CONTROL_AFTER(Form.RECORD, "io.github.gustavo2358.air.model.Control.After"),
    CONTROL_ANY_EXCEPTION(Form.RECORD, "io.github.gustavo2358.air.model.Control.AnyException"),
    CONTROL_BEFORE(Form.RECORD, "io.github.gustavo2358.air.model.Control.Before"),
    CONTROL_CONTINUE_ALTERNATIVE(Form.ENUM, "io.github.gustavo2358.air.model.Control.ContinueAlternative"),
    CONTROL_CONTROL_ALTERNATIVE(Form.UNION, "io.github.gustavo2358.air.model.Control.ControlAlternative"),
    CONTROL_CONTROL_ENVELOPE(Form.RECORD, "io.github.gustavo2358.air.model.Control.ControlEnvelope"),
    CONTROL_DIVERGE(Form.ENUM, "io.github.gustavo2358.air.model.Control.Diverge"),
    CONTROL_DIVERGE_OUTCOME(Form.ENUM, "io.github.gustavo2358.air.model.Control.DivergeOutcome"),
    CONTROL_ENTRY_POINT(Form.RECORD, "io.github.gustavo2358.air.model.Control.EntryPoint"),
    CONTROL_EXCEPTION_DESTINATION(Form.UNION, "io.github.gustavo2358.air.model.Control.ExceptionDestination"),
    CONTROL_EXCEPTION_OUTCOME(Form.RECORD, "io.github.gustavo2358.air.model.Control.ExceptionOutcome"),
    CONTROL_EXCEPTIONAL(Form.RECORD, "io.github.gustavo2358.air.model.Control.Exceptional"),
    CONTROL_EXIT_POINT(Form.RECORD, "io.github.gustavo2358.air.model.Control.ExitPoint"),
    CONTROL_HALT_ALTERNATIVE(Form.ENUM, "io.github.gustavo2358.air.model.Control.HaltAlternative"),
    CONTROL_HALT_OUTCOME(Form.ENUM, "io.github.gustavo2358.air.model.Control.HaltOutcome"),
    CONTROL_HANDLER(Form.RECORD, "io.github.gustavo2358.air.model.Control.Handler"),
    CONTROL_INVOCATION_ALTERNATIVE(Form.UNION, "io.github.gustavo2358.air.model.Control.InvocationAlternative"),
    CONTROL_INVOCATION_OUTCOMES(Form.RECORD, "io.github.gustavo2358.air.model.Control.InvocationOutcomes"),
    CONTROL_JUMP_ALTERNATIVE(Form.RECORD, "io.github.gustavo2358.air.model.Control.JumpAlternative"),
    CONTROL_NORMAL(Form.RECORD, "io.github.gustavo2358.air.model.Control.Normal"),
    CONTROL_NORMAL_OUTCOME(Form.ENUM, "io.github.gustavo2358.air.model.Control.NormalOutcome"),
    CONTROL_OTHER_EXCEPTION_OUTCOME(Form.ENUM, "io.github.gustavo2358.air.model.Control.OtherExceptionOutcome"),
    CONTROL_OUTCOME_KEY(Form.UNION, "io.github.gustavo2358.air.model.Control.OutcomeKey"),
    CONTROL_PROGRAM_POINT(Form.UNION, "io.github.gustavo2358.air.model.Control.ProgramPoint"),
    CONTROL_PROPAGATE(Form.ENUM, "io.github.gustavo2358.air.model.Control.Propagate"),
    CONTROL_RETURN_ALTERNATIVE(Form.ENUM, "io.github.gustavo2358.air.model.Control.ReturnAlternative"),
    DECIMAL_TEXT_KIND(Form.ENUM, "io.github.gustavo2358.air.model.DecimalText.Kind"),
    DECIMAL_TEXT_PART(Form.RECORD, "io.github.gustavo2358.air.model.DecimalText.Part"),
    ENTRIES_COMPLETION_PORT(Form.RECORD, "io.github.gustavo2358.air.model.Entries.CompletionPort"),
    ENTRIES_ENTRY(Form.RECORD, "io.github.gustavo2358.air.model.Entries.Entry"),
    ENTRIES_ENTRY_STATE(Form.RECORD, "io.github.gustavo2358.air.model.Entries.EntryState"),
    ENTRIES_EXTERNAL_UNKNOWN(Form.RECORD, "io.github.gustavo2358.air.model.Entries.ExternalUnknown"),
    ENTRIES_INITIAL_CONDITION(Form.RECORD, "io.github.gustavo2358.air.model.Entries.InitialCondition"),
    ENTRIES_INITIAL_VALUE(Form.UNION, "io.github.gustavo2358.air.model.Entries.InitialValue"),
    ENTRIES_LITERAL_INITIAL(Form.RECORD, "io.github.gustavo2358.air.model.Entries.LiteralInitial"),
    ENTRIES_PARAMETER_INITIAL(Form.RECORD, "io.github.gustavo2358.air.model.Entries.ParameterInitial"),
    ENTRIES_POSSIBLE_LITERALS(Form.RECORD, "io.github.gustavo2358.air.model.Entries.PossibleLiterals"),
    ENTRIES_PRESERVE(Form.ENUM, "io.github.gustavo2358.air.model.Entries.Preserve"),
    ENTRIES_UNINITIALIZED(Form.RECORD, "io.github.gustavo2358.air.model.Entries.Uninitialized"),
    ENVELOPES_DEPENDENCY_ENVELOPE(Form.RECORD, "io.github.gustavo2358.air.model.Envelopes.DependencyEnvelope"),
    ENVELOPES_ENVELOPE(Form.RECORD, "io.github.gustavo2358.air.model.Envelopes.Envelope"),
    ENVELOPES_MEMORY_ENVELOPE(Form.RECORD, "io.github.gustavo2358.air.model.Envelopes.MemoryEnvelope"),
    ENVELOPES_RESOURCE_USE(Form.RECORD, "io.github.gustavo2358.air.model.Envelopes.ResourceUse"),
    EVIDENCE_CLAIM(Form.RECORD, "io.github.gustavo2358.air.model.Evidence.Claim"),
    EVIDENCE_COVERAGE(Form.RECORD, "io.github.gustavo2358.air.model.Evidence.Coverage"),
    EVIDENCE_COVERAGE_ITEM(Form.RECORD, "io.github.gustavo2358.air.model.Evidence.CoverageItem"),
    EVIDENCE_COVERAGE_STATUS(Form.ENUM, "io.github.gustavo2358.air.model.Evidence.CoverageStatus"),
    EVIDENCE_DIMENSION(Form.ENUM, "io.github.gustavo2358.air.model.Evidence.Dimension"),
    EVIDENCE_ELIMINATION(Form.RECORD, "io.github.gustavo2358.air.model.Evidence.Elimination"),
    EVIDENCE_INVENTORY_STATUS(Form.ENUM, "io.github.gustavo2358.air.model.Evidence.InventoryStatus"),
    EVIDENCE_PRECISION(Form.RECORD, "io.github.gustavo2358.air.model.Evidence.Precision"),
    EVIDENCE_PRECISION_STATUS(Form.ENUM, "io.github.gustavo2358.air.model.Evidence.PrecisionStatus"),
    EVIDENCE_UNCERTAINTY(Form.RECORD, "io.github.gustavo2358.air.model.Evidence.Uncertainty"),
    EXPRESSION(Form.UNION, "io.github.gustavo2358.air.model.Expression"),
    EXPRESSIONS_BINARY(Form.RECORD, "io.github.gustavo2358.air.model.Expressions.Binary"),
    EXPRESSIONS_BINARY_OPERATOR(Form.ENUM, "io.github.gustavo2358.air.model.Expressions.BinaryOperator"),
    EXPRESSIONS_FILL_TEXT(Form.RECORD, "io.github.gustavo2358.air.model.Expressions.FillText"),
    EXPRESSIONS_FIT_DECIMAL(Form.RECORD, "io.github.gustavo2358.air.model.Expressions.FitDecimal"),
    EXPRESSIONS_FIT_TEXT(Form.RECORD, "io.github.gustavo2358.air.model.Expressions.FitText"),
    EXPRESSIONS_FORMAT_DECIMAL(Form.RECORD, "io.github.gustavo2358.air.model.Expressions.FormatDecimal"),
    EXPRESSIONS_INTEGER_DIGITS(Form.RECORD, "io.github.gustavo2358.air.model.Expressions.IntegerDigits"),
    EXPRESSIONS_LITERAL(Form.RECORD, "io.github.gustavo2358.air.model.Expressions.Literal"),
    EXPRESSIONS_PARSE_INTEGER(Form.RECORD, "io.github.gustavo2358.air.model.Expressions.ParseInteger"),
    EXPRESSIONS_QUANTIZE(Form.RECORD, "io.github.gustavo2358.air.model.Expressions.Quantize"),
    EXPRESSIONS_READ(Form.RECORD, "io.github.gustavo2358.air.model.Expressions.Read"),
    EXPRESSIONS_ROUNDING(Form.ENUM, "io.github.gustavo2358.air.model.Expressions.Rounding"),
    EXPRESSIONS_SLICE_TEXT(Form.RECORD, "io.github.gustavo2358.air.model.Expressions.SliceText"),
    EXPRESSIONS_TRIM_RIGHT(Form.RECORD, "io.github.gustavo2358.air.model.Expressions.TrimRight"),
    EXPRESSIONS_UNARY(Form.RECORD, "io.github.gustavo2358.air.model.Expressions.Unary"),
    EXPRESSIONS_UNARY_OPERATOR(Form.ENUM, "io.github.gustavo2358.air.model.Expressions.UnaryOperator"),
    EXPRESSIONS_UNKNOWN(Form.RECORD, "io.github.gustavo2358.air.model.Expressions.Unknown"),
    EXPRESSIONS_WRAP_INTEGER(Form.RECORD, "io.github.gustavo2358.air.model.Expressions.WrapInteger"),
    IDS_ARTIFACT_ID(Form.RECORD, "io.github.gustavo2358.air.model.Ids.ArtifactId"),
    IDS_ARTIFACT_RELATION_ID(Form.RECORD, "io.github.gustavo2358.air.model.Ids.ArtifactRelationId"),
    IDS_COMPLETION_PORT_ID(Form.RECORD, "io.github.gustavo2358.air.model.Ids.CompletionPortId"),
    IDS_ENTRY_ID(Form.RECORD, "io.github.gustavo2358.air.model.Ids.EntryId"),
    IDS_ENTRY_OWNER(Form.RECORD, "io.github.gustavo2358.air.model.Ids.EntryOwner"),
    IDS_ID(Form.UNION, "io.github.gustavo2358.air.model.Ids.Id"),
    IDS_LABEL_ID(Form.RECORD, "io.github.gustavo2358.air.model.Ids.LabelId"),
    IDS_OBJECT_ID(Form.RECORD, "io.github.gustavo2358.air.model.Ids.ObjectId"),
    IDS_OPERAND_ID(Form.RECORD, "io.github.gustavo2358.air.model.Ids.OperandId"),
    IDS_OPERAND_OWNER(Form.UNION, "io.github.gustavo2358.air.model.Ids.OperandOwner"),
    IDS_OPERATION_ID(Form.RECORD, "io.github.gustavo2358.air.model.Ids.OperationId"),
    IDS_OPERATION_OWNER(Form.RECORD, "io.github.gustavo2358.air.model.Ids.OperationOwner"),
    IDS_ORIGIN_ID(Form.RECORD, "io.github.gustavo2358.air.model.Ids.OriginId"),
    IDS_PREMISE_ID(Form.RECORD, "io.github.gustavo2358.air.model.Ids.PremiseId"),
    IDS_PUBLICATION_ID(Form.RECORD, "io.github.gustavo2358.air.model.Ids.PublicationId"),
    IDS_RESOURCE_ID(Form.RECORD, "io.github.gustavo2358.air.model.Ids.ResourceId"),
    IDS_STORAGE_ID(Form.RECORD, "io.github.gustavo2358.air.model.Ids.StorageId"),
    IDS_UNCERTAINTY_ID(Form.RECORD, "io.github.gustavo2358.air.model.Ids.UncertaintyId"),
    IDS_UNIT_ID(Form.RECORD, "io.github.gustavo2358.air.model.Ids.UnitId"),
    INSTRUCTION(Form.UNION, "io.github.gustavo2358.air.model.Instruction"),
    INTERACTIONS_ARGUMENT(Form.UNION, "io.github.gustavo2358.air.model.Interactions.Argument"),
    INTERACTIONS_COMPUTED_RESOURCE(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.ComputedResource"),
    INTERACTIONS_COMPUTED_TARGET(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.ComputedTarget"),
    INTERACTIONS_CONTRACT_KNOWLEDGE(Form.UNION, "io.github.gustavo2358.air.model.Interactions.ContractKnowledge"),
    INTERACTIONS_CONTRACT_REF(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.ContractRef"),
    INTERACTIONS_COPY_ARGUMENT(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.CopyArgument"),
    INTERACTIONS_EFFECT_BOUND(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.EffectBound"),
    INTERACTIONS_ENTRY_SIGNATURE(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.EntrySignature"),
    INTERACTIONS_EXACT_NAME(Form.ENUM, "io.github.gustavo2358.air.model.Interactions.ExactName"),
    INTERACTIONS_EXTENSION_NAME(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.ExtensionName"),
    INTERACTIONS_EXTERNAL_BINDING(Form.ENUM, "io.github.gustavo2358.air.model.Interactions.ExternalBinding"),
    INTERACTIONS_EXTERNAL_SIGNATURE(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.ExternalSignature"),
    INTERACTIONS_FOREIGN_EFFECTS(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.ForeignEffects"),
    INTERACTIONS_INTERNAL_TARGET(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.InternalTarget"),
    INTERACTIONS_INVOCATION_SIGNATURE(Form.UNION, "io.github.gustavo2358.air.model.Interactions.InvocationSignature"),
    INTERACTIONS_KNOWN_CONTRACT(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.KnownContract"),
    INTERACTIONS_KNOWN_MODE(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.KnownMode"),
    INTERACTIONS_LITERAL_TARGET(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.LiteralTarget"),
    INTERACTIONS_LOCAL_RESOURCE(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.LocalResource"),
    INTERACTIONS_MODE_KNOWLEDGE(Form.UNION, "io.github.gustavo2358.air.model.Interactions.ModeKnowledge"),
    INTERACTIONS_NAME_POLICY(Form.UNION, "io.github.gustavo2358.air.model.Interactions.NamePolicy"),
    INTERACTIONS_NO_REMAINDER(Form.ENUM, "io.github.gustavo2358.air.model.Interactions.NoRemainder"),
    INTERACTIONS_OBJECT_BINDING(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.ObjectBinding"),
    INTERACTIONS_OUTCOME_EFFECTS(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.OutcomeEffects"),
    INTERACTIONS_PARAMETER(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.Parameter"),
    INTERACTIONS_PARAMETER_BINDING(Form.UNION, "io.github.gustavo2358.air.model.Interactions.ParameterBinding"),
    INTERACTIONS_PARAMETER_INVENTORY(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.ParameterInventory"),
    INTERACTIONS_PASSING_MODE(Form.ENUM, "io.github.gustavo2358.air.model.Interactions.PassingMode"),
    INTERACTIONS_REFERENCE_ARGUMENT(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.ReferenceArgument"),
    INTERACTIONS_RESOURCE(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.Resource"),
    INTERACTIONS_RESOURCE_DECLARATION(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.ResourceDeclaration"),
    INTERACTIONS_RESOURCE_DESCRIPTION(Form.UNION, "io.github.gustavo2358.air.model.Interactions.ResourceDescription"),
    INTERACTIONS_RESOURCE_OBJECT(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.ResourceObject"),
    INTERACTIONS_RESOURCE_USE(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.ResourceUse"),
    INTERACTIONS_RESULT_INVENTORY(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.ResultInventory"),
    INTERACTIONS_RESULT_SLOT(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.ResultSlot"),
    INTERACTIONS_SIGNATURE(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.Signature"),
    INTERACTIONS_TARGET(Form.UNION, "io.github.gustavo2358.air.model.Interactions.Target"),
    INTERACTIONS_UNKNOWN_BOUND(Form.UNION, "io.github.gustavo2358.air.model.Interactions.UnknownBound"),
    INTERACTIONS_UNKNOWN_CONTRACT(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.UnknownContract"),
    INTERACTIONS_UNKNOWN_MODE(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.UnknownMode"),
    INTERACTIONS_UNKNOWN_NAME(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.UnknownName"),
    INTERACTIONS_UNKNOWN_PARAMETER_BINDING(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.UnknownParameterBinding"),
    INTERACTIONS_UNKNOWN_REMAINDER(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.UnknownRemainder"),
    INTERACTIONS_UNKNOWN_RESOURCE(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.UnknownResource"),
    INTERACTIONS_VALUE_ARGUMENT(Form.RECORD, "io.github.gustavo2358.air.model.Interactions.ValueArgument"),
    MEMORY_ALIAS_BINDING(Form.RECORD, "io.github.gustavo2358.air.model.Memory.AliasBinding"),
    MEMORY_ALTERNATIVES_BINDING(Form.RECORD, "io.github.gustavo2358.air.model.Memory.AlternativesBinding"),
    MEMORY_ASCII_TEXT(Form.ENUM, "io.github.gustavo2358.air.model.Memory.AsciiText"),
    MEMORY_BINARY_CODEC(Form.RECORD, "io.github.gustavo2358.air.model.Memory.BinaryCodec"),
    MEMORY_BINDING(Form.UNION, "io.github.gustavo2358.air.model.Memory.Binding"),
    MEMORY_BYTE_ORDER(Form.ENUM, "io.github.gustavo2358.air.model.Memory.ByteOrder"),
    MEMORY_BYTE_RANGE(Form.RECORD, "io.github.gustavo2358.air.model.Memory.ByteRange"),
    MEMORY_CELL(Form.RECORD, "io.github.gustavo2358.air.model.Memory.Cell"),
    MEMORY_CELL_BINDING(Form.RECORD, "io.github.gustavo2358.air.model.Memory.CellBinding"),
    MEMORY_CODEC(Form.UNION, "io.github.gustavo2358.air.model.Memory.Codec"),
    MEMORY_EXTENSION_CODEC(Form.RECORD, "io.github.gustavo2358.air.model.Memory.ExtensionCodec"),
    MEMORY_IDENTITY_BYTES(Form.ENUM, "io.github.gustavo2358.air.model.Memory.IdentityBytes"),
    MEMORY_LIFETIME(Form.ENUM, "io.github.gustavo2358.air.model.Memory.Lifetime"),
    MEMORY_OBJECT_DECLARATION(Form.RECORD, "io.github.gustavo2358.air.model.Memory.ObjectDeclaration"),
    MEMORY_REGION(Form.RECORD, "io.github.gustavo2358.air.model.Memory.Region"),
    MEMORY_STORAGE(Form.UNION, "io.github.gustavo2358.air.model.Memory.Storage"),
    MEMORY_STORAGE_HEADER(Form.RECORD, "io.github.gustavo2358.air.model.Memory.StorageHeader"),
    MEMORY_UNKNOWN_BINDING(Form.RECORD, "io.github.gustavo2358.air.model.Memory.UnknownBinding"),
    MEMORY_UNKNOWN_CODEC(Form.RECORD, "io.github.gustavo2358.air.model.Memory.UnknownCodec"),
    MEMORY_VIEW_BINDING(Form.RECORD, "io.github.gustavo2358.air.model.Memory.ViewBinding"),
    MEMORY_VISIBILITY(Form.ENUM, "io.github.gustavo2358.air.model.Memory.Visibility"),
    OPERAND(Form.UNION, "io.github.gustavo2358.air.model.Operand"),
    OPERAND_HEADER(Form.RECORD, "io.github.gustavo2358.air.model.Operand.Header"),
    OPERAND_ROLE(Form.ENUM, "io.github.gustavo2358.air.model.Operand.Role"),
    OPERATIONS_ASSIGN(Form.RECORD, "io.github.gustavo2358.air.model.Operations.Assign"),
    OPERATIONS_BRANCH(Form.RECORD, "io.github.gustavo2358.air.model.Operations.Branch"),
    OPERATIONS_CASE(Form.RECORD, "io.github.gustavo2358.air.model.Operations.Case"),
    OPERATIONS_COPY_BYTES(Form.RECORD, "io.github.gustavo2358.air.model.Operations.CopyBytes"),
    OPERATIONS_DISPATCH(Form.RECORD, "io.github.gustavo2358.air.model.Operations.Dispatch"),
    OPERATIONS_HALT(Form.RECORD, "io.github.gustavo2358.air.model.Operations.Halt"),
    OPERATIONS_HALT_KIND(Form.ENUM, "io.github.gustavo2358.air.model.Operations.HaltKind"),
    OPERATIONS_HAVOC_MAY(Form.RECORD, "io.github.gustavo2358.air.model.Operations.HavocMay"),
    OPERATIONS_HAVOC_MUST(Form.RECORD, "io.github.gustavo2358.air.model.Operations.HavocMust"),
    OPERATIONS_HEADER(Form.RECORD, "io.github.gustavo2358.air.model.Operations.Header"),
    OPERATIONS_INDIRECT_JUMP(Form.RECORD, "io.github.gustavo2358.air.model.Operations.IndirectJump"),
    OPERATIONS_INVOKE(Form.RECORD, "io.github.gustavo2358.air.model.Operations.Invoke"),
    OPERATIONS_JUMP(Form.RECORD, "io.github.gustavo2358.air.model.Operations.Jump"),
    OPERATIONS_LOCAL_BOUNDARY(Form.RECORD, "io.github.gustavo2358.air.model.Operations.LocalBoundary"),
    OPERATIONS_LOCAL_INVOKE(Form.RECORD, "io.github.gustavo2358.air.model.Operations.LocalInvoke"),
    OPERATIONS_LOCAL_RESUME(Form.RECORD, "io.github.gustavo2358.air.model.Operations.LocalResume"),
    OPERATIONS_LOCAL_UNWIND(Form.RECORD, "io.github.gustavo2358.air.model.Operations.LocalUnwind"),
    OPERATIONS_NOP(Form.RECORD, "io.github.gustavo2358.air.model.Operations.Nop"),
    OPERATIONS_OPAQUE(Form.RECORD, "io.github.gustavo2358.air.model.Operations.Opaque"),
    OPERATIONS_RAISE(Form.RECORD, "io.github.gustavo2358.air.model.Operations.Raise"),
    OPERATIONS_REENTRY_GUARD(Form.RECORD, "io.github.gustavo2358.air.model.Operations.ReentryGuard"),
    OPERATIONS_RESUME_ROUTE(Form.RECORD, "io.github.gustavo2358.air.model.Operations.ResumeRoute"),
    OPERATIONS_RETURN(Form.RECORD, "io.github.gustavo2358.air.model.Operations.Return"),
    ORIGINS_ARTIFACT(Form.RECORD, "io.github.gustavo2358.air.model.Origins.Artifact"),
    ORIGINS_COLUMN_UNIT(Form.ENUM, "io.github.gustavo2358.air.model.Origins.ColumnUnit"),
    ORIGINS_CONTRACTUAL(Form.RECORD, "io.github.gustavo2358.air.model.Origins.Contractual"),
    ORIGINS_DERIVED(Form.RECORD, "io.github.gustavo2358.air.model.Origins.Derived"),
    ORIGINS_INCLUDE_FRAME(Form.RECORD, "io.github.gustavo2358.air.model.Origins.IncludeFrame"),
    ORIGINS_LINE_COLUMNS(Form.RECORD, "io.github.gustavo2358.air.model.Origins.LineColumns"),
    ORIGINS_LOCATION(Form.UNION, "io.github.gustavo2358.air.model.Origins.Location"),
    ORIGINS_OFFSETS(Form.RECORD, "io.github.gustavo2358.air.model.Origins.Offsets"),
    ORIGINS_ORIGIN(Form.UNION, "io.github.gustavo2358.air.model.Origins.Origin"),
    ORIGINS_POSITION(Form.RECORD, "io.github.gustavo2358.air.model.Origins.Position"),
    ORIGINS_SPAN(Form.RECORD, "io.github.gustavo2358.air.model.Origins.Span"),
    ORIGINS_UNAVAILABLE(Form.RECORD, "io.github.gustavo2358.air.model.Origins.Unavailable"),
    ORIGINS_WRITTEN(Form.RECORD, "io.github.gustavo2358.air.model.Origins.Written"),
    PLACE(Form.UNION, "io.github.gustavo2358.air.model.Place"),
    PLACES_CHOICE(Form.RECORD, "io.github.gustavo2358.air.model.Places.Choice"),
    PLACES_OBJECT_PLACE(Form.RECORD, "io.github.gustavo2358.air.model.Places.ObjectPlace"),
    PLACES_REGION_SLICE(Form.RECORD, "io.github.gustavo2358.air.model.Places.RegionSlice"),
    PROOFS_ASSERTION(Form.UNION, "io.github.gustavo2358.air.model.Proofs.Assertion"),
    PROOFS_CALL_PARAMETER_DOMAIN(Form.RECORD, "io.github.gustavo2358.air.model.Proofs.CallParameterDomain"),
    PROOFS_CALL_RESULT_DOMAIN(Form.RECORD, "io.github.gustavo2358.air.model.Proofs.CallResultDomain"),
    PROOFS_CELL_DOMAIN(Form.RECORD, "io.github.gustavo2358.air.model.Proofs.CellDomain"),
    PROOFS_DISJOINT_STORAGE(Form.RECORD, "io.github.gustavo2358.air.model.Proofs.DisjointStorage"),
    PROOFS_DOMAIN_PROOF_SCOPE(Form.UNION, "io.github.gustavo2358.air.model.Proofs.DomainProofScope"),
    PROOFS_DOMAIN_SUBJECT(Form.UNION, "io.github.gustavo2358.air.model.Proofs.DomainSubject"),
    PROOFS_ENTRY_DOMAIN(Form.RECORD, "io.github.gustavo2358.air.model.Proofs.EntryDomain"),
    PROOFS_EXTERNAL_PARAMETER_DOMAIN(Form.RECORD, "io.github.gustavo2358.air.model.Proofs.ExternalParameterDomain"),
    PROOFS_EXTERNAL_RESULT_DOMAIN(Form.RECORD, "io.github.gustavo2358.air.model.Proofs.ExternalResultDomain"),
    PROOFS_INTERSECTION(Form.RECORD, "io.github.gustavo2358.air.model.Proofs.Intersection"),
    PROOFS_INVOCATION_DOMAIN(Form.RECORD, "io.github.gustavo2358.air.model.Proofs.InvocationDomain"),
    PROOFS_OBJECT_DOMAIN(Form.RECORD, "io.github.gustavo2358.air.model.Proofs.ObjectDomain"),
    PROOFS_OPERAND_DOMAIN(Form.RECORD, "io.github.gustavo2358.air.model.Proofs.OperandDomain"),
    PROOFS_OPERATION_DOMAIN(Form.RECORD, "io.github.gustavo2358.air.model.Proofs.OperationDomain"),
    PROOFS_PARAMETER_DOMAIN(Form.RECORD, "io.github.gustavo2358.air.model.Proofs.ParameterDomain"),
    PROOFS_PREMISE(Form.RECORD, "io.github.gustavo2358.air.model.Proofs.Premise"),
    PROOFS_PUBLICATION_DOMAIN(Form.ENUM, "io.github.gustavo2358.air.model.Proofs.PublicationDomain"),
    PROOFS_RESULT_DOMAIN(Form.RECORD, "io.github.gustavo2358.air.model.Proofs.ResultDomain"),
    PROOFS_SAME_DOMAIN(Form.RECORD, "io.github.gustavo2358.air.model.Proofs.SameDomain"),
    PROOFS_UNIT_DOMAIN(Form.RECORD, "io.github.gustavo2358.air.model.Proofs.UnitDomain"),
    PUBLICATION(Form.RECORD, "io.github.gustavo2358.air.model.Publication"),
    SCOPES_ALL_CONTROL(Form.RECORD, "io.github.gustavo2358.air.model.Scopes.AllControl"),
    SCOPES_ALL_MEMORY(Form.RECORD, "io.github.gustavo2358.air.model.Scopes.AllMemory"),
    SCOPES_ANY_RESOURCE(Form.ENUM, "io.github.gustavo2358.air.model.Scopes.AnyResource"),
    SCOPES_CONTROL_BOUND(Form.UNION, "io.github.gustavo2358.air.model.Scopes.ControlBound"),
    SCOPES_CONTROL_SCOPE(Form.UNION, "io.github.gustavo2358.air.model.Scopes.ControlScope"),
    SCOPES_CONTROL_UNION(Form.RECORD, "io.github.gustavo2358.air.model.Scopes.ControlUnion"),
    SCOPES_DEPENDENCY_BOUND(Form.UNION, "io.github.gustavo2358.air.model.Scopes.DependencyBound"),
    SCOPES_ENTITY_SCOPE(Form.RECORD, "io.github.gustavo2358.air.model.Scopes.EntityScope"),
    SCOPES_FACT_SCOPE(Form.UNION, "io.github.gustavo2358.air.model.Scopes.FactScope"),
    SCOPES_LABELS_CONTROL(Form.RECORD, "io.github.gustavo2358.air.model.Scopes.LabelsControl"),
    SCOPES_MEMORY_BOUND(Form.UNION, "io.github.gustavo2358.air.model.Scopes.MemoryBound"),
    SCOPES_MEMORY_SCOPE(Form.UNION, "io.github.gustavo2358.air.model.Scopes.MemoryScope"),
    SCOPES_MEMORY_UNION(Form.RECORD, "io.github.gustavo2358.air.model.Scopes.MemoryUnion"),
    SCOPES_NO_CONTROL(Form.ENUM, "io.github.gustavo2358.air.model.Scopes.NoControl"),
    SCOPES_NO_MEMORY(Form.ENUM, "io.github.gustavo2358.air.model.Scopes.NoMemory"),
    SCOPES_NO_RESOURCES(Form.ENUM, "io.github.gustavo2358.air.model.Scopes.NoResources"),
    SCOPES_OBJECTS_MEMORY(Form.RECORD, "io.github.gustavo2358.air.model.Scopes.ObjectsMemory"),
    SCOPES_PUBLICATION_SCOPE(Form.RECORD, "io.github.gustavo2358.air.model.Scopes.PublicationScope"),
    SCOPES_RESOURCE_CATEGORIES(Form.RECORD, "io.github.gustavo2358.air.model.Scopes.ResourceCategories"),
    SCOPES_STORAGE_MEMORY(Form.RECORD, "io.github.gustavo2358.air.model.Scopes.StorageMemory"),
    SCOPES_UNIT_CONTROL(Form.RECORD, "io.github.gustavo2358.air.model.Scopes.UnitControl"),
    SCOPES_UNIT_SCOPE(Form.RECORD, "io.github.gustavo2358.air.model.Scopes.UnitScope"),
    SCOPES_VISIBLE_MEMORY(Form.RECORD, "io.github.gustavo2358.air.model.Scopes.VisibleMemory"),
    SCOPES_WITHIN_CONTROL(Form.RECORD, "io.github.gustavo2358.air.model.Scopes.WithinControl"),
    SCOPES_WITHIN_MEMORY(Form.RECORD, "io.github.gustavo2358.air.model.Scopes.WithinMemory"),
    SEMANTIC_VERSION(Form.RECORD, "io.github.gustavo2358.air.model.SemanticVersion"),
    SEQUENCE(Form.RECORD, "io.github.gustavo2358.air.model.Sequence"),
    TERMINATOR(Form.UNION, "io.github.gustavo2358.air.model.Terminator"),
    TYPES_BUILTIN(Form.ENUM, "io.github.gustavo2358.air.model.Types.Builtin"),
    TYPES_EXTENSION_TYPE(Form.RECORD, "io.github.gustavo2358.air.model.Types.ExtensionType"),
    TYPES_KNOWN(Form.RECORD, "io.github.gustavo2358.air.model.Types.Known"),
    TYPES_LABEL_TYPE(Form.RECORD, "io.github.gustavo2358.air.model.Types.LabelType"),
    TYPES_TYPE(Form.UNION, "io.github.gustavo2358.air.model.Types.Type"),
    TYPES_TYPE_REF(Form.UNION, "io.github.gustavo2358.air.model.Types.TypeRef"),
    TYPES_UNKNOWN_TYPE(Form.RECORD, "io.github.gustavo2358.air.model.Types.UnknownType"),
    UNIT(Form.RECORD, "io.github.gustavo2358.air.model.Unit"),
    UNIT_BODY_AVAILABILITY(Form.ENUM, "io.github.gustavo2358.air.model.Unit.BodyAvailability"),
    VALUES_BOOL_VALUE(Form.RECORD, "io.github.gustavo2358.air.model.Values.BoolValue"),
    VALUES_BYTES_VALUE(Form.RECORD, "io.github.gustavo2358.air.model.Values.BytesValue"),
    VALUES_DECIMAL_VALUE(Form.RECORD, "io.github.gustavo2358.air.model.Values.DecimalValue"),
    VALUES_INT_VALUE(Form.RECORD, "io.github.gustavo2358.air.model.Values.IntValue"),
    VALUES_LABEL_VALUE(Form.RECORD, "io.github.gustavo2358.air.model.Values.LabelValue"),
    VALUES_LITERAL_VALUE(Form.UNION, "io.github.gustavo2358.air.model.Values.LiteralValue"),
    VALUES_TEXT_VALUE(Form.RECORD, "io.github.gustavo2358.air.model.Values.TextValue");

    public enum Form { TEXT, INTEGER, BOOLEAN, SMALL_INTEGER, LIST, OPTIONAL, RECORD, ENUM, UNION }
    public record Slot(AirShape owner, int index, String name, AirShape value, AirShape element) { }
    private final Form form;
    private final String modelName;
    private static final Slot[][] SLOTS = new Slot[values().length][];
    private static final String[][] ENUMS = new String[values().length][];
    AirShape(Form form, String modelName) { this.form = form; this.modelName = modelName; }
    public Form form() { return form; }
    public String modelName() { return modelName; }
    public int fieldCount() { return SLOTS[ordinal()] == null ? 0 : SLOTS[ordinal()].length; }
    public Slot field(int index) {
        if (index < 0 || index >= fieldCount()) throw new IndexOutOfBoundsException("AIR field " + index);
        return SLOTS[ordinal()][index];
    }
    public int enumCount() { return ENUMS[ordinal()] == null ? 0 : ENUMS[ordinal()].length; }
    public String enumName(int index) {
        if (index < 0 || index >= enumCount()) throw new IndexOutOfBoundsException("AIR enum " + index);
        return ENUMS[ordinal()][index];
    }
    /** Exact subtype closure; unrelated unknown types never act as wildcards. */
    public boolean accepts(AirShape actual) {
        if (this == actual) return true;
        return switch (this) {
            case ARTIFACTS_RELATION_TARGET -> switch (actual) {
                case ARTIFACTS_EXTERNAL_ARTIFACT, ARTIFACTS_INTERNAL_ARTIFACT, ARTIFACTS_RELATION_TARGET -> true;
                default -> false;
            };
            case CONTROL_CONTROL_ALTERNATIVE -> switch (actual) {
                case CONTROL_ANY_EXCEPTION, CONTROL_CONTINUE_ALTERNATIVE, CONTROL_CONTROL_ALTERNATIVE, CONTROL_DIVERGE, CONTROL_EXCEPTIONAL, CONTROL_HALT_ALTERNATIVE, CONTROL_INVOCATION_ALTERNATIVE, CONTROL_JUMP_ALTERNATIVE, CONTROL_NORMAL, CONTROL_RETURN_ALTERNATIVE -> true;
                default -> false;
            };
            case CONTROL_EXCEPTION_DESTINATION -> switch (actual) {
                case CONTROL_EXCEPTION_DESTINATION, CONTROL_HANDLER, CONTROL_PROPAGATE -> true;
                default -> false;
            };
            case CONTROL_INVOCATION_ALTERNATIVE -> switch (actual) {
                case CONTROL_ANY_EXCEPTION, CONTROL_DIVERGE, CONTROL_EXCEPTIONAL, CONTROL_HALT_ALTERNATIVE, CONTROL_INVOCATION_ALTERNATIVE, CONTROL_NORMAL -> true;
                default -> false;
            };
            case CONTROL_OUTCOME_KEY -> switch (actual) {
                case CONTROL_DIVERGE_OUTCOME, CONTROL_EXCEPTION_OUTCOME, CONTROL_HALT_OUTCOME, CONTROL_NORMAL_OUTCOME, CONTROL_OTHER_EXCEPTION_OUTCOME, CONTROL_OUTCOME_KEY -> true;
                default -> false;
            };
            case CONTROL_PROGRAM_POINT -> switch (actual) {
                case CONTROL_AFTER, CONTROL_BEFORE, CONTROL_ENTRY_POINT, CONTROL_EXIT_POINT, CONTROL_PROGRAM_POINT -> true;
                default -> false;
            };
            case ENTRIES_INITIAL_VALUE -> switch (actual) {
                case ENTRIES_EXTERNAL_UNKNOWN, ENTRIES_INITIAL_VALUE, ENTRIES_LITERAL_INITIAL, ENTRIES_PARAMETER_INITIAL, ENTRIES_POSSIBLE_LITERALS, ENTRIES_PRESERVE, ENTRIES_UNINITIALIZED -> true;
                default -> false;
            };
            case EXPRESSION -> switch (actual) {
                case EXPRESSION, EXPRESSIONS_BINARY, EXPRESSIONS_FILL_TEXT, EXPRESSIONS_FIT_DECIMAL, EXPRESSIONS_FIT_TEXT, EXPRESSIONS_FORMAT_DECIMAL, EXPRESSIONS_INTEGER_DIGITS, EXPRESSIONS_LITERAL, EXPRESSIONS_PARSE_INTEGER, EXPRESSIONS_QUANTIZE, EXPRESSIONS_READ, EXPRESSIONS_SLICE_TEXT, EXPRESSIONS_TRIM_RIGHT, EXPRESSIONS_UNARY, EXPRESSIONS_UNKNOWN, EXPRESSIONS_WRAP_INTEGER -> true;
                default -> false;
            };
            case IDS_ID -> switch (actual) {
                case IDS_ARTIFACT_ID, IDS_ARTIFACT_RELATION_ID, IDS_COMPLETION_PORT_ID, IDS_ENTRY_ID, IDS_ID, IDS_LABEL_ID, IDS_OBJECT_ID, IDS_OPERAND_ID, IDS_OPERATION_ID, IDS_ORIGIN_ID, IDS_PREMISE_ID, IDS_PUBLICATION_ID, IDS_RESOURCE_ID, IDS_STORAGE_ID, IDS_UNCERTAINTY_ID, IDS_UNIT_ID -> true;
                default -> false;
            };
            case IDS_OPERAND_OWNER -> switch (actual) {
                case IDS_ENTRY_OWNER, IDS_OPERAND_OWNER, IDS_OPERATION_OWNER -> true;
                default -> false;
            };
            case INSTRUCTION -> switch (actual) {
                case INSTRUCTION, OPERATIONS_ASSIGN, OPERATIONS_COPY_BYTES, OPERATIONS_HAVOC_MAY, OPERATIONS_HAVOC_MUST, OPERATIONS_NOP -> true;
                default -> false;
            };
            case INTERACTIONS_ARGUMENT -> switch (actual) {
                case INTERACTIONS_ARGUMENT, INTERACTIONS_COPY_ARGUMENT, INTERACTIONS_REFERENCE_ARGUMENT, INTERACTIONS_VALUE_ARGUMENT -> true;
                default -> false;
            };
            case INTERACTIONS_CONTRACT_KNOWLEDGE -> switch (actual) {
                case INTERACTIONS_CONTRACT_KNOWLEDGE, INTERACTIONS_KNOWN_CONTRACT, INTERACTIONS_UNKNOWN_CONTRACT -> true;
                default -> false;
            };
            case INTERACTIONS_INVOCATION_SIGNATURE -> switch (actual) {
                case INTERACTIONS_ENTRY_SIGNATURE, INTERACTIONS_EXTERNAL_SIGNATURE, INTERACTIONS_INVOCATION_SIGNATURE -> true;
                default -> false;
            };
            case INTERACTIONS_MODE_KNOWLEDGE -> switch (actual) {
                case INTERACTIONS_KNOWN_MODE, INTERACTIONS_MODE_KNOWLEDGE, INTERACTIONS_UNKNOWN_MODE -> true;
                default -> false;
            };
            case INTERACTIONS_NAME_POLICY -> switch (actual) {
                case INTERACTIONS_EXACT_NAME, INTERACTIONS_EXTENSION_NAME, INTERACTIONS_NAME_POLICY, INTERACTIONS_UNKNOWN_NAME -> true;
                default -> false;
            };
            case INTERACTIONS_PARAMETER_BINDING -> switch (actual) {
                case INTERACTIONS_EXTERNAL_BINDING, INTERACTIONS_OBJECT_BINDING, INTERACTIONS_PARAMETER_BINDING, INTERACTIONS_UNKNOWN_PARAMETER_BINDING -> true;
                default -> false;
            };
            case INTERACTIONS_RESOURCE_DESCRIPTION -> switch (actual) {
                case INTERACTIONS_COMPUTED_RESOURCE, INTERACTIONS_INTERNAL_TARGET, INTERACTIONS_LITERAL_TARGET, INTERACTIONS_LOCAL_RESOURCE, INTERACTIONS_RESOURCE_DESCRIPTION, INTERACTIONS_UNKNOWN_RESOURCE -> true;
                default -> false;
            };
            case INTERACTIONS_TARGET -> switch (actual) {
                case INTERACTIONS_COMPUTED_TARGET, INTERACTIONS_INTERNAL_TARGET, INTERACTIONS_LITERAL_TARGET, INTERACTIONS_TARGET -> true;
                default -> false;
            };
            case INTERACTIONS_UNKNOWN_BOUND -> switch (actual) {
                case INTERACTIONS_NO_REMAINDER, INTERACTIONS_UNKNOWN_BOUND, INTERACTIONS_UNKNOWN_REMAINDER -> true;
                default -> false;
            };
            case MEMORY_BINDING -> switch (actual) {
                case MEMORY_ALIAS_BINDING, MEMORY_ALTERNATIVES_BINDING, MEMORY_BINDING, MEMORY_CELL_BINDING, MEMORY_UNKNOWN_BINDING, MEMORY_VIEW_BINDING -> true;
                default -> false;
            };
            case MEMORY_CODEC -> switch (actual) {
                case MEMORY_ASCII_TEXT, MEMORY_BINARY_CODEC, MEMORY_CODEC, MEMORY_EXTENSION_CODEC, MEMORY_IDENTITY_BYTES, MEMORY_UNKNOWN_CODEC -> true;
                default -> false;
            };
            case MEMORY_STORAGE -> switch (actual) {
                case MEMORY_CELL, MEMORY_REGION, MEMORY_STORAGE -> true;
                default -> false;
            };
            case OPERAND -> switch (actual) {
                case EXPRESSION, EXPRESSIONS_BINARY, EXPRESSIONS_FILL_TEXT, EXPRESSIONS_FIT_DECIMAL, EXPRESSIONS_FIT_TEXT, EXPRESSIONS_FORMAT_DECIMAL, EXPRESSIONS_INTEGER_DIGITS, EXPRESSIONS_LITERAL, EXPRESSIONS_PARSE_INTEGER, EXPRESSIONS_QUANTIZE, EXPRESSIONS_READ, EXPRESSIONS_SLICE_TEXT, EXPRESSIONS_TRIM_RIGHT, EXPRESSIONS_UNARY, EXPRESSIONS_UNKNOWN, EXPRESSIONS_WRAP_INTEGER, OPERAND, PLACE, PLACES_CHOICE, PLACES_OBJECT_PLACE, PLACES_REGION_SLICE -> true;
                default -> false;
            };
            case ORIGINS_LOCATION -> switch (actual) {
                case ORIGINS_LINE_COLUMNS, ORIGINS_LOCATION, ORIGINS_OFFSETS -> true;
                default -> false;
            };
            case ORIGINS_ORIGIN -> switch (actual) {
                case ORIGINS_CONTRACTUAL, ORIGINS_DERIVED, ORIGINS_ORIGIN, ORIGINS_UNAVAILABLE, ORIGINS_WRITTEN -> true;
                default -> false;
            };
            case PLACE -> switch (actual) {
                case PLACE, PLACES_CHOICE, PLACES_OBJECT_PLACE, PLACES_REGION_SLICE -> true;
                default -> false;
            };
            case PROOFS_ASSERTION -> switch (actual) {
                case PROOFS_ASSERTION, PROOFS_DISJOINT_STORAGE, PROOFS_SAME_DOMAIN -> true;
                default -> false;
            };
            case PROOFS_DOMAIN_PROOF_SCOPE -> switch (actual) {
                case PROOFS_DOMAIN_PROOF_SCOPE, PROOFS_ENTRY_DOMAIN, PROOFS_INTERSECTION, PROOFS_INVOCATION_DOMAIN, PROOFS_OPERATION_DOMAIN, PROOFS_PUBLICATION_DOMAIN, PROOFS_UNIT_DOMAIN -> true;
                default -> false;
            };
            case PROOFS_DOMAIN_SUBJECT -> switch (actual) {
                case PROOFS_CALL_PARAMETER_DOMAIN, PROOFS_CALL_RESULT_DOMAIN, PROOFS_CELL_DOMAIN, PROOFS_DOMAIN_SUBJECT, PROOFS_EXTERNAL_PARAMETER_DOMAIN, PROOFS_EXTERNAL_RESULT_DOMAIN, PROOFS_OBJECT_DOMAIN, PROOFS_OPERAND_DOMAIN, PROOFS_PARAMETER_DOMAIN, PROOFS_RESULT_DOMAIN -> true;
                default -> false;
            };
            case SCOPES_CONTROL_BOUND -> switch (actual) {
                case SCOPES_CONTROL_BOUND, SCOPES_NO_CONTROL, SCOPES_WITHIN_CONTROL -> true;
                default -> false;
            };
            case SCOPES_CONTROL_SCOPE -> switch (actual) {
                case SCOPES_ALL_CONTROL, SCOPES_CONTROL_SCOPE, SCOPES_CONTROL_UNION, SCOPES_LABELS_CONTROL, SCOPES_UNIT_CONTROL -> true;
                default -> false;
            };
            case SCOPES_DEPENDENCY_BOUND -> switch (actual) {
                case SCOPES_ANY_RESOURCE, SCOPES_DEPENDENCY_BOUND, SCOPES_NO_RESOURCES, SCOPES_RESOURCE_CATEGORIES -> true;
                default -> false;
            };
            case SCOPES_FACT_SCOPE -> switch (actual) {
                case SCOPES_ENTITY_SCOPE, SCOPES_FACT_SCOPE, SCOPES_PUBLICATION_SCOPE, SCOPES_UNIT_SCOPE -> true;
                default -> false;
            };
            case SCOPES_MEMORY_BOUND -> switch (actual) {
                case SCOPES_MEMORY_BOUND, SCOPES_NO_MEMORY, SCOPES_WITHIN_MEMORY -> true;
                default -> false;
            };
            case SCOPES_MEMORY_SCOPE -> switch (actual) {
                case SCOPES_ALL_MEMORY, SCOPES_MEMORY_SCOPE, SCOPES_MEMORY_UNION, SCOPES_OBJECTS_MEMORY, SCOPES_STORAGE_MEMORY, SCOPES_VISIBLE_MEMORY -> true;
                default -> false;
            };
            case TERMINATOR -> switch (actual) {
                case OPERATIONS_BRANCH, OPERATIONS_DISPATCH, OPERATIONS_HALT, OPERATIONS_INDIRECT_JUMP, OPERATIONS_INVOKE, OPERATIONS_JUMP, OPERATIONS_LOCAL_BOUNDARY, OPERATIONS_LOCAL_INVOKE, OPERATIONS_LOCAL_RESUME, OPERATIONS_LOCAL_UNWIND, OPERATIONS_OPAQUE, OPERATIONS_RAISE, OPERATIONS_RETURN, TERMINATOR -> true;
                default -> false;
            };
            case TYPES_TYPE -> switch (actual) {
                case TYPES_BUILTIN, TYPES_EXTENSION_TYPE, TYPES_LABEL_TYPE, TYPES_TYPE -> true;
                default -> false;
            };
            case TYPES_TYPE_REF -> switch (actual) {
                case TYPES_KNOWN, TYPES_TYPE_REF, TYPES_UNKNOWN_TYPE -> true;
                default -> false;
            };
            case VALUES_LITERAL_VALUE -> switch (actual) {
                case VALUES_BOOL_VALUE, VALUES_BYTES_VALUE, VALUES_DECIMAL_VALUE, VALUES_INT_VALUE, VALUES_LABEL_VALUE, VALUES_LITERAL_VALUE, VALUES_TEXT_VALUE -> true;
                default -> false;
            };
            default -> false;
        };
    }

    static {
        SLOTS[ARTIFACTS_EXTERNAL_ARTIFACT.ordinal()] = new Slot[] {new Slot(ARTIFACTS_EXTERNAL_ARTIFACT, 0, "resource", INTERACTIONS_LITERAL_TARGET, null)};
        SLOTS[ARTIFACTS_INTERNAL_ARTIFACT.ordinal()] = new Slot[] {new Slot(ARTIFACTS_INTERNAL_ARTIFACT, 0, "artifact", IDS_ARTIFACT_ID, null)};
        SLOTS[ARTIFACTS_RELATION.ordinal()] = new Slot[] {new Slot(ARTIFACTS_RELATION, 0, "id", IDS_ARTIFACT_RELATION_ID, null), new Slot(ARTIFACTS_RELATION, 1, "source", IDS_ARTIFACT_ID, null), new Slot(ARTIFACTS_RELATION, 2, "destination", ARTIFACTS_RELATION_TARGET, null), new Slot(ARTIFACTS_RELATION, 3, "kind", TEXT, null), new Slot(ARTIFACTS_RELATION, 4, "origin", IDS_ORIGIN_ID, null), new Slot(ARTIFACTS_RELATION, 5, "coverage", EVIDENCE_COVERAGE_STATUS, null)};
        SLOTS[CAPABILITIES_CAPABILITY.ordinal()] = new Slot[] {new Slot(CAPABILITIES_CAPABILITY, 0, "name", TEXT, null), new Slot(CAPABILITIES_CAPABILITY, 1, "version", TEXT, null)};
        SLOTS[CAPABILITIES_MANIFEST.ordinal()] = new Slot[] {new Slot(CAPABILITIES_MANIFEST, 0, "required", LIST, CAPABILITIES_CAPABILITY), new Slot(CAPABILITIES_MANIFEST, 1, "provided", LIST, CAPABILITIES_CAPABILITY)};
        SLOTS[CONTROL_AFTER.ordinal()] = new Slot[] {new Slot(CONTROL_AFTER, 0, "operation", IDS_OPERATION_ID, null), new Slot(CONTROL_AFTER, 1, "outcome", CONTROL_OUTCOME_KEY, null)};
        SLOTS[CONTROL_ANY_EXCEPTION.ordinal()] = new Slot[] {new Slot(CONTROL_ANY_EXCEPTION, 0, "destination", CONTROL_EXCEPTION_DESTINATION, null)};
        SLOTS[CONTROL_BEFORE.ordinal()] = new Slot[] {new Slot(CONTROL_BEFORE, 0, "operation", IDS_OPERATION_ID, null)};
        ENUMS[CONTROL_CONTINUE_ALTERNATIVE.ordinal()] = new String[] {"INSTANCE"};
        SLOTS[CONTROL_CONTROL_ENVELOPE.ordinal()] = new Slot[] {new Slot(CONTROL_CONTROL_ENVELOPE, 0, "known", LIST, CONTROL_CONTROL_ALTERNATIVE), new Slot(CONTROL_CONTROL_ENVELOPE, 1, "remainder", SCOPES_CONTROL_BOUND, null)};
        ENUMS[CONTROL_DIVERGE.ordinal()] = new String[] {"INSTANCE"};
        ENUMS[CONTROL_DIVERGE_OUTCOME.ordinal()] = new String[] {"INSTANCE"};
        SLOTS[CONTROL_ENTRY_POINT.ordinal()] = new Slot[] {new Slot(CONTROL_ENTRY_POINT, 0, "entry", IDS_ENTRY_ID, null)};
        SLOTS[CONTROL_EXCEPTION_OUTCOME.ordinal()] = new Slot[] {new Slot(CONTROL_EXCEPTION_OUTCOME, 0, "tag", TEXT, null)};
        SLOTS[CONTROL_EXCEPTIONAL.ordinal()] = new Slot[] {new Slot(CONTROL_EXCEPTIONAL, 0, "tag", TEXT, null), new Slot(CONTROL_EXCEPTIONAL, 1, "destination", CONTROL_EXCEPTION_DESTINATION, null)};
        SLOTS[CONTROL_EXIT_POINT.ordinal()] = new Slot[] {new Slot(CONTROL_EXIT_POINT, 0, "unit", IDS_UNIT_ID, null), new Slot(CONTROL_EXIT_POINT, 1, "outcome", CONTROL_OUTCOME_KEY, null)};
        ENUMS[CONTROL_HALT_ALTERNATIVE.ordinal()] = new String[] {"INSTANCE"};
        ENUMS[CONTROL_HALT_OUTCOME.ordinal()] = new String[] {"INSTANCE"};
        SLOTS[CONTROL_HANDLER.ordinal()] = new Slot[] {new Slot(CONTROL_HANDLER, 0, "label", IDS_LABEL_ID, null)};
        SLOTS[CONTROL_INVOCATION_OUTCOMES.ordinal()] = new Slot[] {new Slot(CONTROL_INVOCATION_OUTCOMES, 0, "known", LIST, CONTROL_INVOCATION_ALTERNATIVE), new Slot(CONTROL_INVOCATION_OUTCOMES, 1, "remainder", SCOPES_CONTROL_BOUND, null)};
        SLOTS[CONTROL_JUMP_ALTERNATIVE.ordinal()] = new Slot[] {new Slot(CONTROL_JUMP_ALTERNATIVE, 0, "label", IDS_LABEL_ID, null)};
        SLOTS[CONTROL_NORMAL.ordinal()] = new Slot[] {new Slot(CONTROL_NORMAL, 0, "label", IDS_LABEL_ID, null)};
        ENUMS[CONTROL_NORMAL_OUTCOME.ordinal()] = new String[] {"INSTANCE"};
        ENUMS[CONTROL_OTHER_EXCEPTION_OUTCOME.ordinal()] = new String[] {"INSTANCE"};
        ENUMS[CONTROL_PROPAGATE.ordinal()] = new String[] {"INSTANCE"};
        ENUMS[CONTROL_RETURN_ALTERNATIVE.ordinal()] = new String[] {"INSTANCE"};
        ENUMS[DECIMAL_TEXT_KIND.ordinal()] = new String[] {"DIGITS", "SUPPRESS_SPACE", "SUPPRESS_STAR", "INSERT", "RADIX", "SIGN", "FLOAT_SIGN"};
        SLOTS[DECIMAL_TEXT_PART.ordinal()] = new Slot[] {new Slot(DECIMAL_TEXT_PART, 0, "kind", DECIMAL_TEXT_KIND, null), new Slot(DECIMAL_TEXT_PART, 1, "count", INTEGER, null), new Slot(DECIMAL_TEXT_PART, 2, "text", TEXT, null), new Slot(DECIMAL_TEXT_PART, 3, "negative", TEXT, null)};
        SLOTS[ENTRIES_COMPLETION_PORT.ordinal()] = new Slot[] {new Slot(ENTRIES_COMPLETION_PORT, 0, "id", IDS_COMPLETION_PORT_ID, null), new Slot(ENTRIES_COMPLETION_PORT, 1, "origin", IDS_ORIGIN_ID, null)};
        SLOTS[ENTRIES_ENTRY.ordinal()] = new Slot[] {new Slot(ENTRIES_ENTRY, 0, "id", IDS_ENTRY_ID, null), new Slot(ENTRIES_ENTRY, 1, "initialLabel", OPTIONAL, IDS_LABEL_ID), new Slot(ENTRIES_ENTRY, 2, "signature", INTERACTIONS_SIGNATURE, null), new Slot(ENTRIES_ENTRY, 3, "state", ENTRIES_ENTRY_STATE, null), new Slot(ENTRIES_ENTRY, 4, "origin", IDS_ORIGIN_ID, null)};
        SLOTS[ENTRIES_ENTRY_STATE.ordinal()] = new Slot[] {new Slot(ENTRIES_ENTRY_STATE, 0, "conditions", LIST, ENTRIES_INITIAL_CONDITION), new Slot(ENTRIES_ENTRY_STATE, 1, "uncertainties", LIST, IDS_UNCERTAINTY_ID)};
        SLOTS[ENTRIES_EXTERNAL_UNKNOWN.ordinal()] = new Slot[] {new Slot(ENTRIES_EXTERNAL_UNKNOWN, 0, "reason", IDS_UNCERTAINTY_ID, null)};
        SLOTS[ENTRIES_INITIAL_CONDITION.ordinal()] = new Slot[] {new Slot(ENTRIES_INITIAL_CONDITION, 0, "place", PLACE, null), new Slot(ENTRIES_INITIAL_CONDITION, 1, "value", ENTRIES_INITIAL_VALUE, null), new Slot(ENTRIES_INITIAL_CONDITION, 2, "origin", IDS_ORIGIN_ID, null), new Slot(ENTRIES_INITIAL_CONDITION, 3, "premises", LIST, IDS_PREMISE_ID)};
        SLOTS[ENTRIES_LITERAL_INITIAL.ordinal()] = new Slot[] {new Slot(ENTRIES_LITERAL_INITIAL, 0, "value", EXPRESSIONS_LITERAL, null)};
        SLOTS[ENTRIES_PARAMETER_INITIAL.ordinal()] = new Slot[] {new Slot(ENTRIES_PARAMETER_INITIAL, 0, "position", INTEGER, null)};
        SLOTS[ENTRIES_POSSIBLE_LITERALS.ordinal()] = new Slot[] {new Slot(ENTRIES_POSSIBLE_LITERALS, 0, "candidates", LIST, EXPRESSIONS_LITERAL), new Slot(ENTRIES_POSSIBLE_LITERALS, 1, "remainder", IDS_UNCERTAINTY_ID, null)};
        ENUMS[ENTRIES_PRESERVE.ordinal()] = new String[] {"INSTANCE"};
        SLOTS[ENTRIES_UNINITIALIZED.ordinal()] = new Slot[] {new Slot(ENTRIES_UNINITIALIZED, 0, "reason", IDS_UNCERTAINTY_ID, null)};
        SLOTS[ENVELOPES_DEPENDENCY_ENVELOPE.ordinal()] = new Slot[] {new Slot(ENVELOPES_DEPENDENCY_ENVELOPE, 0, "known", LIST, ENVELOPES_RESOURCE_USE), new Slot(ENVELOPES_DEPENDENCY_ENVELOPE, 1, "remainder", SCOPES_DEPENDENCY_BOUND, null)};
        SLOTS[ENVELOPES_ENVELOPE.ordinal()] = new Slot[] {new Slot(ENVELOPES_ENVELOPE, 0, "memory", ENVELOPES_MEMORY_ENVELOPE, null), new Slot(ENVELOPES_ENVELOPE, 1, "control", CONTROL_CONTROL_ENVELOPE, null), new Slot(ENVELOPES_ENVELOPE, 2, "dependencies", ENVELOPES_DEPENDENCY_ENVELOPE, null)};
        SLOTS[ENVELOPES_MEMORY_ENVELOPE.ordinal()] = new Slot[] {new Slot(ENVELOPES_MEMORY_ENVELOPE, 0, "knownReads", LIST, IDS_OPERAND_ID), new Slot(ENVELOPES_MEMORY_ENVELOPE, 1, "otherReads", SCOPES_MEMORY_BOUND, null), new Slot(ENVELOPES_MEMORY_ENVELOPE, 2, "knownWrites", LIST, IDS_OPERAND_ID), new Slot(ENVELOPES_MEMORY_ENVELOPE, 3, "otherWrites", SCOPES_MEMORY_BOUND, null), new Slot(ENVELOPES_MEMORY_ENVELOPE, 4, "mustOverwrite", LIST, IDS_OPERAND_ID)};
        SLOTS[ENVELOPES_RESOURCE_USE.ordinal()] = new Slot[] {new Slot(ENVELOPES_RESOURCE_USE, 0, "action", TEXT, null), new Slot(ENVELOPES_RESOURCE_USE, 1, "target", INTERACTIONS_RESOURCE_DESCRIPTION, null), new Slot(ENVELOPES_RESOURCE_USE, 2, "point", CONTROL_PROGRAM_POINT, null), new Slot(ENVELOPES_RESOURCE_USE, 3, "origin", IDS_ORIGIN_ID, null)};
        SLOTS[EVIDENCE_CLAIM.ordinal()] = new Slot[] {new Slot(EVIDENCE_CLAIM, 0, "scope", SCOPES_FACT_SCOPE, null), new Slot(EVIDENCE_CLAIM, 1, "status", EVIDENCE_PRECISION_STATUS, null), new Slot(EVIDENCE_CLAIM, 2, "reasons", LIST, IDS_UNCERTAINTY_ID)};
        SLOTS[EVIDENCE_COVERAGE.ordinal()] = new Slot[] {new Slot(EVIDENCE_COVERAGE, 0, "inventory", EVIDENCE_INVENTORY_STATUS, null), new Slot(EVIDENCE_COVERAGE, 1, "scope", SCOPES_FACT_SCOPE, null), new Slot(EVIDENCE_COVERAGE, 2, "items", LIST, EVIDENCE_COVERAGE_ITEM), new Slot(EVIDENCE_COVERAGE, 3, "uncertainties", LIST, IDS_UNCERTAINTY_ID)};
        SLOTS[EVIDENCE_COVERAGE_ITEM.ordinal()] = new Slot[] {new Slot(EVIDENCE_COVERAGE_ITEM, 0, "sourceKey", TEXT, null), new Slot(EVIDENCE_COVERAGE_ITEM, 1, "origin", IDS_ORIGIN_ID, null), new Slot(EVIDENCE_COVERAGE_ITEM, 2, "status", EVIDENCE_COVERAGE_STATUS, null), new Slot(EVIDENCE_COVERAGE_ITEM, 3, "outputs", LIST, IDS_ID), new Slot(EVIDENCE_COVERAGE_ITEM, 4, "uncertainties", LIST, IDS_UNCERTAINTY_ID), new Slot(EVIDENCE_COVERAGE_ITEM, 5, "elimination", OPTIONAL, EVIDENCE_ELIMINATION)};
        ENUMS[EVIDENCE_COVERAGE_STATUS.ordinal()] = new String[] {"MODELED", "ABSTRACTED", "UNSUPPORTED", "INPUT_MISSING"};
        ENUMS[EVIDENCE_DIMENSION.ordinal()] = new String[] {"CONTROL", "STORAGE", "EFFECTS", "VALUES", "DEPENDENCIES"};
        SLOTS[EVIDENCE_ELIMINATION.ordinal()] = new Slot[] {new Slot(EVIDENCE_ELIMINATION, 0, "rule", TEXT, null), new Slot(EVIDENCE_ELIMINATION, 1, "origin", IDS_ORIGIN_ID, null)};
        ENUMS[EVIDENCE_INVENTORY_STATUS.ordinal()] = new String[] {"COMPLETE", "PARTIAL", "UNAVAILABLE"};
        SLOTS[EVIDENCE_PRECISION.ordinal()] = new Slot[] {new Slot(EVIDENCE_PRECISION, 0, "control", EVIDENCE_CLAIM, null), new Slot(EVIDENCE_PRECISION, 1, "storage", EVIDENCE_CLAIM, null), new Slot(EVIDENCE_PRECISION, 2, "effects", EVIDENCE_CLAIM, null), new Slot(EVIDENCE_PRECISION, 3, "values", EVIDENCE_CLAIM, null), new Slot(EVIDENCE_PRECISION, 4, "dependencies", EVIDENCE_CLAIM, null)};
        ENUMS[EVIDENCE_PRECISION_STATUS.ordinal()] = new String[] {"EXACT", "CONSERVATIVE", "OPEN", "UNAVAILABLE", "NOT_APPLICABLE"};
        SLOTS[EVIDENCE_UNCERTAINTY.ordinal()] = new Slot[] {new Slot(EVIDENCE_UNCERTAINTY, 0, "id", IDS_UNCERTAINTY_ID, null), new Slot(EVIDENCE_UNCERTAINTY, 1, "code", TEXT, null), new Slot(EVIDENCE_UNCERTAINTY, 2, "dimensions", LIST, EVIDENCE_DIMENSION), new Slot(EVIDENCE_UNCERTAINTY, 3, "scope", SCOPES_FACT_SCOPE, null), new Slot(EVIDENCE_UNCERTAINTY, 4, "reason", TEXT, null), new Slot(EVIDENCE_UNCERTAINTY, 5, "origin", IDS_ORIGIN_ID, null)};
        SLOTS[EXPRESSIONS_BINARY.ordinal()] = new Slot[] {new Slot(EXPRESSIONS_BINARY, 0, "header", OPERAND_HEADER, null), new Slot(EXPRESSIONS_BINARY, 1, "operator", EXPRESSIONS_BINARY_OPERATOR, null), new Slot(EXPRESSIONS_BINARY, 2, "left", EXPRESSION, null), new Slot(EXPRESSIONS_BINARY, 3, "right", EXPRESSION, null)};
        ENUMS[EXPRESSIONS_BINARY_OPERATOR.ordinal()] = new String[] {"EQ", "NE", "LT", "LE", "GT", "GE", "AND", "OR", "ADD", "SUB", "MUL", "CONCAT"};
        SLOTS[EXPRESSIONS_FILL_TEXT.ordinal()] = new Slot[] {new Slot(EXPRESSIONS_FILL_TEXT, 0, "header", OPERAND_HEADER, null), new Slot(EXPRESSIONS_FILL_TEXT, 1, "character", EXPRESSION, null), new Slot(EXPRESSIONS_FILL_TEXT, 2, "length", INTEGER, null)};
        SLOTS[EXPRESSIONS_FIT_DECIMAL.ordinal()] = new Slot[] {new Slot(EXPRESSIONS_FIT_DECIMAL, 0, "header", OPERAND_HEADER, null), new Slot(EXPRESSIONS_FIT_DECIMAL, 1, "value", EXPRESSION, null), new Slot(EXPRESSIONS_FIT_DECIMAL, 2, "digits", INTEGER, null), new Slot(EXPRESSIONS_FIT_DECIMAL, 3, "scale", INTEGER, null), new Slot(EXPRESSIONS_FIT_DECIMAL, 4, "absolute", BOOLEAN, null)};
        SLOTS[EXPRESSIONS_FIT_TEXT.ordinal()] = new Slot[] {new Slot(EXPRESSIONS_FIT_TEXT, 0, "header", OPERAND_HEADER, null), new Slot(EXPRESSIONS_FIT_TEXT, 1, "value", EXPRESSION, null), new Slot(EXPRESSIONS_FIT_TEXT, 2, "length", INTEGER, null), new Slot(EXPRESSIONS_FIT_TEXT, 3, "pad", TEXT, null)};
        SLOTS[EXPRESSIONS_FORMAT_DECIMAL.ordinal()] = new Slot[] {new Slot(EXPRESSIONS_FORMAT_DECIMAL, 0, "header", OPERAND_HEADER, null), new Slot(EXPRESSIONS_FORMAT_DECIMAL, 1, "value", EXPRESSION, null), new Slot(EXPRESSIONS_FORMAT_DECIMAL, 2, "parts", LIST, DECIMAL_TEXT_PART)};
        SLOTS[EXPRESSIONS_INTEGER_DIGITS.ordinal()] = new Slot[] {new Slot(EXPRESSIONS_INTEGER_DIGITS, 0, "header", OPERAND_HEADER, null), new Slot(EXPRESSIONS_INTEGER_DIGITS, 1, "value", EXPRESSION, null), new Slot(EXPRESSIONS_INTEGER_DIGITS, 2, "digits", INTEGER, null)};
        SLOTS[EXPRESSIONS_LITERAL.ordinal()] = new Slot[] {new Slot(EXPRESSIONS_LITERAL, 0, "header", OPERAND_HEADER, null), new Slot(EXPRESSIONS_LITERAL, 1, "value", VALUES_LITERAL_VALUE, null)};
        SLOTS[EXPRESSIONS_PARSE_INTEGER.ordinal()] = new Slot[] {new Slot(EXPRESSIONS_PARSE_INTEGER, 0, "header", OPERAND_HEADER, null), new Slot(EXPRESSIONS_PARSE_INTEGER, 1, "value", EXPRESSION, null), new Slot(EXPRESSIONS_PARSE_INTEGER, 2, "onInvalid", EXPRESSION, null)};
        SLOTS[EXPRESSIONS_QUANTIZE.ordinal()] = new Slot[] {new Slot(EXPRESSIONS_QUANTIZE, 0, "header", OPERAND_HEADER, null), new Slot(EXPRESSIONS_QUANTIZE, 1, "value", EXPRESSION, null), new Slot(EXPRESSIONS_QUANTIZE, 2, "scale", INTEGER, null), new Slot(EXPRESSIONS_QUANTIZE, 3, "rounding", EXPRESSIONS_ROUNDING, null)};
        SLOTS[EXPRESSIONS_READ.ordinal()] = new Slot[] {new Slot(EXPRESSIONS_READ, 0, "header", OPERAND_HEADER, null), new Slot(EXPRESSIONS_READ, 1, "place", PLACE, null)};
        ENUMS[EXPRESSIONS_ROUNDING.ordinal()] = new String[] {"TOWARD_ZERO", "HALF_EVEN"};
        SLOTS[EXPRESSIONS_SLICE_TEXT.ordinal()] = new Slot[] {new Slot(EXPRESSIONS_SLICE_TEXT, 0, "header", OPERAND_HEADER, null), new Slot(EXPRESSIONS_SLICE_TEXT, 1, "value", EXPRESSION, null), new Slot(EXPRESSIONS_SLICE_TEXT, 2, "start", EXPRESSION, null), new Slot(EXPRESSIONS_SLICE_TEXT, 3, "count", EXPRESSION, null)};
        SLOTS[EXPRESSIONS_TRIM_RIGHT.ordinal()] = new Slot[] {new Slot(EXPRESSIONS_TRIM_RIGHT, 0, "header", OPERAND_HEADER, null), new Slot(EXPRESSIONS_TRIM_RIGHT, 1, "value", EXPRESSION, null), new Slot(EXPRESSIONS_TRIM_RIGHT, 2, "characters", TEXT, null)};
        SLOTS[EXPRESSIONS_UNARY.ordinal()] = new Slot[] {new Slot(EXPRESSIONS_UNARY, 0, "header", OPERAND_HEADER, null), new Slot(EXPRESSIONS_UNARY, 1, "operator", EXPRESSIONS_UNARY_OPERATOR, null), new Slot(EXPRESSIONS_UNARY, 2, "argument", EXPRESSION, null)};
        ENUMS[EXPRESSIONS_UNARY_OPERATOR.ordinal()] = new String[] {"NOT", "NEG", "ABS", "TO_DECIMAL", "TO_INT", "IS_DIGITS", "LENGTH"};
        SLOTS[EXPRESSIONS_UNKNOWN.ordinal()] = new Slot[] {new Slot(EXPRESSIONS_UNKNOWN, 0, "header", OPERAND_HEADER, null), new Slot(EXPRESSIONS_UNKNOWN, 1, "typeRef", TYPES_TYPE_REF, null), new Slot(EXPRESSIONS_UNKNOWN, 2, "dependencies", LIST, EXPRESSION), new Slot(EXPRESSIONS_UNKNOWN, 3, "remainingReads", SCOPES_MEMORY_BOUND, null), new Slot(EXPRESSIONS_UNKNOWN, 4, "reason", IDS_UNCERTAINTY_ID, null)};
        SLOTS[EXPRESSIONS_WRAP_INTEGER.ordinal()] = new Slot[] {new Slot(EXPRESSIONS_WRAP_INTEGER, 0, "header", OPERAND_HEADER, null), new Slot(EXPRESSIONS_WRAP_INTEGER, 1, "value", EXPRESSION, null), new Slot(EXPRESSIONS_WRAP_INTEGER, 2, "width", INTEGER, null), new Slot(EXPRESSIONS_WRAP_INTEGER, 3, "signed", BOOLEAN, null)};
        SLOTS[IDS_ARTIFACT_ID.ordinal()] = new Slot[] {new Slot(IDS_ARTIFACT_ID, 0, "publication", IDS_PUBLICATION_ID, null), new Slot(IDS_ARTIFACT_ID, 1, "localId", TEXT, null)};
        SLOTS[IDS_ARTIFACT_RELATION_ID.ordinal()] = new Slot[] {new Slot(IDS_ARTIFACT_RELATION_ID, 0, "publication", IDS_PUBLICATION_ID, null), new Slot(IDS_ARTIFACT_RELATION_ID, 1, "localId", TEXT, null)};
        SLOTS[IDS_COMPLETION_PORT_ID.ordinal()] = new Slot[] {new Slot(IDS_COMPLETION_PORT_ID, 0, "unit", IDS_UNIT_ID, null), new Slot(IDS_COMPLETION_PORT_ID, 1, "localId", TEXT, null)};
        SLOTS[IDS_ENTRY_ID.ordinal()] = new Slot[] {new Slot(IDS_ENTRY_ID, 0, "unit", IDS_UNIT_ID, null), new Slot(IDS_ENTRY_ID, 1, "localId", TEXT, null)};
        SLOTS[IDS_ENTRY_OWNER.ordinal()] = new Slot[] {new Slot(IDS_ENTRY_OWNER, 0, "entry", IDS_ENTRY_ID, null)};
        SLOTS[IDS_LABEL_ID.ordinal()] = new Slot[] {new Slot(IDS_LABEL_ID, 0, "unit", IDS_UNIT_ID, null), new Slot(IDS_LABEL_ID, 1, "localId", TEXT, null)};
        SLOTS[IDS_OBJECT_ID.ordinal()] = new Slot[] {new Slot(IDS_OBJECT_ID, 0, "unit", IDS_UNIT_ID, null), new Slot(IDS_OBJECT_ID, 1, "localId", TEXT, null)};
        SLOTS[IDS_OPERAND_ID.ordinal()] = new Slot[] {new Slot(IDS_OPERAND_ID, 0, "owner", IDS_OPERAND_OWNER, null), new Slot(IDS_OPERAND_ID, 1, "localId", TEXT, null)};
        SLOTS[IDS_OPERATION_ID.ordinal()] = new Slot[] {new Slot(IDS_OPERATION_ID, 0, "unit", IDS_UNIT_ID, null), new Slot(IDS_OPERATION_ID, 1, "localId", TEXT, null)};
        SLOTS[IDS_OPERATION_OWNER.ordinal()] = new Slot[] {new Slot(IDS_OPERATION_OWNER, 0, "operation", IDS_OPERATION_ID, null)};
        SLOTS[IDS_ORIGIN_ID.ordinal()] = new Slot[] {new Slot(IDS_ORIGIN_ID, 0, "publication", IDS_PUBLICATION_ID, null), new Slot(IDS_ORIGIN_ID, 1, "localId", TEXT, null)};
        SLOTS[IDS_PREMISE_ID.ordinal()] = new Slot[] {new Slot(IDS_PREMISE_ID, 0, "publication", IDS_PUBLICATION_ID, null), new Slot(IDS_PREMISE_ID, 1, "localId", TEXT, null)};
        SLOTS[IDS_PUBLICATION_ID.ordinal()] = new Slot[] {new Slot(IDS_PUBLICATION_ID, 0, "localId", TEXT, null)};
        SLOTS[IDS_RESOURCE_ID.ordinal()] = new Slot[] {new Slot(IDS_RESOURCE_ID, 0, "publication", IDS_PUBLICATION_ID, null), new Slot(IDS_RESOURCE_ID, 1, "localId", TEXT, null)};
        SLOTS[IDS_STORAGE_ID.ordinal()] = new Slot[] {new Slot(IDS_STORAGE_ID, 0, "publication", IDS_PUBLICATION_ID, null), new Slot(IDS_STORAGE_ID, 1, "localId", TEXT, null)};
        SLOTS[IDS_UNCERTAINTY_ID.ordinal()] = new Slot[] {new Slot(IDS_UNCERTAINTY_ID, 0, "publication", IDS_PUBLICATION_ID, null), new Slot(IDS_UNCERTAINTY_ID, 1, "localId", TEXT, null)};
        SLOTS[IDS_UNIT_ID.ordinal()] = new Slot[] {new Slot(IDS_UNIT_ID, 0, "publication", IDS_PUBLICATION_ID, null), new Slot(IDS_UNIT_ID, 1, "localId", TEXT, null)};
        SLOTS[INTERACTIONS_COMPUTED_RESOURCE.ordinal()] = new Slot[] {new Slot(INTERACTIONS_COMPUTED_RESOURCE, 0, "category", TEXT, null), new Slot(INTERACTIONS_COMPUTED_RESOURCE, 1, "namespace", TEXT, null), new Slot(INTERACTIONS_COMPUTED_RESOURCE, 2, "name", IDS_OPERAND_ID, null), new Slot(INTERACTIONS_COMPUTED_RESOURCE, 3, "namePolicy", INTERACTIONS_NAME_POLICY, null), new Slot(INTERACTIONS_COMPUTED_RESOURCE, 4, "origin", IDS_ORIGIN_ID, null)};
        SLOTS[INTERACTIONS_COMPUTED_TARGET.ordinal()] = new Slot[] {new Slot(INTERACTIONS_COMPUTED_TARGET, 0, "category", TEXT, null), new Slot(INTERACTIONS_COMPUTED_TARGET, 1, "namespace", TEXT, null), new Slot(INTERACTIONS_COMPUTED_TARGET, 2, "name", EXPRESSION, null), new Slot(INTERACTIONS_COMPUTED_TARGET, 3, "namePolicy", INTERACTIONS_NAME_POLICY, null), new Slot(INTERACTIONS_COMPUTED_TARGET, 4, "origin", IDS_ORIGIN_ID, null)};
        SLOTS[INTERACTIONS_CONTRACT_REF.ordinal()] = new Slot[] {new Slot(INTERACTIONS_CONTRACT_REF, 0, "authority", TEXT, null), new Slot(INTERACTIONS_CONTRACT_REF, 1, "version", TEXT, null), new Slot(INTERACTIONS_CONTRACT_REF, 2, "evidence", LIST, IDS_ORIGIN_ID)};
        SLOTS[INTERACTIONS_COPY_ARGUMENT.ordinal()] = new Slot[] {new Slot(INTERACTIONS_COPY_ARGUMENT, 0, "value", EXPRESSION, null)};
        SLOTS[INTERACTIONS_EFFECT_BOUND.ordinal()] = new Slot[] {new Slot(INTERACTIONS_EFFECT_BOUND, 0, "otherwise", INTERACTIONS_FOREIGN_EFFECTS, null), new Slot(INTERACTIONS_EFFECT_BOUND, 1, "perOutcome", LIST, INTERACTIONS_OUTCOME_EFFECTS)};
        SLOTS[INTERACTIONS_ENTRY_SIGNATURE.ordinal()] = new Slot[] {new Slot(INTERACTIONS_ENTRY_SIGNATURE, 0, "entry", IDS_ENTRY_ID, null)};
        ENUMS[INTERACTIONS_EXACT_NAME.ordinal()] = new String[] {"INSTANCE"};
        SLOTS[INTERACTIONS_EXTENSION_NAME.ordinal()] = new Slot[] {new Slot(INTERACTIONS_EXTENSION_NAME, 0, "name", TEXT, null), new Slot(INTERACTIONS_EXTENSION_NAME, 1, "version", TEXT, null)};
        ENUMS[INTERACTIONS_EXTERNAL_BINDING.ordinal()] = new String[] {"INSTANCE"};
        SLOTS[INTERACTIONS_EXTERNAL_SIGNATURE.ordinal()] = new Slot[] {new Slot(INTERACTIONS_EXTERNAL_SIGNATURE, 0, "signature", INTERACTIONS_SIGNATURE, null)};
        SLOTS[INTERACTIONS_FOREIGN_EFFECTS.ordinal()] = new Slot[] {new Slot(INTERACTIONS_FOREIGN_EFFECTS, 0, "reads", SCOPES_MEMORY_BOUND, null), new Slot(INTERACTIONS_FOREIGN_EFFECTS, 1, "writes", SCOPES_MEMORY_BOUND, null), new Slot(INTERACTIONS_FOREIGN_EFFECTS, 2, "mustOverwrite", LIST, IDS_OPERAND_ID)};
        SLOTS[INTERACTIONS_INTERNAL_TARGET.ordinal()] = new Slot[] {new Slot(INTERACTIONS_INTERNAL_TARGET, 0, "entry", IDS_ENTRY_ID, null)};
        SLOTS[INTERACTIONS_KNOWN_CONTRACT.ordinal()] = new Slot[] {new Slot(INTERACTIONS_KNOWN_CONTRACT, 0, "reference", INTERACTIONS_CONTRACT_REF, null)};
        SLOTS[INTERACTIONS_KNOWN_MODE.ordinal()] = new Slot[] {new Slot(INTERACTIONS_KNOWN_MODE, 0, "mode", INTERACTIONS_PASSING_MODE, null)};
        SLOTS[INTERACTIONS_LITERAL_TARGET.ordinal()] = new Slot[] {new Slot(INTERACTIONS_LITERAL_TARGET, 0, "category", TEXT, null), new Slot(INTERACTIONS_LITERAL_TARGET, 1, "namespace", TEXT, null), new Slot(INTERACTIONS_LITERAL_TARGET, 2, "name", TEXT, null), new Slot(INTERACTIONS_LITERAL_TARGET, 3, "namePolicy", INTERACTIONS_NAME_POLICY, null), new Slot(INTERACTIONS_LITERAL_TARGET, 4, "origin", IDS_ORIGIN_ID, null)};
        SLOTS[INTERACTIONS_LOCAL_RESOURCE.ordinal()] = new Slot[] {new Slot(INTERACTIONS_LOCAL_RESOURCE, 0, "category", TEXT, null)};
        ENUMS[INTERACTIONS_NO_REMAINDER.ordinal()] = new String[] {"INSTANCE"};
        SLOTS[INTERACTIONS_OBJECT_BINDING.ordinal()] = new Slot[] {new Slot(INTERACTIONS_OBJECT_BINDING, 0, "object", IDS_OBJECT_ID, null)};
        SLOTS[INTERACTIONS_OUTCOME_EFFECTS.ordinal()] = new Slot[] {new Slot(INTERACTIONS_OUTCOME_EFFECTS, 0, "outcome", CONTROL_OUTCOME_KEY, null), new Slot(INTERACTIONS_OUTCOME_EFFECTS, 1, "effects", INTERACTIONS_FOREIGN_EFFECTS, null)};
        SLOTS[INTERACTIONS_PARAMETER.ordinal()] = new Slot[] {new Slot(INTERACTIONS_PARAMETER, 0, "position", INTEGER, null), new Slot(INTERACTIONS_PARAMETER, 1, "mode", INTERACTIONS_MODE_KNOWLEDGE, null), new Slot(INTERACTIONS_PARAMETER, 2, "typeRef", TYPES_TYPE_REF, null), new Slot(INTERACTIONS_PARAMETER, 3, "objectBinding", INTERACTIONS_PARAMETER_BINDING, null), new Slot(INTERACTIONS_PARAMETER, 4, "origin", IDS_ORIGIN_ID, null)};
        SLOTS[INTERACTIONS_PARAMETER_INVENTORY.ordinal()] = new Slot[] {new Slot(INTERACTIONS_PARAMETER_INVENTORY, 0, "known", LIST, INTERACTIONS_PARAMETER), new Slot(INTERACTIONS_PARAMETER_INVENTORY, 1, "remainder", INTERACTIONS_UNKNOWN_BOUND, null)};
        ENUMS[INTERACTIONS_PASSING_MODE.ordinal()] = new String[] {"VALUE", "REFERENCE", "COPY"};
        SLOTS[INTERACTIONS_REFERENCE_ARGUMENT.ordinal()] = new Slot[] {new Slot(INTERACTIONS_REFERENCE_ARGUMENT, 0, "place", PLACE, null)};
        SLOTS[INTERACTIONS_RESOURCE.ordinal()] = new Slot[] {new Slot(INTERACTIONS_RESOURCE, 0, "id", IDS_RESOURCE_ID, null), new Slot(INTERACTIONS_RESOURCE, 1, "description", INTERACTIONS_RESOURCE_DESCRIPTION, null), new Slot(INTERACTIONS_RESOURCE, 2, "origin", IDS_ORIGIN_ID, null), new Slot(INTERACTIONS_RESOURCE, 3, "declaration", OPTIONAL, INTERACTIONS_RESOURCE_DECLARATION)};
        SLOTS[INTERACTIONS_RESOURCE_DECLARATION.ordinal()] = new Slot[] {new Slot(INTERACTIONS_RESOURCE_DECLARATION, 0, "owner", IDS_UNIT_ID, null), new Slot(INTERACTIONS_RESOURCE_DECLARATION, 1, "name", TEXT, null), new Slot(INTERACTIONS_RESOURCE_DECLARATION, 2, "classification", TEXT, null), new Slot(INTERACTIONS_RESOURCE_DECLARATION, 3, "nameSource", TEXT, null), new Slot(INTERACTIONS_RESOURCE_DECLARATION, 4, "objects", LIST, INTERACTIONS_RESOURCE_OBJECT), new Slot(INTERACTIONS_RESOURCE_DECLARATION, 5, "uses", LIST, INTERACTIONS_RESOURCE_USE)};
        SLOTS[INTERACTIONS_RESOURCE_OBJECT.ordinal()] = new Slot[] {new Slot(INTERACTIONS_RESOURCE_OBJECT, 0, "object", IDS_OBJECT_ID, null), new Slot(INTERACTIONS_RESOURCE_OBJECT, 1, "role", TEXT, null)};
        SLOTS[INTERACTIONS_RESOURCE_USE.ordinal()] = new Slot[] {new Slot(INTERACTIONS_RESOURCE_USE, 0, "operation", IDS_OPERATION_ID, null), new Slot(INTERACTIONS_RESOURCE_USE, 1, "role", TEXT, null), new Slot(INTERACTIONS_RESOURCE_USE, 2, "origin", IDS_ORIGIN_ID, null)};
        SLOTS[INTERACTIONS_RESULT_INVENTORY.ordinal()] = new Slot[] {new Slot(INTERACTIONS_RESULT_INVENTORY, 0, "known", LIST, INTERACTIONS_RESULT_SLOT), new Slot(INTERACTIONS_RESULT_INVENTORY, 1, "remainder", INTERACTIONS_UNKNOWN_BOUND, null)};
        SLOTS[INTERACTIONS_RESULT_SLOT.ordinal()] = new Slot[] {new Slot(INTERACTIONS_RESULT_SLOT, 0, "position", INTEGER, null), new Slot(INTERACTIONS_RESULT_SLOT, 1, "typeRef", TYPES_TYPE_REF, null), new Slot(INTERACTIONS_RESULT_SLOT, 2, "origin", IDS_ORIGIN_ID, null)};
        SLOTS[INTERACTIONS_SIGNATURE.ordinal()] = new Slot[] {new Slot(INTERACTIONS_SIGNATURE, 0, "parameters", INTERACTIONS_PARAMETER_INVENTORY, null), new Slot(INTERACTIONS_SIGNATURE, 1, "results", INTERACTIONS_RESULT_INVENTORY, null), new Slot(INTERACTIONS_SIGNATURE, 2, "origin", IDS_ORIGIN_ID, null)};
        SLOTS[INTERACTIONS_UNKNOWN_CONTRACT.ordinal()] = new Slot[] {new Slot(INTERACTIONS_UNKNOWN_CONTRACT, 0, "uncertainty", IDS_UNCERTAINTY_ID, null)};
        SLOTS[INTERACTIONS_UNKNOWN_MODE.ordinal()] = new Slot[] {new Slot(INTERACTIONS_UNKNOWN_MODE, 0, "uncertainty", IDS_UNCERTAINTY_ID, null)};
        SLOTS[INTERACTIONS_UNKNOWN_NAME.ordinal()] = new Slot[] {new Slot(INTERACTIONS_UNKNOWN_NAME, 0, "uncertainty", IDS_UNCERTAINTY_ID, null)};
        SLOTS[INTERACTIONS_UNKNOWN_PARAMETER_BINDING.ordinal()] = new Slot[] {new Slot(INTERACTIONS_UNKNOWN_PARAMETER_BINDING, 0, "uncertainty", IDS_UNCERTAINTY_ID, null)};
        SLOTS[INTERACTIONS_UNKNOWN_REMAINDER.ordinal()] = new Slot[] {new Slot(INTERACTIONS_UNKNOWN_REMAINDER, 0, "uncertainty", IDS_UNCERTAINTY_ID, null)};
        SLOTS[INTERACTIONS_UNKNOWN_RESOURCE.ordinal()] = new Slot[] {new Slot(INTERACTIONS_UNKNOWN_RESOURCE, 0, "category", TEXT, null), new Slot(INTERACTIONS_UNKNOWN_RESOURCE, 1, "namespace", TEXT, null), new Slot(INTERACTIONS_UNKNOWN_RESOURCE, 2, "uncertainty", IDS_UNCERTAINTY_ID, null)};
        SLOTS[INTERACTIONS_VALUE_ARGUMENT.ordinal()] = new Slot[] {new Slot(INTERACTIONS_VALUE_ARGUMENT, 0, "value", EXPRESSION, null)};
        SLOTS[MEMORY_ALIAS_BINDING.ordinal()] = new Slot[] {new Slot(MEMORY_ALIAS_BINDING, 0, "object", IDS_OBJECT_ID, null)};
        SLOTS[MEMORY_ALTERNATIVES_BINDING.ordinal()] = new Slot[] {new Slot(MEMORY_ALTERNATIVES_BINDING, 0, "alternatives", LIST, MEMORY_BINDING), new Slot(MEMORY_ALTERNATIVES_BINDING, 1, "remainder", SCOPES_MEMORY_BOUND, null)};
        ENUMS[MEMORY_ASCII_TEXT.ordinal()] = new String[] {"INSTANCE"};
        SLOTS[MEMORY_BINARY_CODEC.ordinal()] = new Slot[] {new Slot(MEMORY_BINARY_CODEC, 0, "signed", BOOLEAN, null), new Slot(MEMORY_BINARY_CODEC, 1, "width", INTEGER, null), new Slot(MEMORY_BINARY_CODEC, 2, "order", MEMORY_BYTE_ORDER, null)};
        ENUMS[MEMORY_BYTE_ORDER.ordinal()] = new String[] {"LITTLE", "BIG"};
        SLOTS[MEMORY_BYTE_RANGE.ordinal()] = new Slot[] {new Slot(MEMORY_BYTE_RANGE, 0, "region", IDS_STORAGE_ID, null), new Slot(MEMORY_BYTE_RANGE, 1, "offset", EXPRESSION, null), new Slot(MEMORY_BYTE_RANGE, 2, "extent", EXPRESSION, null)};
        SLOTS[MEMORY_CELL.ordinal()] = new Slot[] {new Slot(MEMORY_CELL, 0, "header", MEMORY_STORAGE_HEADER, null), new Slot(MEMORY_CELL, 1, "typeRef", TYPES_TYPE_REF, null)};
        SLOTS[MEMORY_CELL_BINDING.ordinal()] = new Slot[] {new Slot(MEMORY_CELL_BINDING, 0, "storage", IDS_STORAGE_ID, null)};
        SLOTS[MEMORY_EXTENSION_CODEC.ordinal()] = new Slot[] {new Slot(MEMORY_EXTENSION_CODEC, 0, "name", TEXT, null), new Slot(MEMORY_EXTENSION_CODEC, 1, "version", TEXT, null), new Slot(MEMORY_EXTENSION_CODEC, 2, "logicalType", TYPES_TYPE_REF, null)};
        ENUMS[MEMORY_IDENTITY_BYTES.ordinal()] = new String[] {"INSTANCE"};
        ENUMS[MEMORY_LIFETIME.ordinal()] = new String[] {"ACTIVATION", "PERSISTENT", "EXTERNAL"};
        SLOTS[MEMORY_OBJECT_DECLARATION.ordinal()] = new Slot[] {new Slot(MEMORY_OBJECT_DECLARATION, 0, "id", IDS_OBJECT_ID, null), new Slot(MEMORY_OBJECT_DECLARATION, 1, "displayName", OPTIONAL, TEXT), new Slot(MEMORY_OBJECT_DECLARATION, 2, "typeRef", TYPES_TYPE_REF, null), new Slot(MEMORY_OBJECT_DECLARATION, 3, "storage", MEMORY_BINDING, null), new Slot(MEMORY_OBJECT_DECLARATION, 4, "visibility", MEMORY_VISIBILITY, null), new Slot(MEMORY_OBJECT_DECLARATION, 5, "origin", IDS_ORIGIN_ID, null), new Slot(MEMORY_OBJECT_DECLARATION, 6, "coverage", EVIDENCE_COVERAGE_STATUS, null), new Slot(MEMORY_OBJECT_DECLARATION, 7, "precision", EVIDENCE_PRECISION, null)};
        SLOTS[MEMORY_REGION.ordinal()] = new Slot[] {new Slot(MEMORY_REGION, 0, "header", MEMORY_STORAGE_HEADER, null), new Slot(MEMORY_REGION, 1, "extent", OPTIONAL, INTEGER), new Slot(MEMORY_REGION, 2, "extentUnknown", OPTIONAL, IDS_UNCERTAINTY_ID)};
        SLOTS[MEMORY_STORAGE_HEADER.ordinal()] = new Slot[] {new Slot(MEMORY_STORAGE_HEADER, 0, "id", IDS_STORAGE_ID, null), new Slot(MEMORY_STORAGE_HEADER, 1, "owner", OPTIONAL, IDS_UNIT_ID), new Slot(MEMORY_STORAGE_HEADER, 2, "lifetime", MEMORY_LIFETIME, null), new Slot(MEMORY_STORAGE_HEADER, 3, "visibility", MEMORY_VISIBILITY, null), new Slot(MEMORY_STORAGE_HEADER, 4, "origin", IDS_ORIGIN_ID, null)};
        SLOTS[MEMORY_UNKNOWN_BINDING.ordinal()] = new Slot[] {new Slot(MEMORY_UNKNOWN_BINDING, 0, "scope", SCOPES_MEMORY_SCOPE, null), new Slot(MEMORY_UNKNOWN_BINDING, 1, "reason", IDS_UNCERTAINTY_ID, null)};
        SLOTS[MEMORY_UNKNOWN_CODEC.ordinal()] = new Slot[] {new Slot(MEMORY_UNKNOWN_CODEC, 0, "logicalType", TYPES_TYPE_REF, null), new Slot(MEMORY_UNKNOWN_CODEC, 1, "reason", IDS_UNCERTAINTY_ID, null)};
        SLOTS[MEMORY_VIEW_BINDING.ordinal()] = new Slot[] {new Slot(MEMORY_VIEW_BINDING, 0, "region", IDS_STORAGE_ID, null), new Slot(MEMORY_VIEW_BINDING, 1, "offset", INTEGER, null), new Slot(MEMORY_VIEW_BINDING, 2, "extent", INTEGER, null), new Slot(MEMORY_VIEW_BINDING, 3, "codec", MEMORY_CODEC, null)};
        ENUMS[MEMORY_VISIBILITY.ordinal()] = new String[] {"PRIVATE", "SHARED", "UNKNOWN"};
        SLOTS[OPERAND_HEADER.ordinal()] = new Slot[] {new Slot(OPERAND_HEADER, 0, "id", IDS_OPERAND_ID, null), new Slot(OPERAND_HEADER, 1, "role", OPERAND_ROLE, null), new Slot(OPERAND_HEADER, 2, "origin", IDS_ORIGIN_ID, null)};
        ENUMS[OPERAND_ROLE.ordinal()] = new String[] {"VALUE_READ", "VALUE_WRITE", "ADDRESS_READ", "PREDICATE", "CALL_TARGET", "ARGUMENT_VALUE", "ARGUMENT_REFERENCE", "RESULT_TARGET", "RESOURCE_TARGET", "CONTROL_TARGET"};
        SLOTS[OPERATIONS_ASSIGN.ordinal()] = new Slot[] {new Slot(OPERATIONS_ASSIGN, 0, "header", OPERATIONS_HEADER, null), new Slot(OPERATIONS_ASSIGN, 1, "destination", PLACE, null), new Slot(OPERATIONS_ASSIGN, 2, "value", EXPRESSION, null)};
        SLOTS[OPERATIONS_BRANCH.ordinal()] = new Slot[] {new Slot(OPERATIONS_BRANCH, 0, "header", OPERATIONS_HEADER, null), new Slot(OPERATIONS_BRANCH, 1, "predicate", EXPRESSION, null), new Slot(OPERATIONS_BRANCH, 2, "trueDestination", IDS_LABEL_ID, null), new Slot(OPERATIONS_BRANCH, 3, "falseDestination", IDS_LABEL_ID, null)};
        SLOTS[OPERATIONS_CASE.ordinal()] = new Slot[] {new Slot(OPERATIONS_CASE, 0, "value", VALUES_LITERAL_VALUE, null), new Slot(OPERATIONS_CASE, 1, "destination", IDS_LABEL_ID, null)};
        SLOTS[OPERATIONS_COPY_BYTES.ordinal()] = new Slot[] {new Slot(OPERATIONS_COPY_BYTES, 0, "header", OPERATIONS_HEADER, null), new Slot(OPERATIONS_COPY_BYTES, 1, "destination", MEMORY_BYTE_RANGE, null), new Slot(OPERATIONS_COPY_BYTES, 2, "source", MEMORY_BYTE_RANGE, null), new Slot(OPERATIONS_COPY_BYTES, 3, "length", INTEGER, null), new Slot(OPERATIONS_COPY_BYTES, 4, "fallback", ENVELOPES_ENVELOPE, null)};
        SLOTS[OPERATIONS_DISPATCH.ordinal()] = new Slot[] {new Slot(OPERATIONS_DISPATCH, 0, "header", OPERATIONS_HEADER, null), new Slot(OPERATIONS_DISPATCH, 1, "selector", EXPRESSION, null), new Slot(OPERATIONS_DISPATCH, 2, "cases", LIST, OPERATIONS_CASE), new Slot(OPERATIONS_DISPATCH, 3, "defaultDestination", IDS_LABEL_ID, null)};
        SLOTS[OPERATIONS_HALT.ordinal()] = new Slot[] {new Slot(OPERATIONS_HALT, 0, "header", OPERATIONS_HEADER, null), new Slot(OPERATIONS_HALT, 1, "haltKind", OPERATIONS_HALT_KIND, null)};
        ENUMS[OPERATIONS_HALT_KIND.ordinal()] = new String[] {"NORMAL", "ABNORMAL"};
        SLOTS[OPERATIONS_HAVOC_MAY.ordinal()] = new Slot[] {new Slot(OPERATIONS_HAVOC_MAY, 0, "header", OPERATIONS_HEADER, null), new Slot(OPERATIONS_HAVOC_MAY, 1, "scope", SCOPES_MEMORY_SCOPE, null), new Slot(OPERATIONS_HAVOC_MAY, 2, "reason", IDS_UNCERTAINTY_ID, null)};
        SLOTS[OPERATIONS_HAVOC_MUST.ordinal()] = new Slot[] {new Slot(OPERATIONS_HAVOC_MUST, 0, "header", OPERATIONS_HEADER, null), new Slot(OPERATIONS_HAVOC_MUST, 1, "destination", PLACE, null), new Slot(OPERATIONS_HAVOC_MUST, 2, "reason", IDS_UNCERTAINTY_ID, null)};
        SLOTS[OPERATIONS_HEADER.ordinal()] = new Slot[] {new Slot(OPERATIONS_HEADER, 0, "id", IDS_OPERATION_ID, null), new Slot(OPERATIONS_HEADER, 1, "origin", IDS_ORIGIN_ID, null), new Slot(OPERATIONS_HEADER, 2, "coverage", EVIDENCE_COVERAGE_STATUS, null), new Slot(OPERATIONS_HEADER, 3, "precision", EVIDENCE_PRECISION, null), new Slot(OPERATIONS_HEADER, 4, "uncertainties", LIST, IDS_UNCERTAINTY_ID)};
        SLOTS[OPERATIONS_INDIRECT_JUMP.ordinal()] = new Slot[] {new Slot(OPERATIONS_INDIRECT_JUMP, 0, "header", OPERATIONS_HEADER, null), new Slot(OPERATIONS_INDIRECT_JUMP, 1, "target", EXPRESSION, null), new Slot(OPERATIONS_INDIRECT_JUMP, 2, "within", TYPES_LABEL_TYPE, null), new Slot(OPERATIONS_INDIRECT_JUMP, 3, "fallback", ENVELOPES_ENVELOPE, null)};
        SLOTS[OPERATIONS_INVOKE.ordinal()] = new Slot[] {new Slot(OPERATIONS_INVOKE, 0, "header", OPERATIONS_HEADER, null), new Slot(OPERATIONS_INVOKE, 1, "action", TEXT, null), new Slot(OPERATIONS_INVOKE, 2, "target", INTERACTIONS_TARGET, null), new Slot(OPERATIONS_INVOKE, 3, "arguments", LIST, INTERACTIONS_ARGUMENT), new Slot(OPERATIONS_INVOKE, 4, "results", LIST, PLACE), new Slot(OPERATIONS_INVOKE, 5, "signature", INTERACTIONS_INVOCATION_SIGNATURE, null), new Slot(OPERATIONS_INVOKE, 6, "effectOperands", LIST, PLACE), new Slot(OPERATIONS_INVOKE, 7, "effectBound", INTERACTIONS_EFFECT_BOUND, null), new Slot(OPERATIONS_INVOKE, 8, "outcomes", CONTROL_INVOCATION_OUTCOMES, null), new Slot(OPERATIONS_INVOKE, 9, "contract", INTERACTIONS_CONTRACT_KNOWLEDGE, null)};
        SLOTS[OPERATIONS_JUMP.ordinal()] = new Slot[] {new Slot(OPERATIONS_JUMP, 0, "header", OPERATIONS_HEADER, null), new Slot(OPERATIONS_JUMP, 1, "destination", IDS_LABEL_ID, null)};
        SLOTS[OPERATIONS_LOCAL_BOUNDARY.ordinal()] = new Slot[] {new Slot(OPERATIONS_LOCAL_BOUNDARY, 0, "header", OPERATIONS_HEADER, null), new Slot(OPERATIONS_LOCAL_BOUNDARY, 1, "port", IDS_COMPLETION_PORT_ID, null), new Slot(OPERATIONS_LOCAL_BOUNDARY, 2, "defaultDestination", IDS_LABEL_ID, null), new Slot(OPERATIONS_LOCAL_BOUNDARY, 3, "fallback", ENVELOPES_ENVELOPE, null), new Slot(OPERATIONS_LOCAL_BOUNDARY, 4, "resumeKey", OPTIONAL, TEXT)};
        SLOTS[OPERATIONS_LOCAL_INVOKE.ordinal()] = new Slot[] {new Slot(OPERATIONS_LOCAL_INVOKE, 0, "header", OPERATIONS_HEADER, null), new Slot(OPERATIONS_LOCAL_INVOKE, 1, "entry", IDS_LABEL_ID, null), new Slot(OPERATIONS_LOCAL_INVOKE, 2, "completionPorts", LIST, IDS_COMPLETION_PORT_ID), new Slot(OPERATIONS_LOCAL_INVOKE, 3, "resume", IDS_LABEL_ID, null), new Slot(OPERATIONS_LOCAL_INVOKE, 4, "fallback", ENVELOPES_ENVELOPE, null), new Slot(OPERATIONS_LOCAL_INVOKE, 5, "reentryGuard", OPTIONAL, OPERATIONS_REENTRY_GUARD), new Slot(OPERATIONS_LOCAL_INVOKE, 6, "resumeRoutes", LIST, OPERATIONS_RESUME_ROUTE)};
        SLOTS[OPERATIONS_LOCAL_RESUME.ordinal()] = new Slot[] {new Slot(OPERATIONS_LOCAL_RESUME, 0, "header", OPERATIONS_HEADER, null), new Slot(OPERATIONS_LOCAL_RESUME, 1, "fallback", ENVELOPES_ENVELOPE, null), new Slot(OPERATIONS_LOCAL_RESUME, 2, "resumeKey", OPTIONAL, TEXT)};
        SLOTS[OPERATIONS_LOCAL_UNWIND.ordinal()] = new Slot[] {new Slot(OPERATIONS_LOCAL_UNWIND, 0, "header", OPERATIONS_HEADER, null), new Slot(OPERATIONS_LOCAL_UNWIND, 1, "count", INTEGER, null), new Slot(OPERATIONS_LOCAL_UNWIND, 2, "destination", IDS_LABEL_ID, null), new Slot(OPERATIONS_LOCAL_UNWIND, 3, "fallback", ENVELOPES_ENVELOPE, null), new Slot(OPERATIONS_LOCAL_UNWIND, 4, "all", BOOLEAN, null)};
        SLOTS[OPERATIONS_NOP.ordinal()] = new Slot[] {new Slot(OPERATIONS_NOP, 0, "header", OPERATIONS_HEADER, null)};
        SLOTS[OPERATIONS_OPAQUE.ordinal()] = new Slot[] {new Slot(OPERATIONS_OPAQUE, 0, "header", OPERATIONS_HEADER, null), new Slot(OPERATIONS_OPAQUE, 1, "observedKind", TEXT, null), new Slot(OPERATIONS_OPAQUE, 2, "knownOperands", LIST, OPERAND), new Slot(OPERATIONS_OPAQUE, 3, "valueResults", LIST, IDS_OPERAND_ID), new Slot(OPERATIONS_OPAQUE, 4, "envelope", ENVELOPES_ENVELOPE, null)};
        SLOTS[OPERATIONS_RAISE.ordinal()] = new Slot[] {new Slot(OPERATIONS_RAISE, 0, "header", OPERATIONS_HEADER, null), new Slot(OPERATIONS_RAISE, 1, "tag", TEXT, null), new Slot(OPERATIONS_RAISE, 2, "values", LIST, EXPRESSION)};
        SLOTS[OPERATIONS_REENTRY_GUARD.ordinal()] = new Slot[] {new Slot(OPERATIONS_REENTRY_GUARD, 0, "activationKey", TEXT, null), new Slot(OPERATIONS_REENTRY_GUARD, 1, "destination", IDS_LABEL_ID, null)};
        SLOTS[OPERATIONS_RESUME_ROUTE.ordinal()] = new Slot[] {new Slot(OPERATIONS_RESUME_ROUTE, 0, "key", TEXT, null), new Slot(OPERATIONS_RESUME_ROUTE, 1, "destination", IDS_LABEL_ID, null)};
        SLOTS[OPERATIONS_RETURN.ordinal()] = new Slot[] {new Slot(OPERATIONS_RETURN, 0, "header", OPERATIONS_HEADER, null), new Slot(OPERATIONS_RETURN, 1, "values", LIST, EXPRESSION)};
        SLOTS[ORIGINS_ARTIFACT.ordinal()] = new Slot[] {new Slot(ORIGINS_ARTIFACT, 0, "id", IDS_ARTIFACT_ID, null), new Slot(ORIGINS_ARTIFACT, 1, "logicalName", TEXT, null), new Slot(ORIGINS_ARTIFACT, 2, "contentDigest", OPTIONAL, TEXT)};
        ENUMS[ORIGINS_COLUMN_UNIT.ordinal()] = new String[] {"UNICODE_SCALAR", "UTF16_CODE_UNIT", "OCTET"};
        SLOTS[ORIGINS_CONTRACTUAL.ordinal()] = new Slot[] {new Slot(ORIGINS_CONTRACTUAL, 0, "id", IDS_ORIGIN_ID, null), new Slot(ORIGINS_CONTRACTUAL, 1, "authority", TEXT, null), new Slot(ORIGINS_CONTRACTUAL, 2, "version", TEXT, null)};
        SLOTS[ORIGINS_DERIVED.ordinal()] = new Slot[] {new Slot(ORIGINS_DERIVED, 0, "id", IDS_ORIGIN_ID, null), new Slot(ORIGINS_DERIVED, 1, "inputs", LIST, IDS_ORIGIN_ID), new Slot(ORIGINS_DERIVED, 2, "rule", TEXT, null)};
        SLOTS[ORIGINS_INCLUDE_FRAME.ordinal()] = new Slot[] {new Slot(ORIGINS_INCLUDE_FRAME, 0, "including", IDS_ARTIFACT_ID, null), new Slot(ORIGINS_INCLUDE_FRAME, 1, "included", IDS_ARTIFACT_ID, null), new Slot(ORIGINS_INCLUDE_FRAME, 2, "requestedName", TEXT, null), new Slot(ORIGINS_INCLUDE_FRAME, 3, "site", OPTIONAL, ORIGINS_LOCATION)};
        SLOTS[ORIGINS_LINE_COLUMNS.ordinal()] = new Slot[] {new Slot(ORIGINS_LINE_COLUMNS, 0, "span", ORIGINS_SPAN, null)};
        SLOTS[ORIGINS_OFFSETS.ordinal()] = new Slot[] {new Slot(ORIGINS_OFFSETS, 0, "start", INTEGER, null), new Slot(ORIGINS_OFFSETS, 1, "end", INTEGER, null), new Slot(ORIGINS_OFFSETS, 2, "unit", TEXT, null), new Slot(ORIGINS_OFFSETS, 3, "endExclusive", BOOLEAN, null)};
        SLOTS[ORIGINS_POSITION.ordinal()] = new Slot[] {new Slot(ORIGINS_POSITION, 0, "line", INTEGER, null), new Slot(ORIGINS_POSITION, 1, "column", INTEGER, null)};
        SLOTS[ORIGINS_SPAN.ordinal()] = new Slot[] {new Slot(ORIGINS_SPAN, 0, "start", ORIGINS_POSITION, null), new Slot(ORIGINS_SPAN, 1, "end", ORIGINS_POSITION, null), new Slot(ORIGINS_SPAN, 2, "lineBase", INTEGER, null), new Slot(ORIGINS_SPAN, 3, "columnBase", INTEGER, null), new Slot(ORIGINS_SPAN, 4, "columnUnit", ORIGINS_COLUMN_UNIT, null), new Slot(ORIGINS_SPAN, 5, "endExclusive", BOOLEAN, null)};
        SLOTS[ORIGINS_UNAVAILABLE.ordinal()] = new Slot[] {new Slot(ORIGINS_UNAVAILABLE, 0, "id", IDS_ORIGIN_ID, null), new Slot(ORIGINS_UNAVAILABLE, 1, "reason", TEXT, null)};
        SLOTS[ORIGINS_WRITTEN.ordinal()] = new Slot[] {new Slot(ORIGINS_WRITTEN, 0, "id", IDS_ORIGIN_ID, null), new Slot(ORIGINS_WRITTEN, 1, "artifact", IDS_ARTIFACT_ID, null), new Slot(ORIGINS_WRITTEN, 2, "location", OPTIONAL, ORIGINS_LOCATION), new Slot(ORIGINS_WRITTEN, 3, "includes", LIST, ORIGINS_INCLUDE_FRAME), new Slot(ORIGINS_WRITTEN, 4, "exact", BOOLEAN, null)};
        SLOTS[PLACES_CHOICE.ordinal()] = new Slot[] {new Slot(PLACES_CHOICE, 0, "header", OPERAND_HEADER, null), new Slot(PLACES_CHOICE, 1, "candidates", LIST, PLACE), new Slot(PLACES_CHOICE, 2, "remainder", SCOPES_MEMORY_BOUND, null), new Slot(PLACES_CHOICE, 3, "typeRef", TYPES_TYPE_REF, null)};
        SLOTS[PLACES_OBJECT_PLACE.ordinal()] = new Slot[] {new Slot(PLACES_OBJECT_PLACE, 0, "header", OPERAND_HEADER, null), new Slot(PLACES_OBJECT_PLACE, 1, "object", IDS_OBJECT_ID, null)};
        SLOTS[PLACES_REGION_SLICE.ordinal()] = new Slot[] {new Slot(PLACES_REGION_SLICE, 0, "header", OPERAND_HEADER, null), new Slot(PLACES_REGION_SLICE, 1, "region", IDS_STORAGE_ID, null), new Slot(PLACES_REGION_SLICE, 2, "offset", EXPRESSION, null), new Slot(PLACES_REGION_SLICE, 3, "length", EXPRESSION, null), new Slot(PLACES_REGION_SLICE, 4, "codec", MEMORY_CODEC, null), new Slot(PLACES_REGION_SLICE, 5, "typeRef", TYPES_TYPE_REF, null)};
        SLOTS[PROOFS_CALL_PARAMETER_DOMAIN.ordinal()] = new Slot[] {new Slot(PROOFS_CALL_PARAMETER_DOMAIN, 0, "invocation", IDS_OPERATION_ID, null), new Slot(PROOFS_CALL_PARAMETER_DOMAIN, 1, "entry", IDS_ENTRY_ID, null), new Slot(PROOFS_CALL_PARAMETER_DOMAIN, 2, "position", INTEGER, null)};
        SLOTS[PROOFS_CALL_RESULT_DOMAIN.ordinal()] = new Slot[] {new Slot(PROOFS_CALL_RESULT_DOMAIN, 0, "invocation", IDS_OPERATION_ID, null), new Slot(PROOFS_CALL_RESULT_DOMAIN, 1, "entry", IDS_ENTRY_ID, null), new Slot(PROOFS_CALL_RESULT_DOMAIN, 2, "position", INTEGER, null)};
        SLOTS[PROOFS_CELL_DOMAIN.ordinal()] = new Slot[] {new Slot(PROOFS_CELL_DOMAIN, 0, "cell", IDS_STORAGE_ID, null)};
        SLOTS[PROOFS_DISJOINT_STORAGE.ordinal()] = new Slot[] {new Slot(PROOFS_DISJOINT_STORAGE, 0, "storage", LIST, IDS_STORAGE_ID)};
        SLOTS[PROOFS_ENTRY_DOMAIN.ordinal()] = new Slot[] {new Slot(PROOFS_ENTRY_DOMAIN, 0, "entry", IDS_ENTRY_ID, null)};
        SLOTS[PROOFS_EXTERNAL_PARAMETER_DOMAIN.ordinal()] = new Slot[] {new Slot(PROOFS_EXTERNAL_PARAMETER_DOMAIN, 0, "invocation", IDS_OPERATION_ID, null), new Slot(PROOFS_EXTERNAL_PARAMETER_DOMAIN, 1, "position", INTEGER, null)};
        SLOTS[PROOFS_EXTERNAL_RESULT_DOMAIN.ordinal()] = new Slot[] {new Slot(PROOFS_EXTERNAL_RESULT_DOMAIN, 0, "invocation", IDS_OPERATION_ID, null), new Slot(PROOFS_EXTERNAL_RESULT_DOMAIN, 1, "position", INTEGER, null)};
        SLOTS[PROOFS_INTERSECTION.ordinal()] = new Slot[] {new Slot(PROOFS_INTERSECTION, 0, "left", PROOFS_DOMAIN_PROOF_SCOPE, null), new Slot(PROOFS_INTERSECTION, 1, "right", PROOFS_DOMAIN_PROOF_SCOPE, null)};
        SLOTS[PROOFS_INVOCATION_DOMAIN.ordinal()] = new Slot[] {new Slot(PROOFS_INVOCATION_DOMAIN, 0, "invocation", IDS_OPERATION_ID, null)};
        SLOTS[PROOFS_OBJECT_DOMAIN.ordinal()] = new Slot[] {new Slot(PROOFS_OBJECT_DOMAIN, 0, "object", IDS_OBJECT_ID, null)};
        SLOTS[PROOFS_OPERAND_DOMAIN.ordinal()] = new Slot[] {new Slot(PROOFS_OPERAND_DOMAIN, 0, "operand", IDS_OPERAND_ID, null)};
        SLOTS[PROOFS_OPERATION_DOMAIN.ordinal()] = new Slot[] {new Slot(PROOFS_OPERATION_DOMAIN, 0, "operation", IDS_OPERATION_ID, null)};
        SLOTS[PROOFS_PARAMETER_DOMAIN.ordinal()] = new Slot[] {new Slot(PROOFS_PARAMETER_DOMAIN, 0, "entry", IDS_ENTRY_ID, null), new Slot(PROOFS_PARAMETER_DOMAIN, 1, "position", INTEGER, null)};
        SLOTS[PROOFS_PREMISE.ordinal()] = new Slot[] {new Slot(PROOFS_PREMISE, 0, "id", IDS_PREMISE_ID, null), new Slot(PROOFS_PREMISE, 1, "authority", TEXT, null), new Slot(PROOFS_PREMISE, 2, "justification", TEXT, null), new Slot(PROOFS_PREMISE, 3, "origin", IDS_ORIGIN_ID, null), new Slot(PROOFS_PREMISE, 4, "assertion", PROOFS_ASSERTION, null)};
        ENUMS[PROOFS_PUBLICATION_DOMAIN.ordinal()] = new String[] {"INSTANCE"};
        SLOTS[PROOFS_RESULT_DOMAIN.ordinal()] = new Slot[] {new Slot(PROOFS_RESULT_DOMAIN, 0, "entry", IDS_ENTRY_ID, null), new Slot(PROOFS_RESULT_DOMAIN, 1, "position", INTEGER, null)};
        SLOTS[PROOFS_SAME_DOMAIN.ordinal()] = new Slot[] {new Slot(PROOFS_SAME_DOMAIN, 0, "left", PROOFS_DOMAIN_SUBJECT, null), new Slot(PROOFS_SAME_DOMAIN, 1, "right", PROOFS_DOMAIN_SUBJECT, null), new Slot(PROOFS_SAME_DOMAIN, 2, "scope", PROOFS_DOMAIN_PROOF_SCOPE, null)};
        SLOTS[PROOFS_UNIT_DOMAIN.ordinal()] = new Slot[] {new Slot(PROOFS_UNIT_DOMAIN, 0, "unit", IDS_UNIT_ID, null)};
        SLOTS[PUBLICATION.ordinal()] = new Slot[] {new Slot(PUBLICATION, 0, "id", IDS_PUBLICATION_ID, null), new Slot(PUBLICATION, 1, "airVersion", SEMANTIC_VERSION, null), new Slot(PUBLICATION, 2, "capabilities", CAPABILITIES_MANIFEST, null), new Slot(PUBLICATION, 3, "artifacts", LIST, ORIGINS_ARTIFACT), new Slot(PUBLICATION, 4, "units", LIST, UNIT), new Slot(PUBLICATION, 5, "storage", LIST, MEMORY_STORAGE), new Slot(PUBLICATION, 6, "resources", LIST, INTERACTIONS_RESOURCE), new Slot(PUBLICATION, 7, "artifactRelations", LIST, ARTIFACTS_RELATION), new Slot(PUBLICATION, 8, "origins", LIST, ORIGINS_ORIGIN), new Slot(PUBLICATION, 9, "coverage", EVIDENCE_COVERAGE, null), new Slot(PUBLICATION, 10, "uncertainties", LIST, EVIDENCE_UNCERTAINTY), new Slot(PUBLICATION, 11, "premises", LIST, PROOFS_PREMISE)};
        SLOTS[SCOPES_ALL_CONTROL.ordinal()] = new Slot[] {new Slot(SCOPES_ALL_CONTROL, 0, "publication", IDS_PUBLICATION_ID, null)};
        SLOTS[SCOPES_ALL_MEMORY.ordinal()] = new Slot[] {new Slot(SCOPES_ALL_MEMORY, 0, "publication", IDS_PUBLICATION_ID, null), new Slot(SCOPES_ALL_MEMORY, 1, "includingEnvironment", BOOLEAN, null)};
        ENUMS[SCOPES_ANY_RESOURCE.ordinal()] = new String[] {"INSTANCE"};
        SLOTS[SCOPES_CONTROL_UNION.ordinal()] = new Slot[] {new Slot(SCOPES_CONTROL_UNION, 0, "members", LIST, SCOPES_CONTROL_SCOPE)};
        SLOTS[SCOPES_ENTITY_SCOPE.ordinal()] = new Slot[] {new Slot(SCOPES_ENTITY_SCOPE, 0, "entities", LIST, IDS_ID)};
        SLOTS[SCOPES_LABELS_CONTROL.ordinal()] = new Slot[] {new Slot(SCOPES_LABELS_CONTROL, 0, "labels", LIST, IDS_LABEL_ID)};
        SLOTS[SCOPES_MEMORY_UNION.ordinal()] = new Slot[] {new Slot(SCOPES_MEMORY_UNION, 0, "members", LIST, SCOPES_MEMORY_SCOPE)};
        ENUMS[SCOPES_NO_CONTROL.ordinal()] = new String[] {"INSTANCE"};
        ENUMS[SCOPES_NO_MEMORY.ordinal()] = new String[] {"INSTANCE"};
        ENUMS[SCOPES_NO_RESOURCES.ordinal()] = new String[] {"INSTANCE"};
        SLOTS[SCOPES_OBJECTS_MEMORY.ordinal()] = new Slot[] {new Slot(SCOPES_OBJECTS_MEMORY, 0, "objects", LIST, IDS_OBJECT_ID)};
        SLOTS[SCOPES_PUBLICATION_SCOPE.ordinal()] = new Slot[] {new Slot(SCOPES_PUBLICATION_SCOPE, 0, "publication", IDS_PUBLICATION_ID, null)};
        SLOTS[SCOPES_RESOURCE_CATEGORIES.ordinal()] = new Slot[] {new Slot(SCOPES_RESOURCE_CATEGORIES, 0, "categories", LIST, TEXT)};
        SLOTS[SCOPES_STORAGE_MEMORY.ordinal()] = new Slot[] {new Slot(SCOPES_STORAGE_MEMORY, 0, "storage", LIST, IDS_STORAGE_ID)};
        SLOTS[SCOPES_UNIT_CONTROL.ordinal()] = new Slot[] {new Slot(SCOPES_UNIT_CONTROL, 0, "unit", IDS_UNIT_ID, null), new Slot(SCOPES_UNIT_CONTROL, 1, "labels", BOOLEAN, null), new Slot(SCOPES_UNIT_CONTROL, 2, "normalExit", BOOLEAN, null), new Slot(SCOPES_UNIT_CONTROL, 3, "exceptionalExit", BOOLEAN, null), new Slot(SCOPES_UNIT_CONTROL, 4, "halt", BOOLEAN, null), new Slot(SCOPES_UNIT_CONTROL, 5, "diverge", BOOLEAN, null), new Slot(SCOPES_UNIT_CONTROL, 6, "externalControl", BOOLEAN, null)};
        SLOTS[SCOPES_UNIT_SCOPE.ordinal()] = new Slot[] {new Slot(SCOPES_UNIT_SCOPE, 0, "unit", IDS_UNIT_ID, null)};
        SLOTS[SCOPES_VISIBLE_MEMORY.ordinal()] = new Slot[] {new Slot(SCOPES_VISIBLE_MEMORY, 0, "unit", IDS_UNIT_ID, null), new Slot(SCOPES_VISIBLE_MEMORY, 1, "includingExternal", BOOLEAN, null)};
        SLOTS[SCOPES_WITHIN_CONTROL.ordinal()] = new Slot[] {new Slot(SCOPES_WITHIN_CONTROL, 0, "scope", SCOPES_CONTROL_SCOPE, null)};
        SLOTS[SCOPES_WITHIN_MEMORY.ordinal()] = new Slot[] {new Slot(SCOPES_WITHIN_MEMORY, 0, "scope", SCOPES_MEMORY_SCOPE, null)};
        SLOTS[SEMANTIC_VERSION.ordinal()] = new Slot[] {new Slot(SEMANTIC_VERSION, 0, "major", INTEGER, null), new Slot(SEMANTIC_VERSION, 1, "minor", INTEGER, null), new Slot(SEMANTIC_VERSION, 2, "patch", INTEGER, null)};
        SLOTS[SEQUENCE.ordinal()] = new Slot[] {new Slot(SEQUENCE, 0, "label", IDS_LABEL_ID, null), new Slot(SEQUENCE, 1, "instructions", LIST, INSTRUCTION), new Slot(SEQUENCE, 2, "terminator", TERMINATOR, null), new Slot(SEQUENCE, 3, "origin", IDS_ORIGIN_ID, null)};
        ENUMS[TYPES_BUILTIN.ordinal()] = new String[] {"BOOL", "INT", "DECIMAL", "TEXT", "BYTES"};
        SLOTS[TYPES_EXTENSION_TYPE.ordinal()] = new Slot[] {new Slot(TYPES_EXTENSION_TYPE, 0, "name", TEXT, null), new Slot(TYPES_EXTENSION_TYPE, 1, "version", TEXT, null)};
        SLOTS[TYPES_KNOWN.ordinal()] = new Slot[] {new Slot(TYPES_KNOWN, 0, "type", TYPES_TYPE, null)};
        SLOTS[TYPES_LABEL_TYPE.ordinal()] = new Slot[] {new Slot(TYPES_LABEL_TYPE, 0, "unit", IDS_UNIT_ID, null), new Slot(TYPES_LABEL_TYPE, 1, "labels", LIST, IDS_LABEL_ID)};
        SLOTS[TYPES_UNKNOWN_TYPE.ordinal()] = new Slot[] {new Slot(TYPES_UNKNOWN_TYPE, 0, "uncertainty", IDS_UNCERTAINTY_ID, null)};
        SLOTS[UNIT.ordinal()] = new Slot[] {new Slot(UNIT, 0, "id", IDS_UNIT_ID, null), new Slot(UNIT, 1, "containingUnit", OPTIONAL, IDS_UNIT_ID), new Slot(UNIT, 2, "objects", LIST, MEMORY_OBJECT_DECLARATION), new Slot(UNIT, 3, "visibleObjects", LIST, IDS_OBJECT_ID), new Slot(UNIT, 4, "entries", LIST, ENTRIES_ENTRY), new Slot(UNIT, 5, "sequences", LIST, SEQUENCE), new Slot(UNIT, 6, "completionPorts", LIST, ENTRIES_COMPLETION_PORT), new Slot(UNIT, 7, "body", UNIT_BODY_AVAILABILITY, null), new Slot(UNIT, 8, "bodyUnavailable", OPTIONAL, IDS_UNCERTAINTY_ID), new Slot(UNIT, 9, "coverage", EVIDENCE_COVERAGE, null), new Slot(UNIT, 10, "origin", IDS_ORIGIN_ID, null)};
        ENUMS[UNIT_BODY_AVAILABILITY.ordinal()] = new String[] {"AVAILABLE", "UNAVAILABLE"};
        SLOTS[VALUES_BOOL_VALUE.ordinal()] = new Slot[] {new Slot(VALUES_BOOL_VALUE, 0, "value", BOOLEAN, null)};
        SLOTS[VALUES_BYTES_VALUE.ordinal()] = new Slot[] {new Slot(VALUES_BYTES_VALUE, 0, "octets", LIST, SMALL_INTEGER)};
        SLOTS[VALUES_DECIMAL_VALUE.ordinal()] = new Slot[] {new Slot(VALUES_DECIMAL_VALUE, 0, "coefficient", INTEGER, null), new Slot(VALUES_DECIMAL_VALUE, 1, "scale", INTEGER, null)};
        SLOTS[VALUES_INT_VALUE.ordinal()] = new Slot[] {new Slot(VALUES_INT_VALUE, 0, "value", INTEGER, null)};
        SLOTS[VALUES_LABEL_VALUE.ordinal()] = new Slot[] {new Slot(VALUES_LABEL_VALUE, 0, "label", IDS_LABEL_ID, null), new Slot(VALUES_LABEL_VALUE, 1, "domain", TYPES_LABEL_TYPE, null)};
        SLOTS[VALUES_TEXT_VALUE.ordinal()] = new Slot[] {new Slot(VALUES_TEXT_VALUE, 0, "value", TEXT, null)};
    }
}
