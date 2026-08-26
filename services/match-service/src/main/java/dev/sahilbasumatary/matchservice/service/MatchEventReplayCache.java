package dev.sahilbasumatary.matchservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.matchservice.dto.response.MatchEventLogResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class MatchEventReplayCache {

    // 1s collapses a reconnect stampede; no SCAN invalidation so point commits stay off this path.
    public static final Duration TTL = Duration.ofSeconds(1);
    public static final String KEY_PREFIX = "live-events:";

    private static final Logger log = LoggerFactory.getLogger(MatchEventReplayCache.class);
    private static final TypeReference<List<MatchEventLogResponse>> LIST_TYPE =
            new TypeReference<>() {};

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public MatchEventReplayCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public static String key(UUID matchId, long afterSequence, int limit) {
        return KEY_PREFIX + matchId + ":" + afterSequence + ":" + limit;
    }

    public Optional<List<MatchEventLogResponse>> read(UUID matchId, long afterSequence, int limit) {
        try {
            String json = redisTemplate.opsForValue().get(key(matchId, afterSequence, limit));
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, LIST_TYPE));
        } catch (Exception ex) {
            log.debug("live event replay cache miss/decode: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public void write(UUID matchId, long afterSequence, int limit, List<MatchEventLogResponse> rows) {
        try {
            redisTemplate
                    .opsForValue()
                    .set(
                            key(matchId, afterSequence, limit),
                            objectMapper.writeValueAsString(rows),
                            TTL);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize event replay page", ex);
        } catch (RuntimeException ex) {
            log.debug("live event replay cache write skipped: {}", ex.getMessage());
        }
    }
}
