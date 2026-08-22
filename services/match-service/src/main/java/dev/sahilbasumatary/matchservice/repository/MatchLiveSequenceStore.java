package dev.sahilbasumatary.matchservice.repository;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MatchLiveSequenceStore {

    private final JdbcTemplate jdbcTemplate;

    public MatchLiveSequenceStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long next(UUID matchId) {
        Long sequence =
                jdbcTemplate.queryForObject(
                        """
                        UPDATE matches
                        SET live_sequence = live_sequence + 1
                        WHERE id = ?
                        RETURNING live_sequence
                        """,
                        Long.class,
                        matchId);
        if (sequence == null) {
            throw new IllegalStateException("Unable to allocate live sequence for match " + matchId);
        }
        return sequence;
    }
}
