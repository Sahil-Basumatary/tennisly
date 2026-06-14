package dev.sahilbasumatary.tennisdataservice.dto.response;

import dev.sahilbasumatary.tennisdataservice.entity.Backhand;
import dev.sahilbasumatary.tennisdataservice.entity.Gender;
import dev.sahilbasumatary.tennisdataservice.entity.Hand;
import dev.sahilbasumatary.tennisdataservice.entity.Player;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PlayerResponse(
        UUID id,
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
        Gender gender,
        Instant createdAt,
        Instant updatedAt) {

    public static PlayerResponse from(Player player) {
        return new PlayerResponse(
                player.getId(),
                player.getExternalId(),
                player.getFirstName(),
                player.getLastName(),
                player.getNationality(),
                player.getDateOfBirth(),
                player.getHand(),
                player.getBackhand(),
                player.getHeightCm(),
                player.getWeightKg(),
                player.getProYear(),
                player.getCurrentRanking(),
                player.getCurrentPoints(),
                player.getGender(),
                player.getCreatedAt(),
                player.getUpdatedAt());
    }
}
