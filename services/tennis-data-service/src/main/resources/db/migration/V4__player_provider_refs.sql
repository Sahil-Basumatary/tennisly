CREATE TABLE player_provider_refs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id UUID NOT NULL REFERENCES players (id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    provider_ref VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_player_provider_refs_provider_ref UNIQUE (provider, provider_ref),
    CONSTRAINT uq_player_provider_refs_player_provider UNIQUE (player_id, provider)
);

CREATE INDEX idx_player_provider_refs_player_id ON player_provider_refs (player_id);
