---
name: supabase-auth
description: Implement the real SupabaseAuthRepository for InIndy — email magic link, Google, Apple sign in, session persistence, and token management using supabase-kt. Phone OTP is V2 only. Use when connecting the auth flow to the real Supabase backend.
---

# InIndy Supabase Auth Repository

Implement `SupabaseAuthRepository` for InIndy using `supabase-kt`.
This replaces `FakeAuthRepository` — swap in Koin when Supabase project is ready.

## MVP auth methods
- Email magic link — primary, zero third-party setup
- Google Sign-In
- Apple Sign-In

## V2 only — do NOT implement
- Phone OTP — requires Twilio, deferred to v2
- Remove `signUpWithPhone` and `verifyOtp` from the interface entirely

## Prerequisites
- Supabase project created with URL + anon key in `local.properties`
- Email auth enabled in Supabase dashboard (enabled by default — magic link works out of the box)
- Google OAuth configured in Supabase dashboard
- Apple OAuth configured in Supabase dashboard
- ⚠️ Phone auth (Twilio) is V2 only — do NOT configure or implement

---

## Phase 1 — Dependencies

### Add to `gradle/libs.versions.toml`
```toml
[versions]
supabase = "3.1.4"       # check latest at github.com/supabase-community/supabase-kt/releases
ktor = "3.1.2"           # must match supabase-kt's ktor requirement

[libraries]
# Supabase BOM — aligns all module versions
supabase-bom = { module = "io.github.jan-tennert.supabase:bom", version.ref = "supabase" }
supabase-auth = { module = "io.github.jan-tennert.supabase:auth-kt" }
supabase-postgrest = { module = "io.github.jan-tennert.supabase:postgrest-kt" }
supabase-storage = { module = "io.github.jan-tennert.supabase:storage-kt" }
supabase-functions = { module = "io.github.jan-tennert.supabase:functions-kt" }
supabase-compose-auth = { module = "io.github.jan-tennert.supabase:compose-auth" }

# Ktor engines — platform specific
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }  # Android
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }  # iOS
```

### Add to `shared/build.gradle.kts`
```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(platform(libs.supabase.bom))
            implementation(libs.supabase.auth)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.storage)
            implementation(libs.supabase.functions)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.supabase.compose.auth)  // Android only — Google Sign-In
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
```

---

## Phase 2 — Supabase client setup

### local.properties (never commit)
```properties
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_ANON_KEY=your-anon-key
GOOGLE_CLIENT_ID=your-google-client-id
```

### BuildConfig setup
In `androidApp/build.gradle.kts`:
```kotlin
android {
    buildFeatures { buildConfig = true }
    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"${properties["SUPABASE_URL"]}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${properties["SUPABASE_ANON_KEY"]}\"")
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${properties["GOOGLE_CLIENT_ID"]}\"")
    }
}
```

### Supabase client — expect/actual
File: `shared/commonMain/data/remote/SupabaseClientProvider.kt`
```kotlin
expect object SupabaseClientProvider {
    val client: SupabaseClient
}
```

File: `shared/androidMain/data/remote/SupabaseClientProvider.android.kt`
```kotlin
actual object SupabaseClientProvider {
    actual val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth) {
            flowType = FlowType.PKCE
            scheme = "inindy"        // deep link scheme for magic link + OAuth callbacks
            host = "auth"
        }
        install(Postgrest)
        install(Storage)
        install(Functions)
    }
}
```

File: `shared/iosMain/data/remote/SupabaseClientProvider.ios.kt`
```kotlin
actual object SupabaseClientProvider {
    actual val client = createSupabaseClient(
        supabaseUrl = "https://your-project-id.supabase.co",  // read from Info.plist
        supabaseKey = "your-anon-key"
    ) {
        install(Auth) {
            flowType = FlowType.PKCE
            scheme = "inindy"
            host = "auth"
        }
        install(Postgrest)
        install(Storage)
        install(Functions)
    }
}
```

### Deep link setup — Android
Add to `AndroidManifest.xml`:
```xml
<activity android:name=".MainActivity">
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="inindy" android:host="auth" />
    </intent-filter>
</activity>
```

### Deep link setup — iOS
Add to `Info.plist`:
```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>inindy</string>
        </array>
    </dict>
</array>
```

---

## Phase 3 — SupabaseAuthRepository

File: `shared/commonMain/data/repository/SupabaseAuthRepository.kt`

