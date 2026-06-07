-- Migration: core schema — neighborhoods, users, user_stats
-- Created: 2026-06-06

-- ── neighborhoods ──────────────────────────────────────────────────────────
CREATE TABLE neighborhoods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    city TEXT NOT NULL DEFAULT 'Indianapolis',
    slug TEXT NOT NULL UNIQUE
);

ALTER TABLE neighborhoods ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can read neighborhoods"
    ON neighborhoods FOR SELECT
    USING (true);

-- ── users ──────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name TEXT,
    full_name TEXT,
    avatar_url TEXT,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    neighborhood_id UUID REFERENCES neighborhoods(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE users ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Authenticated users can read profiles"
    ON users FOR SELECT
    TO authenticated
    USING (true);

CREATE POLICY "Users can insert own row"
    ON users FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = id);

CREATE POLICY "Users can update own row"
    ON users FOR UPDATE
    TO authenticated
    USING (auth.uid() = id);

-- ── user_stats ─────────────────────────────────────────────────────────────
-- Written by DB triggers only — no INSERT/UPDATE/DELETE policies for clients.
-- The trigger function (below) uses SECURITY DEFINER to bypass RLS on writes.
CREATE TABLE user_stats (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    hosted_count INTEGER NOT NULL DEFAULT 0,
    attended_count INTEGER NOT NULL DEFAULT 0,
    rsvp_count INTEGER NOT NULL DEFAULT 0,
    no_show_count INTEGER NOT NULL DEFAULT 0,
    response_rate NUMERIC(5,4) NOT NULL DEFAULT 0
);

ALTER TABLE user_stats ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Authenticated users can read user stats"
    ON user_stats FOR SELECT
    TO authenticated
    USING (true);

-- ── trigger function: update_user_stats_on_rsvp ────────────────────────────
-- The CREATE TRIGGER ... ON rsvps statement is intentionally NOT in this
-- migration — rsvps doesn't exist yet in dependency order. The trigger will
-- be attached in the rsvps migration via:
--   CREATE TRIGGER trg_user_stats_on_rsvp
--     AFTER INSERT OR UPDATE ON rsvps
--     FOR EACH ROW EXECUTE FUNCTION update_user_stats_on_rsvp();
--
-- Expected rsvps columns: user_id UUID, status TEXT ('confirmed' | 'cancelled').
CREATE OR REPLACE FUNCTION update_user_stats_on_rsvp()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    affected_user UUID;
BEGIN
    affected_user := COALESCE(NEW.user_id, OLD.user_id);

    INSERT INTO user_stats (user_id) VALUES (affected_user)
    ON CONFLICT (user_id) DO NOTHING;

    UPDATE user_stats SET
        rsvp_count = (
            SELECT COUNT(*) FROM rsvps
            WHERE user_id = affected_user AND status = 'confirmed'
        )
    WHERE user_id = affected_user;

    RETURN NEW;
END;
$$;
