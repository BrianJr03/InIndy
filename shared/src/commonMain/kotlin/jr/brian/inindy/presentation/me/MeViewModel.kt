package jr.brian.inindy.presentation.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.domain.repository.AttendanceRepository
import jr.brian.inindy.domain.repository.GroupRepository
import jr.brian.inindy.domain.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MeViewModel(
    private val postRepository: PostRepository,
    private val groupRepository: GroupRepository,
    private val attendanceRepository: AttendanceRepository,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow(MeUiState())
    val uiState: StateFlow<MeUiState> = _uiState.asStateFlow()

    init {
        observeData()
        load()
        loadUserPosts()
    }

    fun refresh() {
        loadUserPosts()
        loadUserGroups()
    }

    private fun loadUserPosts() {
        viewModelScope.launch {
            postRepository.getUserPosts().onSuccess { posts ->
                _uiState.value = _uiState.value.copy(recentPosts = posts)
            }
        }
    }

    private fun loadUserGroups() {
        viewModelScope.launch {
            groupRepository.getUserGroups().onSuccess { groups ->
                _uiState.value = _uiState.value.copy(groups = groups)
            }
        }
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

        currentUserProvider.user
            .onEach { prefs ->
                val user = User(
                    id = prefs.userId ?: "me",
                    fullName = prefs.fullName,
                    avatarUrl = prefs.avatarUrl,
                    phoneVerified = true,
                    neighborhoodId = prefs.neighborhoodId,
                    interests = prefs.interests.mapNotNull { name ->
                        runCatching { Interest.valueOf(name) }.getOrNull()
                    }
                )
                _uiState.value = _uiState.value.copy(
                    user = user,
                    neighborhoodName = prefs.neighborhoodName ?: _uiState.value.neighborhoodName
                )
            }
            .launchIn(viewModelScope)
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val prefs = currentUserProvider.get()
            val user = User(
                id = prefs.userId ?: "me",
                fullName = prefs.fullName,
                avatarUrl = prefs.avatarUrl,
                phoneVerified = true,
                neighborhoodId = prefs.neighborhoodId,
                interests = prefs.interests.mapNotNull { name ->
                    runCatching { Interest.valueOf(name) }.getOrNull()
                }
            )
            val attendance = attendanceRepository.getAttendanceHistory().getOrDefault(emptyList())
            val rate = attendanceRepository.getAttendanceRate().getOrDefault(0f)
            _uiState.value = _uiState.value.copy(
                user = user,
                neighborhoodName = prefs.neighborhoodName ?: _uiState.value.neighborhoodName,
                attendanceHistory = attendance,
                attendanceRate = rate,
                isLoading = false
            )
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            postRepository.deletePost(postId)
                .onSuccess {
                    _uiState.update { current ->
                        current.copy(
                            recentPosts = current.recentPosts.filterNot { it.id == postId }
                        )
                    }
                }
                .onFailure { e ->
                    println("[InIndy] MeViewModel deletePost FAILED — postId: $postId, error: ${e.message}")
                }
        }
    }
}