```kotlin
class SupabaseAuthRepository(
    private val supabase: SupabaseClient,
    private val tokenStorage: TokenStorage,
    private val userPreferencesStore: UserPreferencesStore
) : AuthRepository {

    // ── Email magic link — primary auth method ────────────────────────────

    override suspend fun signInWithEmail(email: String): Result<Unit> =
        runCatching {
            supabase.auth.signInWith(OTP) {
                this.email = email
            }
            // Magic link sent — session established via deep link callback
            // supabase-kt handles PKCE automatically when deep link is received
        }

    // Phone OTP — V2 only, do not implement

    // ── Google Sign-In ──────────────────────────────────────────────────────

    override suspend fun signInWithGoogle(idToken: String): Result<User> =
        runCatching {
            supabase.auth.signInWith(IDToken) {
                this.idToken = idToken
                this.provider = Google
            }
            val session = supabase.auth.currentSessionOrNull()
                ?: error("No session after Google sign in")
            persistSession(session)
            session.toUser()
        }

    // ── Apple Sign-In ───────────────────────────────────────────────────────

    override suspend fun signInWithApple(idToken: String): Result<User> =
        runCatching {
            supabase.auth.signInWith(IDToken) {
                this.idToken = idToken
                this.provider = Apple
            }
            val session = supabase.auth.currentSessionOrNull()
                ?: error("No session after Apple sign in")
            persistSession(session)
            session.toUser()
        }

    // ── Session ─────────────────────────────────────────────────────────────

    override suspend fun isSessionValid(): Boolean {
        return try {
            val session = supabase.auth.currentSessionOrNull()
            if (session != null) {
                // Refresh if expiring within 60s
                if (session.expiresIn < 60) {
                    supabase.auth.refreshCurrentSession()
                }
                true
            } else {
                // Try to restore from TokenStorage
                val token = tokenStorage.getToken() ?: return false
                supabase.auth.importSession(UserSession(accessToken = token, tokenType = "bearer"))
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getCurrentUser(): User? {
        return try {
            val supabaseUser = supabase.auth.retrieveUserForCurrentSession(updateSession = true)
            val prefs = userPreferencesStore.preferences.first()
            User(
                id = supabaseUser.id,
                fullName = prefs.fullName,
                avatarUrl = prefs.avatarUrl,
                phoneVerified = supabaseUser.phone != null,
                neighborhoodId = prefs.neighborhoodId,
                interests = prefs.interests
                    .mapNotNull { runCatching { Interest.valueOf(it) }.getOrNull() }
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun signOut(): Result<Unit> =
        runCatching {
            supabase.auth.signOut()
            tokenStorage.clearToken()
            userPreferencesStore.clear()
        }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private suspend fun persistSession(session: UserSession) {
        tokenStorage.saveToken(session.accessToken)
        userPreferencesStore.saveUserId(session.user?.id ?: return)
    }

    private fun UserSession.toUser(): User = User(
        id = user?.id ?: "",
        fullName = null,        // populated after onboarding
        avatarUrl = null,
        phoneVerified = user?.phone != null,
        neighborhoodId = null,
        interests = emptyList()
    )
}
```

---

## Phase 4 — SocialAuthProvider actuals

### Android actual — Google
File: `shared/androidMain/data/social/SocialAuthProvider.android.kt`
```kotlin
actual class SocialAuthProvider(private val context: Context) {
    actual suspend fun signInWithGoogle(): Result<String> = runCatching {
        val credentialManager = CredentialManager.create(context)
        val request = GetCredentialRequest(
            listOf(
                GetSignInWithGoogleOption
                    .Builder(BuildConfig.GOOGLE_CLIENT_ID)
                    .build()
            )
        )
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential as? GoogleIdTokenCredential
            ?: error("Not a GoogleIdTokenCredential")
        credential.idToken
    }

    actual suspend fun signInWithApple(): Result<String> {
        // Apple Sign-In on Android via Supabase OAuth redirect — not native
        // Trigger OAuth flow, return empty — handled via deep link callback
        return Result.failure(UnsupportedOperationException("Apple sign in on Android uses OAuth redirect"))
    }
}
```

### iOS actual — Apple + Google
File: `shared/iosMain/data/social/SocialAuthProvider.ios.kt`
```kotlin
actual class SocialAuthProvider {
    actual suspend fun signInWithGoogle(): Result<String> = runCatching {
        // Use GIDSignIn — requires GoogleSignIn iOS SDK in Podfile
        suspendCoroutine { continuation ->
            GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController) { result, error ->
                if let error = error {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                } else if let idToken = result?.user.idToken?.tokenString {
                    continuation.resume(idToken)
                }
            }
        }
    }

    actual suspend fun signInWithApple(): Result<String> = runCatching {
        // Use ASAuthorizationAppleIDProvider
        suspendCoroutine { continuation ->
            val request = ASAuthorizationAppleIDProvider().createRequest()
            request.requestedScopes = listOf(ASAuthorizationScope.fullName, ASAuthorizationScope.email)
            val controller = ASAuthorizationController(authorizationRequests = listOf(request))
            // Set delegate, handle credential → extract identityToken → resume
        }
    }
}
```

