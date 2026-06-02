package jr.brian.inindy.data.repository

import jr.brian.inindy.domain.model.CreateGroupRequest
import jr.brian.inindy.domain.model.Group
import jr.brian.inindy.domain.model.GroupInvite
import jr.brian.inindy.domain.model.GroupMember
import jr.brian.inindy.domain.model.GroupRole
import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.repository.GroupRepository
import jr.brian.inindy.domain.repository.PostRepository
import jr.brian.inindy.util.currentTimeMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeGroupRepository(
    private val postRepository: PostRepository
) : GroupRepository {
    private val groupsState = MutableStateFlow(buildSeedGroups())
    private val membersState = MutableStateFlow(buildSeedMembers())
    private val invitesState = MutableStateFlow(buildSeedInvites())

    override fun observeUserGroups(): Flow<List<Group>> = groupsState.asStateFlow()

    override suspend fun getUserGroups(): Result<List<Group>> {
        delay(SHORT_DELAY_MS)
        return Result.success(groupsState.value)
    }

    override suspend fun searchGroups(query: String): Result<List<Group>> {
        delay(SHORT_DELAY_MS)
        val now = currentTimeMillis()
        val publicPool = listOf(
            Group(
                id = "g-fountain-square-crew",
                name = "Fountain Square Crew",
                description = "Live music, late food, weekend wanders.",
                coverUrl = null,
                createdBy = "u-21",
                isOpen = true,
                memberCount = 32,
                role = GroupRole.MEMBER,
                createdAt = now - 90L * 86_400_000L
            ),
            Group(
                id = "g-indy-cyclists",
                name = "Indy Cyclists",
                description = "Weekend rides across Marion County.",
                coverUrl = null,
                createdBy = "u-31",
                isOpen = true,
                memberCount = 124,
                role = GroupRole.MEMBER,
                createdAt = now - 120L * 86_400_000L
            ),
            Group(
                id = "g-sunday-hikers",
                name = "Sunday Hikers",
                description = "Trails within an hour of Indy.",
                coverUrl = null,
                createdBy = "u-44",
                isOpen = true,
                memberCount = 47,
                role = GroupRole.MEMBER,
                createdAt = now - 75L * 86_400_000L
            ),
            Group(
                id = "g-mass-ave-makers",
                name = "Mass Ave Makers",
                description = "Local creatives sharing studio time.",
                coverUrl = null,
                createdBy = "u-55",
                isOpen = true,
                memberCount = 18,
                role = GroupRole.MEMBER,
                createdAt = now - 50L * 86_400_000L
            ),
            Group(
                id = "g-irvington-readers",
                name = "Irvington Readers",
                description = "Monthly book swaps at the library.",
                coverUrl = null,
                createdBy = "u-66",
                isOpen = true,
                memberCount = 21,
                role = GroupRole.MEMBER,
                createdAt = now - 40L * 86_400_000L
            )
        )
        val combined = groupsState.value + publicPool
        val filtered = if (query.isBlank()) {
            combined
        } else {
            combined.filter { it.name.contains(query, ignoreCase = true) }
        }
        return Result.success(filtered)
    }

    override suspend fun getGroupById(groupId: String): Result<Group> {
        delay(SHORT_DELAY_MS)
        val group = groupsState.value.firstOrNull { it.id == groupId }
            ?: return Result.failure(IllegalStateException("Group $groupId not found"))
        return Result.success(group)
    }

    override suspend fun createGroup(request: CreateGroupRequest): Result<Group> {
        delay(NETWORK_DELAY_MS)
        val now = currentTimeMillis()
        val group = Group(
            id = "g-$now",
            name = request.name,
            description = request.description,
            coverUrl = request.coverImageUri,
            createdBy = ME_USER_ID,
            isOpen = false,
            memberCount = 1,
            role = GroupRole.ADMIN,
            createdAt = now
        )
        groupsState.value = listOf(group) + groupsState.value
        membersState.value += (group.id to listOf(
                    GroupMember(
                        userId = ME_USER_ID,
                        displayName = "You",
                        avatarUrl = null,
                        role = GroupRole.ADMIN,
                        joinedAt = now
                    )
                ))
        return Result.success(group)
    }

    override suspend fun getGroupMembers(groupId: String): Result<List<GroupMember>> {
        delay(SHORT_DELAY_MS)
        return Result.success(membersState.value[groupId].orEmpty())
    }

    override suspend fun getGroupPosts(groupId: String, limit: Int): Result<List<Post>> {
        val feed = postRepository.getGroupFeed(groupId).getOrDefault(emptyList())
        return Result.success(feed.take(limit))
    }

    override suspend fun getPendingInvites(groupId: String): Result<List<GroupInvite>> {
        delay(SHORT_DELAY_MS)
        return Result.success(invitesState.value[groupId].orEmpty())
    }

    override suspend fun removeMember(groupId: String, userId: String): Result<Unit> {
        delay(SHORT_DELAY_MS)
        val current = membersState.value.toMutableMap()
        current[groupId] = current[groupId].orEmpty().filterNot { it.userId == userId }
        membersState.value = current
        groupsState.value = groupsState.value.map {
            if (it.id == groupId) it.copy(memberCount = (it.memberCount - 1).coerceAtLeast(0)) else it
        }
        return Result.success(Unit)
    }

    override suspend fun generateInviteLink(groupId: String): Result<String> {
        delay(SHORT_DELAY_MS)
        return Result.success("https://inindy.app/i/${groupId}-${currentTimeMillis()}")
    }

    override suspend fun revokeInvite(inviteId: String): Result<Unit> {
        delay(SHORT_DELAY_MS)
        val current = invitesState.value.toMutableMap()
        current.keys.forEach { gid ->
            current[gid] = current[gid].orEmpty().filterNot { it.id == inviteId }
        }
        invitesState.value = current
        return Result.success(Unit)
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> {
        delay(NETWORK_DELAY_MS)
        groupsState.value = groupsState.value.filterNot { it.id == groupId }
        membersState.value -= groupId
        invitesState.value -= groupId
        return Result.success(Unit)
    }

    override suspend fun leaveGroup(groupId: String): Result<Unit> {
        delay(SHORT_DELAY_MS)
        return removeMember(groupId, ME_USER_ID).also {
            groupsState.value = groupsState.value.filterNot { it.id == groupId }
        }
    }

    private fun buildSeedGroups(): List<Group> {
        val now = currentTimeMillis()
        return listOf(
            Group(
                id = "g-broad-ripple-runners",
                name = "Broad Ripple Runners",
                description = "Weekly Saturday morning runs on the Monon.",
                coverUrl = null,
                createdBy = ME_USER_ID,
                isOpen = false,
                memberCount = 8,
                role = GroupRole.ADMIN,
                createdAt = now - 30L * 86_400_000L
            ),
            Group(
                id = "g-indy-picnic-club",
                name = "Indy Picnic Club",
                description = "Spreading blankets across every park in Marion County.",
                coverUrl = null,
                createdBy = "u-9",
                isOpen = false,
                memberCount = 14,
                role = GroupRole.MEMBER,
                createdAt = now - 60L * 86_400_000L
            )
        )
    }

    private fun buildSeedMembers(): Map<String, List<GroupMember>> {
        val now = currentTimeMillis()
        return mapOf(
            "g-broad-ripple-runners" to listOf(
                GroupMember(ME_USER_ID, "You", null, GroupRole.ADMIN, now - 30L * 86_400_000L),
                GroupMember("u-2", "Marcus T.", null, GroupRole.MEMBER, now - 20L * 86_400_000L),
                GroupMember("u-3", "Priya K.", null, GroupRole.MEMBER, now - 15L * 86_400_000L),
                GroupMember("u-4", "Jordan O.", null, GroupRole.MEMBER, now - 8L * 86_400_000L)
            ),
            "g-indy-picnic-club" to listOf(
                GroupMember("u-9", "Michelle W.", null, GroupRole.ADMIN, now - 60L * 86_400_000L),
                GroupMember(ME_USER_ID, "You", null, GroupRole.MEMBER, now - 12L * 86_400_000L),
                GroupMember("u-7", "Sam B.", null, GroupRole.MEMBER, now - 9L * 86_400_000L)
            )
        )
    }

    private fun buildSeedInvites(): Map<String, List<GroupInvite>> {
        val now = currentTimeMillis()
        val week = 7L * 86_400_000L
        return mapOf(
            "g-broad-ripple-runners" to listOf(
                GroupInvite(
                    id = "inv-1",
                    groupId = "g-broad-ripple-runners",
                    invitedBy = ME_USER_ID,
                    token = "abc123token",
                    createdAt = now - 86_400_000L,
                    expiresAt = now + week - 86_400_000L
                ),
                GroupInvite(
                    id = "inv-2",
                    groupId = "g-broad-ripple-runners",
                    invitedBy = ME_USER_ID,
                    token = "def456token",
                    createdAt = now - 2L * 3_600_000L,
                    expiresAt = now + week - 2L * 3_600_000L
                )
            )
        )
    }

    private companion object {
        const val ME_USER_ID = "me"
        const val SHORT_DELAY_MS = 200L
        const val NETWORK_DELAY_MS = 600L
    }
}
