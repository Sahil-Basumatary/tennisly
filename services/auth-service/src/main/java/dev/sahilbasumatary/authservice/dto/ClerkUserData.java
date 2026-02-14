package dev.sahilbasumatary.authservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ClerkUserData(
        String id,
        List<ClerkEmailAddress> emailAddresses,
        String primaryEmailAddressId,
        String firstName,
        String lastName,
        String imageUrl
) {}
