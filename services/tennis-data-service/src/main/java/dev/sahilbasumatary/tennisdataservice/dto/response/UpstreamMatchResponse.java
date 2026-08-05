package dev.sahilbasumatary.tennisdataservice.dto.response;

import dev.sahilbasumatary.tennisdataservice.dto.UpstreamMatchData;
import java.time.Instant;

public record UpstreamMatchResponse(
        long providerMatchId,
        String externalId,
        String tournamentName,
        String surface,
        String format,
        String round,
        String status,
        boolean doubles,
        boolean indoor,
        Instant scheduledAt,
        PlayerSide home,
        PlayerSide away,
        Integer winnerSide) {

    public record PlayerSide(
            Long providerPlayerId, String firstName, String lastName, String displayName) {}

    public static UpstreamMatchResponse from(UpstreamMatchData match) {
        return new UpstreamMatchResponse(
                match.providerMatchId(),
                match.externalId(),
                match.tournamentName(),
                match.surface(),
                match.format(),
                match.round(),
                match.status(),
                match.doubles(),
                match.indoor(),
                match.scheduledAt(),
                toSide(match.home()),
                toSide(match.away()),
                match.winnerSide());
    }

    private static PlayerSide toSide(UpstreamMatchData.UpstreamPlayerRef ref) {
        if (ref == null) {
            return new PlayerSide(null, "", "", "Unknown");
        }
        return new PlayerSide(
                ref.providerPlayerId(), ref.firstName(), ref.lastName(), ref.displayName());
    }
}
