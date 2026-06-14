package dev.sahilbasumatary.tennisdataservice.service;

import dev.sahilbasumatary.tennisdataservice.dto.response.PlayerResponse;
import dev.sahilbasumatary.tennisdataservice.dto.response.RankingResponse;
import dev.sahilbasumatary.tennisdataservice.entity.Gender;
import dev.sahilbasumatary.tennisdataservice.entity.Player;
import dev.sahilbasumatary.tennisdataservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.tennisdataservice.repository.PlayerRepository;
import dev.sahilbasumatary.tennisdataservice.repository.RankingRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final RankingRepository rankingRepository;

    public PlayerService(PlayerRepository playerRepository, RankingRepository rankingRepository) {
        this.playerRepository = playerRepository;
        this.rankingRepository = rankingRepository;
    }

    @Transactional(readOnly = true)
    public List<PlayerResponse> listPlayers(Gender gender, String nationality) {
        List<Player> players;
        if (gender != null) {
            players = playerRepository.findByGender(gender);
        } else if (nationality != null) {
            players = playerRepository.findByNationality(nationality);
        } else {
            players = playerRepository.findByActiveTrueOrderByCurrentRankingAsc();
        }
        return players.stream().map(PlayerResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PlayerResponse getPlayer(UUID id) {
        return playerRepository
                .findById(id)
                .map(PlayerResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Player", id));
    }

    @Transactional(readOnly = true)
    public List<RankingResponse> getPlayerRankingHistory(UUID id) {
        if (!playerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Player", id);
        }
        return rankingRepository.findByPlayerIdOrderByRankingDateDesc(id).stream()
                .map(RankingResponse::from)
                .toList();
    }
}
