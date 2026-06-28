package dev.sahilbasumatary.replayservice.client.dto;

import dev.sahilbasumatary.replayservice.domain.PlayerSide;
import java.util.UUID;

public record MatchPlayerSummary(
        UUID id, UUID playerId, String displayName, PlayerSide side, Integer seedNumber) {}
