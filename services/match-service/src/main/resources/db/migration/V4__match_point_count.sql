ALTER TABLE matches ADD COLUMN point_count INTEGER NOT NULL DEFAULT 0;

UPDATE matches m
SET point_count = COALESCE(
    (SELECT COUNT(*) FROM match_points p WHERE p.match_id = m.id),
    0);
