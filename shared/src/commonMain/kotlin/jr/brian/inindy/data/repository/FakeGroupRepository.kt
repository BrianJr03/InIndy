package jr.brian.inindy.data.repository

import jr.brian.inindy.domain.model.Group
import jr.brian.inindy.domain.model.GroupInvite
import jr.brian.inindy.domain.model.GroupMember
import jr.brian.inindy.domain.model.GroupRole
import jr.brian.inindy.domain.repository.GroupRepository
import jr.brian.inindy.util.currentTimeMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeGroupRepository : GroupRepository {

    private val groupsState = MutableStateFlow(buildSeedGroups())
    private val membersState = MutableStateFlow(buildSeedMembers())
    private val invitesState = MutableStateFlow(buildSeedInvites())

    override fun observeUserGroups(): Flow<List<Group>> = groupsState.asStateFlow()

    override suspend fun getUserGroups(): Result<List<Group>> {
        delay(SHORT_DELAY_MS)
        return Result.success(groupsState.value)
    }

    override suspend fun getGroup(groupId: String): Result<Group> {
        delay(SHORT_DELAY_MS)
        val group = groupsState.value.firstOrNull { it.id == groupId }
            ?: return Result.failure(IllegalStateException("Group $groupId not found"))
        return Result.success(group)
    }

    override suspend fun createGroup(name: String, description: String?): Result<Group> {
        delay(NETWORK_DELAY_MS)
        val now = currentTimeMillis()
        val group = Group(
            id = "g-$now",
            name = name,
            description = description,
            coverUrl = null,
            createdBy = ME_USER_ID,
            isOpen = false,
            memberCount = 1,
            role = GroupRole.ADMIN,
            createdAt = now
        )
        groupsState.value = listOf(group) + groupsState.value
        membersState.value = membersState.value + (group.id to listOf(
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
        membersState.value = membersState.value - groupId
        invitesState.value = invitesState.value - groupId
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
                GroupMember("u-9", "Audrea W.", null, GroupRole.ADMIN, now - 60L * 86_400_000L),
                GroupMember(ME_USER_ID, "You", null, GroupRole.MEMBER, now - 12L * 86_400_000L),
                GroupMember("u-7", "Sam B.", null, GroupRole.MEMBER, now - 9L * 86_400_000L)
            )
        )
    }

    private fun buildSeedInvites(): Map<String, List<GroupInvite>> {
        val now = currentTimeMillis()
        val expires = now + 7L * 86_400_000L
        return mapOf(
            "g-broad-ripple-runners" to listOf(
                GroupInvite("inv-1", "g-broad-ripple-runners", "casey@example.com", "tk-1", expires),
                GroupInvite("inv-2", "g-broad-ripple-runners", "ari@example.com", "tk-2", expires)
            )
        )
    }

    private companion object {
        const val ME_USER_ID = "me"
        const val SHORT_DELAY_MS = 200L
        const val NETWORK_DELAY_MS = 600L
    }
}
