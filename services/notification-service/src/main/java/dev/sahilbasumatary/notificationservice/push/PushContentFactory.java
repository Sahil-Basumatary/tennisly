package dev.sahilbasumatary.notificationservice.push;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PushContentFactory {

    public PushContent welcome(String displayName) {
        String name = blank(displayName, "there");
        return new PushContent(
                "Welcome to Tennisly",
                "Hi " + name + " — your account is ready.",
                Map.of("type", "welcome"));
    }

    public PushContent apiKeyRevoked(String keyPrefix) {
        return new PushContent(
                "API key revoked",
                "A key starting with " + blank(keyPrefix, "tly_live_") + " was revoked.",
                Map.of("type", "api_key_revoked"));
    }

    public PushContent webhookFailed(String eventType) {
        return new PushContent(
                "Webhook delivery failed",
                "Retries exhausted for " + blank(eventType, "event") + ".",
                Map.of("type", "webhook_failed", "eventType", blank(eventType, "")));
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
