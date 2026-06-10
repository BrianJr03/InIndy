package jr.brian.inindy.di

import jr.brian.inindy.data.repository.FakePostRepository
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.MediaRepository
import jr.brian.inindy.domain.repository.PostRepository

// supabase-kt has no wasmJs artifact, so this target keeps the fake.
@Suppress("UNUSED_PARAMETER")
actual fun providePostRepository(
    mediaRepository: MediaRepository,
    currentUserProvider: CurrentUserProvider
): PostRepository = FakePostRepository()
