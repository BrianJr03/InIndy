package jr.brian.inindy.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.domain.push.PushRegistrar
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
    private val deepLinkBus: DeepLinkBus,
    private val pushRegistrar: PushRegistrar
) : ViewModel() {
    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    val pendingInviteToken: StateFlow<String?> = deepLinkBus.pendingInviteToken
    val pendingPostId: StateFlow<String?> = deepLinkBus.pendingPostId

    init {
        observeSession()
    }

    fun consumeInviteToken(): String? {
        val token = deepLinkBus.pendingInviteToken.value
        deepLinkBus.clearInviteToken()
        return token
    }

    fun consumePostId(): String? {
        val postId = deepLinkBus.pendingPostId.value
        deepLinkBus.clearPostId()
        return postId
    }

    private fun observeSession() {
        viewModelScope.launch {
            authRepository.sessionState.collect { status ->
                when (status) {
                    AuthSessionState.Initializing -> {
                        // Cold-bootstrap case: hasResolved is already false —
                        // RootNavGraph shows the brand splash while we wait.
                        // Resume-refresh case: repository re-emits Initializing
                        // after the app returns to the foreground. We must NOT
                        // flip hasResolved back to false, or the entire NavHost
                        // gets torn down and the user's current screen is
                        // replaced by the splash mid-session.
                    }
                    AuthSessionState.SignedIn -> {
                        val prefs = userPreferencesStore.preferences.first()
                        val destination = if (prefs.onboardingComplete) AppDestination.Main
                        else AppDestination.Onboarding
                        _state.update { it.copy(hasResolved = true, destination = destination) }
                        viewModelScope.launch { pushRegistrar.registerCurrentToken() }
                    }
                    AuthSessionState.SignedOut -> {
                        _state.update { it.copy(hasResolved = true, destination = AppDestination.Auth) }
                    }
                }
            }
        }
    }
}
