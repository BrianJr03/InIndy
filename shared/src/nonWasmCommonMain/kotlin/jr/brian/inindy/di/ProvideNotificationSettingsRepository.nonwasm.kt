package jr.brian.inindy.di

import jr.brian.inindy.data.remote.SupabaseClientProvider
import jr.brian.inindy.data.repository.SupabaseNotificationSettingsRepository
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.NotificationSettingsRepository

actual fun provideNotificationSettingsRepository(
    currentUserProvider: CurrentUserProvider
): NotificationSettingsRepository = SupabaseNotificationSettingsRepository(
    supabase = SupabaseClientProvider.client,
    currentUserProvider = currentUserProvider
)
