package jr.brian.inindy.presentation.explore

import jr.brian.inindy.domain.model.Group

sealed class ExploreIntent {
    data object ToggleFilterDropdown : ExploreIntent()
    data object DismissFilterDropdown : ExploreIntent()
    data object SelectFilterAll : ExploreIntent()
    data object SelectFilterNeighborhood : ExploreIntent()
    data object OpenGroupSearch : ExploreIntent()
    data object DismissGroupSearch : ExploreIntent()
    data class GroupSearchQueryChanged(val query: String) : ExploreIntent()
    data class SelectFilterGroup(val group: Group) : ExploreIntent()
}
