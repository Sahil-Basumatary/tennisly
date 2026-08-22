package dev.sahilbasumatary.matchservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.matchservice.dto.response.MatchLiveEventResponse;
import dev.sahilbasumatary.matchservice.dto.response.MatchResponse;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import dev.sahilbasumatary.matchservice.entity.Surface;
import dev.sahilbasumatary.matchservice.metrics.MatchTimers;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.Message;

class MatchLiveRedisSubscriberTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final MatchRealtimeNotifier notifier = mock(MatchRealtimeNotifier.class);
    private final MatchTimers timers = new MatchTimers(new SimpleMeterRegistry());
    private final MatchFanoutScheduler scheduler =
            new MatchFanoutScheduler(Runnable::run);
    private final MatchLiveRedisSubscriber subscriber =
            new MatchLiveRedisSubscriber(objectMapper, notifier, timers, scheduler);

    @Test
    void republishesRedisEventsToTheLocalBroker() throws Exception {
        MatchLiveEventResponse event = event();
        Message message = mock(Message.class);
        when(message.getBody())
                .thenReturn(
                        objectMapper
                                .writeValueAsString(event)
                                .getBytes(StandardCharsets.UTF_8));

        subscriber.onMessage(message, null);

        verify(notifier).publishLocally(event);
        assertEquals(1, timers.livePublishAfterCommit().count());
        assertEquals(0, timers.livePublishFailure().count());
    }

    @Test
    void countsMalformedRedisMessagesWithoutPublishing() {
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn("not-json".getBytes(StandardCharsets.UTF_8));

        subscriber.onMessage(message, null);

        assertEquals(1, timers.livePublishFailure().count());
    }

    private MatchLiveEventResponse event() {
        UUID matchId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Instant now = Instant.now();
        MatchResponse snapshot =
                new MatchResponse(
                        matchId,
                        "live-test",
                        null,
                        Surface.HARD,
                        MatchStatus.IN_PROGRESS,
                        3,
                        null,
                        now,
                        null,
                        Map.of(),
                        Map.of(),
                        List.of(),
                        1,
                        3,
                        now,
                        now);
        return new MatchLiveEventResponse(
                "event-3", "MATCH_POINT_RECORDED", matchId, 3, now, now, snapshot);
    }
}
