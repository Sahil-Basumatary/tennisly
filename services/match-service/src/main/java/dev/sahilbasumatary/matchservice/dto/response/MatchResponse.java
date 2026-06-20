package dev.sahilbasumatary.matchservice.dto.response;

import dev.sahilbasumatary.matchservice.entity.Match;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import dev.sahilbasumatary.matchservice.entity.Surface;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MatchResponse(
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
        List<MatchPlayerResponse> players,
        int pointsPlayed,
        Instant createdAt,
        Instant updatedAt) {

    public static MatchResponse from(Match match) {
        return new MatchResponse(
                match.getId(),
                match.getExternalId(),
                match.getTournamentId(),
                match.getSurface(),
                match.getStatus(),
                match.getBestOfSets(),
                match.getScheduledAt(),
                match.getStartedAt(),
                match.getEndedAt(),
                match.getMetadata(),
                match.getCurrentScore(),
                match.getPlayers().stream().map(MatchPlayerResponse::from).toList(),
                match.getPoints().size(),
                match.getCreatedAt(),
                match.getUpdatedAt());
    }
}
