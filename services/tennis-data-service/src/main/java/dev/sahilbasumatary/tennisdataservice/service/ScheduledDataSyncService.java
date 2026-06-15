package dev.sahilbasumatary.tennisdataservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "tennis.data.sync.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledDataSyncService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledDataSyncService.class);

    private final DataSyncService dataSyncService;

    public ScheduledDataSyncService(DataSyncService dataSyncService) {
        this.dataSyncService = dataSyncService;
    }

    @Scheduled(cron = "${tennis.data.sync.players-cron}")
    public void syncPlayers() {
        log.info("Starting scheduled player sync");
        dataSyncService.syncPlayers();
    }

    @Scheduled(cron = "${tennis.data.sync.tournaments-cron}")
    public void syncTournaments() {
        log.info("Starting scheduled tournament sync");
        dataSyncService.syncTournaments();
    }

    @Scheduled(cron = "${tennis.data.sync.rankings-cron}")
    public void syncRankings() {
        log.info("Starting scheduled ranking sync");
        dataSyncService.syncRankings();
    }
}
