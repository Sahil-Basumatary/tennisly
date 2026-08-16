package dev.sahilbasumatary.userservice.service;

import dev.sahilbasumatary.common.event.WebhookDomainEvent;
import dev.sahilbasumatary.common.kafka.EventPublisher;
import dev.sahilbasumatary.common.kafka.TopicNames;
import dev.sahilbasumatary.userservice.context.RequestContext;
import dev.sahilbasumatary.userservice.dto.request.AdminCreateApiKeyRequest;
import dev.sahilbasumatary.userservice.dto.response.AdminApiKeyResponse;
import dev.sahilbasumatary.userservice.dto.response.AdminCreateApiKeyResponse;
import dev.sahilbasumatary.userservice.dto.response.AdminPageResponse;
import dev.sahilbasumatary.userservice.entity.Organization;
import dev.sahilbasumatary.userservice.entity.OrganizationApiKey;
import dev.sahilbasumatary.userservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.userservice.repository.OrganizationApiKeyRepository;
import dev.sahilbasumatary.userservice.repository.OrganizationRepository;
import dev.sahilbasumatary.userservice.security.AdminAccess;
import dev.sahilbasumatary.userservice.security.ApiKeyGenerator;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    private final OrganizationApiKeyRepository apiKeyRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditLogService auditLogService;
    private final UsageMeter usageMeter;
    private final EventPublisher eventPublisher;

    public ApiKeyService(
            OrganizationApiKeyRepository apiKeyRepository,
            OrganizationRepository organizationRepository,
            AuditLogService auditLogService,
            UsageMeter usageMeter,
            EventPublisher eventPublisher) {
        this.apiKeyRepository = apiKeyRepository;
        this.organizationRepository = organizationRepository;
        this.auditLogService = auditLogService;
        this.usageMeter = usageMeter;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public AdminPageResponse<AdminApiKeyResponse> list(
            UUID organizationId, Boolean active, int page, int size) {
        AdminAccess.assertPlatformAdmin();
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        // Untyped null UUID binds as bytea on Postgres and 500s the empty filter on /admin/keys.
        Page<OrganizationApiKey> keys =
                organizationId == null && active == null
                        ? apiKeyRepository.findAll(pageable)
                        : apiKeyRepository.search(organizationId, active, pageable);
        return AdminPageResponse.from(keys.map(AdminApiKeyResponse::from));
    }

    @Transactional
    public AdminCreateApiKeyResponse create(AdminCreateApiKeyRequest request) {
        AdminAccess.assertPlatformAdmin();
        Organization org =
                organizationRepository
                        .findById(request.organizationId())
                        .orElseThrow(() -> new ResourceNotFoundException("Organization", request.organizationId()));
        ApiKeyGenerator.GeneratedApiKey generated = ApiKeyGenerator.generate();
        OrganizationApiKey key = new OrganizationApiKey();
        key.setOrganization(org);
        key.setName(request.name().trim());
        key.setKeyPrefix(generated.prefix());
        key.setKeyHash(generated.hash());
        key.setScopes(resolveScopes(request.scopes()));
        key.setExpiresAt(request.expiresAt());
        key.setCreatedByClerkId(RequestContext.getUserId());
        OrganizationApiKey saved = apiKeyRepository.save(key);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", saved.getName());
        metadata.put("keyPrefix", saved.getKeyPrefix());
        metadata.put("scopes", saved.getScopes());
        auditLogService.record(
                "API_KEY_CREATE",
                "API_KEY",
                saved.getId().toString(),
                org.getId(),
                metadata);
        usageMeter.increment(org.getId(), "admin_actions", 1);
        log.info("Admin created API key keyId={} orgId={}", saved.getId(), org.getId());
        return AdminCreateApiKeyResponse.from(saved, generated.plaintext());
    }

    @Transactional
    public AdminApiKeyResponse revoke(UUID id) {
        AdminAccess.assertPlatformAdmin();
        OrganizationApiKey key =
                apiKeyRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("OrganizationApiKey", id));
        if (key.isActive()) {
            key.setActive(false);
            key.setRevokedAt(Instant.now());
            apiKeyRepository.save(key);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("keyPrefix", key.getKeyPrefix());
            auditLogService.record(
                    "API_KEY_REVOKE",
                    "API_KEY",
                    key.getId().toString(),
                    key.getOrganization().getId(),
                    metadata);
            usageMeter.increment(key.getOrganization().getId(), "admin_actions", 1);
            WebhookDomainEvent webhookEvent = WebhookDomainEvent.apiKeyRevoked(
                    key.getOrganization().getId(), key.getId(), key.getKeyPrefix());
            eventPublisher.publish(
                    TopicNames.WEBHOOK_EVENTS, key.getOrganization().getId().toString(), webhookEvent);
            log.info("Admin revoked API key keyId={}", id);
        }
        return AdminApiKeyResponse.from(key);
    }

    private List<String> resolveScopes(List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return List.of("read");
        }
        return List.copyOf(scopes);
    }
}
