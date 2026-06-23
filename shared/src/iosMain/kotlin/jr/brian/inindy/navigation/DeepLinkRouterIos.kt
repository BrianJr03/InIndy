package jr.brian.inindy.navigation

import jr.brian.inindy.data.remote.handleSupabaseDeepLink
import org.koin.mp.KoinPlatform.getKoin

/**
 * Single entry point for Swift `.onOpenURL`. Routes inindy:// URLs to either
 * Supabase auth handling or the in-app invite bus.
 */
suspend fun handleInIndyDeepLink(url: String) {
    when (val result = parseDeepLink(url)) {
        is DeepLinkResult.GroupInvite -> {
            getKoin().get<DeepLinkBus>().postInviteToken(result.token)
        }
        DeepLinkResult.Auth,
        DeepLinkResult.Unknown -> handleSupabaseDeepLink(url)
    }
}
