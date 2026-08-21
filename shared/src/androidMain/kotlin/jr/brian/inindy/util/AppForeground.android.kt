package jr.brian.inindy.util

// Android's OkHttp websocket delivers close callbacks reliably, so supabase-kt
// notices background disconnects and reconnects on its own. Nothing to do here.
actual fun onAppForegrounded() = Unit
