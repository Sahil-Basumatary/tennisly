package dev.sahilbasumatary.common.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WebhookDomainEvent extends BaseEvent {

    public static final String WEBHOOK_DISPATCH = "WEBHOOK_DISPATCH";

    private UUID organizationId;
    private String publicEventType;
    private String resourceType;
    private String resourceId;
    private Map<String, Object> data;

    public WebhookDomainEvent() {}

    private WebhookDomainEvent(String publicEventType, String source, UUID organizationId) {
        super(WEBHOOK_DISPATCH, source);
        this.organizationId = organizationId;
        this.publicEventType = publicEventType;
        this.data = new HashMap<>();
    }

    public static WebhookDomainEvent apiKeyRevoked(UUID orgId, UUID apiKeyId, String keyPrefix) {
        WebhookDomainEvent event = new WebhookDomainEvent(
                WebhookEventTypes.API_KEY_REVOKED, "user-service", orgId);
        event.resourceType = "api_key";
        event.resourceId = apiKeyId.toString();
        event.data.put("apiKeyId", apiKeyId.toString());
        event.data.put("keyPrefix", keyPrefix);
        event.data.put("organizationId", orgId.toString());
        return event;
    }

    public static WebhookDomainEvent webhookTest(UUID orgId, UUID endpointId) {
        WebhookDomainEvent event = new WebhookDomainEvent(
                WebhookEventTypes.WEBHOOK_TEST, "user-service", orgId);
        event.resourceType = "webhook_endpoint";
        event.resourceId = endpointId.toString();
        event.data.put("endpointId", endpointId.toString());
        event.data.put("organizationId", orgId.toString());
        return event;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public String getPublicEventType() {
        return publicEventType;
    }

    public void setPublicEventType(String publicEventType) {
        this.publicEventType = publicEventType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
