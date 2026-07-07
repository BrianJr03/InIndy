package jr.brian.inindy.di

import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.domain.repository.MediaRepository
import jr.brian.inindy.domain.repository.OnboardingRepository

/**
 * Returns the active OnboardingRepository for this platform.
 * On Android/iOS/JS this is SupabaseOnboardingRepository.
 * On wasmJs this falls back to FakeOnboardingRepository (supabase-kt has no wasmJs artifact).
 */
expect fun provideOnboardingRepository(
    userPreferencesStore: UserPreferencesStore,
    mediaRepository: MediaRepository
): OnboardingRepository
