package jr.brian.inindy.di

import jr.brian.inindy.data.remote.SupabaseClientProvider
import jr.brian.inindy.data.repository.SupabaseGroupChatRepository
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.GroupChatRepository

actual fun provideGroupChatRepository(
    currentUserProvider: CurrentUserProvider
): GroupChatRepository = SupabaseGroupChatRepository(
    supabase = SupabaseClientProvider.client,
    currentUserProvider = currentUserProvider
)
