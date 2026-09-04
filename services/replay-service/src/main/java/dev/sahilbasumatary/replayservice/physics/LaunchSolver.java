package dev.sahilbasumatary.replayservice.physics;

import dev.sahilbasumatary.replayservice.domain.SpinType;
import org.springframework.stereotype.Component;

/**
 * Inverse trajectory solver: recover a launch vector that delivers the ball to a sampled landing
 * under drag and Magnus force.
 *
 * <p>v2 starts from a vacuum ballistic guess, samples the branch bounds, then bisects. It does not
 * scan dozens of full flights. Net-clipping probes are discarded when a clearing neighbour exists.
 */
@Component
public class LaunchSolver {

    private static final Vector3 UP = new Vector3(0, 0, 1);
    private static final double MIN_ELEVATION = Math.toRadians(2.0);
    private static final double MAX_ELEVATION = Math.toRadians(75.0);
    private static final double LOW_ARC_MAX = Math.toRadians(44.0);
    private static final double HIGH_ARC_MIN = Math.toRadians(36.0);
    private static final double DRAG_RANGE_STRETCH = 1.12;
    private static final int BISECT_LIMIT = 12;

    private final BallPhysicsSimulator simulator;

    public LaunchSolver(BallPhysicsSimulator simulator) {
        this.simulator = simulator;
    }

    public LaunchSolution solve(
            Vector3 launchPoint,
            double speed,
            double spinRate,
            SpinType spinType,
            Vector3 target,
            BounceProfile profile,
            boolean highArc,
            int maxIterations,
            double toleranceMetres,
            double stepSeconds,
            double maxFlightSeconds) {
        return solve(
                launchPoint,
                speed,
                spinRate,
                spinType,
                target,
                profile,
                highArc,
                maxIterations,
                toleranceMetres,
                stepSeconds,
                maxFlightSeconds,
                null);
    }

