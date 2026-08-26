package dev.sahilbasumatary.perf;

import dev.sahilbasumatary.replayservice.config.ReplayEngineProperties;
import dev.sahilbasumatary.replayservice.domain.PlayerSide;
import dev.sahilbasumatary.replayservice.domain.PlayerTier;
import dev.sahilbasumatary.replayservice.domain.ShotType;
import dev.sahilbasumatary.replayservice.domain.Surface;
import dev.sahilbasumatary.replayservice.dto.response.ReplayFrame;
import dev.sahilbasumatary.replayservice.physics.BallPhysicsSimulator;
import dev.sahilbasumatary.replayservice.physics.BallState;
import dev.sahilbasumatary.replayservice.physics.BounceProfile;
import dev.sahilbasumatary.replayservice.physics.LaunchSolver;
import dev.sahilbasumatary.replayservice.physics.Vector3;
import dev.sahilbasumatary.replayservice.trajectory.FrameAssembler;
import dev.sahilbasumatary.replayservice.trajectory.PointTrajectory;
import dev.sahilbasumatary.replayservice.trajectory.ReplayEngineFixtures;
import dev.sahilbasumatary.replayservice.trajectory.ShotDistributionIndex;
import dev.sahilbasumatary.replayservice.trajectory.ShotSampler;
import dev.sahilbasumatary.replayservice.trajectory.TrajectoryEngine;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(
        value = 1,
        jvmArgsAppend = {"-Xms512m", "-Xmx512m"})
@State(Scope.Thread)
public class ReplayPhysicsBenchmark {

    private BallPhysicsSimulator simulator;
    private LaunchSolver launchSolver;
    private TrajectoryEngine engine;
    private FrameAssembler assembler;
    private ReplayEngineProperties properties;
    private ShotDistributionIndex index;
    private BounceProfile hard;
    private BallState serveLaunch;
    private PointTrajectory point;
    private List<ReplayFrame> assembled;

    @Setup
    public void setup() {
        simulator = new BallPhysicsSimulator();
        launchSolver = new LaunchSolver(simulator);
        engine = new TrajectoryEngine(simulator, launchSolver, new ShotSampler());
        assembler = new FrameAssembler();
        properties = ReplayEngineFixtures.productionShapedEngine();
        index = ReplayEngineFixtures.hardCourtIndex();
        hard = BounceProfile.forSurface(Surface.HARD);
        serveLaunch =
                new BallState(
                        0.0,
                        new Vector3(0, -11.485, 2.65),
                        new Vector3(0, 38.0, 12.0),
                        new Vector3(0, 250.0, 0));
        point = generatePoint(1, ReplayEngineFixtures.POINT_SEED);
        assembled = assembler.framesForPoint(point, 0.0, properties.framesPerSecond());
    }

    @Benchmark
    public BallState rk4Step() {
        return simulator.integrate(serveLaunch, 0.002);
    }

    @Benchmark
    public LaunchSolver.LaunchSolution launchSolve() {
        return solveServe();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public LaunchSolver.LaunchSolution launchSolveSample() {
        return solveServe();
    }

    @Benchmark
    public List<ReplayFrame> assemblerOnly(Blackhole blackhole) {
        List<ReplayFrame> frames =
                assembler.framesForPoint(point, 0.0, properties.framesPerSecond());
        blackhole.consume(frames.size());
        return frames;
    }

    @Benchmark
    public List<ReplayFrame> fullPointPipeline(Blackhole blackhole) {
        return runFullPointPipeline(blackhole);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public List<ReplayFrame> fullPointPipelineSample(Blackhole blackhole) {
        return runFullPointPipeline(blackhole);
    }

    @Benchmark
    public int fullMatchPipeline(Blackhole blackhole) {
        int frames = 0;
        double cursor = 0.0;
        for (int sequence = 1; sequence <= 12; sequence++) {
            long seed = ReplayEngineFixtures.MATCH_SEED ^ ((long) sequence * 0x9E3779B97F4A7C15L);
            PointTrajectory trajectory = generatePoint(sequence, seed);
            List<ReplayFrame> assembledFrames =
                    assembler.framesForPoint(trajectory, cursor, properties.framesPerSecond());
            frames += assembledFrames.size();
            cursor += trajectory.durationSeconds();
        }
        blackhole.consume(frames);
        return frames;
    }

    public int assembledFrameCount() {
        return assembled.size();
    }

    private LaunchSolver.LaunchSolution solveServe() {
        return launchSolver.solve(
                serveLaunch.position(),
                48.0,
                250.0,
                dev.sahilbasumatary.replayservice.domain.SpinType.TOPSPIN,
                new Vector3(1.0, 8.35, 0),
                hard,
                false,
                48,
                0.05,
                0.004,
                6.0);
    }

    private List<ReplayFrame> runFullPointPipeline(Blackhole blackhole) {
        PointTrajectory trajectory = generatePoint(1, ReplayEngineFixtures.POINT_SEED);
        List<ReplayFrame> frames =
                assembler.framesForPoint(trajectory, 0.0, properties.framesPerSecond());
        blackhole.consume(frames.size());
        return frames;
    }

    private PointTrajectory generatePoint(int sequence, long seed) {
        List<ShotType> rally = ReplayEngineFixtures.sixShotRally();
        return engine.generate(
                sequence,
                PlayerSide.HOME,
                rally,
                PlayerTier.OTHER,
                PlayerTier.OTHER,
                Surface.HARD,
                index,
                seed,
                properties);
    }
}
