package jr.brian.inindy.data.remote

import platform.Foundation.NSBundle

actual object SupabaseConfig {
    actual val url: String = readInfoPlistString("SUPABASE_URL")
    actual val anonKey: String = readInfoPlistString("SUPABASE_ANON_KEY")
}

private fun readInfoPlistString(key: String): String =
    (NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String).orEmpty()
