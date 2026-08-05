package dev.sahilbasumatary.tennisdataservice.livetennis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

final class LiveTennisModels {

    private LiveTennisModels() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PageResponse(List<MatchPayload> data, Meta meta) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Meta(Integer count, Integer limit, Integer offset) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MatchPayload(
            long id,
            String tournament,
            String surface,
            Boolean indoor,
            String format,
            String round,
            String status,
            @JsonProperty("is_doubles") Boolean doubles,
            @JsonProperty("scheduled_time") String scheduledTime,
            PlayersPayload players,
            ScorePayload score,
            Integer winner) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PlayersPayload(PlayerPayload p1, PlayerPayload p2) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PlayerPayload(Long id, String name, @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ScorePayload(
            List<Integer> sets,
            List<List<Integer>> games,
            List<String> points,
            Integer server,
            @JsonProperty("is_tiebreak") Boolean tiebreak,
            String timestamp) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HistoryTapeResponse(MatchPayload match, List<ScorePayload> tape) {}
}
