package dev.sahilbasumatary.replayservice.physics;

import dev.sahilbasumatary.replayservice.domain.SpinType;
import org.springframework.stereotype.Component;

/**
 * Inverse trajectory solver: given a launch point, a struck speed and a desired landing spot, it
 * recovers the launch velocity vector that actually delivers the ball there under full aerodynamics.
 *
 * <p>Because drag makes the range/angle relationship non-analytic and non-monotonic, the solver
 * first coarsely scans elevation angles to locate the maximum-range angle, then bisects on the
 * appropriate branch: the flat (ascending) branch for drives and serves, or the steep (descending)
 * branch for lobs and drop shots that need a high arc.
 */
@Component
public class LaunchSolver {

    private static final Vector3 UP = new Vector3(0, 0, 1);
    private static final double SCAN_MIN_DEGREES = 2.0;
    private static final double SCAN_MAX_DEGREES = 75.0;
    private static final double SCAN_STEP_DEGREES = 2.0;

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
        Vector3 horizontalDirection =
                new Vector3(target.x() - launchPoint.x(), target.y() - launchPoint.y(), 0.0)
                        .normalized();
        double targetDistance =
                new Vector3(target.x() - launchPoint.x(), target.y() - launchPoint.y(), 0.0)
                        .magnitude();
        Vector3 spin = spinVector(spinRate, horizontalDirection, spinType);

        ScanResult scan =
                scanForPeak(
                        launchPoint,
                        speed,
                        spin,
                        horizontalDirection,
                        profile,
                        stepSeconds,
                        maxFlightSeconds);

        if (targetDistance >= scan.peakDistance()) {
            return buildSolution(
                    launchPoint,
                    speed,
                    spin,
                    horizontalDirection,
                    scan.peakAngleRadians(),
                    profile,
                    stepSeconds,
                    maxFlightSeconds,
                    false);
        }

        double lowAngle;
        double highAngle;
        if (highArc) {
            lowAngle = scan.peakAngleRadians();
            highAngle = Math.toRadians(SCAN_MAX_DEGREES);
        } else {
            lowAngle = Math.toRadians(SCAN_MIN_DEGREES);
            highAngle = scan.peakAngleRadians();
        }

        double chosenAngle =
                bisect(
                        launchPoint,
                        speed,
                        spin,
                        horizontalDirection,
                        target,
                        profile,
                        lowAngle,
                        highAngle,
                        targetDistance,
                        highArc,
                        maxIterations,
                        toleranceMetres,
                        stepSeconds,
                        maxFlightSeconds);
        return buildSolution(
                launchPoint,
                speed,
                spin,
                horizontalDirection,
                chosenAngle,
                profile,
                stepSeconds,
                maxFlightSeconds,
                true);
    }

    private ScanResult scanForPeak(
            Vector3 launchPoint,
            double speed,
            Vector3 spin,
            Vector3 horizontalDirection,
            BounceProfile profile,
            double stepSeconds,
            double maxFlightSeconds) {
        double peakDistance = -1.0;
        double peakAngle = Math.toRadians(SCAN_MIN_DEGREES);
        for (double degrees = SCAN_MIN_DEGREES;
                degrees <= SCAN_MAX_DEGREES;
                degrees += SCAN_STEP_DEGREES) {
            double radians = Math.toRadians(degrees);
            double distance =
                    landingDistance(
                            launchPoint,
                            speed,
                            spin,
                            horizontalDirection,
                            radians,
                            profile,
                            stepSeconds,
                            maxFlightSeconds);
            if (distance > peakDistance) {
                peakDistance = distance;
                peakAngle = radians;
            }
        }
        return new ScanResult(peakAngle, peakDistance);
    }

    private double bisect(
            Vector3 launchPoint,
            double speed,
            Vector3 spin,
            Vector3 horizontalDirection,
            Vector3 target,
            BounceProfile profile,
            double lowAngle,
            double highAngle,
            double targetDistance,
            boolean highArc,
            int maxIterations,
            double toleranceMetres,
            double stepSeconds,
            double maxFlightSeconds) {
        double low = lowAngle;
        double high = highAngle;
        double mid = (low + high) / 2.0;
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            mid = (low + high) / 2.0;
            double distance =
                    landingDistance(
                            launchPoint,
                            speed,
                            spin,
                            horizontalDirection,
                            mid,
                            profile,
                            stepSeconds,
                            maxFlightSeconds);
            double error = distance - targetDistance;
            if (Math.abs(error) <= toleranceMetres) {
                return mid;
            }
            boolean distanceIncreasesWithAngle = !highArc;
            if ((error < 0) == distanceIncreasesWithAngle) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return mid;
    }

    private double landingDistance(
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
        Vector3 landing =
                simulator.landingPoint(initial, profile, stepSeconds, maxFlightSeconds);
        return new Vector3(landing.x() - launchPoint.x(), landing.y() - launchPoint.y(), 0.0)
                .magnitude();
    }

    private LaunchSolution buildSolution(
            Vector3 launchPoint,
            double speed,
            Vector3 spin,
            Vector3 horizontalDirection,
            double elevationRadians,
            BounceProfile profile,
            double stepSeconds,
            double maxFlightSeconds,
            boolean reachedTarget) {
        BallState initial =
                launchState(launchPoint, speed, spin, horizontalDirection, elevationRadians);
        Vector3 landing =
                simulator.landingPoint(initial, profile, stepSeconds, maxFlightSeconds);
        return new LaunchSolution(initial, landing, elevationRadians, reachedTarget);
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

    private record ScanResult(double peakAngleRadians, double peakDistance) {}
}
