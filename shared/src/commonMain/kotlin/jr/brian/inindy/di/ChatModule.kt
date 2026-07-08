package jr.brian.inindy.di

import jr.brian.inindy.data.chat.ProfanityFilter
import jr.brian.inindy.domain.repository.GroupChatRepository
import jr.brian.inindy.presentation.chat.GroupChatViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val chatModule = module {
    single<GroupChatRepository> { provideGroupChatRepository(get()) }
    single { ProfanityFilter() }
    viewModel { (groupId: String) ->
        GroupChatViewModel(
            groupId = groupId,
            chatRepository = get(),
            currentUserProvider = get(),
            groupRepository = get(),
            profanityFilter = get()
        )
    }
}
