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

## Enum value additions — special rules

Postgres enums can be extended but NOT modified in place. This applies to `user_interest` and `post_tag`.

### When Interest.kt or PostTag.kt gains a new value
Always generate a follow-up migration immediately — never let Kotlin and the DB get out of sync:

```sql
-- Migration: add STARGAZING to user_interest enum
ALTER TYPE user_interest ADD VALUE IF NOT EXISTS 'STARGAZING';
```

### Rules
- Use `IF NOT EXISTS` to make the migration idempotent — safe to run twice
- Enum value name must exactly match the Kotlin enum constant name — `STARGAZING` not `Stargazing`
- One `ALTER TYPE` per new value — do not combine with other schema changes in the same migration
- Enum values CANNOT be removed or renamed without a full migration strategy — never attempt in place
- If asked to remove an enum value: warn the developer, do not generate the migration without explicit confirmation and a data migration plan

### Removal strategy (document, do not implement without confirmation)
```
1. Add new replacement value (if renaming)
2. Migrate existing data to new value
3. Remove references in application code
4. Drop old value — only possible in Postgres 16+ via ALTER TYPE ... DROP VALUE (still experimental)
   OR recreate the enum type entirely (complex, requires table rewrites)
```