package dev.sahilbasumatary.analyticsservice.dto.response;

import dev.sahilbasumatary.analyticsservice.domain.TapeSideMetrics;
import dev.sahilbasumatary.analyticsservice.index.PlayerMatchDocument;
import java.time.Instant;
import java.util.UUID;

public record PlayerMatchRowResponse(
        UUID matchId,
        UUID opponentId,
        String opponentName,
        Boolean won,
        String surface,
        String tournamentName,
        Integer season,
        Instant endedAt,
        Instant scheduledAt,
        TapeSideMetrics metrics) {

    public static PlayerMatchRowResponse from(PlayerMatchDocument document) {
        return new PlayerMatchRowResponse(
                document.getMatchId(),
                document.getOpponentId(),
                document.getOpponentName(),
                document.getWon(),
                document.getSurface(),
                document.getTournamentName(),
                document.getSeason(),
                document.getEndedAt(),
                document.getScheduledAt(),
                document.getMetrics());
    }
}
