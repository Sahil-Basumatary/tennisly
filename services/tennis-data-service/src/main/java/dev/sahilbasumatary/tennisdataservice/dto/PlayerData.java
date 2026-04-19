package dev.sahilbasumatary.tennisdataservice.dto;

import dev.sahilbasumatary.tennisdataservice.entity.Backhand;
import dev.sahilbasumatary.tennisdataservice.entity.Gender;
import dev.sahilbasumatary.tennisdataservice.entity.Hand;
import java.time.LocalDate;

public record PlayerData(
        String externalId,
        String firstName,
        String lastName,
        String nationality,
        LocalDate dateOfBirth,
        Hand hand,
        Backhand backhand,
        Integer heightCm,
        Integer weightKg,
        Integer proYear,
        Integer currentRanking,
        Integer currentPoints,
        Gender gender) {}
