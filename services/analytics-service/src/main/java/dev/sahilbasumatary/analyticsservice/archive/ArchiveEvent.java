package dev.sahilbasumatary.analyticsservice.archive;

import java.util.Map;
import java.util.UUID;

public record ArchiveEvent(
        UUID matchId,
        int pointSequence,
        long liveSequence,
        UUID serverId,
        UUID winnerId,
        String outcome,
        Integer rallyLength,
        Map<String, Object> scoreSnapshot) {}
