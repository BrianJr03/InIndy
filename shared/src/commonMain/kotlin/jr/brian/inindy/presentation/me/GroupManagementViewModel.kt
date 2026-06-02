package jr.brian.inindy.presentation.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.domain.model.GroupRole
import jr.brian.inindy.domain.repository.AuthRepository
import jr.brian.inindy.domain.repository.GroupRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupManagementViewModel(
    private val groupId: String,
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupManagementUiState())
    val uiState: StateFlow<GroupManagementUiState> = _uiState.asStateFlow()

    private val _postNavigation = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val postNavigation: SharedFlow<String> = _postNavigation.asSharedFlow()

    init {
        onIntent(GroupManagementIntent.Load)
    }

    fun onIntent(intent: GroupManagementIntent) {
        when (intent) {
            GroupManagementIntent.Load -> load()
            is GroupManagementIntent.RemoveMember -> removeMember(intent.userId)
            GroupManagementIntent.GenerateInviteLink -> generateInvite()
            GroupManagementIntent.DismissInviteLink -> dismissInviteLink()
            is GroupManagementIntent.RevokeInvite -> revokeInvite(intent.inviteId)
            GroupManagementIntent.ShowDeleteConfirmation -> showDeleteConfirmation()
            GroupManagementIntent.DismissDeleteConfirmation -> dismissDeleteConfirmation()
            GroupManagementIntent.ConfirmDeleteGroup -> confirmDeleteGroup()
            is GroupManagementIntent.NavigateToPost -> emitNavigateToPost(intent.postId)
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val currentUserId = authRepository.getCurrentUser()?.id

            val groupDeferred = async { groupRepository.getGroupById(groupId) }
            val membersDeferred = async { groupRepository.getGroupMembers(groupId) }
            val postsDeferred = async { groupRepository.getGroupPosts(groupId, limit = 3) }
            val invitesDeferred = async { groupRepository.getPendingInvites(groupId) }

            val groupResult = groupDeferred.await()
            val members = membersDeferred.await().getOrDefault(emptyList())
            val posts = postsDeferred.await().getOrDefault(emptyList())
            val invites = invitesDeferred.await().getOrDefault(emptyList())

            val group = groupResult.getOrNull()
            val role = members.firstOrNull { it.userId == currentUserId }?.role
                ?: group?.role
                ?: GroupRole.MEMBER

            _uiState.value = GroupManagementUiState(
                group = group,
                members = members,
                recentPosts = posts,
                pendingInvites = invites,
                currentUserRole = role,
                isLoading = false,
                error = groupResult.exceptionOrNull()?.message
            )
        }
    }

    private fun removeMember(userId: String) {
        viewModelScope.launch {
            groupRepository.removeMember(groupId, userId).onSuccess { refresh() }
        }
    }

    private fun generateInvite() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingInvite = true)
            val link = groupRepository.generateInviteLink(groupId).getOrNull()
            _uiState.value = _uiState.value.copy(
                isGeneratingInvite = false,
                inviteLink = link
            )
            refresh()
        }
    }

    private fun dismissInviteLink() {
        _uiState.value = _uiState.value.copy(inviteLink = null)
    }

    private fun revokeInvite(inviteId: String) {
        viewModelScope.launch {
            groupRepository.revokeInvite(inviteId).onSuccess { refresh() }
        }
    }

    private fun showDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = true)
    }

    private fun dismissDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = false)
    }

    private fun confirmDeleteGroup() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showDeleteConfirmation = false)
            groupRepository.deleteGroup(groupId).onSuccess {
                _uiState.value = _uiState.value.copy(deleted = true)
            }
        }
    }

    private fun emitNavigateToPost(postId: String) {
        viewModelScope.launch { _postNavigation.emit(postId) }
    }

    private fun refresh() {
        viewModelScope.launch {
            val membersDeferred = async { groupRepository.getGroupMembers(groupId) }
            val invitesDeferred = async { groupRepository.getPendingInvites(groupId) }
            val groupDeferred = async { groupRepository.getGroupById(groupId) }
            val members = membersDeferred.await().getOrDefault(emptyList())
            val invites = invitesDeferred.await().getOrDefault(emptyList())
            val group = groupDeferred.await().getOrNull()
            _uiState.value = _uiState.value.copy(
                group = group ?: _uiState.value.group,
                members = members,
                pendingInvites = invites
            )
        }
    }
}
