package jr.brian.inindy.presentation.me

import jr.brian.inindy.domain.model.Group
import jr.brian.inindy.domain.model.GroupInvite
import jr.brian.inindy.domain.model.GroupMember
import jr.brian.inindy.domain.model.GroupRole
import jr.brian.inindy.domain.model.Post

data class GroupManagementUiState(
    val group: Group? = null,
    val members: List<GroupMember> = emptyList(),
    val recentPosts: List<Post> = emptyList(),
    val pendingInvites: List<GroupInvite> = emptyList(),
    val currentUserRole: GroupRole = GroupRole.MEMBER,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isGeneratingInvite: Boolean = false,
    val inviteLink: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val deleted: Boolean = false
)
