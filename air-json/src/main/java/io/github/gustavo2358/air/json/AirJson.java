package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.Publication;
import io.github.gustavo2358.air.model.SemanticVersion;
import io.github.gustavo2358.air.validation.AirValidator;
import io.github.gustavo2358.air.validation.ValidationIssue;
import io.github.gustavo2358.air.validation.ValidationOptions;
import io.github.gustavo2358.air.validation.ValidationResult;
import java.util.Objects;
import static io.github.gustavo2358.air.json.AirJsonException.Code.*;

/**
 * Shared codec for analysis-ir-json 1.0.0 / AIR 2.0.0, DRAFT pin 2c7f31f1.
 * Implements the forms documented in docs/engineering/air-json.md; other forms fail explicitly.
 * Stateless and thread safe. Admission failures expose no facts/bytes; stream I/O failure may leave a prefix. Partial transport is opt-in.
 */
public final class AirJson {
    /** Managed physical JSON staging, not ownership/accounting of the returned resident model. */
    public interface InputStorage extends AutoCloseable {
        // Frozen token schema: eight NODES words per positive token, (token-1)*8.
        // 0 kind(OBJECT0/ARRAY1/TEXT2/TRUE3/FALSE4/NULL5),1 property-name text token,
        // 3 child count,4 decoded UTF-16 start,5 UTF-16 count; other words reserved0.
        // CHARACTERS packs four UTF-16 units per word, least significant unit first.
        // FRAMES is a managed transient two-word (container,state) column per depth.

