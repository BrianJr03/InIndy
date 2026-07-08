-- One chat per group. Messages scope by group_id; RLS restricts to members.

CREATE TABLE group_messages (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id   UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    sender_id  UUID NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    body       TEXT NOT NULL,
    redacted   BOOLEAN NOT NULL DEFAULT FALSE,  -- set TRUE by async moderation if flagged
    deleted    BOOLEAN NOT NULL DEFAULT FALSE,  -- soft delete by sender
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_group_messages_group_created ON group_messages(group_id, created_at DESC);

ALTER TABLE group_messages ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Members read group messages"
    ON group_messages FOR SELECT TO authenticated
    USING (is_group_member(group_id, auth.uid()));

CREATE POLICY "Members send group messages"
    ON group_messages FOR INSERT TO authenticated
    WITH CHECK (is_group_member(group_id, auth.uid()) AND sender_id = auth.uid());

CREATE POLICY "Sender updates own messages"
    ON group_messages FOR UPDATE TO authenticated
    USING (sender_id = auth.uid());
-- Redaction is done by the service role (edge function), which bypasses RLS.

-- REPLICA IDENTITY FULL so realtime UPDATE/DELETE payloads carry group_id for filtering.
ALTER TABLE group_messages REPLICA IDENTITY FULL;
ALTER PUBLICATION supabase_realtime ADD TABLE group_messages;  -- or enable via dashboard → Replication

-- Per-member last-read for unread indicators.
ALTER TABLE group_members ADD COLUMN last_read_chat_at TIMESTAMPTZ;

-- Member marks their own chat read (group_members UPDATE is admin-only otherwise).
CREATE OR REPLACE FUNCTION mark_group_chat_read(p_group_id UUID)
RETURNS void LANGUAGE sql SECURITY DEFINER SET search_path = public AS $$
    UPDATE group_members SET last_read_chat_at = NOW()
    WHERE group_id = p_group_id AND user_id = auth.uid();
$$;
GRANT EXECUTE ON FUNCTION mark_group_chat_read(UUID) TO authenticated;

-- Per-group unread counts for the current user (one round trip for all their groups).
CREATE OR REPLACE FUNCTION group_chat_unread_counts()
RETURNS TABLE(group_id UUID, unread_count BIGINT)
LANGUAGE sql SECURITY DEFINER SET search_path = public AS $$
    SELECT gm.group_id, COUNT(msg.id)
    FROM group_members gm
    LEFT JOIN group_messages msg
      ON  msg.group_id = gm.group_id
      AND msg.sender_id <> gm.user_id
      AND msg.deleted = FALSE
      AND msg.redacted = FALSE
      AND (gm.last_read_chat_at IS NULL OR msg.created_at > gm.last_read_chat_at)
    WHERE gm.user_id = auth.uid()
    GROUP BY gm.group_id;
$$;
GRANT EXECUTE ON FUNCTION group_chat_unread_counts() TO authenticated;
