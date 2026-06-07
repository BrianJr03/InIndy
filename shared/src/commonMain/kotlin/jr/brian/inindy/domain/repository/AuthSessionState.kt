package jr.brian.inindy.domain.repository

sealed interface AuthSessionState {
    /** Initial state — we haven't yet resolved whether a stored session exists. */
    data object Initializing : AuthSessionState
    data object SignedIn : AuthSessionState
    data object SignedOut : AuthSessionState
}
