package dev.sahilbasumatary.matchservice.websocket;

import dev.sahilbasumatary.matchservice.metrics.MatchTimers;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.SessionLimitExceededException;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

public class MatchLiveSessionDecoratorFactory implements WebSocketHandlerDecoratorFactory {

    private final MatchTimers matchTimers;

    public MatchLiveSessionDecoratorFactory(MatchTimers matchTimers) {
        this.matchTimers = matchTimers;
    }

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                matchTimers.sessionOpened();
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus)
                    throws Exception {
                matchTimers.sessionClosed();
                super.afterConnectionClosed(session, closeStatus);
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception)
                    throws Exception {
                if (exception instanceof SessionLimitExceededException) {
                    matchTimers.liveBackpressureDisconnect().increment();
                }
                super.handleTransportError(session, exception);
            }
        };
    }
}
