package jr.brian.inindy.di

import jr.brian.inindy.data.local.TokenStorage
import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.data.repository.FakeAuthRepository
import jr.brian.inindy.domain.repository.AuthRepository

// supabase-kt has no wasmJs artifact, so this target keeps the fake.
actual fun provideAuthRepository(
    tokenStorage: TokenStorage,
    userPreferencesStore: UserPreferencesStore
): AuthRepository = FakeAuthRepository(tokenStorage, userPreferencesStore)
