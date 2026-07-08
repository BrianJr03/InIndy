package jr.brian.inindy.domain.repository

import jr.brian.inindy.domain.model.GroupMessage
import kotlinx.coroutines.flow.Flow

sealed interface ChatEvent {
    data class InitialLoad(val messages: List<GroupMessage>) : ChatEvent
    data class NewMessage(val message: GroupMessage) : ChatEvent
    data class Updated(val message: GroupMessage) : ChatEvent
    data class Error(val throwable: Throwable) : ChatEvent
}

interface GroupChatRepository {
    /** Initial fetch + realtime append/update stream for a group's chat. */
    fun observeMessages(groupId: String): Flow<ChatEvent>

    /** Realtime broadcast stream — emits sender userIds who are typing. */
    fun observeTyping(groupId: String): Flow<String>

    suspend fun loadOlder(
        groupId: String,
        beforeCreatedAt: Long,
        limit: Int = 30
    ): Result<List<GroupMessage>>

    suspend fun sendMessage(groupId: String, body: String): Result<Unit>
    suspend fun deleteMessage(messageId: String): Result<Unit>

    /** Broadcast a "typing" event for the current user. Throttle at the caller. */
    suspend fun broadcastTyping(groupId: String): Result<Unit>

    // Unread indicators (C2).
    suspend fun unreadCounts(): Result<Map<String, Int>>
    suspend fun markChatRead(groupId: String): Result<Unit>
}
