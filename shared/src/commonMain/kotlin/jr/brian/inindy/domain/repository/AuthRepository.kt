package jr.brian.inindy.domain.repository

import jr.brian.inindy.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val sessionState: Flow<AuthSessionState>

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
