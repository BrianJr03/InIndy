package jr.brian.inindy.di

import jr.brian.inindy.domain.repository.RsvpRepository
import jr.brian.inindy.domain.usecase.RsvpPostUseCase
import org.koin.dsl.module

val rsvpModule = module {
    single<RsvpRepository> { provideRsvpRepository(get()) }
    factory { RsvpPostUseCase(get()) }
}
