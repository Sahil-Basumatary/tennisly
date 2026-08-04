package dev.sahilbasumatary.tennisdataservice.seed;

import dev.sahilbasumatary.tennisdataservice.repository.RankingRepository;
import dev.sahilbasumatary.tennisdataservice.service.DataSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Local/dev safety net: rankings APIs stay empty until cron or manual sync. When the table is cold,
 * pull mock (or configured) provider data once so /players has ESPN-depth boards on first boot.
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
        try {
            int players = dataSyncService.syncPlayers();
            int tournaments = dataSyncService.syncTournaments();
            int rankings = dataSyncService.syncRankings();
            log.info(
                    "Bootstrapped tennis-data catalogue players={} tournaments={} rankings={}",
                    players,
                    tournaments,
                    rankings);
        } catch (Exception ex) {
            log.warn("Startup tennis-data sync failed — rankings board may be empty", ex);
        }
    }
}
