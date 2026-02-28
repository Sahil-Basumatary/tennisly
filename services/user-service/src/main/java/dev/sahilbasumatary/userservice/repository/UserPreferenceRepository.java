package dev.sahilbasumatary.userservice.repository;

import dev.sahilbasumatary.userservice.entity.UserPreference;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {

    Optional<UserPreference> findByUserProfileId(UUID userProfileId);
}