        enum Column { NODES, CHARACTERS, FRAMES }
        long get(Column column,long index);
        void set(Column column,long index,long value);
        /** Exact append-only (parent,ordinal) index; random reads must avoid sibling-prefix replay. */
        void child(long parent,long ordinal,long child);
        long child(long parent,long ordinal);
        /** Exact decoded UTF-16 property equality within an object, including escaped spellings. */
        boolean firstField(long parent,long nameToken);
        io.github.gustavo2358.air.model.AirSnapshotBuilder.Lease claim(long bytes);
        @Override void close();
    }
    /** Operational bounds, not AIR cardinality or integer validity rules. */
    public record Limits(int maximumDocumentBytes, int maximumDepth) {
        public Limits {
            if (maximumDocumentBytes < 1 || maximumDepth < 1)
                throw new IllegalArgumentException("Positive operational limits required");
        }
        public static Limits defaults() { return new Limits(Integer.MAX_VALUE, Integer.MAX_VALUE); }
    }
    /** Scheduling only: identical facts, array order and failure precedence for every setting. */
    public record DecodeOptions(int bindingParallelism) {
        public DecodeOptions {
            if (bindingParallelism < 1) throw new IllegalArgumentException("Positive binding parallelism required");
        }
        public static DecodeOptions defaults() {
            return new DecodeOptions(Math.min(4, Runtime.getRuntime().availableProcessors()));
        }
    }
    private final DecodeOptions decodeOptions;
    private final Limits limits;
    private final ValidationOptions validationOptions;
    public AirJson() { this(Limits.defaults(), ValidationOptions.defaults()); }
    public AirJson(Limits limits, ValidationOptions validationOptions) {
        this(limits, validationOptions, DecodeOptions.defaults());
    }
    public AirJson(Limits limits, ValidationOptions validationOptions, DecodeOptions decodeOptions) {
        this.decodeOptions = Objects.requireNonNull(decodeOptions);
        this.limits = Objects.requireNonNull(limits);
        this.validationOptions = Objects.requireNonNull(validationOptions);
    }
    /** Canonical UTF-8 bytes after structural validation. Outstanding semantic obligations are not discharged. */
    public byte[] encode(Publication publication) { return encoded(publication,false).bytes(); }
    /** Canonical bytes and unchanged validation status; the byte array is defensively owned. */
    public record PartialOutput(byte[] bytes,ValidationResult validation) {
        public PartialOutput { bytes=bytes.clone();Objects.requireNonNull(validation); }
        @Override public byte[] bytes() { return bytes.clone(); }
    }
    public PartialOutput encodeForPartialAnalysis(Publication publication) { return encode(publication,true); }
    private record Encoded(byte[] bytes,ValidationResult validation) { }
    private PartialOutput encode(Publication publication,boolean partialAnalysis) {
        var result=encoded(publication,partialAnalysis);
        return new PartialOutput(result.bytes(),result.validation());
    }
    private Encoded encoded(Publication publication,boolean partialAnalysis) {
        Objects.requireNonNull(publication, "publication");
        if (!publication.airVersion().equals(SemanticVersion.AIR_2_0_0))
            throw new AirJsonException(VERSION_MISMATCH, "$.airVersion", "Expected AIR 2.0.0");
        // Map coverage first so an unimplemented valid form is never blamed on the Validator.
        Json.Value wire = new BindingWriter().envelope(publication);
        var validation=validate(publication,partialAnalysis);
        return new Encoded(Json.write(wire, limits),validation);
    }
    /** Prepared immutable mapping: no emitted bytes or whole publication JSON tree. */
    public static final class PreparedOutput {
        private final Json.Value wire;private final Limits limits;private final ValidationResult validation;
        private PreparedOutput(Json.Value wire,Limits limits,ValidationResult validation){this.wire=wire;this.limits=limits;this.validation=validation;}
        public ValidationResult validation(){return validation;}
        /** I/O failure may leave a prefix; file callers must stage and atomically publish. Caller owns the stream. */
        public void writeTo(java.io.OutputStream output)throws java.io.IOException {Json.emit(wire,limits,Objects.requireNonNull(output,"output"));}
    }
    public PreparedOutput prepareWrite(Publication publication){return prepareWrite(publication,false);}
    public PreparedOutput prepareWriteForPartialAnalysis(Publication publication){return prepareWrite(publication,true);}
    private PreparedOutput prepareWrite(Publication publication,boolean partialAnalysis) {
        Objects.requireNonNull(publication,"publication");
        if(!publication.airVersion().equals(SemanticVersion.AIR_2_0_0))throw new AirJsonException(VERSION_MISMATCH,"$.airVersion","Expected AIR 2.0.0");
        // Same typed coverage walk/order as the eager writer, with no retained fact tree.
        new BindingWriter(true,false).envelope(publication);
        var validation=validate(publication,partialAnalysis);
        var wire=new BindingWriter(false,true).envelope(publication);
        Json.measure(wire,limits);
        return new PreparedOutput(wire,limits,validation);
    }
    /** Checks admission and physical budgets before emitting bytes. Caller owns the stream. */
    public void write(Publication publication,java.io.OutputStream output)throws java.io.IOException {
        Objects.requireNonNull(output,"output");prepareWrite(publication).writeTo(output);
    }
    /** Opt-in partial transport with unchanged structural validation status and caller ownership. */
    public ValidationResult writeForPartialAnalysis(Publication publication,java.io.OutputStream output)throws java.io.IOException {
        Objects.requireNonNull(output,"output");var prepared=prepareWriteForPartialAnalysis(publication);prepared.writeTo(output);return prepared.validation();
    }
    /** Decode exact facts, then check AIR closure. Throws a typed failure, never a partial Publication. */
    public Publication decode(byte[] bytes) { return decode(bytes,false).publication(); }
    /** Original decoded facts and their actual validation result; never a validity certificate. */
    public record PartialInput(Publication publication, ValidationResult validation) {
        public PartialInput { Objects.requireNonNull(publication); Objects.requireNonNull(validation); }
    }
    /** Opt-in AIR 08 §9 admission. Strict decode/encode behavior and wire format remain unchanged. */
    public PartialInput decodeForPartialAnalysis(byte[] bytes) { return decode(bytes,true); }
    private PartialInput decode(byte[] bytes,boolean partialAnalysis) {
        var checked = decodeChecked(bytes, partialAnalysis);
        return new PartialInput(checked.publication(), checked.result());
    }
    /** Decode and retain the validation run for downstream consumers of the same snapshot. */
    public AirValidator.CheckedPublication decodeChecked(byte[] bytes) { return decodeChecked(bytes, false); }
    public AirValidator.CheckedPublication decodeCheckedForPartialAnalysis(byte[] bytes) { return decodeChecked(bytes, true); }
    private AirValidator.CheckedPublication decodeChecked(byte[] bytes,boolean partialAnalysis) {
        Objects.requireNonNull(bytes, "bytes");
        Json.Value wire = Utf8Input.parse(bytes, limits);
        Publication publication;
        try (var blocks = new OrderedBlocks(decodeOptions.bindingParallelism())) {
            publication = new BindingReader(blocks).envelope(wire);
        }
        return checkBound(publication,partialAnalysis);
    }
    /** Reads incrementally into managed staging; borrows input and transfers/closes storage. */
    public AirValidator.CheckedPublication decodeChecked(java.io.InputStream input,InputStorage storage)throws java.io.IOException {
        return decodeChecked(input,storage,false);
    }
    public AirValidator.CheckedPublication decodeCheckedForPartialAnalysis(java.io.InputStream input,InputStorage storage)throws java.io.IOException {
        return decodeChecked(input,storage,true);
    }
    private AirValidator.CheckedPublication decodeChecked(java.io.InputStream input,InputStorage storage,boolean partialAnalysis)throws java.io.IOException {
        try(var staged=PagedJson.parse(input,limits,storage);var blocks=new OrderedBlocks(decodeOptions.bindingParallelism())) {
            return checkBound(new BindingReader(blocks).envelope(staged.node(staged.root())),partialAnalysis);
        }
    }
    private AirValidator.CheckedPublication checkBound(Publication publication,boolean partialAnalysis) {
        // Validate capability use only after the complete typed payload is available.
        var names = io.github.gustavo2358.air.model.NamePolicies.extensions(publication);
        for (var capabilities : java.util.List.of(publication.capabilities().required(), publication.capabilities().provided()))
            for (var capability : capabilities)
                if (!java.util.List.of(io.github.gustavo2358.air.model.Capabilities.LOCAL_CONTROL, io.github.gustavo2358.air.model.Capabilities.LOCAL_REENTRY_GUARD, io.github.gustavo2358.air.model.Capabilities.LOCAL_RESUME_ROUTES, io.github.gustavo2358.air.model.Capabilities.LOCAL_BOUNDARY_ROUTES, io.github.gustavo2358.air.model.Capabilities.LOCAL_UNWIND_ALL, io.github.gustavo2358.air.model.Capabilities.MEMORY_REGIONS, io.github.gustavo2358.air.model.Capabilities.IBM1047, io.github.gustavo2358.air.model.Capabilities.ENTRY_POSSIBILITIES, io.github.gustavo2358.air.model.Capabilities.ENTRY_POSSIBILITIES_V2, io.github.gustavo2358.air.model.Capabilities.TARGET_POSSIBILITIES, io.github.gustavo2358.air.model.Capabilities.RESOURCE_BINDINGS).contains(capability) && !names.contains(capability))
                    throw new AirJsonException(UNSUPPORTED_CAPABILITY,"$.publication.capabilities","Capability outside implemented transport profile");
        var checked = AirValidator.check(publication, validationOptions);
        admit(checked.result(), partialAnalysis);
        return checked;
    }
    private void validate(Publication publication) { validate(publication,false); }
    private ValidationResult validate(Publication publication,boolean partialAnalysis) {
        return admit(AirValidator.validate(publication, validationOptions), partialAnalysis);
    }
    private ValidationResult admit(ValidationResult result, boolean partialAnalysis) {
        if (result.hasIssues(ValidationIssue.Kind.RESOURCE_LIMIT))
            throw new AirJsonException(RESOURCE_LIMIT, "$", "AIR validation operational budget exhausted", result);
        if (result.status() == ValidationResult.Status.INVALID_IR)
            throw new AirJsonException(INVALID_IR, "$", "AIR structural validation failed", result);
        if (result.hasIssues(ValidationIssue.Kind.UNSUPPORTED_CAPABILITY))
            throw new AirJsonException(UNSUPPORTED_CAPABILITY, "$", "AIR capability not supported", result);
        // SEMANTIC_OBLIGATION alone does not block transport. Totals include unretained diagnostics;
        // Strict transport blocks limits/incomplete traversal; partial admission additionally requires a complete operation scope.
        if (result.status() == ValidationResult.Status.INCOMPLETE_VALIDATION
                && !(partialAnalysis && result.unprovedOperationPreconditions().isPresent()))
            throw new AirJsonException(INCOMPLETE_VALIDATION, "$", "AIR validation not complete", result);
        return result;
    }
}
