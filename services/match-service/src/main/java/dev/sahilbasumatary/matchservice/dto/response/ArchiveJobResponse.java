package dev.sahilbasumatary.matchservice.dto.response;

import java.util.UUID;

public record ArchiveJobResponse(
        UUID jobId,
        UUID matchId,
        String status,
        long sourceRows,
        long acceptedRows,
        long duplicateRows,
        long bytesReceived,
        String checksum,
        String contentSha256) {}
