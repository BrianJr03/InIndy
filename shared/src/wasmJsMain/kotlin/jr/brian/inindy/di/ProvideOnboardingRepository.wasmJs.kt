package jr.brian.inindy.di

import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.data.repository.FakeOnboardingRepository
import jr.brian.inindy.domain.repository.OnboardingRepository

// supabase-kt has no wasmJs artifact, so this target keeps the fake.
actual fun provideOnboardingRepository(
    userPreferencesStore: UserPreferencesStore
): OnboardingRepository = FakeOnboardingRepository(userPreferencesStore)
