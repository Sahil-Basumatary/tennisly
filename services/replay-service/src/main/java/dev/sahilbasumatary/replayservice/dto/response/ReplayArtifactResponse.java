package dev.sahilbasumatary.replayservice.dto.response;

import dev.sahilbasumatary.replayservice.domain.ReplayStatus;
import dev.sahilbasumatary.replayservice.domain.Surface;
import dev.sahilbasumatary.replayservice.entity.ReplayArtifact;
import java.time.Instant;
import java.util.UUID;

/** Metadata describing a materialised replay stored in object storage. */
public record ReplayArtifactResponse(
        UUID id,
        UUID matchId,
        String storageBucket,
        String storageKey,
        Surface surface,
        int frameRate,
        int pointCount,
        int shotCount,
        int frameCount,
        double durationSeconds,
        long sizeBytes,
        long uncompressedBytes,
        double compressionRatio,
        String checksumSha256,
        String engineVersion,
        ReplayStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static ReplayArtifactResponse from(ReplayArtifact artifact) {
        double ratio =
                artifact.getSizeBytes() == 0
                        ? 0.0
                        : (double) artifact.getUncompressedBytes() / artifact.getSizeBytes();
        return new ReplayArtifactResponse(
                artifact.getId(),
                artifact.getMatchId(),
                artifact.getStorageBucket(),
                artifact.getStorageKey(),
                artifact.getSurface(),
                artifact.getFrameRate(),
                artifact.getPointCount(),
                artifact.getShotCount(),
                artifact.getFrameCount(),
                artifact.getDurationSeconds(),
                artifact.getSizeBytes(),
                artifact.getUncompressedBytes(),
                Math.round(ratio * 100.0) / 100.0,
                artifact.getChecksumSha256(),
                artifact.getEngineVersion(),
                artifact.getStatus(),
                artifact.getCreatedAt(),
                artifact.getUpdatedAt());
    }
}
