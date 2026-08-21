-- Migration: harden realtime publication + replica identity for every table
-- the app subscribes to via postgresChangeFlow.
-- Created: 2026-08-21
--
-- Audit against the deployed DB on 2026-08-21 found:
--   posts            in publication ✓, replica full   ✓
--   group_messages   in publication ✓, replica full   ✓
--   notifications    in publication ✓, replica default ✗ (bug: filter drops)
--   group_members    in publication ✗ (bug: observeUserGroups dead),
--                    replica full ✓
--
-- Fixes and hardening below. Wrapped in DO blocks / idempotent statements so
-- re-running (or running against a fresh env where the dashboard toggles are
-- absent) is safe. Modelled on 20260708000003_enable_realtime_posts.sql.
--
-- Why REPLICA IDENTITY FULL on every subscribed table:
-- postgres_changes with a filter on a non-PK column (`user_id`, `group_id`,
-- `neighborhood_id`) needs the whole old and new row in the WAL so the
-- Realtime service can evaluate the filter server-side. Default replica
-- identity carries only the PK, so filtered UPDATE/DELETE events are dropped
-- before they reach the client.

-- ── posts ────────────────────────────────────────────────────────────────
-- Already correct today; the idempotent add is defence against another
-- dashboard toggle drop like the one 20260708000003 was written to recover.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime' AND tablename = 'posts'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE posts;
  END IF;
END $$;
ALTER TABLE posts REPLICA IDENTITY FULL;

-- ── group_messages ───────────────────────────────────────────────────────
-- 20260708000002_group_chat.sql added it non-idempotently; this pass makes
-- re-runs safe and reasserts REPLICA IDENTITY FULL.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime' AND tablename = 'group_messages'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE group_messages;
  END IF;
END $$;
ALTER TABLE group_messages REPLICA IDENTITY FULL;

-- ── group_members ────────────────────────────────────────────────────────
-- SupabaseGroupRepository.observeUserGroups filters on user_id. Without the
-- table in the publication no join/leave events reach the client, so a fresh
-- membership never appears until the app is relaunched.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime' AND tablename = 'group_members'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE group_members;
  END IF;
END $$;
ALTER TABLE group_members REPLICA IDENTITY FULL;

-- ── notifications ────────────────────────────────────────────────────────
-- SupabaseNotificationRepository filters on user_id, which is not the PK. With
-- the default replica identity the WAL carries only `id` on UPDATE/DELETE so
-- the filter cannot be evaluated and the event is dropped before delivery
-- (mark-as-read, delete). INSERTs already worked because INSERT payloads
-- carry the whole new row regardless of replica identity.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime' AND tablename = 'notifications'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE notifications;
  END IF;
END $$;
ALTER TABLE notifications REPLICA IDENTITY FULL;
