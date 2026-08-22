package dev.sahilbasumatary.matchservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class MatchFanoutSchedulerTest {

    @Test
    void preservesSubmissionOrderForEachMatch() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            MatchFanoutScheduler scheduler = new MatchFanoutScheduler(executor);
            UUID matchId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            List<Integer> completed = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch done = new CountDownLatch(3);

            for (int index = 1; index <= 3; index += 1) {
                int sequence = index;
                scheduler.execute(
                        matchId,
                        () -> {
                            completed.add(sequence);
                            done.countDown();
                        });
            }

            assertTrue(done.await(Duration.ofSeconds(2).toMillis(), TimeUnit.MILLISECONDS));
            assertEquals(List.of(1, 2, 3), completed);
        } finally {
            executor.shutdownNow();
        }
    }
}
