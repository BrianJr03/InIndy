package jr.brian.inindy.data.social

expect class SocialAuthProvider {
    suspend fun signInWithGoogle(): Result<String>
    suspend fun signInWithApple(): Result<String>
}
