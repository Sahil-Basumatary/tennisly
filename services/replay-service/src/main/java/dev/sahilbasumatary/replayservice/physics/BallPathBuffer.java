package dev.sahilbasumatary.replayservice.physics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Structure-of-arrays flight samples. Replay JSON still serialises {@link BallState} records at the
 * assembler boundary; this buffer exists so integration and interpolation can stay on primitives.
 */
public final class BallPathBuffer {

    private static final int INITIAL_CAPACITY = 256;

    private double[] time = new double[INITIAL_CAPACITY];
    private double[] x = new double[INITIAL_CAPACITY];
    private double[] y = new double[INITIAL_CAPACITY];
    private double[] z = new double[INITIAL_CAPACITY];
    private double[] vx = new double[INITIAL_CAPACITY];
    private double[] vy = new double[INITIAL_CAPACITY];
    private double[] vz = new double[INITIAL_CAPACITY];
    private double[] sx = new double[INITIAL_CAPACITY];
    private double[] sy = new double[INITIAL_CAPACITY];
    private double[] sz = new double[INITIAL_CAPACITY];
    private int size;

    public void clear() {
        size = 0;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void ensureCapacity(int needed) {
        if (needed <= time.length) {
            return;
        }
        int cap = time.length;
        while (cap < needed) {
            cap *= 2;
        }
        time = Arrays.copyOf(time, cap);
        x = Arrays.copyOf(x, cap);
        y = Arrays.copyOf(y, cap);
        z = Arrays.copyOf(z, cap);
        vx = Arrays.copyOf(vx, cap);
        vy = Arrays.copyOf(vy, cap);
        vz = Arrays.copyOf(vz, cap);
        sx = Arrays.copyOf(sx, cap);
        sy = Arrays.copyOf(sy, cap);
        sz = Arrays.copyOf(sz, cap);
    }

    public void append(BallState state) {
        Vector3 position = state.position();
        Vector3 velocity = state.velocity();
        Vector3 spin = state.spin();
        append(
                state.timeSeconds(),
                position.x(),
                position.y(),
                position.z(),
                velocity.x(),
                velocity.y(),
                velocity.z(),
                spin.x(),
                spin.y(),
                spin.z());
    }

    public void append(
            double timeSeconds,
            double px,
            double py,
            double pz,
            double velX,
            double velY,
            double velZ,
            double spinX,
            double spinY,
            double spinZ) {
        ensureCapacity(size + 1);
        int index = size++;
        time[index] = timeSeconds;
        x[index] = px;
        y[index] = py;
        z[index] = pz;
        vx[index] = velX;
        vy[index] = velY;
        vz[index] = velZ;
        sx[index] = spinX;
        sy[index] = spinY;
        sz[index] = spinZ;
    }

    public void copyPrefix(BallPathBuffer source, int count) {
        if (count < 0 || count > source.size) {
            throw new IllegalArgumentException("prefix count out of range: " + count);
        }
        ensureCapacity(count);
        System.arraycopy(source.time, 0, time, 0, count);
        System.arraycopy(source.x, 0, x, 0, count);
        System.arraycopy(source.y, 0, y, 0, count);
        System.arraycopy(source.z, 0, z, 0, count);
        System.arraycopy(source.vx, 0, vx, 0, count);
        System.arraycopy(source.vy, 0, vy, 0, count);
        System.arraycopy(source.vz, 0, vz, 0, count);
        System.arraycopy(source.sx, 0, sx, 0, count);
        System.arraycopy(source.sy, 0, sy, 0, count);
        System.arraycopy(source.sz, 0, sz, 0, count);
        size = count;
    }

    public double time(int index) {
        return time[index];
    }

    public double x(int index) {
        return x[index];
    }

    public double y(int index) {
        return y[index];
    }

    public double z(int index) {
        return z[index];
    }

    public double vx(int index) {
        return vx[index];
    }

    public double vy(int index) {
        return vy[index];
    }

    public double vz(int index) {
        return vz[index];
    }

    public BallState state(int index) {
        return new BallState(
                time[index],
                new Vector3(x[index], y[index], z[index]),
                new Vector3(vx[index], vy[index], vz[index]),
                new Vector3(sx[index], sy[index], sz[index]));
    }

    public List<BallState> toList() {
        List<BallState> samples = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            samples.add(state(index));
        }
        return List.copyOf(samples);
    }
}
