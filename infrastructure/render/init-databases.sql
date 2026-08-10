-- Create app databases on the Render Postgres instance after Blueprint provision.
-- Run once via Render shell / psql against the tennisly database host.

SELECT 'CREATE DATABASE tennisly_tennis_data'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'tennisly_tennis_data')\gexec

SELECT 'CREATE DATABASE tennisly_matches'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'tennisly_matches')\gexec
