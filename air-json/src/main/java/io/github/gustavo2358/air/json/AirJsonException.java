package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.validation.ValidationIssue;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import io.github.gustavo2358.air.validation.ValidationResult;

/** An indivisible codec failure. No partial Publication or output is exposed. */
public final class AirJsonException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public enum Code {
        INPUT_ERROR, VERSION_MISMATCH, INVALID_IR, UNSUPPORTED_CAPABILITY,
        INCOMPLETE_VALIDATION, IMPLEMENTATION_LIMIT, RESOURCE_LIMIT
    }
    private final Code code;
    private final String path;
    // Java object serialization is not a transport API for this domain exception.
    @SuppressWarnings("serial")
    private final List<ValidationIssue> issues;

    @SuppressWarnings("serial")
    private final Optional<ValidationResult> validationResult;

    AirJsonException(Code code, String path, String message) { this(code, path, message, List.of()); }
    AirJsonException(Code code, String path, String message, List<ValidationIssue> issues) {
        this(code,path,message,issues,Optional.empty());
    }
    AirJsonException(Code code, String path, String message, ValidationResult result) {
        this(code,path,message,result.issues(),Optional.of(result));
    }
    private AirJsonException(Code code, String path, String message, List<ValidationIssue> issues,
                             Optional<ValidationResult> result) {
        super(message);
        this.code = Objects.requireNonNull(code);
        this.path = Objects.requireNonNull(path);
        this.issues = List.copyOf(issues);
        this.validationResult = result;
    }
    /** Complete classification/counters, including diagnostics omitted by retention, when validation ran. */
    public Optional<ValidationResult> validationResult() { return validationResult; }
    public Code code() { return code; }
    /** JSON path for transport/local shape failures; $ for publication validation. */
    public String path() { return path; }
    /** AIR diagnostics: explicit local rule/site via path(), or unchanged AirValidator issues. */
    public List<ValidationIssue> issues() { return issues; }
}
