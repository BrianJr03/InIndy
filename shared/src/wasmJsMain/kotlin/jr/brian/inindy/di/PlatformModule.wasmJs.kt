package jr.brian.inindy.di

import jr.brian.inindy.data.local.TokenStorage
import jr.brian.inindy.data.social.SocialAuthProvider
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { TokenStorage() }
    single { SocialAuthProvider() }
}
