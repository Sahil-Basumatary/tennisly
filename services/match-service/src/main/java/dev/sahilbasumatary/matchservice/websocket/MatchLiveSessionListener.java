package dev.sahilbasumatary.matchservice.websocket;

import dev.sahilbasumatary.matchservice.metrics.MatchTimers;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
public class MatchLiveSessionListener {

    private final MatchTimers matchTimers;

    public MatchLiveSessionListener(MatchTimers matchTimers) {
        this.matchTimers = matchTimers;
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        matchTimers.subscriptionOpened(sessionId(event));
    }

    @EventListener
    public void onUnsubscribe(SessionUnsubscribeEvent event) {
        matchTimers.subscriptionClosed(sessionId(event));
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        matchTimers.clearSessionSubscriptions(sessionId(event));
        matchTimers.liveSessionDisconnect().increment();
        if (isBackpressureClose(event.getCloseStatus())) {
            matchTimers.liveBackpressureDisconnect().increment();
        }
    }

    static String sessionId(AbstractSubProtocolEvent event) {
        if (event == null || event.getMessage() == null) {
            return null;
        }
        return SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
    }

    static boolean isBackpressureClose(CloseStatus status) {
        return status != null
                && (status.equalsCode(CloseStatus.SESSION_NOT_RELIABLE)
                        || status.equalsCode(CloseStatus.PROTOCOL_ERROR));
    }
}
