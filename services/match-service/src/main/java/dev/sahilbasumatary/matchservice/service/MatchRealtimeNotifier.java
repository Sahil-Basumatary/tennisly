package dev.sahilbasumatary.matchservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.matchservice.dto.response.MatchLiveEventResponse;
import dev.sahilbasumatary.matchservice.dto.response.MatchResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MatchRealtimeNotifier {

    private static final Logger log = LoggerFactory.getLogger(MatchRealtimeNotifier.class);
    private static final Duration LIVE_SNAPSHOT_TTL = Duration.ofHours(6);
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public MatchRealtimeNotifier(
            SimpMessagingTemplate messagingTemplate,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(MatchLiveEventResponse event) {
        cacheSnapshot(event.snapshot());
        messagingTemplate.convertAndSend(topic(event.matchId()), event);
    }

    public Optional<MatchResponse> findCachedSnapshot(UUID matchId) {
        try {
            String json = redisTemplate.opsForValue().get(cacheKey(matchId));
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, MatchResponse.class));
        } catch (Exception ex) {
            log.debug("live snapshot cache miss/decode matchId={}: {}", matchId, ex.getMessage());
            return Optional.empty();
        }
    }

    private void cacheSnapshot(MatchResponse response) {
        try {
            redisTemplate
                    .opsForValue()
                    .set(
                            cacheKey(response.id()),
                            objectMapper.writeValueAsString(response),
                            LIVE_SNAPSHOT_TTL);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize live match snapshot matchId={}", response.id(), ex);
        }
    }

    private String topic(UUID matchId) {
        return "/topic/matches/" + matchId;
    }

    private String cacheKey(UUID matchId) {
        return "live-match:" + matchId;
    }
}
