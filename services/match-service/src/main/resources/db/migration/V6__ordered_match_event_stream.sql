ALTER TABLE matches
    ADD COLUMN live_sequence BIGINT NOT NULL DEFAULT 0;

ALTER TABLE match_event_logs
    ADD COLUMN sequence_number BIGINT;

WITH ranked_events AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY match_id
               ORDER BY created_at ASC, id ASC
           ) AS sequence_number
    FROM match_event_logs
)
UPDATE match_event_logs AS event_log
SET sequence_number = ranked_events.sequence_number
FROM ranked_events
WHERE event_log.id = ranked_events.id;

UPDATE matches AS match
SET live_sequence = latest.sequence_number
FROM (
    SELECT match_id, MAX(sequence_number) AS sequence_number
    FROM match_event_logs
    GROUP BY match_id
) AS latest
WHERE match.id = latest.match_id;

ALTER TABLE match_event_logs
    ALTER COLUMN sequence_number SET NOT NULL;

ALTER TABLE match_event_logs
    ADD CONSTRAINT uq_match_event_logs_match_sequence
        UNIQUE (match_id, sequence_number);
