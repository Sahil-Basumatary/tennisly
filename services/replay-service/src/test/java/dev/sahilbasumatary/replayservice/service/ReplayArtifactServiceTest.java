package dev.sahilbasumatary.replayservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.replayservice.config.ReplayStorageProperties;
import dev.sahilbasumatary.replayservice.domain.ReplayStatus;
import dev.sahilbasumatary.replayservice.domain.Surface;
import dev.sahilbasumatary.replayservice.dto.response.MatchReplayResponse;
import dev.sahilbasumatary.replayservice.dto.response.ReplayArtifactResponse;
import dev.sahilbasumatary.replayservice.entity.ReplayArtifact;
import dev.sahilbasumatary.replayservice.exception.ReplayStorageException;
import dev.sahilbasumatary.replayservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.replayservice.repository.ReplayArtifactRepository;
import dev.sahilbasumatary.replayservice.storage.CompressedPayload;
import dev.sahilbasumatary.replayservice.storage.ReplayObjectStore;
import dev.sahilbasumatary.replayservice.storage.ReplayPayloadCodec;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReplayArtifactServiceTest {

    private static final UUID MATCH_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String KEY = "replays/match/object.json.gz";

    private final ReplayPayloadCodec payloadCodec = new ReplayPayloadCodec();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private AtomicInteger generateCalls;
    private AtomicReference<MatchReplayResponse> nextReplay;
    private Map<UUID, ReplayArtifact> artifacts;
    private Map<String, byte[]> objects;
    private AtomicInteger putCalls;
    private AtomicReference<ReplayArtifact> deletedArtifact;
    private ReplayArtifactService service;

    @BeforeEach
    void setUp() {
        generateCalls = new AtomicInteger();
        nextReplay = new AtomicReference<>();
        artifacts = new HashMap<>();
        objects = new HashMap<>();
        putCalls = new AtomicInteger();
        deletedArtifact = new AtomicReference<>();

        ReplayGenerationService generationService =
                new ReplayGenerationService(null, null, null, null, null, null) {
                    @Override
                    public MatchReplayResponse generateMatchReplay(UUID matchId) {
                        generateCalls.incrementAndGet();
                        return nextReplay.get();
                    }
                };

        ReplayObjectStore objectStore =
                new ReplayObjectStore(
                        null,
                        new ReplayStorageProperties(
                                "http://localhost:9000",
                                "us-east-1",
                                "tennisly-replays",
                                "key",
                                "secret",
                                true,
                                true)) {
                    @Override
                    public String buildKey(UUID matchId, UUID artifactId) {
                        return KEY;
                    }

                    @Override
                    public void put(String key, byte[] data, String checksumSha256) {
                        putCalls.incrementAndGet();
                        objects.put(key, data);
                    }

                    @Override
                    public byte[] get(String key) {
                        byte[] data = objects.get(key);
                        if (data == null) {
                            throw new ReplayStorageException(
                                    "Failed to load replay object " + key, null);
                        }
                        return data;
                    }

                    @Override
                    public void delete(String key) {
                        objects.remove(key);
                    }
                };

        service =
                new ReplayArtifactService(
                        generationService,
                        proxyRepository(),
                        objectStore,
                        payloadCodec,
                        objectMapper);
    }

    @Test
    void materializeUploadsCompressedPayloadAndPersistsMetadata() {
        MatchReplayResponse replay = sampleReplay();
        nextReplay.set(replay);

        ReplayArtifactResponse response = service.materialize(MATCH_ID);

        assertEquals(1, putCalls.get());
        assertTrue(objects.containsKey(KEY));
        assertEquals(MATCH_ID, response.matchId());
        assertEquals(replay.frameCount(), response.frameCount());
        assertEquals(ReplayStatus.READY, response.status());
        assertEquals(KEY, response.storageKey());
        assertTrue(artifacts.containsKey(MATCH_ID));
    }

    @Test
    void materializeSkipsWhenArtifactAlreadyReady() {
        artifacts.put(MATCH_ID, readyArtifact("checksum"));

        ReplayArtifactResponse response = service.materialize(MATCH_ID);

        assertEquals(MATCH_ID, response.matchId());
        assertEquals(ReplayStatus.READY, response.status());
        assertEquals(0, generateCalls.get());
        assertEquals(0, putCalls.get());
    }

    @Test
    void getReplayServesStoredArtifactWithoutRegenerating() {
        MatchReplayResponse replay = sampleReplay();
        CompressedPayload payload = payloadCodec.compress(writeBytes(replay));
        ReplayArtifact artifact = readyArtifact(payload.checksumSha256());
        artifacts.put(MATCH_ID, artifact);
        objects.put(KEY, payload.data());

        MatchReplayResponse result = service.getReplay(MATCH_ID);

        assertEquals(replay.matchId(), result.matchId());
        assertEquals(replay.frameCount(), result.frameCount());
        assertEquals(0, generateCalls.get());
    }

    @Test
    void getReplayFallsBackToLiveGenerationWhenNotMaterialized() {
        MatchReplayResponse replay = sampleReplay();
        nextReplay.set(replay);

        MatchReplayResponse result = service.getReplay(MATCH_ID);

        assertSame(replay, result);
        assertEquals(1, generateCalls.get());
    }

    @Test
    void getReplayRegeneratesWhenStoredObjectIsCorrupt() {
        MatchReplayResponse replay = sampleReplay();
        nextReplay.set(replay);
        artifacts.put(MATCH_ID, readyArtifact("deadbeef"));
        objects.put(KEY, "not-gzip".getBytes());

        MatchReplayResponse result = service.getReplay(MATCH_ID);

        assertSame(replay, result);
        assertEquals(1, generateCalls.get());
    }

    @Test
    void deleteRemovesRowAndObject() {
        artifacts.put(MATCH_ID, readyArtifact("checksum"));
        objects.put(KEY, new byte[] {1});

        service.delete(MATCH_ID);

        assertFalse(artifacts.containsKey(MATCH_ID));
        assertFalse(objects.containsKey(KEY));
        assertEquals(MATCH_ID, deletedArtifact.get().getMatchId());
    }

    @Test
    void getArtifactThrowsWhenMissing() {
        assertThrows(ResourceNotFoundException.class, () -> service.getArtifact(MATCH_ID));
    }

    private ReplayArtifactRepository proxyRepository() {
        return (ReplayArtifactRepository)
                Proxy.newProxyInstance(
                        ReplayArtifactRepository.class.getClassLoader(),
                        new Class<?>[] {ReplayArtifactRepository.class},
                        (proxy, method, args) -> {
                            return switch (method.getName()) {
                                case "findByMatchId" ->
                                        Optional.ofNullable(artifacts.get((UUID) args[0]));
                                case "save" -> {
                                    ReplayArtifact artifact = (ReplayArtifact) args[0];
                                    artifacts.put(artifact.getMatchId(), artifact);
                                    yield artifact;
                                }
                                case "delete" -> {
                                    ReplayArtifact artifact = (ReplayArtifact) args[0];
                                    deletedArtifact.set(artifact);
                                    artifacts.remove(artifact.getMatchId());
                                    yield null;
                                }
                                default ->
                                        throw new UnsupportedOperationException(method.getName());
                            };
                        });
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
