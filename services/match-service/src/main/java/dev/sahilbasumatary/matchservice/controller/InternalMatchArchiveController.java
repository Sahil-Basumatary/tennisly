package dev.sahilbasumatary.matchservice.controller;

import dev.sahilbasumatary.matchservice.dto.request.CreateArchiveJobRequest;
import dev.sahilbasumatary.matchservice.dto.request.RecordPointBatchRequest;
import dev.sahilbasumatary.matchservice.dto.response.ArchiveBatchResponse;
import dev.sahilbasumatary.matchservice.dto.response.ArchiveJobResponse;
import dev.sahilbasumatary.matchservice.service.MatchArchiveIngestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/matches")
public class InternalMatchArchiveController {

    private static final Logger log = LoggerFactory.getLogger(InternalMatchArchiveController.class);
    private final MatchArchiveIngestService archiveIngestService;

    public InternalMatchArchiveController(MatchArchiveIngestService archiveIngestService) {
        this.archiveIngestService = archiveIngestService;
    }

    @PostMapping("/{matchId}/points/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public ArchiveBatchResponse ingestBatch(
            @PathVariable UUID matchId, @Valid @RequestBody RecordPointBatchRequest request) {
        log.debug(
                "POST /internal/matches/{}/points/batch size={}", matchId, request.points().size());
        return archiveIngestService.ingestBatch(matchId, request);
    }

    @PostMapping("/{matchId}/archive/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    public ArchiveJobResponse createJob(
            @PathVariable UUID matchId, @RequestBody(required = false) CreateArchiveJobRequest request) {
        log.debug("POST /internal/matches/{}/archive/jobs", matchId);
        return archiveIngestService.createJob(matchId, request);
    }

    @GetMapping("/archive/jobs/{jobId}")
    public ArchiveJobResponse getJob(@PathVariable UUID jobId) {
        return archiveIngestService.getJob(jobId);
    }

    @PutMapping(
            value = "/archive/jobs/{jobId}/stream",
            consumes = {"text/tab-separated-values", "text/plain", "application/octet-stream"})
    public ArchiveJobResponse stream(@PathVariable UUID jobId, HttpServletRequest request)
            throws IOException {
        log.debug("PUT /internal/matches/archive/jobs/{}/stream", jobId);
        return archiveIngestService.streamStaging(jobId, request.getInputStream());
    }

    @PostMapping("/archive/jobs/{jobId}/promote")
    public ArchiveJobResponse promote(@PathVariable UUID jobId) {
        log.debug("POST /internal/matches/archive/jobs/{}/promote", jobId);
        return archiveIngestService.promote(jobId);
    }
}
