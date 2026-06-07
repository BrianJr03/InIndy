# InIndy — Claude Code Project Memory

## Project overview
InIndy is a Kotlin Multiplatform (CMP) social app for Indianapolis.
Two-tab structure: user-generated local event posts ("Explore") + curated Eventbrite events ("Events").
Target: Android + iOS. Backend: Supabase (Postgres + PostGIS + Storage). Auth: Supabase Auth.

## Current focus
- Building:
- Started:

## Module structure
```
inIndy/
├── androidApp/          # Android entry point, MainActivity
├── iosApp/              # Xcode project, iOS entry point
└── shared/
    ├── commonMain/      # All shared logic — models, repos, ViewModels, API
    ├── androidMain/     # Android-specific actuals
    └── iosMain/         # iOS-specific actuals
```
All business logic, networking, and state lives in `shared/commonMain`. Never put logic in platform modules.

## Architecture
- Pattern: MVI (Model-View-Intent) with unidirectional data flow
- ViewModel: `CommonViewModel` base in commonMain using Kotlin coroutines + StateFlow
- UI state: sealed `UiState<T>` — Loading / Success / Error
- Navigation: Compose Navigation Multiplatform
- DI: Koin (multiplatform)

## Tech stack
| Layer | Library |
|---|---|
| Networking | Ktor (commonMain) |
| Local DB | SQLDelight |
| Image loading | Coil 3 (multiplatform) |
| Location | `expect/actual` — platform GPS APIs |
| DI | Koin Multiplatform |
| Serialization | kotlinx.serialization |
| Coroutines | kotlinx.coroutines |

## String resources
- All strings defined in `shared/commonMain/composeResources/values/strings.xml`
- Access via `stringResource(Res.string.key_name)` in all composables
- Never hardcode user-facing strings in Kotlin files
- Uses official Compose Multiplatform Resources API (CMP 1.6+)

## Brand
- Accent color: TBD — update once finalised
- Typography: TBD — update once finalised
- Full theme defined in `shared/commonMain/ui/theme/InIndyTheme.kt`
- Color tokens, type scale, and shapes all live in the theme — never hardcode values in composables

## Supabase integration
- All Supabase calls go through Ktor in `shared/commonMain/data/remote/`
- Base URL: stored in `BuildConfig` / `local.properties` — never hardcoded
- Auth: Supabase JWT via `Authorization: Bearer <token>` header on every request
- Storage: Supabase Storage for post images — bucket `post-images`, public CDN URLs stored in DB
- Geo queries: use PostGIS `ST_DWithin` for proximity — always pass lat/lng + radius in meters

## Eventbrite API
- Used for the Events tab only — read-only, no auth required for public events
- Base URL: `https://www.eventbriteapi.com/v3/`
- Key param: `location.address=Indianapolis,IN` + `expand=venue`
- Cache responses in SQLDelight for 30 min to conserve API quota (500 req/day free tier)
- Eventbrite models live in `shared/commonMain/data/remote/eventbrite/`

## Database schema (key tables)

### Users & identity
- `users` — id, display_name, full_name, avatar_url, phone_verified, neighborhood_id, created_at
- `neighborhoods` — id, name, city (default: Indianapolis), slug

### Trust & reputation
- `user_stats` — user_id (PK), hosted_count, attended_count, rsvp_count, no_show_count, response_rate
- Attendance rate derived: `attended_count / rsvp_count` — computed, never stored directly
- `follows` — follower_id, following_id, created_at

### Posts
- `posts` — id, user_id, group_id (nullable — null = neighborhood post), neighborhood_id, title, description, location (PostGIS point), address, starts_at, ends_at, max_attendees (nullable), created_at
- `post_images` — id, post_id, storage_url, sort_order
- `post_tags` — post_id, tag (enum: hike, run, picnic, sport, walk, explore, other)
- `rsvps` — id, post_id, user_id, status (confirmed/cancelled), created_at

### Groups
- `groups` — id, name, description, cover_url, created_by, is_open (false = invite-only), created_at
- `group_members` — group_id, user_id, role (admin/member), joined_at
- `group_invites` — id, group_id, invited_by, token, expires_at, used_at

### Feed logic
- Neighborhood feed: posts WHERE group_id IS NULL AND neighborhood_id = user's neighborhood
- Group feed: posts WHERE group_id = ? AND user is a group_member
- group_id IS NULL = public neighborhood post; group_id populated = private group post
- Never expose group posts to non-members under any circumstances

### RLS rules
- Row-level security enabled on all tables
- Group posts: only visible to group_members
- group_invites: only visible to inviter and recipient
- user_stats: readable by all, writable by DB triggers only
- Always test queries against anon + authed roles

## Code style
- Kotlin idioms: use `data class`, `sealed class`, `object`, extension functions appropriately
- Coroutines: `viewModelScope` for UI-bound work; `withContext(Dispatchers.IO)` for data layer
- Error handling: `Result<T>` or `Either<Error, T>` — never throw across module boundaries
- Naming: `XxxViewModel`, `XxxRepository`, `XxxUseCase`, `XxxDto` (network), `XxxEntity` (DB), `XxxUiState`
- No magic strings — use `object Constants` or enums
- Compose: stateless composables wherever possible; hoist state to ViewModel
- Every public composable takes a `modifier: Modifier = Modifier` parameter
- Every composable has a `@Preview` for both light and dark theme

