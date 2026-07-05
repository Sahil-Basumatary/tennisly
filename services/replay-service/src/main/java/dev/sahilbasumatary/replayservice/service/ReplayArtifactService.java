package dev.sahilbasumatary.replayservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.replayservice.domain.ReplayStatus;
import dev.sahilbasumatary.replayservice.dto.response.MatchReplayResponse;
import dev.sahilbasumatary.replayservice.dto.response.ReplayArtifactResponse;
import dev.sahilbasumatary.replayservice.entity.ReplayArtifact;
import dev.sahilbasumatary.replayservice.exception.ReplayStorageException;
import dev.sahilbasumatary.replayservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.replayservice.repository.ReplayArtifactRepository;
import dev.sahilbasumatary.replayservice.storage.CompressedPayload;
import dev.sahilbasumatary.replayservice.storage.ReplayObjectStore;
import dev.sahilbasumatary.replayservice.storage.ReplayPayloadCodec;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the lifecycle of persisted replays: materialise a generated replay into object storage, serve
 * it back on demand, and invalidate it. Because generation is deterministic, a stored replay and a
 * freshly generated one are identical, so a cache miss safely falls back to live generation.
 */
@Service
public class ReplayArtifactService {

    private static final String ENGINE_VERSION = "1.0.0";
    private static final Logger log = LoggerFactory.getLogger(ReplayArtifactService.class);

    private final ReplayGenerationService replayGenerationService;
    private final ReplayArtifactRepository artifactRepository;
    private final ReplayObjectStore objectStore;
    private final ReplayPayloadCodec payloadCodec;
    private final ObjectMapper objectMapper;

    public ReplayArtifactService(
            ReplayGenerationService replayGenerationService,
            ReplayArtifactRepository artifactRepository,
            ReplayObjectStore objectStore,
            ReplayPayloadCodec payloadCodec,
            ObjectMapper objectMapper) {
        this.replayGenerationService = replayGenerationService;
        this.artifactRepository = artifactRepository;
        this.objectStore = objectStore;
        this.payloadCodec = payloadCodec;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ReplayArtifactResponse materialize(UUID matchId) {
        MatchReplayResponse replay = replayGenerationService.generateMatchReplay(matchId);
        CompressedPayload payload = payloadCodec.compress(serialize(replay));

        UUID objectId = UUID.randomUUID();
        String key = objectStore.buildKey(matchId, objectId);
        objectStore.put(key, payload.data(), payload.checksumSha256());

        ReplayArtifact artifact =
                artifactRepository.findByMatchId(matchId).orElseGet(ReplayArtifact::new);
        String previousKey = artifact.getStorageKey();
        applyMetadata(artifact, matchId, replay, key, payload);

        ReplayArtifact saved;
        try {
            saved = artifactRepository.save(artifact);
        } catch (RuntimeException ex) {
            objectStore.delete(key);
            throw ex;
        }

        if (previousKey != null && !previousKey.equals(key)) {
            deleteQuietly(previousKey);
        }
        log.info(
                "Materialized replay matchId={} key={} sizeBytes={} ratio={}",
                matchId,
                key,
                payload.sizeBytes(),
                payload.uncompressedBytes() == 0
                        ? 0
                        : payload.uncompressedBytes() / Math.max(1, payload.sizeBytes()));
        return ReplayArtifactResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public MatchReplayResponse getReplay(UUID matchId) {
        return artifactRepository
                .findByMatchId(matchId)
                .filter(artifact -> artifact.getStatus() == ReplayStatus.READY)
                .map(this::loadOrRegenerate)
                .orElseGet(() -> replayGenerationService.generateMatchReplay(matchId));
    }

    @Transactional(readOnly = true)
    public ReplayArtifactResponse getArtifact(UUID matchId) {
        return artifactRepository
                .findByMatchId(matchId)
                .map(ReplayArtifactResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Replay artifact", matchId));
    }

    @Transactional
    public void delete(UUID matchId) {
        ReplayArtifact artifact =
                artifactRepository
                        .findByMatchId(matchId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Replay artifact", matchId));
        artifactRepository.delete(artifact);
        deleteQuietly(artifact.getStorageKey());
        log.info("Deleted replay artifact matchId={} key={}", matchId, artifact.getStorageKey());
    }

    private MatchReplayResponse loadOrRegenerate(ReplayArtifact artifact) {
        try {
            byte[] compressed = objectStore.get(artifact.getStorageKey());
            byte[] raw = payloadCodec.decompress(compressed, artifact.getChecksumSha256());
            return deserialize(raw);
        } catch (ReplayStorageException ex) {
            log.warn(
                    "Falling back to live generation for matchId={} after storage read failure: {}",
                    artifact.getMatchId(),
                    ex.getMessage());
            return replayGenerationService.generateMatchReplay(artifact.getMatchId());
        }
    }

    private void applyMetadata(
            ReplayArtifact artifact,
            UUID matchId,
            MatchReplayResponse replay,
            String key,
            CompressedPayload payload) {
        artifact.setMatchId(matchId);
        artifact.setStorageBucket(objectStore.bucket());
        artifact.setStorageKey(key);
        artifact.setSurface(replay.surface());
        artifact.setFrameRate(replay.frameRate());
        artifact.setPointCount(replay.pointCount());
        artifact.setShotCount(replay.shotCount());
        artifact.setFrameCount(replay.frameCount());
        artifact.setDurationSeconds(replay.durationSeconds());
        artifact.setContentEncoding("gzip");
        artifact.setSizeBytes(payload.sizeBytes());
        artifact.setUncompressedBytes(payload.uncompressedBytes());
        artifact.setChecksumSha256(payload.checksumSha256());
        artifact.setEngineVersion(ENGINE_VERSION);
        artifact.setStatus(ReplayStatus.READY);
    }

    private void deleteQuietly(String key) {
        if (key == null) {
            return;
        }
        try {
            objectStore.delete(key);
        } catch (ReplayStorageException ex) {
            log.warn("Failed to delete stale replay object {}: {}", key, ex.getMessage());
        }
    }

    private byte[] serialize(MatchReplayResponse replay) {
        try {
            return objectMapper.writeValueAsBytes(replay);
        } catch (JsonProcessingException ex) {
            throw new ReplayStorageException("Failed to serialize replay payload", ex);
        }
    }

    private MatchReplayResponse deserialize(byte[] raw) {
        try {
            return objectMapper.readValue(raw, MatchReplayResponse.class);
        } catch (java.io.IOException ex) {
            throw new ReplayStorageException("Failed to deserialize replay payload", ex);
        }
    }
}
