package dev.sahilbasumatary.analyticsservice.dto.response;

import java.util.List;
import java.util.UUID;

public record PlayerTrendsResponse(UUID playerId, List<PlayerTrendPointResponse> trends) {}
