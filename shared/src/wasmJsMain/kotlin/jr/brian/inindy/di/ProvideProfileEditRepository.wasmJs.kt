package jr.brian.inindy.di

import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.data.repository.FakeProfileEditRepository
import jr.brian.inindy.domain.repository.MediaRepository
import jr.brian.inindy.domain.repository.ProfileEditRepository

// supabase-kt has no wasmJs artifact, so this target keeps the fake.
actual fun provideProfileEditRepository(
    userPreferencesStore: UserPreferencesStore,
    mediaRepository: MediaRepository
): ProfileEditRepository = FakeProfileEditRepository(userPreferencesStore, mediaRepository)
