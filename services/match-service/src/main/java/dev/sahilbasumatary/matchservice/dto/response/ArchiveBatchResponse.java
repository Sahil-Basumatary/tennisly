package dev.sahilbasumatary.matchservice.dto.response;

import java.util.UUID;

public record ArchiveBatchResponse(
        UUID matchId, int accepted, int firstSequence, int lastSequence, long liveSequence) {}
