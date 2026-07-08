-- In-app notifications for group posts, plus per-group mute.

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,   -- recipient
    type TEXT NOT NULL,                                             -- 'group_post'
    actor_id UUID REFERENCES users(id) ON DELETE CASCADE,           -- who triggered it
    group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    post_id UUID REFERENCES posts(id) ON DELETE CASCADE,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_unread ON notifications(user_id, read, created_at DESC);

ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

-- Recipients read / update (mark read) / delete (dismiss) only their own.
-- No INSERT policy: only the service role (edge function) inserts, bypassing RLS.
CREATE POLICY "Users read own notifications"   ON notifications FOR SELECT TO authenticated USING (user_id = auth.uid());
CREATE POLICY "Users update own notifications" ON notifications FOR UPDATE TO authenticated USING (user_id = auth.uid());
CREATE POLICY "Users delete own notifications" ON notifications FOR DELETE TO authenticated USING (user_id = auth.uid());

-- Realtime so the bell updates live. (If this errors, add the table via
-- dashboard → Database → Replication instead.)
ALTER PUBLICATION supabase_realtime ADD TABLE notifications;

-- Per-group mute flag on membership.
ALTER TABLE group_members ADD COLUMN notifications_muted BOOLEAN NOT NULL DEFAULT FALSE;

-- Member toggles their OWN mute only (group_members UPDATE is otherwise admin-only,
-- and we must not let members change their own role). SECURITY DEFINER + fixed
-- column keeps it safe.
CREATE OR REPLACE FUNCTION set_group_notifications_muted(p_group_id UUID, p_muted BOOLEAN)
RETURNS void LANGUAGE sql SECURITY DEFINER SET search_path = public AS $$
    UPDATE group_members SET notifications_muted = p_muted
    WHERE group_id = p_group_id AND user_id = auth.uid();
$$;
GRANT EXECUTE ON FUNCTION set_group_notifications_muted(UUID, BOOLEAN) TO authenticated;

-- Fan-out helper called by moderate-post on approval. Idempotent: NOT EXISTS guard
-- means re-approving an edited post won't create duplicate notifications. Skips the
-- author and anyone who muted the group.
CREATE OR REPLACE FUNCTION notify_group_post(p_post_id UUID, p_group_id UUID, p_actor_id UUID)
RETURNS void LANGUAGE sql SECURITY DEFINER SET search_path = public AS $$
    INSERT INTO notifications (user_id, type, actor_id, group_id, post_id)
    SELECT gm.user_id, 'group_post', p_actor_id, p_group_id, p_post_id
    FROM group_members gm
    WHERE gm.group_id = p_group_id
      AND gm.user_id <> p_actor_id
      AND gm.notifications_muted = false
      AND NOT EXISTS (
          SELECT 1 FROM notifications n
          WHERE n.post_id = p_post_id AND n.user_id = gm.user_id
      );
$$;
GRANT EXECUTE ON FUNCTION notify_group_post(UUID, UUID, UUID) TO service_role;
