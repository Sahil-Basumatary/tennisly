CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(255),
    tournament_id UUID,
    surface VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED',
    best_of_sets INTEGER NOT NULL DEFAULT 3,
    scheduled_at TIMESTAMP WITH TIME ZONE,
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    current_score JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_matches_external_id UNIQUE (external_id),
    CONSTRAINT chk_matches_best_of_sets CHECK (best_of_sets IN (3, 5))
);

CREATE INDEX idx_matches_external_id ON matches (external_id);
CREATE INDEX idx_matches_tournament_id ON matches (tournament_id);
CREATE INDEX idx_matches_status ON matches (status);
CREATE INDEX idx_matches_scheduled_at ON matches (scheduled_at);

CREATE TABLE match_players (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    player_id UUID NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    side VARCHAR(16) NOT NULL,
    seed_number INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_match_players_match_side UNIQUE (match_id, side),
    CONSTRAINT uq_match_players_match_player UNIQUE (match_id, player_id)
);

CREATE INDEX idx_match_players_match_id ON match_players (match_id);
CREATE INDEX idx_match_players_player_id ON match_players (player_id);

CREATE TABLE match_points (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    sequence_number INTEGER NOT NULL,
    server_id UUID NOT NULL,
    winner_id UUID NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    rally_length INTEGER NOT NULL DEFAULT 0,
    score_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    shot_summary JSONB NOT NULL DEFAULT '{}'::jsonb,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_match_points_match_sequence UNIQUE (match_id, sequence_number),
    CONSTRAINT chk_match_points_rally_length CHECK (rally_length >= 0)
);

CREATE INDEX idx_match_points_match_id ON match_points (match_id);
CREATE INDEX idx_match_points_winner_id ON match_points (winner_id);

CREATE TABLE match_event_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    event_type VARCHAR(32) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_match_event_logs_match_id ON match_event_logs (match_id);
CREATE INDEX idx_match_event_logs_event_type ON match_event_logs (event_type);
CREATE INDEX idx_match_event_logs_created_at ON match_event_logs (created_at);
