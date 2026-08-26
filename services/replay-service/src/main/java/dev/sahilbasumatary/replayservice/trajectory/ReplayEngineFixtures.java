package dev.sahilbasumatary.replayservice.trajectory;

import dev.sahilbasumatary.replayservice.client.dto.ShotDistributionModel;
import dev.sahilbasumatary.replayservice.config.ReplayEngineProperties;
import dev.sahilbasumatary.replayservice.domain.PlayerTier;
import dev.sahilbasumatary.replayservice.domain.ShotType;
import dev.sahilbasumatary.replayservice.domain.SpinType;
import dev.sahilbasumatary.replayservice.domain.Surface;
import dev.sahilbasumatary.replayservice.physics.Vector3;
import java.util.ArrayList;
import java.util.List;

/** Deterministic production-shaped priors used by golden hashes and in-process physics benches. */
public final class ReplayEngineFixtures {

    public static final long POINT_SEED = 123_456_789L;
    public static final long MATCH_SEED = 0xC0FFEE1234L;
    public static final double REFERENCE_STEP_SECONDS = 0.00025;

    private ReplayEngineFixtures() {}

    public static ReplayEngineProperties productionShapedEngine() {
        return new ReplayEngineProperties(60, 0.002, 6.0, 48, 0.05, 40, 0);
    }

    public static List<ShotType> sixShotRally() {
        return List.of(
                ShotType.FIRST_SERVE,
                ShotType.FOREHAND_GROUNDSTROKE,
                ShotType.BACKHAND_GROUNDSTROKE,
                ShotType.FOREHAND_GROUNDSTROKE,
                ShotType.BACKHAND_GROUNDSTROKE,
                ShotType.FOREHAND_GROUNDSTROKE);
    }

    public static ShotDistributionIndex hardCourtIndex() {
        return ShotDistributionIndex.from(
                List.of(
                        serve(ShotType.FIRST_SERVE),
                        serve(ShotType.SECOND_SERVE),
                        groundstroke(ShotType.FOREHAND_GROUNDSTROKE),
                        groundstroke(ShotType.BACKHAND_GROUNDSTROKE),
                        volley(ShotType.FOREHAND_VOLLEY),
                        volley(ShotType.BACKHAND_VOLLEY),
                        slice(ShotType.FOREHAND_SLICE),
                        slice(ShotType.BACKHAND_SLICE),
                        lob(),
                        drop(),
                        overhead()));
    }

    public static List<AccuracyCase> accuracyCorpus() {
        List<AccuracyCase> cases = new ArrayList<>();
        Surface[] surfaces = {Surface.HARD, Surface.CLAY, Surface.GRASS};
        long[] seeds = {1L, 7L, 99L, POINT_SEED};
        for (Surface surface : surfaces) {
            for (long seed : seeds) {
                cases.add(
                        new AccuracyCase(
                                "serve-" + surface + "-" + seed,
                                new Vector3(0.4, -11.485, 2.65),
                                48.0,
                                2200.0,
                                SpinType.TOPSPIN,
                                new Vector3(1.1, 6.4, 0),
                                surface,
                                false,
                                seed));
                cases.add(
                        new AccuracyCase(
                                "drive-" + surface + "-" + seed,
                                new Vector3(-1.2, -11.0, 0.95),
                                32.0 + (seed % 9),
                                2800.0,
                                SpinType.TOPSPIN,
                                new Vector3(1.4, 8.2, 0),
                                surface,
                                false,
                                seed));
            }
        }
        cases.add(
                new AccuracyCase(
                        "wide-drive",
                        new Vector3(2.5, -10.8, 0.95),
                        30.0,
                        2400.0,
                        SpinType.TOPSPIN,
                        new Vector3(-3.1, 7.5, 0),
                        Surface.HARD,
                        false,
                        11L));
        cases.add(
                new AccuracyCase(
                        "short-angle",
                        new Vector3(-2.0, -9.5, 1.20),
                        28.0,
                        2100.0,
                        SpinType.TOPSPIN,
                        new Vector3(3.4, 5.0, 0),
                        Surface.CLAY,
                        false,
                        13L));
        cases.add(
                new AccuracyCase(
                        "lob",
                        new Vector3(0.3, -8.5, 0.95),
                        18.0,
                        1800.0,
                        SpinType.BACKSPIN,
                        new Vector3(-0.4, 9.8, 0),
                        Surface.HARD,
                        true,
                        17L));
        cases.add(
                new AccuracyCase(
                        "drop",
                        new Vector3(0.2, -7.0, 0.95),
                        16.0,
                        1600.0,
                        SpinType.BACKSPIN,
                        new Vector3(0.6, 1.8, 0),
                        Surface.CLAY,
                        true,
                        19L));
        cases.add(
                new AccuracyCase(
                        "slice",
                        new Vector3(0.6, -10.8, 0.95),
                        30.0,
                        1600.0,
                        SpinType.TOPSPIN,
                        new Vector3(-0.5, 7.2, 0),
                        Surface.GRASS,
                        false,
                        23L));
        cases.add(
                new AccuracyCase(
                        "volley",
                        new Vector3(-0.4, -10.5, 1.15),
                        30.0,
                        400.0,
                        SpinType.FLAT,
                        new Vector3(0.8, 7.5, 0),
                        Surface.HARD,
                        false,
                        29L));
        cases.add(
                new AccuracyCase(
                        "second-serve",
                        new Vector3(-0.5, -11.485, 2.55),
                        40.0,
                        2400.0,
                        SpinType.TOPSPIN,
                        new Vector3(-1.2, 6.2, 0),
                        Surface.HARD,
                        false,
                        31L));
        cases.add(
                new AccuracyCase(
                        "overhead",
                        new Vector3(0.1, -6.0, 2.4),
                        25.0,
                        1200.0,
                        SpinType.TOPSPIN,
                        new Vector3(-0.4, 8.0, 0),
                        Surface.HARD,
                        false,
                        37L));
        cases.add(
                new AccuracyCase(
                        "slow-spinny",
                        new Vector3(0.0, -11.0, 0.95),
                        26.0,
                        2600.0,
                        SpinType.TOPSPIN,
                        new Vector3(0.4, 6.0, 0),
                        Surface.CLAY,
                        false,
                        41L));
        cases.add(
                new AccuracyCase(
                        "flat-fast",
                        new Vector3(0.0, -11.2, 1.10),
                        32.0,
                        400.0,
                        SpinType.FLAT,
                        new Vector3(0.2, 8.0, 0),
                        Surface.GRASS,
                        false,
                        43L));
        return List.copyOf(cases);
    }