## Agent behavior
- Never run build, compile, lint, or test commands automatically
- Never verify work by building — the developer handles all builds

## Gradle
- Version catalog: `gradle/libs.versions.toml` — all deps declared here, never inline
- Run `./gradlew :shared:build` to verify shared module compiles for all targets
- Run `./gradlew :androidApp:assembleDebug` for Android build
- iOS builds require Xcode — run from `iosApp/` in Xcode or via `xcodebuild`

## Common commands
```bash
./gradlew :shared:build                          # Build shared KMP module
./gradlew :androidApp:assembleDebug              # Android debug APK
./gradlew :shared:allTests                       # Run all shared tests
./gradlew lint                                   # Lint check
./gradlew :shared:generateSqlDelightInterface    # Regenerate SQLDelight queries
```

## Testing
- Unit tests: `shared/commonTest/` — all business logic tested here
- Use `kotlinx.coroutines.test` with `runTest` for coroutine tests
- Mock network: Ktor `MockEngine` in tests — no real network calls in unit tests
- Prefer testing UseCases and Repositories over ViewModels directly

## Git workflow
- Branches: `feature/short-description`, `fix/short-description`, `chore/short-description`
- Commits: conventional commits — `feat:`, `fix:`, `chore:`, `refactor:`
- Never commit secrets, API keys, or `local.properties`
- `CLAUDE.local.md` is gitignored — use it for machine-specific notes

## What NOT to do
- Don't add platform-specific code to `commonMain` — use `expect/actual`
- Don't call Supabase directly from a Composable — always go through ViewModel → Repository
- Don't store auth tokens in SharedPreferences plaintext — use EncryptedSharedPreferences (Android) / Keychain (iOS) via expect/actual
- Don't cache Eventbrite images — hotlink from Eventbrite CDN via Coil
- Don't hardcode the Indianapolis lat/lng — use a `CityConfig` constant in commonMain
- Don't hardcode user-facing strings — use `stringResource(Res.string.x)` always
- Don't hardcode colors, typography, or shapes — use `InIndyTheme` tokens always
- Don't expose group posts to non-members — enforce at RLS level, not just app level

## Key files to read first
- `@shared/commonMain/data/remote/SupabaseClient.kt` — Ktor client setup
- `@shared/commonMain/domain/model/Post.kt` — core domain model
- `@shared/commonMain/domain/model/Group.kt` — group model
- `@shared/commonMain/ui/theme/InIndyTheme.kt` — full theme definition
- `@shared/commonMain/composeResources/values/strings.xml` — all string resources
- `@gradle/libs.versions.toml` — all dependency versions

## Auth & onboarding flow

### Navigation routing (root level)
```
isSessionValid() == false     → auth_graph (intro → welcome → sign up/in)
isOnboardingComplete == false → onboarding_graph (profile → neighborhood → interests)
else                          → main_graph (Explore + Events tabs)
```
Always check both gates on app launch and after sign in. Never skip onboarding gate.

### Session
- Auto sign-in if JWT token is valid on launch — never show auth screen to returning users
- Token stored via `TokenStorage` expect/actual — `EncryptedSharedPreferences` (Android), Keychain (iOS)
- `TokenStorage` lives in `shared/commonMain/data/local/TokenStorage.kt`

### Onboarding completion check
```kotlin
val User.isOnboardingComplete: Boolean
    get() = fullName != null && neighborhoodId != null && interests.isNotEmpty()
```
This is the single source of truth — use this extension, never replicate the logic elsewhere.

### Auth methods — MVP
- Email → magic link (no password — Supabase handles natively, zero third-party setup)
- Google → `SocialAuthProvider` expect/actual → Supabase OAuth
- Apple → `SocialAuthProvider` expect/actual → Supabase OAuth

### Auth methods — V2 (do not implement)
- Phone OTP — requires Twilio setup, SMS costs, deferred to v2
- Never implement phone auth without explicit instruction

### Fake vs real repositories
- `FakeAuthRepository` — used during development before Supabase is connected
- `SupabaseAuthRepository` — real implementation, swap in Koin when Supabase is ready
- Swap location: `shared/commonMain/di/AuthModule.kt` — one line change
- Never reference `FakeAuthRepository` outside of `AuthModule.kt`

### Social auth
- Google + Apple SDKs are native — wired via `SocialAuthProvider` expect/actual
- `androidMain`: uses `CredentialManager` API — requires `GOOGLE_CLIENT_ID` in `local.properties`
- `iosMain`: uses `ASAuthorizationAppleIDProvider` (Apple) + `GIDSignIn` (Google)
- ViewModel receives idToken from `SocialAuthProvider`, passes to `AuthRepository` — never handles OAuth directly

