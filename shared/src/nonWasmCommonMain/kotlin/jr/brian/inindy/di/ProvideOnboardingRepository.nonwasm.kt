package jr.brian.inindy.di

import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.data.remote.SupabaseClientProvider
import jr.brian.inindy.data.repository.SupabaseOnboardingRepository
import jr.brian.inindy.domain.repository.MediaRepository
import jr.brian.inindy.domain.repository.OnboardingRepository

actual fun provideOnboardingRepository(
    userPreferencesStore: UserPreferencesStore,
    mediaRepository: MediaRepository
): OnboardingRepository = SupabaseOnboardingRepository(
    supabase = SupabaseClientProvider.client,
    userPreferencesStore = userPreferencesStore,
    mediaRepository = mediaRepository
)
