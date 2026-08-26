package dev.sahilbasumatary.matchservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.matchservice.dto.response.MatchResponse;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class MatchTickerCache {

    public static final String TICKER_KEY = "live-ticker:v1";
    public static final String LIVE_IDS_KEY = "live-ticker:ids";
    public static final int MAX_ITEMS = 12;

    private static final Logger log = LoggerFactory.getLogger(MatchTickerCache.class);
    private static final TypeReference<List<MatchResponse>> LIST_TYPE = new TypeReference<>() {};

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public MatchTickerCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void remember(MatchResponse snapshot) {
        try {
            String id = snapshot.id().toString();
            if (snapshot.status() == MatchStatus.IN_PROGRESS
                    || snapshot.status() == MatchStatus.SUSPENDED) {
                redisTemplate.opsForSet().add(LIVE_IDS_KEY, id);
            } else {
                redisTemplate.opsForSet().remove(LIVE_IDS_KEY, id);
            }
            write(rebuild());
        } catch (RuntimeException ex) {
            log.warn(
                    "Failed to refresh live ticker cache matchId={}: {}",
                    snapshot.id(),
                    ex.getMessage());
            log.debug("Live ticker cache refresh failure", ex);
        }
    }

    public Optional<List<MatchResponse>> read() {
        try {
            String json = redisTemplate.opsForValue().get(TICKER_KEY);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, LIST_TYPE));
        } catch (Exception ex) {
            log.debug("live ticker cache miss/decode: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public void write(List<MatchResponse> items) {
        try {
            redisTemplate.opsForValue().set(TICKER_KEY, objectMapper.writeValueAsString(items));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize live ticker", ex);
        }
    }

    private List<MatchResponse> rebuild() {
        Set<String> ids = redisTemplate.opsForSet().members(LIVE_IDS_KEY);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<MatchResponse> rows = new ArrayList<>();
        for (String id : ids) {
            try {
                String json = redisTemplate.opsForValue().get(MatchRealtimeNotifier.cacheKey(UUID.fromString(id)));
                if (json == null || json.isBlank()) {
                    continue;
                }
                rows.add(objectMapper.readValue(json, MatchResponse.class));
            } catch (Exception ex) {
                log.debug("skip ticker member {}: {}", id, ex.getMessage());
            }
        }
        rows.sort(
                Comparator.comparing((MatchResponse row) -> liveRank(row.status()))
                        .thenComparing(MatchResponse::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        if (rows.size() <= MAX_ITEMS) {
            return rows;
        }
        return List.copyOf(rows.subList(0, MAX_ITEMS));
    }

    private static int liveRank(MatchStatus status) {
        if (status == MatchStatus.IN_PROGRESS) {
            return 0;
        }
        if (status == MatchStatus.SUSPENDED) {
            return 1;
        }
        return 2;
    }
}
