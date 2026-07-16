package jr.brian.inindy.domain.repository

import jr.brian.inindy.domain.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeNotifications(): Flow<Result<List<Notification>>>
    suspend fun markAsRead(id: String): Result<Unit>
    suspend fun markAllRead(): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
    suspend fun setGroupMuted(groupId: String, muted: Boolean): Result<Unit>
    suspend fun isGroupMuted(groupId: String): Result<Boolean>
}
