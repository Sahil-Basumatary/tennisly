package dev.sahilbasumatary.analyticsservice.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.data.elasticsearch.core.document.Document;

final class ElasticsearchMappings {

    private ElasticsearchMappings() {}

    static Document matchAnalyticsMapping() {
        Map<String, Object> properties = new HashMap<>();
        keyword(properties, "matchId");
        keyword(properties, "externalId");
        keyword(properties, "tournamentId");
        keyword(properties, "tournamentKey");
        textKeyword(properties, "tournamentName");
        integer(properties, "season");
        keyword(properties, "surface");
        keyword(properties, "status");
        integer(properties, "bestOfSets");
        date(properties, "scheduledAt");
        date(properties, "startedAt");
        date(properties, "endedAt");
        keyword(properties, "homePlayerId");
        textKeyword(properties, "homeDisplayName");
        keyword(properties, "awayPlayerId");
        textKeyword(properties, "awayDisplayName");
        keyword(properties, "winnerPlayerId");
        object(properties, "homeMetrics", sideMetricsProperties());
        object(properties, "awayMetrics", sideMetricsProperties());
        integer(properties, "pointsPlayed");
        object(properties, "scoreSnapshot");
        date(properties, "indexedAt");
        return Document.from(Map.of("properties", properties));
    }

    static Document playerMatchMapping() {
        Map<String, Object> properties = new HashMap<>();
        keyword(properties, "playerId");
        keyword(properties, "matchId");
        keyword(properties, "opponentId");
        textKeyword(properties, "opponentName");
        keyword(properties, "side");
        booleanField(properties, "won");
        keyword(properties, "surface");
        keyword(properties, "tournamentKey");
        textKeyword(properties, "tournamentName");
        integer(properties, "season");
        keyword(properties, "status");
        date(properties, "endedAt");
        date(properties, "scheduledAt");
        object(properties, "metrics", sideMetricsProperties());
        date(properties, "indexedAt");
        return Document.from(Map.of("properties", properties));
    }

    private static Map<String, Object> sideMetricsProperties() {
        Map<String, Object> properties = new HashMap<>();
        integer(properties, "pointsWon");
        integer(properties, "servicePointsWon");
        integer(properties, "breakPointsWon");
        return properties;
    }

    private static void keyword(Map<String, Object> properties, String field) {
        properties.put(field, Map.of("type", "keyword"));
    }

    private static void textKeyword(Map<String, Object> properties, String field) {
        properties.put(field, Map.of("type", "text", "fields", Map.of("keyword", Map.of("type", "keyword"))));
    }

    private static void integer(Map<String, Object> properties, String field) {
        properties.put(field, Map.of("type", "integer"));
    }

    private static void booleanField(Map<String, Object> properties, String field) {
        properties.put(field, Map.of("type", "boolean"));
    }

    private static void date(Map<String, Object> properties, String field) {
        properties.put(field, Map.of("type", "date"));
    }

    private static void object(Map<String, Object> properties, String field) {
        properties.put(field, Map.of("type", "object", "enabled", true));
    }

    private static void object(Map<String, Object> properties, String field, Map<String, Object> nested) {
        properties.put(field, Map.of("type", "object", "properties", nested));
    }
}
