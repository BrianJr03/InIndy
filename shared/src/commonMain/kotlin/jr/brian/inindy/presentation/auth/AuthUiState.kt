package jr.brian.inindy.presentation.auth

import jr.brian.inindy.domain.model.User

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data object SessionValid : AuthUiState()
    data object OtpSent : AuthUiState()
    data object EmailLinkSent : AuthUiState()
    data class Authenticated(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
