package dev.sahilbasumatary.tennisdataservice.dto;

import dev.sahilbasumatary.tennisdataservice.entity.Gender;
import dev.sahilbasumatary.tennisdataservice.entity.Surface;
import dev.sahilbasumatary.tennisdataservice.entity.TournamentLevel;

public record TournamentData(
        String externalId,
        String name,
        TournamentLevel level,
        Surface surface,
        Gender gender,
        String city,
        String countryCode,
        String venueName) {}
