package jr.brian.inindy.data.remote

import android.content.Intent
import io.github.jan.supabase.auth.handleDeeplinks

/**
 * Called from MainActivity.onCreate/onNewIntent. Hands the incoming intent
 * to supabase-kt so PKCE magic-link callbacks complete the session.
 */
fun handleSupabaseDeepLink(intent: Intent) {
    SupabaseClientProvider.client.handleDeeplinks(intent)
}
