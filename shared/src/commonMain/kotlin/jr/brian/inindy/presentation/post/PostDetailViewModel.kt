package jr.brian.inindy.presentation.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.domain.repository.ExploreRepository
import jr.brian.inindy.domain.repository.PostRepository
import jr.brian.inindy.domain.usecase.RsvpPostUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PostDetailViewModel(
    private val postRepository: PostRepository,
    private val exploreRepository: ExploreRepository,
    private val rsvpPost: RsvpPostUseCase,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<PostDetailUiState>(PostDetailUiState.Loading)
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    fun load(postId: String) {
        viewModelScope.launch {
            _uiState.value = PostDetailUiState.Loading
            val post = postRepository.getPostById(postId).getOrNull()
                ?: findInExplore(postId)
            if (post == null) {
                _uiState.value = PostDetailUiState.Unavailable
                return@launch
            }
            val currentUserId = currentUserProvider.get().userId
            _uiState.value = PostDetailUiState.Success(
                post = post,
                isHost = post.userId == currentUserId,
                isRsvpd = rsvpPost.isRsvpd(post.id),
                attendeesLoading = true
            )
            loadAttendees()
        }
    }

    fun rsvp() {
        val current = _uiState.value as? PostDetailUiState.Success ?: return
        if (current.isRsvpd) return
        viewModelScope.launch {
            rsvpPost(current.post.id).onSuccess {
                val me = currentUserAsAttendee() ?: return@onSuccess
                val latest = _uiState.value as? PostDetailUiState.Success ?: return@onSuccess
                _uiState.value = latest.copy(
                    post = latest.post.copy(rsvpCount = latest.post.rsvpCount + 1),
                    isRsvpd = true,
                    attendees = if (latest.attendees.any { it.id == me.id }) latest.attendees
                    else latest.attendees + me
                )
            }
        }
    }

    fun cancelRsvp() {
        val current = _uiState.value as? PostDetailUiState.Success ?: return
        if (!current.isRsvpd) return
        viewModelScope.launch {
            rsvpPost.unRsvp(current.post.id).onSuccess {
                val meId = currentUserProvider.get().userId
                val latest = _uiState.value as? PostDetailUiState.Success ?: return@onSuccess
                _uiState.value = latest.copy(
                    post = latest.post.copy(rsvpCount = (latest.post.rsvpCount - 1).coerceAtLeast(0)),
                    isRsvpd = false,
                    attendees = latest.attendees.filterNot { it.id == meId }
                )
            }
        }
    }

    private suspend fun currentUserAsAttendee(): User? {
        val prefs = currentUserProvider.get()
        val id = prefs.userId ?: return null
        return User(id = id, fullName = prefs.fullName, avatarUrl = prefs.avatarUrl)
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
        viewModelScope.launch {
            if (!current.attendeesLoading) {
                _uiState.value = current.copy(attendeesLoading = true)
            }
            val result = postRepository.getPostAttendees(current.post.id).getOrDefault(emptyList())
            val latest = _uiState.value as? PostDetailUiState.Success ?: return@launch
            _uiState.value = latest.copy(attendees = result, attendeesLoading = false)
        }
    }

    private suspend fun findInExplore(postId: String): Post? {
        val snapshot = exploreRepository.getPosts().first()
        return snapshot.getOrNull()?.firstOrNull { it.id == postId }
    }
}
