package jr.brian.inindy.presentation.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.usecase.GetExplorePostsUseCase
import jr.brian.inindy.domain.usecase.RsvpPostUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExploreViewModel(
    private val getExplorePosts: GetExplorePostsUseCase,
    private val rsvpPost: RsvpPostUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExploreUiState>(ExploreUiState.Loading)
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _uiState.value = ExploreUiState.Loading
            getExplorePosts().collect { result ->
                _uiState.value = result.fold(
                    onSuccess = { ExploreUiState.Success(it) },
                    onFailure = { ExploreUiState.Error(it.message ?: "Something went wrong") }
                )
            }
        }
    }

    fun rsvp(postId: String) {
        viewModelScope.launch {
            rsvpPost(postId)
        }
    }

    fun unRsvp(postId: String) {
        viewModelScope.launch {
            rsvpPost.unRsvp(postId)
        }
    }

    fun isRsvpd(postId: String): Boolean = rsvpPost.isRsvpd(postId)

    fun findPost(postId: String): Post? =
        (_uiState.value as? ExploreUiState.Success)?.posts?.firstOrNull { it.id == postId }
}
