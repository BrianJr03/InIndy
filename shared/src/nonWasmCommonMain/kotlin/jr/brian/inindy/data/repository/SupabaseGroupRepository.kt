package jr.brian.inindy.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.model.CreateGroupRequest
import jr.brian.inindy.domain.model.Group
import jr.brian.inindy.domain.model.GroupInvite
import jr.brian.inindy.domain.model.GroupMember
import jr.brian.inindy.domain.model.GroupRole
import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.repository.GroupRepository
import jr.brian.inindy.domain.repository.MediaRepository
import jr.brian.inindy.domain.repository.PostRepository
import jr.brian.inindy.util.currentTimeMillis
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalUuidApi::class)
class SupabaseGroupRepository(
    private val supabase: SupabaseClient,
    private val currentUserProvider: CurrentUserProvider,
    private val postRepository: PostRepository,
    private val mediaRepository: MediaRepository
) : GroupRepository {
    private val sharedScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sharedFlows = mutableMapOf<String, SharedFlow<List<Group>>>()
    private val sharedFlowsMutex = Mutex()

    override fun observeUserGroups(): Flow<List<Group>> = flow {
        val userId = currentUserProvider.get().userId
            ?: run {
                emit(emptyList())
                return@flow
            }
        emitAll(sharedUserGroupsFlow(userId))
    }

    private suspend fun sharedUserGroupsFlow(userId: String): SharedFlow<List<Group>> =
        sharedFlowsMutex.withLock {
            sharedFlows.getOrPut(userId) {
                buildUserGroupsFlow(userId).shareIn(
                    scope = sharedScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    replay = 1
                )
            }
        }

    private fun buildUserGroupsFlow(userId: String): Flow<List<Group>> = channelFlow {
        suspend fun emitLatest() {
            send(fetchUserGroups(userId).getOrElse { emptyList() })
        }

        emitLatest()

        val channel = supabase.channel("group-members-$userId-${Uuid.random()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = GROUP_MEMBERS_TABLE
            filter("user_id", FilterOperator.EQ, userId)
        }
        launch { changes.collect { emitLatest() } }
        channel.subscribe()

        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                supabase.realtime.removeChannel(channel)
            }
        }
    }

    override suspend fun getUserGroups(): Result<List<Group>> = runCatching {
        val userId = currentUserProvider.get().userId
            ?: throw IllegalStateException("No signed-in user")
        fetchUserGroups(userId).getOrThrow()
    }

    private suspend fun fetchUserGroups(userId: String): Result<List<Group>> = runCatching {
        supabase.from(GROUP_MEMBERS_TABLE)
            .select(Columns.raw("role, group:groups(*)")) {
                filter { eq("user_id", userId) }
            }
            .decodeList<MembershipRow>()
            .mapNotNull { row -> row.group?.toGroup(roleOf(row.role)) }
    }

    override suspend fun searchGroups(query: String): Result<List<Group>> = runCatching {
        val userId = currentUserProvider.get().userId
            ?: throw IllegalStateException("No signed-in user")

        val publicResults = supabase.from(GROUPS_TABLE)
            .select {
                filter {
                    ilike("name", "%$query%")
                    eq("is_open", true)
                }
                order("member_count", order = Order.DESCENDING)
                limit(SEARCH_LIMIT)
            }
            .decodeList<GroupDto>()
            .map { it.toGroup(GroupRole.MEMBER) }

        val mine = fetchUserGroups(userId).getOrDefault(emptyList())

        (mine + publicResults).distinctBy { it.id }
    }

    override suspend fun getGroupById(groupId: String): Result<Group> = runCatching {
        val userId = currentUserProvider.get().userId
            ?: throw IllegalStateException("No signed-in user")
        val dto = supabase.from(GROUPS_TABLE)
            .select { filter { eq("id", groupId) } }
            .decodeSingleOrNull<GroupDto>()
            ?: throw IllegalStateException("Group $groupId not found")
        val role = supabase.from(GROUP_MEMBERS_TABLE)
            .select(Columns.list("role")) {
                filter {
                    eq("group_id", groupId)
                    eq("user_id", userId)
                }
            }
            .decodeSingleOrNull<RoleRow>()
            ?.role
            ?.let { roleOf(it) }
            ?: GroupRole.MEMBER
        dto.toGroup(role)
    }

    override suspend fun createGroup(request: CreateGroupRequest): Result<Group> = runCatching {
        val userId = currentUserProvider.get().userId
            ?: throw IllegalStateException("No signed-in user")

        val coverUrl = when {
            request.coverImageUri == null -> null
            request.coverImageUri.startsWith("http") -> request.coverImageUri
            else -> mediaRepository.uploadGroupCover(request.coverImageUri).getOrThrow()
        }

        val inserted = supabase.from(GROUPS_TABLE)
            .insert(
                GroupInsertDto(
                    name = request.name,
                    description = request.description,
                    coverUrl = coverUrl,
                    createdBy = userId,
                    isOpen = false
                )
            ) { select() }
            .decodeSingle<GroupDto>()

        supabase.from(GROUP_MEMBERS_TABLE).insert(
            GroupMemberInsertDto(
                groupId = inserted.id,
                userId = userId,
                role = GroupRole.ADMIN.name.lowercase()
            )
        )

        inserted.toGroup(GroupRole.ADMIN)
    }

    override suspend fun getGroupMembers(groupId: String): Result<List<GroupMember>> = runCatching {
        supabase.from(GROUP_MEMBERS_TABLE)
            .select(Columns.raw("user_id, group_id, role, joined_at, user:users(id, full_name, avatar_url)")) {
                filter { eq("group_id", groupId) }
            }
            .decodeList<GroupMemberDto>()
            .map { dto ->
                GroupMember(
                    userId = dto.userId,
                    displayName = dto.user?.fullName ?: "Unknown",
                    avatarUrl = dto.user?.avatarUrl,
                    role = roleOf(dto.role),
                    joinedAt = parseIso8601UtcSafe(dto.joinedAt)
                )
            }
    }

    override suspend fun getGroupPosts(groupId: String, limit: Int): Result<List<Post>> = runCatching {
        postRepository.getGroupFeed(groupId).getOrDefault(emptyList()).take(limit)
    }

    override suspend fun getPendingInvites(groupId: String): Result<List<GroupInvite>> = runCatching {
        val now = currentTimeMillis()
        supabase.from(GROUP_INVITES_TABLE)
            .select { filter { eq("group_id", groupId) } }
            .decodeList<GroupInviteDto>()
            .map { dto ->
                GroupInvite(
                    id = dto.id,
                    groupId = dto.groupId,
                    invitedBy = dto.invitedBy,
                    token = dto.token,
                    createdAt = parseIso8601UtcSafe(dto.createdAt),
                    expiresAt = parseIso8601UtcSafe(dto.expiresAt)
                )
            }
            .filter { it.expiresAt > now }
    }

    override suspend fun removeMember(groupId: String, userId: String): Result<Unit> = runCatching {
        supabase.from(GROUP_MEMBERS_TABLE).delete {
            filter {
                eq("group_id", groupId)
                eq("user_id", userId)
            }
        }
        val currentCount = supabase.from(GROUPS_TABLE)
            .select(Columns.list("member_count")) {
                filter { eq("id", groupId) }
            }
            .decodeSingleOrNull<MemberCountRow>()
            ?.memberCount
            ?: 0
        supabase.from(GROUPS_TABLE).update({
            set("member_count", (currentCount - 1).coerceAtLeast(0))
        }) {
            filter { eq("id", groupId) }
        }
    }

    override suspend fun generateInviteLink(groupId: String): Result<String> = runCatching {
        val userId = currentUserProvider.get().userId
            ?: throw IllegalStateException("No signed-in user")
        val token = Uuid.random().toString()
        val expiresAtMs = currentTimeMillis() + 7L * 86_400_000L
        supabase.from(GROUP_INVITES_TABLE).insert(
            GroupInviteInsertDto(
                groupId = groupId,
                invitedBy = userId,
                token = token,
                expiresAt = formatIso8601Utc(expiresAtMs)
            )
        )
        "https://inindy.app/i/$token"
    }

    override suspend fun revokeInvite(inviteId: String): Result<Unit> = runCatching {
        supabase.from(GROUP_INVITES_TABLE).delete {
            filter { eq("id", inviteId) }
        }
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> = runCatching {
        supabase.from(GROUPS_TABLE).delete {
            filter { eq("id", groupId) }
        }
    }

    override suspend fun leaveGroup(groupId: String): Result<Unit> = runCatching {
        val userId = currentUserProvider.get().userId
            ?: throw IllegalStateException("No signed-in user")
        removeMember(groupId, userId).getOrThrow()
    }

    // ── Mapping helpers ──────────────────────────────────────────────────────

    private fun GroupDto.toGroup(role: GroupRole): Group = Group(
        id = id,
        name = name,
        description = description,
        coverUrl = coverUrl,
        createdBy = createdBy,
        isOpen = isOpen,
        memberCount = memberCount,
        role = role,
        createdAt = parseIso8601UtcSafe(createdAt)
    )

    private fun roleOf(name: String): GroupRole =
        runCatching { GroupRole.valueOf(name.uppercase()) }.getOrDefault(GroupRole.MEMBER)

    // ── ISO 8601 helpers ─────────────────────────────────────────────────────
    // Supabase emits timestamptz as "YYYY-MM-DDTHH:MM:SS[.fff]+00:00" — UTC.
    // Inlined here to avoid pulling in kotlinx-datetime.

    private fun parseIso8601UtcSafe(iso: String): Long =
        runCatching { parseIso8601Utc(iso) }.getOrDefault(0L)

    private fun parseIso8601Utc(iso: String): Long {
        val year = iso.substring(0, 4).toInt()
        val month = iso.substring(5, 7).toInt()
        val day = iso.substring(8, 10).toInt()
        val hour = iso.substring(11, 13).toInt()
        val minute = iso.substring(14, 16).toInt()
        val second = iso.substring(17, 19).toInt()
        return civilToEpochDays(year, month, day) * 86_400_000L +
                hour * 3_600_000L +
                minute * 60_000L +
                second * 1_000L
    }

    private fun formatIso8601Utc(epochMs: Long): String {
        val days = epochMs.floorDiv(86_400_000L)
        val remainderMs = epochMs - days * 86_400_000L
        val (y, m, d) = epochDaysToYmd(days)
        val hour = (remainderMs / 3_600_000L).toInt()
        val minute = ((remainderMs / 60_000L) % 60L).toInt()
        val second = ((remainderMs / 1_000L) % 60L).toInt()
        return buildString {
            append(y.toString().padStart(4, '0'))
            append('-').append(m.toString().padStart(2, '0'))
            append('-').append(d.toString().padStart(2, '0'))
            append('T')
            append(hour.toString().padStart(2, '0'))
            append(':').append(minute.toString().padStart(2, '0'))
            append(':').append(second.toString().padStart(2, '0'))
            append('Z')
        }
    }

    // Howard Hinnant's civil calendar algorithms.
    private fun civilToEpochDays(year: Int, month: Int, day: Int): Long {
        val y = if (month <= 2) year - 1 else year
        val era = if (y >= 0) y / 400 else (y - 399) / 400
        val yoe = (y - era * 400).toLong()
        val mp = if (month > 2) month - 3 else month + 9
        val doy = (153L * mp + 2L) / 5L + day.toLong() - 1L
        val doe = yoe * 365L + yoe / 4L - yoe / 100L + doy
        return era * 146097L + doe - 719468L
    }

    private fun epochDaysToYmd(days: Long): Triple<Int, Int, Int> {
        val z = days + 719468L
        val era = if (z >= 0) z / 146097L else (z - 146096L) / 146097L
        val doe = (z - era * 146097L).toInt()
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = (yoe + era * 400L).toInt()
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        val year = if (m <= 2) y + 1 else y
        return Triple(year, m, d)
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    @Serializable
    private data class GroupDto(
        @SerialName("id") val id: String,
        @SerialName("name") val name: String,
        @SerialName("description") val description: String? = null,
        @SerialName("cover_url") val coverUrl: String? = null,
        @SerialName("created_by") val createdBy: String,
        @SerialName("is_open") val isOpen: Boolean = false,
        @SerialName("member_count") val memberCount: Int = 1,
        @SerialName("created_at") val createdAt: String,
        @SerialName("role") val role: String? = null
    )

    @Serializable
    private data class MembershipRow(
        @SerialName("role") val role: String,
        @SerialName("group") val group: GroupDto? = null
    )

    @Serializable
    private data class GroupMemberDto(
        @SerialName("user_id") val userId: String,
        @SerialName("group_id") val groupId: String,
        @SerialName("role") val role: String,
        @SerialName("joined_at") val joinedAt: String,
        @SerialName("user") val user: UserJoinDto? = null
    )

    @Serializable
    private data class UserJoinDto(
        @SerialName("id") val id: String,
        @SerialName("full_name") val fullName: String? = null,
        @SerialName("avatar_url") val avatarUrl: String? = null
    )

    @Serializable
    private data class GroupInviteDto(
        @SerialName("id") val id: String,
        @SerialName("group_id") val groupId: String,
        @SerialName("invited_by") val invitedBy: String,
        @SerialName("token") val token: String,
        @SerialName("created_at") val createdAt: String,
        @SerialName("expires_at") val expiresAt: String
    )

    @Serializable
    private data class GroupInsertDto(
        @SerialName("name") val name: String,
        @SerialName("description") val description: String?,
        @SerialName("cover_url") val coverUrl: String?,
        @SerialName("created_by") val createdBy: String,
        @SerialName("is_open") val isOpen: Boolean
    )

    @Serializable
    private data class GroupMemberInsertDto(
        @SerialName("group_id") val groupId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("role") val role: String
    )

    @Serializable
    private data class GroupInviteInsertDto(
        @SerialName("group_id") val groupId: String,
        @SerialName("invited_by") val invitedBy: String,
        @SerialName("token") val token: String,
        @SerialName("expires_at") val expiresAt: String
    )

    @Serializable
    private data class RoleRow(@SerialName("role") val role: String)

    @Serializable
    private data class MemberCountRow(@SerialName("member_count") val memberCount: Int)

    private companion object {
        const val GROUPS_TABLE = "groups"
        const val GROUP_MEMBERS_TABLE = "group_members"
        const val GROUP_INVITES_TABLE = "group_invites"
        const val SEARCH_LIMIT = 20L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
