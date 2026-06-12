package jr.brian.inindy.di

import jr.brian.inindy.data.repository.FakeRsvpRepository
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.RsvpRepository

// supabase-kt has no wasmJs artifact, so this target keeps the fake.
actual fun provideRsvpRepository(
    currentUserProvider: CurrentUserProvider
): RsvpRepository = FakeRsvpRepository()
