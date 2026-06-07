---
name: auth
description: Implement the InIndy sign up, sign in, magic link email, social auth (Google/Apple), and onboarding flow. Use when building or modifying any part of the authentication or onboarding experience.
---

# InIndy Auth & Onboarding Flow

Implement the complete authentication and onboarding flow for InIndy based on $ARGUMENTS.
Build against interfaces — Supabase does not need to exist yet.

## MVP auth methods
- **Email magic link** — primary, zero third-party setup, Supabase handles everything
- **Google Sign-In** — via `SocialAuthProvider` expect/actual
- **Apple Sign-In** — via `SocialAuthProvider` expect/actual

## V2 only — do NOT implement
- Phone OTP — requires Twilio, deferred to v2
- Remove any phone-related screens, intents, or repository methods

---

## Flow overview

```
App launch
    ↓
Token valid? → Auto sign-in → Main app
    ↓ no
Splash / Intro (3 screens)
    ↓
Welcome screen
    ├── [ Continue with email ]  ← primary CTA
    ├── [ Continue with Google ]
    └── [ Continue with Apple ]
    │
    ↓ email selected
Email screen → user enters email → tap "Send link"
    ↓
"Check your email" confirmation screen
    ↓ user taps magic link in email (deep link)
Session established → check onboarding gate
    ↓
Onboarding (new users only)
    ├── Step 1: Full name + photo
    ├── Step 2: Pick neighborhood
    └── Step 3: Pick interests (multi-select)
    ↓
Main app
```

---

## Phase 1 — Domain layer

### User domain model
File: `shared/commonMain/domain/model/User.kt`
```kotlin
data class User(
    val id: String,
    val fullName: String?,
    val avatarUrl: String?,
    val neighborhoodId: String?,
    val interests: List<Interest>
)

val User.isOnboardingComplete: Boolean
    get() = fullName != null && neighborhoodId != null && interests.isNotEmpty()
```

### Auth repository interface
File: `shared/commonMain/domain/repository/AuthRepository.kt`
```kotlin
interface AuthRepository {
    suspend fun signInWithEmail(email: String): Result<Unit>   // sends magic link
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun signInWithApple(idToken: String): Result<User>
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentUser(): User?
    suspend fun isSessionValid(): Boolean
}
```

### Onboarding repository interface
File: `shared/commonMain/domain/repository/OnboardingRepository.kt`
```kotlin
interface OnboardingRepository {
    suspend fun updateProfile(fullName: String, avatarUrl: String?): Result<Unit>
    suspend fun updateNeighborhood(neighborhoodId: String): Result<Unit>
    suspend fun updateInterests(interests: List<Interest>): Result<Unit>
    suspend fun completeOnboarding(): Result<Unit>
    suspend fun getNeighborhoods(): Result<List<Neighborhood>>
}
```

### Token storage — expect/actual
File: `shared/commonMain/data/local/TokenStorage.kt`
```kotlin
expect class TokenStorage {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
}
```
- `androidMain`: `EncryptedSharedPreferences`
- `iosMain`: Keychain via `Security` framework
- Never use plain `SharedPreferences` or `NSUserDefaults`

### Fake repository
File: `shared/commonMain/data/repository/FakeAuthRepository.kt`
```kotlin
class FakeAuthRepository(
    private val tokenStorage: TokenStorage,
    private val userPreferencesStore: UserPreferencesStore
) : AuthRepository {
    override suspend fun signInWithEmail(email: String): Result<Unit> {
        delay(800)
        // Simulate magic link sent — does not auto sign in
        return Result.success(Unit)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        delay(1000)
        val user = User("fake_user_001", null, null, null, emptyList())
        tokenStorage.saveToken("fake_jwt_${System.currentTimeMillis()}")
        userPreferencesStore.saveUserId(user.id)
        return Result.success(user)
    }

    override suspend fun signInWithApple(idToken: String): Result<User> {
        delay(1000)
        val user = User("fake_user_001", null, null, null, emptyList())
        tokenStorage.saveToken("fake_jwt_${System.currentTimeMillis()}")
        userPreferencesStore.saveUserId(user.id)
        return Result.success(user)
    }

    override suspend fun isSessionValid(): Boolean =
        tokenStorage.getToken() != null

    override suspend fun getCurrentUser(): User? {
        val prefs = userPreferencesStore.preferences.first()
        return prefs.userId?.let {
            User(it, prefs.fullName, prefs.avatarUrl, prefs.neighborhoodId, emptyList())
        }
    }

    override suspend fun signOut(): Result<Unit> {
        tokenStorage.clearToken()
        userPreferencesStore.clear()
        return Result.success(Unit)
    }
}
```

---

## Phase 2 — Social auth expect/actual

File: `shared/commonMain/data/social/SocialAuthProvider.kt`
```kotlin
expect class SocialAuthProvider {
    suspend fun signInWithGoogle(): Result<String>  // returns idToken
    suspend fun signInWithApple(): Result<String>   // returns idToken
}
```

- `androidMain`: `CredentialManager` API for Google, Supabase OAuth redirect for Apple
- `iosMain`: `GIDSignIn` for Google, `ASAuthorizationAppleIDProvider` for Apple

---

## Phase 3 — ViewModels & UiState

### AuthViewModel
File: `shared/commonMain/presentation/auth/AuthViewModel.kt`

