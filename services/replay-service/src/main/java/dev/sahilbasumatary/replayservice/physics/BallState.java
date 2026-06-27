package dev.sahilbasumatary.replayservice.physics;

/**
 * A snapshot of the ball at a single instant.
 *
 * @param timeSeconds elapsed time since the shot was struck
 * @param position court-frame position in metres
 * @param velocity velocity in metres per second
 * @param spin angular velocity vector in radians per second (its direction is the spin axis)
 */
public record BallState(double timeSeconds, Vector3 position, Vector3 velocity, Vector3 spin) {

    public BallState withTime(double newTimeSeconds) {
        return new BallState(newTimeSeconds, position, velocity, spin);
    }

    public BallState withPositionAndVelocity(Vector3 newPosition, Vector3 newVelocity) {
        return new BallState(timeSeconds, newPosition, newVelocity, spin);
    }
}
