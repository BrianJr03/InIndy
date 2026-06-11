package jr.brian.inindy.di

import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.PostRepository

/**
 * Returns the active PostRepository for this platform.
 * On Android/iOS/JS this is SupabasePostRepository.
 * On wasmJs this falls back to FakePostRepository (supabase-kt has no wasmJs artifact).
 */
expect fun providePostRepository(
    currentUserProvider: CurrentUserProvider
): PostRepository
