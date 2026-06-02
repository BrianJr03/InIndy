package jr.brian.inindy.domain.repository

import jr.brian.inindy.domain.model.CreateGroupRequest
import jr.brian.inindy.domain.model.Group
import jr.brian.inindy.domain.model.GroupInvite
import jr.brian.inindy.domain.model.GroupMember
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    fun observeUserGroups(): Flow<List<Group>>
    suspend fun getUserGroups(): Result<List<Group>>
    suspend fun searchGroups(query: String): Result<List<Group>>
    suspend fun getGroup(groupId: String): Result<Group>
    suspend fun createGroup(request: CreateGroupRequest): Result<Group>
    suspend fun getGroupMembers(groupId: String): Result<List<GroupMember>>
    suspend fun getPendingInvites(groupId: String): Result<List<GroupInvite>>
    suspend fun removeMember(groupId: String, userId: String): Result<Unit>
    suspend fun generateInviteLink(groupId: String): Result<String>
    suspend fun revokeInvite(inviteId: String): Result<Unit>
    suspend fun deleteGroup(groupId: String): Result<Unit>
    suspend fun leaveGroup(groupId: String): Result<Unit>
}
