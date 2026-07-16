package jr.brian.inindy.di

import jr.brian.inindy.data.local.TokenStorage
import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.data.repository.FakeAuthRepository
import jr.brian.inindy.domain.push.PushRegistrar
import jr.brian.inindy.domain.repository.AuthRepository
import jr.brian.inindy.domain.repository.RsvpRepository

// supabase-kt has no wasmJs artifact, so this target keeps the fake.
// rsvpRepository is unused here — the wasmJs fake auth repo doesn't sync RSVP state.
// pushRegistrar is unused here — wasmJs has no OS-level push.
actual fun provideAuthRepository(
    tokenStorage: TokenStorage,
    userPreferencesStore: UserPreferencesStore,
    rsvpRepository: RsvpRepository,
    pushRegistrar: PushRegistrar
): AuthRepository = FakeAuthRepository(tokenStorage, userPreferencesStore)
