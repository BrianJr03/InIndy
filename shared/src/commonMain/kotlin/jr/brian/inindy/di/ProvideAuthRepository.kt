package jr.brian.inindy.di

import jr.brian.inindy.data.local.TokenStorage
import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.domain.push.PushRegistrar
import jr.brian.inindy.domain.repository.AuthRepository
import jr.brian.inindy.domain.repository.RsvpRepository

/**
 * Returns the active AuthRepository for this platform.
 * On Android/iOS/JS this is SupabaseAuthRepository.
 * On wasmJs this falls back to FakeAuthRepository (supabase-kt has no wasmJs artifact).
 */
expect fun provideAuthRepository(
    tokenStorage: TokenStorage,
    userPreferencesStore: UserPreferencesStore,
    rsvpRepository: RsvpRepository,
    pushRegistrar: PushRegistrar
): AuthRepository
