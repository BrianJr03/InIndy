package jr.brian.inindy.presentation.me

sealed class GroupManagementIntent {
    data object Load : GroupManagementIntent()
    data class RemoveMember(val userId: String) : GroupManagementIntent()
    data object GenerateInviteLink : GroupManagementIntent()
    data object DismissInviteLink : GroupManagementIntent()
    data class RevokeInvite(val inviteId: String) : GroupManagementIntent()
    data object ShowDeleteConfirmation : GroupManagementIntent()
    data object DismissDeleteConfirmation : GroupManagementIntent()
    data object ConfirmDeleteGroup : GroupManagementIntent()
    data class NavigateToPost(val postId: String) : GroupManagementIntent()
}
