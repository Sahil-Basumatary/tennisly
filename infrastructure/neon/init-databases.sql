-- Create the per-service databases on the external Postgres project (Neon/Supabase).
-- Run once with psql before the first Render deploy; Flyway migrates schemas but
-- never creates the databases themselves.
--
--   psql "postgresql://USER:PASSWORD@HOST/neondb?sslmode=require" -f infrastructure/neon/init-databases.sql

SELECT 'CREATE DATABASE tennisly_tennis_data'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'tennisly_tennis_data')\gexec

SELECT 'CREATE DATABASE tennisly_matches'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'tennisly_matches')\gexec

SELECT 'CREATE DATABASE tennisly_auth'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'tennisly_auth')\gexec

SELECT 'CREATE DATABASE tennisly_users'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'tennisly_users')\gexec
