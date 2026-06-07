-- Migration: activity schema — groups, group_members, group_invites, posts,
--            post_images, post_tags, rsvps, follows, user_interests
-- Created: 2026-06-06

-- ── enum types ─────────────────────────────────────────────────────────────
CREATE TYPE group_role AS ENUM ('admin', 'member');
CREATE TYPE post_tag AS ENUM ('hike', 'run', 'picnic', 'sport', 'walk', 'explore', 'other');
CREATE TYPE rsvp_status AS ENUM ('confirmed', 'cancelled');
-- user_interest matches the Kotlin Interest enum names verbatim (uppercase).
-- Grouped here in the same order as Interest.kt for easy cross-reference.
CREATE TYPE user_interest AS ENUM (
    -- Outdoor & Active
    'RUNNING', 'HIKING', 'CYCLING', 'WALKING', 'YOGA', 'SPORTS', 'SWIMMING', 'SKATING',
    -- Social & Casual
    'PICNICS', 'BONFIRES', 'GAME_NIGHTS', 'COFFEE', 'FOOD', 'VOLUNTEERING',
    -- Creative & Enrichment
    'PHOTOGRAPHY', 'DRAWING', 'READING', 'MUSIC', 'CRAFTS', 'WRITING',
    -- Exploration & Nature
    'EXPLORING', 'BIRDWATCHING', 'GARDENING', 'STARGAZING', 'NATURE',
    -- Pets
    'DOG_WALKS', 'PET_FRIENDLY'
);

-- ── groups ─────────────────────────────────────────────────────────────────
CREATE TABLE groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    description TEXT,
    cover_url TEXT,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_open BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
ALTER TABLE groups ENABLE ROW LEVEL SECURITY;

-- ── group_members ──────────────────────────────────────────────────────────
CREATE TABLE group_members (
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role group_role NOT NULL DEFAULT 'member',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (group_id, user_id)
);
CREATE INDEX idx_group_members_user_id ON group_members(user_id);
ALTER TABLE group_members ENABLE ROW LEVEL SECURITY;

-- ── group_invites ──────────────────────────────────────────────────────────
CREATE TABLE group_invites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    invited_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '7 days'),
    used_at TIMESTAMPTZ
);
ALTER TABLE group_invites ENABLE ROW LEVEL SECURITY;

-- ── posts ──────────────────────────────────────────────────────────────────
-- NOTE: posts.neighborhood_id uses CASCADE per the "all FKs CASCADE unless noted"
-- rule. Reference-data CASCADE is destructive in principle (deleting a
-- neighborhood would delete every post in it) — revisit if neighborhoods ever
-- become deletable. Today neighborhoods are seeded reference rows and never
-- deleted by the app, so this is safe in practice.
CREATE TABLE posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    neighborhood_id UUID NOT NULL REFERENCES neighborhoods(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    location GEOGRAPHY(POINT, 4326) NOT NULL,
    address TEXT,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ,
    max_attendees INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_posts_location ON posts USING GIST(location);
CREATE INDEX idx_posts_neighborhood_starts_at ON posts(neighborhood_id, starts_at);
CREATE INDEX idx_posts_group_id ON posts(group_id) WHERE group_id IS NOT NULL;
CREATE INDEX idx_posts_user_id ON posts(user_id);
ALTER TABLE posts ENABLE ROW LEVEL SECURITY;

-- ── post_images ────────────────────────────────────────────────────────────
CREATE TABLE post_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    storage_url TEXT NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_post_images_post_id ON post_images(post_id);
ALTER TABLE post_images ENABLE ROW LEVEL SECURITY;

-- ── post_tags ──────────────────────────────────────────────────────────────
CREATE TABLE post_tags (
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    tag post_tag NOT NULL,
    PRIMARY KEY (post_id, tag)
);
ALTER TABLE post_tags ENABLE ROW LEVEL SECURITY;

-- ── rsvps ──────────────────────────────────────────────────────────────────
CREATE TABLE rsvps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status rsvp_status NOT NULL DEFAULT 'confirmed',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (post_id, user_id)
);
CREATE INDEX idx_rsvps_user_id ON rsvps(user_id);
ALTER TABLE rsvps ENABLE ROW LEVEL SECURITY;

