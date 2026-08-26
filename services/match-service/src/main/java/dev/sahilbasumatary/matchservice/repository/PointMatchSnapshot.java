package dev.sahilbasumatary.matchservice.repository;

import dev.sahilbasumatary.matchservice.dto.response.MatchPlayerResponse;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import dev.sahilbasumatary.matchservice.entity.Surface;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PointMatchSnapshot(
        UUID id,
        String externalId,
        UUID tournamentId,
        Surface surface,
        MatchStatus status,
        int bestOfSets,
        Instant scheduledAt,
        Instant startedAt,
        Instant endedAt,
        Map<String, Object> metadata,
        Map<String, Object> currentScore,
        int pointCount,
        long liveSequence,
        Instant createdAt,
        Instant updatedAt,
        List<MatchPlayerResponse> players) {

    public boolean hasPlayer(UUID playerId) {
        for (MatchPlayerResponse player : players) {
            if (player.playerId().equals(playerId)) {
                return true;
            }
        }
        return false;
    }
}
