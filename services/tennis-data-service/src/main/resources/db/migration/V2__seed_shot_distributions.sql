WITH base_shots AS (
    SELECT *
    FROM (
        VALUES
            ('FIRST_SERVE', 1.05, 8.35, 0.72, 0.92, 188.0, 14.0, 2450.0, 420.0, 2.25, 0.34, 5200),
            ('SECOND_SERVE', 0.80, 7.65, 0.82, 1.05, 145.0, 12.0, 3650.0, 520.0, 2.85, 0.42, 4800),
            ('FOREHAND_GROUNDSTROKE', 1.45, 9.15, 1.18, 1.32, 119.0, 11.0, 2950.0, 460.0, 1.85, 0.31, 7200),
            ('BACKHAND_GROUNDSTROKE', -1.20, 8.85, 1.12, 1.28, 108.0, 10.0, 2550.0, 430.0, 1.78, 0.29, 6800),
            ('FOREHAND_VOLLEY', 1.65, 5.85, 0.82, 0.86, 76.0, 9.0, 1250.0, 260.0, 0.95, 0.18, 2100),
            ('BACKHAND_VOLLEY', -1.45, 5.70, 0.78, 0.82, 70.0, 8.0, 1150.0, 240.0, 0.90, 0.17, 1900),
            ('FOREHAND_SLICE', 1.20, 7.95, 1.02, 1.10, 78.0, 8.0, 1650.0, 320.0, 1.25, 0.24, 2600),
            ('BACKHAND_SLICE', -1.05, 7.70, 0.98, 1.08, 72.0, 8.0, 1750.0, 340.0, 1.22, 0.23, 3100),
            ('DROP_SHOT', 0.35, 3.35, 0.52, 0.58, 42.0, 6.0, 950.0, 220.0, 0.72, 0.16, 1200),
            ('LOB', -0.10, 10.65, 1.26, 1.42, 63.0, 9.0, 1850.0, 380.0, 5.80, 0.75, 1500),
            ('OVERHEAD', 0.95, 6.20, 0.92, 1.05, 132.0, 14.0, 1550.0, 360.0, 2.40, 0.45, 1300)
    ) AS shot(
        shot_type,
        mean_landing_x,
        mean_landing_y,
        std_dev_x,
        std_dev_y,
        mean_speed_kmh,
        speed_std_dev,
        mean_spin_rpm,
        spin_std_dev,
        mean_arc_height,
        arc_std_dev,
        sample_size
    )
),
surface_modifiers AS (
    SELECT *
    FROM (
        VALUES
            ('HARD', 1.00, 1.00, 1.00, 1.00),
            ('CLAY', 0.94, 1.04, 1.14, 1.08),
            ('GRASS', 1.07, 0.96, 0.88, 0.93),
            ('CARPET', 1.10, 0.94, 0.82, 0.90)
    ) AS surface(surface, speed_multiplier, depth_multiplier, spin_multiplier, spread_multiplier)
),
tier_modifiers AS (
    SELECT *
    FROM (
        VALUES
            ('TOP_10', 1.05, 0.78, 1.08, 1.20),
            ('TOP_50', 1.02, 0.88, 1.03, 1.00),
            ('TOP_100', 1.00, 1.00, 1.00, 0.82),
            ('OTHER', 0.96, 1.18, 0.94, 0.58)
    ) AS tier(player_tier, speed_multiplier, spread_multiplier, spin_multiplier, sample_multiplier)
)
INSERT INTO shot_distributions (
    shot_type,
    surface,
    player_tier,
    mean_landing_x,
    mean_landing_y,
    std_dev_x,
    std_dev_y,
    mean_speed_kmh,
    speed_std_dev,
    mean_spin_rpm,
    spin_std_dev,
    mean_arc_height,
    arc_std_dev,
    sample_size,
    active
)
SELECT
    base_shots.shot_type,
    surface_modifiers.surface,
    tier_modifiers.player_tier,
    base_shots.mean_landing_x,
    base_shots.mean_landing_y * surface_modifiers.depth_multiplier,
    base_shots.std_dev_x * surface_modifiers.spread_multiplier * tier_modifiers.spread_multiplier,
    base_shots.std_dev_y * surface_modifiers.spread_multiplier * tier_modifiers.spread_multiplier,
    base_shots.mean_speed_kmh * surface_modifiers.speed_multiplier * tier_modifiers.speed_multiplier,
    base_shots.speed_std_dev * tier_modifiers.spread_multiplier,
    base_shots.mean_spin_rpm * surface_modifiers.spin_multiplier * tier_modifiers.spin_multiplier,
    base_shots.spin_std_dev * tier_modifiers.spread_multiplier,
    base_shots.mean_arc_height,
    base_shots.arc_std_dev * tier_modifiers.spread_multiplier,
    GREATEST(100, (base_shots.sample_size * tier_modifiers.sample_multiplier)::INTEGER),
    true
FROM base_shots
CROSS JOIN surface_modifiers
CROSS JOIN tier_modifiers
ON CONFLICT (shot_type, surface, player_tier)
DO UPDATE SET
    mean_landing_x = EXCLUDED.mean_landing_x,
    mean_landing_y = EXCLUDED.mean_landing_y,
    std_dev_x = EXCLUDED.std_dev_x,
    std_dev_y = EXCLUDED.std_dev_y,
    mean_speed_kmh = EXCLUDED.mean_speed_kmh,
    speed_std_dev = EXCLUDED.speed_std_dev,
    mean_spin_rpm = EXCLUDED.mean_spin_rpm,
    spin_std_dev = EXCLUDED.spin_std_dev,
    mean_arc_height = EXCLUDED.mean_arc_height,
    arc_std_dev = EXCLUDED.arc_std_dev,
    sample_size = EXCLUDED.sample_size,
    active = true,
    updated_at = CURRENT_TIMESTAMP;
