-- Atomic increment/decrement helpers for posts.rsvp_count. Called from the
-- client after an rsvps INSERT/DELETE so the count stays accurate without a
-- client-side read-modify-write race.
--
-- post_id is declared as text to match how supabase-kt serialises the param;
-- Postgres casts it back to uuid on the WHERE clause.

CREATE OR REPLACE FUNCTION increment_rsvp_count(post_id text)
RETURNS void
LANGUAGE sql
AS $$
    UPDATE posts SET rsvp_count = rsvp_count + 1 WHERE id::text = post_id;
$$;

CREATE OR REPLACE FUNCTION decrement_rsvp_count(post_id text)
RETURNS void
LANGUAGE sql
AS $$
    UPDATE posts SET rsvp_count = GREATEST(rsvp_count - 1, 0) WHERE id::text = post_id;
$$;

GRANT EXECUTE ON FUNCTION increment_rsvp_count(text) TO authenticated;
GRANT EXECUTE ON FUNCTION decrement_rsvp_count(text) TO authenticated;
