package dev.sahilbasumatary.replayservice.service;

import dev.sahilbasumatary.replayservice.client.MatchDataClient;
import dev.sahilbasumatary.replayservice.client.ShotDistributionClient;
import dev.sahilbasumatary.replayservice.client.dto.MatchPlayerSummary;
import dev.sahilbasumatary.replayservice.client.dto.MatchPointSummary;
import dev.sahilbasumatary.replayservice.client.dto.MatchSummary;
import dev.sahilbasumatary.replayservice.config.ReplayEngineProperties;
import dev.sahilbasumatary.replayservice.domain.PlayerSide;
import dev.sahilbasumatary.replayservice.domain.PlayerTier;
import dev.sahilbasumatary.replayservice.domain.PointOutcome;
import dev.sahilbasumatary.replayservice.domain.ShotType;
import dev.sahilbasumatary.replayservice.dto.response.MatchReplayResponse;
import dev.sahilbasumatary.replayservice.dto.response.PointReplayResponse;
import dev.sahilbasumatary.replayservice.dto.response.PointReplaySummary;
import dev.sahilbasumatary.replayservice.dto.response.ReplayFrame;
import dev.sahilbasumatary.replayservice.dto.response.ShotSummaryResponse;
import dev.sahilbasumatary.replayservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.replayservice.trajectory.FrameAssembler;
import dev.sahilbasumatary.replayservice.trajectory.PointTrajectory;
import dev.sahilbasumatary.replayservice.trajectory.RallySynthesizer;
import dev.sahilbasumatary.replayservice.trajectory.ShotDistributionIndex;
import dev.sahilbasumatary.replayservice.trajectory.TrajectoryEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Coordinates a replay generation run: pull the point ledger and shot priors from the upstream
 * services, drive the physics engine per point with a deterministic seed, then resample the result
 * into frames. Generation is pure and side-effect free, so identical inputs always yield identical
 * replays.
 */
@Service
public class ReplayGenerationService {

    private static final long POINT_SEED_PRIME = 0x9E3779B97F4A7C15L;
    private static final Logger log = LoggerFactory.getLogger(ReplayGenerationService.class);

    private final MatchDataClient matchDataClient;
    private final ShotDistributionClient shotDistributionClient;
    private final RallySynthesizer rallySynthesizer;
    private final TrajectoryEngine trajectoryEngine;
    private final FrameAssembler frameAssembler;
    private final ReplayEngineProperties engineProperties;

    public ReplayGenerationService(
            MatchDataClient matchDataClient,
            ShotDistributionClient shotDistributionClient,
            RallySynthesizer rallySynthesizer,
            TrajectoryEngine trajectoryEngine,
            FrameAssembler frameAssembler,
            ReplayEngineProperties engineProperties) {
        this.matchDataClient = matchDataClient;
        this.shotDistributionClient = shotDistributionClient;
        this.rallySynthesizer = rallySynthesizer;
        this.trajectoryEngine = trajectoryEngine;
        this.frameAssembler = frameAssembler;
        this.engineProperties = engineProperties;
    }

    public MatchReplayResponse generateMatchReplay(UUID matchId) {
        MatchSummary match = matchDataClient.fetchMatch(matchId);
        List<MatchPointSummary> points = matchDataClient.fetchPoints(matchId);
        ShotDistributionIndex index =
                ShotDistributionIndex.from(shotDistributionClient.fetchBySurface(match.surface()));

        List<PointReplaySummary> pointSummaries = new ArrayList<>(points.size());
        List<ShotSummaryResponse> shotSummaries = new ArrayList<>();
        List<ReplayFrame> frames = new ArrayList<>();
        double cursor = 0.0;
        for (MatchPointSummary point : points) {
            PointTrajectory trajectory = simulatePoint(matchId, match, point, index);
            frames.addAll(
                    frameAssembler.framesForPoint(
                            trajectory, cursor, engineProperties.framesPerSecond()));
            shotSummaries.addAll(frameAssembler.shotSummaries(trajectory));
            pointSummaries.add(toPointSummary(point, trajectory));
            cursor += trajectory.durationSeconds();
        }

        log.info(
                "Generated match replay matchId={} points={} shots={} frames={} duration={}s",
                matchId,
                pointSummaries.size(),
                shotSummaries.size(),
                frames.size(),
                cursor);
        return new MatchReplayResponse(
                matchId,
                match.surface(),
                engineProperties.framesPerSecond(),
                pointSummaries.size(),
                shotSummaries.size(),
                frames.size(),
                round(cursor),
                pointSummaries,
                shotSummaries,
                frames);
    }

