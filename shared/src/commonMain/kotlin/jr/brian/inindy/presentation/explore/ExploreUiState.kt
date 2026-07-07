package jr.brian.inindy.presentation.explore

import jr.brian.inindy.domain.model.ExploreFilter
import jr.brian.inindy.domain.model.Group
import jr.brian.inindy.domain.model.Post

data class ExploreUiState(
    val feed: FeedState = FeedState.Loading,
    val activeFilter: ExploreFilter = ExploreFilter.All,
    val neighborhoodName: String = "",
    val brandMarkText: String = "InIndy",
    val isFilterDropdownVisible: Boolean = false,
    val isGroupSearchSheetVisible: Boolean = false,
    val groupSearchQuery: String = "",
    val userGroups: List<Group> = emptyList(),
    val searchedGroups: List<Group> = emptyList(),
    val isSearchingGroups: Boolean = false,
    val lastSelectedGroup: Group? = null,
    val isRefreshing: Boolean = false
) {
    sealed class FeedState {
        data object Loading : FeedState()
        data class Success(val posts: List<Post>) : FeedState()
        data class Error(val message: String) : FeedState()
    }
}
