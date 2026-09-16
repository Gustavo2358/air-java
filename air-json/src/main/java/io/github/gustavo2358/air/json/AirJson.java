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
 * Shared codec for analysis-ir-json 1.0.0 / AIR 2.0.0, DRAFT pin 122ce54e.
 * Implements the forms documented in docs/engineering/air-json.md; other forms fail explicitly.
 * Stateless and thread safe. No method exposes facts/bytes on failure. Partial transport is explicitly opt-in.
 */
public final class AirJson {
    /** Operational bounds, not AIR cardinality or integer validity rules. */
    public record Limits(int maximumDocumentBytes, int maximumDepth) {
        public Limits {
            if (maximumDocumentBytes < 1 || maximumDepth < 1)
                throw new IllegalArgumentException("Positive operational limits required");
        }
        public static Limits defaults() { return new Limits(Integer.MAX_VALUE, Integer.MAX_VALUE); }
    }
    private final Limits limits;
    private final ValidationOptions validationOptions;
    public AirJson() { this(Limits.defaults(), ValidationOptions.defaults()); }
    public AirJson(Limits limits, ValidationOptions validationOptions) {
        this.limits = Objects.requireNonNull(limits);
        this.validationOptions = Objects.requireNonNull(validationOptions);
    }
    /** Canonical UTF-8 bytes after structural validation. Outstanding semantic obligations are not discharged. */
    public byte[] encode(Publication publication) { return encode(publication,false).bytes(); }
    /** Canonical bytes and unchanged validation status; the byte array is defensively owned. */
    public record PartialOutput(byte[] bytes,ValidationResult validation) {
        public PartialOutput { bytes=bytes.clone();Objects.requireNonNull(validation); }
        @Override public byte[] bytes() { return bytes.clone(); }
    }
    public PartialOutput encodeForPartialAnalysis(Publication publication) { return encode(publication,true); }
    private PartialOutput encode(Publication publication,boolean partialAnalysis) {
        Objects.requireNonNull(publication, "publication");
        if (!publication.airVersion().equals(SemanticVersion.AIR_2_0_0))
            throw new AirJsonException(VERSION_MISMATCH, "$.airVersion", "Expected AIR 2.0.0");
        // Map coverage first so an unimplemented valid form is never blamed on the Validator.
        Json.Value wire = new BindingWriter().envelope(publication);
        var validation=validate(publication,partialAnalysis);
        return new PartialOutput(Json.write(wire, limits),validation);
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
        Objects.requireNonNull(bytes, "bytes");
        Json.Value wire = Json.parse(bytes, limits);
        Publication publication = new BindingReader().envelope(wire);
        // Validate capability use only after the complete typed payload is available.
        var names = io.github.gustavo2358.air.model.NamePolicies.extensions(publication);
        for (var capabilities : java.util.List.of(publication.capabilities().required(), publication.capabilities().provided()))
            for (var capability : capabilities)
                if (!java.util.List.of(io.github.gustavo2358.air.model.Capabilities.MEMORY_REGIONS, io.github.gustavo2358.air.model.Capabilities.IBM1047, io.github.gustavo2358.air.model.Capabilities.ENTRY_POSSIBILITIES, io.github.gustavo2358.air.model.Capabilities.ENTRY_POSSIBILITIES_V2, io.github.gustavo2358.air.model.Capabilities.TARGET_POSSIBILITIES).contains(capability) && !names.contains(capability))
                    throw new AirJsonException(UNSUPPORTED_CAPABILITY,"$.publication.capabilities","Capability outside implemented transport profile");
        return new PartialInput(publication,validate(publication,partialAnalysis));
    }
    private void validate(Publication publication) { validate(publication,false); }
    private ValidationResult validate(Publication publication,boolean partialAnalysis) {
        ValidationResult result = AirValidator.validate(publication, validationOptions);
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
