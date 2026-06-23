package jr.brian.inindy.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.domain.repository.AuthRepository
import jr.brian.inindy.domain.repository.AuthSessionState
import jr.brian.inindy.navigation.DeepLinkBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(
    private val authRepository: AuthRepository,
    private val userPreferencesStore: UserPreferencesStore,
    private val deepLinkBus: DeepLinkBus
) : ViewModel() {
    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    val pendingInviteToken: StateFlow<String?> = deepLinkBus.pendingInviteToken

    init {
        observeSession()
    }

    fun consumeInviteToken(): String? {
        val token = deepLinkBus.pendingInviteToken.value
        deepLinkBus.clearInviteToken()
        return token
    }

    private fun observeSession() {
        viewModelScope.launch {
            authRepository.sessionState.collect { status ->
                when (status) {
                    AuthSessionState.Initializing -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                    AuthSessionState.SignedIn -> {
                        val prefs = userPreferencesStore.preferences.first()
                        val destination = if (prefs.onboardingComplete) AppDestination.Main
                        else AppDestination.Onboarding
                        _state.update { it.copy(isLoading = false, destination = destination) }
                    }
                    AuthSessionState.SignedOut -> {
                        _state.update { it.copy(isLoading = false, destination = AppDestination.Auth) }
                    }
                }
            }
        }
    }
}
