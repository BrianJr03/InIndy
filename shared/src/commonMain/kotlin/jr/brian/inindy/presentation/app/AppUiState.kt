package jr.brian.inindy.presentation.app

data class AppUiState(
    // True only on cold bootstrap, before the auth repository has resolved the
    // session for the first time. Once flipped false it never flips back —
    // even if the repository re-enters an Initializing state on resume,
    // RootNavGraph must not tear down the mounted NavHost to show a splash.
    val hasResolved: Boolean = false,
    val destination: AppDestination = AppDestination.Auth
) {
    val isInitializing: Boolean get() = !hasResolved
}
