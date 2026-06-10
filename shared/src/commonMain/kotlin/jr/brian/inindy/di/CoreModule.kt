package jr.brian.inindy.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import jr.brian.inindy.domain.CurrentUserProvider
import org.koin.dsl.module

val coreModule = module {
    single { CurrentUserProvider(get()) }

    // Plain Ktor client used only for direct R2 uploads via signed URLs.
    // Kept separate from the Supabase client so no Authorization header is attached.
    single {
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
            }
        }
    }
}
