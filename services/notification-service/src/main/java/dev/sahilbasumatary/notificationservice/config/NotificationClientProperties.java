package dev.sahilbasumatary.notificationservice.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.clients")
public record NotificationClientProperties(
        String userServiceUri, Duration connectTimeout, Duration readTimeout) {

    public NotificationClientProperties {
        if (userServiceUri == null || userServiceUri.isBlank()) {
            userServiceUri = "http://user-service";
        }
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(3);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(10);
        }
    }
}