    public LaunchSolution solve(
            Vector3 launchPoint,
            double speed,
            double spinRate,
            SpinType spinType,
            Vector3 target,
            BounceProfile profile,
            boolean highArc,
            int maxIterations,
            double toleranceMetres,
            double stepSeconds,
            double maxFlightSeconds,
            BallPathBuffer acceptedPath) {
        Vector3 horizontalDirection =
                new Vector3(target.x() - launchPoint.x(), target.y() - launchPoint.y(), 0.0)
                        .normalized();
        double targetDistance =
                new Vector3(target.x() - launchPoint.x(), target.y() - launchPoint.y(), 0.0)
                        .magnitude();
        Vector3 spin = spinVector(spinRate, horizontalDirection, spinType);
        final int probeBudget = Math.max(6, Math.min(maxIterations, 16));

        double lowBound = highArc ? HIGH_ARC_MIN : MIN_ELEVATION;
        double highBound = highArc ? MAX_ELEVATION : LOW_ARC_MAX;
        double guess =
                clamp(
                        vacuumElevation(
                                speed,
                                targetDistance * DRAG_RANGE_STRETCH,
                                launchPoint.z() - target.z(),
                                highArc),
                        lowBound,
                        highBound);

        Probe guessed =
                probe(
                        launchPoint,
                        speed,
                        spin,
                        horizontalDirection,
                        guess,
                        profile,
                        stepSeconds,
                        maxFlightSeconds);
        if (acceptable(guessed, targetDistance, toleranceMetres)) {
            return finish(
                    launchPoint,
                    speed,
                    spin,
                    horizontalDirection,
                    guessed,
                    true,
                    profile,
                    stepSeconds,
                    maxFlightSeconds,
                    acceptedPath);
        }

        Probe peak = guessed;
        double[] scan =
                new double[] {
                    lowBound,
                    lowBound + (highBound - lowBound) * 0.25,
                    lowBound + (highBound - lowBound) * 0.5,
                    lowBound + (highBound - lowBound) * 0.75,
                    highBound
                };
        Probe low = guessed;
        Probe high = guessed;
        for (int index = 0; index < scan.length; index++) {
            Probe sample =
                    probe(
                            launchPoint,
                            speed,
                            spin,
                            horizontalDirection,
                            scan[index],
                            profile,
                            stepSeconds,
                            maxFlightSeconds);
            peak = maxRange(peak, sample);
            if (index == 0) {
                low = sample;
            }
            if (index == scan.length - 1) {
                high = sample;
            }
        }
        if (targetDistance >= peak.range - toleranceMetres) {
            Probe chosen = preferClear(peak, guessed, targetDistance, toleranceMetres);
            return finish(
                    launchPoint,
                    speed,
                    spin,
                    horizontalDirection,
                    chosen,
                    chosen.range + 0.30 >= targetDistance,
                    profile,
                    stepSeconds,
                    maxFlightSeconds,
                    acceptedPath);
        }

        Probe left = highArc ? peak : low;
        Probe right = highArc ? high : peak;
        if (left.angle > right.angle) {
            Probe swap = left;
            left = right;
            right = swap;
        }
        Probe best = preferClear(low, guessed, targetDistance, toleranceMetres);
        best = preferClear(best, high, targetDistance, toleranceMetres);
        best = preferClear(best, peak, targetDistance, toleranceMetres);
        int bisectIters = Math.min(BISECT_LIMIT, Math.max(4, probeBudget - 6));
        for (int iteration = 0; iteration < bisectIters; iteration++) {
            double midAngle = (left.angle + right.angle) / 2.0;
            Probe mid =
                    probe(
                            launchPoint,
                            speed,
                            spin,
                            horizontalDirection,
                            midAngle,
                            profile,
                            stepSeconds,
                            maxFlightSeconds);
            best = preferClear(best, mid, targetDistance, toleranceMetres);
            if (acceptable(mid, targetDistance, toleranceMetres)) {
                return finish(
                        launchPoint,
                        speed,
                        spin,
                        horizontalDirection,
                        mid,
                        true,
                        profile,
                        stepSeconds,
                        maxFlightSeconds,
                        acceptedPath);
            }
            boolean tooShort = mid.range < targetDistance;
            if (!mid.clearsNet && !highArc) {
                tooShort = true;
            }
            if (highArc) {
                if (tooShort) {
                    right = mid;
                } else {
                    left = mid;
                }
            } else if (tooShort) {
                left = mid;
            } else {
                right = mid;
            }
            if (right.angle - left.angle < 1.0e-4) {
                break;
            }
        }
        if (!acceptable(best, targetDistance, toleranceMetres)) {
            int extras = 8;
            for (int index = 0; index <= extras; index++) {
                double angle = lowBound + (highBound - lowBound) * index / extras;
                Probe sample =
                        probe(
                                launchPoint,
                                speed,
                                spin,
                                horizontalDirection,
                                angle,
                                profile,
                                stepSeconds,
                                maxFlightSeconds);
                best = preferClear(best, sample, targetDistance, toleranceMetres);
                if (acceptable(sample, targetDistance, toleranceMetres)) {
                    return finish(
                            launchPoint,
                            speed,
                            spin,
                            horizontalDirection,
                            sample,
                            true,
                            profile,
                            stepSeconds,
                            maxFlightSeconds,
                            acceptedPath);
                }
            }
        }
        boolean reached = acceptable(best, targetDistance, Math.max(toleranceMetres, 0.30));
        return finish(
                launchPoint,
                speed,
                spin,
                horizontalDirection,
                best,
                reached,
                profile,
                stepSeconds,
                maxFlightSeconds,
                acceptedPath);
    }

    private Probe probe(
            Vector3 launchPoint,
            double speed,
            Vector3 spin,
            Vector3 horizontalDirection,
            double elevationRadians,
            BounceProfile profile,
            double stepSeconds,
            double maxFlightSeconds) {
        BallState initial =
                launchState(launchPoint, speed, spin, horizontalDirection, elevationRadians);
        BallPhysicsSimulator.BounceSample sample =
                simulator.sampleFirstBounce(initial, profile, stepSeconds, maxFlightSeconds);
        double dx = sample.landingX() - launchPoint.x();
        double dy = sample.landingY() - launchPoint.y();
        double range = dx * horizontalDirection.x() + dy * horizontalDirection.y();
        return new Probe(
                elevationRadians,
                range,
                sample.landingX(),
                sample.landingY(),
                sample.clearsNet());
    }

