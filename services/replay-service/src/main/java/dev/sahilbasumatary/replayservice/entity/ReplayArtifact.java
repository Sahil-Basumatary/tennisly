package dev.sahilbasumatary.replayservice.entity;

import dev.sahilbasumatary.replayservice.domain.ReplayStatus;
import dev.sahilbasumatary.replayservice.domain.Surface;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "replay_artifacts")
@EntityListeners(AuditingEntityListener.class)
public class ReplayArtifact {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "match_id", nullable = false, unique = true)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID matchId;

    @Column(name = "storage_bucket", nullable = false, length = 255)
    private String storageBucket;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Surface surface;

    @Column(name = "frame_rate", nullable = false)
    private int frameRate;

    @Column(name = "point_count", nullable = false)
    private int pointCount;

    @Column(name = "shot_count", nullable = false)
    private int shotCount;

    @Column(name = "frame_count", nullable = false)
    private int frameCount;

    @Column(name = "duration_seconds", nullable = false)
    private double durationSeconds;

    @Column(name = "content_encoding", nullable = false, length = 32)
    private String contentEncoding;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "uncompressed_bytes", nullable = false)
    private long uncompressedBytes;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Column(name = "engine_version", nullable = false, length = 32)
    private String engineVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReplayStatus status = ReplayStatus.READY;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
    }

    public String getStorageBucket() {
        return storageBucket;
    }

    public void setStorageBucket(String storageBucket) {
        this.storageBucket = storageBucket;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public Surface getSurface() {
        return surface;
    }

    public void setSurface(Surface surface) {
        this.surface = surface;
    }

    public int getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    public int getPointCount() {
        return pointCount;
    }

    public void setPointCount(int pointCount) {
        this.pointCount = pointCount;
    }

    public int getShotCount() {
        return shotCount;
    }

    public void setShotCount(int shotCount) {
        this.shotCount = shotCount;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public void setFrameCount(int frameCount) {
        this.frameCount = frameCount;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(double durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public long getUncompressedBytes() {
        return uncompressedBytes;
    }

    public void setUncompressedBytes(long uncompressedBytes) {
        this.uncompressedBytes = uncompressedBytes;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }

    public ReplayStatus getStatus() {
        return status;
    }

    public void setStatus(ReplayStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
