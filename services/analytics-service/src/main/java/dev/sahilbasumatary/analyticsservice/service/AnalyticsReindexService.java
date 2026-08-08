package dev.sahilbasumatary.analyticsservice.service;

import dev.sahilbasumatary.analyticsservice.client.MatchDataClient;
import dev.sahilbasumatary.analyticsservice.client.dto.CompletedMatchFeedResponse;
import dev.sahilbasumatary.analyticsservice.config.AnalyticsReindexProperties;
import dev.sahilbasumatary.analyticsservice.domain.ReindexJobStatus;
import dev.sahilbasumatary.analyticsservice.entity.AnalyticsReindexJob;
import dev.sahilbasumatary.analyticsservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.analyticsservice.repository.AnalyticsReindexJobRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsReindexService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsReindexService.class);

    private final AnalyticsReindexJobRepository jobRepository;
    private final MatchDataClient matchDataClient;
    private final MatchAnalyticsIngestionService ingestionService;
    private final AnalyticsReindexProperties reindexProperties;
    private final ApplicationEventPublisher eventPublisher;

    public AnalyticsReindexService(
            AnalyticsReindexJobRepository jobRepository,
            MatchDataClient matchDataClient,
            MatchAnalyticsIngestionService ingestionService,
            AnalyticsReindexProperties reindexProperties,
            ApplicationEventPublisher eventPublisher) {
        this.jobRepository = jobRepository;
        this.matchDataClient = matchDataClient;
        this.ingestionService = ingestionService;
        this.reindexProperties = reindexProperties;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AnalyticsReindexJob startJob() {
        AnalyticsReindexJob job = new AnalyticsReindexJob();
        job.setStatus(ReindexJobStatus.RUNNING);
        job.setProcessedCount(0);
        job.setStartedAt(Instant.now());
        AnalyticsReindexJob saved = jobRepository.save(job);
        eventPublisher.publishEvent(new ReindexJobStartedEvent(saved.getId()));
        return saved;
    }

    @EventListener
    @Async("analyticsTaskExecutor")
    public void onReindexJobStarted(ReindexJobStartedEvent event) {
        runJob(event.jobId());
    }

    public void runJob(UUID jobId) {
        AnalyticsReindexJob job =
                jobRepository
                        .findById(jobId)
                        .orElseThrow(() -> new ResourceNotFoundException("Reindex job", jobId));
        UUID cursor = job.getCursorMatchId();
        try {
            while (true) {
                CompletedMatchFeedResponse page =
                        matchDataClient.fetchCompletedMatchIds(cursor, reindexProperties.pageSize());
                for (UUID matchId : page.matchIds()) {
                    ingestionService.reconcile(matchId);
                    job.setProcessedCount(job.getProcessedCount() + 1);
                    job.setCursorMatchId(matchId);
                    jobRepository.save(job);
                }
                if (!page.hasMore()) {
                    break;
                }
                cursor = page.nextCursor();
            }
            job.setStatus(ReindexJobStatus.COMPLETED);
            job.setFinishedAt(Instant.now());
            jobRepository.save(job);
            log.info("Completed analytics reindex jobId={} processed={}", jobId, job.getProcessedCount());
        } catch (RuntimeException ex) {
            job.setStatus(ReindexJobStatus.FAILED);
            job.setErrorMessage(ex.getMessage());
            job.setFinishedAt(Instant.now());
            jobRepository.save(job);
            log.error("Analytics reindex job failed jobId={}: {}", jobId, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public AnalyticsReindexJob getJob(UUID jobId) {
        return jobRepository
                .findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Reindex job", jobId));
    }
}
