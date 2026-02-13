package dev.sahilbasumatary.authservice.repository;

import dev.sahilbasumatary.authservice.entity.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByClerkId(String clerkId);

    Optional<AppUser> findByEmail(String email);
}
