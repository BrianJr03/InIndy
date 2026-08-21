package jr.brian.inindy.util

import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import jr.brian.inindy.data.remote.SupabaseClientProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val log = appLog("Realtime")
private val foregroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

actual fun onAppForegrounded() {
    val realtime = SupabaseClientProvider.client.realtime
    val status = realtime.status.value
    log.i { "onAppForegrounded — socket status: $status" }
    if (status != Realtime.Status.CONNECTED) {
        foregroundScope.launch {
            runCatching { realtime.connect() }
                .onFailure { log.e(it) { "onAppForegrounded — reconnect failed" } }
        }
    }
}

// Exposed to Swift as `AppForegroundIosKt.notifyAppForegrounded()` so the
// AppDelegate can bridge `UIApplication.didBecomeActiveNotification` into
// Kotlin without pulling supabase-kt into Swift.
fun notifyAppForegrounded() = onAppForegrounded()