    public record AccuracyCase(
            String name,
            Vector3 launch,
            double speed,
            double spinRate,
            SpinType spinType,
            Vector3 target,
            Surface surface,
            boolean highArc,
            long seed) {}

    private static ShotDistributionModel serve(ShotType shotType) {
        return new ShotDistributionModel(
                shotType,
                Surface.HARD,
                PlayerTier.OTHER,
                1.0,
                8.35,
                0.7,
                0.9,
                185.0,
                12.0,
                2400.0,
                400.0,
                2.2,
                0.3,
                5000);
    }

    private static ShotDistributionModel groundstroke(ShotType shotType) {
        return new ShotDistributionModel(
                shotType,
                Surface.HARD,
                PlayerTier.OTHER,
                0.0,
                9.0,
                1.1,
                1.2,
                120.0,
                10.0,
                2800.0,
                400.0,
                1.8,
                0.3,
                5000);
    }

    private static ShotDistributionModel volley(ShotType shotType) {
        return new ShotDistributionModel(
                shotType,
                Surface.HARD,
                PlayerTier.OTHER,
                0.4,
                4.5,
                0.8,
                0.7,
                90.0,
                8.0,
                800.0,
                200.0,
                1.2,
                0.2,
                2000);
    }

    private static ShotDistributionModel slice(ShotType shotType) {
        return new ShotDistributionModel(
                shotType,
                Surface.HARD,
                PlayerTier.OTHER,
                -0.6,
                8.2,
                1.0,
                1.0,
                95.0,
                8.0,
                2200.0,
                300.0,
                1.4,
                0.25,
                2000);
    }

    private static ShotDistributionModel lob() {
        return new ShotDistributionModel(
                ShotType.LOB,
                Surface.HARD,
                PlayerTier.OTHER,
                0.2,
                10.4,
                1.0,
                0.8,
                70.0,
                6.0,
                1800.0,
                250.0,
                4.5,
                0.4,
                800);
    }

    private static ShotDistributionModel drop() {
        return new ShotDistributionModel(
                ShotType.DROP_SHOT,
                Surface.HARD,
                PlayerTier.OTHER,
                0.3,
                3.2,
                0.7,
                0.5,
                55.0,
                5.0,
                1600.0,
                200.0,
                1.1,
                0.2,
                800);
    }

    private static ShotDistributionModel overhead() {
        return new ShotDistributionModel(
                ShotType.OVERHEAD,
                Surface.HARD,
                PlayerTier.OTHER,
                0.5,
                8.8,
                0.9,
                0.9,
                140.0,
                10.0,
                2000.0,
                300.0,
                2.0,
                0.3,
                800);
    }
}