---

## Phase 5 — Handle magic link deep link

### Android — MainActivity
```kotlin
class MainActivity : ComponentActivity() {
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // supabase-kt handles PKCE deep link automatically
        // Just ensure the intent is passed to the Supabase client
        intent.data?.let { uri ->
            lifecycleScope.launch {
                SupabaseClientProvider.client.handleDeeplinks(uri.toString())
            }
        }
    }
}
```

### iOS — Scene delegate / SwiftUI
```swift
.onOpenURL { url in
    Task {
        try? await SupabaseClientProvider.shared.client.auth.session(from: url)
    }
}
```

---

## Phase 6 — Listen to auth state changes

In `AppViewModel` — replace one-time `isSessionValid()` check with a Flow:
```kotlin
init {
    viewModelScope.launch {
        SupabaseClientProvider.client.auth.authStateFlow.collect { state ->
            when (state) {
                is AuthState.SignedIn -> {
                    val prefs = userPreferencesStore.preferences.first()
                    val destination = if (prefs.onboardingComplete) AppDestination.Main
                                      else AppDestination.Onboarding
                    _state.update { it.copy(isLoading = false, destination = destination) }
                }
                is AuthState.SignedOut -> {
                    _state.update { it.copy(isLoading = false, destination = AppDestination.Auth) }
                }
                is AuthState.NotAuthenticated -> checkStoredToken()
            }
        }
    }
}
```

---

## Phase 7 — Koin swap

In `shared/commonMain/di/AuthModule.kt` — one line change:
```kotlin
val authModule = module {
    // Before:
    // single<AuthRepository> { FakeAuthRepository(get(), get()) }

    // After:
    single<AuthRepository> {
        SupabaseAuthRepository(
            supabase = SupabaseClientProvider.client,
            tokenStorage = get(),
            userPreferencesStore = get()
        )
    }

    // Everything else stays the same
    single<OnboardingRepository> { FakeOnboardingRepository(get()) }  // swap later
    single { TokenStorage() }
    viewModel { AuthViewModel(get(), get()) }
    viewModel { OnboardingViewModel(get()) }
}
```

---

## Implementation order
1. Add supabase-kt dependencies to `libs.versions.toml` + `build.gradle.kts`
2. Add `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GOOGLE_CLIENT_ID` to `local.properties`
3. Wire `BuildConfig` fields in `androidApp/build.gradle.kts`
4. Create `SupabaseClientProvider` expect/actual
5. Add deep link intent filter to `AndroidManifest.xml`
6. Add URL scheme to iOS `Info.plist`
7. Create `SupabaseAuthRepository`
8. Update `SocialAuthProvider` Android actual — `CredentialManager` for Google
9. Update `SocialAuthProvider` iOS actual — `GIDSignIn` + `ASAuthorizationAppleIDProvider`
10. Handle deep links in `MainActivity` (Android) and `.onOpenURL` (iOS)
11. Update `AppViewModel` to use `authStateFlow` instead of one-time check
12. Swap `FakeAuthRepository` → `SupabaseAuthRepository` in Koin

## What NOT to do
- Don't hardcode Supabase URL or anon key — always from `local.properties` via `BuildConfig`
- Don't commit `local.properties` — it must be in `.gitignore`
- Don't call `supabase.auth.currentSessionOrNull()` without handling null — always guard
- Don't skip PKCE flow — required for mobile OAuth and magic links
- Don't implement Apple Sign-In on Android natively — use Supabase OAuth redirect instead
- Don't manually manage JWT refresh — `supabase-kt` handles this automatically via `authStateFlow`
- Don't swap the Koin binding until Supabase project is fully set up and migrations are run
- Don't forget to add `ktor-client-okhttp` to androidMain and `ktor-client-darwin` to iosMain
  EOF
---

## Phase 8 — Secrets checklist (run before Phase 2)

Before writing any code, prompt the developer for every secret needed.
Display this checklist and wait for confirmation that all keys are available.

---

### Required secrets — ask for each one

```
Before we start, I need the following keys and secrets.
For each one, I'll tell you where to get it and how to provide it safely.
Please confirm when you have each one ready.
```

