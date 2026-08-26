CREATE TABLE match_archive_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status VARCHAR(16) NOT NULL,
    match_id UUID,
    idempotency_key VARCHAR(128),
    source_rows BIGINT NOT NULL DEFAULT 0,
    accepted_rows BIGINT NOT NULL DEFAULT 0,
    duplicate_rows BIGINT NOT NULL DEFAULT 0,
    first_sequence INTEGER,
    last_sequence INTEGER,
    live_sequence BIGINT,
    checksum VARCHAR(64),
    last_error VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_match_archive_jobs_idempotency
    ON match_archive_jobs (match_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE match_archive_staging (
    job_id UUID NOT NULL REFERENCES match_archive_jobs (id) ON DELETE CASCADE,
    match_id UUID NOT NULL,
    sequence_number INTEGER NOT NULL,
    server_id UUID NOT NULL,
    winner_id UUID NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    rally_length INTEGER,
    score_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    shot_summary JSONB NOT NULL DEFAULT '{}'::jsonb,
    PRIMARY KEY (job_id, match_id, sequence_number)
);

CREATE INDEX idx_match_archive_staging_match
    ON match_archive_staging (job_id, match_id);
