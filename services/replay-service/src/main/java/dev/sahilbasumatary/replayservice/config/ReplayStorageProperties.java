package dev.sahilbasumatary.replayservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Object storage coordinates. Defaults target a local MinIO instance; in production these resolve to
 * a real S3 bucket. Path-style access is required by MinIO and harmless against S3.
 */
@ConfigurationProperties(prefix = "replay.storage")
public record ReplayStorageProperties(
        String endpoint,
        String region,
        String bucket,
        String accessKey,
        String secretKey,
        boolean pathStyleAccess,
        boolean autoCreateBucket) {

    public ReplayStorageProperties {
        if (region == null || region.isBlank()) {
            region = "us-east-1";
        }
        if (bucket == null || bucket.isBlank()) {
            bucket = "tennisly-replays";
        }
    }
}
