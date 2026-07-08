package jr.brian.inindy.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

sealed class DeleteAccountState {
    data object Idle : DeleteAccountState()
    data object Deleting : DeleteAccountState()
    data class Error(val message: String) : DeleteAccountState()
}

data class SettingsUiState(
    val deleteAccount: DeleteAccountState = DeleteAccountState.Idle,
    // Mirrors UserPreferences.feedInterestOrderingEnabled — same default (off).
    val feedInterestOrderingEnabled: Boolean = false
)

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val userPreferencesStore: UserPreferencesStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        userPreferencesStore.preferences
            .map { it.feedInterestOrderingEnabled }
            .distinctUntilChanged()
            .onEach { enabled ->
                _uiState.value = _uiState.value.copy(feedInterestOrderingEnabled = enabled)
            }
            .launchIn(viewModelScope)
    }

    fun setInterestOrdering(enabled: Boolean) {
        // Optimistic UI update so the switch feels instant; persistence follows.
        // The observer above will confirm/overwrite with the store's value if
        // the write ever fails.
        _uiState.value = _uiState.value.copy(feedInterestOrderingEnabled = enabled)
        viewModelScope.launch {
            userPreferencesStore.setFeedInterestOrdering(enabled)
        }
    }

    fun deleteAccount() {
        if (_uiState.value.deleteAccount is DeleteAccountState.Deleting) return
        _uiState.value = _uiState.value.copy(deleteAccount = DeleteAccountState.Deleting)
        viewModelScope.launch {
            authRepository.deleteAccount()
                .onSuccess {
                    // Do NOT change state to Idle here — the session flip
                    // triggers RootNavGraph's redirect, and this screen leaves
                    // the composition. Leaving Deleting on state keeps the
                    // spinner visible during the tear-down.
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        deleteAccount = DeleteAccountState.Error(
                            e.message ?: "Failed to delete account"
                        )
                    )
                }
        }
    }

    fun dismissError() {
        if (_uiState.value.deleteAccount is DeleteAccountState.Error) {
            _uiState.value = _uiState.value.copy(deleteAccount = DeleteAccountState.Idle)
        }
    }
}
