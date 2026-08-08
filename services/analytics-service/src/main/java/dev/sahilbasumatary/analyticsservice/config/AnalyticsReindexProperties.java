package dev.sahilbasumatary.analyticsservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "analytics.reindex")
public record AnalyticsReindexProperties(int pageSize) {

    public AnalyticsReindexProperties {
        if (pageSize <= 0) {
            pageSize = 50;
        }
        pageSize = Math.max(1, Math.min(pageSize, 100));
    }
}
