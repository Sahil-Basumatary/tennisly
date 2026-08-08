package dev.sahilbasumatary.analyticsservice.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TournamentKeyTest {

    @Test
    void normalizesNameAndBuildsCompositeKey() {
        assertEquals(
                "atp|australian-open|2026",
                TournamentKey.from("ATP", "  Australian   Open!!!  ", 2026));
    }

    @Test
    void lowercasesProvider() {
        assertEquals("wta|wimbledon|2025", TournamentKey.from("WTA", "Wimbledon", 2025));
    }

    @Test
    void rejectsBlankProvider() {
        assertThrows(IllegalArgumentException.class, () -> TournamentKey.from("  ", "US Open", 2024));
    }

    @Test
    void rejectsNullSeason() {
        assertThrows(IllegalArgumentException.class, () -> TournamentKey.from("atp", "US Open", null));
    }
}
