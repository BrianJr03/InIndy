package jr.brian.inindy.presentation.root

sealed class RootDestination {
    data object Splash : RootDestination()
    data object Auth : RootDestination()
    data object Onboarding : RootDestination()
    data object Main : RootDestination()
}
