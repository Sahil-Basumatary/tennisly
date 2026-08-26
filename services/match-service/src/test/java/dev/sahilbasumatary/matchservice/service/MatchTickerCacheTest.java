package dev.sahilbasumatary.matchservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.matchservice.dto.response.MatchResponse;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import dev.sahilbasumatary.matchservice.entity.Surface;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class MatchTickerCacheTest {

    @Test
    void rebuildsABoundedTickerDocumentFromLiveSnapshots() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> set = mock(SetOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForSet()).thenReturn(set);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        MatchTickerCache cache = new MatchTickerCache(redis, mapper);
        Instant now = Instant.parse("2026-08-26T12:00:00Z");
        UUID matchId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        MatchResponse snapshot =
                new MatchResponse(
                        matchId,
                        "live-1",
                        null,
                        Surface.HARD,
                        MatchStatus.IN_PROGRESS,
                        3,
                        null,
                        now,
                        null,
                        Map.of(),
                        Map.of("points", List.of("15", "0")),
                        List.of(),
                        2,
                        3,
                        now,
                        now);
        String snapshotJson = mapper.writeValueAsString(snapshot);
        when(set.members(MatchTickerCache.LIVE_IDS_KEY)).thenReturn(Set.of(matchId.toString()));
        when(values.get(MatchRealtimeNotifier.cacheKey(matchId))).thenReturn(snapshotJson);

        cache.remember(snapshot);

        verify(set).add(MatchTickerCache.LIVE_IDS_KEY, matchId.toString());
        verify(values).set(eq(MatchTickerCache.TICKER_KEY), anyString());
    }
}
