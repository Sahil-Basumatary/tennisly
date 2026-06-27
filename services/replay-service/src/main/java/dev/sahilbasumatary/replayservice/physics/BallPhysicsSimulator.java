package dev.sahilbasumatary.replayservice.physics;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Numerically integrates the flight of a tennis ball under gravity, quadratic aerodynamic drag and
 * the Magnus force produced by spin, with surface-aware bounce handling.
 *
 * <p>There is no closed-form solution once drag is involved, so the path is advanced with a
 * fourth-order Runge-Kutta integrator. Spin is treated as constant over the short flight time, which
 * is an accepted simplification for single-shot trajectories.
 */
@Component
public class BallPhysicsSimulator {

    private final double mass;
    private final double gravity;
    private final double ballRadius;
    private final double dragFactor;
    private final double liftFactor;

    public BallPhysicsSimulator() {
        this(
                CourtGeometry.BALL_MASS_KG,
                CourtGeometry.GRAVITY_METRES_PER_SECOND_SQUARED,
                CourtGeometry.BALL_RADIUS_METRES,
                CourtGeometry.AIR_DENSITY_KG_PER_CUBIC_METRE,
                CourtGeometry.DRAG_COEFFICIENT,
                CourtGeometry.ballCrossSectionalAreaSquareMetres());
    }

    public BallPhysicsSimulator(
            double mass,
            double gravity,
            double ballRadius,
            double airDensity,
            double dragCoefficient,
            double crossSectionalArea) {
        this.mass = mass;
        this.gravity = gravity;
        this.ballRadius = ballRadius;
        this.dragFactor = 0.5 * airDensity * dragCoefficient * crossSectionalArea;
        this.liftFactor = 0.5 * airDensity * crossSectionalArea;
    }

    /**
     * Acceleration acting on the ball for a given velocity and spin, combining gravity, drag and the
     * Magnus (lift) force.
     */
    public Vector3 acceleration(Vector3 velocity, Vector3 spin) {
        Vector3 gravityAcceleration = new Vector3(0, 0, -gravity);
        double speed = velocity.magnitude();
        if (speed == 0.0) {
            return gravityAcceleration;
        }
        Vector3 dragForce = velocity.scale(-dragFactor * speed);
        Vector3 magnusForce = magnusForce(velocity, spin, speed);
        return gravityAcceleration.add(dragForce.add(magnusForce).scale(1.0 / mass));
    }

    private Vector3 magnusForce(Vector3 velocity, Vector3 spin, double speed) {
        double spinRate = spin.magnitude();
        if (spinRate == 0.0) {
            return Vector3.ZERO;
        }
        Vector3 spinCrossVelocity = spin.cross(velocity);
        if (spinCrossVelocity.magnitude() == 0.0) {
            return Vector3.ZERO;
        }
        double spinRatio = (ballRadius * spinRate) / speed;
        double liftCoefficient = 1.0 / (2.0 + (1.0 / spinRatio));
        double magnitude = liftFactor * liftCoefficient * speed * speed;
        return spinCrossVelocity.normalized().scale(magnitude);
    }

    /** Advances the ball by one timestep using fourth-order Runge-Kutta, ignoring the ground. */
    public BallState integrate(BallState state, double stepSeconds) {
        Vector3 p0 = state.position();
        Vector3 v0 = state.velocity();
        Vector3 spin = state.spin();

        Vector3 a1 = acceleration(v0, spin);
        Vector3 v1 = v0;

        Vector3 v2 = v0.add(a1.scale(stepSeconds / 2.0));
        Vector3 a2 = acceleration(v2, spin);

        Vector3 v3 = v0.add(a2.scale(stepSeconds / 2.0));
        Vector3 a3 = acceleration(v3, spin);

        Vector3 v4 = v0.add(a3.scale(stepSeconds));
        Vector3 a4 = acceleration(v4, spin);

        Vector3 velocityIncrement =
                a1.add(a2.scale(2)).add(a3.scale(2)).add(a4).scale(stepSeconds / 6.0);
        Vector3 positionIncrement =
                v1.add(v2.scale(2)).add(v3.scale(2)).add(v4).scale(stepSeconds / 6.0);

        return new BallState(
                state.timeSeconds() + stepSeconds,
                p0.add(positionIncrement),
                v0.add(velocityIncrement),
                spin);
    }

    /**
     * Simulates the full flight, sampling the ball on every integration step, applying bounces using
     * the surface profile until the {@code stopOnFirstBounce} condition or the time budget is hit.
     */
    public List<BallState> simulate(
            BallState initial,
            BounceProfile profile,
            double stepSeconds,
            double maxTimeSeconds,
            boolean stopOnFirstBounce) {
        List<BallState> path = new ArrayList<>();
        path.add(initial);
        BallState current = initial;
        while (current.timeSeconds() < maxTimeSeconds) {
            BallState next = integrate(current, stepSeconds);
            if (next.position().z() <= 0.0 && current.position().z() > 0.0) {
                BallState impact = interpolateGroundContact(current, next);
                path.add(impact);
                if (stopOnFirstBounce) {
                    return path;
                }
                current = applyBounce(impact, profile);
                continue;
            }
            path.add(next);
            current = next;
        }
        return path;
    }

    /** Returns the horizontal landing point of the first ground contact for a launch state. */
    public Vector3 landingPoint(
            BallState initial, BounceProfile profile, double stepSeconds, double maxTimeSeconds) {
        List<BallState> path = simulate(initial, profile, stepSeconds, maxTimeSeconds, true);
        return path.get(path.size() - 1).position();
    }

    private BallState interpolateGroundContact(BallState above, BallState below) {
        double z0 = above.position().z();
        double z1 = below.position().z();
        double fraction = z0 / (z0 - z1);
        Vector3 position =
                above.position().add(below.position().subtract(above.position()).scale(fraction));
        Vector3 velocity =
                above.velocity().add(below.velocity().subtract(above.velocity()).scale(fraction));
        double time = above.timeSeconds() + (below.timeSeconds() - above.timeSeconds()) * fraction;
        return new BallState(time, new Vector3(position.x(), position.y(), 0.0), velocity, above.spin());
    }

    private BallState applyBounce(BallState impact, BounceProfile profile) {
        Vector3 velocity = impact.velocity();
        double bouncedVerticalSpeed = -velocity.z() * profile.verticalRestitution();
        double horizontalRetention = 1.0 - 0.35 * profile.horizontalFriction();
        double spinSurfaceSpeed = impact.spin().magnitude() * ballRadius;
        Vector3 horizontalDirection =
                new Vector3(velocity.x(), velocity.y(), 0.0).normalized();
        double forwardBoost = profile.spinForwardFactor() * spinSurfaceSpeed;
        Vector3 bouncedVelocity =
                new Vector3(
                        velocity.x() * horizontalRetention + horizontalDirection.x() * forwardBoost,
                        velocity.y() * horizontalRetention + horizontalDirection.y() * forwardBoost,
                        bouncedVerticalSpeed);
        return impact.withPositionAndVelocity(impact.position(), bouncedVelocity);
    }
}
