package io.github.gustavo2358.air.json;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntFunction;

/** One decode owns its workers; nested inventories stay on their current worker. */
final class OrderedBlocks implements AutoCloseable {
    private static final int MINIMUM_BLOCK_SIZE = 256;
    private static final int PARALLEL_THRESHOLD = 1024;
    private final int parallelism;
    private final ThreadLocal<Boolean> worker = new ThreadLocal<>();
    private ExecutorService executor;

    OrderedBlocks(int parallelism) { this.parallelism = parallelism; }

    <T> List<T> map(int size, IntFunction<T> mapper) {
        if (parallelism == 1 || size < PARALLEL_THRESHOLD || Boolean.TRUE.equals(worker.get()))
            return range(0, size, mapper);
        if (executor == null) executor = Executors.newFixedThreadPool(parallelism,
                Thread.ofPlatform().name("air-json-binding-", 0).factory());
        int count = (int) Math.min((long) parallelism * 4, size / MINIMUM_BLOCK_SIZE);
        int width = (size - 1) / count + 1;
        var tasks = new ArrayList<Future<List<T>>>(count);
        for (int start = 0; start < size;) {
            int first = start, end = (int) Math.min((long) start + width, size);
            tasks.add(executor.submit(() -> {
                worker.set(true);
                try { return range(first, end, mapper); }
                finally { worker.remove(); }
            }));
            start = end;
        }
        var result = new ArrayList<T>(size);
        // Observe futures in input order, preserving the first failure of sequential traversal.
        boolean interrupted = false;
        try {
            for (var task : tasks) {
                while (true) {
                    try { result.addAll(task.get()); break; }
                    catch (InterruptedException e) { interrupted = true; }
                    catch (ExecutionException e) {
                        if (e.getCause() instanceof RuntimeException failure) throw failure;
                        if (e.getCause() instanceof Error failure) throw failure;
                        throw new IllegalStateException(e.getCause());
                    }
                }
            }
            return List.copyOf(result);
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private static <T> List<T> range(int first, int end, IntFunction<T> mapper) {
        var result = new ArrayList<T>(end - first);
        for (int i = first; i < end; i++) result.add(mapper.apply(i));
        return List.copyOf(result);
    }

    @Override public void close() {
        if (executor != null) executor.close();
    }
}
