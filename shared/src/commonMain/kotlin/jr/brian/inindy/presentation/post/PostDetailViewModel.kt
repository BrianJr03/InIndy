package jr.brian.inindy.presentation.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.repository.ExploreRepository
import jr.brian.inindy.domain.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PostDetailViewModel(
    private val postRepository: PostRepository,
    private val exploreRepository: ExploreRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<PostDetailUiState>(PostDetailUiState.Loading)
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    fun load(postId: String) {
        viewModelScope.launch {
            _uiState.value = PostDetailUiState.Loading
            val post = postRepository.getPostById(postId).getOrNull()
                ?: findInExplore(postId)
            if (post == null) {
                _uiState.value = PostDetailUiState.Error("Post not found")
                return@launch
            }
            _uiState.value = PostDetailUiState.Success(
                post = post,
                isHost = post.userId == CURRENT_USER_ID,
                isRsvpd = exploreRepository.isRsvpd(post.id)
            )
        }
    }

    fun rsvp() {
        val current = _uiState.value as? PostDetailUiState.Success ?: return
        if (current.isRsvpd) return
        viewModelScope.launch {
            exploreRepository.rsvp(current.post.id).onSuccess {
                _uiState.value = current.copy(
                    post = current.post.copy(rsvpCount = current.post.rsvpCount + 1),
                    isRsvpd = true
                )
            }
        }
    }

    fun cancelRsvp() {
        val current = _uiState.value as? PostDetailUiState.Success ?: return
        if (!current.isRsvpd) return
        viewModelScope.launch {
            exploreRepository.unRsvp(current.post.id).onSuccess {
                _uiState.value = current.copy(
                    post = current.post.copy(rsvpCount = (current.post.rsvpCount - 1).coerceAtLeast(0)),
                    isRsvpd = false
                )
            }
        }
    }

    fun delete() {
        val current = _uiState.value as? PostDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isDeleting = true)
            postRepository.deletePost(current.post.id)
                .onSuccess {
                    _uiState.value = current.copy(isDeleting = false, deleted = true)
                }
                .onFailure {
                    _uiState.value = current.copy(isDeleting = false)
                }
        }
    }

    fun loadAttendees() {
        val current = _uiState.value as? PostDetailUiState.Success ?: return
        if (current.attendees.isNotEmpty() || current.attendeesLoading) return
        viewModelScope.launch {
            _uiState.value = current.copy(attendeesLoading = true)
            val result = exploreRepository.getAttendees(current.post.id).getOrDefault(emptyList())
            val latest = _uiState.value as? PostDetailUiState.Success ?: return@launch
            _uiState.value = latest.copy(attendees = result, attendeesLoading = false)
        }
    }

    private suspend fun findInExplore(postId: String): Post? {
        val snapshot = exploreRepository.getPosts().first()
        return snapshot.getOrNull()?.firstOrNull { it.id == postId }
    }

    private companion object {
        const val CURRENT_USER_ID = "me"
    }
}
