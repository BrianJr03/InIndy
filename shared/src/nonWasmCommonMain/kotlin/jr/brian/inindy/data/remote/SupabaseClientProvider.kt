package jr.brian.inindy.data.remote

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {
    val client = createSupabaseClient(
        supabaseUrl = SupabaseConfig.url,
        supabaseKey = SupabaseConfig.anonKey
    ) {
        install(Auth) {
            flowType = FlowType.PKCE
            scheme = "inindy"
            host = "auth"
        }
        install(Postgrest)
        install(Storage)
        install(Functions)
    }
}
