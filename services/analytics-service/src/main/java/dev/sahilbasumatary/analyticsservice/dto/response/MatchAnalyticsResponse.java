package dev.sahilbasumatary.analyticsservice.dto.response;

import dev.sahilbasumatary.analyticsservice.domain.TapeSideMetrics;
import dev.sahilbasumatary.analyticsservice.index.MatchAnalyticsDocument;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MatchAnalyticsResponse(
        UUID matchId,
        String externalId,
        UUID tournamentId,
        String tournamentKey,
        String tournamentName,
        Integer season,
        String surface,
        String status,
        int bestOfSets,
        Instant scheduledAt,
        Instant startedAt,
        Instant endedAt,
        UUID homePlayerId,
        String homeDisplayName,
        UUID awayPlayerId,
        String awayDisplayName,
        UUID winnerPlayerId,
        TapeSideMetrics homeMetrics,
        TapeSideMetrics awayMetrics,
        int pointsPlayed,
        Map<String, Object> scoreSnapshot,
        Instant indexedAt) {

    public static MatchAnalyticsResponse from(MatchAnalyticsDocument document) {
        return new MatchAnalyticsResponse(
                document.getMatchId(),
                document.getExternalId(),
                document.getTournamentId(),
                document.getTournamentKey(),
                document.getTournamentName(),
                document.getSeason(),
                document.getSurface(),
                document.getStatus(),
                document.getBestOfSets(),
                document.getScheduledAt(),
                document.getStartedAt(),
                document.getEndedAt(),
                document.getHomePlayerId(),
                document.getHomeDisplayName(),
                document.getAwayPlayerId(),
                document.getAwayDisplayName(),
                document.getWinnerPlayerId(),
                document.getHomeMetrics(),
                document.getAwayMetrics(),
                document.getPointsPlayed(),
                document.getScoreSnapshot(),
                document.getIndexedAt());
    }
}