```kotlin
sealed class AuthIntent {
    data class SignInWithEmail(val email: String) : AuthIntent()
    object SignInWithGoogle : AuthIntent()
    object SignInWithApple : AuthIntent()
    object CheckSession : AuthIntent()
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object SessionValid : AuthUiState()             // → route to main app
    object MagicLinkSent : AuthUiState()            // → show "check your email" screen
    data class Authenticated(val user: User) : AuthUiState()  // → check onboarding
    data class Error(val message: String) : AuthUiState()
}
```

### OnboardingViewModel
File: `shared/commonMain/presentation/onboarding/OnboardingViewModel.kt`

```kotlin
sealed class OnboardingIntent {
    data class SubmitProfile(val fullName: String, val avatarUri: String?) : OnboardingIntent()
    data class SelectNeighborhood(val neighborhoodId: String) : OnboardingIntent()
    data class ToggleInterest(val interest: Interest) : OnboardingIntent()
    object CompleteOnboarding : OnboardingIntent()
}

sealed class OnboardingUiState {
    object Loading : OnboardingUiState()
    data class ProfileStep(val error: String? = null) : OnboardingUiState()
    data class NeighborhoodStep(
        val neighborhoods: List<Neighborhood>,
        val selected: String? = null
    ) : OnboardingUiState()
    data class InterestsStep(
        val interests: List<Interest> = Interest.entries,
        val selected: Set<Interest> = emptySet()
    ) : OnboardingUiState()
    object Complete : OnboardingUiState()
    data class Error(val message: String) : OnboardingUiState()
}
```

---

## Phase 4 — Navigation

```
auth_graph
    ├── intro (3-screen pager)
    ├── welcome
    ├── signup_email
    ├── magic_link_sent
    └── signin (reuses welcome — sign in is secondary)

onboarding_graph
    ├── onboarding_profile
    ├── onboarding_neighborhood
    └── onboarding_interests

main_graph
    └── (Explore, Me, Events tabs)
```

### Session routing in AppViewModel
```kotlin
when {
    !isSessionValid() → auth_graph
    !user.isOnboardingComplete → onboarding_graph
    else → main_graph
}
```

---

## Phase 5 — Screens

Apply `/design` skill to every screen.

### Intro screens (3-screen pager)
- Full-screen illustrated cards — warm outdoor Indy imagery
- Large headline + short subtext per screen
- Dot page indicator
- "Get started" CTA on last screen only
- Skip button top-right on screens 1 and 2

### Welcome screen
- InIndy brandmark centered
- Three buttons stacked vertically:
  1. "Continue with email" — primary filled (accent color)
  2. "Continue with Google" — outlined with Google logo
  3. "Continue with Apple" — outlined with Apple logo (black on light, white on dark)
- "Sign in" text link at bottom for returning users — navigates to same screen, no separate flow
- Legal copy: "By continuing you agree to our Terms & Privacy Policy"
- No phone option — MVP is email + social only

### Email screen
- Single email text field, email keyboard
- Helper text: "We'll send you a sign-in link — no password needed"
- "Send link" primary button — always enabled, validate email format on tap
- Email format error shown below field if invalid
- Back button returns to welcome

### Magic link sent screen
- Confirmation illustration
- Headline: "Check your email"
- Body: "We sent a sign-in link to {email}. Tap the link to continue."
- "Resend link" text button — cooldown 60s
- "Use a different email" text button — pops back to email screen
- No automatic polling — deep link handles session establishment

### Onboarding screens
- Step indicator (1 of 3) at top
- Step 1: name text field + `AvatarPickerSection` (reuse from `/photo-picker` skill)
- Step 2: scrollable neighborhood list, single select, tap to select + checkmark
- Step 3: wrapping `InterestChipGrid` — all `Interest.entries`, multi-select, min 1 required
- "Continue" CTA disabled until required fields filled (onboarding is the exception — CTA IS disabled here)
- No back navigation from onboarding — must complete all steps

---

## Phase 6 — Koin wiring

File: `shared/commonMain/di/AuthModule.kt`
```kotlin
val authModule = module {
    single<AuthRepository> { FakeAuthRepository(get(), get()) }
    single<OnboardingRepository> { FakeOnboardingRepository(get()) }
    single { TokenStorage() }
    single { SocialAuthProvider() }
    viewModel { AuthViewModel(get(), get()) }
    viewModel { OnboardingViewModel(get()) }
}
```

---

## Implementation order
1. Domain models — `User`, `Interest`, `Neighborhood`
2. `AuthRepository` + `OnboardingRepository` interfaces
3. `TokenStorage` expect/actual
4. `FakeAuthRepository` + `FakeOnboardingRepository`
5. `SocialAuthProvider` expect/actual (stub — real impl in `/supabase-auth` skill)
6. Koin `authModule`
7. `AuthViewModel` + `OnboardingViewModel`
8. Navigation graph skeleton
9. Screens in flow order: Intro → Welcome → Email → Magic Link Sent → Onboarding steps
10. Deep link handling for magic link callback (document hook, real wiring in `/supabase-auth`)

## What NOT to do
- Don't implement phone OTP — deferred to v2
- Don't add a phone field anywhere in the auth UI
- Don't poll for session after sending magic link — deep link callback handles it
- Don't disable the email "Send link" button — validate on tap, show inline error
- Don't allow back navigation from onboarding — the CTA is the only way forward
- Don't store JWT in plain SharedPreferences — always use `TokenStorage` expect/actual
- Don't reference `FakeAuthRepository` outside `AuthModule.kt`