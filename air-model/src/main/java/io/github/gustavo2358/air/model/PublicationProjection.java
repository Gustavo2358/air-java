package io.github.gustavo2358.air.model;

/** Explicit complete typed projection. Runtime reflection and guessed wire tags are absent. */
final class PublicationProjection {
    private PublicationProjection() { }
    static AirShape shape(Object value) {
        return switch (value) {
            case String ignored -> AirShape.TEXT;
            case java.math.BigInteger ignored -> AirShape.INTEGER;
            case Boolean ignored -> AirShape.BOOLEAN;
            case Integer ignored -> AirShape.SMALL_INTEGER;
            case java.util.List<?> ignored -> AirShape.LIST;
            case java.util.Optional<?> ignored -> AirShape.OPTIONAL;
            case Artifacts.ExternalArtifact ignored -> AirShape.ARTIFACTS_EXTERNAL_ARTIFACT;
            case Artifacts.InternalArtifact ignored -> AirShape.ARTIFACTS_INTERNAL_ARTIFACT;
            case Artifacts.Relation ignored -> AirShape.ARTIFACTS_RELATION;
            case Capabilities.Capability ignored -> AirShape.CAPABILITIES_CAPABILITY;
            case Capabilities.Manifest ignored -> AirShape.CAPABILITIES_MANIFEST;
            case Control.After ignored -> AirShape.CONTROL_AFTER;
            case Control.AnyException ignored -> AirShape.CONTROL_ANY_EXCEPTION;
            case Control.Before ignored -> AirShape.CONTROL_BEFORE;
            case Control.ContinueAlternative ignored -> AirShape.CONTROL_CONTINUE_ALTERNATIVE;
            case Control.ControlEnvelope ignored -> AirShape.CONTROL_CONTROL_ENVELOPE;
            case Control.Diverge ignored -> AirShape.CONTROL_DIVERGE;
            case Control.DivergeOutcome ignored -> AirShape.CONTROL_DIVERGE_OUTCOME;
            case Control.EntryPoint ignored -> AirShape.CONTROL_ENTRY_POINT;
            case Control.ExceptionOutcome ignored -> AirShape.CONTROL_EXCEPTION_OUTCOME;
            case Control.Exceptional ignored -> AirShape.CONTROL_EXCEPTIONAL;
            case Control.ExitPoint ignored -> AirShape.CONTROL_EXIT_POINT;
            case Control.HaltAlternative ignored -> AirShape.CONTROL_HALT_ALTERNATIVE;
            case Control.HaltOutcome ignored -> AirShape.CONTROL_HALT_OUTCOME;
            case Control.Handler ignored -> AirShape.CONTROL_HANDLER;
            case Control.InvocationOutcomes ignored -> AirShape.CONTROL_INVOCATION_OUTCOMES;
            case Control.JumpAlternative ignored -> AirShape.CONTROL_JUMP_ALTERNATIVE;
            case Control.Normal ignored -> AirShape.CONTROL_NORMAL;
            case Control.NormalOutcome ignored -> AirShape.CONTROL_NORMAL_OUTCOME;
            case Control.OtherExceptionOutcome ignored -> AirShape.CONTROL_OTHER_EXCEPTION_OUTCOME;
            case Control.Propagate ignored -> AirShape.CONTROL_PROPAGATE;
            case Control.ReturnAlternative ignored -> AirShape.CONTROL_RETURN_ALTERNATIVE;
            case DecimalText.Kind ignored -> AirShape.DECIMAL_TEXT_KIND;
            case DecimalText.Part ignored -> AirShape.DECIMAL_TEXT_PART;
            case Entries.CompletionPort ignored -> AirShape.ENTRIES_COMPLETION_PORT;
            case Entries.Entry ignored -> AirShape.ENTRIES_ENTRY;
            case Entries.EntryState ignored -> AirShape.ENTRIES_ENTRY_STATE;
            case Entries.ExternalUnknown ignored -> AirShape.ENTRIES_EXTERNAL_UNKNOWN;
            case Entries.InitialCondition ignored -> AirShape.ENTRIES_INITIAL_CONDITION;
            case Entries.LiteralInitial ignored -> AirShape.ENTRIES_LITERAL_INITIAL;
            case Entries.ParameterInitial ignored -> AirShape.ENTRIES_PARAMETER_INITIAL;
            case Entries.PossibleLiterals ignored -> AirShape.ENTRIES_POSSIBLE_LITERALS;
            case Entries.Preserve ignored -> AirShape.ENTRIES_PRESERVE;
            case Entries.Uninitialized ignored -> AirShape.ENTRIES_UNINITIALIZED;
            case Envelopes.DependencyEnvelope ignored -> AirShape.ENVELOPES_DEPENDENCY_ENVELOPE;
            case Envelopes.Envelope ignored -> AirShape.ENVELOPES_ENVELOPE;
            case Envelopes.MemoryEnvelope ignored -> AirShape.ENVELOPES_MEMORY_ENVELOPE;
            case Envelopes.ResourceUse ignored -> AirShape.ENVELOPES_RESOURCE_USE;
            case Evidence.Claim ignored -> AirShape.EVIDENCE_CLAIM;
            case Evidence.Coverage ignored -> AirShape.EVIDENCE_COVERAGE;
            case Evidence.CoverageItem ignored -> AirShape.EVIDENCE_COVERAGE_ITEM;
            case Evidence.CoverageStatus ignored -> AirShape.EVIDENCE_COVERAGE_STATUS;
            case Evidence.Dimension ignored -> AirShape.EVIDENCE_DIMENSION;
            case Evidence.Elimination ignored -> AirShape.EVIDENCE_ELIMINATION;
            case Evidence.InventoryStatus ignored -> AirShape.EVIDENCE_INVENTORY_STATUS;
            case Evidence.Precision ignored -> AirShape.EVIDENCE_PRECISION;
            case Evidence.PrecisionStatus ignored -> AirShape.EVIDENCE_PRECISION_STATUS;
            case Evidence.Uncertainty ignored -> AirShape.EVIDENCE_UNCERTAINTY;
            case Expressions.Binary ignored -> AirShape.EXPRESSIONS_BINARY;
            case Expressions.BinaryOperator ignored -> AirShape.EXPRESSIONS_BINARY_OPERATOR;
            case Expressions.FillText ignored -> AirShape.EXPRESSIONS_FILL_TEXT;
            case Expressions.FitDecimal ignored -> AirShape.EXPRESSIONS_FIT_DECIMAL;
            case Expressions.FitText ignored -> AirShape.EXPRESSIONS_FIT_TEXT;
            case Expressions.FormatDecimal ignored -> AirShape.EXPRESSIONS_FORMAT_DECIMAL;
            case Expressions.IntegerDigits ignored -> AirShape.EXPRESSIONS_INTEGER_DIGITS;
            case Expressions.Literal ignored -> AirShape.EXPRESSIONS_LITERAL;
            case Expressions.ParseInteger ignored -> AirShape.EXPRESSIONS_PARSE_INTEGER;
            case Expressions.Quantize ignored -> AirShape.EXPRESSIONS_QUANTIZE;
            case Expressions.Read ignored -> AirShape.EXPRESSIONS_READ;
            case Expressions.Rounding ignored -> AirShape.EXPRESSIONS_ROUNDING;
            case Expressions.SliceText ignored -> AirShape.EXPRESSIONS_SLICE_TEXT;
            case Expressions.TrimRight ignored -> AirShape.EXPRESSIONS_TRIM_RIGHT;
            case Expressions.Unary ignored -> AirShape.EXPRESSIONS_UNARY;
            case Expressions.UnaryOperator ignored -> AirShape.EXPRESSIONS_UNARY_OPERATOR;
            case Expressions.Unknown ignored -> AirShape.EXPRESSIONS_UNKNOWN;
            case Expressions.WrapInteger ignored -> AirShape.EXPRESSIONS_WRAP_INTEGER;
            case Ids.ArtifactId ignored -> AirShape.IDS_ARTIFACT_ID;
            case Ids.ArtifactRelationId ignored -> AirShape.IDS_ARTIFACT_RELATION_ID;
            case Ids.CompletionPortId ignored -> AirShape.IDS_COMPLETION_PORT_ID;
            case Ids.EntryId ignored -> AirShape.IDS_ENTRY_ID;
            case Ids.EntryOwner ignored -> AirShape.IDS_ENTRY_OWNER;
            case Ids.LabelId ignored -> AirShape.IDS_LABEL_ID;
            case Ids.ObjectId ignored -> AirShape.IDS_OBJECT_ID;
            case Ids.OperandId ignored -> AirShape.IDS_OPERAND_ID;
            case Ids.OperationId ignored -> AirShape.IDS_OPERATION_ID;
            case Ids.OperationOwner ignored -> AirShape.IDS_OPERATION_OWNER;
            case Ids.OriginId ignored -> AirShape.IDS_ORIGIN_ID;
            case Ids.PremiseId ignored -> AirShape.IDS_PREMISE_ID;
            case Ids.PublicationId ignored -> AirShape.IDS_PUBLICATION_ID;
            case Ids.ResourceId ignored -> AirShape.IDS_RESOURCE_ID;
            case Ids.StorageId ignored -> AirShape.IDS_STORAGE_ID;
            case Ids.UncertaintyId ignored -> AirShape.IDS_UNCERTAINTY_ID;
            case Ids.UnitId ignored -> AirShape.IDS_UNIT_ID;
            case Interactions.ComputedResource ignored -> AirShape.INTERACTIONS_COMPUTED_RESOURCE;
            case Interactions.ComputedTarget ignored -> AirShape.INTERACTIONS_COMPUTED_TARGET;
            case Interactions.ContractRef ignored -> AirShape.INTERACTIONS_CONTRACT_REF;
            case Interactions.CopyArgument ignored -> AirShape.INTERACTIONS_COPY_ARGUMENT;
            case Interactions.EffectBound ignored -> AirShape.INTERACTIONS_EFFECT_BOUND;
            case Interactions.EntrySignature ignored -> AirShape.INTERACTIONS_ENTRY_SIGNATURE;
            case Interactions.ExactName ignored -> AirShape.INTERACTIONS_EXACT_NAME;
            case Interactions.ExtensionName ignored -> AirShape.INTERACTIONS_EXTENSION_NAME;
            case Interactions.ExternalBinding ignored -> AirShape.INTERACTIONS_EXTERNAL_BINDING;
            case Interactions.ExternalSignature ignored -> AirShape.INTERACTIONS_EXTERNAL_SIGNATURE;
            case Interactions.ForeignEffects ignored -> AirShape.INTERACTIONS_FOREIGN_EFFECTS;
            case Interactions.InternalTarget ignored -> AirShape.INTERACTIONS_INTERNAL_TARGET;
            case Interactions.KnownContract ignored -> AirShape.INTERACTIONS_KNOWN_CONTRACT;
            case Interactions.KnownMode ignored -> AirShape.INTERACTIONS_KNOWN_MODE;
            case Interactions.LiteralTarget ignored -> AirShape.INTERACTIONS_LITERAL_TARGET;
            case Interactions.LocalResource ignored -> AirShape.INTERACTIONS_LOCAL_RESOURCE;
            case Interactions.NoRemainder ignored -> AirShape.INTERACTIONS_NO_REMAINDER;
            case Interactions.ObjectBinding ignored -> AirShape.INTERACTIONS_OBJECT_BINDING;
            case Interactions.OutcomeEffects ignored -> AirShape.INTERACTIONS_OUTCOME_EFFECTS;
            case Interactions.Parameter ignored -> AirShape.INTERACTIONS_PARAMETER;
            case Interactions.ParameterInventory ignored -> AirShape.INTERACTIONS_PARAMETER_INVENTORY;
            case Interactions.PassingMode ignored -> AirShape.INTERACTIONS_PASSING_MODE;
            case Interactions.ReferenceArgument ignored -> AirShape.INTERACTIONS_REFERENCE_ARGUMENT;
            case Interactions.Resource ignored -> AirShape.INTERACTIONS_RESOURCE;
            case Interactions.ResourceDeclaration ignored -> AirShape.INTERACTIONS_RESOURCE_DECLARATION;
            case Interactions.ResourceObject ignored -> AirShape.INTERACTIONS_RESOURCE_OBJECT;
            case Interactions.ResourceUse ignored -> AirShape.INTERACTIONS_RESOURCE_USE;
            case Interactions.ResultInventory ignored -> AirShape.INTERACTIONS_RESULT_INVENTORY;
            case Interactions.ResultSlot ignored -> AirShape.INTERACTIONS_RESULT_SLOT;
            case Interactions.Signature ignored -> AirShape.INTERACTIONS_SIGNATURE;
            case Interactions.UnknownContract ignored -> AirShape.INTERACTIONS_UNKNOWN_CONTRACT;
            case Interactions.UnknownMode ignored -> AirShape.INTERACTIONS_UNKNOWN_MODE;
            case Interactions.UnknownName ignored -> AirShape.INTERACTIONS_UNKNOWN_NAME;
            case Interactions.UnknownParameterBinding ignored -> AirShape.INTERACTIONS_UNKNOWN_PARAMETER_BINDING;
            case Interactions.UnknownRemainder ignored -> AirShape.INTERACTIONS_UNKNOWN_REMAINDER;
            case Interactions.UnknownResource ignored -> AirShape.INTERACTIONS_UNKNOWN_RESOURCE;
            case Interactions.ValueArgument ignored -> AirShape.INTERACTIONS_VALUE_ARGUMENT;
            case Memory.AliasBinding ignored -> AirShape.MEMORY_ALIAS_BINDING;
            case Memory.AlternativesBinding ignored -> AirShape.MEMORY_ALTERNATIVES_BINDING;
            case Memory.AsciiText ignored -> AirShape.MEMORY_ASCII_TEXT;
            case Memory.BinaryCodec ignored -> AirShape.MEMORY_BINARY_CODEC;
            case Memory.ByteOrder ignored -> AirShape.MEMORY_BYTE_ORDER;
            case Memory.ByteRange ignored -> AirShape.MEMORY_BYTE_RANGE;
            case Memory.Cell ignored -> AirShape.MEMORY_CELL;
            case Memory.CellBinding ignored -> AirShape.MEMORY_CELL_BINDING;
            case Memory.ExtensionCodec ignored -> AirShape.MEMORY_EXTENSION_CODEC;
            case Memory.IdentityBytes ignored -> AirShape.MEMORY_IDENTITY_BYTES;
            case Memory.Lifetime ignored -> AirShape.MEMORY_LIFETIME;
            case Memory.ObjectDeclaration ignored -> AirShape.MEMORY_OBJECT_DECLARATION;
            case Memory.Region ignored -> AirShape.MEMORY_REGION;
            case Memory.StorageHeader ignored -> AirShape.MEMORY_STORAGE_HEADER;
            case Memory.UnknownBinding ignored -> AirShape.MEMORY_UNKNOWN_BINDING;
            case Memory.UnknownCodec ignored -> AirShape.MEMORY_UNKNOWN_CODEC;
            case Memory.ViewBinding ignored -> AirShape.MEMORY_VIEW_BINDING;
            case Memory.Visibility ignored -> AirShape.MEMORY_VISIBILITY;
            case Operand.Header ignored -> AirShape.OPERAND_HEADER;
            case Operand.Role ignored -> AirShape.OPERAND_ROLE;
            case Operations.Assign ignored -> AirShape.OPERATIONS_ASSIGN;
            case Operations.Branch ignored -> AirShape.OPERATIONS_BRANCH;
            case Operations.Case ignored -> AirShape.OPERATIONS_CASE;
            case Operations.CopyBytes ignored -> AirShape.OPERATIONS_COPY_BYTES;
            case Operations.Dispatch ignored -> AirShape.OPERATIONS_DISPATCH;
            case Operations.Halt ignored -> AirShape.OPERATIONS_HALT;
            case Operations.HaltKind ignored -> AirShape.OPERATIONS_HALT_KIND;
            case Operations.HavocMay ignored -> AirShape.OPERATIONS_HAVOC_MAY;
            case Operations.HavocMust ignored -> AirShape.OPERATIONS_HAVOC_MUST;
            case Operations.Header ignored -> AirShape.OPERATIONS_HEADER;
            case Operations.IndirectJump ignored -> AirShape.OPERATIONS_INDIRECT_JUMP;
            case Operations.Invoke ignored -> AirShape.OPERATIONS_INVOKE;
            case Operations.Jump ignored -> AirShape.OPERATIONS_JUMP;
            case Operations.LocalBoundary ignored -> AirShape.OPERATIONS_LOCAL_BOUNDARY;
            case Operations.LocalInvoke ignored -> AirShape.OPERATIONS_LOCAL_INVOKE;
            case Operations.LocalResume ignored -> AirShape.OPERATIONS_LOCAL_RESUME;
            case Operations.LocalUnwind ignored -> AirShape.OPERATIONS_LOCAL_UNWIND;
            case Operations.Nop ignored -> AirShape.OPERATIONS_NOP;
            case Operations.Opaque ignored -> AirShape.OPERATIONS_OPAQUE;
            case Operations.Raise ignored -> AirShape.OPERATIONS_RAISE;
            case Operations.ReentryGuard ignored -> AirShape.OPERATIONS_REENTRY_GUARD;
            case Operations.ResumeRoute ignored -> AirShape.OPERATIONS_RESUME_ROUTE;
            case Operations.Return ignored -> AirShape.OPERATIONS_RETURN;
            case Origins.Artifact ignored -> AirShape.ORIGINS_ARTIFACT;
            case Origins.ColumnUnit ignored -> AirShape.ORIGINS_COLUMN_UNIT;
            case Origins.Contractual ignored -> AirShape.ORIGINS_CONTRACTUAL;
            case Origins.Derived ignored -> AirShape.ORIGINS_DERIVED;
            case Origins.IncludeFrame ignored -> AirShape.ORIGINS_INCLUDE_FRAME;
            case Origins.LineColumns ignored -> AirShape.ORIGINS_LINE_COLUMNS;
            case Origins.Offsets ignored -> AirShape.ORIGINS_OFFSETS;
            case Origins.Position ignored -> AirShape.ORIGINS_POSITION;
            case Origins.Span ignored -> AirShape.ORIGINS_SPAN;
            case Origins.Unavailable ignored -> AirShape.ORIGINS_UNAVAILABLE;
            case Origins.Written ignored -> AirShape.ORIGINS_WRITTEN;
            case Places.Choice ignored -> AirShape.PLACES_CHOICE;
            case Places.ObjectPlace ignored -> AirShape.PLACES_OBJECT_PLACE;
            case Places.RegionSlice ignored -> AirShape.PLACES_REGION_SLICE;
            case Proofs.CallParameterDomain ignored -> AirShape.PROOFS_CALL_PARAMETER_DOMAIN;
            case Proofs.CallResultDomain ignored -> AirShape.PROOFS_CALL_RESULT_DOMAIN;
            case Proofs.CellDomain ignored -> AirShape.PROOFS_CELL_DOMAIN;
            case Proofs.DisjointStorage ignored -> AirShape.PROOFS_DISJOINT_STORAGE;
            case Proofs.EntryDomain ignored -> AirShape.PROOFS_ENTRY_DOMAIN;
            case Proofs.ExternalParameterDomain ignored -> AirShape.PROOFS_EXTERNAL_PARAMETER_DOMAIN;
            case Proofs.ExternalResultDomain ignored -> AirShape.PROOFS_EXTERNAL_RESULT_DOMAIN;
            case Proofs.Intersection ignored -> AirShape.PROOFS_INTERSECTION;
            case Proofs.InvocationDomain ignored -> AirShape.PROOFS_INVOCATION_DOMAIN;
            case Proofs.ObjectDomain ignored -> AirShape.PROOFS_OBJECT_DOMAIN;
            case Proofs.OperandDomain ignored -> AirShape.PROOFS_OPERAND_DOMAIN;
            case Proofs.OperationDomain ignored -> AirShape.PROOFS_OPERATION_DOMAIN;
            case Proofs.ParameterDomain ignored -> AirShape.PROOFS_PARAMETER_DOMAIN;
            case Proofs.Premise ignored -> AirShape.PROOFS_PREMISE;
            case Proofs.PublicationDomain ignored -> AirShape.PROOFS_PUBLICATION_DOMAIN;
            case Proofs.ResultDomain ignored -> AirShape.PROOFS_RESULT_DOMAIN;
            case Proofs.SameDomain ignored -> AirShape.PROOFS_SAME_DOMAIN;
            case Proofs.UnitDomain ignored -> AirShape.PROOFS_UNIT_DOMAIN;
            case Publication ignored -> AirShape.PUBLICATION;
            case Scopes.AllControl ignored -> AirShape.SCOPES_ALL_CONTROL;
            case Scopes.AllMemory ignored -> AirShape.SCOPES_ALL_MEMORY;
            case Scopes.AnyResource ignored -> AirShape.SCOPES_ANY_RESOURCE;
            case Scopes.ControlUnion ignored -> AirShape.SCOPES_CONTROL_UNION;
            case Scopes.EntityScope ignored -> AirShape.SCOPES_ENTITY_SCOPE;
            case Scopes.LabelsControl ignored -> AirShape.SCOPES_LABELS_CONTROL;
            case Scopes.MemoryUnion ignored -> AirShape.SCOPES_MEMORY_UNION;
            case Scopes.NoControl ignored -> AirShape.SCOPES_NO_CONTROL;
            case Scopes.NoMemory ignored -> AirShape.SCOPES_NO_MEMORY;
            case Scopes.NoResources ignored -> AirShape.SCOPES_NO_RESOURCES;
            case Scopes.ObjectsMemory ignored -> AirShape.SCOPES_OBJECTS_MEMORY;
            case Scopes.PublicationScope ignored -> AirShape.SCOPES_PUBLICATION_SCOPE;
            case Scopes.ResourceCategories ignored -> AirShape.SCOPES_RESOURCE_CATEGORIES;
            case Scopes.StorageMemory ignored -> AirShape.SCOPES_STORAGE_MEMORY;
            case Scopes.UnitControl ignored -> AirShape.SCOPES_UNIT_CONTROL;
            case Scopes.UnitScope ignored -> AirShape.SCOPES_UNIT_SCOPE;
            case Scopes.VisibleMemory ignored -> AirShape.SCOPES_VISIBLE_MEMORY;
            case Scopes.WithinControl ignored -> AirShape.SCOPES_WITHIN_CONTROL;
            case Scopes.WithinMemory ignored -> AirShape.SCOPES_WITHIN_MEMORY;
            case SemanticVersion ignored -> AirShape.SEMANTIC_VERSION;
            case Sequence ignored -> AirShape.SEQUENCE;
            case Types.Builtin ignored -> AirShape.TYPES_BUILTIN;
            case Types.ExtensionType ignored -> AirShape.TYPES_EXTENSION_TYPE;
            case Types.Known ignored -> AirShape.TYPES_KNOWN;
            case Types.LabelType ignored -> AirShape.TYPES_LABEL_TYPE;
            case Types.UnknownType ignored -> AirShape.TYPES_UNKNOWN_TYPE;
            case Unit ignored -> AirShape.UNIT;
            case Unit.BodyAvailability ignored -> AirShape.UNIT_BODY_AVAILABILITY;
            case Values.BoolValue ignored -> AirShape.VALUES_BOOL_VALUE;
            case Values.BytesValue ignored -> AirShape.VALUES_BYTES_VALUE;
            case Values.DecimalValue ignored -> AirShape.VALUES_DECIMAL_VALUE;
            case Values.IntValue ignored -> AirShape.VALUES_INT_VALUE;
            case Values.LabelValue ignored -> AirShape.VALUES_LABEL_VALUE;
            case Values.TextValue ignored -> AirShape.VALUES_TEXT_VALUE;
            default -> throw new IllegalArgumentException("value outside AIR Publication vocabulary");
        };
    }

