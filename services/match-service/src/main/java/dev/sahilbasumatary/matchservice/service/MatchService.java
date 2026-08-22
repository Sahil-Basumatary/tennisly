package dev.sahilbasumatary.matchservice.service;

import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.matchservice.dto.request.CreateMatchPlayerRequest;
import dev.sahilbasumatary.matchservice.dto.request.CreateMatchRequest;
import dev.sahilbasumatary.matchservice.dto.request.RecordPointRequest;
import dev.sahilbasumatary.matchservice.dto.request.UpdateMatchRequest;
import dev.sahilbasumatary.matchservice.dto.request.UpdateMatchStatusRequest;
import dev.sahilbasumatary.matchservice.dto.response.CompletedMatchFeedResponse;
import dev.sahilbasumatary.matchservice.dto.response.MatchEventLogResponse;
import dev.sahilbasumatary.matchservice.dto.response.MatchPointResponse;
import dev.sahilbasumatary.matchservice.dto.response.MatchResponse;
import dev.sahilbasumatary.matchservice.entity.Match;
import dev.sahilbasumatary.matchservice.entity.MatchEventType;
import dev.sahilbasumatary.matchservice.entity.MatchPlayer;
import dev.sahilbasumatary.matchservice.entity.MatchPoint;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import dev.sahilbasumatary.matchservice.entity.PlayerSide;
import dev.sahilbasumatary.matchservice.exception.DuplicateResourceException;
import dev.sahilbasumatary.matchservice.exception.InvalidMatchStateException;
import dev.sahilbasumatary.matchservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.matchservice.metrics.MatchTimers;
import dev.sahilbasumatary.matchservice.repository.MatchEventLogRepository;
import dev.sahilbasumatary.matchservice.repository.MatchPointRepository;
import dev.sahilbasumatary.matchservice.repository.MatchRepository;
import dev.sahilbasumatary.matchservice.web.PageBounds;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);
    private final MatchRepository matchRepository;
    private final MatchPointRepository pointRepository;
    private final MatchEventLogRepository eventLogRepository;
    private final MatchStateMachine stateMachine;
    private final MatchEventLogService eventLogService;
    private final MatchRealtimeNotifier realtimeNotifier;
    private final MatchEventDispatch eventDispatch;
    private final MatchTimers matchTimers;

    public MatchService(
            MatchRepository matchRepository,
            MatchPointRepository pointRepository,
            MatchEventLogRepository eventLogRepository,
            MatchStateMachine stateMachine,
            MatchEventLogService eventLogService,
            MatchRealtimeNotifier realtimeNotifier,
            MatchEventDispatch eventDispatch,
            MatchTimers matchTimers) {
        this.matchRepository = matchRepository;
        this.pointRepository = pointRepository;
        this.eventLogRepository = eventLogRepository;
        this.stateMachine = stateMachine;
        this.eventLogService = eventLogService;
        this.realtimeNotifier = realtimeNotifier;
        this.eventDispatch = eventDispatch;
        this.matchTimers = matchTimers;
    }

    @Transactional
    public MatchResponse createMatch(CreateMatchRequest request) {
        assertExternalIdAvailable(request.externalId(), null);
        validatePlayers(request.players());
        Match match = new Match();
        match.setExternalId(request.externalId());
        match.setTournamentId(request.tournamentId());
        match.setSurface(request.surface());
        match.setBestOfSets(request.bestOfSets() == null ? 3 : request.bestOfSets());
        match.setScheduledAt(request.scheduledAt());
        match.setMetadata(request.metadata());
        match.setCurrentScore(initialScore(request.players()));
        request.players().forEach(playerRequest -> match.addPlayer(toPlayer(playerRequest)));
        Match saved = matchRepository.saveAndFlush(match);
        long sequence =
                eventLogService.append(
                        saved, MatchEventType.CREATED, Map.of("status", saved.getStatus()));
        MatchResponse response = MatchResponse.from(saved);
        publish(
                saved.getId(),
                MatchEvent.created(saved.getId(), saved.getStatus().name()),
                response,
                sequence);
        log.info("Created match matchId={} status={}", saved.getId(), saved.getStatus());
        return response;
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> listMatches(
            MatchStatus status, UUID tournamentId, Integer page, Integer size) {
        Pageable pageable = PageBounds.of(page, size);
        List<Match> matches;
        if (tournamentId != null && status != null) {
            matches =
                    matchRepository.findByTournamentIdAndStatusOrderByScheduledAtAsc(
                            tournamentId, status, pageable);
        } else if (tournamentId != null) {
            matches = matchRepository.findByTournamentIdOrderByScheduledAtAsc(tournamentId, pageable);
        } else if (status != null) {
            matches = matchRepository.findByStatusOrderByScheduledAtAsc(status, pageable);
        } else {
            matches = matchRepository.findAllByOrderByScheduledAtAsc(pageable);
        }
        return matches.stream().map(MatchResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MatchResponse getMatch(UUID matchId) {
        Timer.Sample sample = Timer.start();
        try {
            var cached = realtimeNotifier.findCachedSnapshot(matchId);
            if (cached.isPresent()) {
                matchTimers.liveCacheHit().increment();
                return cached.get();
            }
            matchTimers.liveCacheMiss().increment();
            Match match = findMatch(matchId);
            return MatchResponse.from(match);
        } finally {
            sample.stop(matchTimers.getMatch());
        }
    }

    @Transactional(readOnly = true)
    public MatchResponse getMatchByExternalId(String externalId) {
        Match match =
                matchRepository
                        .findByExternalId(externalId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Match externalId", externalId));
        return MatchResponse.from(match);
    }

    @Transactional
    public MatchResponse updateMatch(UUID matchId, UpdateMatchRequest request) {
        Match match = findMatch(matchId);
        assertMutable(match);
        assertExternalIdAvailable(request.externalId(), matchId);
        if (request.externalId() != null) {
            match.setExternalId(request.externalId());
        }
        if (request.tournamentId() != null) {
            match.setTournamentId(request.tournamentId());
        }
        if (request.surface() != null) {
            match.setSurface(request.surface());
        }
        if (request.bestOfSets() != null) {
            match.setBestOfSets(request.bestOfSets());
        }
        if (request.scheduledAt() != null) {
            match.setScheduledAt(request.scheduledAt());
        }
        if (request.metadata() != null) {
            match.setMetadata(request.metadata());
        }
        Match saved = matchRepository.save(match);
        long sequence =
                eventLogService.append(
                        saved, MatchEventType.UPDATED, Map.of("status", saved.getStatus()));
        MatchResponse response = MatchResponse.from(saved);
        publish(
                saved.getId(),
                MatchEvent.updated(saved.getId(), saved.getStatus().name()),
                response,
                sequence);
        log.info("Updated match matchId={}", saved.getId());
        return response;
    }

    @Transactional
    public MatchResponse updateStatus(UUID matchId, UpdateMatchStatusRequest request) {
        Match match = findMatch(matchId);
        MatchStatus nextStatus = request.status();
        stateMachine.assertCanTransition(match.getStatus(), nextStatus);
        if (match.getStatus() != nextStatus) {
            applyStatusTimestamps(match, nextStatus);
            match.setStatus(nextStatus);
        }
        if (request.metadata() != null && !request.metadata().isEmpty()) {
            Map<String, Object> metadata = new HashMap<>(match.getMetadata());
            metadata.putAll(request.metadata());
            match.setMetadata(metadata);
        }
        Match saved = matchRepository.save(match);
        long sequence =
                eventLogService.append(
                        saved, MatchEventType.STATUS_CHANGED, Map.of("status", nextStatus));
        MatchResponse response = MatchResponse.from(saved);
        publish(
                saved.getId(),
                MatchEvent.statusChanged(saved.getId(), nextStatus.name()),
                response,
                sequence);
        log.info("Changed match status matchId={} status={}", saved.getId(), nextStatus);
        return response;
    }

    @Transactional
    public MatchPointResponse recordPoint(UUID matchId, RecordPointRequest request) {
        Timer.Sample sample = Timer.start();
        try {
            Match match = findMatch(matchId);
            stateMachine.assertCanRecordPoint(match.getStatus());
            assertPlayerInMatch(match, request.serverId());
            assertPlayerInMatch(match, request.winnerId());
            MatchPoint point = new MatchPoint();
            point.setSequenceNumber(match.getPointCount() + 1);
            point.setServerId(request.serverId());
            point.setWinnerId(request.winnerId());
            point.setOutcome(request.outcome());
            point.setRallyLength(request.rallyLength());
            point.setScoreSnapshot(request.scoreSnapshot());
            point.setShotSummary(request.shotSummary());
            point.setMatch(match);
            match.setCurrentScore(request.scoreSnapshot());
            match.setPointCount(match.getPointCount() + 1);
            pointRepository.save(point);
            matchRepository.save(match);
            long sequence =
                    eventLogService.append(
                            match, MatchEventType.POINT_RECORDED, pointPayload(point));
            MatchResponse response = MatchResponse.from(match);
            publish(
                    matchId,
                    MatchEvent.pointRecorded(
                            matchId,
                            match.getStatus().name(),
                            point.getSequenceNumber(),
                            point.getWinnerId(),
                            point.getOutcome().name()),
                    response,
                    sequence);
            log.info(
                    "Recorded point matchId={} sequence={} winnerId={}",
                    matchId,
                    point.getSequenceNumber(),
                    point.getWinnerId());
            return MatchPointResponse.from(point);
        } finally {
            sample.stop(matchTimers.recordPoint());
        }
    }

    @Transactional(readOnly = true)
    public List<MatchPointResponse> listPoints(UUID matchId) {
        findMatch(matchId);
        return pointRepository.findByMatchIdOrderBySequenceNumberAsc(matchId).stream()
                .map(MatchPointResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MatchEventLogResponse> listEvents(UUID matchId, long afterSequence, int limit) {
        findMatch(matchId);
        int clamped = Math.max(1, Math.min(limit, 1_000));
        long cursor = Math.max(0, afterSequence);
        return eventLogRepository
                .findByMatchIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
                        matchId, cursor, PageRequest.of(0, clamped))
                .stream()
                .map(MatchEventLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompletedMatchFeedResponse listCompletedMatchIds(UUID cursor, int limit) {
        int clamped = Math.max(1, Math.min(limit, 100));
        Pageable pageable = PageRequest.of(0, clamped + 1);
        // Postgres cannot infer a bind type for a null UUID compared with IS NULL, so the
        // unseeded first page uses its own query rather than a nullable cursor predicate.
        List<Match> matches =
                cursor == null
                        ? matchRepository.findByStatusOrderByIdAsc(MatchStatus.COMPLETED, pageable)
                        : matchRepository.findByStatusAndIdGreaterThanOrderByIdAsc(
                                MatchStatus.COMPLETED, cursor, pageable);
        boolean hasMore = matches.size() > clamped;
        List<UUID> matchIds = matches.stream().limit(clamped).map(Match::getId).toList();
        UUID nextCursor = matchIds.isEmpty() ? cursor : matchIds.get(matchIds.size() - 1);
        return new CompletedMatchFeedResponse(matchIds, nextCursor, hasMore);
    }

    private Match findMatch(UUID matchId) {
        return matchRepository
                .findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", matchId));
    }

    private void assertExternalIdAvailable(String externalId, UUID currentMatchId) {
        if (externalId == null || externalId.isBlank()) {
            return;
        }
        matchRepository
                .findByExternalId(externalId)
                .filter(existing -> !existing.getId().equals(currentMatchId))
                .ifPresent(
                        existing -> {
                            throw new DuplicateResourceException("Match externalId", externalId);
                        });
    }

    private void validatePlayers(List<CreateMatchPlayerRequest> players) {
        Set<UUID> playerIds = new HashSet<>();
        Set<PlayerSide> sides = new HashSet<>();
        for (CreateMatchPlayerRequest player : players) {
            if (!playerIds.add(player.playerId())) {
                throw new InvalidMatchStateException("A match cannot contain the same player twice");
            }
            if (!sides.add(player.side())) {
                throw new InvalidMatchStateException("A match must contain one HOME and one AWAY player");
            }
        }
        if (!sides.containsAll(Set.of(PlayerSide.HOME, PlayerSide.AWAY))) {
            throw new InvalidMatchStateException("A match must contain one HOME and one AWAY player");
        }
    }

    private MatchPlayer toPlayer(CreateMatchPlayerRequest request) {
        MatchPlayer player = new MatchPlayer();
        player.setPlayerId(request.playerId());
        player.setDisplayName(request.displayName());
        player.setSide(request.side());
        player.setSeedNumber(request.seedNumber());
        return player;
    }

    private Map<String, Object> initialScore(List<CreateMatchPlayerRequest> players) {
        Map<String, Object> score = new HashMap<>();
        score.put("sets", List.of());
        score.put("game", Map.of("HOME", "0", "AWAY", "0"));
        score.put(
                "players",
                players.stream()
                        .map(player -> Map.of("playerId", player.playerId(), "side", player.side()))
                        .toList());
        return score;
    }

    private void assertMutable(Match match) {
        if (match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.CANCELLED) {
            throw new InvalidMatchStateException(
                    "Completed or cancelled matches cannot be changed");
        }
    }

    private void applyStatusTimestamps(Match match, MatchStatus nextStatus) {
        Instant now = Instant.now();
        if (nextStatus == MatchStatus.IN_PROGRESS && match.getStartedAt() == null) {
            match.setStartedAt(now);
        }
        if (nextStatus == MatchStatus.COMPLETED || nextStatus == MatchStatus.CANCELLED) {
            match.setEndedAt(now);
        }
    }

    private void assertPlayerInMatch(Match match, UUID playerId) {
        boolean exists =
                match.getPlayers().stream().anyMatch(player -> player.getPlayerId().equals(playerId));
        if (!exists) {
            throw new InvalidMatchStateException("Player " + playerId + " is not part of this match");
        }
    }

    private Map<String, Object> pointPayload(MatchPoint point) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sequenceNumber", point.getSequenceNumber());
        payload.put("serverId", point.getServerId());
        payload.put("winnerId", point.getWinnerId());
        payload.put("outcome", point.getOutcome());
        payload.put("rallyLength", point.getRallyLength());
        payload.put("scoreSnapshot", point.getScoreSnapshot());
        return payload;
    }

    private void publish(
            UUID matchId, MatchEvent event, MatchResponse response, long liveSequence) {
        event.setSequence(liveSequence);
        eventDispatch.publish(matchId, event, response);
    }
}
