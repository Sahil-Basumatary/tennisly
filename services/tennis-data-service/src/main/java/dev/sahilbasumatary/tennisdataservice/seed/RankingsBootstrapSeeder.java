package dev.sahilbasumatary.tennisdataservice.seed;

import dev.sahilbasumatary.tennisdataservice.repository.RankingRepository;
import dev.sahilbasumatary.tennisdataservice.service.DataSyncService;
import java.util.function.IntSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Cold-start safety net: rankings APIs stay empty until the cron or a manual sync runs, so pull the
 * configured provider once when the table is empty. Each sync is isolated because they fail
 * independently — a provider quirk in one catalogue should not leave the whole board blank.
 */
@Component
@ConditionalOnProperty(
        name = "tennis.data.seed.on-startup",
        havingValue = "true",
        matchIfMissing = true)
public class RankingsBootstrapSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RankingsBootstrapSeeder.class);

    private final RankingRepository rankingRepository;
    private final DataSyncService dataSyncService;

    public RankingsBootstrapSeeder(
            RankingRepository rankingRepository, DataSyncService dataSyncService) {
        this.rankingRepository = rankingRepository;
        this.dataSyncService = dataSyncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (rankingRepository.count() > 0) {
            log.info("Rankings already present — skipping startup sync");
            return;
        }
        // Rankings resolve players by external id, so players must land before rankings run.
        int players = runSync("players", dataSyncService::syncPlayers);
        int rankings = runSync("rankings", dataSyncService::syncRankings);
        int tournaments = runSync("tournaments", dataSyncService::syncTournaments);
        log.info(
                "Bootstrapped tennis-data catalogue players={} tournaments={} rankings={}",
                players,
                tournaments,
                rankings);
    }

    private int runSync(String name, IntSupplier sync) {
        try {
            return sync.getAsInt();
        } catch (Exception ex) {
            log.warn("Startup {} sync failed — that catalogue will stay empty", name, ex);
            return 0;
        }
    }
}
