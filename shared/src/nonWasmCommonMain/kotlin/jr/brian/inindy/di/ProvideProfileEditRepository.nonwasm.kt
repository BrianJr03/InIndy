package jr.brian.inindy.di

import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.data.remote.SupabaseClientProvider
import jr.brian.inindy.data.repository.SupabaseProfileEditRepository
import jr.brian.inindy.domain.repository.MediaRepository
import jr.brian.inindy.domain.repository.ProfileEditRepository

actual fun provideProfileEditRepository(
    userPreferencesStore: UserPreferencesStore,
    mediaRepository: MediaRepository
): ProfileEditRepository = SupabaseProfileEditRepository(
    supabase = SupabaseClientProvider.client,
    userPreferencesStore = userPreferencesStore,
    mediaRepository = mediaRepository
)
