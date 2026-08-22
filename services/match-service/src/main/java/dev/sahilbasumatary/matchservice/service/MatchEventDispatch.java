package dev.sahilbasumatary.matchservice.service;

import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.common.kafka.EventPublisher;
import dev.sahilbasumatary.common.kafka.TopicNames;
import dev.sahilbasumatary.matchservice.dto.response.MatchLiveEventResponse;
import dev.sahilbasumatary.matchservice.dto.response.MatchResponse;
import dev.sahilbasumatary.matchservice.metrics.MatchTimers;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class MatchEventDispatch {

    private static final Logger log = LoggerFactory.getLogger(MatchEventDispatch.class);
    private final MatchRealtimeNotifier realtimeNotifier;
    private final EventPublisher eventPublisher;
    private final MatchOutboxWriter outboxWriter;
    private final Executor matchFanoutExecutor;
    private final MatchTimers matchTimers;

    public MatchEventDispatch(
            MatchRealtimeNotifier realtimeNotifier,
            EventPublisher eventPublisher,
            MatchOutboxWriter outboxWriter,
            @Qualifier("matchFanoutExecutor") Executor matchFanoutExecutor,
            MatchTimers matchTimers) {
        this.realtimeNotifier = realtimeNotifier;
        this.eventPublisher = eventPublisher;
        this.outboxWriter = outboxWriter;
        this.matchFanoutExecutor = matchFanoutExecutor;
        this.matchTimers = matchTimers;
    }

    public void publish(UUID matchId, MatchEvent event, MatchResponse response) {
        outboxWriter.enqueue(event);
        scheduleFanout(matchId, event, response);
    }

    private void scheduleFanout(UUID matchId, MatchEvent event, MatchResponse response) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            submitFanout(matchId, event, response, Instant.now(), System.nanoTime());
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        submitFanout(matchId, event, response, Instant.now(), System.nanoTime());
                    }
                });
    }

    private void submitFanout(
            UUID matchId,
            MatchEvent event,
            MatchResponse response,
            Instant commitObservedAt,
            long commitObservedNanos) {
        matchFanoutExecutor.execute(
                () -> publishAfterCommit(
                        matchId, event, response, commitObservedAt, commitObservedNanos));
    }

    private void publishAfterCommit(
            UUID matchId,
            MatchEvent event,
            MatchResponse response,
            Instant commitObservedAt,
            long commitObservedNanos) {
        try {
            realtimeNotifier.publish(
                    MatchLiveEventResponse.from(event, response, commitObservedAt));
            matchTimers
                    .livePublishAfterCommit()
                    .record(System.nanoTime() - commitObservedNanos, TimeUnit.NANOSECONDS);
        } catch (RuntimeException ex) {
            matchTimers.livePublishFailure().increment();
            log.warn(
                    "Post-commit WebSocket publication failed matchId={} eventId={} error={}",
                    matchId,
                    event.getEventId(),
                    ex.getMessage());
            log.debug("Post-commit WebSocket publication stack", ex);
        }
        try {
            eventPublisher.publish(TopicNames.MATCH_EVENTS, matchId.toString(), event);
        } catch (RuntimeException ex) {
            log.warn(
                    "Post-commit Kafka publication failed matchId={} eventId={} error={}",
                    matchId,
                    event.getEventId(),
                    ex.getMessage());
            log.debug("Post-commit Kafka publication stack", ex);
        }
    }
}
