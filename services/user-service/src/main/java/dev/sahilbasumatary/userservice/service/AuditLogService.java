package dev.sahilbasumatary.userservice.service;

import dev.sahilbasumatary.userservice.context.RequestContext;
import dev.sahilbasumatary.userservice.dto.response.AdminAuditLogResponse;
import dev.sahilbasumatary.userservice.dto.response.AdminPageResponse;
import dev.sahilbasumatary.userservice.entity.AuditLog;
import dev.sahilbasumatary.userservice.repository.AuditLogRepository;
import dev.sahilbasumatary.userservice.repository.UserProfileRepository;
import dev.sahilbasumatary.userservice.security.AdminAccess;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserProfileRepository userProfileRepository;

    public AuditLogService(
            AuditLogRepository auditLogRepository, UserProfileRepository userProfileRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public AuditLog record(
            String action,
            String resourceType,
            String resourceId,
            UUID organizationId,
            Map<String, Object> metadata) {
        String actorClerkId = RequestContext.getUserId();
        String actorEmail =
                userProfileRepository
                        .findByClerkId(actorClerkId)
                        .map(profile -> profile.getEmail())
                        .orElse(null);
        AuditLog log = new AuditLog();
        log.setActorClerkId(actorClerkId);
        log.setActorEmail(actorEmail);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setOrganizationId(organizationId);
        if (metadata != null) {
            log.setMetadata(metadata);
        }
        return auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public AdminPageResponse<AdminAuditLogResponse> list(
            String q,
            String action,
            UUID organizationId,
            java.time.Instant from,
            java.time.Instant to,
            int page,
            int size) {
        AdminAccess.assertPlatformAdmin();
        String query = normalizeQuery(q);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AdminAuditLogResponse> result =
                auditLogRepository
                        .search(query, action, organizationId, from, to, pageable)
                        .map(AdminAuditLogResponse::from);
        return AdminPageResponse.from(result);
    }

    private String normalizeQuery(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return q.trim();
    }
}
