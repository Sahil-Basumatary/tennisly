package dev.sahilbasumatary.replayservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.replayservice.domain.ReplayStatus;
import dev.sahilbasumatary.replayservice.domain.Surface;
import dev.sahilbasumatary.replayservice.dto.response.MatchReplayResponse;
import dev.sahilbasumatary.replayservice.dto.response.ReplayArtifactResponse;
import dev.sahilbasumatary.replayservice.entity.ReplayArtifact;
import dev.sahilbasumatary.replayservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.replayservice.repository.ReplayArtifactRepository;
import dev.sahilbasumatary.replayservice.storage.CompressedPayload;
import dev.sahilbasumatary.replayservice.storage.ReplayObjectStore;
import dev.sahilbasumatary.replayservice.storage.ReplayPayloadCodec;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReplayArtifactServiceTest {

    private static final UUID MATCH_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String KEY = "replays/match/object.json.gz";

    private final ReplayGenerationService generationService =
            org.mockito.Mockito.mock(ReplayGenerationService.class);
    private final ReplayArtifactRepository artifactRepository =
            org.mockito.Mockito.mock(ReplayArtifactRepository.class);
    private final ReplayObjectStore objectStore =
            org.mockito.Mockito.mock(ReplayObjectStore.class);
    private final ReplayPayloadCodec payloadCodec = new ReplayPayloadCodec();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ReplayArtifactService service;

    @BeforeEach
    void setUp() {
        service =
                new ReplayArtifactService(
                        generationService,
                        artifactRepository,
                        objectStore,
                        payloadCodec,
                        objectMapper);
    }

    @Test
    void materializeUploadsCompressedPayloadAndPersistsMetadata() {
        MatchReplayResponse replay = sampleReplay();
        when(generationService.generateMatchReplay(MATCH_ID)).thenReturn(replay);
        when(objectStore.buildKey(eq(MATCH_ID), any())).thenReturn(KEY);
        when(objectStore.bucket()).thenReturn("tennisly-replays");
        when(artifactRepository.findByMatchId(MATCH_ID)).thenReturn(Optional.empty());
        when(artifactRepository.save(any(ReplayArtifact.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReplayArtifactResponse response = service.materialize(MATCH_ID);

        verify(objectStore).put(eq(KEY), any(byte[].class), anyString());
        assertEquals(MATCH_ID, response.matchId());
        assertEquals(replay.frameCount(), response.frameCount());
        assertEquals(ReplayStatus.READY, response.status());
        assertEquals(KEY, response.storageKey());
    }

    @Test
    void getReplayServesStoredArtifactWithoutRegenerating() {
        MatchReplayResponse replay = sampleReplay();
        byte[] raw = writeBytes(replay);
        CompressedPayload payload = payloadCodec.compress(raw);
        ReplayArtifact artifact = readyArtifact(payload.checksumSha256());
        when(artifactRepository.findByMatchId(MATCH_ID)).thenReturn(Optional.of(artifact));
        when(objectStore.get(KEY)).thenReturn(payload.data());

        MatchReplayResponse result = service.getReplay(MATCH_ID);

        assertEquals(replay.matchId(), result.matchId());
        assertEquals(replay.frameCount(), result.frameCount());
        verify(generationService, never()).generateMatchReplay(any());
    }

    @Test
    void getReplayFallsBackToLiveGenerationWhenNotMaterialized() {
        MatchReplayResponse replay = sampleReplay();
        when(artifactRepository.findByMatchId(MATCH_ID)).thenReturn(Optional.empty());
        when(generationService.generateMatchReplay(MATCH_ID)).thenReturn(replay);

        MatchReplayResponse result = service.getReplay(MATCH_ID);

        assertSame(replay, result);
        verify(generationService).generateMatchReplay(MATCH_ID);
    }

    @Test
    void getReplayRegeneratesWhenStoredObjectIsCorrupt() {
        MatchReplayResponse replay = sampleReplay();
        ReplayArtifact artifact = readyArtifact("deadbeef");
        when(artifactRepository.findByMatchId(MATCH_ID)).thenReturn(Optional.of(artifact));
        when(objectStore.get(KEY)).thenReturn("not-gzip".getBytes());
        when(generationService.generateMatchReplay(MATCH_ID)).thenReturn(replay);

        MatchReplayResponse result = service.getReplay(MATCH_ID);

        assertSame(replay, result);
        verify(generationService).generateMatchReplay(MATCH_ID);
    }

    @Test
    void deleteRemovesRowAndObject() {
        ReplayArtifact artifact = readyArtifact("checksum");
        when(artifactRepository.findByMatchId(MATCH_ID)).thenReturn(Optional.of(artifact));

        service.delete(MATCH_ID);

        ArgumentCaptor<ReplayArtifact> captor = ArgumentCaptor.forClass(ReplayArtifact.class);
        verify(artifactRepository).delete(captor.capture());
        verify(objectStore).delete(KEY);
        assertEquals(MATCH_ID, captor.getValue().getMatchId());
    }

    @Test
    void getArtifactThrowsWhenMissing() {
        when(artifactRepository.findByMatchId(MATCH_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getArtifact(MATCH_ID));
    }

    private ReplayArtifact readyArtifact(String checksum) {
        ReplayArtifact artifact = new ReplayArtifact();
        artifact.setMatchId(MATCH_ID);
        artifact.setStorageBucket("tennisly-replays");
        artifact.setStorageKey(KEY);
        artifact.setSurface(Surface.HARD);
        artifact.setFrameRate(60);
        artifact.setPointCount(1);
        artifact.setShotCount(1);
        artifact.setFrameCount(1);
        artifact.setDurationSeconds(1.0);
        artifact.setContentEncoding("gzip");
        artifact.setSizeBytes(10);
        artifact.setUncompressedBytes(20);
        artifact.setChecksumSha256(checksum);
        artifact.setEngineVersion("1.0.0");
        artifact.setStatus(ReplayStatus.READY);
        return artifact;
    }

    private MatchReplayResponse sampleReplay() {
        return new MatchReplayResponse(
                MATCH_ID,
                Surface.HARD,
                60,
                1,
                1,
                2,
                1.5,
                List.of(),
                List.of(),
                List.of());
    }

    private byte[] writeBytes(MatchReplayResponse replay) {
        try {
            return objectMapper.writeValueAsBytes(replay);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
