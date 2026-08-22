package dev.sahilbasumatary.matchservice.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import dev.sahilbasumatary.matchservice.metrics.MatchTimers;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.SessionLimitExceededException;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

class MatchLiveSessionListenerTest {

    @Test
    void countsSessionNotReliableAsBackpressureAndClearsSubscriptions() {
        MatchTimers timers = new MatchTimers(new SimpleMeterRegistry());
        MatchLiveSessionListener listener = new MatchLiveSessionListener(timers);
        Message<byte[]> message = sessionMessage("sess-1");

        listener.onSubscribe(new SessionSubscribeEvent(this, message));
        listener.onDisconnect(
                new SessionDisconnectEvent(this, message, "sess-1", CloseStatus.SESSION_NOT_RELIABLE));

        assertEquals(1, timers.liveSubscribeSuccess().count());
        assertEquals(0, timers.activeSubscriptionCount());
        assertEquals(1, timers.liveSessionDisconnect().count());
        assertEquals(1, timers.liveBackpressureDisconnect().count());
        assertTrue(MatchLiveSessionListener.isBackpressureClose(CloseStatus.SESSION_NOT_RELIABLE));
        assertFalse(MatchLiveSessionListener.isBackpressureClose(CloseStatus.NORMAL));
        assertEquals("sess-1", MatchLiveSessionListener.sessionId(new SessionSubscribeEvent(this, message)));
    }

    @Test
    void decoratorCountsOpenSessionsAndSendLimitBreaches() throws Exception {
        MatchTimers timers = new MatchTimers(new SimpleMeterRegistry());
        WebSocketHandler delegate = mock(WebSocketHandler.class);
        WebSocketSession session = mock(WebSocketSession.class);
        WebSocketHandler decorated =
                new MatchLiveSessionDecoratorFactory(timers).decorate(delegate);
        SessionLimitExceededException error =
                new SessionLimitExceededException("buffer", CloseStatus.SESSION_NOT_RELIABLE);

        decorated.afterConnectionEstablished(session);
        decorated.handleTransportError(session, error);
        decorated.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertEquals(1, timers.liveSessionConnect().count());
        assertEquals(0, timers.activeSessionCount());
        assertEquals(1, timers.liveBackpressureDisconnect().count());
        verify(delegate).afterConnectionEstablished(session);
        verify(delegate).handleTransportError(session, error);
        verify(delegate).afterConnectionClosed(session, CloseStatus.NORMAL);
    }

    private static Message<byte[]> sessionMessage(String sessionId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setSessionId(sessionId);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
