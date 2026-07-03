package dev.sahilbasumatary.replayservice.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sahilbasumatary.replayservice.domain.Surface;
import java.util.List;
import org.junit.jupiter.api.Test;

class BallPhysicsSimulatorTest {

    private static final double STEP_SECONDS = 0.002;
    private static final double MAX_FLIGHT_SECONDS = 6.0;

    private final BallPhysicsSimulator simulator = new BallPhysicsSimulator();

    @Test
    void gravityActsDownwardWhenStationary() {
        Vector3 acceleration = simulator.acceleration(Vector3.ZERO, Vector3.ZERO);

        assertEquals(0.0, acceleration.x(), 1.0e-9);
        assertEquals(0.0, acceleration.y(), 1.0e-9);
        assertEquals(
                -CourtGeometry.GRAVITY_METRES_PER_SECOND_SQUARED, acceleration.z(), 1.0e-9);
    }

    @Test
    void dragShortensRange() {
        BallState launch = launch(30.0, Math.toRadians(20), Vector3.ZERO);
        BounceProfile profile = BounceProfile.forSurface(Surface.HARD);

        BallPhysicsSimulator withoutDrag =
                new BallPhysicsSimulator(
                        CourtGeometry.BALL_MASS_KG,
                        CourtGeometry.GRAVITY_METRES_PER_SECOND_SQUARED,
                        CourtGeometry.BALL_RADIUS_METRES,
                        0.0,
                        0.0,
                        CourtGeometry.ballCrossSectionalAreaSquareMetres());

        double dragRange =
                simulator.landingPoint(launch, profile, STEP_SECONDS, MAX_FLIGHT_SECONDS).x();
        double vacuumRange =
                withoutDrag.landingPoint(launch, profile, STEP_SECONDS, MAX_FLIGHT_SECONDS).x();

        assertTrue(
                dragRange < vacuumRange,
                "drag range " + dragRange + " should be shorter than vacuum range " + vacuumRange);
    }

    @Test
    void topspinDipsAndBackspinFloats() {
        double spinRate = 300.0;
        BallState topspin = launch(28.0, Math.toRadians(18), new Vector3(0, spinRate, 0));
        BallState backspin = launch(28.0, Math.toRadians(18), new Vector3(0, -spinRate, 0));
        BounceProfile profile = BounceProfile.forSurface(Surface.HARD);

        double topspinRange =
                simulator.landingPoint(topspin, profile, STEP_SECONDS, MAX_FLIGHT_SECONDS).x();
        double backspinRange =
                simulator.landingPoint(backspin, profile, STEP_SECONDS, MAX_FLIGHT_SECONDS).x();

        assertTrue(
                topspinRange < backspinRange,
                "topspin range " + topspinRange + " should be shorter than backspin " + backspinRange);
    }

    @Test
    void clayBouncesHigherThanGrass() {
        BallState launch = launch(22.0, Math.toRadians(22), Vector3.ZERO);

        double clayApex = apexAfterBounce(launch, Surface.CLAY);
        double grassApex = apexAfterBounce(launch, Surface.GRASS);

        assertTrue(
                clayApex > grassApex,
                "clay apex " + clayApex + " should exceed grass apex " + grassApex);
    }

    @Test
    void grassPreservesMorePaceThanClay() {
        BallState launch = launch(26.0, Math.toRadians(12), Vector3.ZERO);

        double grassPace = horizontalSpeedAfterBounce(launch, Surface.GRASS);
        double clayPace = horizontalSpeedAfterBounce(launch, Surface.CLAY);

        assertTrue(
                grassPace > clayPace,
                "grass pace " + grassPace + " should exceed clay pace " + clayPace);
    }

    private double apexAfterBounce(BallState launch, Surface surface) {
        List<BallState> path =
                simulator.simulate(
                        launch,
                        BounceProfile.forSurface(surface),
                        STEP_SECONDS,
                        MAX_FLIGHT_SECONDS,
                        false);
        int bounce = firstBounceIndex(path);
        double apex = 0.0;
        for (int index = bounce + 1; index < path.size(); index++) {
            apex = Math.max(apex, path.get(index).position().z());
        }
        return apex;
    }

    private double horizontalSpeedAfterBounce(BallState launch, Surface surface) {
        List<BallState> path =
                simulator.simulate(
                        launch,
                        BounceProfile.forSurface(surface),
                        STEP_SECONDS,
                        MAX_FLIGHT_SECONDS,
                        false);
        int bounce = firstBounceIndex(path);
        return path.get(bounce + 1).velocity().horizontalMagnitude();
    }

    private int firstBounceIndex(List<BallState> path) {
        for (int index = 1; index < path.size(); index++) {
            if (path.get(index).position().z() <= 1.0e-6
                    && path.get(index - 1).position().z() > 1.0e-6) {
                return index;
            }
        }
        return path.size() - 1;
    }

    private BallState launch(double speed, double elevationRadians, Vector3 spin) {
        Vector3 velocity =
                new Vector3(
                        speed * Math.cos(elevationRadians),
                        0.0,
                        speed * Math.sin(elevationRadians));
        return new BallState(0.0, new Vector3(0, 0, 1.0), velocity, spin);
    }
}
