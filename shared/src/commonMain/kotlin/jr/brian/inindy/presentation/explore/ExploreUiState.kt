package jr.brian.inindy.presentation.explore

import jr.brian.inindy.domain.model.Post

sealed class ExploreUiState {
    data object Loading : ExploreUiState()
    data class Success(val posts: List<Post>) : ExploreUiState()
    data class Error(val message: String) : ExploreUiState()
}
