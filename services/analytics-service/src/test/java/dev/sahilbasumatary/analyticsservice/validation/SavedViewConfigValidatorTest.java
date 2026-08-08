package dev.sahilbasumatary.analyticsservice.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.sahilbasumatary.analyticsservice.exception.BadRequestException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SavedViewConfigValidatorTest {

    private SavedViewConfigValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SavedViewConfigValidator();
    }

    @Test
    void acceptsValidConfig() {
        Map<String, Object> config =
                Map.of(
                        "chartType",
                        "trend",
                        "playerIds",
                        List.of("player-1"),
                        "filters",
                        Map.of("surface", "HARD"));
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    void rejectsUnknownKey() {
        Map<String, Object> config = new HashMap<>();
        config.put("chartType", "overview");
        config.put("unknownField", "value");
        BadRequestException ex =
                assertThrows(BadRequestException.class, () -> validator.validate(config));
        assertEquals("Unknown config key: unknownField", ex.getMessage());
    }

    @Test
    void rejectsInvalidChartType() {
        Map<String, Object> config = Map.of("chartType", "pie");
        BadRequestException ex =
                assertThrows(BadRequestException.class, () -> validator.validate(config));
        assertEquals("Invalid chartType: pie", ex.getMessage());
    }

    @Test
    void rejectsTooManyTopLevelKeys() {
        Map<String, Object> config = new HashMap<>();
        for (int i = 0; i < 33; i++) {
            config.put("extraKey" + i, "value");
        }
        BadRequestException ex =
                assertThrows(BadRequestException.class, () -> validator.validate(config));
        assertEquals("config must have at most 32 top-level keys", ex.getMessage());
    }

    @Test
    void rejectsOversizedConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("surface", "x".repeat(8001));
        BadRequestException ex =
                assertThrows(BadRequestException.class, () -> validator.validate(config));
        assertEquals("config exceeds maximum allowed size", ex.getMessage());
    }

    @Test
    void rejectsTooManyPlayerIds() {
        Map<String, Object> config =
                Map.of("playerIds", List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"));
        BadRequestException ex =
                assertThrows(BadRequestException.class, () -> validator.validate(config));
        assertEquals("playerIds must have at most 10 entries", ex.getMessage());
    }
}
