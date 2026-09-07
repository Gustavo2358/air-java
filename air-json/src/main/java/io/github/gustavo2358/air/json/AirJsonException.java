package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.validation.ValidationIssue;
import java.util.List;
import java.util.Objects;

/** An indivisible codec failure. No partial Publication or output is exposed. */
public final class AirJsonException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public enum Code {
        INPUT_ERROR, VERSION_MISMATCH, INVALID_IR, UNSUPPORTED_CAPABILITY,
        INCOMPLETE_VALIDATION, IMPLEMENTATION_LIMIT
    }
    private final Code code;
    private final String path;
    // Java object serialization is not a transport API for this domain exception.
    @SuppressWarnings("serial")
    private final List<ValidationIssue> issues;

    AirJsonException(Code code, String path, String message) { this(code, path, message, List.of()); }
    AirJsonException(Code code, String path, String message, List<ValidationIssue> issues) {
        super(message);
        this.code = Objects.requireNonNull(code);
        this.path = Objects.requireNonNull(path);
        this.issues = List.copyOf(issues);
    }
    public Code code() { return code; }
    /** JSON path for transport/local shape failures; $ for publication validation. */
    public String path() { return path; }
    /** AIR diagnostics: explicit local rule/site via path(), or unchanged AirValidator issues. */
    public List<ValidationIssue> issues() { return issues; }
}
