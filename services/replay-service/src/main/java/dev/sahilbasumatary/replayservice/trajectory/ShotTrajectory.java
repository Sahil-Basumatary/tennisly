package dev.sahilbasumatary.replayservice.trajectory;

import dev.sahilbasumatary.replayservice.domain.PlayerSide;
import dev.sahilbasumatary.replayservice.domain.ShotType;
import dev.sahilbasumatary.replayservice.domain.SpinType;
import dev.sahilbasumatary.replayservice.physics.BallState;
import dev.sahilbasumatary.replayservice.physics.Vector3;
import java.util.List;

/**
 * The computed flight of one shot, in world coordinates, together with the player kinematics needed
 * to animate it. {@code samples} run from the moment of contact ({@code t = 0}) to the receiver's
 * next contact.
 */
public record ShotTrajectory(
        int shotIndex,
        ShotType shotType,
        PlayerSide hitterSide,
        SpinType spinType,
        List<BallState> samples,
        Vector3 contactPoint,
        Vector3 landingPoint,
        Vector3 nextContactPoint,
        Vector3 receiverStart,
        Vector3 receiverEnd,
        double apexHeightMetres,
        double launchSpeedMetresPerSecond,
        double flightSeconds) {}
