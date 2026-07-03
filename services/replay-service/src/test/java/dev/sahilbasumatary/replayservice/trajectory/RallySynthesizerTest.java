package dev.sahilbasumatary.replayservice.trajectory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sahilbasumatary.replayservice.domain.PointOutcome;
import dev.sahilbasumatary.replayservice.domain.ShotType;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class RallySynthesizerTest {

    private final RallySynthesizer synthesizer = new RallySynthesizer();

    @Test
    void aceIsASingleServe() {
        List<ShotType> shots =
                synthesizer.synthesize(1, PointOutcome.ACE, 40, new SplittableRandom(1));

        assertEquals(List.of(ShotType.FIRST_SERVE), shots);
    }

    @Test
    void doubleFaultIsASingleSecondServe() {
        List<ShotType> shots =
                synthesizer.synthesize(0, PointOutcome.DOUBLE_FAULT, 40, new SplittableRandom(1));

        assertEquals(List.of(ShotType.SECOND_SERVE), shots);
    }

    @Test
    void rallyStartsWithServeAndRespectsLength() {
        List<ShotType> shots =
                synthesizer.synthesize(6, PointOutcome.WINNER, 40, new SplittableRandom(7));

        assertEquals(6, shots.size());
        assertEquals(ShotType.FIRST_SERVE, shots.get(0));
    }

    @Test
    void rallyLengthIsCappedByMaximum() {
        List<ShotType> shots =
                synthesizer.synthesize(500, PointOutcome.WINNER, 12, new SplittableRandom(7));

        assertTrue(shots.size() <= 12, "rally must be capped at the configured maximum");
    }

    @Test
    void sameSeedProducesSameRally() {
        List<ShotType> first =
                synthesizer.synthesize(8, PointOutcome.UNFORCED_ERROR, 40, new SplittableRandom(99));
        List<ShotType> second =
                synthesizer.synthesize(8, PointOutcome.UNFORCED_ERROR, 40, new SplittableRandom(99));

        assertEquals(first, second);
    }
}
