package dev.sahilbasumatary.replayservice.trajectory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sahilbasumatary.replayservice.config.ReplayEngineProperties;
import dev.sahilbasumatary.replayservice.domain.PlayerSide;
import dev.sahilbasumatary.replayservice.domain.PlayerTier;
import dev.sahilbasumatary.replayservice.domain.ShotType;
import dev.sahilbasumatary.replayservice.domain.Surface;
import dev.sahilbasumatary.replayservice.physics.BallPhysicsSimulator;
import dev.sahilbasumatary.replayservice.physics.CourtGeometry;
import dev.sahilbasumatary.replayservice.physics.LaunchSolver;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrajectoryEngineTest {

    private static final long SEED = 123_456L;

    private final BallPhysicsSimulator simulator = new BallPhysicsSimulator();
    private final TrajectoryEngine engine =
            new TrajectoryEngine(simulator, new LaunchSolver(simulator), new ShotSampler());
    private final ReplayEngineProperties properties =
            new ReplayEngineProperties(60, 0.004, 6.0, 48, 0.1, 40);

    private final List<ShotType> shotTypes =
            List.of(
                    ShotType.FIRST_SERVE,
                    ShotType.FOREHAND_GROUNDSTROKE,
                    ShotType.BACKHAND_GROUNDSTROKE);

    private ShotDistributionIndex index() {
        return ShotDistributionIndex.from(
                List.of(
                        ShotModels.serve(),
                        ShotModels.groundstroke(ShotType.FOREHAND_GROUNDSTROKE),
                        ShotModels.groundstroke(ShotType.BACKHAND_GROUNDSTROKE)));
    }

    @Test
    void generatesOneTrajectoryPerShotWithFrames() {
        PointTrajectory trajectory =
                engine.generate(
                        1,
                        PlayerSide.HOME,
                        shotTypes,
                        PlayerTier.OTHER,
                        PlayerTier.OTHER,
                        Surface.HARD,
                        index(),
                        SEED,
                        properties);

        assertEquals(shotTypes.size(), trajectory.shots().size());
        assertTrue(trajectory.durationSeconds() > 0.0);
        trajectory
                .shots()
                .forEach(shot -> assertFalse(shot.samples().isEmpty(), "shot must have samples"));
    }

    @Test
    void ballStaysWithinPlayableBounds() {
        PointTrajectory trajectory =
                engine.generate(
                        1,
                        PlayerSide.HOME,
                        shotTypes,
                        PlayerTier.OTHER,
                        PlayerTier.OTHER,
                        Surface.HARD,
                        index(),
                        SEED,
                        properties);

        trajectory
                .shots()
                .forEach(
                        shot ->
                                shot.samples()
                                        .forEach(
                                                sample -> {
                                                    assertTrue(
                                                            sample.position().z() >= -0.01,
                                                            "ball should not sink below the court");
                                                    assertTrue(
                                                            sample.position().z() < 20.0,
                                                            "ball height should be realistic");
                                                    assertTrue(
                                                            Math.abs(sample.position().y())
                                                                    < CourtGeometry
                                                                                    .HALF_LENGTH_METRES
                                                                            + 3.0,
                                                            "ball should stay near the court");
                                                }));
    }

    @Test
    void generationIsDeterministicForSameSeed() {
        PointTrajectory first =
                engine.generate(
                        1,
                        PlayerSide.HOME,
                        shotTypes,
                        PlayerTier.OTHER,
                        PlayerTier.OTHER,
                        Surface.HARD,
                        index(),
                        SEED,
                        properties);
        PointTrajectory second =
                engine.generate(
                        1,
                        PlayerSide.HOME,
                        shotTypes,
                        PlayerTier.OTHER,
                        PlayerTier.OTHER,
                        Surface.HARD,
                        index(),
                        SEED,
                        properties);

        assertEquals(
                first.shots().get(2).landingPoint(), second.shots().get(2).landingPoint());
    }
}
