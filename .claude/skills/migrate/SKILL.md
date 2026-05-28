---
name: migrate
description: Generate a Supabase SQL migration file for InIndy schema changes. Use whenever the database schema needs to change.
argument-hint: <description of schema change>
---

# Supabase Migration

Generate a SQL migration for InIndy based on $ARGUMENTS.

## Steps

1. **Determine the next migration number** by checking `supabase/migrations/` for the highest existing timestamp
2. **Create the migration file** at `supabase/migrations/<timestamp>_<slug>.sql`

## Key schema conventions

### Posts
- `group_id` is nullable — NULL = public neighborhood post, populated = private group post
- Always include `neighborhood_id` on posts — required for neighborhood feed queries
- Location column must use `GEOGRAPHY(POINT, 4326)` not `GEOMETRY`

### Groups
- `is_open` boolean — false = invite-only (default), true = request to join
- Group post visibility enforced at RLS level, not just app level
- `group_invites.token` must be unique and cryptographically random (`gen_random_uuid()`)

### Trust & reputation
- `user_stats` is written by DB triggers only — never by the app directly
- Attendance rate is always computed (`attended_count / NULLIF(rsvp_count, 0)`) — never stored
- Triggers needed: on rsvp INSERT/UPDATE → update user_stats

## Migration template

```sql
-- Migration: <description>
-- Created: <date>

-- Up migration
<SQL here>

-- RLS for every new table
ALTER TABLE <table> ENABLE ROW LEVEL SECURITY;

-- Public neighborhood posts
CREATE POLICY "Anyone can read neighborhood posts"
  ON posts FOR SELECT
  USING (group_id IS NULL);

-- Group posts — members only
CREATE POLICY "Group members can read group posts"
  ON posts FOR SELECT
  USING (
    group_id IS NULL OR
    EXISTS (
      SELECT 1 FROM group_members
      WHERE group_members.group_id = posts.group_id
      AND group_members.user_id = auth.uid()
    )
  );

CREATE POLICY "Users can insert own posts"
  ON posts FOR INSERT
  WITH CHECK (auth.uid() = user_id);

-- Geo index on every location column
CREATE INDEX ON <table> USING GIST(location);
```

## Rules
- Never DROP TABLE or DROP COLUMN without a comment explaining why
- Always add RLS policies for every new table
- Foreign keys must have ON DELETE CASCADE unless there is a specific reason not to
- New tables affecting user_stats must include the corresponding trigger
- After generating, remind the developer to run: `supabase db push` or apply via Supabase dashboard
