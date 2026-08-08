package dev.sahilbasumatary.analyticsservice.validation;

import dev.sahilbasumatary.analyticsservice.exception.BadRequestException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SavedViewConfigValidator {

    private static final int MAX_TOP_LEVEL_KEYS = 32;
    private static final int MAX_FILTERS_KEYS = 16;
    private static final int MAX_PLAYER_IDS = 10;
    private static final int MAX_PLAYER_ID_LENGTH = 64;
    private static final int MAX_SERIALIZED_LENGTH = 8000;
    private static final Set<String> ALLOWED_KEYS =
            Set.of(
                    "chartType",
                    "filters",
                    "playerIds",
                    "tournamentKey",
                    "surface",
                    "from",
                    "to",
                    "metrics",
                    "comparePlayerIds");
    private static final Set<String> ALLOWED_CHART_TYPES =
            Set.of("trend", "h2h", "surface", "tournament", "overview");

    public void validate(Map<String, Object> config) {
        if (config == null) {
            throw new BadRequestException("config must not be null");
        }
        if (config.size() > MAX_TOP_LEVEL_KEYS) {
            throw new BadRequestException(
                    "config must have at most " + MAX_TOP_LEVEL_KEYS + " top-level keys");
        }
        for (String key : config.keySet()) {
            if (!ALLOWED_KEYS.contains(key)) {
                throw new BadRequestException("Unknown config key: " + key);
            }
        }
        if (config.containsKey("chartType")) {
            validateChartType(config.get("chartType"));
        }
        if (config.containsKey("playerIds")) {
            validatePlayerIdList("playerIds", config.get("playerIds"));
        }
        if (config.containsKey("comparePlayerIds")) {
            validatePlayerIdList("comparePlayerIds", config.get("comparePlayerIds"));
        }
        if (config.containsKey("filters")) {
            validateFilters(config.get("filters"));
        }
        if (config.toString().length() > MAX_SERIALIZED_LENGTH) {
            throw new BadRequestException("config exceeds maximum allowed size");
        }
    }

    private void validateChartType(Object value) {
        if (!(value instanceof String chartType) || chartType.isBlank()) {
            throw new BadRequestException("chartType must be a non-blank string");
        }
        if (!ALLOWED_CHART_TYPES.contains(chartType)) {
            throw new BadRequestException("Invalid chartType: " + chartType);
        }
    }

    private void validatePlayerIdList(String field, Object value) {
        if (!(value instanceof List<?> list)) {
            throw new BadRequestException(field + " must be a list of strings");
        }
        if (list.size() > MAX_PLAYER_IDS) {
            throw new BadRequestException(
                    field + " must have at most " + MAX_PLAYER_IDS + " entries");
        }
        for (Object entry : list) {
            if (!(entry instanceof String id) || id.isBlank()) {
                throw new BadRequestException(field + " entries must be non-blank strings");
            }
            if (id.length() > MAX_PLAYER_ID_LENGTH) {
                throw new BadRequestException(
                        field + " entries must be at most " + MAX_PLAYER_ID_LENGTH + " characters");
            }
        }
    }

    private void validateFilters(Object value) {
        if (!(value instanceof Map<?, ?> filters)) {
            throw new BadRequestException("filters must be an object");
        }
        if (filters.size() > MAX_FILTERS_KEYS) {
            throw new BadRequestException(
                    "filters must have at most " + MAX_FILTERS_KEYS + " keys");
        }
    }
}
