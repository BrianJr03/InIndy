package jr.brian.inindy.di

import jr.brian.inindy.domain.repository.NotificationRepository
import jr.brian.inindy.presentation.notifications.NotificationsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val notificationModule = module {
    single<NotificationRepository> { provideNotificationRepository(get()) }
    viewModel { NotificationsViewModel(get()) }
}
