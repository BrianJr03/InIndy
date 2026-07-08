package jr.brian.inindy.di

import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.GroupChatRepository

/**
 * Returns the active GroupChatRepository for this platform.
 * On Android/iOS this is SupabaseGroupChatRepository. wasmJs is disabled.
 */
expect fun provideGroupChatRepository(
    currentUserProvider: CurrentUserProvider
): GroupChatRepository
