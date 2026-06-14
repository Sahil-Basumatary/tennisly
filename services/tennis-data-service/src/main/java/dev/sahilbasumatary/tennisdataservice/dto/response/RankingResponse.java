package dev.sahilbasumatary.tennisdataservice.dto.response;

import dev.sahilbasumatary.tennisdataservice.entity.Gender;
import dev.sahilbasumatary.tennisdataservice.entity.Ranking;
import dev.sahilbasumatary.tennisdataservice.entity.RankingType;
import java.time.LocalDate;
import java.util.UUID;

public record RankingResponse(
        UUID id,
        UUID playerId,
        String playerName,
        Integer rank,
        Integer points,
        LocalDate rankingDate,
        RankingType rankingType,
        Gender gender) {

    public static RankingResponse from(Ranking ranking) {
        var player = ranking.getPlayer();
        return new RankingResponse(
                ranking.getId(),
                player.getId(),
                player.getFirstName() + " " + player.getLastName(),
                ranking.getRank(),
                ranking.getPoints(),
                ranking.getRankingDate(),
                ranking.getRankingType(),
                ranking.getGender());
    }
}
