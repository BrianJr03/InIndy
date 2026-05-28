---
name: auth
description: Implement the InIndy sign up, sign in, OTP, social auth, and onboarding flow. Use when building or modifying any part of the authentication or onboarding experience.
---

# InIndy Auth & Onboarding Flow

Implement the complete authentication and onboarding flow for InIndy based on the plan below.
Build against interfaces — Supabase does not need to exist yet.

## Flow overview

```
App launch
    ↓
Token valid? → Auto sign-in → Main app
    ↓ no
Splash / Intro (3 screens)
    ↓
Welcome screen (Sign up primary, Sign in secondary)
    ↓
Sign up                          Sign in
  ├── Phone → OTP verify          ├── Phone → OTP verify
  ├── Email (magic link)          ├── Email (magic link)
  └── Google / Apple              └── Google / Apple
         ↓                                ↓
Onboarding (new users only)          Main app
  ├── Step 1: Full name + photo
  ├── Step 2: Pick neighborhood
  └── Step 3: Pick interests (multi-select)
         ↓
    Main app
```

---

## Phase 1 — Domain layer (no Supabase needed)

### 1. User domain model
File: `shared/commonMain/domain/model/User.kt`
```kotlin
data class User(
    val id: String,
    val fullName: String?,
    val avatarUrl: String?,
    val phoneVerified: Boolean,
    val neighborhoodId: String?,
    val interests: List<Interest>
)

val User.isOnboardingComplete: Boolean
    get() = fullName != null && neighborhoodId != null && interests.isNotEmpty()
```

### 2. Auth repository interface
File: `shared/commonMain/domain/repository/AuthRepository.kt`
```kotlin
interface AuthRepository {
    suspend fun signUpWithPhone(phone: String): Result<Unit>
    suspend fun signUpWithEmail(email: String): Result<Unit>
    suspend fun verifyOtp(phone: String, code: String): Result<User>
    suspend fun verifyEmailLink(token: String): Result<User>
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun signInWithApple(idToken: String): Result<User>
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentUser(): User?
    suspend fun isSessionValid(): Boolean
}
```

### 3. Onboarding repository interface
File: `shared/commonMain/domain/repository/OnboardingRepository.kt`
```kotlin
interface OnboardingRepository {
    suspend fun updateProfile(fullName: String, avatarUrl: String?): Result<Unit>
    suspend fun updateNeighborhood(neighborhoodId: String): Result<Unit>
    suspend fun updateInterests(interests: List<Interest>): Result<Unit>
    suspend fun getNeighborhoods(): Result<List<Neighborhood>>
}
```

### 4. Interests enum
File: `shared/commonMain/domain/model/Interest.kt`
```kotlin
enum class Interest(val displayName: String) {
    RUNNING("Running"),
    HIKING("Hiking"),
    CYCLING("Cycling"),
    WALKING("Walking"),
    PICNICS("Picnics"),
    SPORTS("Sports"),
    YOGA("Yoga"),
    EXPLORING("Exploring"),
    DOG_WALKS("Dog Walks"),
    BONFIRES("Bonfires")
}
```

### 5. Token storage — expect/actual
File: `shared/commonMain/data/local/TokenStorage.kt`
```kotlin
expect class TokenStorage {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
}
```
- `androidMain`: implement with `EncryptedSharedPreferences`
- `iosMain`: implement with Keychain via `Security` framework
- Never use plain `SharedPreferences` or `NSUserDefaults`

### 6. Fake repository (use until Supabase is ready)
File: `shared/commonMain/data/repository/FakeAuthRepository.kt`
- `signUpWithPhone` → delay 1000ms, return `Result.success(Unit)`
- `verifyOtp` → delay 1000ms, return mock `User` with `isOnboardingComplete = false`
- `signInWithGoogle` / `signInWithApple` → return mock `User`
- `isSessionValid` → return false (forces auth flow in dev)
- Register in Koin as the `AuthRepository` binding — one line swap when Supabase is ready

---

## Phase 2 — Social auth expect/actual

Google and Apple Sign-In are native SDKs — wire via `expect/actual`.

### Interface in commonMain
File: `shared/commonMain/data/social/SocialAuthProvider.kt`
```kotlin
expect class SocialAuthProvider {
    suspend fun signInWithGoogle(): Result<String> // returns idToken
    suspend fun signInWithApple(): Result<String>  // returns idToken
}
```

### Android actual
File: `shared/androidMain/data/social/SocialAuthProvider.android.kt`
- Use `CredentialManager` API (modern Google Sign-In)
- Requires `GOOGLE_CLIENT_ID` in `local.properties`

### iOS actual
File: `shared/iosMain/data/social/SocialAuthProvider.ios.kt`
- Use `ASAuthorizationAppleIDProvider` for Apple
- Use `GIDSignIn` for Google
- Both require entitlements in Xcode

