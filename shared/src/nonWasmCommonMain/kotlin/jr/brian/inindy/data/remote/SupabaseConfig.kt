package jr.brian.inindy.data.remote

/**
 * Per-platform source for the Supabase project URL and anon key.
 * Android reads from BuildConfig (populated via local.properties).
 * iOS reads from Info.plist (SupabaseUrl, SupabaseAnonKey keys).
 * JS gets empty strings — JS is not a deployment target.
 */
expect object SupabaseConfig {
    val url: String
    val anonKey: String
}
