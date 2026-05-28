package jr.brian.inindy.presentation.me

import jr.brian.inindy.domain.model.Group
import jr.brian.inindy.domain.model.GroupInvite
import jr.brian.inindy.domain.model.GroupMember

data class GroupManagementUiState(
    val group: Group? = null,
    val members: List<GroupMember> = emptyList(),
    val pendingInvites: List<GroupInvite> = emptyList(),
    val isLoading: Boolean = true,
    val isGeneratingInvite: Boolean = false,
    val newInviteLink: String? = null,
    val error: String? = null,
    val deleted: Boolean = false
)
