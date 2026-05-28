package jr.brian.inindy.data.repository

import jr.brian.inindy.data.local.TokenStorage
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.domain.repository.AuthRepository
import kotlinx.coroutines.delay

class FakeAuthRepository(
    private val tokenStorage: TokenStorage
) : AuthRepository {

    private var currentUser: User? = null

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
        currentUser = user
        tokenStorage.saveToken("fake-token-${user.id}")
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
        currentUser = user
        tokenStorage.saveToken("fake-token-${user.id}")
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
        currentUser = user
        tokenStorage.saveToken("fake-token-${user.id}")
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
        currentUser = user
        tokenStorage.saveToken("fake-token-${user.id}")
        return Result.success(user)
    }

    override suspend fun signOut(): Result<Unit> {
        currentUser = null
        tokenStorage.clearToken()
        return Result.success(Unit)
    }

    override suspend fun getCurrentUser(): User? = currentUser

    override suspend fun isSessionValid(): Boolean = false

    private companion object {
        const val NETWORK_DELAY_MS = 1_000L
        const val FAKE_VALID_OTP = "123456"
    }
}
