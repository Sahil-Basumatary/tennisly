package dev.sahilbasumatary.replayservice.storage;

import dev.sahilbasumatary.replayservice.config.ReplayStorageProperties;
import dev.sahilbasumatary.replayservice.exception.ReplayStorageException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Thin wrapper over the S3 API for replay payloads. Keys are namespaced per match so a materialised
 * replay is easy to locate, list or purge.
 */
@Component
public class ReplayObjectStore {

    private static final String KEY_PREFIX = "replays";
    private static final String CONTENT_TYPE = "application/json";
    private static final String CONTENT_ENCODING = "gzip";
    private static final Logger log = LoggerFactory.getLogger(ReplayObjectStore.class);

    private final S3Client s3Client;
    private final ReplayStorageProperties properties;

    public ReplayObjectStore(S3Client s3Client, ReplayStorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureBucketExists() {
        if (!properties.autoCreateBucket()) {
            return;
        }
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(properties.bucket()).build());
        } catch (NoSuchBucketException ex) {
            createBucket();
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                createBucket();
                return;
            }
            log.warn(
                    "Could not verify replay bucket {}; storage may be unavailable: {}",
                    properties.bucket(),
                    ex.getMessage());
        } catch (RuntimeException ex) {
            log.warn(
                    "Object storage is unreachable at startup; replays will fail to persist until"
                            + " it recovers: {}",
                    ex.getMessage());
        }
    }

    public String buildKey(UUID matchId, UUID artifactId) {
        return KEY_PREFIX + "/" + matchId + "/" + artifactId + ".json.gz";
    }

    public String bucket() {
        return properties.bucket();
    }

    public void put(String key, byte[] data, String checksumSha256) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.bucket())
                            .key(key)
                            .contentType(CONTENT_TYPE)
                            .contentEncoding(CONTENT_ENCODING)
                            .metadata(java.util.Map.of("checksum-sha256", checksumSha256))
                            .build(),
                    RequestBody.fromBytes(data));
        } catch (RuntimeException ex) {
            throw new ReplayStorageException("Failed to store replay object " + key, ex);
        }
    }

    public byte[] get(String key) {
        try {
            ResponseBytes<GetObjectResponse> response =
                    s3Client.getObjectAsBytes(
                            GetObjectRequest.builder()
                                    .bucket(properties.bucket())
                                    .key(key)
                                    .build());
            return response.asByteArray();
        } catch (RuntimeException ex) {
            throw new ReplayStorageException("Failed to load replay object " + key, ex);
        }
    }

    public void delete(String key) {
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder().bucket(properties.bucket()).key(key).build());
        } catch (RuntimeException ex) {
            throw new ReplayStorageException("Failed to delete replay object " + key, ex);
        }
    }

    private void createBucket() {
        try {
            s3Client.createBucket(
                    CreateBucketRequest.builder().bucket(properties.bucket()).build());
            log.info("Created replay bucket {}", properties.bucket());
        } catch (RuntimeException ex) {
            log.warn("Failed to create replay bucket {}: {}", properties.bucket(), ex.getMessage());
        }
    }
}
