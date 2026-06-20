package dev.sahilbasumatary.matchservice.dto.response;

import dev.sahilbasumatary.matchservice.entity.MatchPlayer;
import dev.sahilbasumatary.matchservice.entity.PlayerSide;
import java.util.UUID;

public record MatchPlayerResponse(
        UUID id, UUID playerId, String displayName, PlayerSide side, Integer seedNumber) {

    public static MatchPlayerResponse from(MatchPlayer player) {
        return new MatchPlayerResponse(
                player.getId(),
                player.getPlayerId(),
                player.getDisplayName(),
                player.getSide(),
                player.getSeedNumber());
    }
}
