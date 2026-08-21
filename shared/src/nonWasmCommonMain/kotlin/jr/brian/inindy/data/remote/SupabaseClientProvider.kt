package jr.brian.inindy.data.remote

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.logging.LogLevel
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import jr.brian.inindy.util.appLog
import jr.brian.inindy.util.isDebugBuild
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

object SupabaseClientProvider {
    private val log = appLog("Realtime")
    private val loggingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val client = createSupabaseClient(
        supabaseUrl = SupabaseConfig.url,
        supabaseKey = SupabaseConfig.anonKey
    ) {
        // Verbose Supabase logs in debug — surfaces realtime join failures that
        // silently drop postgres_changes when the socket's JWT is anon/expired.
        if (isDebugBuild) defaultLogLevel = LogLevel.DEBUG
        install(Auth) {
            flowType = FlowType.PKCE
            scheme = "inindy"
            host = "auth"
        }
        install(Postgrest)
        install(Storage)
        install(Functions)
        install(Realtime) {
            // Every observer used to cancel-and-recreate its channel on refresh,
            // and the default (true) tears the whole socket down when the last
            // channel is removed. The replacement channel then subscribed into a
            // socket mid-teardown, silently dropping postgres_changes on iOS.
            disconnectOnNoSubscriptions = false
        }
    }.also { c ->
        c.realtime.status
            .onEach { log.i { "socket status → $it" } }
            .launchIn(loggingScope)
    }
}
