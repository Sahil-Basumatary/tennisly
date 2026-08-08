CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE webhook_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    endpoint_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    next_attempt_at TIMESTAMPTZ,
    last_http_status INT,
    last_error TEXT,
    response_ms INT,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMPTZ
);

CREATE INDEX idx_webhook_deliveries_poll
    ON webhook_deliveries (status, next_attempt_at);

CREATE INDEX idx_webhook_deliveries_org
    ON webhook_deliveries (organization_id);

CREATE INDEX idx_webhook_deliveries_event_type
    ON webhook_deliveries (event_type);
