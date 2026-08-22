package dev.sahilbasumatary.matchservice.service;

import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.common.kafka.EventPublisher;
import dev.sahilbasumatary.common.kafka.TopicNames;
import dev.sahilbasumatary.matchservice.dto.response.MatchLiveEventResponse;
import dev.sahilbasumatary.matchservice.dto.response.MatchResponse;
import dev.sahilbasumatary.matchservice.metrics.MatchTimers;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class MatchEventDispatch {

    private static final Logger log = LoggerFactory.getLogger(MatchEventDispatch.class);
    private final MatchRealtimeNotifier realtimeNotifier;
    private final EventPublisher eventPublisher;
    private final MatchOutboxWriter outboxWriter;
    private final MatchFanoutScheduler matchFanoutScheduler;
    private final MatchTimers matchTimers;

    public MatchEventDispatch(
            MatchRealtimeNotifier realtimeNotifier,
            EventPublisher eventPublisher,
            MatchOutboxWriter outboxWriter,
            MatchFanoutScheduler matchFanoutScheduler,
            MatchTimers matchTimers) {
        this.realtimeNotifier = realtimeNotifier;
        this.eventPublisher = eventPublisher;
        this.outboxWriter = outboxWriter;
        this.matchFanoutScheduler = matchFanoutScheduler;
        this.matchTimers = matchTimers;
    }

    public void publish(UUID matchId, MatchEvent event, MatchResponse response) {
        outboxWriter.enqueue(event);
        scheduleFanout(matchId, event, response);
    }

    private void scheduleFanout(UUID matchId, MatchEvent event, MatchResponse response) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            submitFanout(matchId, event, response, Instant.now());
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        submitFanout(matchId, event, response, Instant.now());
                    }
                });
    }

    private void submitFanout(
            UUID matchId,
            MatchEvent event,
            MatchResponse response,
            Instant commitObservedAt) {
        matchFanoutScheduler.execute(
                matchId, () -> publishAfterCommit(matchId, event, response, commitObservedAt));
    }

    private void publishAfterCommit(
            UUID matchId,
            MatchEvent event,
            MatchResponse response,
            Instant commitObservedAt) {
        try {
            realtimeNotifier.publish(
                    MatchLiveEventResponse.from(event, response, commitObservedAt));
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
