package jr.brian.inindy.di

import jr.brian.inindy.data.local.TokenStorage
import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.data.remote.SupabaseClientProvider
import jr.brian.inindy.data.repository.SupabaseAuthRepository
import jr.brian.inindy.domain.repository.AuthRepository

actual fun provideAuthRepository(
    tokenStorage: TokenStorage,
    userPreferencesStore: UserPreferencesStore
): AuthRepository = SupabaseAuthRepository(
    supabase = SupabaseClientProvider.client,
    tokenStorage = tokenStorage,
    userPreferencesStore = userPreferencesStore
)
