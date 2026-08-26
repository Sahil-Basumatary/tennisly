package dev.sahilbasumatary.matchservice.controller;

import dev.sahilbasumatary.matchservice.dto.response.MatchResponse;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import java.util.List;
import org.springframework.http.ResponseEntity;

final class MatchPublicCache {

    static final String LIVE = "public, s-maxage=2, stale-while-revalidate=3";
    static final String TICKER = "public, s-maxage=3, stale-while-revalidate=10";
    static final String COMPLETED = "public, max-age=86400, s-maxage=86400, immutable";
    static final String PRIVATE_NO_STORE = "private, no-store, no-cache, must-revalidate";

    private MatchPublicCache() {}

    static boolean terminal(MatchStatus status) {
        return status == MatchStatus.COMPLETED || status == MatchStatus.CANCELLED;
    }

    static String liveControl(MatchStatus status) {
        return terminal(status) ? COMPLETED : LIVE;
    }

    static <T> ResponseEntity<T> withEtag(T body, String etag, String cacheControl) {
        return ResponseEntity.ok().eTag(etag).header("Cache-Control", cacheControl).body(body);
    }

    static ResponseEntity<List<MatchResponse>> ticker(List<MatchResponse> items) {
        long stamp = 0L;
        for (MatchResponse item : items) {
            stamp = Math.max(stamp, item.liveSequence());
        }
        return withEtag(items, "ticker-" + items.size() + "-" + stamp, TICKER);
    }
}
