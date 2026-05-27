package jr.brian.inindy.di

import jr.brian.inindy.data.repository.ExploreRepositoryImpl
import jr.brian.inindy.domain.repository.ExploreRepository
import jr.brian.inindy.domain.usecase.GetExplorePostsUseCase
import jr.brian.inindy.presentation.explore.ExploreViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<ExploreRepository> { ExploreRepositoryImpl() }
    factory { GetExplorePostsUseCase(get()) }
    viewModel { ExploreViewModel(get()) }
}
