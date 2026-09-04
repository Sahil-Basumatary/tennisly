package dev.sahilbasumatary.matchservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sahilbasumatary.matchservice.client.TennisDataMatchClient;
import dev.sahilbasumatary.matchservice.client.TennisDataMatchClient.PlayerSide;
import dev.sahilbasumatary.matchservice.client.TennisDataMatchClient.ResolvedPlayerDto;
import dev.sahilbasumatary.matchservice.client.TennisDataMatchClient.UpstreamMatchDto;
import dev.sahilbasumatary.matchservice.entity.Match;
import dev.sahilbasumatary.matchservice.repository.MatchPointRepository;
import dev.sahilbasumatary.matchservice.repository.MatchRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MatchIngestionServiceTest {

    @Test
    void liveBoardChecksMainToursBeforeTheGeneralFeed() {
        TennisDataMatchClient matchClient = mock(TennisDataMatchClient.class);
        final MatchIngestionService service =
                new MatchIngestionService(
                        matchClient,
                        mock(MatchRepository.class),
                        mock(MatchPointRepository.class),
                        mock(MatchEventLogService.class),
                        mock(MatchEventDispatch.class),
                        50);

        service.ingestLiveBoard();

        verify(matchClient).listMatches("live", "atp", 50, 0);
        verify(matchClient).listMatches("live", "wta", 50, 0);
        verify(matchClient).listMatches("live", 50, 0);
        verify(matchClient).listMatches("upcoming", 50, 0);
    }

    @Test
    void storesTheSourceTourForLaterOrdering() {
        TennisDataMatchClient matchClient = mock(TennisDataMatchClient.class);
        MatchRepository matchRepository = mock(MatchRepository.class);
        MatchEventLogService eventLogService = mock(MatchEventLogService.class);
        final MatchIngestionService service =
                new MatchIngestionService(
                        matchClient,
                        matchRepository,
                        mock(MatchPointRepository.class),
                        eventLogService,
                        mock(MatchEventDispatch.class),
                        50);
        ResolvedPlayerDto home =
                new ResolvedPlayerDto(
                        UUID.randomUUID(), "home", "Home", "Player", "GBR", "FEMALE");
        ResolvedPlayerDto away =
                new ResolvedPlayerDto(
                        UUID.randomUUID(), "away", "Away", "Player", "USA", "FEMALE");
        when(matchClient.resolvePlayer(any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(home), Optional.of(away));
        when(matchRepository.findByExternalId("lta-42")).thenReturn(Optional.empty());
        when(matchRepository.saveAndFlush(any(Match.class)))
                .thenAnswer(
                        invocation -> {
                            Match saved = invocation.getArgument(0);
                            saved.setId(UUID.randomUUID());
                            return saved;
                        });
        UpstreamMatchDto upstream =
                new UpstreamMatchDto(
                        42,
                        "lta-42",
                        "Monterrey Open",
                        "HARD",
                        "BEST_OF_THREE",
                        "Round of 32",
                        "live",
                        false,
                        false,
                        Instant.parse("2026-08-25T12:00:00Z"),
                        new PlayerSide(1L, "Home", "Player", "Home Player"),
                        new PlayerSide(2L, "Away", "Player", "Away Player"),
                        null,
                        null);

        service.upsertMatch(upstream, false, "wta");

        ArgumentCaptor<Match> saved = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).saveAndFlush(saved.capture());
        assertEquals("wta", saved.getValue().getMetadata().get("tour"));
    }
}
