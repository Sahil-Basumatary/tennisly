package dev.sahilbasumatary.analyticsservice.controller;

import dev.sahilbasumatary.analyticsservice.dto.response.ReindexJobResponse;
import dev.sahilbasumatary.analyticsservice.entity.AnalyticsReindexJob;
import dev.sahilbasumatary.analyticsservice.service.AnalyticsReindexService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/analytics/reindex")
public class InternalReindexController {

    private static final Logger log = LoggerFactory.getLogger(InternalReindexController.class);
    private final AnalyticsReindexService reindexService;

    public InternalReindexController(AnalyticsReindexService reindexService) {
        this.reindexService = reindexService;
    }

    @PostMapping
    public ResponseEntity<ReindexJobResponse> startReindex() {
        log.info("POST /internal/analytics/reindex");
        AnalyticsReindexJob job = reindexService.startJob();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(toResponse(job));
    }

    @GetMapping("/{jobId}")
    public ReindexJobResponse getReindexJob(@PathVariable UUID jobId) {
        log.debug("GET /internal/analytics/reindex/{}", jobId);
        return toResponse(reindexService.getJob(jobId));
    }

    private static ReindexJobResponse toResponse(AnalyticsReindexJob job) {
        return new ReindexJobResponse(
                job.getId(),
                job.getStatus(),
                job.getCursorMatchId(),
                job.getProcessedCount(),
                job.getTotalCount(),
                job.getErrorMessage(),
                job.getStartedAt(),
                job.getFinishedAt());
    }
}
