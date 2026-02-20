CREATE TABLE user_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clerk_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    phone VARCHAR(50),
    country VARCHAR(100),
    timezone VARCHAR(100),
    bio TEXT,
    avatar_url VARCHAR(512),
    skill_level VARCHAR(32),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_profiles_clerk_id UNIQUE (clerk_id),
    CONSTRAINT uq_user_profiles_email UNIQUE (email)
);

CREATE INDEX idx_user_profiles_clerk_id ON user_profiles (clerk_id);
CREATE INDEX idx_user_profiles_email ON user_profiles (email);

CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clerk_org_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,
    logo_url VARCHAR(512),
    website VARCHAR(512),
    plan_tier VARCHAR(32) NOT NULL DEFAULT 'FREE',
    max_members INTEGER NOT NULL DEFAULT 10,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_organizations_clerk_org_id UNIQUE (clerk_org_id),
    CONSTRAINT uq_organizations_slug UNIQUE (slug)
);

CREATE INDEX idx_organizations_clerk_org_id ON organizations (clerk_org_id);
CREATE INDEX idx_organizations_slug ON organizations (slug);

CREATE TABLE organization_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_profile_id UUID NOT NULL REFERENCES user_profiles (id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL DEFAULT 'MEMBER',
    active BOOLEAN NOT NULL DEFAULT true,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_membership_user_org UNIQUE (user_profile_id, organization_id)
);

CREATE INDEX idx_memberships_user_profile_id ON organization_memberships (user_profile_id);
CREATE INDEX idx_memberships_organization_id ON organization_memberships (organization_id);
CREATE INDEX idx_memberships_role ON organization_memberships (role);

CREATE TABLE user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_profile_id UUID NOT NULL REFERENCES user_profiles (id) ON DELETE CASCADE,
    theme VARCHAR(32) NOT NULL DEFAULT 'SYSTEM',
    notifications_enabled BOOLEAN NOT NULL DEFAULT true,
    email_notifications BOOLEAN NOT NULL DEFAULT true,
    favorite_surface VARCHAR(64),
    locale VARCHAR(16) NOT NULL DEFAULT 'en',
    extra_settings JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_preferences_user_profile_id UNIQUE (user_profile_id)
);

CREATE INDEX idx_user_preferences_user_profile_id ON user_preferences (user_profile_id);
