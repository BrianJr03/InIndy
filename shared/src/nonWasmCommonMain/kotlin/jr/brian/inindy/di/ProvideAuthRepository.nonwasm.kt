package jr.brian.inindy.di

import jr.brian.inindy.data.local.TokenStorage
import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.data.remote.SupabaseClientProvider
import jr.brian.inindy.data.repository.SupabaseAuthRepository
import jr.brian.inindy.domain.push.PushRegistrar
import jr.brian.inindy.domain.repository.AuthRepository
import jr.brian.inindy.domain.repository.RsvpRepository

actual fun provideAuthRepository(
    tokenStorage: TokenStorage,
    userPreferencesStore: UserPreferencesStore,
    rsvpRepository: RsvpRepository,
    pushRegistrar: PushRegistrar
): AuthRepository = SupabaseAuthRepository(
    supabase = SupabaseClientProvider.client,
    tokenStorage = tokenStorage,
    userPreferencesStore = userPreferencesStore,
    rsvpRepository = rsvpRepository,
    pushRegistrar = pushRegistrar
)
