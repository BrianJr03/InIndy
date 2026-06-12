package jr.brian.inindy.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.ProfileStep())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var fullName: String? = null
    private var avatarUri: String? = null
    private var neighborhoodId: String? = null
    private val selectedInterests = mutableSetOf<Interest>()

    fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            is OnboardingIntent.SubmitProfile -> submitProfile(intent.fullName, intent.avatarUri)
            is OnboardingIntent.SelectNeighborhood -> selectNeighborhood(intent.neighborhoodId)
            OnboardingIntent.ConfirmNeighborhood -> confirmNeighborhood()
            is OnboardingIntent.ToggleInterest -> toggleInterest(intent.interest)
            OnboardingIntent.CompleteOnboarding -> completeOnboarding()
        }
    }

    private fun submitProfile(name: String, avatar: String?) {
        if (name.isBlank()) {
            _uiState.value = OnboardingUiState.ProfileStep(error = "Name is required")
            return
        }
        fullName = name.trim()
        avatarUri = avatar
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            val profileResult = onboardingRepository.updateProfile(name.trim(), avatar)
            if (profileResult.isFailure) {
                _uiState.value = OnboardingUiState.ProfileStep(
                    error = profileResult.exceptionOrNull()?.message ?: "Could not save profile"
                )
                return@launch
            }
            onboardingRepository.getNeighborhoods().fold(
                onSuccess = { hoods ->
                    _uiState.value = OnboardingUiState.NeighborhoodStep(
                        neighborhoods = hoods,
                        selected = neighborhoodId
                    )
                },
                onFailure = {
                    _uiState.value = OnboardingUiState.Error(
                        it.message ?: "Could not load neighborhoods"
                    )
                }
            )
        }
    }

    private fun selectNeighborhood(id: String) {
        neighborhoodId = id
        val current = _uiState.value
        if (current is OnboardingUiState.NeighborhoodStep) {
            _uiState.value = current.copy(selected = id, error = null)
        }
    }

    private fun confirmNeighborhood() {
        val id = neighborhoodId
        if (id == null) {
            val current = _uiState.value
            if (current is OnboardingUiState.NeighborhoodStep) {
                _uiState.value = current.copy(error = "Pick a neighborhood")
            }
            return
        }
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            onboardingRepository.updateNeighborhood(id).fold(
                onSuccess = {
                    _uiState.value = OnboardingUiState.InterestsStep(
                        interests = Interest.entries,
                        selected = selectedInterests.toSet()
                    )
                },
                onFailure = {
                    _uiState.value = OnboardingUiState.Error(
                        it.message ?: "Could not save neighborhood"
                    )
                }
            )
        }
    }

    private fun toggleInterest(interest: Interest) {
        if (interest in selectedInterests) {
            selectedInterests.remove(interest)
        } else {
            selectedInterests.add(interest)
        }
        val current = _uiState.value
        if (current is OnboardingUiState.InterestsStep) {
            _uiState.value = current.copy(selected = selectedInterests.toSet(), error = null)
        }
    }

    private fun completeOnboarding() {
        if (selectedInterests.isEmpty()) {
            val current = _uiState.value
            if (current is OnboardingUiState.InterestsStep) {
                _uiState.value = current.copy(error = "Pick at least one interest")
            }
            return
        }
        viewModelScope.launch {
            val snapshot = _uiState.value
            _uiState.value = OnboardingUiState.Loading
            val interestsResult = onboardingRepository.updateInterests(selectedInterests.toList())
            if (interestsResult.isFailure) {
                _uiState.value = if (snapshot is OnboardingUiState.InterestsStep) {
                    snapshot.copy(error = interestsResult.exceptionOrNull()?.message ?: "Could not save interests")
                } else {
                    OnboardingUiState.Error(
                        interestsResult.exceptionOrNull()?.message ?: "Could not save interests"
                    )
                }
                return@launch
            }
            onboardingRepository.completeOnboarding().fold(
                onSuccess = { _uiState.value = OnboardingUiState.Complete },
                onFailure = {
                    _uiState.value = OnboardingUiState.Error(it.message ?: "Could not finish onboarding")
                }
            )
        }
    }
}
