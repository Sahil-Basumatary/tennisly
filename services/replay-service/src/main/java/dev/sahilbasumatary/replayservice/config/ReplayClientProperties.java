package dev.sahilbasumatary.replayservice.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Downstream service coordinates. The URIs are logical Eureka service ids by default so requests are
 * client-side load balanced; they can be overridden with absolute URLs for local testing.
 */
@ConfigurationProperties(prefix = "replay.clients")
public record ReplayClientProperties(
        String matchServiceUri,
        String tennisDataServiceUri,
        Duration connectTimeout,
        Duration readTimeout) {

    public ReplayClientProperties {
        if (matchServiceUri == null || matchServiceUri.isBlank()) {
            matchServiceUri = "http://match-service";
        }
        if (tennisDataServiceUri == null || tennisDataServiceUri.isBlank()) {
            tennisDataServiceUri = "http://tennis-data-service";
        }
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(5);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(20);
        }
    }
}
