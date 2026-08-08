package dev.sahilbasumatary.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sahilbasumatary.common.event.BaseEvent;
import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.common.event.WebhookDomainEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WebhookPayloadBuilder {

    private final ObjectMapper objectMapper;

    public WebhookPayloadBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildEnvelope(String webhookEventType, BaseEvent sourceEvent) {
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("id", sourceEvent.getEventId());
        envelope.put("type", webhookEventType);
        envelope.put("createdAt", sourceEvent.getTimestamp().toString());
        envelope.set("data", objectMapper.valueToTree(buildData(sourceEvent)));
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize webhook envelope", ex);
        }
    }

    private Map<String, Object> buildData(BaseEvent sourceEvent) {
        if (sourceEvent instanceof WebhookDomainEvent webhookEvent) {
            Map<String, Object> data = new LinkedHashMap<>();
            if (webhookEvent.getData() != null) {
                data.putAll(webhookEvent.getData());
            }
            if (webhookEvent.getOrganizationId() != null) {
                data.putIfAbsent("organizationId", webhookEvent.getOrganizationId().toString());
            }
            if (webhookEvent.getResourceType() != null) {
                data.putIfAbsent("resourceType", webhookEvent.getResourceType());
            }
            if (webhookEvent.getResourceId() != null) {
                data.putIfAbsent("resourceId", webhookEvent.getResourceId());
            }
            return data;
        }
        if (sourceEvent instanceof MatchEvent matchEvent) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put(
                    "matchId",
                    matchEvent.getMatchId() == null ? null : matchEvent.getMatchId().toString());
            data.put("status", matchEvent.getStatus());
            if (matchEvent.getPointSequence() != null) {
                data.put("pointSequence", matchEvent.getPointSequence());
            }
            if (matchEvent.getWinnerId() != null) {
                data.put("winnerId", matchEvent.getWinnerId().toString());
            }
            if (matchEvent.getSummary() != null) {
                data.put("summary", matchEvent.getSummary());
            }
            return data;
        }
        return Map.of("source", sourceEvent.getSource() == null ? "" : sourceEvent.getSource());
    }
}
