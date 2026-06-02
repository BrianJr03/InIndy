package jr.brian.inindy.presentation.app

data class AppUiState(
    val isLoading: Boolean = true,
    val destination: AppDestination = AppDestination.Auth
)
