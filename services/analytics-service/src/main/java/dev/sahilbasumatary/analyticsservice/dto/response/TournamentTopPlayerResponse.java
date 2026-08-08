package dev.sahilbasumatary.analyticsservice.dto.response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TournamentTopPlayerResponse(UUID playerId, String displayName, int pointsWon) {}
