package jr.brian.inindy.di

import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.NotificationRepository

/**
 * Returns the active NotificationRepository for this platform.
 * On Android/iOS this is SupabaseNotificationRepository. wasmJs is disabled.
 */
expect fun provideNotificationRepository(
    currentUserProvider: CurrentUserProvider
): NotificationRepository
