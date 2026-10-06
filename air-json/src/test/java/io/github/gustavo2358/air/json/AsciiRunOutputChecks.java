package io.github.gustavo2358.air.json;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Literal physical bytes and maximal scalar run, independent of the writer. */
final class AsciiRunOutputChecks {
    static void run() {
        try {checked();} catch(IOException failure){throw new AssertionError(failure);}
    }
    private static void checked() throws IOException {
        for(int stop=0;stop<160;stop++) {
            String text="AB ~"+(char)stop+"tail";int expected=0;
            while(expected<text.length()) {int code=text.codePointAt(expected);if(code<32||code>127||code==34||code==92)break;expected+=Character.charCount(code);}
            if(Json.asciiRunEnd(text,0)!=expected)throw new AssertionError("ASCII maximality: "+stop);
        }
        for(int length:new int[]{0,1,8191,8192,8193,16383,16384,16385}) {
            String prefix="a".repeat(length),suffix="b".repeat(length);
            String text=prefix+"\n\"\\é𝄞\u0000"+suffix;
            byte[] expected=("\""+prefix+"\\n\\\"\\\\é𝄞\\u0000"+suffix+"\"").getBytes(StandardCharsets.UTF_8);
            var value=new Json.Text(text);var limits=AirJson.Limits.defaults();
            if(!Arrays.equals(expected,Json.write(value,limits)))throw new AssertionError("literal byte writer: "+length);
            var received=new ByteArrayOutputStream();var output=new OutputStream(){
                @Override public void write(int value){throw new AssertionError("unbuffered byte");}
                @Override public void write(byte[] bytes,int start,int count){if(count>8192)throw new AssertionError("buffer exceeds bounded owner");received.write(bytes,start,count);}
                @Override public void close(){throw new AssertionError("caller closed");}
            };
            Json.measure(value,limits);Json.emit(value,limits,output);
            if(!Arrays.equals(expected,received.toByteArray()))throw new AssertionError("literal stream writer: "+length);
            try{Json.measure(value,new AirJson.Limits(expected.length-1,Integer.MAX_VALUE));throw new AssertionError("byte budget");}
            catch(AirJsonException failure){if(failure.code()!=AirJsonException.Code.RESOURCE_LIMIT)throw failure;}
        }
    }
}
