package dev.sahilbasumatary.replayservice.trajectory;

import java.util.List;

/** All shot trajectories that make up a single point, in play order. */
public record PointTrajectory(int sequence, List<ShotTrajectory> shots, double durationSeconds) {}
