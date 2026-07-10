package jr.brian.inindy.data.remote

import io.github.jan.supabase.auth.auth
import jr.brian.inindy.util.appLog

private val log = appLog("SupabaseDeepLinksIos")
private var lastHandledUrl: String? = null

/**
 * Called from Swift's `.onOpenURL` handler. Hands the deep-link URL off to
 * supabase-kt's Auth plugin so PKCE magic-link callbacks complete the session.
 *
 * PKCE auth codes are single-use — `onOpenURL` can fire more than once for the
 * same URL on iOS Simulator, so we drop exact-match repeats.
 */
suspend fun handleSupabaseDeepLink(url: String) {
    if (url == lastHandledUrl) {
        log.d { "Deep link ignored (duplicate): $url" }
        return
    }
    lastHandledUrl = url
    runCatching { SupabaseClientProvider.client.auth.exchangeCodeForSession(url) }
        .onSuccess { log.i { "Deep link exchanged successfully: $url" } }
        .onFailure { e ->
            log.e(e) { "Deep link failed — url: $url" }
        }
}