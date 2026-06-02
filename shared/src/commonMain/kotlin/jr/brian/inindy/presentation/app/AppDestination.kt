package jr.brian.inindy.presentation.app

sealed class AppDestination {
    data object Auth : AppDestination()
    data object Onboarding : AppDestination()
    data object Main : AppDestination()
}
