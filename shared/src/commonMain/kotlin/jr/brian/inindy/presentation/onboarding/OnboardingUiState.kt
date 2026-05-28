package jr.brian.inindy.presentation.onboarding

import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.domain.model.Neighborhood

sealed class OnboardingUiState {
    data object Loading : OnboardingUiState()
    data class ProfileStep(val error: String? = null) : OnboardingUiState()
    data class NeighborhoodStep(
        val neighborhoods: List<Neighborhood>,
        val selected: String? = null,
        val error: String? = null
    ) : OnboardingUiState()
    data class InterestsStep(
        val interests: List<Interest>,
        val selected: Set<Interest> = emptySet(),
        val error: String? = null
    ) : OnboardingUiState()
    data object Complete : OnboardingUiState()
    data class Error(val message: String) : OnboardingUiState()
}
