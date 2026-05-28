package jr.brian.inindy.di

import jr.brian.inindy.data.repository.FakeAuthRepository
import jr.brian.inindy.data.repository.FakeOnboardingRepository
import jr.brian.inindy.domain.repository.AuthRepository
import jr.brian.inindy.domain.repository.OnboardingRepository
import jr.brian.inindy.presentation.auth.AuthViewModel
import jr.brian.inindy.presentation.onboarding.OnboardingViewModel
import jr.brian.inindy.presentation.root.RootViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    single<AuthRepository> { FakeAuthRepository(get()) }
    single<OnboardingRepository> { FakeOnboardingRepository() }
    viewModel { AuthViewModel(get(), get()) }
    viewModel { OnboardingViewModel(get()) }
    viewModel { RootViewModel(get()) }
}
