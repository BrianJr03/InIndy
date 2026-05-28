package jr.brian.inindy.data.social

actual class SocialAuthProvider {

    actual suspend fun signInWithGoogle(): Result<String> =
        Result.failure(NotImplementedError("Google Sign-In not wired yet — needs GIDSignIn"))

    actual suspend fun signInWithApple(): Result<String> =
        Result.failure(NotImplementedError("Apple Sign-In not wired yet — needs ASAuthorizationAppleIDProvider"))
}