    static Object field(Object value, int field) {
        return switch (value) {
            case Artifacts.ExternalArtifact record -> switch (field) {
                case 0 -> record.resource();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Artifacts.InternalArtifact record -> switch (field) {
                case 0 -> record.artifact();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Artifacts.Relation record -> switch (field) {
                case 0 -> record.id();
                case 1 -> record.source();
                case 2 -> record.destination();
                case 3 -> record.kind();
                case 4 -> record.origin();
                case 5 -> record.coverage();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Capabilities.Capability record -> switch (field) {
                case 0 -> record.name();
                case 1 -> record.version();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Capabilities.Manifest record -> switch (field) {
                case 0 -> record.required();
                case 1 -> record.provided();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Control.After record -> switch (field) {
                case 0 -> record.operation();
                case 1 -> record.outcome();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Control.AnyException record -> switch (field) {
                case 0 -> record.destination();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Control.Before record -> switch (field) {
                case 0 -> record.operation();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Control.ControlEnvelope record -> switch (field) {
                case 0 -> record.known();
                case 1 -> record.remainder();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Control.EntryPoint record -> switch (field) {
                case 0 -> record.entry();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Control.ExceptionOutcome record -> switch (field) {
                case 0 -> record.tag();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Control.Exceptional record -> switch (field) {
                case 0 -> record.tag();
                case 1 -> record.destination();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Control.ExitPoint record -> switch (field) {
                case 0 -> record.unit();
                case 1 -> record.outcome();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Control.Handler record -> switch (field) {
                case 0 -> record.label();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Control.InvocationOutcomes record -> switch (field) {
                case 0 -> record.known();
                case 1 -> record.remainder();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Control.JumpAlternative record -> switch (field) {
                case 0 -> record.label();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Control.Normal record -> switch (field) {
                case 0 -> record.label();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case DecimalText.Part record -> switch (field) {
                case 0 -> record.kind();
                case 1 -> record.count();
                case 2 -> record.text();
                case 3 -> record.negative();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Entries.CompletionPort record -> switch (field) {
                case 0 -> record.id();
                case 1 -> record.origin();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Entries.Entry record -> switch (field) {
                case 0 -> record.id();
                case 1 -> record.initialLabel();
                case 2 -> record.signature();
                case 3 -> record.state();
                case 4 -> record.origin();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Entries.EntryState record -> switch (field) {
                case 0 -> record.conditions();
                case 1 -> record.uncertainties();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Entries.ExternalUnknown record -> switch (field) {
                case 0 -> record.reason();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Entries.InitialCondition record -> switch (field) {
                case 0 -> record.place();
                case 1 -> record.value();
                case 2 -> record.origin();
                case 3 -> record.premises();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Entries.LiteralInitial record -> switch (field) {
                case 0 -> record.value();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Entries.ParameterInitial record -> switch (field) {
                case 0 -> record.position();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Entries.PossibleLiterals record -> switch (field) {
                case 0 -> record.candidates();
                case 1 -> record.remainder();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Entries.Uninitialized record -> switch (field) {
                case 0 -> record.reason();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Envelopes.DependencyEnvelope record -> switch (field) {
                case 0 -> record.known();
                case 1 -> record.remainder();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Envelopes.Envelope record -> switch (field) {
                case 0 -> record.memory();
                case 1 -> record.control();
                case 2 -> record.dependencies();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Envelopes.MemoryEnvelope record -> switch (field) {
                case 0 -> record.knownReads();
                case 1 -> record.otherReads();
                case 2 -> record.knownWrites();
                case 3 -> record.otherWrites();
                case 4 -> record.mustOverwrite();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Envelopes.ResourceUse record -> switch (field) {
                case 0 -> record.action();
                case 1 -> record.target();
                case 2 -> record.point();
                case 3 -> record.origin();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Evidence.Claim record -> switch (field) {
                case 0 -> record.scope();
                case 1 -> record.status();
                case 2 -> record.reasons();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Evidence.Coverage record -> switch (field) {
                case 0 -> record.inventory();
                case 1 -> record.scope();
                case 2 -> record.items();
                case 3 -> record.uncertainties();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Evidence.CoverageItem record -> switch (field) {
                case 0 -> record.sourceKey();
                case 1 -> record.origin();
                case 2 -> record.status();
                case 3 -> record.outputs();
                case 4 -> record.uncertainties();
                case 5 -> record.elimination();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Evidence.Elimination record -> switch (field) {
                case 0 -> record.rule();
                case 1 -> record.origin();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Evidence.Precision record -> switch (field) {
                case 0 -> record.control();
                case 1 -> record.storage();
                case 2 -> record.effects();
                case 3 -> record.values();
                case 4 -> record.dependencies();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Evidence.Uncertainty record -> switch (field) {
                case 0 -> record.id();
                case 1 -> record.code();
                case 2 -> record.dimensions();
                case 3 -> record.scope();
                case 4 -> record.reason();
                case 5 -> record.origin();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Expressions.Binary record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.operator();
                case 2 -> record.left();
                case 3 -> record.right();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Expressions.FillText record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.character();
                case 2 -> record.length();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Expressions.FitDecimal record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.value();
                case 2 -> record.digits();
                case 3 -> record.scale();
                case 4 -> record.absolute();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Expressions.FitText record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.value();
                case 2 -> record.length();
                case 3 -> record.pad();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Expressions.FormatDecimal record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.value();
                case 2 -> record.parts();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Expressions.IntegerDigits record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.value();
                case 2 -> record.digits();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Expressions.Literal record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.value();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Expressions.ParseInteger record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.value();
                case 2 -> record.onInvalid();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Expressions.Quantize record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.value();
                case 2 -> record.scale();
                case 3 -> record.rounding();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Expressions.Read record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.place();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Expressions.SliceText record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.value();
                case 2 -> record.start();
                case 3 -> record.count();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Expressions.TrimRight record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.value();
                case 2 -> record.characters();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Expressions.Unary record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.operator();
                case 2 -> record.argument();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Expressions.Unknown record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.typeRef();
                case 2 -> record.dependencies();
                case 3 -> record.remainingReads();
                case 4 -> record.reason();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Expressions.WrapInteger record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.value();
                case 2 -> record.width();
                case 3 -> record.signed();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Ids.ArtifactId record -> switch (field) {
                case 0 -> record.publication();
                case 1 -> record.localId();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Ids.ArtifactRelationId record -> switch (field) {
                case 0 -> record.publication();
                case 1 -> record.localId();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Ids.CompletionPortId record -> switch (field) {
                case 0 -> record.unit();
                case 1 -> record.localId();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Ids.EntryId record -> switch (field) {
                case 0 -> record.unit();
                case 1 -> record.localId();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Ids.EntryOwner record -> switch (field) {
                case 0 -> record.entry();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Ids.LabelId record -> switch (field) {
                case 0 -> record.unit();
                case 1 -> record.localId();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Ids.ObjectId record -> switch (field) {
                case 0 -> record.unit();
                case 1 -> record.localId();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Ids.OperandId record -> switch (field) {
                case 0 -> record.owner();
                case 1 -> record.localId();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Ids.OperationId record -> switch (field) {
                case 0 -> record.unit();
                case 1 -> record.localId();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Ids.OperationOwner record -> switch (field) {
                case 0 -> record.operation();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Ids.OriginId record -> switch (field) {
                case 0 -> record.publication();
                case 1 -> record.localId();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Ids.PremiseId record -> switch (field) {
                case 0 -> record.publication();
                case 1 -> record.localId();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Ids.PublicationId record -> switch (field) {
                case 0 -> record.localId();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Ids.ResourceId record -> switch (field) {
                case 0 -> record.publication();
                case 1 -> record.localId();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Ids.StorageId record -> switch (field) {
                case 0 -> record.publication();
                case 1 -> record.localId();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Ids.UncertaintyId record -> switch (field) {
                case 0 -> record.publication();
                case 1 -> record.localId();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Ids.UnitId record -> switch (field) {
                case 0 -> record.publication();
                case 1 -> record.localId();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.ComputedResource record -> switch (field) {
                case 0 -> record.category();
                case 1 -> record.namespace();
                case 2 -> record.name();
                case 3 -> record.namePolicy();
                case 4 -> record.origin();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.ComputedTarget record -> switch (field) {
                case 0 -> record.category();
                case 1 -> record.namespace();
                case 2 -> record.name();
                case 3 -> record.namePolicy();
                case 4 -> record.origin();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.ContractRef record -> switch (field) {
                case 0 -> record.authority();
                case 1 -> record.version();
                case 2 -> record.evidence();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.CopyArgument record -> switch (field) {
                case 0 -> record.value();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.EffectBound record -> switch (field) {
                case 0 -> record.otherwise();
                case 1 -> record.perOutcome();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.EntrySignature record -> switch (field) {
                case 0 -> record.entry();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.ExtensionName record -> switch (field) {
                case 0 -> record.name();
                case 1 -> record.version();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.ExternalSignature record -> switch (field) {
                case 0 -> record.signature();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.ForeignEffects record -> switch (field) {
                case 0 -> record.reads();
                case 1 -> record.writes();
                case 2 -> record.mustOverwrite();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.InternalTarget record -> switch (field) {
                case 0 -> record.entry();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.KnownContract record -> switch (field) {
                case 0 -> record.reference();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.KnownMode record -> switch (field) {
                case 0 -> record.mode();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.LiteralTarget record -> switch (field) {
                case 0 -> record.category();
                case 1 -> record.namespace();
                case 2 -> record.name();
                case 3 -> record.namePolicy();
                case 4 -> record.origin();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.LocalResource record -> switch (field) {
                case 0 -> record.category();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.ObjectBinding record -> switch (field) {
                case 0 -> record.object();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.OutcomeEffects record -> switch (field) {
                case 0 -> record.outcome();
                case 1 -> record.effects();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.Parameter record -> switch (field) {
                case 0 -> record.position();
                case 1 -> record.mode();
                case 2 -> record.typeRef();
                case 3 -> record.objectBinding();
                case 4 -> record.origin();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.ParameterInventory record -> switch (field) {
                case 0 -> record.known();
                case 1 -> record.remainder();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.ReferenceArgument record -> switch (field) {
                case 0 -> record.place();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.Resource record -> switch (field) {
                case 0 -> record.id();
                case 1 -> record.description();
                case 2 -> record.origin();
                case 3 -> record.declaration();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.ResourceDeclaration record -> switch (field) {
                case 0 -> record.owner();
                case 1 -> record.name();
                case 2 -> record.classification();
                case 3 -> record.nameSource();
                case 4 -> record.objects();
                case 5 -> record.uses();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.ResourceObject record -> switch (field) {
                case 0 -> record.object();
                case 1 -> record.role();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.ResourceUse record -> switch (field) {
                case 0 -> record.operation();
                case 1 -> record.role();
                case 2 -> record.origin();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.ResultInventory record -> switch (field) {
                case 0 -> record.known();
                case 1 -> record.remainder();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.ResultSlot record -> switch (field) {
                case 0 -> record.position();
                case 1 -> record.typeRef();
                case 2 -> record.origin();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.Signature record -> switch (field) {
                case 0 -> record.parameters();
                case 1 -> record.results();
                case 2 -> record.origin();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.UnknownContract record -> switch (field) {
                case 0 -> record.uncertainty();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.UnknownMode record -> switch (field) {
                case 0 -> record.uncertainty();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.UnknownName record -> switch (field) {
                case 0 -> record.uncertainty();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.UnknownParameterBinding record -> switch (field) {
                case 0 -> record.uncertainty();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.UnknownRemainder record -> switch (field) {
                case 0 -> record.uncertainty();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.UnknownResource record -> switch (field) {
                case 0 -> record.category();
                case 1 -> record.namespace();
                case 2 -> record.uncertainty();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Interactions.ValueArgument record -> switch (field) {
                case 0 -> record.value();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Memory.AliasBinding record -> switch (field) {
                case 0 -> record.object();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Memory.AlternativesBinding record -> switch (field) {
                case 0 -> record.alternatives();
                case 1 -> record.remainder();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Memory.BinaryCodec record -> switch (field) {
                case 0 -> record.signed();
                case 1 -> record.width();
                case 2 -> record.order();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Memory.ByteRange record -> switch (field) {
                case 0 -> record.region();
                case 1 -> record.offset();
                case 2 -> record.extent();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Memory.Cell record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.typeRef();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Memory.CellBinding record -> switch (field) {
                case 0 -> record.storage();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Memory.ExtensionCodec record -> switch (field) {
                case 0 -> record.name();
                case 1 -> record.version();
                case 2 -> record.logicalType();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Memory.ObjectDeclaration record -> switch (field) {
                case 0 -> record.id();
                case 1 -> record.displayName();
                case 2 -> record.typeRef();
                case 3 -> record.storage();
                case 4 -> record.visibility();
                case 5 -> record.origin();
                case 6 -> record.coverage();
                case 7 -> record.precision();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Memory.Region record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.extent();
                case 2 -> record.extentUnknown();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Memory.StorageHeader record -> switch (field) {
                case 0 -> record.id();
                case 1 -> record.owner();
                case 2 -> record.lifetime();
                case 3 -> record.visibility();
                case 4 -> record.origin();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Memory.UnknownBinding record -> switch (field) {
                case 0 -> record.scope();
                case 1 -> record.reason();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Memory.UnknownCodec record -> switch (field) {
                case 0 -> record.logicalType();
                case 1 -> record.reason();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Memory.ViewBinding record -> switch (field) {
                case 0 -> record.region();
                case 1 -> record.offset();
                case 2 -> record.extent();
                case 3 -> record.codec();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operand.Header record -> switch (field) {
                case 0 -> record.id();
                case 1 -> record.role();
                case 2 -> record.origin();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.Assign record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.destination();
                case 2 -> record.value();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.Branch record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.predicate();
                case 2 -> record.trueDestination();
                case 3 -> record.falseDestination();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.Case record -> switch (field) {
                case 0 -> record.value();
                case 1 -> record.destination();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.CopyBytes record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.destination();
                case 2 -> record.source();
                case 3 -> record.length();
                case 4 -> record.fallback();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.Dispatch record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.selector();
                case 2 -> record.cases();
                case 3 -> record.defaultDestination();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.Halt record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.haltKind();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.HavocMay record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.scope();
                case 2 -> record.reason();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.HavocMust record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.destination();
                case 2 -> record.reason();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.Header record -> switch (field) {
                case 0 -> record.id();
                case 1 -> record.origin();
                case 2 -> record.coverage();
                case 3 -> record.precision();
                case 4 -> record.uncertainties();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.IndirectJump record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.target();
                case 2 -> record.within();
                case 3 -> record.fallback();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.Invoke record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.action();
                case 2 -> record.target();
                case 3 -> record.arguments();
                case 4 -> record.results();
                case 5 -> record.signature();
                case 6 -> record.effectOperands();
                case 7 -> record.effectBound();
                case 8 -> record.outcomes();
                case 9 -> record.contract();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.Jump record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.destination();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.LocalBoundary record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.port();
                case 2 -> record.defaultDestination();
                case 3 -> record.fallback();
                case 4 -> record.resumeKey();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.LocalInvoke record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.entry();
                case 2 -> record.completionPorts();
                case 3 -> record.resume();
                case 4 -> record.fallback();
                case 5 -> record.reentryGuard();
                case 6 -> record.resumeRoutes();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.LocalResume record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.fallback();
                case 2 -> record.resumeKey();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.LocalUnwind record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.count();
                case 2 -> record.destination();
                case 3 -> record.fallback();
                case 4 -> record.all();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.Nop record -> switch (field) {
                case 0 -> record.header();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.Opaque record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.observedKind();
                case 2 -> record.knownOperands();
                case 3 -> record.valueResults();
                case 4 -> record.envelope();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.Raise record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.tag();
                case 2 -> record.values();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.ReentryGuard record -> switch (field) {
                case 0 -> record.activationKey();
                case 1 -> record.destination();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.ResumeRoute record -> switch (field) {
                case 0 -> record.key();
                case 1 -> record.destination();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Operations.Return record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.values();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Origins.Artifact record -> switch (field) {
                case 0 -> record.id();
                case 1 -> record.logicalName();
                case 2 -> record.contentDigest();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Origins.Contractual record -> switch (field) {
                case 0 -> record.id();
                case 1 -> record.authority();
                case 2 -> record.version();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Origins.Derived record -> switch (field) {
                case 0 -> record.id();
                case 1 -> record.inputs();
                case 2 -> record.rule();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Origins.IncludeFrame record -> switch (field) {
                case 0 -> record.including();
                case 1 -> record.included();
                case 2 -> record.requestedName();
                case 3 -> record.site();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Origins.LineColumns record -> switch (field) {
                case 0 -> record.span();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Origins.Offsets record -> switch (field) {
                case 0 -> record.start();
                case 1 -> record.end();
                case 2 -> record.unit();
                case 3 -> record.endExclusive();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Origins.Position record -> switch (field) {
                case 0 -> record.line();
                case 1 -> record.column();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Origins.Span record -> switch (field) {
                case 0 -> record.start();
                case 1 -> record.end();
                case 2 -> record.lineBase();
                case 3 -> record.columnBase();
                case 4 -> record.columnUnit();
                case 5 -> record.endExclusive();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Origins.Unavailable record -> switch (field) {
                case 0 -> record.id();
                case 1 -> record.reason();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Origins.Written record -> switch (field) {
                case 0 -> record.id();
                case 1 -> record.artifact();
                case 2 -> record.location();
                case 3 -> record.includes();
                case 4 -> record.exact();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Places.Choice record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.candidates();
                case 2 -> record.remainder();
                case 3 -> record.typeRef();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Places.ObjectPlace record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.object();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Places.RegionSlice record -> switch (field) {
                case 0 -> record.header();
                case 1 -> record.region();
                case 2 -> record.offset();
                case 3 -> record.length();
                case 4 -> record.codec();
                case 5 -> record.typeRef();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Proofs.CallParameterDomain record -> switch (field) {
                case 0 -> record.invocation();
                case 1 -> record.entry();
                case 2 -> record.position();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Proofs.CallResultDomain record -> switch (field) {
                case 0 -> record.invocation();
                case 1 -> record.entry();
                case 2 -> record.position();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Proofs.CellDomain record -> switch (field) {
                case 0 -> record.cell();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Proofs.DisjointStorage record -> switch (field) {
                case 0 -> record.storage();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Proofs.EntryDomain record -> switch (field) {
                case 0 -> record.entry();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Proofs.ExternalParameterDomain record -> switch (field) {
                case 0 -> record.invocation();
                case 1 -> record.position();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Proofs.ExternalResultDomain record -> switch (field) {
                case 0 -> record.invocation();
                case 1 -> record.position();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Proofs.Intersection record -> switch (field) {
                case 0 -> record.left();
                case 1 -> record.right();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Proofs.InvocationDomain record -> switch (field) {
                case 0 -> record.invocation();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Proofs.ObjectDomain record -> switch (field) {
                case 0 -> record.object();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Proofs.OperandDomain record -> switch (field) {
                case 0 -> record.operand();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Proofs.OperationDomain record -> switch (field) {
                case 0 -> record.operation();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Proofs.ParameterDomain record -> switch (field) {
                case 0 -> record.entry();
                case 1 -> record.position();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Proofs.Premise record -> switch (field) {
                case 0 -> record.id();
                case 1 -> record.authority();
                case 2 -> record.justification();
                case 3 -> record.origin();
                case 4 -> record.assertion();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Proofs.ResultDomain record -> switch (field) {
                case 0 -> record.entry();
                case 1 -> record.position();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Proofs.SameDomain record -> switch (field) {
                case 0 -> record.left();
                case 1 -> record.right();
                case 2 -> record.scope();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Proofs.UnitDomain record -> switch (field) {
                case 0 -> record.unit();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Publication record -> switch (field) {
                case 0 -> record.id();
                case 1 -> record.airVersion();
                case 2 -> record.capabilities();
                case 3 -> record.artifacts();
                case 4 -> record.units();
                case 5 -> record.storage();
                case 6 -> record.resources();
                case 7 -> record.artifactRelations();
                case 8 -> record.origins();
                case 9 -> record.coverage();
                case 10 -> record.uncertainties();
                case 11 -> record.premises();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Scopes.AllControl record -> switch (field) {
                case 0 -> record.publication();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Scopes.AllMemory record -> switch (field) {
                case 0 -> record.publication();
                case 1 -> record.includingEnvironment();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Scopes.ControlUnion record -> switch (field) {
                case 0 -> record.members();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Scopes.EntityScope record -> switch (field) {
                case 0 -> record.entities();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Scopes.LabelsControl record -> switch (field) {
                case 0 -> record.labels();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Scopes.MemoryUnion record -> switch (field) {
                case 0 -> record.members();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Scopes.ObjectsMemory record -> switch (field) {
                case 0 -> record.objects();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Scopes.PublicationScope record -> switch (field) {
                case 0 -> record.publication();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Scopes.ResourceCategories record -> switch (field) {
                case 0 -> record.categories();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Scopes.StorageMemory record -> switch (field) {
                case 0 -> record.storage();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Scopes.UnitControl record -> switch (field) {
                case 0 -> record.unit();
                case 1 -> record.labels();
                case 2 -> record.normalExit();
                case 3 -> record.exceptionalExit();
                case 4 -> record.halt();
                case 5 -> record.diverge();
                case 6 -> record.externalControl();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Scopes.UnitScope record -> switch (field) {
                case 0 -> record.unit();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Scopes.VisibleMemory record -> switch (field) {
                case 0 -> record.unit();
                case 1 -> record.includingExternal();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Scopes.WithinControl record -> switch (field) {
                case 0 -> record.scope();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Scopes.WithinMemory record -> switch (field) {
                case 0 -> record.scope();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case SemanticVersion record -> switch (field) {
                case 0 -> record.major();
                case 1 -> record.minor();
                case 2 -> record.patch();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Sequence record -> switch (field) {
                case 0 -> record.label();
                case 1 -> record.instructions();
                case 2 -> record.terminator();
                case 3 -> record.origin();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Types.ExtensionType record -> switch (field) {
                case 0 -> record.name();
                case 1 -> record.version();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Types.Known record -> switch (field) {
                case 0 -> record.type();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Types.LabelType record -> switch (field) {
                case 0 -> record.unit();
                case 1 -> record.labels();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Types.UnknownType record -> switch (field) {
                case 0 -> record.uncertainty();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Unit record -> switch (field) {
                case 0 -> record.id();
                case 1 -> record.containingUnit();
                case 2 -> record.objects();
                case 3 -> record.visibleObjects();
                case 4 -> record.entries();
                case 5 -> record.sequences();
                case 6 -> record.completionPorts();
                case 7 -> record.body();
                case 8 -> record.bodyUnavailable();
                case 9 -> record.coverage();
                case 10 -> record.origin();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Values.BoolValue record -> switch (field) {
                case 0 -> record.value();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Values.BytesValue record -> switch (field) {
                case 0 -> record.octets();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Values.DecimalValue record -> switch (field) {
                case 0 -> record.coefficient();
                case 1 -> record.scale();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Values.IntValue record -> switch (field) {
                case 0 -> record.value();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Values.LabelValue record -> switch (field) {
                case 0 -> record.label();
                case 1 -> record.domain();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            case Values.TextValue record -> switch (field) {
                case 0 -> record.value();
                default -> throw new IndexOutOfBoundsException("AIR record field " + field);
            };
            default -> throw new IllegalArgumentException("AIR record required");
        };
    }
}
