package dev.sahilbasumatary.matchservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tennisly.websocket")
public class LiveWebSocketProperties {

    private String[] allowedOrigins = new String[] {"http://localhost:3000"};
    private String redisChannel = "match-live-events";
    private int sendTimeLimitMs = 500;
    private int sendBufferLimitBytes = 16_384;
    private int outboundPoolSize = 16;
    private int outboundQueueCapacity = 20_000;
    private long[] heartbeatMs = new long[] {10_000L, 10_000L};

    public String[] getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null ? new String[0] : allowedOrigins;
    }

    public String getRedisChannel() {
        return redisChannel;
    }

    public void setRedisChannel(String redisChannel) {
        this.redisChannel = redisChannel == null || redisChannel.isBlank()
                ? "match-live-events"
                : redisChannel;
    }

    public int getSendTimeLimitMs() {
        return sendTimeLimitMs;
    }

    public void setSendTimeLimitMs(int sendTimeLimitMs) {
        this.sendTimeLimitMs = Math.max(100, sendTimeLimitMs);
    }

    public int getSendBufferLimitBytes() {
        return sendBufferLimitBytes;
    }

    public void setSendBufferLimitBytes(int sendBufferLimitBytes) {
        this.sendBufferLimitBytes = Math.max(4_096, sendBufferLimitBytes);
    }

    public int getOutboundPoolSize() {
        return outboundPoolSize;
    }

    public void setOutboundPoolSize(int outboundPoolSize) {
        this.outboundPoolSize = Math.max(2, outboundPoolSize);
    }

    public int getOutboundQueueCapacity() {
        return outboundQueueCapacity;
    }

    public void setOutboundQueueCapacity(int outboundQueueCapacity) {
        this.outboundQueueCapacity = Math.max(64, outboundQueueCapacity);
    }

    public long[] getHeartbeatMs() {
        return heartbeatMs;
    }

    public void setHeartbeatMs(long[] heartbeatMs) {
        this.heartbeatMs = heartbeatMs == null || heartbeatMs.length != 2
                ? new long[] {10_000L, 10_000L}
                : heartbeatMs;
    }
}
