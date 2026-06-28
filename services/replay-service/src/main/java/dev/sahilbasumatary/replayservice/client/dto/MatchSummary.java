package dev.sahilbasumatary.replayservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.sahilbasumatary.replayservice.domain.PlayerSide;
import dev.sahilbasumatary.replayservice.domain.Surface;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MatchSummary(
        UUID id,
        String externalId,
        UUID tournamentId,
        Surface surface,
        String status,
        int bestOfSets,
        List<MatchPlayerSummary> players,
        int pointsPlayed) {

    public MatchPlayerSummary playerOn(PlayerSide side) {
        return players.stream().filter(player -> player.side() == side).findFirst().orElse(null);
    }
}
