CREATE TABLE device_push_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clerk_id VARCHAR(255) NOT NULL,
    token VARCHAR(512) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_device_push_tokens_token UNIQUE (token)
);

CREATE INDEX idx_device_push_tokens_clerk_id ON device_push_tokens (clerk_id);
CREATE INDEX idx_device_push_tokens_active ON device_push_tokens (active);

CREATE TABLE push_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(200) NOT NULL,
    category VARCHAR(64) NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    recipient_clerk_id VARCHAR(255) NOT NULL,
    device_token_id UUID,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_push_deliveries_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_push_deliveries_category ON push_deliveries (category);
CREATE INDEX idx_push_deliveries_created_at ON push_deliveries (created_at DESC);
CREATE INDEX idx_push_deliveries_status ON push_deliveries (status);
