package dev.sahilbasumatary.analyticsservice.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "analytics.clients")
public record AnalyticsClientProperties(
        String matchServiceUri, Duration connectTimeout, Duration readTimeout) {

    public AnalyticsClientProperties {
        if (matchServiceUri == null || matchServiceUri.isBlank()) {
            matchServiceUri = "http://match-service";
        }
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(5);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(10);
        }
    }
}
