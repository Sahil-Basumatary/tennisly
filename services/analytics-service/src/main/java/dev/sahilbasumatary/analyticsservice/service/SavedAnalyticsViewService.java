package dev.sahilbasumatary.analyticsservice.service;

import dev.sahilbasumatary.analyticsservice.context.RequestContext;
import dev.sahilbasumatary.analyticsservice.dto.request.CreateSavedViewRequest;
import dev.sahilbasumatary.analyticsservice.dto.request.UpdateSavedViewRequest;
import dev.sahilbasumatary.analyticsservice.dto.response.SavedAnalyticsViewResponse;
import dev.sahilbasumatary.analyticsservice.entity.SavedAnalyticsView;
import dev.sahilbasumatary.analyticsservice.exception.BadRequestException;
import dev.sahilbasumatary.analyticsservice.exception.ConflictException;
import dev.sahilbasumatary.analyticsservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.analyticsservice.repository.SavedAnalyticsViewRepository;
import dev.sahilbasumatary.analyticsservice.validation.SavedViewConfigValidator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavedAnalyticsViewService {

    private static final Logger log = LoggerFactory.getLogger(SavedAnalyticsViewService.class);
    private static final int MAX_VIEWS_PER_USER = 50;
    private final SavedAnalyticsViewRepository repository;
    private final SavedViewConfigValidator configValidator;

    public SavedAnalyticsViewService(
            SavedAnalyticsViewRepository repository, SavedViewConfigValidator configValidator) {
        this.repository = repository;
        this.configValidator = configValidator;
    }

    @Transactional(readOnly = true)
    public List<SavedAnalyticsViewResponse> listForCurrentUser() {
        String userId = requireUserId();
        return repository.findByUserIdOrderByFavoriteDescUpdatedAtDesc(userId).stream()
                .map(SavedAnalyticsViewResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SavedAnalyticsViewResponse getForCurrentUser(UUID id) {
        return SavedAnalyticsViewResponse.from(findOwnedView(id));
    }

    @Transactional
    public SavedAnalyticsViewResponse create(CreateSavedViewRequest request) {
        String userId = requireUserId();
        if (repository.countByUserId(userId) >= MAX_VIEWS_PER_USER) {
            throw new BadRequestException(
                    "Maximum of " + MAX_VIEWS_PER_USER + " saved views allowed per user");
        }
        configValidator.validate(request.config());
        SavedAnalyticsView view = new SavedAnalyticsView();
        view.setUserId(userId);
        String orgId = RequestContext.getOrgId();
        if (orgId != null && !orgId.isBlank()) {
            view.setOrganizationId(orgId);
        }
        view.setName(request.name());
        view.setFavorite(request.favorite() != null && request.favorite());
        view.setConfig(new HashMap<>(request.config()));
        SavedAnalyticsView saved = repository.save(view);
        log.info("Created saved analytics view id={} for userId={}", saved.getId(), userId);
        return SavedAnalyticsViewResponse.from(saved);
    }

    @Transactional
    public SavedAnalyticsViewResponse update(UUID id, UpdateSavedViewRequest request) {
        SavedAnalyticsView view = findOwnedView(id);
        configValidator.validate(request.config());
        view.setVersion(request.version());
        view.setName(request.name());
        view.setConfig(new HashMap<>(request.config()));
        if (request.favorite() != null) {
            view.setFavorite(request.favorite());
        }
        try {
            SavedAnalyticsView saved = repository.save(view);
            log.info("Updated saved analytics view id={} for userId={}", id, view.getUserId());
            return SavedAnalyticsViewResponse.from(saved);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConflictException("Saved view was modified by another request; refresh and retry");
        }
    }

    @Transactional
    public void delete(UUID id) {
        SavedAnalyticsView view = findOwnedView(id);
        repository.delete(view);
        log.info("Deleted saved analytics view id={} for userId={}", id, view.getUserId());
    }

    @Transactional
    public SavedAnalyticsViewResponse setFavorite(UUID id, boolean favorite) {
        SavedAnalyticsView view = findOwnedView(id);
        view.setFavorite(favorite);
        SavedAnalyticsView saved = repository.save(view);
        log.info(
                "Set favorite={} on saved analytics view id={} for userId={}",
                favorite,
                id,
                view.getUserId());
        return SavedAnalyticsViewResponse.from(saved);
    }

    private SavedAnalyticsView findOwnedView(UUID id) {
        String userId = requireUserId();
        return repository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SavedAnalyticsView", id));
    }

    private String requireUserId() {
        String userId = RequestContext.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("Authenticated user context is required");
        }
        return userId;
    }
}
