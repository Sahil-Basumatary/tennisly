package dev.sahilbasumatary.replayservice.trajectory;

import dev.sahilbasumatary.replayservice.client.dto.ShotDistributionModel;
import dev.sahilbasumatary.replayservice.config.ReplayEngineProperties;
import dev.sahilbasumatary.replayservice.domain.PlayerSide;
import dev.sahilbasumatary.replayservice.domain.PlayerTier;
import dev.sahilbasumatary.replayservice.domain.ShotType;
import dev.sahilbasumatary.replayservice.domain.Surface;
import dev.sahilbasumatary.replayservice.physics.BallPhysicsSimulator;
import dev.sahilbasumatary.replayservice.physics.BallState;
import dev.sahilbasumatary.replayservice.physics.BounceProfile;
import dev.sahilbasumatary.replayservice.physics.CourtGeometry;
import dev.sahilbasumatary.replayservice.physics.LaunchSolver;
import dev.sahilbasumatary.replayservice.physics.Vector3;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Component;

/**
 * Turns a point's shot sequence into a continuous, physically consistent rally. Each shot is solved
 * so it lands where the sampled distribution dictates, simulated with full aerodynamics, then
 * chained: where one shot reaches the opponent becomes the contact point of the next.
 */
@Component
public class TrajectoryEngine {

    private static final double BASELINE_REST_OFFSET_METRES = 0.90;
    private static final double RECEIVER_CONTACT_HEIGHT_METRES =
            CourtGeometry.GROUNDSTROKE_CONTACT_HEIGHT_METRES;

    private final BallPhysicsSimulator simulator;
    private final LaunchSolver launchSolver;
    private final ShotSampler shotSampler;

    public TrajectoryEngine(
            BallPhysicsSimulator simulator,
            LaunchSolver launchSolver,
            ShotSampler shotSampler) {
        this.simulator = simulator;
        this.launchSolver = launchSolver;
        this.shotSampler = shotSampler;
    }

