package jr.brian.inindy.data.remote

actual object SupabaseConfig {
    actual val url: String = SupabaseBuildConfig.URL
    actual val anonKey: String = SupabaseBuildConfig.ANON_KEY
}
