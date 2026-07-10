package jr.brian.inindy.util

// JS is a dev-only target for MVP (see comment in SupabaseConfig.js.kt).
// Defaulting to debug keeps verbose logs visible when running the web app
// locally; there is no release channel to protect.
actual val isDebugBuild: Boolean = true
