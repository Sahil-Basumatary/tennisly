package dev.sahilbasumatary.tennisdataservice.service;

import dev.sahilbasumatary.tennisdataservice.config.RedisCacheConfig;
import dev.sahilbasumatary.tennisdataservice.dto.response.PlayerResponse;
import dev.sahilbasumatary.tennisdataservice.dto.response.RankingResponse;
import dev.sahilbasumatary.tennisdataservice.entity.Gender;
import dev.sahilbasumatary.tennisdataservice.entity.Player;
import dev.sahilbasumatary.tennisdataservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.tennisdataservice.repository.PlayerRepository;
import dev.sahilbasumatary.tennisdataservice.repository.RankingRepository;
import dev.sahilbasumatary.tennisdataservice.web.PageBounds;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
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

    @Cacheable(
            cacheNames = RedisCacheConfig.PLAYERS_CACHE,
            sync = true,
            key =
                    "(#gender != null ? #gender.name() : 'ALL') + ':' + (#nationality != null ?"
                            + " #nationality : 'ALL') + ':' + (#page == null ? 0 : #page) + ':' +"
                            + " (#size == null ? 50 : #size)")
    @Transactional(readOnly = true)
    public List<PlayerResponse> listPlayers(
            Gender gender, String nationality, Integer page, Integer size) {
        Pageable pageable = PageBounds.of(page, size);
        List<Player> players;
        if (gender != null) {
            players = playerRepository.findByGender(gender, pageable);
        } else if (nationality != null) {
            players = playerRepository.findByNationality(nationality, pageable);
        } else {
            players = playerRepository.findByActiveTrueOrderByCurrentRankingAsc(pageable);
        }
        return players.stream().map(PlayerResponse::from).toList();
    }

    @Cacheable(cacheNames = RedisCacheConfig.PLAYER_CACHE, key = "'ext:' + #externalId", sync = true)
    @Transactional(readOnly = true)
    public PlayerResponse getPlayerByExternalId(String externalId) {
        return playerRepository
                .findByExternalId(externalId)
                .map(PlayerResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Player externalId", externalId));
    }

    @Cacheable(cacheNames = RedisCacheConfig.PLAYER_CACHE, key = "#id", sync = true)
    @Transactional(readOnly = true)
    public PlayerResponse getPlayer(UUID id) {
        return playerRepository
                .findById(id)
                .map(PlayerResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Player", id));
    }

    @Cacheable(cacheNames = RedisCacheConfig.PLAYER_RANKINGS_CACHE, key = "#id", sync = true)
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
