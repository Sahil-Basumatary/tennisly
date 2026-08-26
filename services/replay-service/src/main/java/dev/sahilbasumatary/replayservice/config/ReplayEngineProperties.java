package dev.sahilbasumatary.replayservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable parameters for the trajectory engine. Defaults are sensible for broadcast-quality
 * replays; lowering the frame rate or coarsening the integration step trades fidelity for
 * generation speed.
 */
@ConfigurationProperties(prefix = "replay.engine")
public record ReplayEngineProperties(
        int framesPerSecond,
        double integrationStepSeconds,
        double maxFlightSeconds,
        int solverMaxIterations,
        double solverToleranceMetres,
        int maxRallyLength,
        int pointWorkers) {

    public ReplayEngineProperties(
            int framesPerSecond,
            double integrationStepSeconds,
            double maxFlightSeconds,
            int solverMaxIterations,
            double solverToleranceMetres,
            int maxRallyLength) {
        this(
                framesPerSecond,
                integrationStepSeconds,
                maxFlightSeconds,
                solverMaxIterations,
                solverToleranceMetres,
                maxRallyLength,
                0);
    }

    public ReplayEngineProperties {
        if (framesPerSecond <= 0) {
            framesPerSecond = 60;
        }
        if (integrationStepSeconds <= 0) {
            integrationStepSeconds = 0.002;
        }
        if (maxFlightSeconds <= 0) {
            maxFlightSeconds = 6.0;
        }
        if (solverMaxIterations <= 0) {
            solverMaxIterations = 48;
        }
        if (solverToleranceMetres <= 0) {
            solverToleranceMetres = 0.05;
        }
        if (maxRallyLength <= 0) {
            maxRallyLength = 40;
        }
        if (pointWorkers < 0) {
            pointWorkers = 0;
        }
    }

    public double solverStepSeconds() {
        return Math.max(integrationStepSeconds * 2.0, 0.004);
    }
}
