---
name: migrate
description: Generate a Supabase SQL migration file for InIndy schema changes. Use whenever the database schema needs to change.
argument-hint: <description of schema change>
---

# Supabase Migration

Generate a SQL migration for InIndy based on $ARGUMENTS.

## Steps

1. **Determine the next migration number** by checking `supabase/migrations/` for the highest existing number
2. **Create the migration file** at `supabase/migrations/<timestamp>_<slug>.sql`

## Migration template to follow

```sql
-- Migration: <description>
-- Created: <date>

-- ✅ Up migration
<SQL here>

-- Always add RLS policies for new tables
ALTER TABLE <table> ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can read own data"
  ON <table> FOR SELECT
  USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own data"
  ON <table> FOR INSERT
  WITH CHECK (auth.uid() = user_id);

-- For geo columns, always use PostGIS geography type
-- EXAMPLE: location GEOGRAPHY(POINT, 4326)
-- INDEX:    CREATE INDEX ON posts USING GIST(location);
```

## Rules
- Never use `DROP TABLE` or `DROP COLUMN` without a comment explaining why
- Always add RLS policies for every new table
- Geo columns must use `GEOGRAPHY(POINT, 4326)` not `GEOMETRY`
- Add a GiST index on every geo column
- Foreign keys must have `ON DELETE CASCADE` unless there's a specific reason not to
- After generating, remind the user to run: `supabase db push` or apply via Supabase dashboard
