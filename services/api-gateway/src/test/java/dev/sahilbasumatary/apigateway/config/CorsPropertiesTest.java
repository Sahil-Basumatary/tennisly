package dev.sahilbasumatary.apigateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CorsPropertiesTest {

    @Test
    void parsesCommaSeparatedOrigins() {
        CorsProperties props = new CorsProperties();
        props.setAllowedOrigins("https://app.example.com, https://www.example.com ");
        assertEquals(2, props.resolvedOrigins().size());
        assertEquals("https://app.example.com", props.resolvedOrigins().get(0));
        assertEquals("https://www.example.com", props.resolvedOrigins().get(1));
    }

    @Test
    void blankCsvClearsOrigins() {
        CorsProperties props = new CorsProperties();
        props.setAllowedOrigins("   ");
        assertTrue(props.resolvedOrigins().isEmpty());
    }

    @Test
    void defaultIsLocalhost() {
        CorsProperties props = new CorsProperties();
        assertEquals(List.of("http://localhost:3000"), props.resolvedOrigins());
    }
}
