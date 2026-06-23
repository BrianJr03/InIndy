-- Authoritative member_count sync. Counts rows in group_members for the given
-- group and writes the result to groups.member_count. Replaces a client-side
-- read-modify-write that drifted whenever an UPSERT no-op'd (e.g. join via
-- invite for an already-joined user) or two writers raced.
--
-- group_id_input is declared as uuid since groups.id is uuid; supabase-kt
-- serialises strings as uuid params and casts on the WHERE clause.

CREATE OR REPLACE FUNCTION sync_group_member_count(group_id_input uuid)
RETURNS void
LANGUAGE sql
SECURITY DEFINER
AS $$
    UPDATE groups
    SET member_count = (
        SELECT COUNT(*) FROM group_members WHERE group_id = group_id_input
    )
    WHERE id = group_id_input;
$$;

GRANT EXECUTE ON FUNCTION sync_group_member_count(uuid) TO authenticated;

-- Backfill existing groups so anything stale from prior client-side drift
-- gets corrected at deploy time.
UPDATE groups SET member_count = (
    SELECT COUNT(*) FROM group_members WHERE group_members.group_id = groups.id
);
