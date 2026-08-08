package dev.sahilbasumatary.analyticsservice.dto.response;

import dev.sahilbasumatary.analyticsservice.domain.TapeSideMetrics;
import dev.sahilbasumatary.analyticsservice.index.PlayerMatchDocument;
import java.time.Instant;
import java.util.UUID;

public record PlayerTrendPointResponse(
        UUID matchId,
        Instant endedAt,
        Instant scheduledAt,
        TapeSideMetrics metrics,
        Boolean won) {

    public static PlayerTrendPointResponse from(PlayerMatchDocument document) {
        return new PlayerTrendPointResponse(
                document.getMatchId(),
                document.getEndedAt(),
                document.getScheduledAt(),
                document.getMetrics(),
                document.getWon());
    }
}
