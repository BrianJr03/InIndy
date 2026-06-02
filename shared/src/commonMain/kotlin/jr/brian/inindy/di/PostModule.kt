package jr.brian.inindy.di

import jr.brian.inindy.data.location.AddressSearchDataSource
import jr.brian.inindy.data.location.FakeAddressSearchDataSource
import jr.brian.inindy.data.location.LocationProvider
import jr.brian.inindy.data.repository.FakeAttendanceRepository
import jr.brian.inindy.data.repository.FakeGroupRepository
import jr.brian.inindy.data.repository.FakeMediaRepository
import jr.brian.inindy.data.repository.FakePostRepository
import jr.brian.inindy.domain.repository.AttendanceRepository
import jr.brian.inindy.domain.repository.GroupRepository
import jr.brian.inindy.domain.repository.MediaRepository
import jr.brian.inindy.domain.repository.PostRepository
import jr.brian.inindy.presentation.creategroup.CreateGroupViewModel
import jr.brian.inindy.presentation.createpost.CreatePostViewModel
import jr.brian.inindy.presentation.me.GroupManagementViewModel
import jr.brian.inindy.presentation.me.MeViewModel
import jr.brian.inindy.presentation.post.PostDetailViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val postModule = module {
    single<PostRepository> { FakePostRepository() }
    single<GroupRepository> { FakeGroupRepository() }
    single<AttendanceRepository> { FakeAttendanceRepository() }
    single<MediaRepository> { FakeMediaRepository() }
    single<AddressSearchDataSource> { FakeAddressSearchDataSource() }
    single { LocationProvider() }
    viewModel { MeViewModel(get(), get(), get()) }
    viewModel { CreatePostViewModel(get(), get(), get(), get()) }
    viewModel { CreateGroupViewModel(get(), get()) }
    viewModel { GroupManagementViewModel(get()) }
    viewModel { PostDetailViewModel(get(), get()) }
}
