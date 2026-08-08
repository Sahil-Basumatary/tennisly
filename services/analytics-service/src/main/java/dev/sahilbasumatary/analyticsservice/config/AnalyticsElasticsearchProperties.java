package dev.sahilbasumatary.analyticsservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "analytics.elasticsearch")
public record AnalyticsElasticsearchProperties(
        String matchIndex,
        String playerMatchIndex,
        String matchAlias,
        String playerMatchAlias) {

    public AnalyticsElasticsearchProperties {
        if (matchIndex == null || matchIndex.isBlank()) {
            matchIndex = "tennisly-match-analytics-v1";
        }
        if (playerMatchIndex == null || playerMatchIndex.isBlank()) {
            playerMatchIndex = "tennisly-player-match-v1";
        }
        if (matchAlias == null || matchAlias.isBlank()) {
            matchAlias = "tennisly-match-analytics";
        }
        if (playerMatchAlias == null || playerMatchAlias.isBlank()) {
            playerMatchAlias = "tennisly-player-match";
        }
    }
}
