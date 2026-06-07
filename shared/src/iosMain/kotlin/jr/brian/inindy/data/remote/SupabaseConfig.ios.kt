package jr.brian.inindy.data.remote

import platform.Foundation.NSBundle

actual object SupabaseConfig {
    actual val url: String = readInfoPlistString("SupabaseUrl")
    actual val anonKey: String = readInfoPlistString("SupabaseAnonKey")
}

private fun readInfoPlistString(key: String): String =
    (NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String).orEmpty()
