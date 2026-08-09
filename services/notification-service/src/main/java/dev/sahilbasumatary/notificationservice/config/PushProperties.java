package dev.sahilbasumatary.notificationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.push")
public record PushProperties(
        boolean enabled,
        String provider,
        String fcmProjectId,
        String fcmAccessToken) {}
