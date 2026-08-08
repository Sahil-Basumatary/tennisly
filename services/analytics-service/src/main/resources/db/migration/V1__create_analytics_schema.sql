CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE analytics_ingest_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) NOT NULL,
    match_id UUID NOT NULL,
    event_type VARCHAR(64),
    processed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_analytics_ingest_receipts_event_id UNIQUE (event_id)
);

CREATE INDEX idx_analytics_ingest_receipts_match_id ON analytics_ingest_receipts (match_id);

CREATE TABLE analytics_reindex_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status VARCHAR(32) NOT NULL,
    cursor_match_id UUID,
    processed_count INTEGER NOT NULL DEFAULT 0,
    total_count INTEGER,
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_analytics_reindex_jobs_status ON analytics_reindex_jobs (status);

CREATE TABLE saved_analytics_views (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    organization_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    favorite BOOLEAN NOT NULL DEFAULT FALSE,
    config JSONB NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_saved_analytics_views_user_id ON saved_analytics_views (user_id);