    public PointTrajectory generate(
            int sequence,
            PlayerSide serverSide,
            List<ShotType> shotTypes,
            PlayerTier serverTier,
            PlayerTier receiverTier,
            Surface surface,
            ShotDistributionIndex index,
            long seed,
            ReplayEngineProperties engine) {
        RandomGenerator random = new SplittableRandom(seed);
        BounceProfile profile = BounceProfile.forSurface(surface);
        double baselineRest = CourtGeometry.HALF_LENGTH_METRES - BASELINE_REST_OFFSET_METRES;

        Vector3 homePosition = new Vector3(0, -baselineRest, 0);
        Vector3 awayPosition = new Vector3(0, baselineRest, 0);
        int serverSign = serverSide == PlayerSide.HOME ? 1 : -1;

        List<ShotTrajectory> shots = new ArrayList<>(shotTypes.size());
        Vector3 contactPoint = null;
        double totalDuration = 0.0;

        for (int shotIndex = 0; shotIndex < shotTypes.size(); shotIndex++) {
            ShotType shotType = shotTypes.get(shotIndex);
            int sign = serverSign * ((shotIndex % 2 == 0) ? 1 : -1);
            PlayerSide hitterSide = sign == 1 ? PlayerSide.HOME : PlayerSide.AWAY;
            PlayerTier tier = shotIndex % 2 == 0 ? serverTier : receiverTier;

            Vector3 launchPoint =
                    shotIndex == 0
                            ? serveLaunchPoint(sign)
                            : new Vector3(
                                    contactPoint.x(),
                                    contactPoint.y(),
                                    ShotKinematics.contactHeightMetres(shotType));

            if (hitterSide == PlayerSide.HOME) {
                homePosition = new Vector3(launchPoint.x(), launchPoint.y(), 0);
            } else {
                awayPosition = new Vector3(launchPoint.x(), launchPoint.y(), 0);
            }

            ShotDistributionModel model = index.resolve(shotType, tier);
            ShotParameters parameters = shotSampler.sample(shotType, model, random);
            Vector3 target =
                    new Vector3(
                            sign * parameters.landingLateralMetres(),
                            sign * parameters.landingDepthMetres(),
                            0);

            LaunchSolver.LaunchSolution solution =
                    launchSolver.solve(
                            launchPoint,
                            parameters.speedMetresPerSecond(),
                            parameters.spinRateRadiansPerSecond(),
                            parameters.spinType(),
                            target,
                            profile,
                            ShotKinematics.needsHighArc(shotType),
                            engine.solverMaxIterations(),
                            engine.solverToleranceMetres(),
                            engine.solverStepSeconds(),
                            engine.maxFlightSeconds());

            List<BallState> fullPath =
                    simulator.simulate(
                            solution.launchState(),
                            profile,
                            engine.integrationStepSeconds(),
                            engine.maxFlightSeconds(),
                            false);

            int bounceIndex = firstBounceIndex(fullPath);
            Vector3 landing = fullPath.get(bounceIndex).position();
            int contactIndex = nextContactIndex(fullPath, bounceIndex, RECEIVER_CONTACT_HEIGHT_METRES);
            List<BallState> samples = new ArrayList<>(fullPath.subList(0, contactIndex + 1));

            Vector3 nextContact =
                    clampToCourt(
                            new Vector3(
                                    samples.get(samples.size() - 1).position().x(),
                                    samples.get(samples.size() - 1).position().y(),
                                    0));

            PlayerSide receiverSide =
                    hitterSide == PlayerSide.HOME ? PlayerSide.AWAY : PlayerSide.HOME;
            Vector3 receiverStart =
                    receiverSide == PlayerSide.HOME ? homePosition : awayPosition;

            double flightSeconds = samples.get(samples.size() - 1).timeSeconds();
            shots.add(
                    new ShotTrajectory(
                            shotIndex,
                            shotType,
                            hitterSide,
                            parameters.spinType(),
                            samples,
                            launchPoint,
                            landing,
                            nextContact,
                            receiverStart,
                            nextContact,
                            apexHeight(samples),
                            solution.launchState().velocity().magnitude(),
                            flightSeconds));

            if (receiverSide == PlayerSide.HOME) {
                homePosition = nextContact;
            } else {
                awayPosition = nextContact;
            }
            contactPoint = nextContact;
            totalDuration += flightSeconds;
        }

        return new PointTrajectory(sequence, shots, totalDuration);
    }

    private Vector3 serveLaunchPoint(int sign) {
        double behindBaseline = CourtGeometry.HALF_LENGTH_METRES - 0.40;
        return new Vector3(0, -sign * behindBaseline, CourtGeometry.SERVE_CONTACT_HEIGHT_METRES);
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

    private int nextContactIndex(List<BallState> path, int bounceIndex, double contactHeight) {
        if (bounceIndex >= path.size() - 1) {
            return path.size() - 1;
        }
        double depthCap = CourtGeometry.HALF_LENGTH_METRES + 0.80;
        for (int index = bounceIndex + 1; index < path.size(); index++) {
            BallState sample = path.get(index);
            boolean reachedBaseline = Math.abs(sample.position().y()) >= depthCap;
            boolean reachableOnDescent =
                    sample.velocity().z() < 0.0 && sample.position().z() <= contactHeight;
            if (reachedBaseline || reachableOnDescent) {
                return index;
            }
        }
        return path.size() - 1;
    }

    private double apexHeight(List<BallState> samples) {
        double apex = 0.0;
        for (BallState sample : samples) {
            apex = Math.max(apex, sample.position().z());
        }
        return apex;
    }

    private Vector3 clampToCourt(Vector3 position) {
        double depthBound = CourtGeometry.HALF_LENGTH_METRES + 1.5;
        double widthBound = CourtGeometry.DOUBLES_HALF_WIDTH_METRES + 1.0;
        double clampedX = Math.max(-widthBound, Math.min(widthBound, position.x()));
        double clampedY = Math.max(-depthBound, Math.min(depthBound, position.y()));
        return new Vector3(clampedX, clampedY, 0);
    }
}
