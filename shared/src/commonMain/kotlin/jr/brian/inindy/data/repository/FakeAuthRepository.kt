package jr.brian.inindy.data.repository

import jr.brian.inindy.data.local.TokenStorage
import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.domain.repository.AuthRepository
import jr.brian.inindy.domain.repository.AuthSessionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class FakeAuthRepository(
    private val tokenStorage: TokenStorage,
    private val userPreferencesStore: UserPreferencesStore
) : AuthRepository {

    private val _sessionState = MutableStateFlow<AuthSessionState>(
        if (tokenStorage.getToken() != null) AuthSessionState.SignedIn
        else AuthSessionState.SignedOut
    )
    override val sessionState: StateFlow<AuthSessionState> = _sessionState.asStateFlow()

    override suspend fun signUpWithPhone(phone: String): Result<Unit> {
        delay(NETWORK_DELAY_MS)
        return Result.success(Unit)
    }

    override suspend fun signUpWithEmail(email: String): Result<Unit> {
        delay(NETWORK_DELAY_MS)
        return Result.success(Unit)
    }

    override suspend fun verifyOtp(phone: String, code: String): Result<User> {
        delay(NETWORK_DELAY_MS)
        if (code != FAKE_VALID_OTP) {
            return Result.failure(IllegalArgumentException("Invalid code — try $FAKE_VALID_OTP"))
        }
        val user = User(
            id = "fake-user-${phone.takeLast(4)}",
            fullName = null,
            avatarUrl = null,
            phoneVerified = true
        )
        persistSignIn(user)
        return Result.success(user)
    }

    override suspend fun verifyEmailLink(token: String): Result<User> {
        delay(NETWORK_DELAY_MS)
        val user = User(
            id = "fake-email-user",
            fullName = null,
            avatarUrl = null,
            phoneVerified = false
        )
        persistSignIn(user)
        return Result.success(user)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        delay(NETWORK_DELAY_MS)
        val user = User(
            id = "fake-google-user",
            fullName = null,
            avatarUrl = null,
            phoneVerified = false
        )
        persistSignIn(user)
        return Result.success(user)
    }

    override suspend fun signInWithApple(idToken: String): Result<User> {
        delay(NETWORK_DELAY_MS)
        val user = User(
            id = "fake-apple-user",
            fullName = null,
            avatarUrl = null,
            phoneVerified = false
        )
        persistSignIn(user)
        return Result.success(user)
    }

    override suspend fun signOut(): Result<Unit> {
        tokenStorage.clearToken()
        userPreferencesStore.clear()
        _sessionState.value = AuthSessionState.SignedOut
        return Result.success(Unit)
    }

    override suspend fun getCurrentUser(): User? {
        val prefs = userPreferencesStore.preferences.first()
        val userId = prefs.userId ?: return null
        return User(
            id = userId,
            fullName = prefs.fullName,
            avatarUrl = prefs.avatarUrl,
            phoneVerified = true,
            neighborhoodId = prefs.neighborhoodId,
            interests = prefs.interests.mapNotNull { name ->
                runCatching { Interest.valueOf(name) }.getOrNull()
            }
        )
    }

    override suspend fun isSessionValid(): Boolean = tokenStorage.getToken() != null

    private suspend fun persistSignIn(user: User) {
        tokenStorage.saveToken("fake-jwt-${user.id}")
        userPreferencesStore.saveUserId(user.id)
        _sessionState.value = AuthSessionState.SignedIn
    }

    private companion object {
        const val NETWORK_DELAY_MS = 1_000L
        const val FAKE_VALID_OTP = "123456"
    }
}
