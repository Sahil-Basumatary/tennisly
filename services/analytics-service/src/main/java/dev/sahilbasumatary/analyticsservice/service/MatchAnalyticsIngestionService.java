package dev.sahilbasumatary.analyticsservice.service;

import dev.sahilbasumatary.analyticsservice.client.MatchDataClient;
import dev.sahilbasumatary.analyticsservice.client.dto.MatchPointSummary;
import dev.sahilbasumatary.analyticsservice.client.dto.MatchSummary;
import dev.sahilbasumatary.analyticsservice.domain.TapeMatchMetrics;
import dev.sahilbasumatary.common.event.MatchEvent;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MatchAnalyticsIngestionService {

    private static final Logger log = LoggerFactory.getLogger(MatchAnalyticsIngestionService.class);

    private final IngestReceiptService receiptService;
    private final MatchDataClient matchDataClient;
    private final TapeMetricAggregator tapeMetricAggregator;
    private final AnalyticsProjectionIndexer projectionIndexer;

    public MatchAnalyticsIngestionService(
            IngestReceiptService receiptService,
            MatchDataClient matchDataClient,
            TapeMetricAggregator tapeMetricAggregator,
            AnalyticsProjectionIndexer projectionIndexer) {
        this.receiptService = receiptService;
        this.matchDataClient = matchDataClient;
        this.tapeMetricAggregator = tapeMetricAggregator;
        this.projectionIndexer = projectionIndexer;
    }

    public boolean processEvent(MatchEvent event) {
        if (receiptService.alreadyProcessed(event.getEventId())) {
            log.debug("Skipping duplicate analytics event eventId={}", event.getEventId());
            return false;
        }
        reconcile(event.getMatchId());
        receiptService.recordIfAbsent(event);
        return true;
    }

    public void reconcile(UUID matchId) {
        MatchSummary match = matchDataClient.fetchMatch(matchId);
        List<MatchPointSummary> points = matchDataClient.fetchPoints(matchId);
        TapeMatchMetrics metrics = tapeMetricAggregator.aggregate(match, points);
        projectionIndexer.index(match, metrics);
        log.info(
                "Indexed analytics projection matchId={} status={} points={}",
                matchId,
                match.status(),
                metrics.pointsPlayed());
    }
}
