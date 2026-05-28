package jr.brian.inindy.data.social

import android.content.Context

actual class SocialAuthProvider(private val context: Context) {

    actual suspend fun signInWithGoogle(): Result<String> =
        Result.failure(NotImplementedError("Google Sign-In not wired yet — needs CredentialManager + GOOGLE_CLIENT_ID"))

    actual suspend fun signInWithApple(): Result<String> =
        Result.failure(NotImplementedError("Apple Sign-In is not supported on Android"))
}
