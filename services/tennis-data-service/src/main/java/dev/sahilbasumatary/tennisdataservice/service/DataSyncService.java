package dev.sahilbasumatary.tennisdataservice.service;

import dev.sahilbasumatary.common.event.TennisDataEvent;
import dev.sahilbasumatary.common.kafka.EventPublisher;
import dev.sahilbasumatary.common.kafka.TopicNames;
import dev.sahilbasumatary.tennisdataservice.config.RedisCacheConfig;
import dev.sahilbasumatary.tennisdataservice.dto.PlayerData;
import dev.sahilbasumatary.tennisdataservice.dto.RankingData;
import dev.sahilbasumatary.tennisdataservice.dto.TournamentData;
import dev.sahilbasumatary.tennisdataservice.entity.Player;
import dev.sahilbasumatary.tennisdataservice.entity.Ranking;
import dev.sahilbasumatary.tennisdataservice.entity.Tournament;
import dev.sahilbasumatary.tennisdataservice.provider.TennisDataProvider;
import dev.sahilbasumatary.tennisdataservice.repository.PlayerRepository;
import dev.sahilbasumatary.tennisdataservice.repository.RankingRepository;
import dev.sahilbasumatary.tennisdataservice.repository.TournamentRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataSyncService {

    private static final Logger log = LoggerFactory.getLogger(DataSyncService.class);

    private final TennisDataProvider provider;
    private final PlayerRepository playerRepository;
    private final TournamentRepository tournamentRepository;
    private final RankingRepository rankingRepository;
    private final EventPublisher eventPublisher;
    private final String providerName;

    public DataSyncService(
            TennisDataProvider provider,
            PlayerRepository playerRepository,
            TournamentRepository tournamentRepository,
            RankingRepository rankingRepository,
            EventPublisher eventPublisher,
            @Value("${tennis.data.provider:mock}") String providerName) {
        this.provider = provider;
        this.playerRepository = playerRepository;
        this.tournamentRepository = tournamentRepository;
        this.rankingRepository = rankingRepository;
        this.eventPublisher = eventPublisher;
        this.providerName = providerName;
    }

    @Caching(
            evict = {
                @CacheEvict(cacheNames = RedisCacheConfig.PLAYER_CACHE, allEntries = true),
                @CacheEvict(cacheNames = RedisCacheConfig.PLAYERS_CACHE, allEntries = true),
                @CacheEvict(cacheNames = RedisCacheConfig.PLAYER_RANKINGS_CACHE, allEntries = true),
                @CacheEvict(cacheNames = RedisCacheConfig.RANKINGS_CACHE, allEntries = true)
            })
    @Transactional
    public int syncPlayers() {
        List<PlayerData> source = provider.fetchPlayers();
        for (PlayerData data : source) {
            Player player =
                    playerRepository.findByExternalId(data.externalId()).orElseGet(Player::new);
            applyPlayer(player, data);
            playerRepository.save(player);
        }
        int processed = source.size();
        log.info("Player sync complete: {} records processed", processed);
        publish(TennisDataEvent.playersSynced(processed, providerName));
        return processed;
    }

    @CacheEvict(cacheNames = RedisCacheConfig.TOURNAMENTS_CACHE, allEntries = true)
    @Transactional
    public int syncTournaments() {
        List<TournamentData> source = provider.fetchTournaments();
        for (TournamentData data : source) {
            Tournament tournament =
                    tournamentRepository
                            .findByExternalId(data.externalId())
                            .orElseGet(Tournament::new);
            applyTournament(tournament, data);
            tournamentRepository.save(tournament);
        }
        int processed = source.size();
        log.info("Tournament sync complete: {} records processed", processed);
        publish(TennisDataEvent.tournamentsSynced(processed, providerName));
        return processed;
    }

    @Caching(
            evict = {
                @CacheEvict(cacheNames = RedisCacheConfig.RANKINGS_CACHE, allEntries = true),
                @CacheEvict(cacheNames = RedisCacheConfig.PLAYER_RANKINGS_CACHE, allEntries = true)
            })
    @Transactional
    public int syncRankings() {
        List<RankingData> source = provider.fetchRankings();
        int processed = 0;
        for (RankingData data : source) {
            Player player = playerRepository.findByExternalId(data.playerExternalId()).orElse(null);
            if (player == null) {
                // Rankings reference players, so a player sync must run first.
                log.warn(
                        "Skipping ranking for unknown player externalId={}",
                        data.playerExternalId());
                continue;
            }
            Ranking ranking =
                    rankingRepository
                            .findByPlayerIdAndRankingDateAndRankingType(
                                    player.getId(), data.rankingDate(), data.rankingType())
                            .orElseGet(Ranking::new);
            ranking.setPlayer(player);
            ranking.setRank(data.rank());
            ranking.setPoints(data.points());
            ranking.setRankingDate(data.rankingDate());
            ranking.setRankingType(data.rankingType());
            ranking.setGender(data.gender());
            ranking.setActive(true);
            rankingRepository.save(ranking);
            processed++;
        }
        log.info("Ranking sync complete: {} records processed", processed);
        publish(TennisDataEvent.rankingsSynced(processed, providerName));
        return processed;
    }

    private void publish(TennisDataEvent event) {
        eventPublisher.publish(TopicNames.TENNIS_DATA_EVENTS, event.getResourceType(), event);
    }

    private void applyPlayer(Player player, PlayerData data) {
        player.setExternalId(data.externalId());
        player.setFirstName(data.firstName());
        player.setLastName(data.lastName());
        player.setNationality(data.nationality());
        player.setDateOfBirth(data.dateOfBirth());
        player.setHand(data.hand());
        player.setBackhand(data.backhand());
        player.setHeightCm(data.heightCm());
        player.setWeightKg(data.weightKg());
        player.setProYear(data.proYear());
        player.setCurrentRanking(data.currentRanking());
        player.setCurrentPoints(data.currentPoints());
        player.setGender(data.gender());
        player.setActive(true);
    }

    private void applyTournament(Tournament tournament, TournamentData data) {
        tournament.setExternalId(data.externalId());
        tournament.setName(data.name());
        tournament.setLevel(data.level());
        tournament.setSurface(data.surface());
        tournament.setGender(data.gender());
        tournament.setCity(data.city());
        tournament.setCountryCode(data.countryCode());
        tournament.setVenueName(data.venueName());
        tournament.setActive(true);
    }
}
