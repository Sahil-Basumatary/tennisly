package dev.sahilbasumatary.replayservice.physics;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sahilbasumatary.replayservice.domain.SpinType;
import dev.sahilbasumatary.replayservice.domain.Surface;
import org.junit.jupiter.api.Test;

class LaunchSolverTest {

    private static final double STEP_SECONDS = 0.004;
    private static final double MAX_FLIGHT_SECONDS = 6.0;
    private static final int MAX_ITERATIONS = 48;
    private static final double TOLERANCE_METRES = 0.05;

    private final BallPhysicsSimulator simulator = new BallPhysicsSimulator();
    private final LaunchSolver solver = new LaunchSolver(simulator);

    @Test
    void flatDriveLandsNearTarget() {
        Vector3 launch = new Vector3(0, -11.0, 0.95);
        Vector3 target = new Vector3(1.5, 8.0, 0);

        LaunchSolver.LaunchSolution solution =
                solver.solve(
                        launch,
                        33.0,
                        280.0,
                        SpinType.TOPSPIN,
                        target,
                        BounceProfile.forSurface(Surface.HARD),
                        false,
                        MAX_ITERATIONS,
                        TOLERANCE_METRES,
                        STEP_SECONDS,
                        MAX_FLIGHT_SECONDS);

        double error = solution.predictedLanding().subtract(target).horizontalMagnitude();
        assertTrue(error < 0.15, "landing error " + error + "m should be small");
    }

    @Test
    void highArcShotClearsHigherThanFlatShot() {
        Vector3 launch = new Vector3(0, -10.0, 0.95);
        Vector3 target = new Vector3(0.0, 4.0, 0);
        BounceProfile profile = BounceProfile.forSurface(Surface.HARD);

        LaunchSolver.LaunchSolution flat =
                solver.solve(
                        launch,
                        20.0,
                        150.0,
                        SpinType.TOPSPIN,
                        target,
                        profile,
                        false,
                        MAX_ITERATIONS,
                        TOLERANCE_METRES,
                        STEP_SECONDS,
                        MAX_FLIGHT_SECONDS);
        LaunchSolver.LaunchSolution lob =
                solver.solve(
                        launch,
                        20.0,
                        150.0,
                        SpinType.BACKSPIN,
                        target,
                        profile,
                        true,
                        MAX_ITERATIONS,
                        TOLERANCE_METRES,
                        STEP_SECONDS,
                        MAX_FLIGHT_SECONDS);

        assertTrue(
                lob.launchElevationRadians() > flat.launchElevationRadians(),
                "lob should launch at a steeper angle than the flat drive");
    }

    @Test
    void shortCrosscourtDriveClearsTheNet() {
        Vector3 launch = new Vector3(-2.0, -9.5, 1.20);
        Vector3 target = new Vector3(3.4, 5.0, 0);
        LaunchSolver.LaunchSolution solution =
                solver.solve(
                        launch,
                        28.0,
                        2100.0,
                        SpinType.TOPSPIN,
                        target,
                        BounceProfile.forSurface(Surface.CLAY),
                        false,
                        MAX_ITERATIONS,
                        TOLERANCE_METRES,
                        STEP_SECONDS,
                        MAX_FLIGHT_SECONDS);
        BallPathBuffer path = new BallPathBuffer();
        simulator.simulateInto(
                solution.launchState(),
                BounceProfile.forSurface(Surface.CLAY),
                STEP_SECONDS,
                MAX_FLIGHT_SECONDS,
                false,
                path);
        assertTrue(clearsNet(path, launch.y(), target.y()), "short-angle drive clipped the net");
        assertTrue(
                solution.predictedLanding().subtract(target).horizontalMagnitude() < 0.30,
                "landing error " + solution.predictedLanding().subtract(target).horizontalMagnitude());
    }

    private static boolean clearsNet(BallPathBuffer path, double launchY, double targetY) {
        if (Math.signum(launchY) == Math.signum(targetY) || path.size() < 2) {
            return true;
        }
        for (int index = 1; index < path.size(); index++) {
            double y0 = path.y(index - 1);
            double y1 = path.y(index);
            if (y0 * y1 > 0.0) {
                continue;
            }
            double span = y1 - y0;
            double fraction = Math.abs(span) < 1.0e-9 ? 0.0 : (0.0 - y0) / span;
            double x = path.x(index - 1) + (path.x(index) - path.x(index - 1)) * fraction;
            double z = path.z(index - 1) + (path.z(index) - path.z(index - 1)) * fraction;
            if (z + 0.02 < CourtGeometry.netHeightAt(x)) {
                return false;
            }
        }
        return true;
    }
}
