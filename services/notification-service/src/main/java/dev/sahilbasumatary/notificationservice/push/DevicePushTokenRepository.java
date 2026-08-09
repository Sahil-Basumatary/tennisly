package dev.sahilbasumatary.notificationservice.push;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DevicePushTokenRepository extends JpaRepository<DevicePushToken, UUID> {

    Optional<DevicePushToken> findByToken(String token);

    List<DevicePushToken> findByClerkIdAndActiveTrue(String clerkId);

    List<DevicePushToken> findByClerkId(String clerkId);
}
