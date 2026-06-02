package jr.brian.inindy.di

import jr.brian.inindy.domain.CurrentUserProvider
import org.koin.dsl.module

val coreModule = module {
    single { CurrentUserProvider(get()) }
}
