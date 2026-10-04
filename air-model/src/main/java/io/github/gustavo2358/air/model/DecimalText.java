package io.github.gustavo2358.air.model;
import java.math.BigInteger;
import java.util.*;
/** Typed, compressed decimal formatting. No locale, source-language spelling or codec. */
public final class DecimalText {
    private DecimalText() { }
    public enum Kind { DIGITS, SUPPRESS_SPACE, SUPPRESS_STAR, INSERT, RADIX, SIGN, FLOAT_SIGN }
    public record Part(Kind kind,BigInteger count,String text,String negative) {
        public Part {
            Objects.requireNonNull(kind);Objects.requireNonNull(count);
            text=Require.unicode(text,"text");negative=Require.unicode(negative,"negative");
            if(count.signum()<=0)throw new IllegalArgumentException("positive format count");
            int size=text.codePointCount(0,text.length()),other=negative.codePointCount(0,negative.length());
            boolean valid=switch(kind) {
                case DIGITS,SUPPRESS_SPACE,SUPPRESS_STAR -> size==0&&other==0;
                case INSERT -> size>0&&other==0;
                case RADIX -> count.equals(BigInteger.ONE)&&size==1&&other==0;
                case SIGN -> count.equals(BigInteger.ONE)&&size>0&&size==other;
                case FLOAT_SIGN -> count.equals(BigInteger.ONE)&&size==1&&other==1;
            };
            if(!valid)throw new IllegalArgumentException("incoherent decimal format segment");
        }
    }
    public record Shape(BigInteger digits,BigInteger scale,BigInteger extent) { }
    public static Shape describe(List<Part> parts) {
        Objects.requireNonNull(parts);BigInteger digits=BigInteger.ZERO,scale=BigInteger.ZERO,extent=BigInteger.ZERO;
        boolean point=false,star=false,space=false,floating=false;
        for(var p:parts) {
            Objects.requireNonNull(p);
            boolean digit=p.kind()==Kind.DIGITS||p.kind()==Kind.SUPPRESS_SPACE||p.kind()==Kind.SUPPRESS_STAR;
            if(digit){digits=digits.add(p.count());if(point)scale=scale.add(p.count());}
            if(p.kind()==Kind.RADIX){if(point)throw new IllegalArgumentException("duplicate decimal radix");point=true;}
            if(p.kind()==Kind.FLOAT_SIGN){if(floating)throw new IllegalArgumentException("duplicate floating sign");floating=true;}
            star|=p.kind()==Kind.SUPPRESS_STAR;space|=p.kind()==Kind.SUPPRESS_SPACE;
            extent=extent.add(p.count().multiply(BigInteger.valueOf(digit?1:p.text().codePointCount(0,p.text().length()))));
        }
        if(digits.signum()<=0||star&&(space||floating))throw new IllegalArgumentException("incoherent decimal suppression");
        return new Shape(digits,scale,extent);
    }
}