    public PointReplayResponse generatePointReplay(UUID matchId, int sequenceNumber) {
        MatchSummary match = matchDataClient.fetchMatch(matchId);
        MatchPointSummary point =
                matchDataClient.fetchPoints(matchId).stream()
                        .filter(candidate -> candidate.sequenceNumber() == sequenceNumber)
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Match point", matchId + "#" + sequenceNumber));
        ShotDistributionIndex index =
                ShotDistributionIndex.from(shotDistributionClient.fetchBySurface(match.surface()));

        PointTrajectory trajectory = simulatePoint(matchId, match, point, index);
        List<ReplayFrame> frames =
                frameAssembler.framesForPoint(trajectory, 0.0, engineProperties.framesPerSecond());
        List<ShotSummaryResponse> shots = frameAssembler.shotSummaries(trajectory);
        return new PointReplayResponse(
                matchId,
                match.surface(),
                engineProperties.framesPerSecond(),
                toPointSummary(point, trajectory),
                shots,
                frames);
    }

    private PointTrajectory simulatePoint(
            UUID matchId, MatchSummary match, MatchPointSummary point, ShotDistributionIndex index) {
        PointOutcome outcome = PointOutcome.fromExternal(point.outcome());
        PlayerSide serverSide = sideOf(match, point.serverId());
        PlayerSide receiverSide =
                serverSide == PlayerSide.HOME ? PlayerSide.AWAY : PlayerSide.HOME;
        PlayerTier serverTier = tierOf(match.playerOn(serverSide));
        PlayerTier receiverTier = tierOf(match.playerOn(receiverSide));
        long seed = pointSeed(matchId, point.sequenceNumber());
        int rallyLength =
                point.rallyLength() == null || point.rallyLength() <= 0
                        ? synthesizedRallyLength(seed, outcome)
                        : point.rallyLength();

        List<ShotType> shotTypes =
                rallySynthesizer.synthesize(
                        rallyLength,
                        outcome,
                        engineProperties.maxRallyLength(),
                        new SplittableRandom(seed));

        return trajectoryEngine.generate(
                point.sequenceNumber(),
                serverSide,
                shotTypes,
                serverTier,
                receiverTier,
                match.surface(),
                index,
                seed ^ POINT_SEED_PRIME,
                engineProperties);
    }

    private PointReplaySummary toPointSummary(MatchPointSummary point, PointTrajectory trajectory) {
        return new PointReplaySummary(
                point.sequenceNumber(),
                point.serverId(),
                point.winnerId(),
                PointOutcome.fromExternal(point.outcome()),
                point.rallyLength() == null ? trajectory.shots().size() : point.rallyLength(),
                trajectory.shots().size(),
                round(trajectory.durationSeconds()),
                point.scoreSnapshot() == null ? Map.of() : point.scoreSnapshot());
    }

    private static int synthesizedRallyLength(long seed, PointOutcome outcome) {
        if (outcome == PointOutcome.ACE) {
            return 1;
        }
        if (outcome == PointOutcome.DOUBLE_FAULT) {
            return 0;
        }
        return 4 + (int) Math.floorMod(seed, 5L);
    }

    private PlayerSide sideOf(MatchSummary match, UUID playerId) {
        return match.players().stream()
                .filter(player -> player.playerId().equals(playerId))
                .map(MatchPlayerSummary::side)
                .findFirst()
                .orElse(PlayerSide.HOME);
    }

    private PlayerTier tierOf(MatchPlayerSummary player) {
        if (player == null || player.seedNumber() == null) {
            return PlayerTier.OTHER;
        }
        int seed = player.seedNumber();
        if (seed <= 4) {
            return PlayerTier.TOP_10;
        }
        if (seed <= 16) {
            return PlayerTier.TOP_50;
        }
        if (seed <= 32) {
            return PlayerTier.TOP_100;
        }
        return PlayerTier.OTHER;
    }

    private long pointSeed(UUID matchId, int sequenceNumber) {
        long base = matchId.getMostSignificantBits() ^ matchId.getLeastSignificantBits();
        return base ^ ((long) sequenceNumber * POINT_SEED_PRIME);
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
