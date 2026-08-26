package dev.sahilbasumatary.analyticsservice.archive;

import java.util.UUID;

public record ArchiveMatchRoster(UUID matchId, UUID homeId, UUID awayId) {}
