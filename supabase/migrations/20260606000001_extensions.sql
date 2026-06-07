-- Migration: enable required Postgres extensions
-- Created: 2026-06-06

-- PostGIS — required before any GEOGRAPHY(POINT, 4326) columns are created
CREATE EXTENSION IF NOT EXISTS postgis;

-- uuid-ossp — required for gen_random_uuid() used in primary keys and invite tokens
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
