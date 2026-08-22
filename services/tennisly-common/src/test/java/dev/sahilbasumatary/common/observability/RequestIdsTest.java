package dev.sahilbasumatary.common.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class RequestIdsTest {

    @Test
    void prefersIncomingRequestId() {
        assertEquals("abc", RequestIds.resolve("abc", "00-deadbeef-span-01"));
    }

    @Test
    void extractsW3cTraceId() {
        assertEquals(
                "4bf92f3577b34da6a3ce929d0e0e4736",
                RequestIds.resolve(
                        null, "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"));
    }

    @Test
    void generatesWhenMissing() {
        assertNotNull(RequestIds.resolve(null, null));
    }
}
