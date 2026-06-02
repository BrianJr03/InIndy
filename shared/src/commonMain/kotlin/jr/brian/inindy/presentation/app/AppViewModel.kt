package jr.brian.inindy.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(
    private val authRepository: AuthRepository,
    private val userPreferencesStore: UserPreferencesStore
) : ViewModel() {

    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            val sessionValid = authRepository.isSessionValid()
            if (!sessionValid) {
                _state.update { it.copy(isLoading = false, destination = AppDestination.Auth) }
                return@launch
            }
            val prefs = userPreferencesStore.preferences.first()
            val destination = if (prefs.onboardingComplete) {
                AppDestination.Main
            } else {
                AppDestination.Onboarding
            }
            _state.update { it.copy(isLoading = false, destination = destination) }
        }
    }
}
