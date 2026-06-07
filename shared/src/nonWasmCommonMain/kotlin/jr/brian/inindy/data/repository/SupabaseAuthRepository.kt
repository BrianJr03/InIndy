package jr.brian.inindy.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import jr.brian.inindy.data.local.TokenStorage
import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.domain.repository.AuthRepository
import jr.brian.inindy.domain.repository.AuthSessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Real auth repository backed by Supabase.
 *
 * MVP scope:
 * - Email magic link — fully implemented.
 * - Session state — observed from supabase.auth.sessionStatus.
 *
 * Stubbed for V2 (returns Result.failure):
 * - Phone OTP (signUpWithPhone, verifyOtp) — needs Twilio configured in the Supabase dashboard.
 * - Google sign-in — needs CredentialManager wiring + Supabase Google provider.
 * - Apple sign-in — needs ASAuthorizationAppleIDProvider + Supabase Apple provider.
 */
class SupabaseAuthRepository(
    private val supabase: SupabaseClient,
    private val tokenStorage: TokenStorage,
    private val userPreferencesStore: UserPreferencesStore
) : AuthRepository {

    override val sessionState: Flow<AuthSessionState> =
        supabase.auth.sessionStatus
            .map { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        // Mirror the JWT into TokenStorage so cold starts can detect
                        // a stored session before supabase-kt finishes restoring.
                        tokenStorage.saveToken(status.session.accessToken)
                        AuthSessionState.SignedIn
                    }
                    is SessionStatus.NotAuthenticated -> {
                        tokenStorage.clearToken()
                        AuthSessionState.SignedOut
                    }
                    is SessionStatus.Initializing,
                    is SessionStatus.RefreshFailure -> AuthSessionState.Initializing
                }
            }
            .distinctUntilChanged()

    // ── Email magic link — MVP primary auth ─────────────────────────────────

    override suspend fun signUpWithEmail(email: String): Result<Unit> = runCatching {
        supabase.auth.signInWith(OTP) {
            this.email = email
        }
        // Magic link sent. Session is completed by the deep-link handler in
        // MainActivity / iOSApp once the user taps the link.
    }

    /** Verification of the link is automatic via supabase-kt's PKCE deep link callback. */
    override suspend fun verifyEmailLink(token: String): Result<User> = runCatching {
        val current = getCurrentUser()
            ?: error("Magic link verification is handled via deep link; no user is signed in yet")
        current
    }

    // ── Phone OTP — V2 only, Twilio not yet configured ──────────────────────

    override suspend fun signUpWithPhone(phone: String): Result<Unit> =
        Result.failure(NotImplementedError("Phone OTP is V2 only — Twilio not configured"))

    override suspend fun verifyOtp(phone: String, code: String): Result<User> =
        Result.failure(NotImplementedError("Phone OTP is V2 only — Twilio not configured"))

    // ── Social — stubbed for MVP ────────────────────────────────────────────

    override suspend fun signInWithGoogle(idToken: String): Result<User> =
        Result.failure(NotImplementedError("Google sign-in is post-MVP — CredentialManager not yet wired"))

    override suspend fun signInWithApple(idToken: String): Result<User> =
        Result.failure(NotImplementedError("Apple sign-in is post-MVP — not yet wired"))

    // ── Session ─────────────────────────────────────────────────────────────

    override suspend fun signOut(): Result<Unit> = runCatching {
        supabase.auth.signOut()
        tokenStorage.clearToken()
        userPreferencesStore.clear()
    }

    override suspend fun getCurrentUser(): User? {
        val supabaseUser = runCatching { supabase.auth.currentUserOrNull() }.getOrNull()
            ?: return null
        val prefs = userPreferencesStore.preferences.first()
        return User(
            id = supabaseUser.id,
            fullName = prefs.fullName,
            avatarUrl = prefs.avatarUrl,
            phoneVerified = supabaseUser.phone != null,
            neighborhoodId = prefs.neighborhoodId,
            interests = prefs.interests.mapNotNull { name ->
                runCatching { Interest.valueOf(name) }.getOrNull()
            }
        )
    }

    override suspend fun isSessionValid(): Boolean =
        supabase.auth.currentSessionOrNull() != null
}
