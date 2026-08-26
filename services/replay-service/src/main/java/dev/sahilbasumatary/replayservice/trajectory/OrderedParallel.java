package dev.sahilbasumatary.replayservice.trajectory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntFunction;

/** Runs independent point work on bounded workers and collects results in input order. */
public final class OrderedParallel {

    private OrderedParallel() {}

    public static int resolveWorkers(int configuredWorkers) {
        if (configuredWorkers > 0) {
            return configuredWorkers;
        }
        return Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors()));
    }

    public static <T> List<T> map(int size, int workers, IntFunction<T> mapper) {
        if (size <= 0) {
            return List.of();
        }
        if (size == 1 || workers <= 1) {
            List<T> results = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                results.add(mapper.apply(index));
            }
            return results;
        }
        int poolSize = Math.min(workers, size);
        Object[] slots = new Object[size];
        try (ExecutorService pool = Executors.newFixedThreadPool(poolSize)) {
            List<Callable<Void>> tasks = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                final int slot = index;
                tasks.add(
                        () -> {
                            slots[slot] = mapper.apply(slot);
                            return null;
                        });
            }
            List<Future<Void>> futures = pool.invokeAll(tasks);
            for (Future<Void> future : futures) {
                future.get();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while generating replay points", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Failed to generate replay points", cause);
        }
        List<T> results = new ArrayList<>(size);
        for (Object slot : slots) {
            @SuppressWarnings("unchecked")
            T value = (T) slot;
            results.add(value);
        }
        return results;
    }
}
