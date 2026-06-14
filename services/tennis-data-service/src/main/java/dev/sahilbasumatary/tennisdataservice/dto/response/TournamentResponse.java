package dev.sahilbasumatary.tennisdataservice.dto.response;

import dev.sahilbasumatary.tennisdataservice.entity.Gender;
import dev.sahilbasumatary.tennisdataservice.entity.Surface;
import dev.sahilbasumatary.tennisdataservice.entity.Tournament;
import dev.sahilbasumatary.tennisdataservice.entity.TournamentLevel;
import java.time.Instant;
import java.util.UUID;

public record TournamentResponse(
        UUID id,
        String externalId,
        String name,
        TournamentLevel level,
        Surface surface,
        Gender gender,
        String city,
        String countryCode,
        String venueName,
        Instant createdAt,
        Instant updatedAt) {

    public static TournamentResponse from(Tournament tournament) {
        return new TournamentResponse(
                tournament.getId(),
                tournament.getExternalId(),
                tournament.getName(),
                tournament.getLevel(),
                tournament.getSurface(),
                tournament.getGender(),
                tournament.getCity(),
                tournament.getCountryCode(),
                tournament.getVenueName(),
                tournament.getCreatedAt(),
                tournament.getUpdatedAt());
    }
}
