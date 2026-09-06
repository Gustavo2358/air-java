package io.github.gustavo2358.air.model;


/** Version of a semantic contract, independent from the Maven library version. */
public record SemanticVersion(int major, int minor, int patch) {
    public static final SemanticVersion AIR_2_0_0 = new SemanticVersion(2,0,0);
    public SemanticVersion {
        if (major<0 || minor<0 || patch<0) throw new IllegalArgumentException("negative version");
    }
    @Override public String toString() { return major+"."+minor+"."+patch; }
    public static SemanticVersion parse(String text) {
        java.util.Objects.requireNonNull(text, "text");
        if (!text.matches("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)"))
            throw new IllegalArgumentException("expected major.minor.patch: "+text);
        String[] p=text.split("\\.");
        return new SemanticVersion(Integer.parseInt(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2]));
    }
}
