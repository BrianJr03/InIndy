package jr.brian.inindy.presentation.post

import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.model.User

sealed class PostDetailUiState {
    object Loading : PostDetailUiState()
    data class Success(
        val post: Post,
        val isHost: Boolean,
        val isRsvpd: Boolean,
        val isDeleting: Boolean = false,
        val deleted: Boolean = false,
        val attendees: List<User> = emptyList(),
        val attendeesLoading: Boolean = false
    ) : PostDetailUiState()
    data object Unavailable : PostDetailUiState()
}

sealed class PostDetailIntent {
    object Load : PostDetailIntent()
    object Rsvp : PostDetailIntent()
    object CancelRsvp : PostDetailIntent()
    object Edit : PostDetailIntent()
    object Delete : PostDetailIntent()
}