-- ── follows ────────────────────────────────────────────────────────────────
CREATE TABLE follows (
    follower_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (follower_id, following_id),
    CHECK (follower_id <> following_id)
);
CREATE INDEX idx_follows_following_id ON follows(following_id);
ALTER TABLE follows ENABLE ROW LEVEL SECURITY;

-- ── user_interests ─────────────────────────────────────────────────────────
CREATE TABLE user_interests (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    interest user_interest NOT NULL,
    PRIMARY KEY (user_id, interest)
);
ALTER TABLE user_interests ENABLE ROW LEVEL SECURITY;

-- ── helper functions (SECURITY DEFINER to break RLS recursion) ─────────────
-- Plain RLS policies that query the same table they protect can recurse on
-- group_members; SECURITY DEFINER functions bypass RLS for their internal
-- query and return only a boolean, so no data leaks.

CREATE OR REPLACE FUNCTION is_group_member(p_group_id UUID, p_user_id UUID)
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
STABLE
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM group_members
        WHERE group_id = p_group_id AND user_id = p_user_id
    );
$$;

CREATE OR REPLACE FUNCTION is_group_admin(p_group_id UUID, p_user_id UUID)
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
STABLE
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM group_members
        WHERE group_id = p_group_id
        AND user_id = p_user_id
        AND role = 'admin'
    );
$$;

CREATE OR REPLACE FUNCTION can_read_post(p_post_id UUID, p_user_id UUID)
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
STABLE
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM posts p
        WHERE p.id = p_post_id
        AND (p.group_id IS NULL OR is_group_member(p.group_id, p_user_id))
    );
$$;

CREATE OR REPLACE FUNCTION is_post_owner(p_post_id UUID, p_user_id UUID)
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
STABLE
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM posts WHERE id = p_post_id AND user_id = p_user_id
    );
$$;

-- ── RLS policies ───────────────────────────────────────────────────────────

-- groups
CREATE POLICY "Open or member can read group"
    ON groups FOR SELECT
    TO authenticated
    USING (is_open = TRUE OR is_group_member(id, auth.uid()));

CREATE POLICY "Authenticated users create own groups"
    ON groups FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = created_by);

CREATE POLICY "Admins update group"
    ON groups FOR UPDATE
    TO authenticated
    USING (is_group_admin(id, auth.uid()));

CREATE POLICY "Admins delete group"
    ON groups FOR DELETE
    TO authenticated
    USING (is_group_admin(id, auth.uid()));

-- group_members
CREATE POLICY "Members read members of their groups"
    ON group_members FOR SELECT
    TO authenticated
    USING (is_group_member(group_id, auth.uid()));

CREATE POLICY "Admins add members, or self-join open groups"
    ON group_members FOR INSERT
    TO authenticated
    WITH CHECK (
        is_group_admin(group_id, auth.uid())
        OR (
            user_id = auth.uid()
            AND EXISTS (SELECT 1 FROM groups WHERE id = group_id AND is_open = TRUE)
        )
    );

CREATE POLICY "Admins update member roles"
    ON group_members FOR UPDATE
    TO authenticated
    USING (is_group_admin(group_id, auth.uid()));

CREATE POLICY "Self leave or admin remove"
    ON group_members FOR DELETE
    TO authenticated
    USING (user_id = auth.uid() OR is_group_admin(group_id, auth.uid()));

-- group_invites — inviter and admins only. Redemption is via a separate
-- SECURITY DEFINER function (not in this migration) that looks up by token.
CREATE POLICY "Inviter or admin reads invites"
    ON group_invites FOR SELECT
    TO authenticated
    USING (invited_by = auth.uid() OR is_group_admin(group_id, auth.uid()));

CREATE POLICY "Admins create invites"
    ON group_invites FOR INSERT
    TO authenticated
    WITH CHECK (
        is_group_admin(group_id, auth.uid())
        AND invited_by = auth.uid()
    );

CREATE POLICY "Inviter or admin revokes invite"
    ON group_invites FOR DELETE
    TO authenticated
    USING (invited_by = auth.uid() OR is_group_admin(group_id, auth.uid()));

-- posts
CREATE POLICY "Public posts or group members can read posts"
    ON posts FOR SELECT
    TO authenticated
    USING (group_id IS NULL OR is_group_member(group_id, auth.uid()));

