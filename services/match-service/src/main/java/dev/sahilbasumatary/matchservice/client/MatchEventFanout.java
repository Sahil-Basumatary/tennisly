package dev.sahilbasumatary.matchservice.client;

import dev.sahilbasumatary.common.event.MatchEvent;
import org.springframework.stereotype.Component;

@Component
public class MatchEventFanout {

    private final NotificationEventClient notificationEventClient;
    private final AnalyticsEventClient analyticsEventClient;
    private final ReplayEventClient replayEventClient;

    public MatchEventFanout(
            NotificationEventClient notificationEventClient,
            AnalyticsEventClient analyticsEventClient,
            ReplayEventClient replayEventClient) {
        this.notificationEventClient = notificationEventClient;
        this.analyticsEventClient = analyticsEventClient;
        this.replayEventClient = replayEventClient;
    }

    public void relay(MatchEvent event) {
        notificationEventClient.relayMatch(event);
        analyticsEventClient.relayMatch(event);
        replayEventClient.relayMatch(event);
    }
}
