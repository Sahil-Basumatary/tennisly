package dev.sahilbasumatary.analyticsservice.service;

import dev.sahilbasumatary.analyticsservice.client.dto.MatchPlayerSummary;
import dev.sahilbasumatary.analyticsservice.client.dto.MatchSummary;
import dev.sahilbasumatary.analyticsservice.config.AnalyticsElasticsearchProperties;
import dev.sahilbasumatary.analyticsservice.domain.TapeMatchMetrics;
import dev.sahilbasumatary.analyticsservice.domain.TapeSideMetrics;
import dev.sahilbasumatary.analyticsservice.domain.TournamentKey;
import dev.sahilbasumatary.analyticsservice.index.MatchAnalyticsDocument;
import dev.sahilbasumatary.analyticsservice.index.PlayerMatchDocument;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsProjectionIndexer {

    private final AnalyticsElasticsearchProperties properties;
    private final ElasticsearchOperations elasticsearchOperations;

    public AnalyticsProjectionIndexer(
            AnalyticsElasticsearchProperties properties,
            ElasticsearchOperations elasticsearchOperations) {
        this.properties = properties;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public void index(MatchSummary match, TapeMatchMetrics metrics) {
        Instant indexedAt = Instant.now();
        MatchPlayerSummary home = requireSide(match, "HOME");
        MatchPlayerSummary away = requireSide(match, "AWAY");
        TournamentContext tournament = resolveTournament(match);
        UUID winnerPlayerId = resolveWinnerPlayerId(match.currentScore(), home.playerId(), away.playerId());
        MatchAnalyticsDocument matchDoc = new MatchAnalyticsDocument();
        matchDoc.setId(match.id().toString());
        matchDoc.setMatchId(match.id());
        matchDoc.setExternalId(match.externalId());
        matchDoc.setTournamentId(match.tournamentId());
        matchDoc.setTournamentKey(tournament.key());
        matchDoc.setTournamentName(tournament.name());
        matchDoc.setSeason(tournament.season());
        matchDoc.setSurface(match.surface());
        matchDoc.setStatus(match.status());
        matchDoc.setBestOfSets(match.bestOfSets());
        matchDoc.setScheduledAt(match.scheduledAt());
        matchDoc.setStartedAt(match.startedAt());
        matchDoc.setEndedAt(match.endedAt());
        matchDoc.setHomePlayerId(home.playerId());
        matchDoc.setHomeDisplayName(home.displayName());
        matchDoc.setAwayPlayerId(away.playerId());
        matchDoc.setAwayDisplayName(away.displayName());
        matchDoc.setWinnerPlayerId(winnerPlayerId);
        matchDoc.setHomeMetrics(metrics.home());
        matchDoc.setAwayMetrics(metrics.away());
        matchDoc.setPointsPlayed(metrics.pointsPlayed());
        matchDoc.setScoreSnapshot(match.currentScore());
        matchDoc.setIndexedAt(indexedAt);
        elasticsearchOperations.save(
                matchDoc, IndexCoordinates.of(properties.matchAlias()));
        elasticsearchOperations.save(
                playerDoc(
                        home,
                        away,
                        match,
                        metrics.home(),
                        winnerPlayerId,
                        tournament,
                        indexedAt),
                IndexCoordinates.of(properties.playerMatchAlias()));
        elasticsearchOperations.save(
                playerDoc(
                        away,
                        home,
                        match,
                        metrics.away(),
                        winnerPlayerId,
                        tournament,
                        indexedAt),
                IndexCoordinates.of(properties.playerMatchAlias()));
    }

    private static PlayerMatchDocument playerDoc(
            MatchPlayerSummary player,
            MatchPlayerSummary opponent,
            MatchSummary match,
            TapeSideMetrics metrics,
            UUID winnerPlayerId,
            TournamentContext tournament,
            Instant indexedAt) {
        PlayerMatchDocument doc = new PlayerMatchDocument();
        doc.setId(player.playerId() + "_" + match.id());
        doc.setPlayerId(player.playerId());
        doc.setMatchId(match.id());
        doc.setOpponentId(opponent.playerId());
        doc.setOpponentName(opponent.displayName());
        doc.setSide(player.side());
        doc.setWon(winnerPlayerId == null ? null : winnerPlayerId.equals(player.playerId()));
        doc.setSurface(match.surface());
        doc.setTournamentKey(tournament.key());
        doc.setTournamentName(tournament.name());
        doc.setSeason(tournament.season());
        doc.setStatus(match.status());
        doc.setEndedAt(match.endedAt());
        doc.setScheduledAt(match.scheduledAt());
        doc.setMetrics(metrics);
        doc.setIndexedAt(indexedAt);
        return doc;
    }

    private static MatchPlayerSummary requireSide(MatchSummary match, String side) {
        return match.players().stream()
                .filter(player -> side.equals(player.side()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Match missing " + side + " player"));
    }

    private static UUID resolveWinnerPlayerId(
            Map<String, Object> currentScore, UUID homeId, UUID awayId) {
        if (currentScore == null || currentScore.isEmpty()) {
            return null;
        }
        Object winner = currentScore.get("winner");
        if (winner == null) {
            winner = currentScore.get("winnerId");
        }
        if (winner == null) {
            winner = currentScore.get("winnerPlayerId");
        }
        if (winner == null) {
            return null;
        }
        String raw = String.valueOf(winner);
        if ("HOME".equalsIgnoreCase(raw)) {
            return homeId;
        }
        if ("AWAY".equalsIgnoreCase(raw)) {
            return awayId;
        }
        try {
            UUID parsed = UUID.fromString(raw);
            if (parsed.equals(homeId) || parsed.equals(awayId)) {
                return parsed;
            }
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return null;
    }

    private static TournamentContext resolveTournament(MatchSummary match) {
        Map<String, Object> metadata = match.metadata() == null ? Map.of() : match.metadata();
        String provider = stringMeta(metadata, "provider", "unknown");
        String tournamentName =
                firstNonBlank(
                        stringMeta(metadata, "tournamentName", null),
                        stringMeta(metadata, "tournament", null),
                        match.tournamentId() == null
                                ? "unknown-tournament"
                                : "tournament-" + match.tournamentId());
        int season = resolveSeason(metadata, match);
        return new TournamentContext(
                TournamentKey.from(provider, tournamentName, season), tournamentName, season);
    }

    private static int resolveSeason(Map<String, Object> metadata, MatchSummary match) {
        Integer fromMeta = intMeta(metadata, "season");
        if (fromMeta == null) {
            fromMeta = intMeta(metadata, "year");
        }
        if (fromMeta != null) {
            return fromMeta;
        }
        Instant anchor = match.endedAt() != null ? match.endedAt() : match.scheduledAt();
        if (anchor != null) {
            return anchor.atZone(ZoneOffset.UTC).getYear();
        }
        return Instant.now().atZone(ZoneOffset.UTC).getYear();
    }

    private static String stringMeta(Map<String, Object> metadata, String key, String fallback) {
        Object value = metadata.get(key);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        return fallback;
    }

    private static Integer intMeta(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "unknown-tournament";
    }

    private record TournamentContext(String key, String name, int season) {}
}
