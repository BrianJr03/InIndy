package jr.brian.inindy.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.domain.model.Notification
import jr.brian.inindy.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val unreadCount: Int get() = notifications.count { !it.read }
}

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        notificationRepository.observeNotifications()
            .onEach { result ->
                result
                    .onSuccess { list ->
                        _uiState.value = _uiState.value.copy(
                            notifications = list,
                            isLoading = false,
                            error = null
                        )
                    }
                    .onFailure { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load notifications"
                        )
                    }
            }
            .launchIn(viewModelScope)
    }

    fun markAsRead(id: String) {
        val current = _uiState.value.notifications
        val already = current.firstOrNull { it.id == id }?.read == true
        if (already) return
        _uiState.value = _uiState.value.copy(
            notifications = current.map { if (it.id == id) it.copy(read = true) else it }
        )
        viewModelScope.launch { notificationRepository.markAsRead(id) }
    }

    fun markAllRead() {
        val current = _uiState.value.notifications
        if (current.none { !it.read }) return
        _uiState.value = _uiState.value.copy(
            notifications = current.map { it.copy(read = true) }
        )
        viewModelScope.launch { notificationRepository.markAllRead() }
    }
}
