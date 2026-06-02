package jr.brian.inindy.di

import jr.brian.inindy.data.repository.FakeAuthRepository
import jr.brian.inindy.data.repository.FakeOnboardingRepository
import jr.brian.inindy.domain.repository.AuthRepository
import jr.brian.inindy.domain.repository.OnboardingRepository
import jr.brian.inindy.presentation.auth.AuthViewModel
import jr.brian.inindy.presentation.onboarding.OnboardingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    single<AuthRepository> { FakeAuthRepository(get(), get()) }
    single<OnboardingRepository> { FakeOnboardingRepository(get()) }
    viewModel { AuthViewModel(get(), get()) }
    viewModel { OnboardingViewModel(get()) }
}
