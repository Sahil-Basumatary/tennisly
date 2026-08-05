-- Carpet is no longer a supported tour surface; drop any rows seeded under V2.
DELETE FROM shot_distributions WHERE surface = 'CARPET';
