package jr.brian.inindy.di

import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.NotificationSettingsRepository

/**
 * Returns the active NotificationSettingsRepository for this platform.
 * On Android/iOS this is SupabaseNotificationSettingsRepository. wasmJs is disabled.
 */
expect fun provideNotificationSettingsRepository(
    currentUserProvider: CurrentUserProvider
): NotificationSettingsRepository
