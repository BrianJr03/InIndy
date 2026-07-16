package jr.brian.inindy.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import jr.brian.inindy.data.remote.notification.NotificationDto
import jr.brian.inindy.data.remote.notification.toDomain
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.model.Notification
import jr.brian.inindy.domain.repository.NotificationRepository
import jr.brian.inindy.util.appLog
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalUuidApi::class)
class SupabaseNotificationRepository(
    private val supabase: SupabaseClient,
    private val currentUserProvider: CurrentUserProvider
) : NotificationRepository {

    private val log = appLog("SupabaseNotificationRepository")

    override fun observeNotifications(): Flow<Result<List<Notification>>> = channelFlow {
        val userId = currentUserProvider.get().userId
        if (userId == null) {
            log.d { "observeNotifications — no signed-in user, emitting empty" }
            send(Result.success(emptyList()))
            return@channelFlow
        }

        suspend fun emitLatest() {
            send(fetchNotifications(userId))
        }

        emitLatest()

        val channel = supabase.channel("notifications-$userId-${Uuid.random()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = NOTIFICATIONS_TABLE
            filter("user_id", FilterOperator.EQ, userId)
        }
        launch {
            changes.collect { action ->
                when (action) {
                    is PostgresAction.Insert,
                    is PostgresAction.Update,
                    is PostgresAction.Delete -> emitLatest()
                    else -> {}
                }
            }
        }
        channel.subscribe()

        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                supabase.realtime.removeChannel(channel)
            }
        }
    }

    override suspend fun markAsRead(id: String): Result<Unit> = runCatching {
        supabase.from(NOTIFICATIONS_TABLE).update({ set("read", true) }) {
            filter { eq("id", id) }
        }
    }.map { }

    override suspend fun delete(id: String): Result<Unit> = runCatching {
        val userId = currentUserProvider.get().userId ?: error("No signed-in user")
        supabase.from(NOTIFICATIONS_TABLE).delete {
            filter {
                eq("id", id)
                eq("user_id", userId)
            }
        }
    }.map { }

    override suspend fun markAllRead(): Result<Unit> = runCatching {
        val userId = currentUserProvider.get().userId ?: error("No signed-in user")
        supabase.from(NOTIFICATIONS_TABLE).update({ set("read", true) }) {
            filter {
                eq("user_id", userId)
                eq("read", false)
            }
        }
    }.map { }

    override suspend fun setGroupMuted(groupId: String, muted: Boolean): Result<Unit> =
        runCatching {
            supabase.postgrest.rpc(
                function = "set_group_notifications_muted",
                parameters = buildJsonObject {
                    put("p_group_id", groupId)
                    put("p_muted", muted)
                }
            )
        }.map { }

    override suspend fun isGroupMuted(groupId: String): Result<Boolean> = runCatching {
        val userId = currentUserProvider.get().userId ?: error("No signed-in user")
        val row = supabase.from(GROUP_MEMBERS_TABLE)
            .select(Columns.raw("notifications_muted")) {
                filter {
                    eq("group_id", groupId)
                    eq("user_id", userId)
                }
                limit(1)
            }
            .decodeSingleOrNull<MutedRow>()
        row?.notificationsMuted ?: false
    }

    private suspend fun fetchNotifications(userId: String): Result<List<Notification>> =
        runCatching {
            supabase.from(NOTIFICATIONS_TABLE).select(JOINED_COLUMNS) {
                filter { eq("user_id", userId) }
                order("created_at", order = Order.DESCENDING)
                limit(FEED_LIMIT)
            }.decodeList<NotificationDto>().map { it.toDomain() }
        }

    @Serializable
    private data class MutedRow(
        @SerialName("notifications_muted") val notificationsMuted: Boolean
    )

    private companion object {
        const val NOTIFICATIONS_TABLE = "notifications"
        const val GROUP_MEMBERS_TABLE = "group_members"
        const val FEED_LIMIT = 100L
        // `actor` and `user_id` both reference users(id), so PostgREST needs a
        // hint to pick the actor FK. `users!actor_id` disambiguates by the local
        // FK column name.
        val JOINED_COLUMNS = Columns.raw(
            "*, actor:users!actor_id(id, full_name, avatar_url), group:groups(id, name)"
        )
    }
}
