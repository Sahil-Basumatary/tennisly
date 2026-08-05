package dev.sahilbasumatary.matchservice.service;

import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.common.kafka.EventPublisher;
import dev.sahilbasumatary.common.kafka.TopicNames;
import dev.sahilbasumatary.matchservice.client.TennisDataMatchClient;
import dev.sahilbasumatary.matchservice.client.TennisDataMatchClient.ResolvedPlayerDto;
import dev.sahilbasumatary.matchservice.client.TennisDataMatchClient.UpstreamMatchDto;
import dev.sahilbasumatary.matchservice.client.TennisDataMatchClient.UpstreamPointDto;
import dev.sahilbasumatary.matchservice.dto.response.MatchResponse;
import dev.sahilbasumatary.matchservice.entity.Match;
import dev.sahilbasumatary.matchservice.entity.MatchEventType;
import dev.sahilbasumatary.matchservice.entity.MatchPlayer;
import dev.sahilbasumatary.matchservice.entity.MatchPoint;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import dev.sahilbasumatary.matchservice.entity.PlayerSide;
import dev.sahilbasumatary.matchservice.entity.PointOutcome;
import dev.sahilbasumatary.matchservice.entity.Surface;
import dev.sahilbasumatary.matchservice.repository.MatchPointRepository;
import dev.sahilbasumatary.matchservice.repository.MatchRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "tennisly.ingest.enabled", havingValue = "true", matchIfMissing = true)
public class MatchIngestionService {

    private static final Logger log = LoggerFactory.getLogger(MatchIngestionService.class);

    private final TennisDataMatchClient matchClient;
    private final MatchRepository matchRepository;
    private final MatchPointRepository pointRepository;
    private final MatchEventLogService eventLogService;
    private final MatchRealtimeNotifier realtimeNotifier;
    private final EventPublisher eventPublisher;
    private final int pageSize;

    public MatchIngestionService(
            TennisDataMatchClient matchClient,
            MatchRepository matchRepository,
            MatchPointRepository pointRepository,
            MatchEventLogService eventLogService,
            MatchRealtimeNotifier realtimeNotifier,
            EventPublisher eventPublisher,
            @Value("${tennisly.ingest.page-size:50}") int pageSize) {
        this.matchClient = matchClient;
        this.matchRepository = matchRepository;
        this.pointRepository = pointRepository;
        this.eventLogService = eventLogService;
        this.realtimeNotifier = realtimeNotifier;
        this.eventPublisher = eventPublisher;
        this.pageSize = Math.max(1, Math.min(pageSize, 100));
    }

    @Scheduled(fixedDelayString = "${tennisly.ingest.live-fixed-delay-ms:60000}")
    public void ingestLiveBoard() {
        ingestStatus("live");
        ingestStatus("upcoming");
    }

    @Scheduled(fixedDelayString = "${tennisly.ingest.completed-fixed-delay-ms:21600000}")
    public void ingestCompletedBoard() {
        ingestStatus("completed");
    }

    public int ingestStatus(String status) {
        List<UpstreamMatchDto> upstream = matchClient.listMatches(status, pageSize, 0);
        int upserted = 0;
        for (UpstreamMatchDto dto : upstream) {
            if (dto.doubles()) {
                continue;
            }
            try {
                if (upsertMatch(dto)) {
                    upserted += 1;
                }
            } catch (RuntimeException ex) {
                log.warn(
                        "Skipping upstream match {} ({}): {}",
                        dto.externalId(),
                        dto.providerMatchId(),
                        ex.getMessage());
            }
        }
        log.info("Ingested status={} upserted={}", status, upserted);
        return upserted;
    }

    @Transactional
    public boolean upsertMatch(UpstreamMatchDto dto) {
        Optional<ResolvedPlayerDto> home =
                matchClient.resolvePlayer(
                        dto.home() == null ? null : dto.home().providerPlayerId(),
                        dto.home() == null ? null : dto.home().firstName(),
                        dto.home() == null ? null : dto.home().lastName(),
                        dto.home() == null ? "Home" : dto.home().displayName(),
                        null);
        Optional<ResolvedPlayerDto> away =
                matchClient.resolvePlayer(
                        dto.away() == null ? null : dto.away().providerPlayerId(),
                        dto.away() == null ? null : dto.away().firstName(),
                        dto.away() == null ? null : dto.away().lastName(),
                        dto.away() == null ? "Away" : dto.away().displayName(),
                        null);
        if (home.isEmpty() || away.isEmpty()) {
            throw new IllegalStateException("Unable to resolve both players");
        }

        MatchStatus nextStatus = mapStatus(dto.status());
        Match match =
                matchRepository
                        .findByExternalId(dto.externalId())
                        .orElseGet(Match::new);
        boolean created = match.getId() == null;
        MatchStatus previousStatus = match.getStatus();

        match.setExternalId(dto.externalId());
        match.setSurface(mapSurface(dto.surface()));
        match.setBestOfSets(mapBestOf(dto.format()));
        match.setScheduledAt(dto.scheduledAt());
        match.setMetadata(buildMetadata(dto, home.get(), away.get()));
        applyStatusTimestamps(match, nextStatus);
        match.setStatus(nextStatus);

        if (created) {
            match.addPlayer(toPlayer(home.get(), PlayerSide.HOME));
            match.addPlayer(toPlayer(away.get(), PlayerSide.AWAY));
            match.setCurrentScore(emptyScore(home.get().id(), away.get().id()));
        } else {
            for (MatchPlayer player : match.getPlayers()) {
                if (player.getSide() == PlayerSide.HOME) {
                    player.setPlayerId(home.get().id());
                    player.setDisplayName(displayName(home.get()));
                } else if (player.getSide() == PlayerSide.AWAY) {
                    player.setPlayerId(away.get().id());
                    player.setDisplayName(displayName(away.get()));
                }
            }
        }

        if (nextStatus == MatchStatus.COMPLETED) {
            replacePoints(match, dto.providerMatchId(), home.get().id(), away.get().id());
        }

        Match saved = matchRepository.save(match);
        if (created) {
            eventLogService.append(saved, MatchEventType.CREATED, Map.of("status", nextStatus));
            publish(saved, MatchEvent.created(saved.getId(), nextStatus.name()));
        } else if (previousStatus != nextStatus) {
            eventLogService.append(
                    saved, MatchEventType.STATUS_CHANGED, Map.of("status", nextStatus));
            publish(saved, MatchEvent.statusChanged(saved.getId(), nextStatus.name()));
        } else {
            eventLogService.append(saved, MatchEventType.UPDATED, Map.of("status", nextStatus));
            publish(saved, MatchEvent.updated(saved.getId(), nextStatus.name()));
        }
        return true;
    }

