package dev.sahilbasumatary.matchservice.websocket;

import dev.sahilbasumatary.matchservice.dto.response.MatchResponse;
import dev.sahilbasumatary.matchservice.service.MatchService;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class MatchWebSocketController {

    private final MatchService matchService;

    public MatchWebSocketController(MatchService matchService) {
        this.matchService = matchService;
    }

    @MessageMapping("/matches/{matchId}/snapshot")
    @SendToUser("/queue/matches")
    public MatchResponse snapshot(@DestinationVariable UUID matchId) {
        return matchService.getMatch(matchId);
    }
}
