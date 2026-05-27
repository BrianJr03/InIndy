---
name: feature
description: Plan and scaffold a complete end-to-end feature for InIndy across all layers. Use at the start of any significant new feature.
argument-hint: <feature description>
---

# New Feature — End to End

Build a complete feature from description in $ARGUMENTS across all layers of InIndy.

## Phase 1 — Plan (show before writing any code)
Produce a short plan with:
- What screens are needed
- What data models / repositories are needed
- What Supabase tables / columns are affected
- Any expect/actual platform code needed
- Estimated file list

Wait for confirmation before proceeding.

## Phase 2 — Data layer
1. Run `/new-repository` for each new domain
2. Write SQLDelight migrations if schema changes
3. Add Supabase RLS policies (document in a comment block at top of RepositoryImpl)

## Phase 3 — Presentation layer
1. Run `/new-screen` for each new screen
2. Wire ViewModel → Repository via UseCase if logic is non-trivial

## Phase 4 — Navigation
1. Add routes to `NavGraph.kt`
2. Add bottom nav or back stack entry as appropriate

## Phase 5 — Verification checklist
- [ ] `./gradlew :shared:build` passes
- [ ] `./gradlew :shared:allTests` passes
- [ ] No hardcoded strings
- [ ] No API keys or secrets in code
- [ ] Koin DI graph compiles (run app and check for missing bindings)
- [ ] Works offline (local cache returns stale data gracefully)
