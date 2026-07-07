package jr.brian.inindy.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DeleteAccountState {
    data object Idle : DeleteAccountState()
    data object Deleting : DeleteAccountState()
    data class Error(val message: String) : DeleteAccountState()
}

data class SettingsUiState(
    val deleteAccount: DeleteAccountState = DeleteAccountState.Idle
)

class SettingsViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

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
