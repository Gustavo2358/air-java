package io.github.gustavo2358.air.model;


final class Require {
    private Require() {}
    static String text(String s, String name) {
        java.util.Objects.requireNonNull(s, name);
        if (s.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        unicode(s, name);
        return s;
    }
    static String unicode(String s, String name) {
        java.util.Objects.requireNonNull(s, name);
        for (int i=0; i<s.length(); i++) {
            char c=s.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i>=s.length() || !Character.isLowSurrogate(s.charAt(i)))
                    throw new IllegalArgumentException(name+" contains an unpaired surrogate");
            } else if (Character.isLowSurrogate(c))
                throw new IllegalArgumentException(name+" contains an unpaired surrogate");
        }
        return s;
    }
    static void nonNegative(java.math.BigInteger n,String name) {
        if(n.signum()<0) throw new IllegalArgumentException(name+" must be non-negative");
    }
}
