-- Migration: change neighborhood id columns from UUID to text
-- Created: 2026-06-09
--
-- Switches neighborhoods.id (and the columns referencing it) to text so
-- stable, human-readable slugs can be used as the primary key instead of
-- generated UUIDs. Foreign key constraints have to be dropped before the
-- referenced/referencing column types can change, then re-added afterwards.

BEGIN;

-- ── users.neighborhood_id ─────────────────────────────────────────────────
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_neighborhood_id_fkey;
ALTER TABLE users ALTER COLUMN neighborhood_id TYPE text;

-- ── posts.neighborhood_id ─────────────────────────────────────────────────
ALTER TABLE posts DROP CONSTRAINT IF EXISTS posts_neighborhood_id_fkey;
ALTER TABLE posts ALTER COLUMN neighborhood_id TYPE text;

-- ── neighborhoods.id (last — both FKs are now gone) ───────────────────────
ALTER TABLE neighborhoods ALTER COLUMN id TYPE text;

-- ── re-add FK constraints ─────────────────────────────────────────────────
ALTER TABLE users ADD CONSTRAINT users_neighborhood_id_fkey
    FOREIGN KEY (neighborhood_id) REFERENCES neighborhoods(id) ON DELETE SET NULL;

ALTER TABLE posts ADD CONSTRAINT posts_neighborhood_id_fkey
    FOREIGN KEY (neighborhood_id) REFERENCES neighborhoods(id) ON DELETE CASCADE;

COMMIT;
