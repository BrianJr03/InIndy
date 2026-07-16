package jr.brian.inindy.di

import jr.brian.inindy.domain.push.PushRegistrar
import jr.brian.inindy.domain.push.PushTokenProvider
import jr.brian.inindy.domain.repository.DeviceTokenRepository
import org.koin.dsl.module

val pushModule = module {
    single<DeviceTokenRepository> { provideDeviceTokenRepository(get()) }
    single<PushTokenProvider> { providePushTokenProvider() }
    single { PushRegistrar(get(), get(), get()) }
}
