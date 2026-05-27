# InIndy — Claude Code Project Memory

## Project overview
InIndy is a Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP) social app for Indianapolis.
Two-tab structure: user-generated local event posts ("Catch Up") + curated Eventbrite events ("Explore").
Target: Android + iOS. Backend: Supabase (Postgres + PostGIS + Storage). Auth: Supabase Auth.

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

## Supabase integration
- All Supabase calls go through Ktor in `shared/commonMain/data/remote/`
- Base URL: stored in `BuildConfig` / `local.properties` — never hardcoded
- Auth: Supabase JWT via `Authorization: Bearer <token>` header on every request
- Storage: Supabase Storage for post images — bucket `post-images`, public CDN URLs stored in DB
- Geo queries: use PostGIS `ST_DWithin` for proximity — always pass lat/lng + radius in meters

## Eventbrite API
- Used for the Explore tab only — read-only, no auth required for public events
- Base URL: `https://www.eventbriteapi.com/v3/`
- Key param: `location.address=Indianapolis,IN` + `expand=venue`
- Cache responses in SQLDelight for 30 min to conserve API quota (500 req/day free tier)
- Eventbrite models live in `shared/commonMain/data/remote/eventbrite/`

## Database schema (key tables)
- `users` — id, display_name, avatar_url, created_at
- `posts` — id, user_id, title, description, location (PostGIS point), address, starts_at, ends_at, created_at
- `post_images` — id, post_id, storage_url, sort_order
- `post_tags` — post_id, tag (enum: hike, run, picnic, sport, walk, explore, other)
- `rsvps` — id, post_id, user_id, created_at
- Row-level security enabled on all tables — always test queries against anon + authed roles

## Code style
- Kotlin idioms: use `data class`, `sealed class`, `object`, extension functions appropriately
- Coroutines: `viewModelScope` for UI-bound work; `withContext(Dispatchers.IO)` for data layer
- Error handling: `Result<T>` or `Either<Error, T>` — never throw across module boundaries
- Naming: `XxxViewModel`, `XxxRepository`, `XxxUseCase`, `XxxDto` (network), `XxxEntity` (DB), `XxxUiState`
- No magic strings — use `object Constants` or enums
- Compose: stateless composables wherever possible; hoist state to ViewModel

## Gradle
- Version catalog: `gradle/libs.versions.toml` — all deps declared here, never inline
- Run `./gradlew :shared:build` to verify shared module compiles for all targets
- Run `./gradlew :androidApp:assembleDebug` for Android build
- iOS builds require Xcode — run from `iosApp/` in Xcode or via `xcodebuild`

## Common commands
```bash
./gradlew :shared:indy-build              # Build shared KMP module
./gradlew :androidApp:assembleDebug  # Android debug APK
./gradlew :shared:allTests           # Run all shared tests
./gradlew lint                        # Lint check
./gradlew :shared:generateSqlDelightInterface  # Regenerate SQLDelight queries
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

## Key files to read first
- `@shared/commonMain/data/remote/SupabaseClient.kt` — Ktor client setup
- `@shared/commonMain/domain/model/Post.kt` — core domain model
- `@gradle/libs.versions.toml` — all dependency versions