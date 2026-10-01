package io.github.gustavo2358.air.json;

import io.github.gustavo2358.air.model.Ids.ArtifactId;
import io.github.gustavo2358.air.model.Origins.Artifact;
import io.github.gustavo2358.air.model.Publication;
import io.github.gustavo2358.air.validation.ValidationOptions;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

final class DecodeSchedulingChecks {
    private DecodeSchedulingChecks() {}
    static void run() {
        try { checks(); } catch (Exception failure) { throw new AssertionError(failure); }
    }
    private static void checks() throws Exception {
        try { new AirJson.DecodeOptions(0); throw new AssertionError("zero workers"); }
        catch (IllegalArgumentException expected) { /* operational setting */ }
        orderedBlocks();
        var base = GobackOracle.publication();
        var artifacts = new ArrayList<>(base.artifacts());
        for (int i = 0; i < 2048; i++)
            artifacts.add(new Artifact(new ArtifactId(base.id(), "parallel-" + i), "file/Ω😀/" + i, Optional.empty()));
        var publication = new Publication(base.id(), base.airVersion(), base.capabilities(), artifacts,
                base.units(), base.storage(), base.resources(), base.artifactRelations(), base.origins(),
                base.coverage(), base.uncertainties(), base.premises());
        byte[] bytes = new AirJson().encode(publication);
        var sequential = codec(1).decodeForPartialAnalysis(bytes);
        for (int workers : List.of(1, 2, 4, 6)) {
            var codec = codec(workers);
            var result = codec.decodeForPartialAnalysis(bytes);
            require(publication.equals(result.publication()), "publication order/facts");
            require(sequential.validation().equals(result.validation()), "validation changed");
            require(Arrays.equals(bytes, codec.encode(result.publication())), "canonical bytes changed");
        }
        // One reusable codec must isolate mutable capability state and per-call worker ownership.
        var shared = codec(4);
        var withResources = ResourceBindingOracle.publication("A6");
        byte[] resourceBytes = shared.encode(withResources);
        try (var calls = Executors.newFixedThreadPool(3)) {
            var futures = new ArrayList<java.util.concurrent.Future<Publication>>();
            for (int i = 0; i < 6; i++) {
                futures.add(calls.submit(() -> shared.decode(bytes)));
                futures.add(calls.submit(() -> shared.decode(resourceBytes)));
            }
            for (int i = 0; i < futures.size(); i++) require(
                    (i % 2 == 0 ? publication : withResources).equals(futures.get(i).get()), "concurrent decode");
        }
        String wire = new String(bytes, StandardCharsets.UTF_8);
        byte[] bad = wire.replace("\"logicalName\":\"file/Ω😀/0\"", "\"logicalName\":null")
                .replace("\"logicalName\":\"file/Ω😀/1024\"", "\"logicalName\":null").getBytes(StandardCharsets.UTF_8);
        require(!Arrays.equals(bytes, bad), "negative fixture mutation");
        AirJsonException first = failure(codec(1), bad);
        require(first.path().equals("$.publication.artifacts[2].logicalName"), "lazy indexed diagnostic path");
        for (int i = 0; i < 8; i++) {
            var other = failure(codec(4), bad);
            require(first.code() == other.code() && first.path().equals(other.path())
                    && first.getMessage().equals(other.getMessage()) && first.issues().equals(other.issues()),
                    "parallel failure precedence");
        }
    }
    private static AirJson codec(int workers) {
        return new AirJson(AirJson.Limits.defaults(), ValidationOptions.defaults(), new AirJson.DecodeOptions(workers));
    }
    private static AirJsonException failure(AirJson codec, byte[] bytes) {
        try { codec.decode(bytes); throw new AssertionError("expected failure"); }
        catch (AirJsonException expected) { return expected; }
    }
    private static void orderedBlocks() {
        Set<String> threads = ConcurrentHashMap.newKeySet();
        try (var blocks = new OrderedBlocks(4)) {
            var result = blocks.map(4096, i -> {
                threads.add(Thread.currentThread().getName());
                if (i == 0) require(blocks.map(2048, j -> j).get(2047) == 2047, "nested mapping");
                return i;
            });
            for (int i = 0; i < result.size(); i++) require(result.get(i) == i, "encounter order");
            require(threads.size() > 1, "parallel path not exercised");
            try { result.set(0, 3); throw new AssertionError("mutable result"); }
            catch (UnsupportedOperationException expected) { /* immutable model inventory */ }
        }
        var later = new CountDownLatch(1);
        var first = new IllegalArgumentException("first input failure");
        try (var blocks = new OrderedBlocks(4)) {
            try {
                blocks.map(2048, i -> {
                    if (i == 0) {
                        try { require(later.await(10, TimeUnit.SECONDS), "later block did not run"); }
                        catch (InterruptedException e) { throw new AssertionError(e); }
                        throw first;
                    }
                    if (i == 256) { later.countDown(); throw new IllegalStateException("later failure"); }
                    return i;
                });
                throw new AssertionError("expected ordered failure");
            } catch (IllegalArgumentException failure) { require(failure == first, "exception identity/order"); }
        }
        Thread.currentThread().interrupt();
        try (var blocks = new OrderedBlocks(2)) {
            require(blocks.map(2048, i -> i).size() == 2048, "interrupted caller result");
        } finally { require(Thread.interrupted(), "interrupt flag lost"); }
    }
    private static void require(boolean condition, String detail) {
        if (!condition) throw new AssertionError(detail);
    }
}
