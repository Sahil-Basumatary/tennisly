package dev.sahilbasumatary.replayservice.physics;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Numerically integrates the flight of a tennis ball under gravity, quadratic aerodynamic drag and
 * the Magnus force produced by spin, with surface-aware bounce handling.
 *
 * <p>There is no closed-form solution once drag is involved, so the path is advanced with a
 * fourth-order Runge-Kutta integrator. Spin is treated as constant over the short flight time,
 * which is an accepted simplification for single-shot trajectories. Hot-path samples live in a
 * primitive buffer so solver scans do not allocate a {@link BallState} per step.
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
     * Acceleration acting on the ball for a given velocity and spin, combining gravity, drag and
     * the Magnus (lift) force.
     */
    public Vector3 acceleration(Vector3 velocity, Vector3 spin) {
        Accel accel = new Accel();
        accelerationInto(
                velocity.x(), velocity.y(), velocity.z(), spin.x(), spin.y(), spin.z(), accel);
        return new Vector3(accel.ax, accel.ay, accel.az);
    }

    /** Advances the ball by one timestep using fourth-order Runge-Kutta, ignoring the ground. */
    public BallState integrate(BallState state, double stepSeconds) {
        Step step = new Step();
        integrateInto(
                state.timeSeconds(),
                state.position().x(),
                state.position().y(),
                state.position().z(),
                state.velocity().x(),
                state.velocity().y(),
                state.velocity().z(),
                state.spin().x(),
                state.spin().y(),
                state.spin().z(),
                stepSeconds,
                step);
        return new BallState(
                step.time,
                new Vector3(step.px, step.py, step.pz),
                new Vector3(step.vx, step.vy, step.vz),
                state.spin());
    }

    /**
     * Simulates the full flight, sampling the ball on every integration step, applying bounces
     * using the surface profile until the {@code stopOnFirstBounce} condition or the time budget is
     * hit.
     */
    public List<BallState> simulate(
            BallState initial,
            BounceProfile profile,
            double stepSeconds,
            double maxTimeSeconds,
            boolean stopOnFirstBounce) {
        BallPathBuffer path = new BallPathBuffer();
        simulateInto(initial, profile, stepSeconds, maxTimeSeconds, stopOnFirstBounce, path);
        return path.toList();
    }

    public void simulateInto(
            BallState initial,
            BounceProfile profile,
            double stepSeconds,
            double maxTimeSeconds,
            boolean stopOnFirstBounce,
            BallPathBuffer path) {
        path.clear();
        int estimatedSteps = (int) Math.ceil(maxTimeSeconds / Math.max(stepSeconds, 1.0e-6)) + 8;
        path.ensureCapacity(estimatedSteps);
        path.append(initial);
        Step current = Step.from(initial);
        Step next = new Step();
        while (current.time < maxTimeSeconds) {
            integrateInto(
                    current.time,
                    current.px,
                    current.py,
                    current.pz,
                    current.vx,
                    current.vy,
                    current.vz,
                    current.sx,
                    current.sy,
                    current.sz,
                    stepSeconds,
                    next);
            if (next.pz <= 0.0 && current.pz > 0.0) {
                interpolateGroundContact(current, next, next);
                path.append(
                        next.time, next.px, next.py, 0.0, next.vx, next.vy, next.vz, next.sx,
                        next.sy, next.sz);
                if (stopOnFirstBounce) {
                    return;
                }
                applyBounce(next, profile, current);
                continue;
            }
            path.append(
                    next.time, next.px, next.py, next.pz, next.vx, next.vy, next.vz, next.sx,
                    next.sy, next.sz);
            current.copyFrom(next);
        }
    }

    /** Returns the horizontal landing point of the first ground contact for a launch state. */
    public Vector3 landingPoint(
            BallState initial, BounceProfile profile, double stepSeconds, double maxTimeSeconds) {
        BounceSample sample = sampleFirstBounce(initial, profile, stepSeconds, maxTimeSeconds);
        return new Vector3(sample.landingX(), sample.landingY(), 0.0);
    }

    /**
     * First bounce plus the net-plane crossing. The extra interpolate is free relative to another
     * full flight: the solver needs both to reject shots that land in and still clip the tape.
     */
    public BounceSample sampleFirstBounce(
            BallState initial, BounceProfile profile, double stepSeconds, double maxTimeSeconds) {
        Step current = Step.from(initial);
        Step next = new Step();
        boolean crossedNet = false;
        double netX = initial.position().x();
        double netZ = initial.position().z();
        while (current.time < maxTimeSeconds) {
            integrateInto(
                    current.time,
                    current.px,
                    current.py,
                    current.pz,
                    current.vx,
                    current.vy,
                    current.vz,
                    current.sx,
                    current.sy,
                    current.sz,
                    stepSeconds,
                    next);
            if (!crossedNet && current.py * next.py <= 0.0 && current.py != next.py) {
                double span = next.py - current.py;
                double fraction = (0.0 - current.py) / span;
                netX = current.px + (next.px - current.px) * fraction;
                netZ = current.pz + (next.pz - current.pz) * fraction;
                crossedNet = true;
            }
            if (next.pz <= 0.0 && current.pz > 0.0) {
                interpolateGroundContact(current, next, next);
                return new BounceSample(next.px, next.py, netX, netZ, crossedNet);
            }
            current.copyFrom(next);
        }
        return new BounceSample(current.px, current.py, netX, netZ, crossedNet);
    }

    public record BounceSample(
            double landingX, double landingY, double netX, double netZ, boolean crossedNet) {

        public boolean clearsNet() {
            if (!crossedNet) {
                return true;
            }
            return netZ + 0.02 >= CourtGeometry.netHeightAt(netX);
        }
    }

    private void integrateInto(
            double time,
            double px,
            double py,
            double pz,
            double vx,
            double vy,
            double vz,
            double sx,
            double sy,
            double sz,
            double stepSeconds,
            Step out) {
        Accel a1 = out.a1;
        Accel a2 = out.a2;
        Accel a3 = out.a3;
        final Accel a4 = out.a4;
        accelerationInto(vx, vy, vz, sx, sy, sz, a1);
        double v2x = vx + a1.ax * (stepSeconds / 2.0);
        double v2y = vy + a1.ay * (stepSeconds / 2.0);
        double v2z = vz + a1.az * (stepSeconds / 2.0);
        accelerationInto(v2x, v2y, v2z, sx, sy, sz, a2);
        double v3x = vx + a2.ax * (stepSeconds / 2.0);
        double v3y = vy + a2.ay * (stepSeconds / 2.0);
        double v3z = vz + a2.az * (stepSeconds / 2.0);
        accelerationInto(v3x, v3y, v3z, sx, sy, sz, a3);
        double v4x = vx + a3.ax * stepSeconds;
        double v4y = vy + a3.ay * stepSeconds;
        double v4z = vz + a3.az * stepSeconds;
        accelerationInto(v4x, v4y, v4z, sx, sy, sz, a4);
        final double velocityIncrementX =
                (a1.ax + a2.ax * 2.0 + a3.ax * 2.0 + a4.ax) * (stepSeconds / 6.0);
        final double velocityIncrementY =
                (a1.ay + a2.ay * 2.0 + a3.ay * 2.0 + a4.ay) * (stepSeconds / 6.0);
        final double velocityIncrementZ =
                (a1.az + a2.az * 2.0 + a3.az * 2.0 + a4.az) * (stepSeconds / 6.0);
        // v1 is v0; chained Vector3 add/scale must stay left-associative.
        final double positionIncrementX =
                (((vx + v2x * 2.0) + v3x * 2.0) + v4x) * (stepSeconds / 6.0);
        final double positionIncrementY =
                (((vy + v2y * 2.0) + v3y * 2.0) + v4y) * (stepSeconds / 6.0);
        final double positionIncrementZ =
                (((vz + v2z * 2.0) + v3z * 2.0) + v4z) * (stepSeconds / 6.0);
        out.time = time + stepSeconds;
        out.px = px + positionIncrementX;
        out.py = py + positionIncrementY;
        out.pz = pz + positionIncrementZ;
        out.vx = vx + velocityIncrementX;
        out.vy = vy + velocityIncrementY;
        out.vz = vz + velocityIncrementZ;
        out.sx = sx;
        out.sy = sy;
        out.sz = sz;
    }

    private void accelerationInto(
            double vx, double vy, double vz, double sx, double sy, double sz, Accel out) {
        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (speed == 0.0) {
            out.ax = 0.0;
            out.ay = 0.0;
            out.az = -gravity;
            return;
        }
        double dragScale = -dragFactor * speed;
        double dragX = vx * dragScale;
        double dragY = vy * dragScale;
        final double dragZ = vz * dragScale;
        magnusForce(vx, vy, vz, sx, sy, sz, speed, out);
        double invMass = 1.0 / mass;
        out.ax = 0.0 + (dragX + out.ax) * invMass;
        out.ay = 0.0 + (dragY + out.ay) * invMass;
        out.az = -gravity + (dragZ + out.az) * invMass;
    }

    private void magnusForce(
            double vx,
            double vy,
            double vz,
            double sx,
            double sy,
            double sz,
            double speed,
            Accel out) {
        double spinRate = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (spinRate == 0.0) {
            out.ax = 0.0;
            out.ay = 0.0;
            out.az = 0.0;
            return;
        }
        double cx = sy * vz - sz * vy;
        double cy = sz * vx - sx * vz;
        double cz = sx * vy - sy * vx;
        double crossMagnitude = Math.sqrt(cx * cx + cy * cy + cz * cz);
        if (crossMagnitude == 0.0) {
            out.ax = 0.0;
            out.ay = 0.0;
            out.az = 0.0;
            return;
        }
        double spinRatio = (ballRadius * spinRate) / speed;
        double liftCoefficient = 1.0 / (2.0 + (1.0 / spinRatio));
        double magnitude = liftFactor * liftCoefficient * speed * speed;
        double inv = 1.0 / crossMagnitude;
        out.ax = cx * inv * magnitude;
        out.ay = cy * inv * magnitude;
        out.az = cz * inv * magnitude;
    }

    private void interpolateGroundContact(Step above, Step below, Step out) {
        double z0 = above.pz;
        double z1 = below.pz;
        double fraction = z0 / (z0 - z1);
        out.time = above.time + (below.time - above.time) * fraction;
        out.px = above.px + (below.px - above.px) * fraction;
        out.py = above.py + (below.py - above.py) * fraction;
        out.pz = 0.0;
        out.vx = above.vx + (below.vx - above.vx) * fraction;
        out.vy = above.vy + (below.vy - above.vy) * fraction;
        out.vz = above.vz + (below.vz - above.vz) * fraction;
        out.sx = above.sx;
        out.sy = above.sy;
        out.sz = above.sz;
    }

    private void applyBounce(Step impact, BounceProfile profile, Step out) {
        final double bouncedVerticalSpeed = -impact.vz * profile.verticalRestitution();
        final double horizontalRetention = 1.0 - 0.35 * profile.horizontalFriction();
        double spinRate =
                Math.sqrt(impact.sx * impact.sx + impact.sy * impact.sy + impact.sz * impact.sz);
        double spinSurfaceSpeed = spinRate * ballRadius;
        double horizontalMagnitude = Math.sqrt(impact.vx * impact.vx + impact.vy * impact.vy);
        double nx;
        double ny;
        if (horizontalMagnitude == 0.0) {
            nx = 0.0;
            ny = 0.0;
        } else {
            nx = impact.vx / horizontalMagnitude;
            ny = impact.vy / horizontalMagnitude;
        }
        final double forwardBoost = profile.spinForwardFactor() * spinSurfaceSpeed;
        out.time = impact.time;
        out.px = impact.px;
        out.py = impact.py;
        out.pz = impact.pz;
        out.vx = impact.vx * horizontalRetention + nx * forwardBoost;
        out.vy = impact.vy * horizontalRetention + ny * forwardBoost;
        out.vz = bouncedVerticalSpeed;
        out.sx = impact.sx;
        out.sy = impact.sy;
        out.sz = impact.sz;
    }

    private static final class Accel {
        private double ax;
        private double ay;
        private double az;
    }

    private static final class Step {
        private final Accel a1 = new Accel();
        private final Accel a2 = new Accel();
        private final Accel a3 = new Accel();
        private final Accel a4 = new Accel();
        private double time;
        private double px;
        private double py;
        private double pz;
        private double vx;
        private double vy;
        private double vz;
        private double sx;
        private double sy;
        private double sz;

        private static Step from(BallState state) {
            Step step = new Step();
            step.time = state.timeSeconds();
            step.px = state.position().x();
            step.py = state.position().y();
            step.pz = state.position().z();
            step.vx = state.velocity().x();
            step.vy = state.velocity().y();
            step.vz = state.velocity().z();
            step.sx = state.spin().x();
            step.sy = state.spin().y();
            step.sz = state.spin().z();
            return step;
        }

        private void copyFrom(Step other) {
            time = other.time;
            px = other.px;
            py = other.py;
            pz = other.pz;
            vx = other.vx;
            vy = other.vy;
            vz = other.vz;
            sx = other.sx;
            sy = other.sy;
            sz = other.sz;
        }
    }
}
