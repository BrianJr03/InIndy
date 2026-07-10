package jr.brian.inindy.util

// Set from InIndyApplication.onCreate before initKoin so [isDebugBuild] reflects
// the app module's BuildConfig.DEBUG. The shared KMP library plugin doesn't
// expose its own BuildConfig (same reason SupabaseConfig has a generated file),
// so we thread the flag in at startup instead of reading it directly.
private var _isDebugBuild: Boolean = false

fun setIsDebugBuild(debug: Boolean) {
    _isDebugBuild = debug
}

actual val isDebugBuild: Boolean
    get() = _isDebugBuild
