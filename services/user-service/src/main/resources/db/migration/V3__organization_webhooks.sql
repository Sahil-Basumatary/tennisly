CREATE TABLE organization_webhook_endpoints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    target_url VARCHAR(2048) NOT NULL,
    secret_prefix VARCHAR(16) NOT NULL,
    secret_hash VARCHAR(64) NOT NULL,
    secret_ciphertext TEXT NOT NULL,
    event_types JSONB NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    description VARCHAR(500),
    created_by_clerk_id VARCHAR(255) NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    last_delivery_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_organization_webhook_endpoints_secret_hash UNIQUE (secret_hash)
);

CREATE INDEX idx_webhook_endpoints_organization_id ON organization_webhook_endpoints (organization_id);
CREATE INDEX idx_webhook_endpoints_active ON organization_webhook_endpoints (active);
