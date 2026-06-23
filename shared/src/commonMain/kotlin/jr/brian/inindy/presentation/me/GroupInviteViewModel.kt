package jr.brian.inindy.presentation.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.domain.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupInviteViewModel(
    private val groupRepository: GroupRepository
) : ViewModel() {
    private val _state = MutableStateFlow<GroupInviteUiState>(GroupInviteUiState.Loading)
    val state: StateFlow<GroupInviteUiState> = _state.asStateFlow()

    private var loadedToken: String? = null

    fun load(token: String) {
        if (token == loadedToken) return
        loadedToken = token
        _state.value = GroupInviteUiState.Loading
        viewModelScope.launch {
            groupRepository.getGroupByInviteToken(token)
                .onSuccess { _state.value = GroupInviteUiState.Preview(it) }
                .onFailure { _state.value = GroupInviteUiState.Error }
        }
    }

    fun join() {
        val token = loadedToken ?: return
        val previewGroup = when (val current = _state.value) {
            is GroupInviteUiState.Preview -> current.group
            is GroupInviteUiState.Joining -> return
            else -> return
        }
        _state.value = GroupInviteUiState.Joining(previewGroup)
        viewModelScope.launch {
            groupRepository.joinGroupByToken(token)
                .onSuccess { _state.value = GroupInviteUiState.Joined(it) }
                .onFailure { _state.value = GroupInviteUiState.Error }
        }
    }

    fun reset() {
        loadedToken = null
        _state.value = GroupInviteUiState.Loading
    }
}
