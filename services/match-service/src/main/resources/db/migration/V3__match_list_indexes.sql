-- Composite indexes for filtered match catalogue lists (status / tournament + schedule).
CREATE INDEX IF NOT EXISTS idx_matches_status_scheduled_at
    ON matches (status, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_matches_tournament_status_scheduled_at
    ON matches (tournament_id, status, scheduled_at);