    private LaunchSolution finish(
            Vector3 launchPoint,
            double speed,
            Vector3 spin,
            Vector3 horizontalDirection,
            Probe probe,
            boolean reachedTarget,
            BounceProfile profile,
            double stepSeconds,
            double maxFlightSeconds,
            BallPathBuffer acceptedPath) {
        BallState initial =
                launchState(launchPoint, speed, spin, horizontalDirection, probe.angle);
        if (acceptedPath != null) {
            simulator.simulateInto(
                    initial, profile, stepSeconds, maxFlightSeconds, false, acceptedPath);
        }
        return new LaunchSolution(
                initial, new Vector3(probe.landingX, probe.landingY, 0.0), probe.angle, reachedTarget);
    }

    private static boolean acceptable(Probe probe, double targetDistance, double toleranceMetres) {
        return Math.abs(probe.range - targetDistance) <= toleranceMetres && probe.clearsNet;
    }

    private static Probe preferClear(
            Probe left, Probe right, double targetDistance, double toleranceMetres) {
        double leftErr = Math.abs(left.range - targetDistance);
        double rightErr = Math.abs(right.range - targetDistance);
        if (left.clearsNet != right.clearsNet) {
            Probe clearer = left.clearsNet ? left : right;
            double clearErr = Math.abs(clearer.range - targetDistance);
            if (clearErr <= Math.max(0.30, toleranceMetres)) {
                return clearer;
            }
        }
        if (leftErr <= rightErr) {
            return left;
        }
        return right;
    }

    private static double vacuumElevation(double speed, double range, double height, boolean highArc) {
        if (range < 1.0e-4) {
            return highArc ? Math.toRadians(70.0) : Math.toRadians(8.0);
        }
        double gravity = CourtGeometry.GRAVITY_METRES_PER_SECOND_SQUARED;
        double v2 = speed * speed;
        double disc = v2 * v2 - gravity * (gravity * range * range + 2.0 * height * v2);
        if (disc <= 0.0) {
            return highArc ? Math.toRadians(55.0) : Math.toRadians(32.0);
        }
        double root = Math.sqrt(disc);
        double numerator = highArc ? v2 + root : v2 - root;
        return Math.atan(numerator / (gravity * range));
    }

    private static Probe maxRange(Probe left, Probe right) {
        return left.range >= right.range ? left : right;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private BallState launchState(
            Vector3 launchPoint,
            double speed,
            Vector3 spin,
            Vector3 horizontalDirection,
            double elevationRadians) {
        double horizontalSpeed = Math.cos(elevationRadians) * speed;
        double verticalSpeed = Math.sin(elevationRadians) * speed;
        Vector3 velocity =
                new Vector3(
                        horizontalDirection.x() * horizontalSpeed,
                        horizontalDirection.y() * horizontalSpeed,
                        verticalSpeed);
        return new BallState(0.0, launchPoint, velocity, spin);
    }

    private Vector3 spinVector(double spinRate, Vector3 horizontalDirection, SpinType spinType) {
        if (spinType == SpinType.FLAT || spinRate == 0.0) {
            return Vector3.ZERO;
        }
        Vector3 axis = UP.cross(horizontalDirection).normalized();
        double sign = spinType == SpinType.TOPSPIN ? 1.0 : -1.0;
        return axis.scale(spinRate * sign);
    }

    /** Outcome of an inverse solve. */
    public record LaunchSolution(
            BallState launchState,
            Vector3 predictedLanding,
            double launchElevationRadians,
            boolean reachedTarget) {}

    private record Probe(
            double angle, double range, double landingX, double landingY, boolean clearsNet) {}
}
