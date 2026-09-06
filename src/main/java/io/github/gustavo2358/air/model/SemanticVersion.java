package io.github.gustavo2358.air.model;

import java.math.BigInteger;
import java.util.Objects;

/** Version of a semantic contract, independent from the Maven library version. */
public record SemanticVersion(BigInteger major, BigInteger minor, BigInteger patch) {
    public static final SemanticVersion AIR_2_0_0 = new SemanticVersion(
            BigInteger.TWO, BigInteger.ZERO, BigInteger.ZERO);

    public SemanticVersion {
        major = Objects.requireNonNull(major, "major");
        minor = Objects.requireNonNull(minor, "minor");
        patch = Objects.requireNonNull(patch, "patch");
        if (major.signum()<0 || minor.signum()<0 || patch.signum()<0)
            throw new IllegalArgumentException("negative version");
    }

    @Override public String toString() { return major+"."+minor+"."+patch; }

    public static SemanticVersion parse(String text) {
        Objects.requireNonNull(text, "text");
        if (!text.matches("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)"))
            throw new IllegalArgumentException("expected major.minor.patch: "+text);
        String[] p=text.split("\\.");
        return new SemanticVersion(new BigInteger(p[0]),new BigInteger(p[1]),new BigInteger(p[2]));
    }
}
