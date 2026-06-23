package jr.brian.inindy.di

import jr.brian.inindy.data.location.AddressSearchDataSource
import jr.brian.inindy.data.location.FakeAddressSearchDataSource
import jr.brian.inindy.data.location.LocationProvider
import jr.brian.inindy.domain.repository.AttendanceRepository
import jr.brian.inindy.domain.repository.GroupRepository
import jr.brian.inindy.domain.repository.PostRepository
import jr.brian.inindy.domain.repository.ProfileEditRepository
import jr.brian.inindy.presentation.creategroup.CreateGroupViewModel
import jr.brian.inindy.presentation.createpost.CreatePostViewModel
import jr.brian.inindy.presentation.me.GroupInviteViewModel
import jr.brian.inindy.presentation.me.GroupManagementViewModel
import jr.brian.inindy.presentation.me.MeViewModel
import jr.brian.inindy.presentation.post.PostDetailViewModel
import jr.brian.inindy.presentation.profileedit.ProfileEditViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val postModule = module {
    single<PostRepository> { providePostRepository(get()) }
    single<GroupRepository> { provideGroupRepository(get(), get(), get()) }
    single<AttendanceRepository> { provideAttendanceRepository(get()) }
    single<ProfileEditRepository> { provideProfileEditRepository(get(), get()) }
    single<AddressSearchDataSource> { FakeAddressSearchDataSource() }
    single { LocationProvider() }
    viewModel { MeViewModel(get(), get(), get(), get()) }
    viewModel { CreatePostViewModel(get(), get(), get(), get(), get()) }
    viewModel { CreateGroupViewModel(get(), get()) }
    viewModel { (groupId: String) -> GroupManagementViewModel(groupId, get(), get()) }
    viewModel { GroupInviteViewModel(get()) }
    viewModel { PostDetailViewModel(get(), get(), get(), get()) }
    viewModel { ProfileEditViewModel(get(), get()) }
}
