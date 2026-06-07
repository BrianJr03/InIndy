---
name: interests
description: Add, manage, or audit Interest and PostTag enum values in InIndy. Use whenever Interest.kt or PostTag.kt needs a new value, or when checking if Kotlin enums are in sync with the database. Always generates the required Supabase migration alongside any Kotlin change.
---

# InIndy Interest & PostTag Enum Management

Manage `Interest` and `PostTag` enum values safely across Kotlin and Postgres.

## Critical rule
**Every Kotlin enum addition requires a Supabase migration.**
Postgres enums can be added to but NEVER removed or renamed in place.
Kotlin and the DB must always be in sync — a mismatch causes runtime crashes on insert.

---

## Current Interest values

File: `shared/commonMain/domain/model/Interest.kt`
```kotlin
enum class Interest(val displayName: String) {
    // Outdoor & Active
    RUNNING("Running"),
    HIKING("Hiking"),
    CYCLING("Cycling"),
    WALKING("Walking"),
    YOGA("Yoga"),
    SPORTS("Sports"),
    SWIMMING("Swimming"),
    SKATING("Skating"),

    // Social & Casual
    PICNICS("Picnics"),
    BONFIRES("Bonfires"),
    GAME_NIGHTS("Game Nights"),
    COFFEE("Coffee"),
    FOOD("Food & Drinks"),
    VOLUNTEERING("Volunteering"),

    // Creative & Enrichment
    PHOTOGRAPHY("Photography"),
    DRAWING("Drawing & Sketching"),
    READING("Reading"),
    MUSIC("Music"),
    CRAFTS("Crafts"),
    WRITING("Writing"),

    // Exploration & Nature
    EXPLORING("Exploring"),
    BIRDWATCHING("Birdwatching"),
    GARDENING("Gardening"),
    STARGAZING("Stargazing"),
    NATURE("Nature Walks"),

    // Pets
    DOG_WALKS("Dog Walks"),
    PET_FRIENDLY("Pet Friendly")
}
```

## Current PostTag values

File: `shared/commonMain/domain/model/PostTag.kt`
```kotlin
enum class PostTag(val displayName: String) {
    HIKE("Hike"),
    RUN("Run"),
    PICNIC("Picnic"),
    SPORT("Sport"),
    WALK("Walk"),
    EXPLORE("Explore"),
    SWIM("Swim"),
    SKATE("Skate"),
    COFFEE("Coffee"),
    FOOD("Food"),
    VOLUNTEER("Volunteer"),
    PHOTO("Photography"),
    MUSIC("Music"),
    NATURE("Nature"),
    DOGS("Dogs"),
    OTHER("Other")
}
```

---

## How to add a new Interest value

### Step 1 — Add to Kotlin
Add to `Interest.kt` in the appropriate category section:
```kotlin
NEW_VALUE("Display Name"),
```

### Step 2 — Generate migration immediately
```sql
-- supabase/migrations/<timestamp>_add_<value>_to_user_interest.sql
ALTER TYPE user_interest ADD VALUE IF NOT EXISTS 'NEW_VALUE';
```
- Use `IF NOT EXISTS` — idempotent, safe to run twice
- Enum constant name must exactly match Kotlin: `STARGAZING` not `Stargazing`
- One `ALTER TYPE` per new value — do not batch with other schema changes

### Step 3 — Update FakeOnboardingRepository + FakeProfileEditRepository
Both return hardcoded `Interest` lists — they don't need updating (Kotlin enum is the source), but verify the new value appears in the UI chip grid automatically since it iterates `Interest.entries`

### Step 4 — Push migration
```bash
supabase db push
```

---

## How to add a new PostTag value

Same process as Interest:

### Step 1 — Add to Kotlin
```kotlin
NEW_TAG("Display Name"),
```

### Step 2 — Generate migration immediately
```sql
ALTER TYPE post_tag ADD VALUE IF NOT EXISTS 'NEW_TAG';
```

### Step 3 — Push migration
```bash
supabase db push
```

---

## How to audit sync status

If you're unsure whether Kotlin and the DB are in sync, generate this check:

```sql
-- Run in Supabase SQL editor to see all current enum values
SELECT enumlabel
FROM pg_enum
JOIN pg_type ON pg_enum.enumtypid = pg_type.oid
WHERE pg_type.typname = 'user_interest'
ORDER BY enumsortorder;

SELECT enumlabel
FROM pg_enum
JOIN pg_type ON pg_enum.enumtypid = pg_type.oid
WHERE pg_type.typname = 'post_tag'
ORDER BY enumsortorder;
```

Compare output against `Interest.kt` and `PostTag.kt` — every Kotlin value must have a matching DB row.

---

## What NEVER to do

- **Never** add a Kotlin enum value without a migration — app will crash on insert
- **Never** remove a Kotlin enum value that exists in the DB — existing rows will fail to deserialize
- **Never** rename a Kotlin enum value without a full migration strategy — name must match exactly
- **Never** batch an `ALTER TYPE` with other table changes — run enum additions in their own migration file
- **Never** use `displayName` as the DB value — always use the Kotlin constant name (`STARGAZING` not `"Stargazing"`)

---

## Removing or renaming an enum value (complex — requires explicit confirmation)

Postgres does NOT support `DROP VALUE` or `RENAME VALUE` before Postgres 16, and even in 16 it's experimental.

**If asked to remove or rename an enum value:**
1. Warn the developer — this is a destructive operation
2. Do NOT generate any migration without explicit confirmation
3. Present this strategy for confirmation:

```
Safe removal strategy:
1. Add new replacement value to DB enum (if renaming)
2. Update application code to write new value going forward
3. Migrate existing rows: UPDATE table SET column = 'NEW' WHERE column = 'OLD'
4. Remove old value references from Kotlin
5. Coordinate DB enum cleanup with a maintenance window
   (Postgres 16+: ALTER TYPE ... DROP VALUE — still experimental)
   (Postgres <16: recreate enum type + all dependent columns — complex)
```

---

## DB enum names reference

| Kotlin file | DB enum type name |
|---|---|
| `Interest.kt` | `user_interest` |
| `PostTag.kt` | `post_tag` |
| `GroupRole` | `group_role` |
| `RsvpStatus` | `rsvp_status` |
EOF