-- Migration: enable realtime for posts table
-- Created: 2026-07-08
--
-- postgresChangeFlow subscriptions on `posts` (feed observers in
-- SupabasePostRepository + observePost for the detail screen) only receive
-- events when `posts` is in the `supabase_realtime` publication. Previously
-- this was enabled via the Supabase dashboard (Database → Replication), which
-- meant fresh environments and cases where the toggle was lost never got
-- events — the UI only updated on manual refresh.
--
-- REPLICA IDENTITY FULL is already set on posts by
-- 20260615000002_realtime_delete_replica_identity.sql; this migration only
-- adds it to the publication.
--
-- Idempotent: the DO block checks pg_publication_tables so re-running against
-- an environment where the dashboard toggle already enabled it is safe.

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime' AND tablename = 'posts'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE posts;
  END IF;
END $$;
