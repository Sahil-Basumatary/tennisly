package dev.sahilbasumatary.matchservice.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LiveWebSocketPropertiesTest {

    @Test
    void clampsSendLimitsSoSlowClientsCannotHoldUnboundedBuffers() {
        LiveWebSocketProperties properties = new LiveWebSocketProperties();
        properties.setSendTimeLimitMs(1);
        properties.setSendBufferLimitBytes(16);
        properties.setOutboundPoolSize(1);
        properties.setOutboundQueueCapacity(1);

        assertEquals(100, properties.getSendTimeLimitMs());
        assertEquals(4_096, properties.getSendBufferLimitBytes());
        assertEquals(2, properties.getOutboundPoolSize());
        assertEquals(64, properties.getOutboundQueueCapacity());
        assertEquals(2, properties.getHeartbeatMs().length);
        assertTrue(properties.getSendBufferLimitBytes() >= 4_096);
    }
}
