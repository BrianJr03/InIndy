package jr.brian.inindy.data.remote

import io.github.jan.supabase.auth.auth
import jr.brian.inindy.util.appLog
import kotlinx.coroutines.delay
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem

private val log = appLog("SupabaseDeepLinksIos")
private var lastHandledCode: String? = null

/**
 * Called from Swift's `.onOpenURL` handler. Extracts the PKCE `code` from the
 * deep-link URL and exchanges it for a Supabase session.
 *
 * Why not use supabase-kt's `SupabaseClient.handleDeeplinks(NSURL)` — that
 * helper launches the exchange on the Auth plugin's internal scope with no
 * error handling, so any HTTP failure (e.g. iOS Simulator's frequent
 * NSURLErrorDomain -1005 "network connection was lost") becomes an uncaught
 * Kotlin exception that can crash the app.
 *
 * PKCE codes are single-use — `onOpenURL` can fire more than once for the
 * same URL on iOS Simulator, so we drop exact-code repeats.
 */
suspend fun handleSupabaseDeepLink(url: String) {
    val nsUrl = NSURL.URLWithString(url)
    if (nsUrl == null) {
        log.e { "Deep link failed — could not parse URL: $url" }
        return
    }
    val components = NSURLComponents(nsUrl, false)
    val code = (
        components.queryItems?.firstOrNull { it is NSURLQueryItem && it.name == "code" }
            as? NSURLQueryItem
        )?.value
    if (code.isNullOrEmpty()) {
        log.w { "Deep link has no code query param: $url" }
        return
    }
    if (code == lastHandledCode) {
        log.d { "Deep link ignored (duplicate code)" }
        return
    }
    lastHandledCode = code

    // Simulator networking is flaky enough that a single retry recovers most
    // NSURLErrorDomain -1005 blips. Keep the retry count small — real network
    // failures should surface, not spin forever.
    val maxAttempts = 3
    repeat(maxAttempts) { attempt ->
        val result = runCatching {
            SupabaseClientProvider.client.auth.exchangeCodeForSession(code)
        }
        result.onSuccess {
            log.i { "Deep link exchanged successfully (attempt ${attempt + 1})" }
            return
        }
        result.onFailure { e ->
            val last = attempt == maxAttempts - 1
            if (last) {
                log.e(e) { "Deep link exchange failed after $maxAttempts attempts" }
                // Reset so a manual retry (tap the link again) isn't dropped by
                // the duplicate guard.
                lastHandledCode = null
            } else {
                log.w(e) { "Deep link exchange attempt ${attempt + 1} failed, retrying" }
                delay(400L * (attempt + 1))
            }
        }
    }
}
