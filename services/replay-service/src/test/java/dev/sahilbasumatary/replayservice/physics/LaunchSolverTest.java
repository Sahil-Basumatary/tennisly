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

        double error =
                solution
                        .predictedLanding()
                        .subtract(target)
                        .horizontalMagnitude();
        assertTrue(error < 0.6, "landing error " + error + "m should be small");
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
}
