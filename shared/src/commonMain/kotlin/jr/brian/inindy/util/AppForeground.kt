package jr.brian.inindy.util

/**
 * Called by the platform layer when the app returns to the foreground.
 *
 * iOS suspends NSURLSession websockets when the app is backgrounded and does
 * not reliably deliver the close callback to Ktor's Darwin engine — supabase-kt
 * therefore never learns it must reconnect. The iOS actual reads the realtime
 * socket status and kicks off a reconnect when it is not CONNECTED; Swift wires
 * this up via `UIApplication.didBecomeActiveNotification`.
 *
 * Android's websocket does not exhibit the same silent-drop, so the Android
 * actual is a no-op.
 */
expect fun onAppForegrounded()
