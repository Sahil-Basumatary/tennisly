package dev.sahilbasumatary.matchservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.matchservice.dto.response.MatchLiveEventResponse;
import dev.sahilbasumatary.matchservice.metrics.MatchTimers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class MatchLiveRedisSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(MatchLiveRedisSubscriber.class);
    private final ObjectMapper objectMapper;
    private final MatchRealtimeNotifier realtimeNotifier;
    private final MatchTimers matchTimers;
    private final MatchFanoutScheduler fanoutScheduler;

    public MatchLiveRedisSubscriber(
            ObjectMapper objectMapper,
            MatchRealtimeNotifier realtimeNotifier,
            MatchTimers matchTimers,
            MatchFanoutScheduler fanoutScheduler) {
        this.objectMapper = objectMapper;
        this.realtimeNotifier = realtimeNotifier;
        this.matchTimers = matchTimers;
        this.fanoutScheduler = fanoutScheduler;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            MatchLiveEventResponse event =
                    objectMapper.readValue(
                            new String(message.getBody(), StandardCharsets.UTF_8),
                            MatchLiveEventResponse.class);
            fanoutScheduler.execute(event.matchId(), () -> publishLocally(event));
        } catch (Exception ex) {
            matchTimers.livePublishFailure().increment();
            log.warn("Failed to consume Redis live event: {}", ex.getMessage());
            log.debug("Redis live event failure", ex);
        }
    }

    private void publishLocally(MatchLiveEventResponse event) {
        try {
            realtimeNotifier.publishLocally(event);
            Duration elapsed = Duration.between(event.commitObservedAt(), Instant.now());
            matchTimers
                    .livePublishAfterCommit()
                    .record(elapsed.isNegative() ? Duration.ZERO : elapsed);
        } catch (RuntimeException ex) {
            matchTimers.livePublishFailure().increment();
            log.warn(
                    "Failed to publish Redis live event matchId={} eventId={}: {}",
                    event.matchId(),
                    event.eventId(),
                    ex.getMessage());
            log.debug("Redis live event publication failure", ex);
        }
    }
}
