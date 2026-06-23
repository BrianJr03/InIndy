package jr.brian.inindy.presentation.me

import jr.brian.inindy.domain.model.Group

sealed class GroupInviteUiState {
    data object Loading : GroupInviteUiState()
    data class Preview(val group: Group) : GroupInviteUiState()
    data class Joining(val group: Group) : GroupInviteUiState()
    data class Joined(val group: Group) : GroupInviteUiState()
    data object Error : GroupInviteUiState()
}
