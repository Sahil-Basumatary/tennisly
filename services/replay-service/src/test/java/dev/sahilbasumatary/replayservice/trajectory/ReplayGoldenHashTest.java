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

class ReplayGoldenHashTest {

    static final String GOLDEN_POINT_FRAMES =
            "d9c930aed61c4e0d161a9ee34e7ad69b2c1dff45379888ac4be304e48ef2cb94";
    static final String GOLDEN_POINT_SHOTS =
            "e2b5b5fb056ce5610ff9cfb4b87c0e06a3506fbc985d864f1225436aa6f7b281";
    static final String GOLDEN_MATCH_FRAMES =
            "be0023794100e9e82d2f3b9d9d8fd22fe9d80a05657c805639c2308f18b3a8ad";
    static final int GOLDEN_POINT_FRAME_COUNT = 363;
    static final int GOLDEN_MATCH_FRAME_COUNT = 4340;

    private final ReplayEngineProperties properties = ReplayEngineFixtures.productionShapedEngine();
    private final BallPhysicsSimulator simulator = new BallPhysicsSimulator();
    private final TrajectoryEngine engine =
            new TrajectoryEngine(simulator, new LaunchSolver(simulator), new ShotSampler());
    private final FrameAssembler assembler = new FrameAssembler();
    private final ShotDistributionIndex index = ReplayEngineFixtures.hardCourtIndex();

    @Test
    void sixShotPointHashIsBitIdentical() {
        PointTrajectory trajectory = sixShotPoint();
        List<ReplayFrame> frames =
                assembler.framesForPoint(trajectory, 0.0, properties.framesPerSecond());
        String frameHash = ReplayCanonicalHash.sha256Frames(frames);
        String shotHash = ReplayCanonicalHash.sha256Shots(assembler.shotSummaries(trajectory));
        assertEquals(GOLDEN_POINT_FRAME_COUNT, frames.size(), "point frames=" + frames.size() + " hash=" + frameHash);
        assertEquals(GOLDEN_POINT_FRAMES, frameHash, "point hash");
        assertEquals(GOLDEN_POINT_SHOTS, shotHash, "point shots hash shots=" + trajectory.shots().size());
    }

    @Test
    void twelvePointMatchHashIsBitIdentical() {
        List<ReplayFrame> frames = twelvePointMatch();
        String hash = ReplayCanonicalHash.sha256Frames(frames);
        assertEquals(GOLDEN_MATCH_FRAME_COUNT, frames.size(), "match frames=" + frames.size() + " hash=" + hash);
        assertEquals(GOLDEN_MATCH_FRAMES, hash);
    }

    private PointTrajectory sixShotPoint() {
        return engine.generate(
                1,
                PlayerSide.HOME,
                ReplayEngineFixtures.sixShotRally(),
                PlayerTier.OTHER,
                PlayerTier.OTHER,
                Surface.HARD,
                index,
                ReplayEngineFixtures.POINT_SEED,
                properties);
    }

    private List<ReplayFrame> twelvePointMatch() {
        List<ReplayFrame> frames = new ArrayList<>();
        double cursor = 0.0;
        for (int sequence = 1; sequence <= 12; sequence++) {
            long seed = ReplayEngineFixtures.MATCH_SEED ^ ((long) sequence * 0x9E3779B97F4A7C15L);
            PointTrajectory trajectory =
                    engine.generate(
                            sequence,
                            sequence % 2 == 0 ? PlayerSide.AWAY : PlayerSide.HOME,
                            ReplayEngineFixtures.sixShotRally(),
                            PlayerTier.OTHER,
                            PlayerTier.OTHER,
                            Surface.HARD,
                            index,
                            seed,
                            properties);
            frames.addAll(
                    assembler.framesForPoint(trajectory, cursor, properties.framesPerSecond()));
            cursor += trajectory.durationSeconds();
        }
        return frames;
    }
}
