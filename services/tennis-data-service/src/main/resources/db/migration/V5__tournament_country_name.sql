-- BallDontLie reports tournament location as free text ("Melbourne, Australia"), so the trailing
-- segment is a country name rather than an ISO alpha-3 code. Store what the provider actually sends.
ALTER TABLE tournaments RENAME COLUMN country_code TO country;

ALTER TABLE tournaments ALTER COLUMN country TYPE VARCHAR(100);
