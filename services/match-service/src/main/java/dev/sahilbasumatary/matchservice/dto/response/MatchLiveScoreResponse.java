package dev.sahilbasumatary.matchservice.dto.response;

import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MatchLiveScoreResponse(
        UUID id,
        MatchStatus status,
        long liveSequence,
        int pointsPlayed,
        Map<String, Object> currentScore,
        Instant updatedAt) {

    public static MatchLiveScoreResponse from(MatchResponse match) {
        return new MatchLiveScoreResponse(
                match.id(),
                match.status(),
                match.liveSequence(),
                match.pointsPlayed(),
                match.currentScore(),
                match.updatedAt());
    }
}
