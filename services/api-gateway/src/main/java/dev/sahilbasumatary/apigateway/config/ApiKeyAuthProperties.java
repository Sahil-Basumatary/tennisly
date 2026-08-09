package dev.sahilbasumatary.apigateway.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tennisly.api-key-auth")
public class ApiKeyAuthProperties {

    private String userServiceUri = "http://user-service";
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(5);
    private boolean rateLimitFailOpen = true;

    public String getUserServiceUri() {
        return userServiceUri;
    }

    public void setUserServiceUri(String userServiceUri) {
        this.userServiceUri = userServiceUri;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public boolean isRateLimitFailOpen() {
        return rateLimitFailOpen;
    }

    public void setRateLimitFailOpen(boolean rateLimitFailOpen) {
        this.rateLimitFailOpen = rateLimitFailOpen;
    }
}
