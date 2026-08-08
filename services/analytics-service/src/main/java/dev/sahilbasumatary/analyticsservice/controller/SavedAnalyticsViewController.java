package dev.sahilbasumatary.analyticsservice.controller;

import dev.sahilbasumatary.analyticsservice.dto.request.CreateSavedViewRequest;
import dev.sahilbasumatary.analyticsservice.dto.request.SetFavoriteRequest;
import dev.sahilbasumatary.analyticsservice.dto.request.UpdateSavedViewRequest;
import dev.sahilbasumatary.analyticsservice.dto.response.SavedAnalyticsViewResponse;
import dev.sahilbasumatary.analyticsservice.exception.BadRequestException;
import dev.sahilbasumatary.analyticsservice.service.SavedAnalyticsViewService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/analytics/views")
public class SavedAnalyticsViewController {

    private static final Logger log = LoggerFactory.getLogger(SavedAnalyticsViewController.class);
    private final SavedAnalyticsViewService viewService;

    public SavedAnalyticsViewController(SavedAnalyticsViewService viewService) {
        this.viewService = viewService;
    }

    @GetMapping
    public List<SavedAnalyticsViewResponse> listViews() {
        log.debug("GET /api/analytics/views");
        return viewService.listForCurrentUser();
    }

    @GetMapping("/{id}")
    public SavedAnalyticsViewResponse getView(@PathVariable UUID id) {
        log.debug("GET /api/analytics/views/{}", id);
        return viewService.getForCurrentUser(id);
    }

    @PostMapping
    public ResponseEntity<SavedAnalyticsViewResponse> createView(
            @Valid @RequestBody CreateSavedViewRequest request) {
        log.debug("POST /api/analytics/views");
        SavedAnalyticsViewResponse created = viewService.create(request);
        URI location =
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(created.id())
                        .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public SavedAnalyticsViewResponse updateView(
            @PathVariable UUID id, @Valid @RequestBody UpdateSavedViewRequest request) {
        log.debug("PUT /api/analytics/views/{}", id);
        return viewService.update(id, request);
    }

    @PatchMapping("/{id}/favorite")
    public SavedAnalyticsViewResponse setFavorite(
            @PathVariable UUID id,
            @RequestParam(required = false) Boolean favorite,
            @Valid @RequestBody(required = false) SetFavoriteRequest body) {
        log.debug("PATCH /api/analytics/views/{}/favorite", id);
        Boolean resolved = body != null ? body.favorite() : favorite;
        if (resolved == null) {
            throw new BadRequestException("favorite is required");
        }
        return viewService.setFavorite(id, resolved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteView(@PathVariable UUID id) {
        log.debug("DELETE /api/analytics/views/{}", id);
        viewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
