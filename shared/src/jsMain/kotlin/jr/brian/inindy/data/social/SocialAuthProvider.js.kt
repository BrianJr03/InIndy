package jr.brian.inindy.data.social

actual class SocialAuthProvider {

    actual suspend fun signInWithGoogle(): Result<String> =
        Result.failure(NotImplementedError("Social auth not available on web"))

    actual suspend fun signInWithApple(): Result<String> =
        Result.failure(NotImplementedError("Social auth not available on web"))
}