### Key auth files
- `@shared/commonMain/domain/repository/AuthRepository.kt` — interface
- `@shared/commonMain/domain/repository/OnboardingRepository.kt` — interface
- `@shared/commonMain/data/repository/FakeAuthRepository.kt` — dev stub
- `@shared/commonMain/data/local/TokenStorage.kt` — expect/actual token storage
- `@shared/commonMain/data/social/SocialAuthProvider.kt` — expect/actual social auth
- `@shared/commonMain/presentation/auth/AuthViewModel.kt` — auth MVI
- `@shared/commonMain/presentation/onboarding/OnboardingViewModel.kt` — onboarding MVI
- `@shared/commonMain/di/AuthModule.kt` — Koin wiring

## Media

### Photos only — no video at MVP
- Photos: supported, required for posts (minimum 1, maximum 3)
- Videos: deferred to v2 — too costly and complex for MVP
- Never implement video upload without explicit instruction

### Storage stack
- **Cloudflare R2** — object storage, zero egress fees ($0.015/GB/month writes only)
- **Cloudflare Images** — CDN + auto-resize ($5/month flat, up to 100k images)
- Supabase Storage is NOT used for post images — R2 only
- All public image URLs are Cloudflare CDN URLs — stored in `post_images.storage_url`

### Upload flow
```
User picks photo (native picker via expect/actual)
    ↓
Compress client-side (expect/actual ImageCompressor)
    ↓
Request signed R2 upload URL (Supabase Edge Function)
    ↓
Upload directly from device to R2 (never through backend)
    ↓
Store CDN URL in post_images table
    ↓
Coil loads from CDN URL
```
The backend never handles image bytes — only issues signed URLs. Keeps Supabase compute costs low.

### Client-side compression (expect/actual)
File: `shared/commonMain/data/media/ImageCompressor.kt`
```kotlin
expect class ImageCompressor {
    suspend fun compress(uri: String): ByteArray
}
```
- `androidMain`: `Bitmap.compress()`, JPEG 80%, max 1200px wide
- `iosMain`: `UIImage.jpegData(compressionQuality: 0.8)`, max 1200px wide
- Target output: under 1MB per image
- Never upload uncompressed images

### Cloudflare Image URL pattern
Always request the appropriate variant via URL params:
- Feed thumbnail: `?width=600&fit=cover`
- Detail / full view: `?width=1200&fit=scale-down`
- Avatar: `?width=200&height=200&fit=cover`
- Never load full-size images in feed — always use thumbnail variant

### What NOT to do
- Don't upload images through Supabase Storage — use R2 directly
- Don't upload uncompressed images — always compress client-side first
- Don't store raw device URIs in the database — only CDN URLs
- Don't implement video upload — deferred to v2
- Don't load full-size images in list/feed views — use Cloudflare resize params

## App navigation structure

### Bottom nav tabs
```
[ Me ] [ Explore ] [ Events ]
```
- **Me** (index 0) — personal activity hub. Your posts, groups, attendance history, + create post CTA
- **Explore** (index 1) — neighborhood + group feed. Default selected on first launch after onboarding
- **Events** (index 2) — Eventbrite-powered curated Indianapolis events

### Me tab screens
- `MeScreen` — hub with Create CTA, Your Posts, Your Groups, Attendance History sections
- `CreatePostScreen` — full-screen, pushed from Me CTA. Back returns to Me tab
- `GroupManagementScreen` — pushed from group card tap. Admin vs member view

### Tab naming rationale
- "Me" not "Create" or "Profile" — personal hub framing, ownership-first
- "Explore" — user-generated neighborhood/group posts
- "Events" — Eventbrite curated events

### V2 features (do not implement now)
- Draft posts
- Push notifications
- Profile edit screen (avatar tap → edit in Me header)

## String resource rules
- Never escape apostrophes in string resources — plain `'` works fine in CMP composeResources
- Do NOT use `\'` — it is not needed and will appear as a literal backslash
- This differs from standard Android `res/values/strings.xml` where escaping is required

## System insets
- Every new screen's outer Box must include `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)`
- This ensures top bar buttons sit below the notch/status bar on Android and iOS
- Scrollable content must clear the home indicator at the bottom
- Never skip this — missing insets breaks layout on notched and gesture-nav devices
- Apply to the outermost `Box` or `Scaffold` of every screen, not individual components

## Enum migration rule
- Every time `Interest.kt` or `PostTag.kt` gains a new value, a follow-up Supabase migration is required
- Postgres enums can be added to but NOT removed or renamed in place
- Migration syntax for adding a value:
  ```sql
  ALTER TYPE user_interest ADD VALUE 'NEW_VALUE_NAME';
  ALTER TYPE post_tag ADD VALUE 'NEW_VALUE_NAME';
  ```
- The enum value name in SQL must exactly match the Kotlin enum name (e.g. `STARGAZING` not `Stargazing`)
- Never add a Kotlin enum value without a corresponding migration — the app will crash on insert
- Never remove or rename a Kotlin enum value that exists in the DB without a full migration strategy
- If an enum value needs to be removed: deprecate in Kotlin first, migrate data, then drop (complex — plan carefully)
