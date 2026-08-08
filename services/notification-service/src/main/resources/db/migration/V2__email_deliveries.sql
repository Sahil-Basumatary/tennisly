CREATE TABLE email_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(160) NOT NULL,
    category VARCHAR(64) NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    recipient_email VARCHAR(320) NOT NULL,
    recipient_clerk_id VARCHAR(255),
    subject VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_email_deliveries_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_email_deliveries_category ON email_deliveries (category);
CREATE INDEX idx_email_deliveries_created_at ON email_deliveries (created_at DESC);
CREATE INDEX idx_email_deliveries_status ON email_deliveries (status);
