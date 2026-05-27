package jr.brian.inindy.presentation.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.domain.usecase.GetExplorePostsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExploreViewModel(
    private val getExplorePosts: GetExplorePostsUseCase
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
}
