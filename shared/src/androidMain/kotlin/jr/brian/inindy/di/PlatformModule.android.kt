package jr.brian.inindy.di

import jr.brian.inindy.data.local.TokenStorage
import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.data.local.UserPreferencesStoreImpl
import jr.brian.inindy.data.social.SocialAuthProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { TokenStorage(androidContext()) }
    single { SocialAuthProvider(androidContext()) }
    single<UserPreferencesStore> { UserPreferencesStoreImpl(androidContext()) }
}
