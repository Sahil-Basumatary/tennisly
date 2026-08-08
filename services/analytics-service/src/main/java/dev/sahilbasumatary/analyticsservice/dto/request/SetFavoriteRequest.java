package dev.sahilbasumatary.analyticsservice.dto.request;

import jakarta.validation.constraints.NotNull;

public record SetFavoriteRequest(@NotNull Boolean favorite) {}
