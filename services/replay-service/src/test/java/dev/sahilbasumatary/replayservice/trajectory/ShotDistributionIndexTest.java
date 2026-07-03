package dev.sahilbasumatary.replayservice.trajectory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.sahilbasumatary.replayservice.client.dto.ShotDistributionModel;
import dev.sahilbasumatary.replayservice.domain.PlayerTier;
import dev.sahilbasumatary.replayservice.domain.ShotType;
import dev.sahilbasumatary.replayservice.exception.ReplayGenerationException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShotDistributionIndexTest {

    @Test
    void fallsBackToAnotherTierWhenExactTierMissing() {
        ShotDistributionModel model = ShotModels.groundstroke(ShotType.FOREHAND_GROUNDSTROKE);
        ShotDistributionIndex index = ShotDistributionIndex.from(List.of(model));

        ShotDistributionModel resolved =
                index.resolve(ShotType.FOREHAND_GROUNDSTROKE, PlayerTier.TOP_10);

        assertEquals(model, resolved);
    }

    @Test
    void throwsWhenShotTypeUnavailable() {
        ShotDistributionIndex index =
                ShotDistributionIndex.from(
                        List.of(ShotModels.groundstroke(ShotType.FOREHAND_GROUNDSTROKE)));

        assertThrows(
                ReplayGenerationException.class,
                () -> index.resolve(ShotType.LOB, PlayerTier.OTHER));
    }
}
