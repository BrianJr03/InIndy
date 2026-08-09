package jr.brian.inindy.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.domain.model.RsvpReminderPref
import jr.brian.inindy.domain.repository.AuthRepository
import jr.brian.inindy.domain.repository.NotificationSettingsRepository
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

sealed class SignOutState {
    data object Idle : SignOutState()
    data object SigningOut : SignOutState()
    data class Error(val message: String) : SignOutState()
}

data class SettingsUiState(
    val deleteAccount: DeleteAccountState = DeleteAccountState.Idle,
    val signOut: SignOutState = SignOutState.Idle,
    val feedInterestOrderingEnabled: Boolean = false,
    val rsvpReminder: RsvpReminderPref = RsvpReminderPref.DAY_OF
)

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val userPreferencesStore: UserPreferencesStore,
    private val notificationSettingsRepository: NotificationSettingsRepository
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

        // One-shot load of the RSVP reminder pref. Subsequent changes come from
        // this device via setRsvpReminder — the flow doesn't re-emit, but the
        // optimistic update below keeps the UI current.
        notificationSettingsRepository.observeRsvpReminder()
            .onEach { result ->
                result.getOrNull()?.let { pref ->
                    _uiState.value = _uiState.value.copy(rsvpReminder = pref)
                }
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

    fun setRsvpReminder(pref: RsvpReminderPref) {
        _uiState.value = _uiState.value.copy(rsvpReminder = pref)
        viewModelScope.launch {
            notificationSettingsRepository.setRsvpReminder(pref)
        }
    }

    fun signOut() {
        if (_uiState.value.signOut is SignOutState.SigningOut) return
        _uiState.value = _uiState.value.copy(signOut = SignOutState.SigningOut)
        viewModelScope.launch {
            authRepository.signOut()
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        signOut = SignOutState.Error(
                            e.message ?: "Failed to sign out"
                        )
                    )
                }
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
        val current = _uiState.value
        val nextDelete = if (current.deleteAccount is DeleteAccountState.Error) {
            DeleteAccountState.Idle
        } else current.deleteAccount
        val nextSignOut = if (current.signOut is SignOutState.Error) {
            SignOutState.Idle
        } else current.signOut
        if (nextDelete !== current.deleteAccount || nextSignOut !== current.signOut) {
            _uiState.value = current.copy(deleteAccount = nextDelete, signOut = nextSignOut)
        }
    }
}
