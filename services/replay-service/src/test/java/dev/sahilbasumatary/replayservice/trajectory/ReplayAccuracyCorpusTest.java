package dev.sahilbasumatary.replayservice.trajectory;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sahilbasumatary.replayservice.config.ReplayEngineProperties;
import dev.sahilbasumatary.replayservice.physics.BallPathBuffer;
import dev.sahilbasumatary.replayservice.physics.BallPhysicsSimulator;
import dev.sahilbasumatary.replayservice.physics.BounceProfile;
import dev.sahilbasumatary.replayservice.physics.CourtGeometry;
import dev.sahilbasumatary.replayservice.physics.LaunchSolver;
import dev.sahilbasumatary.replayservice.physics.Vector3;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReplayAccuracyCorpusTest {

    private static final double MEAN_BUDGET_METRES = 0.05;
    private static final double P99_BUDGET_METRES = 0.10;
    private static final double MAX_BUDGET_METRES = 0.30;

    private final BallPhysicsSimulator simulator = new BallPhysicsSimulator();
    private final LaunchSolver solver = new LaunchSolver(simulator);
    private final ReplayEngineProperties engine = ReplayEngineFixtures.productionShapedEngine();

    @Test
    void highResolutionLandingsStayInsideAccuracyBudget() {
        List<ReplayEngineFixtures.AccuracyCase> corpus = ReplayEngineFixtures.accuracyCorpus();
        double[] errors = new double[corpus.size()];
        List<String> netViolations = new ArrayList<>();
        List<String> contactViolations = new ArrayList<>();
        List<String> namedErrors = new ArrayList<>();
        for (int index = 0; index < corpus.size(); index++) {
            ReplayEngineFixtures.AccuracyCase testCase = corpus.get(index);
            BounceProfile profile = BounceProfile.forSurface(testCase.surface());
            LaunchSolver.LaunchSolution solution =
                    solver.solve(
                            testCase.launch(),
                            testCase.speed(),
                            testCase.spinRate(),
                            testCase.spinType(),
                            testCase.target(),
                            profile,
                            testCase.highArc(),
                            engine.solverMaxIterations(),
                            engine.solverToleranceMetres(),
                            engine.integrationStepSeconds(),
                            engine.maxFlightSeconds());
            Vector3 reference =
                    simulator.landingPoint(
                            solution.launchState(),
                            profile,
                            ReplayEngineFixtures.REFERENCE_STEP_SECONDS,
                            engine.maxFlightSeconds());
            errors[index] = reference.subtract(testCase.target()).horizontalMagnitude();
            BallPathBuffer path = new BallPathBuffer();
            // Bounce is an impulse; continuity is scored on the solved flight to first contact.
            simulator.simulateInto(
                    solution.launchState(),
                    profile,
                    engine.integrationStepSeconds(),
                    engine.maxFlightSeconds(),
                    true,
                    path);
            if (clipsNet(path, testCase.launch().y(), testCase.target().y())) {
                netViolations.add(testCase.name());
            }
            if (hasDiscontinuousContact(path)) {
                contactViolations.add(testCase.name());
            }
            namedErrors.add(testCase.name() + "=" + errors[index]);
        }
        Arrays.sort(errors);
        double sum = 0.0;
        for (double error : errors) {
            sum += error;
        }
        double mean = sum / errors.length;
        double p99 = errors[Math.min(errors.length - 1, (int) Math.ceil(errors.length * 0.99) - 1)];
        double max = errors[errors.length - 1];
        namedErrors.sort(
                (a, b) ->
                        Double.compare(
                                Double.parseDouble(b.substring(b.indexOf('=') + 1)),
                                Double.parseDouble(a.substring(a.indexOf('=') + 1))));
        String summary =
                "mean="
                        + mean
                        + " p99="
                        + p99
                        + " max="
                        + max
                        + " n="
                        + errors.length
                        + " worst="
                        + namedErrors.subList(0, Math.min(8, namedErrors.size()));
        assertTrue(mean <= MEAN_BUDGET_METRES, summary + " mean exceeds " + MEAN_BUDGET_METRES + "m");
        assertTrue(p99 <= P99_BUDGET_METRES, summary + " p99 exceeds " + P99_BUDGET_METRES + "m");
        assertTrue(max <= MAX_BUDGET_METRES, summary + " max exceeds " + MAX_BUDGET_METRES + "m");
        assertTrue(netViolations.isEmpty(), "net violations: " + netViolations);
        assertTrue(contactViolations.isEmpty(), "discontinuous contacts: " + contactViolations);
    }

    private static boolean clipsNet(BallPathBuffer path, double launchY, double targetY) {
        if (Math.signum(launchY) == Math.signum(targetY) || path.size() < 2) {
            return false;
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
                return true;
            }
        }
        return false;
    }

    private static boolean hasDiscontinuousContact(BallPathBuffer path) {
        for (int index = 1; index < path.size(); index++) {
            double dt = path.time(index) - path.time(index - 1);
            if (dt <= 0.0) {
                return true;
            }
            double dx = path.x(index) - path.x(index - 1);
            double dy = path.y(index) - path.y(index - 1);
            double dz = path.z(index) - path.z(index - 1);
            double speed = Math.sqrt(dx * dx + dy * dy + dz * dz) / dt;
            if (speed > 80.0) {
                return true;
            }
        }
        return false;
    }
}
