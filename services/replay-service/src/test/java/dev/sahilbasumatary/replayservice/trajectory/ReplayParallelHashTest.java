package dev.sahilbasumatary.replayservice.trajectory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.sahilbasumatary.replayservice.config.ReplayEngineProperties;
import dev.sahilbasumatary.replayservice.domain.PlayerSide;
import dev.sahilbasumatary.replayservice.domain.PlayerTier;
import dev.sahilbasumatary.replayservice.domain.Surface;
import dev.sahilbasumatary.replayservice.dto.response.ReplayFrame;
import dev.sahilbasumatary.replayservice.physics.BallPhysicsSimulator;
import dev.sahilbasumatary.replayservice.physics.LaunchSolver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReplayParallelHashTest {

    private final ReplayEngineProperties properties = ReplayEngineFixtures.productionShapedEngine();
    private final BallPhysicsSimulator simulator = new BallPhysicsSimulator();
    private final TrajectoryEngine engine =
            new TrajectoryEngine(simulator, new LaunchSolver(simulator), new ShotSampler());
    private final FrameAssembler assembler = new FrameAssembler();
    private final ShotDistributionIndex index = ReplayEngineFixtures.hardCourtIndex();

    @Test
    void oneWorkerAndFourWorkersShareTheMatchFingerprint() {
        String sequential = ReplayCanonicalHash.sha256Frames(assemble(1));
        String parallel = ReplayCanonicalHash.sha256Frames(assemble(4));
        assertEquals(ReplayGoldenHashTest.GOLDEN_MATCH_FRAMES, sequential);
        assertEquals(sequential, parallel);
    }

    private List<ReplayFrame> assemble(int workers) {
        List<PointTrajectory> trajectories = OrderedParallel.map(12, workers, this::pointAt);
        List<ReplayFrame> frames = new ArrayList<>();
        double cursor = 0.0;
        for (PointTrajectory trajectory : trajectories) {
            frames.addAll(
                    assembler.framesForPoint(trajectory, cursor, properties.framesPerSecond()));
            cursor += trajectory.durationSeconds();
        }
        return frames;
    }

    private PointTrajectory pointAt(int slot) {
        int sequence = slot + 1;
        long seed = ReplayEngineFixtures.MATCH_SEED ^ ((long) sequence * 0x9E3779B97F4A7C15L);
        return engine.generate(
                sequence,
                sequence % 2 == 0 ? PlayerSide.AWAY : PlayerSide.HOME,
                ReplayEngineFixtures.sixShotRally(),
                PlayerTier.OTHER,
                PlayerTier.OTHER,
                Surface.HARD,
                index,
                seed,
                properties);
    }
}
