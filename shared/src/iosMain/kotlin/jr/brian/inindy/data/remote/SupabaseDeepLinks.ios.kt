package jr.brian.inindy.data.remote

import io.github.jan.supabase.auth.auth

/**
 * Called from Swift's `.onOpenURL` handler. Hands the deep-link URL off to
 * supabase-kt's Auth plugin so PKCE magic-link callbacks complete the session.
 */
suspend fun handleSupabaseDeepLink(url: String) {
    SupabaseClientProvider.client.auth.parseSessionFromUrl(url)
}
