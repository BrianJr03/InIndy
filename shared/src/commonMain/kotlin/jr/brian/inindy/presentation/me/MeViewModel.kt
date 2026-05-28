package jr.brian.inindy.presentation.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.domain.repository.AttendanceRepository
import jr.brian.inindy.domain.repository.GroupRepository
import jr.brian.inindy.domain.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class MeViewModel(
    private val postRepository: PostRepository,
    private val groupRepository: GroupRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeUiState(user = MOCK_USER))
    val uiState: StateFlow<MeUiState> = _uiState.asStateFlow()

    init {
        observeData()
        load()
    }

    private fun observeData() {
        combine(
            postRepository.observeUserPosts(),
            groupRepository.observeUserGroups()
        ) { posts, groups ->
            _uiState.value = _uiState.value.copy(
                recentPosts = posts,
                groups = groups
            )
        }.launchIn(viewModelScope)
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val attendance = attendanceRepository.getAttendanceHistory().getOrDefault(emptyList())
            val rate = attendanceRepository.getAttendanceRate().getOrDefault(0f)
            _uiState.value = _uiState.value.copy(
                attendanceHistory = attendance,
                attendanceRate = rate,
                isLoading = false
            )
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            postRepository.deletePost(postId)
        }
    }

    fun createGroup(name: String, description: String?) {
        viewModelScope.launch {
            groupRepository.createGroup(name, description)
        }
    }

    private companion object {
        val MOCK_USER = User(
            id = "me",
            fullName = "Brian",
            avatarUrl = null,
            phoneVerified = true,
            neighborhoodId = "broad-ripple"
        )
    }
}
