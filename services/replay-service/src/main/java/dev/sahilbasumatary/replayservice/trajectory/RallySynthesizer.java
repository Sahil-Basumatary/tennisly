package dev.sahilbasumatary.replayservice.trajectory;

import dev.sahilbasumatary.replayservice.domain.PointOutcome;
import dev.sahilbasumatary.replayservice.domain.ShotType;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Component;

/**
 * Reconstructs the ordered sequence of shot types for a point. Real point-by-point feeds rarely
 * include the stroke-by-stroke breakdown, so when it is absent we synthesise a plausible rally from
 * the rally length and the recorded outcome (an ace is a single serve, a double fault is a missed
 * second serve, longer rallies trade groundstrokes with the occasional slice or finishing shot).
 */
@Component
public class RallySynthesizer {

    private static final double SLICE_PROBABILITY = 0.12;
    private static final double FINISHER_PROBABILITY = 0.35;

    public List<ShotType> synthesize(
            int rallyLength, PointOutcome outcome, int maxRallyLength, RandomGenerator random) {
        if (outcome == PointOutcome.ACE) {
            return List.of(ShotType.FIRST_SERVE);
        }
        if (outcome == PointOutcome.DOUBLE_FAULT) {
            return List.of(ShotType.SECOND_SERVE);
        }

        int shotCount = Math.max(1, Math.min(rallyLength, maxRallyLength));
        List<ShotType> shots = new ArrayList<>(shotCount);
        shots.add(ShotType.FIRST_SERVE);
        if (shotCount == 1) {
            return shots;
        }
        shots.add(groundstroke(random));
        for (int index = 2; index < shotCount; index++) {
            boolean isFinalShot = index == shotCount - 1;
            if (isFinalShot && outcome == PointOutcome.WINNER) {
                shots.add(finisher(random));
            } else {
                shots.add(rallyShot(random));
            }
        }
        return shots;
    }

    private ShotType rallyShot(RandomGenerator random) {
        if (random.nextDouble() < SLICE_PROBABILITY) {
            return random.nextBoolean() ? ShotType.FOREHAND_SLICE : ShotType.BACKHAND_SLICE;
        }
        return groundstroke(random);
    }

    private ShotType groundstroke(RandomGenerator random) {
        return random.nextBoolean()
                ? ShotType.FOREHAND_GROUNDSTROKE
                : ShotType.BACKHAND_GROUNDSTROKE;
    }

    private ShotType finisher(RandomGenerator random) {
        if (random.nextDouble() < FINISHER_PROBABILITY) {
            double roll = random.nextDouble();
            if (roll < 0.4) {
                return ShotType.DROP_SHOT;
            }
            if (roll < 0.7) {
                return ShotType.LOB;
            }
            return random.nextBoolean() ? ShotType.FOREHAND_VOLLEY : ShotType.BACKHAND_VOLLEY;
        }
        return groundstroke(random);
    }
}
