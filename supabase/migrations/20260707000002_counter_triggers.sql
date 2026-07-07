-- Maintain posts.rsvp_count and groups.member_count via database triggers so
-- every path — including cascade deletes from account deletion — keeps the
-- denormalized counts correct with no client involvement. Replaces reliance on
-- the client-called increment_rsvp_count / decrement_rsvp_count / sync_group_member_count
-- RPCs (those remain but become redundant; both they and the triggers do the same
-- authoritative recount, so there is no double-counting).

-- ============================================================
-- posts.rsvp_count — maintained by trigger on rsvps
-- ============================================================
CREATE OR REPLACE FUNCTION sync_post_rsvp_count()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF (TG_OP = 'INSERT' OR TG_OP = 'UPDATE') THEN
        UPDATE posts SET rsvp_count = (
            SELECT COUNT(*) FROM rsvps
            WHERE rsvps.post_id = NEW.post_id AND rsvps.status = 'confirmed'
        ) WHERE id = NEW.post_id;
    END IF;

    IF (TG_OP = 'DELETE'
        OR (TG_OP = 'UPDATE' AND NEW.post_id IS DISTINCT FROM OLD.post_id)) THEN
        UPDATE posts SET rsvp_count = (
            SELECT COUNT(*) FROM rsvps
            WHERE rsvps.post_id = OLD.post_id AND rsvps.status = 'confirmed'
        ) WHERE id = OLD.post_id;
    END IF;

    RETURN NULL; -- AFTER trigger; return value ignored
END;
$$;

DROP TRIGGER IF EXISTS trg_sync_post_rsvp_count ON rsvps;
CREATE TRIGGER trg_sync_post_rsvp_count
    AFTER INSERT OR UPDATE OR DELETE ON rsvps
    FOR EACH ROW
    EXECUTE FUNCTION sync_post_rsvp_count();

-- ============================================================
-- groups.member_count — maintained by trigger on group_members
-- ============================================================
CREATE OR REPLACE FUNCTION sync_group_member_count_trigger()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF (TG_OP = 'INSERT' OR TG_OP = 'UPDATE') THEN
        UPDATE groups SET member_count = (
            SELECT COUNT(*) FROM group_members WHERE group_members.group_id = NEW.group_id
        ) WHERE id = NEW.group_id;
    END IF;

    IF (TG_OP = 'DELETE'
        OR (TG_OP = 'UPDATE' AND NEW.group_id IS DISTINCT FROM OLD.group_id)) THEN
        UPDATE groups SET member_count = (
            SELECT COUNT(*) FROM group_members WHERE group_members.group_id = OLD.group_id
        ) WHERE id = OLD.group_id;
    END IF;

    RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS trg_sync_group_member_count ON group_members;
CREATE TRIGGER trg_sync_group_member_count
    AFTER INSERT OR UPDATE OR DELETE ON group_members
    FOR EACH ROW
    EXECUTE FUNCTION sync_group_member_count_trigger();

-- ============================================================
-- Backfill: correct anything already drifted
-- ============================================================
UPDATE posts SET rsvp_count = (
    SELECT COUNT(*) FROM rsvps
    WHERE rsvps.post_id = posts.id AND rsvps.status = 'confirmed'
);

UPDATE groups SET member_count = (
    SELECT COUNT(*) FROM group_members WHERE group_members.group_id = groups.id
);
