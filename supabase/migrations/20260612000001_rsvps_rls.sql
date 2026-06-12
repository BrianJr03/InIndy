-- ── posts.rsvp_count column ───────────────────────────────────────────────
-- The Post domain model carries rsvpCount but the underlying posts table did
-- not have it. Add the column and backfill from existing confirmed rsvps.

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS rsvp_count INTEGER NOT NULL DEFAULT 0;

UPDATE posts SET rsvp_count = (
    SELECT COUNT(*) FROM rsvps
    WHERE rsvps.post_id = posts.id AND rsvps.status = 'confirmed'
);

-- ── rsvps row level security ──────────────────────────────────────────────
-- RLS was already ENABLED on the rsvps table by the activity_schema migration
-- but no policies were attached, which means all reads/writes were blocked
-- for authenticated users. Add per-user policies so a signed-in user can
-- read, insert, and delete only their own rsvps.

ALTER TABLE rsvps ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can read their own rsvps" ON rsvps;
CREATE POLICY "Users can read their own rsvps"
ON rsvps FOR SELECT TO authenticated
USING (user_id::text = auth.uid()::text);

DROP POLICY IF EXISTS "Users can insert their own rsvps" ON rsvps;
CREATE POLICY "Users can insert their own rsvps"
ON rsvps FOR INSERT TO authenticated
WITH CHECK (user_id::text = auth.uid()::text);

DROP POLICY IF EXISTS "Users can delete their own rsvps" ON rsvps;
CREATE POLICY "Users can delete their own rsvps"
ON rsvps FOR DELETE TO authenticated
USING (user_id::text = auth.uid()::text);
