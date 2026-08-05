package dev.sahilbasumatary.tennisdataservice.service;

import dev.sahilbasumatary.tennisdataservice.dto.UpstreamMatchData;
import dev.sahilbasumatary.tennisdataservice.dto.UpstreamPointData;
import dev.sahilbasumatary.tennisdataservice.dto.response.UpstreamMatchResponse;
import dev.sahilbasumatary.tennisdataservice.dto.response.UpstreamPointResponse;
import dev.sahilbasumatary.tennisdataservice.livetennis.LiveTennisClient;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LiveMatchQueryService {

    private final LiveTennisClient liveTennisClient;

    public LiveMatchQueryService(LiveTennisClient liveTennisClient) {
        this.liveTennisClient = liveTennisClient;
    }

    public List<UpstreamMatchResponse> listMatches(String status, String tour, int limit, int offset) {
        return liveTennisClient.listMatches(status, tour, limit, offset).stream()
                .map(UpstreamMatchResponse::from)
                .toList();
    }

    public List<UpstreamMatchResponse> listHistoryMatches(int limit, int offset) {
        return liveTennisClient.listHistoryMatches(limit, offset).stream()
                .map(UpstreamMatchResponse::from)
                .toList();
    }

    public UpstreamMatchResponse getMatch(long matchId) {
        UpstreamMatchData match = liveTennisClient.getMatch(matchId);
        return UpstreamMatchResponse.from(match);
    }

    public List<UpstreamPointResponse> getPoints(long matchId) {
        List<UpstreamPointData> points = liveTennisClient.getPointTape(matchId);
        return points.stream().map(UpstreamPointResponse::from).toList();
    }
}
