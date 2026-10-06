package io.github.gustavo2358.air.json;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.validation.*;

/** Independent literal golden and output/admission ownership laws. */
final class StreamingOutputChecks {
    private StreamingOutputChecks() { }
    static void run() {
        try{checked();}catch(IOException failure){throw new AssertionError(failure);}
    }
    private static void checked()throws IOException {
        var codec=new AirJson();var publication=GobackOracle.publication();
        var golden=Files.readAllBytes(Path.of("src/test/resources/goback.canonical.json"));
        var output=new ByteArrayOutputStream(){@Override public void close(){throw new AssertionError("caller stream closed");}};
        codec.write(publication,output);
        if(!Arrays.equals(golden,output.toByteArray()))throw new AssertionError("stream differs from independent golden");
        var partial=new ByteArrayOutputStream();var validation=codec.writeForPartialAnalysis(publication,partial);
        if(!validation.isStructurallyValid()||!Arrays.equals(golden,partial.toByteArray()))throw new AssertionError("partial stream loses validation or bytes");
        var budget=new AirJson(new AirJson.Limits(golden.length-1,Integer.MAX_VALUE),ValidationOptions.defaults());
        var rejected=new ByteArrayOutputStream();
        try{budget.write(publication,rejected);throw new AssertionError("byte limit ignored");}
        catch(AirJsonException expected){if(expected.code()!=AirJsonException.Code.RESOURCE_LIMIT)throw expected;}
        if(rejected.size()!=0)throw new AssertionError("admission failure exposed bytes");
        // All admitted full canonical fixtures also exercise the streaming route.
        try(var files=Files.list(Path.of("src/test/resources"))) {
            for(var file:files.filter(f->f.getFileName().toString().endsWith(".canonical.json")).sorted().toList()) {
                var bytes=Files.readAllBytes(file);Publication admitted;
                try{admitted=codec.decodeForPartialAnalysis(bytes).publication();}
                catch(AirJsonException unsupportedInput){continue;}
                var expected=codec.encodeForPartialAnalysis(admitted);var streamed=new ByteArrayOutputStream();
                var actual=codec.writeForPartialAnalysis(admitted,streamed);
                if(!Arrays.equals(expected.bytes(),streamed.toByteArray())||!expected.validation().equals(actual))throw new AssertionError("complete streaming fixture differs: "+file);
            }
        }
        boolean failed=false;
        try{codec.write(publication,new OutputStream(){@Override public void write(int b)throws IOException{throw new IOException("destination failure");}@Override public void close(){throw new AssertionError("failed caller closed");}});}
        catch(IOException expected){failed=true;}
        if(!failed)throw new AssertionError("destination failure disappeared");
    }
}
