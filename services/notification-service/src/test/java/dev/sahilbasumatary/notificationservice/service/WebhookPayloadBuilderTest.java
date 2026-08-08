package dev.sahilbasumatary.notificationservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.sahilbasumatary.common.event.MatchEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WebhookPayloadBuilderTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());
    private final WebhookPayloadBuilder builder = new WebhookPayloadBuilder(objectMapper);

    @Test
    void envelopeContainsIdTypeCreatedAtAndData() throws Exception {
        UUID matchId = UUID.randomUUID();
        MatchEvent event = MatchEvent.statusChanged(matchId, "COMPLETED");

        String json = builder.buildEnvelope("match.completed", event);
        JsonNode node = objectMapper.readTree(json);

        assertEquals(event.getEventId(), node.get("id").asText());
        assertEquals("match.completed", node.get("type").asText());
        assertNotNull(node.get("createdAt").asText());
        assertNotNull(node.get("data"));
        assertEquals(matchId.toString(), node.get("data").get("matchId").asText());
    }

    @Test
    void envelopeDataContainsMatchEventFields() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID winnerId = UUID.randomUUID();
        MatchEvent event = MatchEvent.pointRecorded(
                matchId, "IN_PROGRESS", 42, winnerId, "Ace");

        String json = builder.buildEnvelope("match.point_recorded", event);
        JsonNode data = objectMapper.readTree(json).get("data");

        assertEquals(42, data.get("pointSequence").asInt());
        assertEquals(winnerId.toString(), data.get("winnerId").asText());
        assertEquals("Ace", data.get("summary").asText());
    }
}
