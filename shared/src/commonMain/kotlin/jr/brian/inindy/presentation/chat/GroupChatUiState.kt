package jr.brian.inindy.presentation.chat

import jr.brian.inindy.domain.model.GroupMessage

data class GroupChatUiState(
    val isLoading: Boolean = true,
    val messages: List<GroupMessage> = emptyList(),
    val currentUserId: String? = null,
    val error: String? = null,
    // Empty when nothing has been typed since the last successful send/delete.
    val draft: String = "",
    val isSending: Boolean = false,
    // "Please rephrase" nudge from the client-side profanity pre-check.
    val profanityBlocked: Boolean = false,
    val isLoadingOlder: Boolean = false,
    val hasReachedOldest: Boolean = false,
    val typingUserIds: Set<String> = emptySet()
)
