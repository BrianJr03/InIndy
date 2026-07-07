-- Fix: posts.rsvp_count never moved when someone RSVP'd to a post they don't own.
--
-- Root cause: increment_rsvp_count / decrement_rsvp_count were LANGUAGE sql with
-- NO SECURITY DEFINER, so they ran as the *caller* (SECURITY INVOKER). The posts
-- UPDATE RLS policy ("Users update own posts") only lets a user update posts where
-- user_id = auth.uid(). So when user B RSVP'd to user A's post, the function's
-- `UPDATE posts SET rsvp_count = ...` matched zero rows — RLS silently filtered it,
-- no error was raised. The rsvps row still inserted (so the attendee bubble showed),
-- but the counter stayed at 0. Because the posts row never actually updated, the
-- realtime posts-change subscription also never fired, so the count never synced to
-- other devices either.
--
-- Fix: mirror the sync_group_member_count pattern — SECURITY DEFINER so the function
-- bypasses the posts UPDATE RLS, and recount authoritatively from rsvps instead of a
-- blind +1/-1 (which can drift on the stale-cache multi-device race the rsvp repo
-- already guards against). Both function names are kept so no client change is needed;
-- the client keeps calling them after INSERT/DELETE and each call is now an idempotent
-- recount.

CREATE OR REPLACE FUNCTION increment_rsvp_count(post_id text)
RETURNS void
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
    UPDATE posts SET rsvp_count = (
        SELECT COUNT(*) FROM rsvps
        WHERE rsvps.post_id = posts.id AND rsvps.status = 'confirmed'
    )
    WHERE id::text = post_id;
$$;

CREATE OR REPLACE FUNCTION decrement_rsvp_count(post_id text)
RETURNS void
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
    UPDATE posts SET rsvp_count = (
        SELECT COUNT(*) FROM rsvps
        WHERE rsvps.post_id = posts.id AND rsvps.status = 'confirmed'
    )
    WHERE id::text = post_id;
$$;

GRANT EXECUTE ON FUNCTION increment_rsvp_count(text) TO authenticated;
GRANT EXECUTE ON FUNCTION decrement_rsvp_count(text) TO authenticated;

-- Backfill: correct every post whose count got stuck under the old behaviour.
UPDATE posts SET rsvp_count = (
    SELECT COUNT(*) FROM rsvps
    WHERE rsvps.post_id = posts.id AND rsvps.status = 'confirmed'
);
