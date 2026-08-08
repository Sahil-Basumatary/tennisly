package dev.sahilbasumatary.userservice.service;

import dev.sahilbasumatary.common.event.WebhookDomainEvent;
import dev.sahilbasumatary.common.event.WebhookEventTypes;
import dev.sahilbasumatary.common.kafka.EventPublisher;
import dev.sahilbasumatary.common.kafka.TopicNames;
import dev.sahilbasumatary.userservice.context.RequestContext;
import dev.sahilbasumatary.userservice.dto.request.CreateWebhookEndpointRequest;
import dev.sahilbasumatary.userservice.dto.response.CreateWebhookEndpointResponse;
import dev.sahilbasumatary.userservice.dto.response.WebhookEndpointResponse;
import dev.sahilbasumatary.userservice.dto.response.WebhookSubscriptionResponse;
import dev.sahilbasumatary.userservice.entity.Organization;
import dev.sahilbasumatary.userservice.entity.OrganizationWebhookEndpoint;
import dev.sahilbasumatary.userservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.userservice.repository.OrganizationRepository;
import dev.sahilbasumatary.userservice.repository.OrganizationWebhookEndpointRepository;
import dev.sahilbasumatary.userservice.security.WebhookSecretCipher;
import dev.sahilbasumatary.userservice.security.WebhookSecretGenerator;
import dev.sahilbasumatary.userservice.security.WebhookUrlValidator;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookEndpointService {

    private static final Logger log = LoggerFactory.getLogger(WebhookEndpointService.class);

    private final OrganizationWebhookEndpointRepository webhookRepository;
    private final OrganizationRepository organizationRepository;
    private final WebhookSecretCipher secretCipher;
    private final WebhookUrlValidator urlValidator;
    private final AuditLogService auditLogService;
    private final UsageMeter usageMeter;
    private final EventPublisher eventPublisher;

    public WebhookEndpointService(
            OrganizationWebhookEndpointRepository webhookRepository,
            OrganizationRepository organizationRepository,
            WebhookSecretCipher secretCipher,
            WebhookUrlValidator urlValidator,
            AuditLogService auditLogService,
            UsageMeter usageMeter,
            EventPublisher eventPublisher) {
        this.webhookRepository = webhookRepository;
        this.organizationRepository = organizationRepository;
        this.secretCipher = secretCipher;
        this.urlValidator = urlValidator;
        this.auditLogService = auditLogService;
        this.usageMeter = usageMeter;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<WebhookEndpointResponse> list(UUID organizationId) {
        return webhookRepository.findByOrganizationId(organizationId).stream()
                .map(WebhookEndpointResponse::from)
                .toList();
    }

    @Transactional
    public CreateWebhookEndpointResponse create(CreateWebhookEndpointRequest request, UUID orgId) {
        validateEventTypes(request.eventTypes());
        urlValidator.validate(request.targetUrl());
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", orgId));
        WebhookSecretGenerator.GeneratedWebhookSecret generated = WebhookSecretGenerator.generate();
        OrganizationWebhookEndpoint endpoint = new OrganizationWebhookEndpoint();
        endpoint.setOrganization(org);
        endpoint.setName(request.name().trim());
        endpoint.setTargetUrl(request.targetUrl().trim());
        endpoint.setSecretPrefix(generated.prefix());
        endpoint.setSecretHash(generated.hash());
        endpoint.setSecretCiphertext(secretCipher.encrypt(generated.plaintext()));
        endpoint.setEventTypes(List.copyOf(request.eventTypes()));
        endpoint.setDescription(request.description());
        endpoint.setCreatedByClerkId(
                RequestContext.getUserId() == null || RequestContext.getUserId().isBlank()
                        ? "system"
                        : RequestContext.getUserId());
        OrganizationWebhookEndpoint saved = webhookRepository.save(endpoint);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", saved.getName());
        metadata.put("targetUrl", saved.getTargetUrl());
        metadata.put("eventTypes", saved.getEventTypes());
        auditLogService.record(
                "WEBHOOK_CREATE", "WEBHOOK_ENDPOINT", saved.getId().toString(), org.getId(), metadata);
        usageMeter.increment(org.getId(), "admin_actions", 1);
        log.info("Created webhook endpoint endpointId={} orgId={}", saved.getId(), org.getId());
        return CreateWebhookEndpointResponse.from(saved, generated.plaintext());
    }

    @Transactional
    public WebhookEndpointResponse revoke(UUID id, UUID orgId) {
        OrganizationWebhookEndpoint endpoint = findByIdAndOrg(id, orgId);
        if (endpoint.isActive()) {
            endpoint.setActive(false);
            endpoint.setRevokedAt(Instant.now());
            webhookRepository.save(endpoint);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("secretPrefix", endpoint.getSecretPrefix());
            auditLogService.record(
                    "WEBHOOK_REVOKE", "WEBHOOK_ENDPOINT", endpoint.getId().toString(),
                    endpoint.getOrganization().getId(), metadata);
            usageMeter.increment(endpoint.getOrganization().getId(), "admin_actions", 1);
            log.info("Revoked webhook endpoint endpointId={}", id);
        }
        return WebhookEndpointResponse.from(endpoint);
    }

    @Transactional
    public CreateWebhookEndpointResponse rotateSecret(UUID id, UUID orgId) {
        OrganizationWebhookEndpoint endpoint = findByIdAndOrg(id, orgId);
        if (!endpoint.isActive()) {
            throw new IllegalStateException("Cannot rotate secret for revoked webhook endpoint");
        }
        WebhookSecretGenerator.GeneratedWebhookSecret generated = WebhookSecretGenerator.generate();
        endpoint.setSecretPrefix(generated.prefix());
        endpoint.setSecretHash(generated.hash());
        endpoint.setSecretCiphertext(secretCipher.encrypt(generated.plaintext()));
        webhookRepository.save(endpoint);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("newSecretPrefix", generated.prefix());
        auditLogService.record(
                "WEBHOOK_ROTATE_SECRET", "WEBHOOK_ENDPOINT", endpoint.getId().toString(),
                endpoint.getOrganization().getId(), metadata);
        usageMeter.increment(endpoint.getOrganization().getId(), "admin_actions", 1);
        log.info("Rotated secret for webhook endpoint endpointId={}", id);
        return CreateWebhookEndpointResponse.from(endpoint, generated.plaintext());
    }

    @Transactional
    public void test(UUID id, UUID orgId) {
        OrganizationWebhookEndpoint endpoint = findByIdAndOrg(id, orgId);
        if (!endpoint.isActive()) {
            throw new IllegalStateException("Cannot test revoked webhook endpoint");
        }
        WebhookDomainEvent event = WebhookDomainEvent.webhookTest(orgId, id);
        eventPublisher.publish(TopicNames.WEBHOOK_EVENTS, orgId.toString(), event);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("endpointId", id.toString());
        auditLogService.record(
                "WEBHOOK_TEST", "WEBHOOK_ENDPOINT", id.toString(), orgId, metadata);
        log.info("Sent test webhook event endpointId={} orgId={}", id, orgId);
    }

    @Transactional(readOnly = true)
    public List<WebhookSubscriptionResponse> findSubscriptions(String eventType, UUID orgIdFilter) {
        List<OrganizationWebhookEndpoint> active = (orgIdFilter != null)
                ? webhookRepository.findByOrganizationIdAndActiveTrue(orgIdFilter)
                : webhookRepository.findByActiveTrue();
        return active.stream()
                .filter(ep -> ep.getEventTypes().contains(eventType))
                .map(ep -> new WebhookSubscriptionResponse(
                        ep.getId(),
                        ep.getOrganization().getId(),
                        ep.getTargetUrl(),
                        secretCipher.decrypt(ep.getSecretCiphertext()),
                        ep.getEventTypes()))
                .toList();
    }

    @Transactional(readOnly = true)
    public WebhookSubscriptionResponse getSubscription(UUID id) {
        OrganizationWebhookEndpoint ep = webhookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookEndpoint", id));
        return new WebhookSubscriptionResponse(
                ep.getId(),
                ep.getOrganization().getId(),
                ep.getTargetUrl(),
                secretCipher.decrypt(ep.getSecretCiphertext()),
                ep.getEventTypes());
    }

    @Transactional
    public void markDelivered(UUID id, Instant deliveredAt) {
        webhookRepository.findById(id).ifPresent(ep -> {
            ep.setLastDeliveryAt(deliveredAt);
            webhookRepository.save(ep);
        });
    }

    private OrganizationWebhookEndpoint findByIdAndOrg(UUID id, UUID orgId) {
        OrganizationWebhookEndpoint endpoint = webhookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookEndpoint", id));
        if (!endpoint.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("WebhookEndpoint", id);
        }
        return endpoint;
    }

    private void validateEventTypes(List<String> eventTypes) {
        for (String type : eventTypes) {
            if (!WebhookEventTypes.isValid(type)) {
                throw new IllegalArgumentException(
                        "Invalid event type: " + type + ". Valid types: " + WebhookEventTypes.all());
            }
        }
    }
}
