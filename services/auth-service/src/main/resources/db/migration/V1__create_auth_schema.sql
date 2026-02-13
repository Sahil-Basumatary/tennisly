CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clerk_org_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    image_url VARCHAR(512),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_organizations_clerk_org_id UNIQUE (clerk_org_id),
    CONSTRAINT uq_organizations_slug UNIQUE (slug)
);

CREATE INDEX idx_organizations_clerk_org_id ON organizations (clerk_org_id);
CREATE INDEX idx_organizations_slug ON organizations (slug);

CREATE TABLE app_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clerk_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    image_url VARCHAR(512),
    role VARCHAR(32) NOT NULL DEFAULT 'USER',
    organization_id UUID REFERENCES organizations (id),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_users_clerk_id UNIQUE (clerk_id),
    CONSTRAINT uq_app_users_email UNIQUE (email)
);

CREATE INDEX idx_app_users_clerk_id ON app_users (clerk_id);
CREATE INDEX idx_app_users_email ON app_users (email);
CREATE INDEX idx_app_users_organization_id ON app_users (organization_id);
