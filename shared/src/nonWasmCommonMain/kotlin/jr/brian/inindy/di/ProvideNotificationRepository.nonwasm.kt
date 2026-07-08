package jr.brian.inindy.di

import jr.brian.inindy.data.remote.SupabaseClientProvider
import jr.brian.inindy.data.repository.SupabaseNotificationRepository
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.NotificationRepository

actual fun provideNotificationRepository(
    currentUserProvider: CurrentUserProvider
): NotificationRepository = SupabaseNotificationRepository(
    supabase = SupabaseClientProvider.client,
    currentUserProvider = currentUserProvider
)
