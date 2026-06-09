-- Migration: replace post_tag enum with user_interest on post_tags.tag
-- Created: 2026-06-07
--
-- Consolidates post tags onto the single user_interest enum so posts and
-- user interest profiles share the same vocabulary. Postgres cannot change
-- a column's enum type in place, so we swap via a transient column.
--
-- The existing post_tag enum values are lowercase
-- ('hike','run','picnic','sport','walk','explore','other') — the CASE below
-- maps each to its closest user_interest equivalent. 'other' (and any
-- unexpected value) falls back to EXPLORING.

BEGIN;

-- 1. Add new column with the target enum type.
ALTER TABLE post_tags ADD COLUMN tag_new user_interest;

-- 2. Backfill from existing post_tag values.
UPDATE post_tags SET tag_new = CASE tag::text
    WHEN 'hike'    THEN 'HIKING'::user_interest
    WHEN 'run'     THEN 'RUNNING'::user_interest
    WHEN 'picnic'  THEN 'PICNICS'::user_interest
    WHEN 'sport'   THEN 'SPORTS'::user_interest
    WHEN 'walk'    THEN 'WALKING'::user_interest
    WHEN 'explore' THEN 'EXPLORING'::user_interest
    WHEN 'other'   THEN 'EXPLORING'::user_interest
    ELSE 'EXPLORING'::user_interest
END;

-- 3. Drop the old primary key — it depends on the old tag column.
ALTER TABLE post_tags DROP CONSTRAINT post_tags_pkey;

-- 4. Drop the old column and rename the new one into place.
ALTER TABLE post_tags DROP COLUMN tag;
ALTER TABLE post_tags RENAME COLUMN tag_new TO tag;

-- 5. Re-apply NOT NULL on the renamed column.
ALTER TABLE post_tags ALTER COLUMN tag SET NOT NULL;

-- 6. Re-apply the (post_id, tag) primary key.
ALTER TABLE post_tags ADD CONSTRAINT post_tags_pkey PRIMARY KEY (post_id, tag);

-- 7. Drop the now-unused enum type.
DROP TYPE IF EXISTS post_tag;

-- RLS policies on post_tags reference only post_id (via can_read_post /
-- is_post_owner helpers) and do not mention the tag column type, so they
-- remain valid through the swap — no recreation needed.

COMMIT;
