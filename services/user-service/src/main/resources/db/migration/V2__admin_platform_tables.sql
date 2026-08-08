CREATE TABLE organization_api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    key_prefix VARCHAR(16) NOT NULL,
    key_hash VARCHAR(64) NOT NULL,
    scopes JSONB NOT NULL DEFAULT '["read"]'::jsonb,
    active BOOLEAN NOT NULL DEFAULT true,
    last_used_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_by_clerk_id VARCHAR(255) NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_organization_api_keys_key_hash UNIQUE (key_hash)
);

CREATE INDEX idx_organization_api_keys_organization_id ON organization_api_keys (organization_id);
CREATE INDEX idx_organization_api_keys_key_prefix ON organization_api_keys (key_prefix);
CREATE INDEX idx_organization_api_keys_active ON organization_api_keys (active);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_clerk_id VARCHAR(255) NOT NULL,
    actor_email VARCHAR(255),
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(64),
    organization_id UUID,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    ip_address VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_logs_actor_clerk_id ON audit_logs (actor_clerk_id);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_organization_id ON audit_logs (organization_id);

CREATE TABLE usage_daily (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    metric VARCHAR(64) NOT NULL,
    day DATE NOT NULL,
    count BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_usage_daily_org_metric_day UNIQUE (organization_id, metric, day)
);

CREATE INDEX idx_usage_daily_organization_id ON usage_daily (organization_id);
CREATE INDEX idx_usage_daily_day ON usage_daily (day);
