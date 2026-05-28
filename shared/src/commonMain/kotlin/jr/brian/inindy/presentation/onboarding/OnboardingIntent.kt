package jr.brian.inindy.presentation.onboarding

import jr.brian.inindy.domain.model.Interest

sealed class OnboardingIntent {
    data class SubmitProfile(val fullName: String, val avatarUri: String?) : OnboardingIntent()
    data class SelectNeighborhood(val neighborhoodId: String) : OnboardingIntent()
    data object ConfirmNeighborhood : OnboardingIntent()
    data class ToggleInterest(val interest: Interest) : OnboardingIntent()
    data object CompleteOnboarding : OnboardingIntent()
}