    private void replacePoints(Match match, long ltaId, UUID homeId, UUID awayId) {
        List<UpstreamPointDto> tape = matchClient.listPoints(ltaId);
        if (tape.isEmpty()) {
            return;
        }
        if (match.getId() != null) {
            pointRepository.deleteByMatchId(match.getId());
        }
        match.getPoints().clear();
        MatchPoint last = null;
        for (UpstreamPointDto row : tape) {
            MatchPoint point = new MatchPoint();
            point.setSequenceNumber(row.sequenceNumber());
            point.setServerId(row.serverSide() == 2 ? awayId : homeId);
            point.setWinnerId(row.winnerSide() == 2 ? awayId : homeId);
            point.setOutcome(mapOutcome(row.outcome()));
            point.setRallyLength(row.rallyLength());
            point.setScoreSnapshot(
                    row.scoreSnapshot() == null ? Map.of() : new HashMap<>(row.scoreSnapshot()));
            point.setShotSummary(Map.of("source", "livetennis-tape"));
            match.addPoint(point);
            last = point;
        }
        if (last != null) {
            match.setCurrentScore(new HashMap<>(last.getScoreSnapshot()));
        }
    }

    private void publish(Match match, MatchEvent event) {
        MatchResponse response = MatchResponse.from(match);
        realtimeNotifier.publishSnapshot(response);
        eventPublisher.publish(TopicNames.MATCH_EVENTS, match.getId().toString(), event);
    }

    private static MatchPlayer toPlayer(ResolvedPlayerDto player, PlayerSide side) {
        MatchPlayer row = new MatchPlayer();
        row.setPlayerId(player.id());
        row.setDisplayName(displayName(player));
        row.setSide(side);
        return row;
    }

    private static String displayName(ResolvedPlayerDto player) {
        return (nullToEmpty(player.firstName()) + " " + nullToEmpty(player.lastName())).trim();
    }

    private static Map<String, Object> buildMetadata(
            UpstreamMatchDto dto, ResolvedPlayerDto home, ResolvedPlayerDto away) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tournamentName", dto.tournamentName());
        metadata.put("round", dto.round());
        metadata.put("indoor", dto.indoor());
        metadata.put("provider", "livetennis");
        metadata.put("providerMatchId", dto.providerMatchId());
        metadata.put("homeExternalId", home.externalId());
        metadata.put("awayExternalId", away.externalId());
        metadata.put("homeNationality", home.nationality());
        metadata.put("awayNationality", away.nationality());
        metadata.put("replayReady", dto.status() != null && dto.status().equalsIgnoreCase("completed"));
        return metadata;
    }

    private static Map<String, Object> emptyScore(UUID homeId, UUID awayId) {
        Map<String, Object> score = new HashMap<>();
        score.put("sets", List.of());
        score.put("games", List.of());
        score.put("points", List.of("0", "0"));
        score.put("serverId", homeId.toString());
        score.put("players", List.of(homeId.toString(), awayId.toString()));
        return score;
    }

    private static void applyStatusTimestamps(Match match, MatchStatus next) {
        Instant now = Instant.now();
        if (next == MatchStatus.IN_PROGRESS && match.getStartedAt() == null) {
            match.setStartedAt(now);
        }
        if (next == MatchStatus.COMPLETED || next == MatchStatus.CANCELLED) {
            if (match.getStartedAt() == null) {
                match.setStartedAt(now);
            }
            match.setEndedAt(now);
        }
    }

    private static MatchStatus mapStatus(String raw) {
        if (raw == null) {
            return MatchStatus.SCHEDULED;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "live", "in_progress", "inprogress" -> MatchStatus.IN_PROGRESS;
            case "completed", "finished", "final" -> MatchStatus.COMPLETED;
            case "cancelled", "canceled", "retired", "walkover" -> MatchStatus.CANCELLED;
            case "suspended" -> MatchStatus.SUSPENDED;
            default -> MatchStatus.SCHEDULED;
        };
    }

    private static Surface mapSurface(String raw) {
        if (raw == null) {
            return Surface.HARD;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "CLAY" -> Surface.CLAY;
            case "GRASS" -> Surface.GRASS;
            default -> Surface.HARD;
        };
    }

    private static int mapBestOf(String format) {
        if (format != null && format.toUpperCase(Locale.ROOT).contains("BO5")) {
            return 5;
        }
        return 3;
    }

    private static PointOutcome mapOutcome(String raw) {
        if (raw == null || raw.isBlank()) {
            return PointOutcome.UNKNOWN;
        }
        try {
            return PointOutcome.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return PointOutcome.UNKNOWN;
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
