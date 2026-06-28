package dev.sahilbasumatary.replayservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.sahilbasumatary.replayservice.domain.PlayerTier;
import dev.sahilbasumatary.replayservice.domain.ShotType;
import dev.sahilbasumatary.replayservice.domain.Surface;

/**
 * Statistical fingerprint of a shot type on a given surface and player tier, sourced from
 * tennis-data-service. Landing coordinates and dispersions are in metres; speed in km/h; spin in
 * rpm; arc height in metres.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ShotDistributionModel(
        ShotType shotType,
        Surface surface,
        PlayerTier playerTier,
        double meanLandingX,
        double meanLandingY,
        double stdDevX,
        double stdDevY,
        double meanSpeedKmh,
        double speedStdDev,
        double meanSpinRpm,
        double spinStdDev,
        double meanArcHeight,
        double arcStdDev,
        int sampleSize) {}
