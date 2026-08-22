package dev.sahilbasumatary.matchservice.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.matchservice.dto.response.MatchLiveEventResponse;
import dev.sahilbasumatary.matchservice.dto.response.MatchResponse;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import dev.sahilbasumatary.matchservice.entity.Surface;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class MatchRealtimeNotifierTest {

    private static final String CHANNEL = "test-match-live";
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final MatchRealtimeNotifier notifier =
            new MatchRealtimeNotifier(
                    messagingTemplate, redisTemplate, objectMapper, CHANNEL);

    @Test
    void publishesThroughRedisWhenLiveNodesAreSubscribed() {
        MatchLiveEventResponse event = event();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.convertAndSend(CHANNEL, objectMapperValue(event))).thenReturn(2L);

        notifier.publish(event);

        verify(redisTemplate).convertAndSend(CHANNEL, objectMapperValue(event));
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void fallsBackToTheLocalBrokerWhenNoRedisNodeIsListening() {
        MatchLiveEventResponse event = event();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.convertAndSend(CHANNEL, objectMapperValue(event))).thenReturn(0L);

        notifier.publish(event);

        verify(messagingTemplate)
                .convertAndSend("/topic/matches/" + event.matchId(), event);
    }

    @Test
    void fallsBackToTheLocalBrokerWhenRedisIsUnavailable() {
        MatchLiveEventResponse event = event();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(valueOperations)
                .set(anyString(), anyString(), any(Duration.class));
        doThrow(new IllegalStateException("redis unavailable"))
                .when(redisTemplate)
                .convertAndSend(CHANNEL, objectMapperValue(event));

        assertThrows(IllegalStateException.class, () -> notifier.publish(event));

        verify(messagingTemplate)
                .convertAndSend("/topic/matches/" + event.matchId(), event);
    }

    private String objectMapperValue(MatchLiveEventResponse event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private MatchLiveEventResponse event() {
        UUID matchId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Instant now = Instant.parse("2026-08-22T12:00:00Z");
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
