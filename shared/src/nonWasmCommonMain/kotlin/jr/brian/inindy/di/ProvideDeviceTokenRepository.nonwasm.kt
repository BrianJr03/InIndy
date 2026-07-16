package jr.brian.inindy.di

import jr.brian.inindy.data.remote.SupabaseClientProvider
import jr.brian.inindy.data.repository.SupabaseDeviceTokenRepository
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.DeviceTokenRepository

actual fun provideDeviceTokenRepository(
    currentUserProvider: CurrentUserProvider
): DeviceTokenRepository = SupabaseDeviceTokenRepository(
    supabase = SupabaseClientProvider.client,
    currentUserProvider = currentUserProvider
)
