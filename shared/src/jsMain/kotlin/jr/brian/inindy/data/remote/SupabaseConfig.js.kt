package jr.brian.inindy.data.remote

// JS is not a deployment target for MVP — empty config so the build
// compiles. The Supabase client will fail to make requests if used.
actual object SupabaseConfig {
    actual val url: String = ""
    actual val anonKey: String = ""
}
