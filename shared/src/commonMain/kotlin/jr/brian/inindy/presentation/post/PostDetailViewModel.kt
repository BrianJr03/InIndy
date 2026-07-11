package jr.brian.inindy.presentation.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.domain.model.applyRsvpDelta
import jr.brian.inindy.domain.model.isOwnedBy
import jr.brian.inindy.domain.model.withRsvpCountDelta
import jr.brian.inindy.domain.repository.PostRepository
import jr.brian.inindy.domain.usecase.RsvpPostUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PostDetailViewModel(
    private val postRepository: PostRepository,
    private val rsvpPost: RsvpPostUseCase,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<PostDetailUiState>(PostDetailUiState.Loading)
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var observedPostId: String? = null

    fun load(postId: String) {
        if (observedPostId == postId && observeJob?.isActive == true) return

        observeJob?.cancel()
        observedPostId = postId
        _uiState.value = PostDetailUiState.Loading

        observeJob = viewModelScope.launch {
            var hasLoaded = false
            postRepository.observePost(postId).collect { result ->
                result.fold(
                    onSuccess = { post ->
                        val currentUserId = currentUserProvider.get().userId
                        val previous = _uiState.value as? PostDetailUiState.Success
                        _uiState.value = if (previous != null) {
                            previous.copy(
                                post = post,
                                isHost = post.isOwnedBy(currentUserId),
                                isRsvpd = rsvpPost.isRsvpd(post.id)
                            )
                        } else {
                            PostDetailUiState.Success(
                                post = post,
                                isHost = post.isOwnedBy(currentUserId),
                                isRsvpd = rsvpPost.isRsvpd(post.id),
                                attendeesLoading = true
                            )
                        }
                        if (!hasLoaded) {
                            hasLoaded = true
                            loadAttendees()
                        }
                    },
                    onFailure = {
                        val current = _uiState.value as? PostDetailUiState.Success
                        // Preserve user-initiated delete flow — the screen pops via
                        // `deleted = true`, don't overwrite with Unavailable.
                        if (current?.isDeleting == true || current?.deleted == true) return@fold
                        if (hasLoaded) {
                            _uiState.value = PostDetailUiState.Unavailable
                            return@fold
                        }
                        val fallback = postRepository.getPostById(postId).getOrNull()
                        if (fallback == null) {
                            _uiState.value = PostDetailUiState.Unavailable
                            return@fold
                        }
                        val currentUserId = currentUserProvider.get().userId
                        _uiState.value = PostDetailUiState.Success(
                            post = fallback,
                            isHost = fallback.isOwnedBy(currentUserId),
                            isRsvpd = rsvpPost.isRsvpd(fallback.id),
                            attendeesLoading = true
                        )
                        hasLoaded = true
                        loadAttendees()
                    }
                )
            }
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
                    post = latest.post.withRsvpCountDelta(1),
                    isRsvpd = true,
                    attendees = latest.attendees.applyRsvpDelta(1, me)
                )
            }
        }
    }

    fun cancelRsvp() {
        val current = _uiState.value as? PostDetailUiState.Success ?: return
        if (!current.isRsvpd) return
        viewModelScope.launch {
            rsvpPost.unRsvp(current.post.id).onSuccess {
                val me = currentUserAsAttendee()
                val latest = _uiState.value as? PostDetailUiState.Success ?: return@onSuccess
                _uiState.value = latest.copy(
                    post = latest.post.withRsvpCountDelta(-1),
                    isRsvpd = false,
                    attendees = latest.attendees.applyRsvpDelta(-1, me)
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
                    // Cancel realtime observation so the Delete event that fires
                    // as a result of our own deletePost can't race and overwrite
                    // deleted = true with Unavailable.
                    observeJob?.cancel()
                    val latest = _uiState.value as? PostDetailUiState.Success ?: return@onSuccess
                    _uiState.value = latest.copy(isDeleting = false, deleted = true)
                }
                .onFailure {
                    val latest = _uiState.value as? PostDetailUiState.Success ?: return@onFailure
                    _uiState.value = latest.copy(isDeleting = false)
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

    override fun onCleared() {
        observeJob?.cancel()
        super.onCleared()
    }
}
