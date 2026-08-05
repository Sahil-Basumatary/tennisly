package dev.sahilbasumatary.tennisdataservice.service;

import dev.sahilbasumatary.tennisdataservice.config.RedisCacheConfig;
import dev.sahilbasumatary.tennisdataservice.dto.response.TournamentResponse;
import dev.sahilbasumatary.tennisdataservice.entity.Gender;
import dev.sahilbasumatary.tennisdataservice.entity.Surface;
import dev.sahilbasumatary.tennisdataservice.entity.Tournament;
import dev.sahilbasumatary.tennisdataservice.entity.TournamentLevel;
import dev.sahilbasumatary.tennisdataservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.tennisdataservice.repository.TournamentRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TournamentService {

    private final TournamentRepository tournamentRepository;

    public TournamentService(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    @Cacheable(
            cacheNames = RedisCacheConfig.TOURNAMENTS_CACHE,
            key =
                    "(#level != null ? #level.name() : 'ALL') + ':' + (#surface != null ?"
                            + " #surface.name() : 'ALL') + ':' + (#gender != null ? #gender.name() :"
                            + " 'ALL')")
    @Transactional(readOnly = true)
    public List<TournamentResponse> listTournaments(
            TournamentLevel level, Surface surface, Gender gender) {
        List<Tournament> tournaments;
        if (level != null) {
            tournaments = tournamentRepository.findByLevel(level);
        } else if (surface != null) {
            tournaments = tournamentRepository.findBySurface(surface);
        } else if (gender != null) {
            tournaments = tournamentRepository.findByGender(gender);
        } else {
            tournaments = tournamentRepository.findAll();
        }
        return tournaments.stream().map(TournamentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TournamentResponse getTournament(UUID id) {
        return tournamentRepository
                .findById(id)
                .map(TournamentResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", id));
    }
}
