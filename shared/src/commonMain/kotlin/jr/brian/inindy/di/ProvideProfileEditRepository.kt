package jr.brian.inindy.di

import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.domain.repository.MediaRepository
import jr.brian.inindy.domain.repository.ProfileEditRepository

/**
 * Returns the active ProfileEditRepository for this platform.
 * On Android/iOS/JS this is SupabaseProfileEditRepository.
 * On wasmJs this falls back to FakeProfileEditRepository (supabase-kt has no wasmJs artifact).
 */
expect fun provideProfileEditRepository(
    userPreferencesStore: UserPreferencesStore,
    mediaRepository: MediaRepository
): ProfileEditRepository
