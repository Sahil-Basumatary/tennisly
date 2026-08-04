package dev.sahilbasumatary.matchservice.seed;

import static dev.sahilbasumatary.matchservice.seed.BroadcastCatalogueIds.*;
import static dev.sahilbasumatary.matchservice.seed.CataloguePlayerIdentity.*;

import dev.sahilbasumatary.matchservice.entity.Match;
import dev.sahilbasumatary.matchservice.entity.MatchPlayer;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import dev.sahilbasumatary.matchservice.entity.PlayerSide;
import dev.sahilbasumatary.matchservice.entity.Surface;
import dev.sahilbasumatary.matchservice.repository.MatchRepository;
import dev.sahilbasumatary.matchservice.seed.PointLedgerBuilder.Stroke;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent broadcast-day catalogue: three tournaments, mixed tours/rounds/courts/statuses, and
 * replay-ready point ledgers. Player IDs resolve from tennis-data by externalId when available.
 */
@Component
@ConditionalOnProperty(
        name = "tennisly.seed.broadcast-catalogue",
        havingValue = "true",
        matchIfMissing = true)
public class BroadcastCatalogueSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BroadcastCatalogueSeeder.class);

    private final MatchRepository matchRepository;
    private final CataloguePlayerResolver playerResolver;
    private final CatalogueIdentityReconciler identityReconciler;

    public BroadcastCatalogueSeeder(
            MatchRepository matchRepository,
            CataloguePlayerResolver playerResolver,
            CatalogueIdentityReconciler identityReconciler) {
        this.matchRepository = matchRepository;
        this.playerResolver = playerResolver;
        this.identityReconciler = identityReconciler;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (matchRepository.findByExternalId("wimbledon-2026-ms-sf-alcaraz-sinner").isPresent()) {
            identityReconciler.reconcile();
            return;
        }
        Instant day = Instant.parse("2026-07-14T12:00:00Z");
        seedMatch(
                MATCH_WIM_SF_ALC_SIN,
                "wimbledon-2026-ms-sf-alcaraz-sinner",
                TOURNAMENT_WIMBLEDON,
                Surface.GRASS,
                MatchStatus.IN_PROGRESS,
                5,
                day.minus(2, ChronoUnit.HOURS),
                day.minus(90, ChronoUnit.MINUTES),
                null,
                meta(
                        "The Championships, Wimbledon",
                        "Wimbledon",
                        "Semi-Final",
                        "SF",
                        "Centre Court",
                        "ATP",
                        "Men's Singles",
                        "London, GBR",
                        2026),
                player(EXT_ALCARAZ, "Carlos Alcaraz", PlayerSide.HOME, 2),
                player(EXT_SINNER, "Jannik Sinner", PlayerSide.AWAY, 1),
                22);
        seedMatch(
                MATCH_WIM_SF_DJO_MED,
                "wimbledon-2026-ms-sf-djokovic-medvedev",
                TOURNAMENT_WIMBLEDON,
                Surface.GRASS,
                MatchStatus.SCHEDULED,
                5,
                day.plus(3, ChronoUnit.HOURS),
                null,
                null,
                meta(
                        "The Championships, Wimbledon",
                        "Wimbledon",
                        "Semi-Final",
                        "SF",
                        "Centre Court",
                        "ATP",
                        "Men's Singles",
                        "London, GBR",
                        2026),
                player(EXT_DJOKOVIC, "Novak Djokovic", PlayerSide.HOME, 3),
                player(EXT_MEDVEDEV, "Daniil Medvedev", PlayerSide.AWAY, 5),
                0);
        seedMatch(
                MATCH_WIM_QF_ZVE_RUU,
                "wimbledon-2026-ms-qf-zverev-ruud",
                TOURNAMENT_WIMBLEDON,
                Surface.GRASS,
                MatchStatus.COMPLETED,
                5,
                day.minus(1, ChronoUnit.DAYS),
                day.minus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS),
                day.minus(20, ChronoUnit.HOURS),
                meta(
                        "The Championships, Wimbledon",
                        "Wimbledon",
                        "Quarter-Final",
                        "QF",
                        "No.1 Court",
                        "ATP",
                        "Men's Singles",
                        "London, GBR",
                        2026),
                player(EXT_ZVEREV, "Alexander Zverev", PlayerSide.HOME, 4),
                player(EXT_RUUD, "Casper Ruud", PlayerSide.AWAY, 8),
                18);
        seedMatch(
                MATCH_WIM_QF_SWI_GAU,
                "wimbledon-2026-ws-qf-swiatek-gauff",
                TOURNAMENT_WIMBLEDON,
                Surface.GRASS,
                MatchStatus.IN_PROGRESS,
                3,
                day.minus(75, ChronoUnit.MINUTES),
                day.minus(60, ChronoUnit.MINUTES),
                null,
                meta(
                        "The Championships, Wimbledon",
                        "Wimbledon",
                        "Quarter-Final",
                        "QF",
                        "No.2 Court",
                        "WTA",
                        "Women's Singles",
                        "London, GBR",
                        2026),
                player(EXT_SWIATEK, "Iga Swiatek", PlayerSide.HOME, 1),
                player(EXT_GAUFF, "Coco Gauff", PlayerSide.AWAY, 2),
                16);
        seedMatch(
                MATCH_WIM_QF_SAB_RYB,
                "wimbledon-2026-ws-qf-sabalenka-rybakina",
                TOURNAMENT_WIMBLEDON,
                Surface.GRASS,
                MatchStatus.COMPLETED,
                3,
                day.minus(2, ChronoUnit.DAYS),
                day.minus(2, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS),
                day.minus(40, ChronoUnit.HOURS),
                meta(
                        "The Championships, Wimbledon",
                        "Wimbledon",
                        "Quarter-Final",
                        "QF",
                        "Centre Court",
                        "WTA",
                        "Women's Singles",
                        "London, GBR",
                        2026),
                player(EXT_SABALENKA, "Aryna Sabalenka", PlayerSide.HOME, 3),
                player(EXT_RYBAKINA, "Elena Rybakina", PlayerSide.AWAY, 4),
                14);
        seedMatch(
                MATCH_RG_QF_ALC_ZVE,
                "rg-2026-ms-qf-alcaraz-zverev",
                TOURNAMENT_ROLAND_GARROS,
                Surface.CLAY,
                MatchStatus.IN_PROGRESS,
                5,
                day.minus(30, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS),
                day.minus(30, ChronoUnit.DAYS).plus(5, ChronoUnit.HOURS),
                null,
                meta(
                        "Roland-Garros",
                        "Roland-Garros",
                        "Quarter-Final",
                        "QF",
                        "Court Philippe-Chatrier",
                        "ATP",
                        "Men's Singles",
                        "Paris, FRA",
                        2026),
                player(EXT_ALCARAZ, "Carlos Alcaraz", PlayerSide.HOME, 2),
                player(EXT_ZVEREV, "Alexander Zverev", PlayerSide.AWAY, 4),
                20);
        seedMatch(
                MATCH_RG_SF_SIN_DJO,
                "rg-2026-ms-sf-sinner-djokovic",
                TOURNAMENT_ROLAND_GARROS,
                Surface.CLAY,
                MatchStatus.SCHEDULED,
                5,
                day.minus(29, ChronoUnit.DAYS).plus(14, ChronoUnit.HOURS),
                null,
                null,
                meta(
                        "Roland-Garros",
                        "Roland-Garros",
                        "Semi-Final",
                        "SF",
                        "Court Philippe-Chatrier",
                        "ATP",
                        "Men's Singles",
                        "Paris, FRA",
                        2026),
                player(EXT_SINNER, "Jannik Sinner", PlayerSide.HOME, 1),
                player(EXT_DJOKOVIC, "Novak Djokovic", PlayerSide.AWAY, 3),
                0);
        seedMatch(
                MATCH_RG_R16_GAU_KEY,
                "rg-2026-ws-r16-gauff-keys",
                TOURNAMENT_ROLAND_GARROS,
                Surface.CLAY,
                MatchStatus.COMPLETED,
                3,
                day.minus(32, ChronoUnit.DAYS),
                day.minus(32, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS),
                day.minus(32, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS),
                meta(
                        "Roland-Garros",
                        "Roland-Garros",
                        "Round of 16",
                        "R16",
                        "Court Suzanne-Lenglen",
                        "WTA",
                        "Women's Singles",
                        "Paris, FRA",
                        2026),
                player(EXT_GAUFF, "Coco Gauff", PlayerSide.HOME, 2),
                player(EXT_KEYS, "Madison Keys", PlayerSide.AWAY, 12),
                15);
        seedMatch(
                MATCH_USO_R32_SIN_FON,
                "uso-2026-ms-r32-sinner-fonseca",
                TOURNAMENT_US_OPEN,
                Surface.HARD,
                MatchStatus.IN_PROGRESS,
                5,
                day.plus(45, ChronoUnit.DAYS),
                day.plus(45, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
                null,
                meta(
                        "US Open",
                        "US Open",
                        "Round of 32",
                        "R32",
                        "Arthur Ashe Stadium",
                        "ATP",
                        "Men's Singles",
                        "New York, USA",
                        2026),
                player(EXT_SINNER, "Jannik Sinner", PlayerSide.HOME, 1),
                player(EXT_FONSECA, "Joao Fonseca", PlayerSide.AWAY, null),
                17);
        seedMatch(
                MATCH_USO_R32_MED_RUB,
                "uso-2026-ms-r32-medvedev-rublev",
                TOURNAMENT_US_OPEN,
                Surface.HARD,
                MatchStatus.SCHEDULED,
                5,
                day.plus(45, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS),
                null,
                null,
                meta(
                        "US Open",
                        "US Open",
                        "Round of 32",
                        "R32",
                        "Louis Armstrong Stadium",
                        "ATP",
                        "Men's Singles",
                        "New York, USA",
                        2026),
                player(EXT_MEDVEDEV, "Daniil Medvedev", PlayerSide.HOME, 5),
                player(EXT_RUBLEV, "Andrey Rublev", PlayerSide.AWAY, 7),
                0);
        seedMatch(
                MATCH_USO_R16_SWI_PAO,
                "uso-2026-ws-r16-swiatek-paolini",
                TOURNAMENT_US_OPEN,
                Surface.HARD,
                MatchStatus.COMPLETED,
                3,
                day.plus(47, ChronoUnit.DAYS),
                day.plus(47, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS),
                day.plus(47, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS),
                meta(
                        "US Open",
                        "US Open",
                        "Round of 16",
                        "R16",
                        "Arthur Ashe Stadium",
                        "WTA",
                        "Women's Singles",
                        "New York, USA",
                        2026),
                player(EXT_SWIATEK, "Iga Swiatek", PlayerSide.HOME, 1),
                player(EXT_PAOLINI, "Jasmine Paolini", PlayerSide.AWAY, 6),
                13);
        seedMatch(
                MATCH_USO_R64_RUU_TSI,
                "uso-2026-ms-r64-ruud-tsitsipas",
                TOURNAMENT_US_OPEN,
                Surface.HARD,
                MatchStatus.COMPLETED,
                5,
                day.plus(42, ChronoUnit.DAYS),
                day.plus(42, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS),
                day.plus(42, ChronoUnit.DAYS).plus(5, ChronoUnit.HOURS),
                meta(
                        "US Open",
                        "US Open",
                        "Round of 64",
                        "R64",
                        "Court 17",
                        "ATP",
                        "Men's Singles",
                        "New York, USA",
                        2026),
                player(EXT_RUUD, "Casper Ruud", PlayerSide.HOME, 8),
                player(EXT_TSITSIPAS, "Stefanos Tsitsipas", PlayerSide.AWAY, 11),
                12);
        log.info(
                "Seeded broadcast catalogue: 12 matches across Wimbledon, Roland-Garros, US Open");
    }

    private void seedMatch(
            UUID matchId,
            String externalId,
            UUID tournamentId,
            Surface surface,
            MatchStatus status,
            int bestOfSets,
            Instant scheduledAt,
            Instant startedAt,
            Instant endedAt,
            Map<String, Object> metadata,
            MatchPlayer home,
            MatchPlayer away,
            int pointCount) {
        Match match = new Match();
        match.setId(matchId);
        match.setExternalId(externalId);
        match.setTournamentId(tournamentId);
        match.setSurface(surface);
        match.setStatus(status);
        match.setBestOfSets(bestOfSets);
        match.setScheduledAt(scheduledAt);
        match.setStartedAt(startedAt);
        match.setEndedAt(endedAt);
        CataloguePlayerIdentity.CATALOGUE_MATCHES.stream()
                .filter(identity -> identity.matchExternalId().equals(externalId))
                .findFirst()
                .ifPresent(
                        identity -> {
                            metadata.put("homeExternalId", identity.homeExternalId());
                            metadata.put("awayExternalId", identity.awayExternalId());
                        });
        match.setMetadata(metadata);
        match.addPlayer(home);
        match.addPlayer(away);
        if (pointCount > 0) {
            List<Stroke> strokes =
                    PointLedgerBuilder.competitiveRally(home.getPlayerId(), away.getPlayerId(), pointCount);
            PointLedgerBuilder.build(match, home.getPlayerId(), away.getPlayerId(), strokes);
        } else {
            match.setCurrentScore(emptyScore(home.getPlayerId(), away.getPlayerId()));
        }
        matchRepository.save(match);
    }

    private MatchPlayer player(
            String externalId, String displayName, PlayerSide side, Integer seedNumber) {
        MatchPlayer player = new MatchPlayer();
        player.setPlayerId(playerResolver.resolve(externalId));
        player.setDisplayName(displayName);
        player.setSide(side);
        player.setSeedNumber(seedNumber);
        return player;
    }

    private static Map<String, Object> meta(
            String tournamentName,
            String tournamentShortName,
            String round,
            String roundCode,
            String court,
            String tour,
            String draw,
            String location,
            int year) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tournamentName", tournamentName);
        metadata.put("tournamentShortName", tournamentShortName);
        metadata.put("round", round);
        metadata.put("roundCode", roundCode);
        metadata.put("court", court);
        metadata.put("tour", tour);
        metadata.put("draw", draw);
        metadata.put("location", location);
        metadata.put("year", year);
        metadata.put("replayReady", true);
        return metadata;
    }

    private static Map<String, Object> emptyScore(UUID homeId, UUID awayId) {
        Map<String, Object> score = new LinkedHashMap<>();
        score.put("sets", List.of());
        score.put("game", Map.of("HOME", "0", "AWAY", "0", "homeGames", 0, "awayGames", 0));
        score.put(
                "players",
                List.of(
                        Map.of("playerId", homeId.toString(), "side", "HOME"),
                        Map.of("playerId", awayId.toString(), "side", "AWAY")));
        return score;
    }
}
