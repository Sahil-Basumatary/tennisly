CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE replay_artifacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL,
    storage_bucket VARCHAR(255) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    surface VARCHAR(16) NOT NULL,
    frame_rate INTEGER NOT NULL,
    point_count INTEGER NOT NULL,
    shot_count INTEGER NOT NULL,
    frame_count INTEGER NOT NULL,
    duration_seconds DOUBLE PRECISION NOT NULL,
    content_encoding VARCHAR(32) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uncompressed_bytes BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    engine_version VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'READY',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_replay_artifacts_match_id UNIQUE (match_id),
    CONSTRAINT chk_replay_artifacts_size CHECK (size_bytes >= 0),
    CONSTRAINT chk_replay_artifacts_frame_count CHECK (frame_count >= 0)
);

CREATE INDEX idx_replay_artifacts_match_id ON replay_artifacts (match_id);
CREATE INDEX idx_replay_artifacts_status ON replay_artifacts (status);
CREATE INDEX idx_replay_artifacts_created_at ON replay_artifacts (created_at);
