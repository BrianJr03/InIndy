package jr.brian.inindy.di

import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.RsvpRepository

/**
 * Returns the active RsvpRepository for this platform.
 * On Android/iOS/JS this is SupabaseRsvpRepository.
 * On wasmJs this falls back to FakeRsvpRepository (supabase-kt has no wasmJs artifact).
 */
expect fun provideRsvpRepository(
    currentUserProvider: CurrentUserProvider
): RsvpRepository
