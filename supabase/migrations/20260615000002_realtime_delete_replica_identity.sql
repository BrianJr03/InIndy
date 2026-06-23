-- Realtime emits DELETE events with the old row data only when REPLICA IDENTITY
-- is FULL. With the default (just the primary key), the old row sent to
-- subscribers contains only `id` — which means filters on non-PK columns like
-- `user_id`, `neighborhood_id`, or `group_id` can't be evaluated and the DELETE
-- event is dropped before it reaches the client.
--
-- Both tables drive filtered postgresChangeFlow subscriptions in the app
-- (SupabasePostRepository observes posts by user/neighborhood/group;
-- SupabaseGroupRepository observes group_members by user_id), so cross-device
-- deletes don't propagate without this.
--
-- Cost: every DELETE writes the full old row into the WAL. Acceptable for the
-- volume these tables see.

ALTER TABLE posts REPLICA IDENTITY FULL;
ALTER TABLE group_members REPLICA IDENTITY FULL;