---

## Phase 3 — ViewModels & UiState

### Auth ViewModel
File: `shared/commonMain/presentation/auth/AuthViewModel.kt`

```kotlin
sealed class AuthIntent {
    data class SignUpPhone(val phone: String) : AuthIntent()
    data class SignUpEmail(val email: String) : AuthIntent()
    data class VerifyOtp(val phone: String, val code: String) : AuthIntent()
    object SignInWithGoogle : AuthIntent()
    object SignInWithApple : AuthIntent()
    object CheckSession : AuthIntent()
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object SessionValid : AuthUiState()         // → route to main app
    object OtpSent : AuthUiState()              // → route to OTP screen
    object EmailLinkSent : AuthUiState()        // → show "check your email"
    data class Authenticated(val user: User) : AuthUiState()  // → check onboarding
    data class Error(val message: String) : AuthUiState()
}
```

### Onboarding ViewModel
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
        val interests: List<Interest>,
        val selected: Set<Interest> = emptySet()
    ) : OnboardingUiState()
    object Complete : OnboardingUiState()
    data class Error(val message: String) : OnboardingUiState()
}
```

---

## Phase 4 — Navigation

Add to `shared/commonMain/navigation/NavGraph.kt`:

```
auth_graph (shown when not authenticated)
    ├── intro/splash (3 pager screens)
    ├── welcome
    ├── signup_phone
    ├── signup_email
    ├── otp_verify/{phone}
    ├── email_link_sent
    ├── signin (sheet or screen)
    └── onboarding_graph
            ├── onboarding_profile
            ├── onboarding_neighborhood
            └── onboarding_interests

main_graph (shown when authenticated + onboarded)
    ├── explore (tab)
    └── events (tab)
```

### Session routing logic (in RootViewModel or NavHost)
```kotlin
when {
    !isSessionValid() -> navigate to auth_graph
    !user.isOnboardingComplete -> navigate to onboarding_graph
    else -> navigate to main_graph
}
```

---

## Phase 5 — Screens

Apply the `/design` skill to every screen. Key notes per screen:

### Intro/Splash screens (3 pager)
- Full-screen illustrated cards — warm, outdoor Indy imagery
- Large headline + short subtext per screen
- Dot indicator for progress
- "Get started" CTA on last screen only
- Skip button top-right on screens 1 and 2

### Welcome screen
- InIndy logo / wordmark centered
- "Create account" — primary filled button (accent color)
- "Sign in" — text button, secondary
- Social buttons: Google + Apple, full width, below primary CTA
- Legal copy at bottom: "By continuing you agree to our Terms & Privacy Policy"

### Sign up / Sign in screens
- Phone: single field with country code prefix (+1 default), numeric keyboard
- Email: single field, email keyboard, magic link explanation ("We'll send you a link — no password needed")
- Back navigation always available

### OTP screen
- 6-digit code input — individual boxes, auto-advance on input
- Auto-submit when 6th digit entered
- Resend code timer (60s countdown)
- Show last 4 digits of phone number for context

### Onboarding screens
- Step indicator (1 of 3) at top
- Profile: name text field + circular avatar picker (camera / gallery via expect/actual)
- Neighborhood: scrollable list with neighborhood names, single select
- Interests: grid of chips, multi-select, minimum 1 required
- "Continue" CTA disabled until required fields filled
- No back navigation from onboarding — must complete

---

## Phase 6 — Koin wiring

Add to `shared/commonMain/di/AuthModule.kt`:
```kotlin
val authModule = module {
    single<AuthRepository> { FakeAuthRepository() }       // swap for SupabaseAuthRepository later
    single<OnboardingRepository> { FakeOnboardingRepository() }
    single { TokenStorage() }
    single { SocialAuthProvider() }
    viewModel { AuthViewModel(get(), get()) }
    viewModel { OnboardingViewModel(get()) }
}
```

---

## Implementation order
1. Domain models + interfaces (User, Interest, Neighborhood, AuthRepository, OnboardingRepository)
2. TokenStorage expect/actual
3. FakeAuthRepository + FakeOnboardingRepository
4. Koin module
5. AuthViewModel + OnboardingViewModel
6. Navigation graph skeleton
7. Screens in flow order: Intro → Welcome → SignUp → OTP → Onboarding steps
8. SocialAuthProvider expect/actual (do last — requires native SDK setup)

## What NOT to do
- Don't implement SupabaseAuthRepository yet — use Fake until Supabase project is created
- Don't hardcode phone country codes — use a `CountryCode` data class
- Don't store JWT in plain SharedPreferences — always use TokenStorage expect/actual
- Don't skip the `isOnboardingComplete` gate — always check before routing to main app
- Don't reuse the same UiState sealed class for auth and onboarding — keep them separate
  EOF