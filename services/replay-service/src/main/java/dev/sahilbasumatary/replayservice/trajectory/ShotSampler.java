package dev.sahilbasumatary.replayservice.trajectory;

import dev.sahilbasumatary.replayservice.client.dto.ShotDistributionModel;
import dev.sahilbasumatary.replayservice.domain.ShotType;
import dev.sahilbasumatary.replayservice.physics.CourtGeometry;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Component;

/**
 * Draws a concrete shot from a statistical distribution. All randomness flows through the supplied
 * {@link RandomGenerator}, which the caller seeds deterministically so a replay is byte-for-byte
 * reproducible.
 */
@Component
public class ShotSampler {

    private static final double LATERAL_MARGIN_METRES = 0.20;
    private static final double DEPTH_MARGIN_METRES = 0.20;
    private static final double SERVE_MIN_DEPTH_METRES = 3.0;
    private static final double SERVE_MAX_DEPTH_METRES = 6.3;
    private static final double SECONDS_PER_HOUR = 3600.0;
    private static final double METRES_PER_KILOMETRE = 1000.0;
    private static final double RADIANS_PER_REVOLUTION = 2.0 * Math.PI;
    private static final double SECONDS_PER_MINUTE = 60.0;

    public ShotParameters sample(
            ShotType shotType, ShotDistributionModel model, RandomGenerator random) {
        double speedMetresPerSecond =
                kilometresPerHourToMetresPerSecond(
                        positiveGaussian(model.meanSpeedKmh(), model.speedStdDev(), random));
        double spinRateRadiansPerSecond =
                revolutionsPerMinuteToRadiansPerSecond(
                        positiveGaussian(model.meanSpinRpm(), model.spinStdDev(), random));
        double lateral =
                clampLateral(model.meanLandingX() + random.nextGaussian() * model.stdDevX());
        double depth =
                clampDepth(
                        shotType,
                        model.meanLandingY() + random.nextGaussian() * model.stdDevY());
        double arcHeight =
                Math.max(0.2, model.meanArcHeight() + random.nextGaussian() * model.arcStdDev());
        return new ShotParameters(
                lateral,
                depth,
                speedMetresPerSecond,
                spinRateRadiansPerSecond,
                ShotKinematics.spinType(shotType),
                arcHeight);
    }

    private double clampLateral(double value) {
        double bound = CourtGeometry.SINGLES_HALF_WIDTH_METRES - LATERAL_MARGIN_METRES;
        return Math.max(-bound, Math.min(bound, value));
    }

    private double clampDepth(ShotType shotType, double value) {
        if (ShotKinematics.isServe(shotType)) {
            return Math.max(SERVE_MIN_DEPTH_METRES, Math.min(SERVE_MAX_DEPTH_METRES, value));
        }
        double bound = CourtGeometry.HALF_LENGTH_METRES - DEPTH_MARGIN_METRES;
        return Math.max(1.0, Math.min(bound, value));
    }

    private double positiveGaussian(double mean, double standardDeviation, RandomGenerator random) {
        double sampled = mean + random.nextGaussian() * standardDeviation;
        return Math.max(0.1, sampled);
    }

    private double kilometresPerHourToMetresPerSecond(double kilometresPerHour) {
        return kilometresPerHour * METRES_PER_KILOMETRE / SECONDS_PER_HOUR;
    }

    private double revolutionsPerMinuteToRadiansPerSecond(double revolutionsPerMinute) {
        return revolutionsPerMinute * RADIANS_PER_REVOLUTION / SECONDS_PER_MINUTE;
    }
}
