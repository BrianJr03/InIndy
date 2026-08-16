package jr.brian.inindy.data.remote

/**
 * Per-platform source for the Supabase project URL and anon key.
 * Android + iOS both read from SupabaseBuildConfig, generated at build time
 * from root local.properties (see shared/build.gradle.kts). JS gets empty
 * strings — JS is not a deployment target.
 */
expect object SupabaseConfig {
    val url: String
    val anonKey: String
}
