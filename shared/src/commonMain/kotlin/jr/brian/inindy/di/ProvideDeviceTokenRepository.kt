package jr.brian.inindy.di

import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.DeviceTokenRepository

/**
 * Returns the active DeviceTokenRepository for this platform.
 * On Android/iOS this is SupabaseDeviceTokenRepository. wasmJs is disabled.
 */
expect fun provideDeviceTokenRepository(
    currentUserProvider: CurrentUserProvider
): DeviceTokenRepository
