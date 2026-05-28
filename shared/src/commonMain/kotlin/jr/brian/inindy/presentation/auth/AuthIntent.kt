package jr.brian.inindy.presentation.auth

sealed class AuthIntent {
    data class SignUpPhone(val phone: String) : AuthIntent()
    data class SignUpEmail(val email: String) : AuthIntent()
    data class VerifyOtp(val phone: String, val code: String) : AuthIntent()
    data object SignInWithGoogle : AuthIntent()
    data object SignInWithApple : AuthIntent()
    data object CheckSession : AuthIntent()
    data object Reset : AuthIntent()
}
