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
 * Stateless and thread safe. Neither method exposes partial facts/bytes on failure.
 */
public final class AirJson {
    /** Operational bounds, not AIR cardinality or integer validity rules. */
    public record Limits(int maximumDocumentBytes, int maximumDepth) {
        public Limits {
            if (maximumDocumentBytes < 1 || maximumDepth < 1 || maximumDepth > 256)
                throw new IllegalArgumentException("Positive limits required; maximumDepth <= 256");
        }
        public static Limits defaults() { return new Limits(16 * 1024 * 1024, 128); }
    }
    private final Limits limits;
    private final ValidationOptions validationOptions;
    public AirJson() { this(Limits.defaults(), ValidationOptions.defaults()); }
    public AirJson(Limits limits, ValidationOptions validationOptions) {
        this.limits = Objects.requireNonNull(limits);
        this.validationOptions = Objects.requireNonNull(validationOptions);
    }
    /** Canonical UTF-8 bytes, without BOM or final newline, after structural AIR validation. */
    public byte[] encode(Publication publication) {
        Objects.requireNonNull(publication, "publication");
        if (!publication.airVersion().equals(SemanticVersion.AIR_2_0_0))
            throw new AirJsonException(VERSION_MISMATCH, "$.airVersion", "Expected AIR 2.0.0");
        // Map coverage first so an unimplemented valid form is never blamed on the Validator.
        Json.Value wire = new BindingWriter().envelope(publication);
        validate(publication);
        return Json.write(wire, limits);
    }
    /** Decode exact facts, then check AIR closure. Throws a typed failure, never a partial Publication. */
    public Publication decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        Json.Value wire = Json.parse(bytes, limits);
        Publication publication = new BindingReader().envelope(wire);
        validate(publication);
        return publication;
    }
    private void validate(Publication publication) {
        ValidationResult result = AirValidator.validate(publication, validationOptions);
        if (result.status() == ValidationResult.Status.INVALID_IR)
            throw new AirJsonException(INVALID_IR, "$", "AIR structural validation failed", result.issues());
        if (result.issues().stream().anyMatch(i -> i.kind() == ValidationIssue.Kind.UNSUPPORTED_CAPABILITY))
            throw new AirJsonException(UNSUPPORTED_CAPABILITY, "$", "AIR capability not supported", result.issues());
        if (result.status() == ValidationResult.Status.INCOMPLETE_VALIDATION
                || result.issues().stream().anyMatch(i -> i.kind() == ValidationIssue.Kind.SEMANTIC_OBLIGATION))
            throw new AirJsonException(INCOMPLETE_VALIDATION, "$", "AIR validation not complete", result.issues());
    }
}
