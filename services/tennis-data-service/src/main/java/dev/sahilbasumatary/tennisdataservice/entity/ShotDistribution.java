package dev.sahilbasumatary.tennisdataservice.entity;

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
@Table(name = "shot_distributions")
@EntityListeners(AuditingEntityListener.class)
public class ShotDistribution {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "shot_type", nullable = false, length = 32)
    private ShotType shotType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Surface surface;

    @Enumerated(EnumType.STRING)
    @Column(name = "player_tier", nullable = false, length = 16)
    private PlayerTier playerTier;

    @Column(name = "mean_landing_x", nullable = false)
    private Double meanLandingX;

    @Column(name = "mean_landing_y", nullable = false)
    private Double meanLandingY;

    @Column(name = "std_dev_x", nullable = false)
    private Double stdDevX;

    @Column(name = "std_dev_y", nullable = false)
    private Double stdDevY;

    @Column(name = "mean_speed_kmh", nullable = false)
    private Double meanSpeedKmh;

    @Column(name = "speed_std_dev", nullable = false)
    private Double speedStdDev;

    @Column(name = "mean_spin_rpm", nullable = false)
    private Double meanSpinRpm;

    @Column(name = "spin_std_dev", nullable = false)
    private Double spinStdDev;

    @Column(name = "mean_arc_height", nullable = false)
    private Double meanArcHeight;

    @Column(name = "arc_std_dev", nullable = false)
    private Double arcStdDev;

    @Column(name = "sample_size", nullable = false)
    private Integer sampleSize;

    @Column(nullable = false)
    private boolean active = true;

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

    public ShotType getShotType() {
        return shotType;
    }

    public void setShotType(ShotType shotType) {
        this.shotType = shotType;
    }

    public Surface getSurface() {
        return surface;
    }

    public void setSurface(Surface surface) {
        this.surface = surface;
    }

    public PlayerTier getPlayerTier() {
        return playerTier;
    }

    public void setPlayerTier(PlayerTier playerTier) {
        this.playerTier = playerTier;
    }

    public Double getMeanLandingX() {
        return meanLandingX;
    }

    public void setMeanLandingX(Double meanLandingX) {
        this.meanLandingX = meanLandingX;
    }

    public Double getMeanLandingY() {
        return meanLandingY;
    }

    public void setMeanLandingY(Double meanLandingY) {
        this.meanLandingY = meanLandingY;
    }

    public Double getStdDevX() {
        return stdDevX;
    }

    public void setStdDevX(Double stdDevX) {
        this.stdDevX = stdDevX;
    }

    public Double getStdDevY() {
        return stdDevY;
    }

    public void setStdDevY(Double stdDevY) {
        this.stdDevY = stdDevY;
    }

    public Double getMeanSpeedKmh() {
        return meanSpeedKmh;
    }

    public void setMeanSpeedKmh(Double meanSpeedKmh) {
        this.meanSpeedKmh = meanSpeedKmh;
    }

    public Double getSpeedStdDev() {
        return speedStdDev;
    }

    public void setSpeedStdDev(Double speedStdDev) {
        this.speedStdDev = speedStdDev;
    }

    public Double getMeanSpinRpm() {
        return meanSpinRpm;
    }

    public void setMeanSpinRpm(Double meanSpinRpm) {
        this.meanSpinRpm = meanSpinRpm;
    }

    public Double getSpinStdDev() {
        return spinStdDev;
    }

    public void setSpinStdDev(Double spinStdDev) {
        this.spinStdDev = spinStdDev;
    }

    public Double getMeanArcHeight() {
        return meanArcHeight;
    }

    public void setMeanArcHeight(Double meanArcHeight) {
        this.meanArcHeight = meanArcHeight;
    }

    public Double getArcStdDev() {
        return arcStdDev;
    }

    public void setArcStdDev(Double arcStdDev) {
        this.arcStdDev = arcStdDev;
    }

    public Integer getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(Integer sampleSize) {
        this.sampleSize = sampleSize;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
