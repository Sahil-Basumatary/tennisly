package dev.sahilbasumatary.matchservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.matchservice.dto.response.MatchEventLogResponse;
import dev.sahilbasumatary.matchservice.entity.MatchEventType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class MatchEventReplayCacheTest {

    @Test
    void storesARecoveryPageForOneSecondWithoutAPublicCacheHeader() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        MatchEventReplayCache cache = new MatchEventReplayCache(redis, mapper);
        UUID matchId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        MatchEventLogResponse row =
                new MatchEventLogResponse(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        6L,
                        MatchEventType.POINT_RECORDED,
                        Map.of(),
                        Instant.parse("2026-08-26T12:00:00Z"));
        String key = MatchEventReplayCache.key(matchId, 5, 2);

        cache.write(matchId, 5, 2, List.of(row));

        verify(values).set(eq(key), eq(mapper.writeValueAsString(List.of(row))), eq(MatchEventReplayCache.TTL));
        when(values.get(key)).thenReturn(mapper.writeValueAsString(List.of(row)));
        Optional<List<MatchEventLogResponse>> hit = cache.read(matchId, 5, 2);
        assertTrue(hit.isPresent());
        assertEquals(6L, hit.get().get(0).sequence());
    }
}
