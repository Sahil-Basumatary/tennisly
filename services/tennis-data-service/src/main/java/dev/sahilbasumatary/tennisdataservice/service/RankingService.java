package dev.sahilbasumatary.tennisdataservice.service;

import dev.sahilbasumatary.tennisdataservice.config.RedisCacheConfig;
import dev.sahilbasumatary.tennisdataservice.dto.response.RankingResponse;
import dev.sahilbasumatary.tennisdataservice.entity.Gender;
import dev.sahilbasumatary.tennisdataservice.entity.RankingType;
import dev.sahilbasumatary.tennisdataservice.repository.RankingRepository;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RankingService {

    private final RankingRepository rankingRepository;

    public RankingService(RankingRepository rankingRepository) {
        this.rankingRepository = rankingRepository;
    }

    @Cacheable(
            cacheNames = RedisCacheConfig.RANKINGS_CACHE,
            sync = true,
            key = "#gender.name() + ':' + #rankingType.name()")
    @Transactional(readOnly = true)
    public List<RankingResponse> getCurrentRankings(Gender gender, RankingType rankingType) {
        // "Current" means the most recent snapshot we hold, not a hard-coded date.
        return rankingRepository
                .findTopByRankingTypeAndGenderOrderByRankingDateDesc(rankingType, gender)
                .map(
                        latest ->
                                rankingRepository
                                        .findByRankingDateAndRankingTypeAndGenderOrderByRankAsc(
                                                latest.getRankingDate(), rankingType, gender)
                                        .stream()
                                        .map(RankingResponse::from)
                                        .toList())
                .orElseGet(List::of);
    }
}
