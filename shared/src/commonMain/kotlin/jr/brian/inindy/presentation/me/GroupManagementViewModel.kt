package jr.brian.inindy.presentation.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.domain.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupManagementViewModel(
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupManagementUiState())
    val uiState: StateFlow<GroupManagementUiState> = _uiState.asStateFlow()

    private var loadedGroupId: String? = null

    fun load(groupId: String) {
        if (loadedGroupId == groupId && !_uiState.value.isLoading) return
        loadedGroupId = groupId
        viewModelScope.launch {
            _uiState.value = GroupManagementUiState(isLoading = true)
            val group = groupRepository.getGroup(groupId).getOrNull()
            val members = groupRepository.getGroupMembers(groupId).getOrDefault(emptyList())
            val invites = groupRepository.getPendingInvites(groupId).getOrDefault(emptyList())
            _uiState.value = GroupManagementUiState(
                group = group,
                members = members,
                pendingInvites = invites,
                isLoading = false
            )
        }
    }

    fun removeMember(userId: String) {
        val groupId = loadedGroupId ?: return
        viewModelScope.launch {
            groupRepository.removeMember(groupId, userId)
            refresh()
        }
    }

    fun generateInvite() {
        val groupId = loadedGroupId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingInvite = true)
            val link = groupRepository.generateInviteLink(groupId).getOrNull()
            _uiState.value = _uiState.value.copy(
                isGeneratingInvite = false,
                newInviteLink = link
            )
        }
    }

    fun dismissInviteLink() {
        _uiState.value = _uiState.value.copy(newInviteLink = null)
    }

    fun revokeInvite(inviteId: String) {
        viewModelScope.launch {
            groupRepository.revokeInvite(inviteId)
            refresh()
        }
    }

    fun deleteGroup() {
        val groupId = loadedGroupId ?: return
        viewModelScope.launch {
            groupRepository.deleteGroup(groupId).onSuccess {
                _uiState.value = _uiState.value.copy(deleted = true)
            }
        }
    }

    fun leaveGroup() {
        val groupId = loadedGroupId ?: return
        viewModelScope.launch {
            groupRepository.leaveGroup(groupId).onSuccess {
                _uiState.value = _uiState.value.copy(deleted = true)
            }
        }
    }

    private fun refresh() {
        loadedGroupId?.let {
            loadedGroupId = null
            load(it)
        }
    }
}
