package jr.brian.inindy.di

import jr.brian.inindy.data.remote.SupabaseClientProvider
import jr.brian.inindy.data.repository.SupabaseGroupRepository
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.GroupRepository
import jr.brian.inindy.domain.repository.MediaRepository
import jr.brian.inindy.domain.repository.PostRepository

actual fun provideGroupRepository(
    currentUserProvider: CurrentUserProvider,
    postRepository: PostRepository,
    mediaRepository: MediaRepository
): GroupRepository = SupabaseGroupRepository(
    supabase = SupabaseClientProvider.client,
    currentUserProvider = currentUserProvider,
    postRepository = postRepository,
    mediaRepository = mediaRepository
)
