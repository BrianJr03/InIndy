package jr.brian.inindy.di

import jr.brian.inindy.domain.repository.AuthRepository
import jr.brian.inindy.domain.repository.OnboardingRepository
import jr.brian.inindy.presentation.auth.AuthViewModel
import jr.brian.inindy.presentation.onboarding.OnboardingViewModel
import jr.brian.inindy.presentation.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    // Resolved per platform: SupabaseAuthRepository on Android/iOS/JS,
    // FakeAuthRepository on wasmJs (no supabase-kt artifact there).
    single<AuthRepository> { provideAuthRepository(get(), get(), get()) }
    single<OnboardingRepository> { provideOnboardingRepository(get(), get()) }
    viewModel { AuthViewModel(get(), get()) }
    viewModel { OnboardingViewModel(get()) }
    viewModel { SettingsViewModel(get(), get()) }
}
