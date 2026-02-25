package dev.sahilbasumatary.userservice.repository;

import dev.sahilbasumatary.userservice.entity.UserProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByClerkId(String clerkId);

    Optional<UserProfile> findByEmail(String email);

    boolean existsByClerkId(String clerkId);
}
