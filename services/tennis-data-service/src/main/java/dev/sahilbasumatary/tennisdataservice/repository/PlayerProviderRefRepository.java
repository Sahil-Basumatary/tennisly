package dev.sahilbasumatary.tennisdataservice.repository;

import dev.sahilbasumatary.tennisdataservice.entity.PlayerProviderRef;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerProviderRefRepository extends JpaRepository<PlayerProviderRef, UUID> {

    Optional<PlayerProviderRef> findByProviderAndProviderRef(String provider, String providerRef);

    Optional<PlayerProviderRef> findByPlayerIdAndProvider(UUID playerId, String provider);

    // Catch-and-retry after DataIntegrityViolationException marks the surrounding transaction
    // rollback-only, so concurrent resolves must lose the race without throwing.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value =
                    """
                    INSERT INTO player_provider_refs
                        (id, player_id, provider, provider_ref, created_at, updated_at)
                    VALUES
                        (gen_random_uuid(), :playerId, :provider, :providerRef,
                         CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    ON CONFLICT DO NOTHING
                    """,
            nativeQuery = true)
    int insertIgnoreConflict(
            @Param("playerId") UUID playerId,
            @Param("provider") String provider,
            @Param("providerRef") String providerRef);
}
