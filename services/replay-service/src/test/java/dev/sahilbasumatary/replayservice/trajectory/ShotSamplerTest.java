package dev.sahilbasumatary.replayservice.trajectory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sahilbasumatary.replayservice.domain.ShotType;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class ShotSamplerTest {

    private final ShotSampler sampler = new ShotSampler();

    @Test
    void sameSeedProducesIdenticalSample() {
        ShotParameters first =
                sampler.sample(
                        ShotType.FOREHAND_GROUNDSTROKE,
                        ShotModels.groundstroke(ShotType.FOREHAND_GROUNDSTROKE),
                        new SplittableRandom(42));
        ShotParameters second =
                sampler.sample(
                        ShotType.FOREHAND_GROUNDSTROKE,
                        ShotModels.groundstroke(ShotType.FOREHAND_GROUNDSTROKE),
                        new SplittableRandom(42));

        assertEquals(first, second);
    }

    @Test
    void serveLandingIsClampedToServiceBox() {
        for (long seed = 0; seed < 200; seed++) {
            ShotParameters parameters =
                    sampler.sample(
                            ShotType.FIRST_SERVE, ShotModels.serve(), new SplittableRandom(seed));
            assertTrue(
                    parameters.landingDepthMetres() <= 6.3,
                    "serve depth must stay inside the service box");
        }
    }
}
