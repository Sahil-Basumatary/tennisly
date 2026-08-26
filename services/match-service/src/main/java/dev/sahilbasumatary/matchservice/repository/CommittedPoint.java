package dev.sahilbasumatary.matchservice.repository;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CommittedPoint(
        UUID pointId,
        int sequenceNumber,
        long liveSequence,
        Instant recordedAt,
        Instant updatedAt,
        Map<String, Object> currentScore) {}
