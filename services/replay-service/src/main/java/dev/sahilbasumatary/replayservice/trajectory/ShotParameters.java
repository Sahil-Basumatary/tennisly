package dev.sahilbasumatary.replayservice.trajectory;

import dev.sahilbasumatary.replayservice.domain.SpinType;

/**
 * A single sampled realisation of a shot, in the canonical frame where the ball always travels in
 * the positive depth direction. {@code landingDepthMetres} is the distance past the net the ball
 * lands; {@code landingLateralMetres} is the lateral offset from the centre line.
 */
public record ShotParameters(
        double landingLateralMetres,
        double landingDepthMetres,
        double speedMetresPerSecond,
        double spinRateRadiansPerSecond,
        SpinType spinType,
        double arcHeightMetres) {}
