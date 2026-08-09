-- Backfill for the timezone fix. The client used to serialize picked local
-- wall-clock times labelled as UTC — e.g. "6:00 PM Indianapolis on 2026-07-26"
-- was stored as 2026-07-26 18:00+00 (which is 2:00 PM Indianapolis) instead of
-- 2026-07-26 22:00+00. Every existing posts.starts_at / ends_at is off by the
-- local offset that was in effect on that row's date.
--
-- The fix: reinterpret the stored UTC wall clock as if it were Indianapolis
-- local wall clock, then let Postgres reconvert to the correct real UTC
-- instant. Using the named zone (not a fixed offset) means EDT rows shift 4h
-- and EST rows shift 5h automatically.
--
-- rsvp_reminders_sent.reminded_for_starts_at mirrors posts.starts_at and is
-- compared for equality in enqueue_due_rsvp_reminders — shift both by the
-- same amount so the "event moved, re-arm" branch does not fire spuriously
-- for reminders that already went out.

UPDATE posts
SET
    starts_at = ((starts_at AT TIME ZONE 'UTC')::timestamp
                 AT TIME ZONE 'America/Indiana/Indianapolis'),
    ends_at   = CASE
                    WHEN ends_at IS NULL THEN NULL
                    ELSE ((ends_at AT TIME ZONE 'UTC')::timestamp
                          AT TIME ZONE 'America/Indiana/Indianapolis')
                END;

UPDATE rsvp_reminders_sent
SET reminded_for_starts_at = ((reminded_for_starts_at AT TIME ZONE 'UTC')::timestamp
                              AT TIME ZONE 'America/Indiana/Indianapolis');
