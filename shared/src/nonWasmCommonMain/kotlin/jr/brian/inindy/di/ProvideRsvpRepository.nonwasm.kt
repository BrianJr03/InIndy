package jr.brian.inindy.di

import jr.brian.inindy.data.remote.SupabaseClientProvider
import jr.brian.inindy.data.repository.SupabaseRsvpRepository
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.RsvpRepository

actual fun provideRsvpRepository(
    currentUserProvider: CurrentUserProvider
): RsvpRepository = SupabaseRsvpRepository(
    supabase = SupabaseClientProvider.client,
    currentUserProvider = currentUserProvider
)
