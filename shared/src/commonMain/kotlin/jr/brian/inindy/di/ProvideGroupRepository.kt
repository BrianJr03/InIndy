package jr.brian.inindy.di

import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.GroupRepository
import jr.brian.inindy.domain.repository.MediaRepository
import jr.brian.inindy.domain.repository.PostRepository

/**
 * Returns the active GroupRepository for this platform.
 * On Android/iOS/JS this is SupabaseGroupRepository.
 * On wasmJs this falls back to FakeGroupRepository (supabase-kt has no wasmJs artifact).
 */
expect fun provideGroupRepository(
    currentUserProvider: CurrentUserProvider,
    postRepository: PostRepository,
    mediaRepository: MediaRepository
): GroupRepository