CREATE POLICY "Users insert own posts"
    ON posts FOR INSERT
    TO authenticated
    WITH CHECK (
        user_id = auth.uid()
        AND (group_id IS NULL OR is_group_member(group_id, auth.uid()))
    );

CREATE POLICY "Users update own posts"
    ON posts FOR UPDATE
    TO authenticated
    USING (user_id = auth.uid());

CREATE POLICY "Users delete own posts"
    ON posts FOR DELETE
    TO authenticated
    USING (user_id = auth.uid());

-- post_images — visibility follows the parent post; writes by post owner
CREATE POLICY "Post images readable if post readable"
    ON post_images FOR SELECT
    TO authenticated
    USING (can_read_post(post_id, auth.uid()));

CREATE POLICY "Post owner inserts images"
    ON post_images FOR INSERT
    TO authenticated
    WITH CHECK (is_post_owner(post_id, auth.uid()));

CREATE POLICY "Post owner updates images"
    ON post_images FOR UPDATE
    TO authenticated
    USING (is_post_owner(post_id, auth.uid()));

CREATE POLICY "Post owner deletes images"
    ON post_images FOR DELETE
    TO authenticated
    USING (is_post_owner(post_id, auth.uid()));

-- post_tags — same model as post_images
CREATE POLICY "Post tags readable if post readable"
    ON post_tags FOR SELECT
    TO authenticated
    USING (can_read_post(post_id, auth.uid()));

CREATE POLICY "Post owner inserts tags"
    ON post_tags FOR INSERT
    TO authenticated
    WITH CHECK (is_post_owner(post_id, auth.uid()));

CREATE POLICY "Post owner deletes tags"
    ON post_tags FOR DELETE
    TO authenticated
    USING (is_post_owner(post_id, auth.uid()));

-- rsvps
CREATE POLICY "RSVPs readable if post readable"
    ON rsvps FOR SELECT
    TO authenticated
    USING (can_read_post(post_id, auth.uid()));

CREATE POLICY "Users RSVP for themselves on readable posts"
    ON rsvps FOR INSERT
    TO authenticated
    WITH CHECK (
        user_id = auth.uid()
        AND can_read_post(post_id, auth.uid())
    );

CREATE POLICY "Users update own RSVPs"
    ON rsvps FOR UPDATE
    TO authenticated
    USING (user_id = auth.uid());

CREATE POLICY "Users delete own RSVPs"
    ON rsvps FOR DELETE
    TO authenticated
    USING (user_id = auth.uid());

-- follows
CREATE POLICY "Authenticated read follow graph"
    ON follows FOR SELECT
    TO authenticated
    USING (true);

CREATE POLICY "Users follow as themselves"
    ON follows FOR INSERT
    TO authenticated
    WITH CHECK (follower_id = auth.uid());

CREATE POLICY "Users unfollow as themselves"
    ON follows FOR DELETE
    TO authenticated
    USING (follower_id = auth.uid());

-- user_interests
CREATE POLICY "Authenticated read user interests"
    ON user_interests FOR SELECT
    TO authenticated
    USING (true);

CREATE POLICY "Users manage own interests — insert"
    ON user_interests FOR INSERT
    TO authenticated
    WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users manage own interests — delete"
    ON user_interests FOR DELETE
    TO authenticated
    USING (user_id = auth.uid());

-- ── triggers ───────────────────────────────────────────────────────────────

-- Auto-add the group creator as an admin member. Without this, the "admins
-- update / delete group" policies lock the creator out of their own brand-new
-- group until someone else makes them an admin.
CREATE OR REPLACE FUNCTION add_creator_as_group_admin()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO group_members (group_id, user_id, role)
    VALUES (NEW.id, NEW.created_by, 'admin');
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_groups_add_creator_admin
    AFTER INSERT ON groups
    FOR EACH ROW EXECUTE FUNCTION add_creator_as_group_admin();

-- Attach the user_stats updater defined in the previous (core_schema) migration
-- to the rsvps table. INSERT or UPDATE both fire it so status changes recount
-- rsvp_count.
CREATE TRIGGER trg_user_stats_on_rsvp
    AFTER INSERT OR UPDATE ON rsvps
    FOR EACH ROW EXECUTE FUNCTION update_user_stats_on_rsvp();
