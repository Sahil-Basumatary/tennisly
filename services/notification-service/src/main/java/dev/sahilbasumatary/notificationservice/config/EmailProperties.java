package dev.sahilbasumatary.notificationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.email")
public record EmailProperties(
        boolean enabled, String provider, String from, String resendApiKey) {}
