package jr.brian.inindy.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import jr.brian.inindy.data.remote.chat.GroupMessageDto
import jr.brian.inindy.data.remote.chat.toDomain
import jr.brian.inindy.data.remote.post.toIsoString
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.model.GroupMessage
import jr.brian.inindy.domain.repository.ChatEvent
import jr.brian.inindy.domain.repository.GroupChatRepository
import jr.brian.inindy.util.appLog
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseGroupChatRepository(
    private val supabase: SupabaseClient,
    private val currentUserProvider: CurrentUserProvider
) : GroupChatRepository {

    private val log = appLog("SupabaseGroupChatRepository")

    // Live typing channels keyed by groupId, populated by observeTyping once
    // its channel is SUBSCRIBED and cleared in its finally block.
    // broadcastTyping looks up the channel here so it can push over the same
    // socket the flow is listening on — a fresh supabase.channel(name) built
    // on the fly is unsubscribed and falls back to the HTTP broadcast endpoint,
    // which requires RLS on realtime.messages that this project does not set.
    private val typingChannels = mutableMapOf<String, RealtimeChannel>()
    private val typingChannelsMutex = Mutex()

    override fun observeMessages(groupId: String): Flow<ChatEvent> = channelFlow {
        // Initial load (with sender join).
        val initial = fetchLatest(groupId).getOrElse {
            send(ChatEvent.Error(it))
            emptyList()
        }
        send(ChatEvent.InitialLoad(initial))

        val channel = supabase.channel(messagesChannelName(groupId))
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = MESSAGES_TABLE
            filter("group_id", FilterOperator.EQ, groupId)
        }
        // UNDISPATCHED — see SupabasePostRepository for the same reasoning.
        launch(start = CoroutineStart.UNDISPATCHED) {
            changes.collect { action ->
                when (action) {
                    is PostgresAction.Insert -> {
                        // Realtime payload only carries raw columns (no join), so
                        // fetch the full row with sender for a clean append.
                        val raw = runCatching {
                            action.decodeRecord<GroupMessageDto>()
                        }.getOrNull()
                        val id = raw?.id
                        if (id != null) {
                            fetchById(id).onSuccess { send(ChatEvent.NewMessage(it)) }
                        }
                    }
                    is PostgresAction.Update -> {
                        // For redaction/soft-delete the sender is unchanged; keep
                        // the join-less DTO and rely on the UI's own senderId
                        // lookup rather than a second round-trip.
                        val raw = runCatching {
                            action.decodeRecord<GroupMessageDto>()
                        }.getOrNull()
                        if (raw != null) {
                            send(ChatEvent.Updated(raw.toDomain()))
                        }
                    }
                    else -> {}
                }
            }
        }
        channel.subscribe(blockUntilSubscribed = true)

        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                supabase.realtime.removeChannel(channel)
            }
        }
    }

    // Typing runs on its own channel so its lifecycle doesn't tug on
    // observeMessages — if they shared one channel, cancelling either flow
    // would `removeChannel` and kill the other. Two channels means one extra
    // websocket subscription per open chat, which is negligible.
    override fun observeTyping(groupId: String): Flow<String> = channelFlow {
        val myUserId = currentUserProvider.get().userId

        val channel = supabase.channel(typingChannelName(groupId))
        val typing = channel.broadcastFlow<TypingEvent>(event = BROADCAST_TYPING)
        // UNDISPATCHED — broadcastFlow registers its callback on collect, and
        // typing bursts can arrive during the JOIN → SUBSCRIBED window.
        launch(start = CoroutineStart.UNDISPATCHED) {
            typing.collect { event ->
                if (event.userId != myUserId) send(event.userId)
            }
        }
        channel.subscribe(blockUntilSubscribed = true)
        typingChannelsMutex.withLock { typingChannels[groupId] = channel }

        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                typingChannelsMutex.withLock {
                    // Only clear if the entry is still ours; a rapid
                    // resubscribe could have swapped it out already.
                    if (typingChannels[groupId] === channel) {
                        typingChannels.remove(groupId)
                    }
                }
                supabase.realtime.removeChannel(channel)
            }
        }
    }

    override suspend fun loadOlder(
        groupId: String,
        beforeCreatedAt: Long,
        limit: Int
    ): Result<List<GroupMessage>> = runCatching {
        supabase.from(MESSAGES_TABLE).select(JOINED_COLUMNS) {
            filter {
                eq("group_id", groupId)
                filter("created_at", FilterOperator.LT, beforeCreatedAt.toIsoString())
            }
            order("created_at", order = Order.DESCENDING)
            limit(limit.toLong())
        }.decodeList<GroupMessageDto>().map { it.toDomain() }
    }

    override suspend fun sendMessage(groupId: String, body: String): Result<Unit> = runCatching {
        val userId = currentUserProvider.get().userId ?: error("No signed-in user")
        val trimmed = body.trim()
        require(trimmed.isNotEmpty()) { "Message body cannot be empty" }
        supabase.from(MESSAGES_TABLE).insert(
            MessageInsert(groupId = groupId, senderId = userId, body = trimmed)
        )
    }.map { }

    override suspend fun deleteMessage(messageId: String): Result<Unit> = runCatching {
        supabase.from(MESSAGES_TABLE).update({ set("deleted", true) }) {
            filter { eq("id", messageId) }
        }
    }.map { }

    override suspend fun broadcastTyping(groupId: String): Result<Unit> = runCatching {
        val userId = currentUserProvider.get().userId ?: return@runCatching
        // Send over the channel observeTyping already subscribed. Building a
        // fresh supabase.channel(name) here gives an UNSUBSCRIBED channel, so
        // broadcast falls back to the HTTP endpoint which requires RLS on
        // realtime.messages that this project does not set — the request 401s
        // and typing indicators never reach the other device.
        val channel = typingChannelsMutex.withLock { typingChannels[groupId] }
        if (channel == null) {
            // Typing is throttled and best-effort — dropping until observeTyping
            // has finished subscribing is the right call.
            log.d { "broadcastTyping — no subscribed channel for $groupId yet, dropping" }
            return@runCatching
        }
        channel.broadcast(event = BROADCAST_TYPING, message = TypingEvent(userId))
    }.onFailure { e ->
        // Don't let broadcast failures die silently — the whole point of Bug 2
        // being latent was runCatching eating the 401 from the HTTP fallback.
        log.w(e) { "broadcastTyping FAILED — groupId: $groupId" }
    }

    override suspend fun unreadCounts(): Result<Map<String, Int>> = runCatching {
        val rows = supabase.postgrest.rpc(function = "group_chat_unread_counts")
            .decodeList<UnreadCountRow>()
        rows.associate { it.groupId to it.unreadCount.toInt() }
    }

    override suspend fun markChatRead(groupId: String): Result<Unit> = runCatching {
        supabase.postgrest.rpc(
            function = "mark_group_chat_read",
            parameters = buildJsonObject { put("p_group_id", groupId) }
        )
    }.map { }

    private suspend fun fetchLatest(groupId: String): Result<List<GroupMessage>> = runCatching {
        supabase.from(MESSAGES_TABLE).select(JOINED_COLUMNS) {
            filter { eq("group_id", groupId) }
            order("created_at", order = Order.DESCENDING)
            limit(INITIAL_LIMIT)
        }.decodeList<GroupMessageDto>().map { it.toDomain() }
    }

    private suspend fun fetchById(id: String): Result<GroupMessage> = runCatching {
        supabase.from(MESSAGES_TABLE).select(JOINED_COLUMNS) {
            filter { eq("id", id) }
        }.decodeSingle<GroupMessageDto>().toDomain()
    }

    private fun messagesChannelName(groupId: String) = "group-chat-msg-$groupId"
    private fun typingChannelName(groupId: String) = "group-chat-typing-$groupId"

    @Serializable
    private data class MessageInsert(
        @SerialName("group_id") val groupId: String,
        @SerialName("sender_id") val senderId: String,
        @SerialName("body") val body: String
    )

    @Serializable
    private data class UnreadCountRow(
        @SerialName("group_id") val groupId: String,
        @SerialName("unread_count") val unreadCount: Long
    )

    @Serializable
    private data class TypingEvent(
        @SerialName("user_id") val userId: String
    )

    private companion object {
        const val MESSAGES_TABLE = "group_messages"
        const val BROADCAST_TYPING = "typing"
        const val INITIAL_LIMIT = 30L
        val JOINED_COLUMNS = Columns.raw(
            "*, sender:users!sender_id(id, full_name, avatar_url)"
        )
    }
}
