package dev.sahilbasumatary.matchservice.service;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class MatchFanoutScheduler {

    private static final int STRIPE_COUNT = 1_024;
    private final SerialExecutor[] stripes = new SerialExecutor[STRIPE_COUNT];

    public MatchFanoutScheduler(@Qualifier("matchFanoutExecutor") Executor delegate) {
        for (int index = 0; index < stripes.length; index += 1) {
            stripes[index] = new SerialExecutor(delegate);
        }
    }

    public void execute(UUID matchId, Runnable task) {
        int stripe = Math.floorMod(matchId.hashCode(), stripes.length);
        stripes[stripe].execute(task);
    }

    private static final class SerialExecutor implements Executor {

        private final Queue<Runnable> tasks = new ArrayDeque<>();
        private final Executor delegate;
        private Runnable active;

        private SerialExecutor(Executor delegate) {
            this.delegate = delegate;
        }

        @Override
        public synchronized void execute(Runnable command) {
            tasks.add(
                    () -> {
                        try {
                            command.run();
                        } finally {
                            scheduleNext();
                        }
                    });
            if (active == null) {
                scheduleNext();
            }
        }

        private synchronized void scheduleNext() {
            active = tasks.poll();
            if (active != null) {
                delegate.execute(active);
            }
        }
    }
}
