package dev.sahilbasumatary.replayservice.domain;

public enum PointOutcome {
    WINNER,
    FORCED_ERROR,
    UNFORCED_ERROR,
    ACE,
    DOUBLE_FAULT,
    UNKNOWN;

    public static PointOutcome fromExternal(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }
        try {
            return PointOutcome.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
