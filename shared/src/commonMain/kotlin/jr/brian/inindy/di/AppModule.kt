package jr.brian.inindy.di

import jr.brian.inindy.data.repository.ExploreRepositoryImpl
import jr.brian.inindy.domain.repository.ExploreRepository
import jr.brian.inindy.domain.repository.PostRepository
import jr.brian.inindy.domain.usecase.GetExplorePostsUseCase
import jr.brian.inindy.presentation.explore.ExploreViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val exploreModule = module {
    single<ExploreRepository> { ExploreRepositoryImpl() }
    factory { GetExplorePostsUseCase(get()) }
    viewModel {
        ExploreViewModel(
            postRepository = get<PostRepository>(),
            rsvpPost = get(),
            groupRepository = get(),
            currentUserProvider = get(),
            userPreferencesStore = get()
        )
    }
}

val appModules: List<Module> = listOf(
    platformModule,
    coreModule,
    mediaModule,
    authModule,
    rsvpModule,
    postModule,
    exploreModule,
    appViewModelModule
)
