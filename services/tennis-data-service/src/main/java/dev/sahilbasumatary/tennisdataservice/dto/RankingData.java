package dev.sahilbasumatary.tennisdataservice.dto;

import dev.sahilbasumatary.tennisdataservice.entity.Gender;
import dev.sahilbasumatary.tennisdataservice.entity.RankingType;
import java.time.LocalDate;

public record RankingData(
        String playerExternalId,
        Integer rank,
        Integer points,
        LocalDate rankingDate,
        RankingType rankingType,
        Gender gender) {}
