package dev.sahilbasumatary.replayservice.dto.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.replayservice.config.ReplayEngineVersions;
import dev.sahilbasumatary.replayservice.domain.Surface;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchReplayResponsePlaybackTest {

    @Test
    void missingEngineVersionDefaultsToV1() throws Exception {
        String v1Payload =
                """
                {"matchId":"11111111-1111-1111-1111-111111111111","surface":"HARD","frameRate":60,\
                "pointCount":0,"shotCount":0,"frameCount":0,"durationSeconds":0.0,\
                "points":[],"shots":[],"frames":[]}
                """;
        MatchReplayResponse replay =
                new ObjectMapper().readValue(v1Payload, MatchReplayResponse.class);
        assertEquals(ReplayEngineVersions.V1, replay.engineVersion());
        assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"), replay.matchId());
        assertEquals(Surface.HARD, replay.surface());
        assertEquals(List.of(), replay.frames());
    }
}
