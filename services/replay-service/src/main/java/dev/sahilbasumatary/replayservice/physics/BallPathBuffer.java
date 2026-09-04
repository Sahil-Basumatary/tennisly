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
    private double[] posX = new double[INITIAL_CAPACITY];
    private double[] posY = new double[INITIAL_CAPACITY];
    private double[] posZ = new double[INITIAL_CAPACITY];
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
        posX = Arrays.copyOf(posX, cap);
        posY = Arrays.copyOf(posY, cap);
        posZ = Arrays.copyOf(posZ, cap);
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
        posX[index] = px;
        posY[index] = py;
        posZ[index] = pz;
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
        System.arraycopy(source.posX, 0, posX, 0, count);
        System.arraycopy(source.posY, 0, posY, 0, count);
        System.arraycopy(source.posZ, 0, posZ, 0, count);
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

    public double positionX(int index) {
        return posX[index];
    }

    public double positionY(int index) {
        return posY[index];
    }

    public double positionZ(int index) {
        return posZ[index];
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
                new Vector3(posX[index], posY[index], posZ[index]),
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
