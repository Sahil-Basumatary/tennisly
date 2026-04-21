package dev.sahilbasumatary.tennisdataservice.provider;

import dev.sahilbasumatary.tennisdataservice.dto.PlayerData;
import dev.sahilbasumatary.tennisdataservice.dto.RankingData;
import dev.sahilbasumatary.tennisdataservice.dto.TournamentData;
import java.util.List;
import java.util.Optional;

public interface TennisDataProvider {

    List<PlayerData> fetchPlayers();

    Optional<PlayerData> fetchPlayerById(String externalId);

    List<RankingData> fetchRankings();

    List<TournamentData> fetchTournaments();
}
