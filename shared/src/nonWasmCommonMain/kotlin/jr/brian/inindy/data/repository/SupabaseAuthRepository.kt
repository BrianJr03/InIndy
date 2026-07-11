package jr.brian.inindy.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import jr.brian.inindy.data.local.TokenStorage
import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.domain.repository.AuthRepository
import jr.brian.inindy.domain.repository.AuthSessionState
import jr.brian.inindy.domain.repository.RsvpRepository
import jr.brian.inindy.util.appLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Real auth repository backed by Supabase.
 *
 * MVP scope:
 * - Email magic link — fully implemented.
 * - Session state — observed from supabase.auth.sessionStatus.
 * - Profile sync — on every sign-in, fetches public.users + user_interests
 *   and populates UserPreferencesStore so the app always has current user data.
 *
 * Stubbed for V2 (returns Result.failure):
 * - Phone OTP (signUpWithPhone, verifyOtp) — needs Twilio configured in the Supabase dashboard.
 * - Google sign-in — needs CredentialManager wiring + Supabase Google provider.
 * - Apple sign-in — needs ASAuthorizationAppleIDProvider + Supabase Apple provider.
 */
class SupabaseAuthRepository(
    private val supabase: SupabaseClient,
    private val tokenStorage: TokenStorage,
    private val userPreferencesStore: UserPreferencesStore,
    private val rsvpRepository: RsvpRepository
) : AuthRepository {

    private val log = appLog("SupabaseAuthRepository")

    override val sessionState: Flow<AuthSessionState> =
        supabase.auth.sessionStatus
            .map { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        // Mirror the JWT into TokenStorage so cold starts can detect
                        // a stored session before supabase-kt finishes restoring.
                        tokenStorage.saveToken(status.session.accessToken)
                        // Sync profile from Supabase into local DataStore so
                        // UserPreferencesStore is always populated for returning users.
                        syncUserProfile(status.session.user?.id)
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

    // ── Profile sync ─────────────────────────────────────────────────────────

    private suspend fun syncUserProfile(userId: String?) {
        if (userId == null) {
            log.d { "syncUserProfile — skipped, userId is null" }
            return
        }
        log.d { "syncUserProfile — syncing for userId: $userId" }
        try {
            // 1. Fetch user row from public.users
            val userRow = supabase.from("users")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<UserRow>()

            if (userRow == null) {
                log.w { "syncUserProfile — no public.users row found for $userId" }
                // User exists in auth.users but not public.users yet
                // The trigger should have created it — may be a timing issue
                userPreferencesStore.saveUserId(userId)
                userPreferencesStore.setOnboardingComplete(false)
                return
            }

            log.d { "syncUserProfile — found user: ${userRow.fullName}, neighborhood: ${userRow.neighborhoodId}" }

            // 2. Fetch interests
            val interests = Interest.fromStorageNames(
                supabase.from("user_interests")
                    .select {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<UserInterestRow>()
                    .map { it.interest }
            )

            log.d { "syncUserProfile — interests: ${interests.map { it.name }}" }

            // 3. Fetch neighborhood name if neighborhoodId is set
            val neighborhoodName = userRow.neighborhoodId?.let { nid ->
                runCatching {
                    supabase.from("neighborhoods")
                        .select {
                            filter { eq("id", nid) }
                        }
                        .decodeSingleOrNull<NeighborhoodRow>()
                        ?.name
                }.getOrNull()
            }

            // 4. Populate UserPreferencesStore
            userPreferencesStore.saveUserId(userId)

            if (userRow.fullName != null) {
                userPreferencesStore.saveProfile(userRow.fullName, userRow.avatarUrl)
            }

            if (userRow.neighborhoodId != null) {
                userPreferencesStore.saveNeighborhood(
                    userRow.neighborhoodId,
                    neighborhoodName ?: userRow.neighborhoodId
                )
            }

            if (interests.isNotEmpty()) {
                userPreferencesStore.saveInterests(interests)
            }

            // 5. Mark onboarding complete only if all required fields are present
            val isComplete = userRow.fullName != null
                    && userRow.neighborhoodId != null
                    && interests.isNotEmpty()

            userPreferencesStore.setOnboardingComplete(isComplete)

            // 6. Hydrate the RSVP cache so PostCard / PostDetail can render
            // the correct toggle state immediately on first feed render.
            rsvpRepository.getRsvpdPostIds(userId)

            log.i { "syncUserProfile complete — onboardingComplete: $isComplete" }

        } catch (e: Exception) {
            log.e(e) { "syncUserProfile FAILED" }
            // Don't crash — fall back to whatever is already in UserPreferencesStore
        }
    }

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

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        // The SDK attaches the current session JWT to functions.invoke automatically,
        // same as MediaRemoteDataSourceImpl uses for get-upload-url. The edge function
        // resolves the caller server-side from that token.
        val response: HttpResponse = supabase.functions.invoke(function = "delete-account")
        if (!response.status.isSuccess()) {
            error("delete-account edge function failed with status ${response.status.value}")
        }
        // Server-side deletion succeeded — tear down the local session so the
        // session flow emits SignedOut and RootNavGraph redirects to auth.
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
            interests = Interest.fromStorageNames(prefs.interests)
        )
    }

    override suspend fun isSessionValid(): Boolean =
        supabase.auth.currentSessionOrNull() != null

    // ── Private DTOs (internal to this repository only) ─────────────────────

    @Serializable
    private data class UserRow(
        val id: String,
        @SerialName("full_name") val fullName: String? = null,
        @SerialName("avatar_url") val avatarUrl: String? = null,
        @SerialName("neighborhood_id") val neighborhoodId: String? = null
    )

    @Serializable
    private data class UserInterestRow(
        @SerialName("user_id") val userId: String,
        val interest: String
    )

    @Serializable
    private data class NeighborhoodRow(
        val id: String,
        val name: String
    )
}