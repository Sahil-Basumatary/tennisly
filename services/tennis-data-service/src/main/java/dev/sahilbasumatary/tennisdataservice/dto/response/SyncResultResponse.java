package dev.sahilbasumatary.tennisdataservice.dto.response;

import java.time.Instant;

public record SyncResultResponse(String resource, int recordsProcessed, Instant syncedAt) {

    public static SyncResultResponse of(String resource, int recordsProcessed) {
        return new SyncResultResponse(resource, recordsProcessed, Instant.now());
    }
}
