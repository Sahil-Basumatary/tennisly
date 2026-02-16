package dev.sahilbasumatary.authservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.authservice.exception.WebhookVerificationException;
import dev.sahilbasumatary.authservice.service.WebhookService;
import dev.sahilbasumatary.authservice.service.WebhookVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private final WebhookVerificationService verificationService;
    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    public WebhookController(WebhookVerificationService verificationService,
            WebhookService webhookService,
            ObjectMapper objectMapper) {
        this.verificationService = verificationService;
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/clerk")
    public ResponseEntity<Void> handleClerkWebhook(
            @RequestBody String payload,
            @RequestHeader("svix-id") String svixId,
            @RequestHeader("svix-timestamp") String svixTimestamp,
            @RequestHeader("svix-signature") String svixSignature) {
        try {
            verificationService.verify(payload, svixId, svixTimestamp,
                    svixSignature);
        } catch (WebhookVerificationException e) {
            log.warn("Webhook verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.path("type").asText();
            JsonNode data = root.path("data");
            log.info("Processing webhook event: {}", eventType);
            webhookService.processEvent(eventType, data);
            return ResponseEntity.ok().build();
        } catch (JsonProcessingException e) {
            log.error("Malformed webhook payload", e);
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Failed to process webhook event", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
