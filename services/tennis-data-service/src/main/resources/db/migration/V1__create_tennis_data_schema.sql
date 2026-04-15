CREATE TABLE players (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    nationality VARCHAR(100),
    date_of_birth DATE,
    hand VARCHAR(16),
    backhand VARCHAR(16),
    height_cm INTEGER,
    weight_kg INTEGER,
    pro_year INTEGER,
    current_ranking INTEGER,
    current_points INTEGER,
    gender VARCHAR(16) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_players_external_id UNIQUE (external_id)
);

CREATE INDEX idx_players_external_id ON players (external_id);
CREATE INDEX idx_players_last_name ON players (last_name);
CREATE INDEX idx_players_nationality ON players (nationality);
CREATE INDEX idx_players_gender ON players (gender);
CREATE INDEX idx_players_current_ranking ON players (current_ranking);

CREATE TABLE tournaments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    level VARCHAR(32) NOT NULL,
    surface VARCHAR(16) NOT NULL,
    gender VARCHAR(16) NOT NULL,
    city VARCHAR(255),
    country_code VARCHAR(3),
    venue_name VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tournaments_external_id UNIQUE (external_id)
);

CREATE INDEX idx_tournaments_external_id ON tournaments (external_id);
CREATE INDEX idx_tournaments_level ON tournaments (level);
CREATE INDEX idx_tournaments_surface ON tournaments (surface);
CREATE INDEX idx_tournaments_gender ON tournaments (gender);

CREATE TABLE rankings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id UUID NOT NULL REFERENCES players (id) ON DELETE CASCADE,
    rank INTEGER NOT NULL,
    points INTEGER NOT NULL,
    ranking_date DATE NOT NULL,
    ranking_type VARCHAR(16) NOT NULL,
    gender VARCHAR(16) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_rankings_player_date_type UNIQUE (player_id, ranking_date, ranking_type)
);

CREATE INDEX idx_rankings_player_id ON rankings (player_id);
CREATE INDEX idx_rankings_ranking_date ON rankings (ranking_date);
CREATE INDEX idx_rankings_ranking_type ON rankings (ranking_type);
CREATE INDEX idx_rankings_gender ON rankings (gender);

CREATE TABLE seasons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(255) NOT NULL,
    tournament_id UUID NOT NULL REFERENCES tournaments (id) ON DELETE CASCADE,
    year INTEGER NOT NULL,
    start_date DATE,
    end_date DATE,
    prize_money BIGINT,
    currency VARCHAR(3),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_seasons_external_id UNIQUE (external_id)
);

CREATE INDEX idx_seasons_external_id ON seasons (external_id);
CREATE INDEX idx_seasons_tournament_id ON seasons (tournament_id);
CREATE INDEX idx_seasons_year ON seasons (year);

CREATE TABLE shot_distributions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shot_type VARCHAR(32) NOT NULL,
    surface VARCHAR(16) NOT NULL,
    player_tier VARCHAR(16) NOT NULL,
    mean_landing_x DOUBLE PRECISION NOT NULL,
    mean_landing_y DOUBLE PRECISION NOT NULL,
    std_dev_x DOUBLE PRECISION NOT NULL,
    std_dev_y DOUBLE PRECISION NOT NULL,
    mean_speed_kmh DOUBLE PRECISION NOT NULL,
    speed_std_dev DOUBLE PRECISION NOT NULL,
    mean_spin_rpm DOUBLE PRECISION NOT NULL,
    spin_std_dev DOUBLE PRECISION NOT NULL,
    mean_arc_height DOUBLE PRECISION NOT NULL,
    arc_std_dev DOUBLE PRECISION NOT NULL,
    sample_size INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_shot_dist_type_surface_tier UNIQUE (shot_type, surface, player_tier)
);

CREATE INDEX idx_shot_distributions_shot_type ON shot_distributions (shot_type);
CREATE INDEX idx_shot_distributions_surface ON shot_distributions (surface);
CREATE INDEX idx_shot_distributions_player_tier ON shot_distributions (player_tier);