---

**1. Supabase Project URL**
- Where to get it: supabase.com → your project → Settings → API → Project URL
- Format: `https://your-project-id.supabase.co`
- How to provide: add to `local.properties` as `SUPABASE_URL=https://...`
- Safe? ✅ Not a secret — safe to expose in client apps, but keep out of git anyway

**2. Supabase Anon Key**
- Where to get it: supabase.com → your project → Settings → API → `anon` `public` key
- Format: long JWT string starting with `eyJ...`
- How to provide: add to `local.properties` as `SUPABASE_ANON_KEY=eyJ...`
- Safe? ✅ Designed to be used in client apps — RLS policies protect your data, not this key
- ⚠️ Never use the `service_role` key in the app — admin access, no RLS

**3. Google OAuth Client ID (Android)**
- Where to get it: console.cloud.google.com → your project → APIs & Services → Credentials → OAuth 2.0 Client IDs → Android client
- Requires: SHA-1 fingerprint of your debug keystore — run `./gradlew signingReport` to get it
- Format: `your-client-id.apps.googleusercontent.com`
- How to provide: add to `local.properties` as `GOOGLE_CLIENT_ID=...`
- Also: download `google-services.json` → place in `androidApp/`
- Safe? ✅ Client IDs are public — restricted by package name + SHA-1

**4. Google OAuth Client ID (iOS)**
- Where to get it: same Google Cloud console → iOS client
- Requires: your iOS Bundle ID (e.g. `com.yourname.inindy`)
- How to provide: download `GoogleService-Info.plist` → place in `iosApp/iosApp/`
- Also add reversed client ID to `Info.plist` URL schemes: `com.googleusercontent.apps.your-client-id`
- Safe? ✅ Same as Android

**5. Apple Sign In — no key needed in app**
- Where to set up: developer.apple.com → Certificates, Identifiers & Profiles → your App ID → Sign In with Apple
- Also: Xcode → your target → Signing & Capabilities → + Sign In with Apple
- In Supabase dashboard: Authentication → Providers → Apple → enter your Services ID + secret key
- How to provide to app: nothing in `local.properties` — handled entirely by native entitlement + Supabase
- Safe? ✅ No secret lives in the app

**6. Twilio credentials (phone OTP SMS) — V2 ONLY**
- Skip for MVP — phone auth is deferred to v2
- Do not configure Twilio or enable Phone provider in Supabase dashboard during MVP

**7. Cloudflare R2 credentials (media upload)**
- Where to get it: dash.cloudflare.com → R2 → Manage R2 API Tokens → Create API Token
- Keys needed: `Account ID`, `Access Key ID`, `Secret Access Key`, `Bucket name`
- How to provide: do NOT put in `local.properties` — these go in Supabase Edge Function environment variables
  - supabase.com → Edge Functions → your function → Secrets → add each one
- Safe? ✅ Never touches the client app — Edge Function holds them server-side

---

### `local.properties` template — copy this

```properties
# Supabase
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# Google Sign-In
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
```

### `.gitignore` — verify these are present
```
local.properties
google-services.json
GoogleService-Info.plist
*.jks
*.keystore
```

⚠️ Run `git status` before your first commit — if any of these files appear, add them to `.gitignore` immediately and remove them from git tracking with `git rm --cached <file>`.

---

### What goes where — summary table

| Secret | local.properties | Supabase dashboard | App bundle | Git |
|---|---|---|---|---|
| Supabase URL | ✅ | — | via BuildConfig | ❌ |
| Supabase anon key | ✅ | — | via BuildConfig | ❌ |
| Google client ID | ✅ | ✅ | via BuildConfig | ❌ |
| google-services.json | — | — | androidApp/ | ❌ |
| GoogleService-Info.plist | — | — | iosApp/ | ❌ |
| Apple Sign In | — | ✅ | entitlement only | — |
| Twilio credentials | ❌ | V2 only | never | ❌ |
| Cloudflare R2 keys | ❌ | ✅ edge fn secrets | never | ❌ |
| Supabase service_role key | ❌ | ✅ only | never | ❌ |

---

### Before proceeding to Phase 2

Confirm:
- [ ] `local.properties` created with Supabase URL + anon key
- [ ] `local.properties` is in `.gitignore`
- [ ] `google-services.json` placed in `androidApp/` (even if Google sign in is wired later)
- [ ] Twilio configured in Supabase dashboard
- [ ] Never committed any secret to git

Only proceed to Phase 2 once all applicable items are confirmed.