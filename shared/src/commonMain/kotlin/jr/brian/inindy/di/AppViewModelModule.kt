package jr.brian.inindy.di

import jr.brian.inindy.navigation.DeepLinkBus
import jr.brian.inindy.presentation.app.AppViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appViewModelModule = module {
    single { DeepLinkBus() }
    viewModel { AppViewModel(get(), get(), get()) }
}
