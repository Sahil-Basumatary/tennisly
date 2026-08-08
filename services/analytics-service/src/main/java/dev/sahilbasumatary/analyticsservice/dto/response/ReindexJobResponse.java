package dev.sahilbasumatary.analyticsservice.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ReindexJobResponse(
        UUID id,
        String status,
        UUID cursorMatchId,
        int processedCount,
        Integer totalCount,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt) {}
