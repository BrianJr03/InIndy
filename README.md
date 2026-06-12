**InIndy**

Indianapolis has a lot going on... trail runs, park picnics, pickup games, neighborhood meetups, etc. Unfortunately, there is no single place to find it all. InIndy is a community app built for people who want to get outside and connect with others in their neighborhood. Post an activity, see what's happening nearby, join a group, and show up.

---

**Technical Overview**

InIndy is a Kotlin Multiplatform app with a shared codebase running natively on both Android and iOS.

**Mobile**
- Kotlin Multiplatform (KMP) + Compose Multiplatform — shared UI and logic across Android and iOS
- MVI architecture — sealed `UiState`, unidirectional data flow, `onIntent()`
- Koin — dependency injection
- Ktor — HTTP client
- Coil 3 — image loading
- SQLDelight — local database (configured, used for caching)
- DataStore — user preferences persistence
- AndroidX FileProvider — secure file URI handling for camera/gallery

**Backend**
- Supabase — Postgres database, Auth, Realtime, Edge Functions, Row Level Security
- PostGIS — geo queries on neighborhoods
- Supabase Edge Functions (Deno/TypeScript) — serverless functions for R2 signed URL generation

**Media**
- Cloudflare R2 — object storage for post images, avatars, group covers
- Cloudflare CDN — public image serving via `pub-*.r2.dev`

**Auth**
- Supabase Auth — email magic link (MVP)
- PKCE flow — deep link callback handling on both Android and iOS
- Google + Apple sign-in stubbed for post-MVP
