package jr.brian.inindy.di

import jr.brian.inindy.data.remote.SupabaseClientProvider
import jr.brian.inindy.data.repository.SupabasePostRepository
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.MediaRepository
import jr.brian.inindy.domain.repository.PostRepository

actual fun providePostRepository(
    mediaRepository: MediaRepository,
    currentUserProvider: CurrentUserProvider
): PostRepository = SupabasePostRepository(
    supabase = SupabaseClientProvider.client,
    mediaRepository = mediaRepository,
    currentUserProvider = currentUserProvider
)
