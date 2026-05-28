package jr.brian.inindy.presentation.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.domain.model.isOnboardingComplete
import jr.brian.inindy.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RootViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _destination = MutableStateFlow<RootDestination>(RootDestination.Splash)
    val destination: StateFlow<RootDestination> = _destination.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            val valid = authRepository.isSessionValid()
            if (!valid) {
                _destination.value = RootDestination.Auth
                return@launch
            }
            val user = authRepository.getCurrentUser()
            _destination.value = routeFor(user)
        }
    }

    fun onAuthenticated(user: User) {
        _destination.value = routeFor(user)
    }

    fun onOnboardingComplete() {
        _destination.value = RootDestination.Main
    }

    fun onSignOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _destination.value = RootDestination.Auth
        }
    }

    private fun routeFor(user: User?): RootDestination = when {
        user == null -> RootDestination.Auth
        !user.isOnboardingComplete -> RootDestination.Onboarding
        else -> RootDestination.Main
    }
}
