package jr.brian.inindy.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.data.social.SocialAuthProvider
import jr.brian.inindy.domain.repository.AuthRepository
import jr.brian.inindy.domain.repository.AuthSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val socialAuthProvider: SocialAuthProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Magic-link completion arrives asynchronously via the deep-link handler,
        // not through any onIntent call — the browser hands the code back to the
        // OS which hands it to supabase-kt. Observing sessionState here is what
        // flips EmailLinkSent → Authenticated so AuthNavHost can navigate out.
        viewModelScope.launch {
            authRepository.sessionState.collect { status ->
                if (status == AuthSessionState.SignedIn) {
                    val user = authRepository.getCurrentUser() ?: return@collect
                    _uiState.value = AuthUiState.Authenticated(user)
                }
            }
        }
    }

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.SignUpPhone -> signUpWithPhone(intent.phone)
            is AuthIntent.SignUpEmail -> signUpWithEmail(intent.email)
            is AuthIntent.VerifyOtp -> verifyOtp(intent.phone, intent.code)
            AuthIntent.SignInWithGoogle -> signInWithGoogle()
            AuthIntent.SignInWithApple -> signInWithApple()
            AuthIntent.CheckSession -> checkSession()
            AuthIntent.Reset -> _uiState.value = AuthUiState.Idle
        }
    }

    private fun signUpWithPhone(phone: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signUpWithPhone(phone).fold(
                onSuccess = { _uiState.value = AuthUiState.OtpSent },
                onFailure = { _uiState.value = AuthUiState.Error(it.message ?: "Could not send code") }
            )
        }
    }

    private fun signUpWithEmail(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signUpWithEmail(email).fold(
                onSuccess = { _uiState.value = AuthUiState.EmailLinkSent },
                onFailure = { _uiState.value = AuthUiState.Error(it.message ?: "Could not send link") }
            )
        }
    }

    private fun verifyOtp(phone: String, code: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.verifyOtp(phone, code).fold(
                onSuccess = { _uiState.value = AuthUiState.Authenticated(it) },
                onFailure = { _uiState.value = AuthUiState.Error(it.message ?: "Invalid code") }
            )
        }
    }

    private fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            socialAuthProvider.signInWithGoogle().fold(
                onSuccess = { idToken ->
                    authRepository.signInWithGoogle(idToken).fold(
                        onSuccess = { _uiState.value = AuthUiState.Authenticated(it) },
                        onFailure = { _uiState.value = AuthUiState.Error(it.message ?: "Google sign-in failed") }
                    )
                },
                onFailure = { _uiState.value = AuthUiState.Error(it.message ?: "Google sign-in cancelled") }
            )
        }
    }

    private fun signInWithApple() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            socialAuthProvider.signInWithApple().fold(
                onSuccess = { idToken ->
                    authRepository.signInWithApple(idToken).fold(
                        onSuccess = { _uiState.value = AuthUiState.Authenticated(it) },
                        onFailure = { _uiState.value = AuthUiState.Error(it.message ?: "Apple sign-in failed") }
                    )
                },
                onFailure = { _uiState.value = AuthUiState.Error(it.message ?: "Apple sign-in cancelled") }
            )
        }
    }

    private fun checkSession() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            if (authRepository.isSessionValid()) {
                _uiState.value = AuthUiState.SessionValid
            } else {
                _uiState.value = AuthUiState.Idle
            }
        }
    }
}
