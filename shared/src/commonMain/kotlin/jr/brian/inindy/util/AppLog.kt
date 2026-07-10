package jr.brian.inindy.util

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity

/**
 * Thin logging facade for InIndy.
 *
 * The rest of the codebase MUST call this facade (via [appLog]) and MUST NOT
 * import `co.touchlab.kermit` directly. Only this file wraps Kermit — keeping
 * the concrete logger swappable for a different sink later (Crashlytics,
 * Bugsnag, a no-op in tests, etc.) without touching call sites.
 *
 * Messages are lambda-typed so that when a severity is filtered out the
 * message string is never built.
 */
interface AppLog {
    fun v(throwable: Throwable? = null, message: () -> String)
    fun d(throwable: Throwable? = null, message: () -> String)
    fun i(throwable: Throwable? = null, message: () -> String)
    fun w(throwable: Throwable? = null, message: () -> String)
    fun e(throwable: Throwable? = null, message: () -> String)
}

/**
 * Returns a tagged [AppLog]. Typical usage:
 * ```
 * private val log = appLog("ExploreViewModel")
 * ```
 * The tag replaces the old `[InIndy] ClassName` prefix — Kermit adds the
 * `[InIndy]` scope automatically via [KermitAppLog.rootLogger].
 */
fun appLog(tag: String): AppLog = KermitAppLog(tag)

private class KermitAppLog(tag: String) : AppLog {
    private val logger: Logger = rootLogger.withTag(tag)

    override fun v(throwable: Throwable?, message: () -> String) {
        if (logger.config.minSeverity <= Severity.Verbose) {
            logger.v(throwable, message = message)
        }
    }

    override fun d(throwable: Throwable?, message: () -> String) {
        if (logger.config.minSeverity <= Severity.Debug) {
            logger.d(throwable, message = message)
        }
    }

    override fun i(throwable: Throwable?, message: () -> String) {
        if (logger.config.minSeverity <= Severity.Info) {
            logger.i(throwable, message = message)
        }
    }

    override fun w(throwable: Throwable?, message: () -> String) {
        if (logger.config.minSeverity <= Severity.Warn) {
            logger.w(throwable, message = message)
        }
    }

    override fun e(throwable: Throwable?, message: () -> String) {
        if (logger.config.minSeverity <= Severity.Error) {
            logger.e(throwable, message = message)
        }
    }

    companion object {
        // Shared "InIndy" scope so tag output is `InIndy/ExploreViewModel: ...`
        // and mutateConfiguration on this instance updates every tagged logger.
        val rootLogger: Logger = Logger.withTag("InIndy")
    }
}

/**
 * Set the global minimum severity for every [AppLog]. Call once from a single
 * init point (see `KoinInit`); do not scatter this call.
 *
 * Uses debug-vs-release from [isDebugBuild]:
 *   - debug   → [Severity.Verbose] (everything through)
 *   - release → [Severity.Warn]   (warnings and errors only)
 */
fun initAppLogging() {
    val minimum = if (isDebugBuild) Severity.Verbose else Severity.Warn
    Logger.setMinSeverity(minimum)
}
