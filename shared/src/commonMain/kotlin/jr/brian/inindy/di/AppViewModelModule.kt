package jr.brian.inindy.di

import jr.brian.inindy.presentation.app.AppViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appViewModelModule = module {
    viewModel { AppViewModel(get(), get()) }
}
