package dev.sahilbasumatary.replayservice.trajectory;

import dev.sahilbasumatary.replayservice.domain.PlayerSide;
import dev.sahilbasumatary.replayservice.domain.ShotType;
import dev.sahilbasumatary.replayservice.domain.SpinType;
import dev.sahilbasumatary.replayservice.physics.BallPathBuffer;
import dev.sahilbasumatary.replayservice.physics.BallState;
import dev.sahilbasumatary.replayservice.physics.Vector3;
import java.util.List;

/**
 * The computed flight of one shot, in world coordinates, together with the player kinematics needed
 * to animate it. {@code path} runs from the moment of contact ({@code t = 0}) to the receiver's
 * next contact.
 */
public record ShotTrajectory(
        int shotIndex,
        ShotType shotType,
        PlayerSide hitterSide,
        SpinType spinType,
        BallPathBuffer path,
        Vector3 contactPoint,
        Vector3 landingPoint,
        Vector3 nextContactPoint,
        Vector3 receiverStart,
        Vector3 receiverEnd,
        double apexHeightMetres,
        double launchSpeedMetresPerSecond,
        double flightSeconds) {

    public List<BallState> samples() {
        return path.toList();
    }
}
