-- Tracks the last reminder sent per (user, event) AND which start time it was
-- for. When posts.starts_at changes, reminded_for_starts_at no longer matches
-- and the reminder re-arms.
CREATE TABLE IF NOT EXISTS rsvp_reminders_sent (
    user_id                UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id                UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    reminded_for_starts_at TIMESTAMPTZ NOT NULL,
    sent_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, post_id)
);

-- RLS on, no policies: clients (anon/authenticated) get zero access. Only
-- enqueue_due_rsvp_reminders() touches this table, and it's SECURITY DEFINER
-- (owned by postgres), so it bypasses RLS and still works.
ALTER TABLE rsvp_reminders_sent ENABLE ROW LEVEL SECURITY;

CREATE OR REPLACE FUNCTION enqueue_due_rsvp_reminders()
RETURNS integer
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    inserted integer;
    tz CONSTANT text := 'America/Indiana/Indianapolis';
BEGIN
    WITH scheduled AS (
        SELECT
            r.user_id,
            r.post_id,
            p.starts_at,
            CASE COALESCE(ns.rsvp_reminder, 'day_of')
                WHEN 'hour_before' THEN p.starts_at - interval '1 hour'
                WHEN 'day_of' THEN LEAST(
                    ((date_trunc('day', p.starts_at AT TIME ZONE tz) + interval '9 hours') AT TIME ZONE tz),
                    p.starts_at - interval '1 hour'
                )
            END AS reminder_at
        FROM rsvps r
        JOIN posts p ON p.id = r.post_id
        LEFT JOIN notification_settings ns ON ns.user_id = r.user_id
        WHERE r.status = 'confirmed'
          AND p.starts_at > now()
          AND COALESCE(ns.rsvp_reminder, 'day_of') <> 'none'
    ),
    due AS (
        SELECT s.user_id, s.post_id, s.starts_at
        FROM scheduled s
        LEFT JOIN rsvp_reminders_sent snt
               ON snt.user_id = s.user_id AND snt.post_id = s.post_id
        WHERE s.reminder_at <= now()
          -- Fire if never reminded, OR the event time changed since we last did.
          AND (snt.post_id IS NULL OR snt.reminded_for_starts_at <> s.starts_at)
    ),
    ins AS (
        INSERT INTO notifications (user_id, type, actor_id, group_id, post_id)
        SELECT user_id, 'rsvp_reminder', NULL, NULL, post_id FROM due
        RETURNING 1
    )
    -- Record what we just sent (and for which start time) so we don't repeat it
    -- until the event moves again.
    INSERT INTO rsvp_reminders_sent (user_id, post_id, reminded_for_starts_at, sent_at)
    SELECT user_id, post_id, starts_at, now() FROM due
    ON CONFLICT (user_id, post_id)
    DO UPDATE SET reminded_for_starts_at = EXCLUDED.reminded_for_starts_at,
                  sent_at = EXCLUDED.sent_at;

    GET DIAGNOSTICS inserted = ROW_COUNT;
    RETURN inserted;
END;
$$;

-- Only the cron job (owner context) runs this; end users never call it.
REVOKE ALL ON FUNCTION enqueue_due_rsvp_reminders() FROM public;
